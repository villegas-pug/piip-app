import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { OrganizationalUnitFormComponent } from './organizational-unit-form.component';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';

describe('OrganizationalUnitFormComponent', () => {
  let fixture: ComponentFixture<OrganizationalUnitFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizationalUnitFormComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ executingUnitId: '1' }) } } }],
    }).compileComponents();
    fixture = TestBed.createComponent(OrganizationalUnitFormComponent);
    await fixture.whenStable();
  });

  it('requires name and acronym and sends an explicit boolean state on alta', () => {
    const component = fixture.componentInstance;
    component.form.controls.name.setValue('Unidad nueva');
    component.form.controls.acronym.setValue('UN');
    component.form.controls.active.setValue(false);
    expect(component.form.valid).toBe(true);
    expect(component.form.getRawValue().active).toBe(false);
    expect(component.form.get('code')).not.toBeNull();
  });
});
