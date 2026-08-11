import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { ProjectsComponent } from './projects.component';

describe('ProjectsComponent', () => {
  const open = vi.fn();

  beforeEach(async () => {
    open.mockReset();
    await TestBed.configureTestingModule({
      imports: [ProjectsComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: { open } }],
    }).compileComponents();
  });

  it('opens the configured dialog and navigates to the selected initiative', async () => {
    open.mockReturnValue({ afterClosed: () => of({ mode: 'DERIVED_FROM_INITIATIVE', initiativeCode: 'I-019-2026' }) });
    const fixture = TestBed.createComponent(ProjectsComponent);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.openProjectRegistration('initiative-selection');
    await Promise.resolve();

    expect(open).toHaveBeenCalledWith(expect.any(Function), expect.objectContaining({ data: { initialView: 'initiative-selection' } }));
    expect(navigate).toHaveBeenCalledWith(['/proyectos/nuevo/derivado', 'I-019-2026']);
  });

  it('navigates to the preexisting form from the registration dialog', async () => {
    open.mockReturnValue({ afterClosed: () => of({ mode: 'PREEXISTING' }) });
    const fixture = TestBed.createComponent(ProjectsComponent);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.openProjectRegistration('type-selection');
    await Promise.resolve();

    expect(navigate).toHaveBeenCalledWith(['/proyectos/nuevo/preexistente']);
  });

  it('does not open registration for the external consultation profile', () => {
    const fixture = TestBed.createComponent(ProjectsComponent);
    TestBed.inject(PiipMockRepository).toggleRole();

    fixture.componentInstance.openProjectRegistration('type-selection');

    expect(open).not.toHaveBeenCalled();
  });

  it('does not combine an Administrator grant from another UE for project creation', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 1, code: 'UE-001', name: 'UE-001', institutionId: 1 },
      { id: 2, code: 'UE-002', name: 'UE-002', institutionId: 1 },
    ]);
    repository.currentUser.set({
      subject: 'mixed', fullName: 'Usuario mixto', email: 'mixed@example.pe',
      roleScopes: [
        { role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 },
        { role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 2 },
      ], roles: ['CONSULTA_EXTERNA', 'ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1, 2], institutionWide: false,
    });
    repository.selectedExecutingUnitId.set(1);
    const fixture = TestBed.createComponent(ProjectsComponent);

    fixture.componentInstance.openProjectRegistration('type-selection');

    expect(open).not.toHaveBeenCalled();
  });

  it('shows five projects per page and resets when a filter changes', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const template = repository.projects()[0];
    repository.projects.set(Array.from({ length: 6 }, (_, index) => ({ ...template, code: `P-10${index}-2026`, name: `Proyecto ${index + 1}` })));
    const fixture = TestBed.createComponent(ProjectsComponent);
    const component = fixture.componentInstance;

    expect(component.pagedProjects()).toHaveLength(5);
    component.pageIndex.set(1);
    expect(component.pagedProjects()).toHaveLength(1);

    component.filters.patchValue({ search: 'P-100-2026' });
    expect(component.currentPage()).toBe(0);
  });
});
