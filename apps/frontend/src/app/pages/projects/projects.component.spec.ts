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

  it('shows only project states in the status filter and keeps the list consultive', () => {
    const fixture = TestBed.createComponent(ProjectsComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;
    const options = Array.from(nativeElement.querySelectorAll('select[formcontrolname="status"] option'))
      .map((option) => option.textContent?.trim());

    expect(options).toEqual(expect.arrayContaining(['Proyecto en ejecución', 'Producto aprobado', 'Producto no aprobado', 'Suspendido', 'Cancelado', 'Finalizado']));
    expect(options).not.toEqual(expect.arrayContaining(['Presentado', 'Iniciativa aprobada', 'Iniciativa archivada', 'No Admisible', 'No Aplicable']));
    expect(nativeElement.textContent).not.toContain('Cambiar estado');
  });

  it('renders project states with the dashboard icon, tone and accessible text pattern', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const template = repository.projects()[0];
    const expected = [
      ['Proyecto en ejecución', 'play_circle', 'progress'],
      ['Producto aprobado', 'check_circle', 'success'],
      ['Producto no aprobado', 'cancel', 'danger'],
      ['Suspendido', 'pause_circle', 'warning'],
      ['Cancelado', 'cancel', 'danger'],
      ['Finalizado', 'check_circle', 'success'],
    ] as const;
    repository.projects.set(expected.map(([status], index) => ({ ...template, code: `P-STATUS-${index}-2026`, status })));

    const fixture = TestBed.createComponent(ProjectsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const nativeElement = fixture.nativeElement as HTMLElement;
    const firstPageTags = Array.from(nativeElement.querySelectorAll<HTMLElement>('.status-tag'));

    expect(firstPageTags).toHaveLength(5);
    expected.slice(0, 5).forEach(([status, icon, tone], index) => {
      const tag = firstPageTags[index]!;
      expect(tag.getAttribute('data-tone')).toBe(tone);
      expect(tag.textContent).toContain(status);
      expect(tag.querySelector('mat-icon')?.getAttribute('aria-hidden')).toBe('true');
      expect(tag.querySelector('mat-icon')?.textContent?.trim()).toBe(icon);
    });

    component.pageIndex.set(1);
    fixture.detectChanges();
    const lastTag = nativeElement.querySelector<HTMLElement>('.status-tag');
    expect(lastTag?.getAttribute('data-tone')).toBe('success');
    expect(lastTag?.textContent).toContain('Finalizado');
    expect(component.statusVisual('Estado desconocido')).toEqual({ icon: 'circle', tone: 'neutral' });
  });

  it('opens the general project detail and keeps documents as a separate menu option', () => {
    const fixture = TestBed.createComponent(ProjectsComponent);
    fixture.detectChanges();
    const openLink = fixture.nativeElement.querySelector('tbody tr a.secondary-button') as HTMLAnchorElement;
    expect(openLink.getAttribute('href')).toBe('/proyectos/P-003-2026');
  });

  it('filtra por la identidad resuelta de Unidad Orgánica y conserva los filtros existentes', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const target = repository.projects()[0];
    const fixture = TestBed.createComponent(ProjectsComponent);
    fixture.componentInstance.filters.patchValue({ unit: String(target.organizationalUnits?.[0]?.id) });

    expect(fixture.componentInstance.filteredProjects().map((item) => item.code)).toContain(target.code);
    expect(Object.keys(fixture.componentInstance.filters.getRawValue()).sort()).toEqual(['digital', 'search', 'status', 'unit']);
  });

  it('presenta carga, vacío y error de Unidades Orgánicas', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(ProjectsComponent);
    repository.organizationalUnitsState.set({ phase: 'loading', value: [], error: null, requestId: 2 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Cargando Unidades Orgánicas');

    repository.organizationalUnits.set([]);
    repository.organizationalUnitsState.set({ phase: 'ready', value: [], error: null, requestId: 3 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay Unidades Orgánicas activas');

    repository.organizationalUnitsState.set({ phase: 'error', value: [], error: 'Unidades no disponibles', requestId: 4 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Unidades no disponibles');
  });

  it('mantiene el listado consultivo: detalle, documentos y ciclo de vida fuera de edición inline', () => {
    const fixture = TestBed.createComponent(ProjectsComponent);
    fixture.detectChanges();
    const row = fixture.nativeElement.querySelector('tbody tr') as HTMLElement;
    const detailLink = row.querySelector('a.secondary-button') as HTMLAnchorElement;

    expect(detailLink?.getAttribute('href')).toBe('/proyectos/P-003-2026');
    expect(row.querySelector('button[aria-label="Acciones de P-003-2026"]')).not.toBeNull();
    expect(row.querySelector('a[href*="/editar"]')).toBeNull();
    expect(row.textContent).not.toContain('Editar');
    expect(row.textContent).toContain('Proyecto en ejecución');
  });
});
