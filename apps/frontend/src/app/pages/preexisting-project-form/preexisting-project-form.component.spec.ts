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
      solutionType: 'No aplica',
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
});
