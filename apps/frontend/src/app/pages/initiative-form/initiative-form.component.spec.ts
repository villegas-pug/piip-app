import { TestBed } from '@angular/core/testing';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeFormComponent } from './initiative-form.component';
import { InitiativeReviewDialogComponent } from './initiative-review-dialog.component';

describe('InitiativeFormComponent', () => {
  const open = vi.fn();
  const block = vi.fn(() => ({}));

  beforeEach(async () => {
    open.mockReset();
    block.mockClear();
    open.mockReturnValue({});
    await TestBed.configureTestingModule({ imports: [InitiativeFormComponent], providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: { open } }, { provide: Overlay, useValue: { scrollStrategies: { block } } }] }).compileComponents();
  });

  it('starts invalid and uses Presentado only as the official submission state', () => {
    const fixture = TestBed.createComponent(InitiativeFormComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.invalid).toBe(true);
    expect(fixture.componentInstance.form.controls.status.value).toBe('Presentado');
    expect(fixture.componentInstance.form.controls.code.value).toBe('Se asignará al registrar');
    expect(fixture.nativeElement.textContent).not.toContain('I-025-2026');
    expect(fixture.nativeElement.textContent).toContain('El borrador es solo una condición local de la UI');
  });

  it('prevents duplicate initiative registrations while one is pending', async () => {
    const repository = TestBed.inject(PIIP_REPOSITORY);
    let releaseRegistration!: (record: ReturnType<typeof repository.portfolioRecords>[number]) => void;
    const pendingRegistration = new Promise<ReturnType<typeof repository.portfolioRecords>[number]>((resolve) => {
      releaseRegistration = resolve;
    });
    const registerInitiative = vi.spyOn(repository, 'registerInitiative').mockReturnValue(pendingRegistration);
    const fixture = TestBed.createComponent(InitiativeFormComponent);

    const first = fixture.componentInstance.registerInitiative();
    const duplicate = fixture.componentInstance.registerInitiative();

    expect(registerInitiative).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.submitting()).toBe(true);
    releaseRegistration(repository.portfolioRecords()[0]);
    await Promise.all([first, duplicate]);
    expect(fixture.componentInstance.submitting()).toBe(false);
  });

  it('opens the final review in a global CDK dialog only when the form is valid', () => {
    const fixture = TestBed.createComponent(InitiativeFormComponent);
    const component = fixture.componentInstance;
    component.form.patchValue({
      startDate: '2026-08-14', name: 'Iniciativa', solutionType: '2', source: '10',
      digitalComponent: 'Si', description: 'Descripción', responsible: 'Responsable', responsibleUnits: '101',
    });
    component.uploadedFilename.set('ficha.pdf');

    component.openReview();

    expect(open).toHaveBeenCalledWith(InitiativeReviewDialogComponent, expect.objectContaining({
      autoFocus: 'first-heading', panelClass: 'initiative-review-dialog-panel', backdropClass: 'initiative-review-dialog-backdrop',
    }));
    expect(block).toHaveBeenCalledOnce();
  });

  it('renders accessible inline errors for the required initiative fields', () => {
    const fixture = TestBed.createComponent(InitiativeFormComponent);
    fixture.componentInstance.form.markAllAsTouched();
    fixture.detectChanges();

    for (const errorId of ['initiative-name-error', 'initiative-description-error', 'initiative-responsible-error', 'initiative-responsible-unit-error', 'initiative-file-error']) {
      expect(fixture.nativeElement.querySelector(`#${errorId}`)?.getAttribute('role')).toBe('alert');
    }
    expect(fixture.nativeElement.querySelector('[formControlName="name"]')?.getAttribute('aria-describedby')).toBe('initiative-name-error');
    expect(fixture.nativeElement.querySelector('[formControlName="description"]')?.getAttribute('aria-describedby')).toBe('initiative-description-error');
  });

  it('muestra carga, vacío y error de catálogos con reintento', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const reload = vi.spyOn(repository, 'reloadCatalogs');
    const fixture = TestBed.createComponent(InitiativeFormComponent);

    repository.catalogs.set({ phase: 'loading', value: repository.catalogs().value, error: null, requestId: 2 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Cargando opciones');

    repository.catalogs.set({ phase: 'ready', value: { ...repository.catalogs().value, solutionTypes: [] }, error: null, requestId: 3 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay opciones disponibles');

    repository.catalogs.set({ phase: 'error', value: repository.catalogs().value, error: 'Catálogos no disponibles', requestId: 4 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Catálogos no disponibles');
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find((button) => button.textContent?.includes('Reintentar'))?.click();
    expect(reload).toHaveBeenCalled();
  });

  it('sincroniza el disabled del formulario con el estado de catálogos', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(InitiativeFormComponent);
    repository.catalogs.set({ phase: 'loading', value: repository.catalogs().value, error: null, requestId: 5 });
    fixture.detectChanges();
    expect(fixture.componentInstance.form.controls.solutionType.disabled).toBe(true);
    expect(fixture.componentInstance.form.controls.source.disabled).toBe(true);

    repository.catalogs.set({ phase: 'ready', value: repository.catalogs().value, error: null, requestId: 6 });
    fixture.detectChanges();
    expect(fixture.componentInstance.form.controls.solutionType.enabled).toBe(true);
    expect(fixture.componentInstance.form.controls.source.enabled).toBe(true);
  });

  it('preserva y envía las selecciones por ID', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const register = vi.spyOn(repository, 'registerInitiative').mockResolvedValue(repository.portfolioRecords()[0]);
    const fixture = TestBed.createComponent(InitiativeFormComponent);
    fixture.componentInstance.form.patchValue({
      startDate: '2026-08-20', name: 'Iniciativa por ID', solutionType: '2', source: '10', digitalComponent: 'Si',
      description: 'Descripción', responsible: 'Responsable', responsibleUnits: '101', peiObjective: '20', poiActivity: '30',
    });
    fixture.componentInstance.uploadedFilename.set('ficha.pdf');
    repository.catalogs.update((state) => ({
      ...state,
      value: { ...state.value, sources: state.value.sources.map((item) => item.id === 10 ? { ...item, name: 'Fuente renombrada' } : item) },
    }));
    fixture.detectChanges();

    expect(fixture.componentInstance.form.controls.source.value).toBe('10');

    await fixture.componentInstance.registerInitiative();

    expect(register).toHaveBeenCalledWith(expect.objectContaining({
      solutionTypeId: 2, sourceId: 10, organizationalUnitId: 101, peiObjectiveId: 20, poiActivityId: 30,
    }));
  });
});
