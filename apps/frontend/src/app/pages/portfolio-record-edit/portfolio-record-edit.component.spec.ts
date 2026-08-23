import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import type { PiipPortfolioRecord, PiipRecordType } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PortfolioRecordEditComponent } from './portfolio-record-edit.component';

describe('PortfolioRecordEditComponent', () => {
  async function setup(recordType: PiipRecordType = 'Iniciativa', code = 'I-024-2026') {
    const paramMap = convertToParamMap({ code });
    await TestBed.configureTestingModule({
      imports: [PortfolioRecordEditComponent],
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
        { provide: ActivatedRoute, useValue: {
          data: of({ recordType }),
          paramMap: of(paramMap),
          snapshot: { data: { recordType }, paramMap },
        } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PortfolioRecordEditComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return {
      fixture,
      component: fixture.componentInstance,
      repository: TestBed.inject(PiipMockRepository),
      router: TestBed.inject(Router),
      code,
    };
  }

  function markEditableField(component: PortfolioRecordEditComponent, value = 'Nombre actualizado'): void {
    component.form.controls.name.setValue(value);
    component.form.controls.name.markAsDirty();
  }

  it('carga la variante iniciativa y envía un body sparse con confirmación y navegación al detalle', async () => {
    const { component, repository, router, code } = await setup();
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
    expect(component.form.pristine).toBe(true);
    expect(navigate).toHaveBeenCalledWith(['/iniciativas', code], { queryParams: { updated: '1' } });
  });

  it('carga la variante proyecto y conserva keyResults en el PATCH ordenado', async () => {
    const { component, repository, router, code } = await setup('Proyecto', 'P-005-2026');
    const current = repository.getProjectDetail(code)!.portfolioRecord;
    repository.organizationalUnits.set([
      ...repository.organizationalUnits(),
      { id: 102, code: 'UO-102', name: 'Unidad secundaria', acronym: 'US', parentId: null, executingUnitId: 1, active: true },
    ]);
    const updated = { ...current, keyResults: 'Resultado actualizado', responsibleUnitReferences: [repository.organizationalUnits()[0], repository.organizationalUnits()[1]], version: 1 } as PiipPortfolioRecord;
    const update = vi.spyOn(repository, 'updateProject').mockResolvedValue(updated);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.form.controls.keyResults.setValue('Resultado actualizado');
    component.form.controls.keyResults.markAsDirty();
    component.setResponsibleUnitIds([101, 102]);
    await component.save();

    expect(update).toHaveBeenCalledWith(code, expect.objectContaining({ keyResults: 'Resultado actualizado', responsibleUnitIds: [101, 102] }));
  });

  it('no genera PATCH cuando solo permanece la versión baseline', async () => {
    const { component, repository } = await setup();
    const update = vi.spyOn(repository, 'updateInitiative');

    await component.save();

    expect(update).not.toHaveBeenCalled();
    expect(component.errorMessage()).toBe('No hay cambios efectivos para guardar.');
  });

  it('envía referencias opcionales como null y permite conservar el orden de UO', async () => {
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
});
