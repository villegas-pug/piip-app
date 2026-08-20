import { convertToParamMap } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { DerivedProjectFormComponent } from './derived-project-form.component';

describe('DerivedProjectFormComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DerivedProjectFormComponent],
      providers: [
        provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
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

  it('habilita la revisión solo después de reemplazar una referencia inactiva', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const record = repository.portfolioRecords().find((item) => item.code === 'I-019-2026')!;
    record.sourceReference = { id: 99, code: 'LEGACY', name: 'Fuente histórica', displayOrder: 1, active: false };
    const fixture = TestBed.createComponent(DerivedProjectFormComponent);
    const component = fixture.componentInstance;
    component.form.patchValue({ startDate: '2026-08-20', responsibleUnits: '101' });

    component.openReview();
    expect(component.reviewOpen()).toBe(false);

    component.form.controls.source.setValue(String(repository.catalogs().value.sources[0].id));
    component.openReview();
    expect(component.reviewOpen()).toBe(true);
  });
});
