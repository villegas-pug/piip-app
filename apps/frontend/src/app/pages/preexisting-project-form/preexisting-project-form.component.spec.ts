import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PreexistingProjectFormComponent } from './preexisting-project-form.component';

describe('PreexistingProjectFormComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreexistingProjectFormComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }],
    }).compileComponents();
  });

  it('fixes the origin values and starts with the document markers from Leyenda', () => {
    const fixture = TestBed.createComponent(PreexistingProjectFormComponent);
    fixture.detectChanges();
    const form = fixture.componentInstance.form;

    expect(form.controls.recordType.disabled).toBe(true);
    expect(form.controls.originCode.disabled).toBe(true);
    expect(form.controls.solutionType.disabled).toBe(true);
    expect(form.controls.status.disabled).toBe(true);
    expect(form.getRawValue()).toEqual(expect.objectContaining({
      recordType: 'Proyecto',
      originCode: 'NA',
      solutionType: 'Definido por el backend',
      status: 'Proyecto en ejecución',
      technicalOpinionMode: 'NOT_APPLICABLE',
      formalApprovalMode: 'NOT_APPLICABLE',
      finalClosureMode: 'NOT_APPLICABLE',
    }));
  });

  it('renders restricted access for the external consultation profile', () => {
    const fixture = TestBed.createComponent(PreexistingProjectFormComponent);
    fixture.componentInstance.repository.toggleRole();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Acceso restringido');
    expect(fixture.nativeElement.textContent).not.toContain('Revisar registro');
  });

  it('usa IDs canónicos y conserva la identidad de proyecto preexistente', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const register = vi.spyOn(repository, 'registerPreexistingProject').mockResolvedValue(repository.portfolioRecords().find((item) => item.code === 'P-005-2026')!);
    const fixture = TestBed.createComponent(PreexistingProjectFormComponent);
    fixture.componentInstance.form.patchValue({
      startDate: '2026-08-20', name: 'Proyecto preexistente', source: '10', responsible: 'Responsable',
      responsibleUnits: '101', peiObjective: '20', poiActivity: '30', description: 'Descripción', digitalComponent: 'No',
    });

    await fixture.componentInstance.registerProject();

    expect(fixture.componentInstance.form.getRawValue()).toEqual(expect.objectContaining({ recordType: 'Proyecto', originCode: 'NA' }));
    expect(register).toHaveBeenCalledWith(expect.objectContaining({ sourceId: 10, organizationalUnitId: 101, peiObjectiveId: 20, poiActivityId: 30 }));
  });

  it('presenta vacío y error de Unidades Orgánicas sin fallback local', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(PreexistingProjectFormComponent);
    repository.organizationalUnits.set([]);
    repository.organizationalUnitsState.set({ phase: 'ready', value: [], error: null, requestId: 2 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay Unidades Orgánicas disponibles');

    repository.organizationalUnitsState.set({ phase: 'error', value: [], error: 'Unidades no disponibles', requestId: 3 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Unidades no disponibles');
  });
});
