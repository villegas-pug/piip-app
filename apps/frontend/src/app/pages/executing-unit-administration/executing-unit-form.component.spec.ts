import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { ExecutingUnitFormComponent } from './executing-unit-form.component';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';

describe('ExecutingUnitFormComponent', () => {
  let fixture: ComponentFixture<ExecutingUnitFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExecutingUnitFormComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}), queryParamMap: convertToParamMap({ institutionId: '1' }) } } }],
    }).compileComponents();
    fixture = TestBed.createComponent(ExecutingUnitFormComponent);
    await fixture.whenStable();
  });

  it('keeps institution and generated fields readonly and displayOrder optional on create', () => {
    const component = fixture.componentInstance;
    expect(component.form.controls.institution.disabled).toBe(true);
    expect(component.form.controls.code.disabled).toBe(true);
    expect(component.form.controls.displayOrder.value).toBeNull();
  });
});
