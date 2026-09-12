import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { ExecutingUnitAdministrationComponent } from './executing-unit-administration.component';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';

describe('ExecutingUnitAdministrationComponent', () => {
  let fixture: ComponentFixture<ExecutingUnitAdministrationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExecutingUnitAdministrationComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: { open: vi.fn() } }],
    }).compileComponents();
    fixture = TestBed.createComponent(ExecutingUnitAdministrationComponent);
    await fixture.whenStable();
  });

  it('shows the institution context and active/inactive administrative list without the global UE', () => {
    const component = fixture.componentInstance;
    component.repository.selectedExecutingUnitId.set(null);
    expect(component.selectedInstitution()?.id).toBe(1);
    expect(component.units().every((unit) => unit.institution.id === 1)).toBe(true);
  });
});
