import { convertToParamMap } from '@angular/router';
import { Overlay } from '@angular/cdk/overlay';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { NEVER } from 'rxjs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { DerivedProjectFormComponent } from './derived-project-form.component';
import { DerivedProjectReviewDialogComponent, DerivedProjectReviewDialogData } from './derived-project-review-dialog.component';

describe('DerivedProjectFormComponent', () => {
  const open = vi.fn();
  const block = vi.fn(() => ({}));

  beforeEach(async () => {
    open.mockReset();
    block.mockClear();
    open.mockReturnValue({ afterClosed: () => NEVER });
    await TestBed.configureTestingModule({
      imports: [DerivedProjectFormComponent],
      providers: [
        provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatDialog, useValue: { open } },
        { provide: Overlay, useValue: { scrollStrategies: { block } } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ initiativeCode: 'I-019-2026' }) } } },
      ],
    }).compileComponents();
  });

  it('muestra la referencia heredada inactiva y exige reemplazarla', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const record = repository.portfolioRecords().find((item) => item.code === 'I-019-2026')!;
    record.solutionTypeReference = { id: 99, code: 'LEGACY', name: 'Opción histórica', displayOrder: 1, active: false };

    const fixture = TestBed.createComponent(DerivedProjectFormComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.solutionType.value).toBe('');
    expect(fixture.componentInstance.form.controls.solutionType.invalid).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Opción histórica');
    expect(fixture.nativeElement.textContent).toContain('La opción seleccionada ya no está disponible. Elige una opción vigente.');
  });

  it('precarga por ID las referencias heredadas activas', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const record = repository.portfolioRecords().find((item) => item.code === 'I-019-2026')!;
    expect(record.solutionTypeReference?.active).toBe(true);
    expect(record.sourceReference?.active).toBe(true);

    const fixture = TestBed.createComponent(DerivedProjectFormComponent);

    expect(fixture.componentInstance.form.controls.solutionType.value).toBe(String(record.solutionTypeReference?.id));
    expect(fixture.componentInstance.form.controls.source.value).toBe(String(record.sourceReference?.id));
  });

  it('reacciona cuando el detalle de la iniciativa llega después de crear el formulario', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const initiative = repository.initiatives().find((item) => item.code === 'I-019-2026')!;
    const portfolioRecord = repository.portfolioRecords().find((item) => item.code === 'I-019-2026')!;
    repository.initiatives.set(repository.initiatives().filter((item) => item.code !== initiative.code));
    repository.portfolioRecords.set(repository.portfolioRecords().filter((item) => item.code !== portfolioRecord.code));

    const fixture = TestBed.createComponent(DerivedProjectFormComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Iniciativa no encontrada');

    repository.initiatives.set([...repository.initiatives(), initiative]);
    repository.portfolioRecords.set([...repository.portfolioRecords(), portfolioRecord]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Crear proyecto desde Fortalecimiento de capacidades para la gestión de la innovación agraria');
    expect(fixture.componentInstance.form.controls.name.value).toBe(portfolioRecord.name);
    expect(fixture.componentInstance.responsibleUnitIds()).toEqual(portfolioRecord.responsibleUnitReferences?.map((unit) => unit.id));
  });

  it('habilita la revisión solo después de reemplazar una referencia inactiva', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const record = repository.portfolioRecords().find((item) => item.code === 'I-019-2026')!;
    record.sourceReference = { id: 99, code: 'LEGACY', name: 'Fuente histórica', displayOrder: 1, active: false };
    const fixture = TestBed.createComponent(DerivedProjectFormComponent);
    const component = fixture.componentInstance;
    component.form.patchValue({ startDate: '2026-08-20' });
    component.onUnitsChange({ unitIds: [101], rowCount: 1, hasPendingSelection: false });

    component.openReview();
    expect(open).not.toHaveBeenCalled();

    component.form.controls.source.setValue(String(repository.catalogs().value.sources[0].id));
    component.openReview();
    expect(open).toHaveBeenCalledOnce();
  });

  it('abre una sola revisión Material con etiquetas resueltas y sin IDs técnicos', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DerivedProjectFormComponent);
    const component = fixture.componentInstance;
    const solutionType = repository.catalogs().value.solutionTypes[0];
    const source = repository.catalogs().value.sources[0];
    const unit = repository.organizationalUnits()[0];
    component.form.patchValue({
      startDate: '2026-08-20',
      name: 'Proyecto de trazabilidad',
      solutionType: String(solutionType.id),
      source: String(source.id),
      digitalComponent: 'Si',
      responsible: 'Responsable PIIP',
      description: 'Descripción para la revisión',
      keyResults: '',
    });
    component.onUnitsChange({ unitIds: [unit.id], rowCount: 1, hasPendingSelection: false });

    component.openReview();
    component.openReview();

    expect(open).toHaveBeenCalledOnce();
    const dialogComponent = open.mock.calls[0][0];
    const config = open.mock.calls[0][1] as { data: DerivedProjectReviewDialogData; [key: string]: unknown };
    expect(dialogComponent).toBe(DerivedProjectReviewDialogComponent);
    expect(config).toEqual(expect.objectContaining({
      width: '680px',
      autoFocus: 'first-heading',
      restoreFocus: true,
      disableClose: false,
      panelClass: 'derived-project-review-dialog-panel',
    }));
    expect(config.data).toEqual(expect.objectContaining({
      initiativeName: 'Fortalecimiento de capacidades para la gestión de la innovación agraria',
      solutionType: solutionType.name,
      source: source.name,
      organizationalUnits: [unit],
      startDate: expect.stringContaining('2026'),
      keyResults: '',
    }));
    expect(config.data.organizationalUnits).toEqual([unit]);
    expect(block).toHaveBeenCalledOnce();
  });

  it('mantiene el diálogo abierto cuando falla el registro', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    vi.spyOn(repository, 'registerDerivedProject').mockRejectedValue(new Error('Registro no disponible'));
    const fixture = TestBed.createComponent(DerivedProjectFormComponent);

    await expect(fixture.componentInstance.registerProject()).resolves.toBe(false);
    expect(fixture.componentInstance.submitting()).toBe(false);
  });

  it('bloquea revisión y registro si PROJECT_IN_PROGRESS no está disponible', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DerivedProjectFormComponent);
    repository.catalogs.set({ phase: 'ready', value: { ...repository.catalogs().value, portfolioStatuses: repository.catalogs().value.portfolioStatuses.filter((item) => item.code !== 'PROJECT_IN_PROGRESS') }, error: null, requestId: 2 });
    fixture.componentInstance.openReview();
    await expect(fixture.componentInstance.registerProject()).resolves.toBe(false);
    expect(open).not.toHaveBeenCalled();
  });

  it('precarga en orden las unidades de la iniciativa y permite reportar una fila backend', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const origin = repository.portfolioRecords().find((item) => item.code === 'I-019-2026')!;
    const second = { id: 102, code: 'UO-102', name: 'Unidad dos', acronym: 'UD', parentId: null, executingUnitId: 1, active: true };
    repository.organizationalUnits.set([...repository.organizationalUnits(), second]);
    origin.responsibleUnitReferences = [repository.organizationalUnits()[0], second];
    const fixture = TestBed.createComponent(DerivedProjectFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.responsibleUnitIds()).toEqual([101, 102]);
    vi.spyOn(repository, 'registerDerivedProject').mockRejectedValue(new (await import('../../core/piip-http.repository')).PiipApiError(422, 'Unidad inválida.', undefined, 'responsibleUnits[1]'));
    await component.registerProject();
    expect(component.responsibleUnitErrors()).toEqual({ 0: 'Unidad inválida.' });
  });
});
