import { TestBed } from '@angular/core/testing';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { firstValueFrom, Observable, of, Subject } from 'rxjs';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import type { PiipPortfolioRecord, PiipRecordType } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PortfolioRecordEditComponent } from './portfolio-record-edit.component';

describe('PortfolioRecordEditComponent', () => {
  async function setup(
    recordType: PiipRecordType = 'Iniciativa',
    code = 'I-024-2026',
    configure?: (repository: PiipMockRepository) => void,
  ) {
    const paramMap = convertToParamMap({ code });
    const dialogResult = new Subject<boolean | undefined>();
    const dialogRef = { afterClosed: () => dialogResult.asObservable() };
    const dialog = { open: vi.fn().mockReturnValue(dialogRef) };
    const blockScroll = vi.fn().mockReturnValue({});
    await TestBed.configureTestingModule({
      imports: [PortfolioRecordEditComponent],
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
        { provide: MatDialog, useValue: dialog },
        { provide: Overlay, useValue: { scrollStrategies: { block: blockScroll } } },
        { provide: ActivatedRoute, useValue: {
          data: of({ recordType }),
          paramMap: of(paramMap),
          snapshot: { data: { recordType }, paramMap },
        } },
      ],
    }).compileComponents();

    const repository = TestBed.inject(PiipMockRepository);
    configure?.(repository);
    const fixture = TestBed.createComponent(PortfolioRecordEditComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return {
      fixture,
      component: fixture.componentInstance,
      repository,
      router: TestBed.inject(Router),
      dialog,
      dialogResult,
      blockScroll,
      code,
    };
  }

  function markEditableField(component: PortfolioRecordEditComponent, value = 'Nombre actualizado'): void {
    component.form.controls.name.setValue(value);
    component.form.controls.name.markAsDirty();
  }

  it('desplaza, conserva la ruta y enfoca el item activo sin navegación Angular', async () => {
    const originalUrl = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    window.history.replaceState(window.history.state, '', `${window.location.pathname}?source=detail#datos-generales`);
    const { component, fixture, router } = await setup();
    const section = fixture.nativeElement.querySelector('#clasificacion') as HTMLElement;
    const anchor = fixture.nativeElement.querySelector('a[href="#clasificacion"]') as HTMLAnchorElement;
    const scrollIntoView = vi.fn();
    section.scrollIntoView = scrollIntoView;
    const focus = vi.spyOn(anchor, 'focus');
    const event = { preventDefault: vi.fn(), currentTarget: anchor } as unknown as Event;
    const replaceState = vi.spyOn(window.history, 'replaceState').mockImplementation(() => undefined);
    const navigate = vi.spyOn(router, 'navigate');

    component.scrollToSection('clasificacion', event);
    fixture.detectChanges();

    expect(event.preventDefault).toHaveBeenCalledOnce();
    expect(scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
    expect(replaceState).toHaveBeenCalledWith(window.history.state, '', `${window.location.pathname}${window.location.search}#clasificacion`);
    expect(focus).toHaveBeenCalledWith({ preventScroll: true });
    expect(document.activeElement).toBe(anchor);
    expect(anchor.classList.contains('active')).toBe(true);
    expect(anchor.getAttribute('aria-current')).toBe('location');
    expect(fixture.nativeElement.querySelectorAll('.section-nav a[aria-current="location"]')).toHaveLength(1);
    expect(navigate).not.toHaveBeenCalled();

    replaceState.mockRestore();
    window.history.replaceState(window.history.state, '', originalUrl);
  });

  it.each([
    ['#alineamiento', 'alineamiento'],
    ['#seccion-desconocida', 'datos-generales'],
    ['#%E0%A4%A', 'datos-generales'],
  ] as const)('inicializa la sección desde un hash válido o usa el fallback (%s)', async (hash, expectedSection) => {
    const originalUrl = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    window.history.replaceState(window.history.state, '', `${window.location.pathname}${window.location.search}${hash}`);

    const { component, fixture } = await setup();
    const activeAnchor = fixture.nativeElement.querySelector('.section-nav a.active') as HTMLAnchorElement;

    expect(component.selectedSection()).toBe(expectedSection);
    expect(activeAnchor.getAttribute('href')).toBe(`#${expectedSection}`);
    expect(activeAnchor.getAttribute('aria-current')).toBe('location');

    window.history.replaceState(window.history.state, '', originalUrl);
  });

  it('ignora identificadores fuera del conjunto permitido', async () => {
    const { component } = await setup();
    const event = { preventDefault: vi.fn(), currentTarget: null } as unknown as Event;
    const replaceState = vi.spyOn(window.history, 'replaceState');

    component.scrollToSection('seccion-desconocida', event);

    expect(event.preventDefault).toHaveBeenCalledOnce();
    expect(replaceState).not.toHaveBeenCalled();
    expect(component.selectedSection()).toBe('datos-generales');
    replaceState.mockRestore();
  });

  it('activa la sección visible sin robar el foco del campo en edición y desconecta el observer', async () => {
    let observerCallback: IntersectionObserverCallback | undefined;
    const disconnect = vi.fn();
    const observe = vi.fn();
    vi.stubGlobal('IntersectionObserver', vi.fn((callback: IntersectionObserverCallback) => {
      observerCallback = callback;
      return { disconnect, observe, unobserve: vi.fn() } as unknown as IntersectionObserver;
    }));

    try {
      const { component, fixture } = await setup();
      const input = fixture.nativeElement.querySelector('#record-name') as HTMLInputElement;
      const section = fixture.nativeElement.querySelector('#clasificacion') as HTMLElement;
      input.focus();
      observerCallback?.([
        {
          target: section,
          isIntersecting: true,
          intersectionRatio: 0.8,
          boundingClientRect: { top: 140 },
        } as unknown as IntersectionObserverEntry,
      ], {} as IntersectionObserver);
      fixture.detectChanges();

      expect(component.selectedSection()).toBe('clasificacion');
      expect(fixture.nativeElement.querySelectorAll('.section-nav a[aria-current="location"]')).toHaveLength(1);
      expect(document.activeElement).toBe(input);

      fixture.destroy();
      expect(disconnect).toHaveBeenCalled();
    } finally {
      vi.unstubAllGlobals();
    }
  });

  it('carga la variante iniciativa y envía un body sparse con confirmación y navegación al detalle', async () => {
    const { component, fixture, repository, router, code } = await setup();
    const current = repository.getInitiativeDetail(code)!.portfolioRecord;
    const updated = { ...current, name: 'Nombre actualizado', version: 1 } as PiipPortfolioRecord;
    const update = vi.spyOn(repository, 'updateInitiative').mockResolvedValue(updated);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    markEditableField(component);
    await component.save();

    expect(update).toHaveBeenCalledWith(code, expect.objectContaining({ version: 0, name: 'Nombre actualizado' }));
    const body = update.mock.calls[0]![1] as unknown as Record<string, unknown>;
    expect(body).not.toHaveProperty('startDate');
    expect(body).not.toHaveProperty('sourceId');
    expect(component.variant()).toBe('INITIATIVE');
    expect(fixture.nativeElement.querySelector('select[formcontrolname="solutionTypeId"]')).not.toBeNull();
    expect(component.form.pristine).toBe(true);
    expect(navigate).toHaveBeenCalledWith(['/iniciativas', code], { queryParams: { updated: '1' } });
  });

  it('carga la variante proyecto y envía una sola Unidad Orgánica en el PATCH', async () => {
    const { component, fixture, repository, router, code } = await setup('Proyecto', 'P-005-2026');
    const current = repository.getProjectDetail(code)!.portfolioRecord;
    repository.organizationalUnits.set([
      ...repository.organizationalUnits(),
      { id: 102, code: 'UO-102', name: 'Unidad secundaria', acronym: 'US', parentId: null, executingUnitId: 1, active: true },
    ]);
    const updated = { ...current, keyResults: 'Resultado actualizado', responsibleUnitReferences: [repository.organizationalUnits()[1]], version: 1 } as PiipPortfolioRecord;
    const update = vi.spyOn(repository, 'updateProject').mockResolvedValue(updated);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.form.controls.keyResults.setValue('Resultado actualizado');
    component.form.controls.keyResults.markAsDirty();
    component.form.controls.solutionTypeId.setValue('1');
    component.form.controls.solutionTypeId.markAsDirty();
    component.setResponsibleUnitIds([102]);
    await component.save();

    expect(update).toHaveBeenCalledWith(code, expect.objectContaining({ keyResults: 'Resultado actualizado', responsibleUnitIds: [102] }));
    expect(update.mock.calls[0]![1]).not.toHaveProperty('solutionTypeId');
    expect(component.variant()).toBe('PREEXISTING_PROJECT');
    expect(fixture.nativeElement.querySelector('select[formcontrolname="solutionTypeId"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Tipo de solución');
    expect(fixture.nativeElement.textContent).toContain('No aplica');
  });

  it('expone y emite Tipo de solución para un proyecto derivado', async () => {
    const { component, fixture, repository, router, code } = await setup('Proyecto', 'P-003-2026', (repository) => {
      const preexisting = repository.portfolioRecords().find((record) => record.code === 'P-005-2026')!;
      const solutionTypeReference = repository.catalogs().value.solutionTypes.find((option) => option.id === 1)!;
      repository.portfolioRecords.update((records) => [...records, {
        ...preexisting,
        code: 'P-003-2026',
        originCode: 'I-012-2026',
        solutionType: 'Solución potencial o adaptable',
        solutionTypeReference,
      }]);
    });

    expect(component.variant()).toBe('DERIVED_PROJECT');
    expect(fixture.nativeElement.querySelector('select[formcontrolname="solutionTypeId"]')).not.toBeNull();

    const current = repository.getProjectDetail(code)!.portfolioRecord;
    const solutionTypeReference = repository.catalogs().value.solutionTypes.find((option) => option.id === 2)!;
    const update = vi.spyOn(repository, 'updateProject').mockResolvedValue({
      ...current,
      solutionType: 'Solución por definir',
      solutionTypeReference,
      version: 1,
    });
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.form.controls.solutionTypeId.setValue('2');
    component.form.controls.solutionTypeId.markAsDirty();

    await component.save();

    expect(update).toHaveBeenCalledWith(code, expect.objectContaining({ version: 0, solutionTypeId: 2 }));
  });

  it('muestra updatedAt en hora de Lima y usa fallback si está ausente o es inválido', async () => {
    const updatedAt = '2026-08-22T10:00:00Z';
    const { component, fixture, repository, code } = await setup('Iniciativa', 'I-024-2026', (mock) => {
      mock.portfolioRecords.update((records) => records.map((record) => record.code === 'I-024-2026' ? { ...record, updatedAt } : record));
    });
    const expected = new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima',
    }).format(new Date(updatedAt));

    expect(component.formatDateTime(updatedAt)).toBe(expected);
    expect(fixture.nativeElement.textContent).toContain(expected);
    expect(component.formatDateTime()).toBe('Sin información registrada');
    expect(component.formatDateTime('fecha-inválida')).toBe('Sin información registrada');

    repository.portfolioRecords.update((records) => records.map((record) => record.code === code ? { ...record, updatedAt: 'fecha-inválida' } : record));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Sin información registrada');
  });

  it('muestra el nombre de la Unidad Ejecutora sin sustituirlo por su identificador', async () => {
    const executingUnit = 'Programa de Desarrollo Productivo Agrario Rural — AGRO RURAL';
    const { component, fixture } = await setup('Iniciativa', 'I-024-2026', (repository) => {
      repository.portfolioRecords.update((records) => records.map((record) => record.code === 'I-024-2026'
        ? { ...record, executingUnitId: 987, executingUnit }
        : record));
    });
    const value = fixture.nativeElement.querySelector('.executing-unit-value') as HTMLElement;

    expect(value.textContent?.trim()).toBe(executingUnit);
    expect(value.textContent).not.toContain('987');
    expect(component.record()?.executingUnitId).toBe(987);
  });

  it.each([undefined, '', '   '])('usa el fallback explícito cuando la Unidad Ejecutora no está informada (%s)', async (executingUnit) => {
    const { fixture } = await setup('Iniciativa', 'I-024-2026', (repository) => {
      repository.portfolioRecords.update((records) => records.map((record) => record.code === 'I-024-2026'
        ? { ...record, executingUnit }
        : record));
    });

    expect(fixture.nativeElement.querySelector('.executing-unit-value')?.textContent.trim()).toBe('Sin información registrada');
  });

  it('no genera PATCH cuando solo permanece la versión baseline', async () => {
    const { component, repository } = await setup();
    const update = vi.spyOn(repository, 'updateInitiative');

    await component.save();

    expect(update).not.toHaveBeenCalled();
    expect(component.errorMessage()).toBe('No hay cambios efectivos para guardar.');
  });

  it('envía referencias opcionales como null y conserva la UO única', async () => {
    const { component, repository, code } = await setup();
    const current = repository.getInitiativeDetail(code)!.portfolioRecord;
    const update = vi.spyOn(repository, 'updateInitiative').mockResolvedValue({ ...current, peiObjectiveReference: null, poiActivityReference: null, version: 1 });

    component.form.controls.peiObjectiveId.setValue('');
    component.form.controls.peiObjectiveId.markAsDirty();
    component.form.controls.poiActivityId.setValue('');
    component.form.controls.poiActivityId.markAsDirty();
    component.setResponsibleUnitIds([101]);
    await component.save();

    expect(update.mock.calls[0]![1]).toEqual(expect.objectContaining({ version: 0, peiObjectiveId: null, poiActivityId: null }));
    expect(update.mock.calls[0]![1]).not.toHaveProperty('responsibleUnitIds');
  });

  it('activa beforeunload solo cuando el formulario está sucio', async () => {
    const { component } = await setup();
    const cleanEvent = { preventDefault: vi.fn(), returnValue: undefined } as unknown as BeforeUnloadEvent;
    component.onBeforeUnload(cleanEvent);
    expect(cleanEvent.preventDefault).not.toHaveBeenCalled();

    markEditableField(component);
    const dirtyEvent = { preventDefault: vi.fn(), returnValue: undefined } as unknown as BeforeUnloadEvent;
    component.onBeforeUnload(dirtyEvent);
    expect(dirtyEvent.preventDefault).toHaveBeenCalledOnce();
    expect(dirtyEvent.returnValue).toBe('');
  });

  it('abre una sola confirmación accesible y conserva los cambios al seguir editando', async () => {
    const { component, dialog, dialogResult, blockScroll, code } = await setup();
    markEditableField(component);

    const firstDecision = component.confirmPendingChanges();
    const secondDecision = component.confirmPendingChanges();
    const firstResult = firstValueFrom(firstDecision);
    const secondResult = firstValueFrom(secondDecision);

    expect(dialog.open).toHaveBeenCalledOnce();
    expect(dialog.open).toHaveBeenCalledWith(expect.any(Function), expect.objectContaining({
      data: { recordType: 'Iniciativa', code },
      role: 'alertdialog',
      ariaLabelledBy: 'pending-changes-title',
      ariaDescribedBy: 'pending-changes-description',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      disableClose: false,
    }));
    expect(blockScroll).toHaveBeenCalledOnce();

    dialogResult.next(false);
    dialogResult.complete();

    await expect(firstResult).resolves.toBe(false);
    await expect(secondResult).resolves.toBe(false);
    expect(component.form.controls.name.value).toBe('Nombre actualizado');
    expect(component.hasPendingChanges()).toBe(true);
  });

  it('interpreta Escape o backdrop como cancelación y solo el descarte explícito como salida', async () => {
    const cancelSetup = await setup();
    markEditableField(cancelSetup.component);
    const cancelResult = firstValueFrom(cancelSetup.component.confirmPendingChanges());
    cancelSetup.dialogResult.next(undefined);
    cancelSetup.dialogResult.complete();
    await expect(cancelResult).resolves.toBe(false);

    TestBed.resetTestingModule();
    const discardSetup = await setup('Proyecto', 'P-005-2026');
    markEditableField(discardSetup.component);
    const discardResult = firstValueFrom(discardSetup.component.confirmPendingChanges());
    discardSetup.dialogResult.next(true);
    discardSetup.dialogResult.complete();
    await expect(discardResult).resolves.toBe(true);
    expect(discardSetup.dialog.open).toHaveBeenCalledWith(expect.any(Function), expect.objectContaining({
      data: { recordType: 'Proyecto', code: 'P-005-2026' },
    }));
  });

  it('conserva cambios locales tras 409, bloquea el reenvío y ofrece recarga explícita', async () => {
    const { component, repository, code } = await setup();
    const update = vi.spyOn(repository, 'updateInitiative').mockRejectedValue({ status: 409, message: 'Versión desactualizada' });
    const reload = vi.spyOn(repository, 'reloadPortfolioRecord').mockResolvedValue();
    markEditableField(component);

    await component.save();
    expect(component.conflict()).toBe(true);
    expect(component.form.controls.name.value).toBe('Nombre actualizado');
    expect(component.hasPendingChanges()).toBe(true);
    expect(update).toHaveBeenCalledOnce();

    await component.save();
    expect(update).toHaveBeenCalledOnce();
    await component.reloadLatest();
    expect(reload).toHaveBeenCalledWith('Iniciativa', code);
    expect(component.conflict()).toBe(false);
    expect(component.form.controls.name.value).toBe(repository.getInitiativeDetail(code)!.portfolioRecord.name);
  });

  it.each([
    [403, 'No tienes permisos para actualizar este registro. Tus cambios locales se conservaron.'],
    [404, 'El registro solicitado ya no existe o no está disponible. Tus cambios locales se conservaron.'],
    [422, 'La actualización no es válida.'],
  ] as const)('conserva la copia local y no navega después de HTTP %s', async (status, message) => {
    const { component, repository, router } = await setup();
    const update = vi.spyOn(repository, 'updateInitiative').mockRejectedValue(Object.assign(new Error(message), { status }));
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    markEditableField(component);

    await component.save();

    expect(update).toHaveBeenCalledOnce();
    expect(component.form.controls.name.value).toBe('Nombre actualizado');
    expect(component.hasPendingChanges()).toBe(true);
    expect(component.conflict()).toBe(false);
    expect(component.errorMessage()).toBe(message);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('mantiene opciones históricas visibles pero no seleccionables como nuevas referencias', async () => {
    const { component, fixture, repository } = await setup();
    repository.catalogs.update((state) => ({ ...state, value: {
      ...state.value,
      solutionTypes: [
        ...state.value.solutionTypes.map((option) => option.id === 1 ? { ...option, active: false } : option),
        { id: 99, code: 'HISTORICAL', name: 'Opción histórica adicional', displayOrder: 99, active: false },
      ],
    } }));
    fixture.detectChanges();

    expect(component.isHistorical('1', 'solutionTypeId')).toBe(true);
    const host = fixture.nativeElement as HTMLElement;
    const historical = Array.from(host.querySelectorAll<HTMLOptionElement>('select[formcontrolname="solutionTypeId"] option'))
      .find((option) => option.value === '1');
    expect(historical?.textContent).toContain('Histórico');
    expect(historical?.disabled).toBe(false);
    const unrelatedHistorical = Array.from(host.querySelectorAll<HTMLOptionElement>('select[formcontrolname="solutionTypeId"] option'))
      .find((option) => option.value === '99');
    expect(unrelatedHistorical?.disabled).toBe(true);
  });

  it('muestra un selector único y no renderiza el editor de orden', async () => {
    const { fixture, component, repository } = await setup();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('select[aria-label="Unidad Orgánica responsable"]')).not.toBeNull();
    expect(host.querySelectorAll('select').length).toBe(6);
    expect(host.querySelectorAll('input[type="checkbox"]').length).toBe(0);
    expect(host.textContent).not.toContain('Orden de presentación');
    expect(host.textContent).not.toContain('Opciones disponibles');
    expect(component.selectedResponsibleUnitId()).toBe('101');
    expect((host.querySelector('select[aria-label="Unidad Orgánica responsable"]') as HTMLSelectElement).value).toBe('101');

    repository.organizationalUnits.update((units) => units.map((unit) => unit.id === 101 ? { ...unit, active: false } : unit));
    fixture.detectChanges();
    const historicalOption = host.querySelector<HTMLOptionElement>('select[aria-label="Unidad Orgánica responsable"] option[value="101"]');
    expect(historicalOption?.disabled).toBe(true);
  });

  it('conserva varias UO históricas como contexto y no las envía al editar otro campo', async () => {
    const { fixture, component, repository, code } = await setup();
    const current = repository.getInitiativeDetail(code)!.portfolioRecord;
    const secondUnit = { id: 102, code: 'UO-102', name: 'Unidad histórica', acronym: 'UH', parentId: null, executingUnitId: 1, active: true };
    repository.organizationalUnits.set([...repository.organizationalUnits(), secondUnit]);
    repository.portfolioRecords.update((records) => records.map((record) => record.code === code
      ? { ...record, responsibleUnitReferences: [repository.organizationalUnits()[0], secondUnit] }
      : record));
    component.baseline.set({
      version: current.version ?? 0, name: current.name, solutionTypeId: current.solutionTypeReference?.id ?? null, sourceId: current.sourceReference?.id ?? null,
      startDate: current.startDate, responsible: current.responsible, peiObjectiveId: current.peiObjectiveReference?.id ?? null, poiActivityId: current.poiActivityReference?.id ?? null,
      responsibleUnitIds: [101, 102], description: current.description, keyResults: current.keyResults, note: current.note, digitalComponent: current.digitalComponent,
    });
    component.form.controls.responsibleUnitIds.setValue([101, 102]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('varias Unidades Orgánicas responsables históricas');
    expect(fixture.nativeElement.querySelector('input[type="checkbox"]')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Orden de presentación');

    const update = vi.spyOn(repository, 'updateInitiative').mockResolvedValue({ ...current, note: 'Nota editada', version: 1 });
    component.form.controls.note.setValue('Nota editada');
    component.form.controls.note.markAsDirty();
    await component.save();

    expect(update.mock.calls[0]?.[1]).not.toHaveProperty('responsibleUnitIds');
  });
});
