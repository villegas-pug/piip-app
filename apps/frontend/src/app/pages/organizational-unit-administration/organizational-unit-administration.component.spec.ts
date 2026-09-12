import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { OrganizationalUnitAdministrationComponent } from './organizational-unit-administration.component';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';

describe('OrganizationalUnitAdministrationComponent', () => {
  let fixture: ComponentFixture<OrganizationalUnitAdministrationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizationalUnitAdministrationComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: { open: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ executingUnitId: '1' }) } } }],
    }).compileComponents();
    fixture = TestBed.createComponent(OrganizationalUnitAdministrationComponent);
    await fixture.whenStable();
  });

  it('keeps the UE context in the route-driven administrative view and excludes legacy parent data', () => {
    const component = fixture.componentInstance;
    expect(component.executingUnit()?.id).toBe(1);
    expect(component.units()[0]).not.toHaveProperty('parentId');
  });
});
