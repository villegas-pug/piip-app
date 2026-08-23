import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { ProjectDetailComponent } from './project-detail.component';

describe('ProjectDetailComponent', () => {
  const open = vi.fn();

  beforeEach(async () => {
    open.mockReset();
    await TestBed.configureTestingModule({
      imports: [ProjectDetailComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: { open } }, { provide: MatSnackBar, useValue: { open: vi.fn() } }, { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ code: 'P-005-2026' })), snapshot: { paramMap: convertToParamMap({ code: 'P-005-2026' }) } } }],
    }).compileComponents();
  });

  it('exposes only contextual project destinations', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    expect(fixture.componentInstance.transitionOptions()).toEqual(['Producto aprobado', 'Producto no aprobado', 'Suspendido', 'Cancelado']);
    expect(fixture.componentInstance.transitionOptions()).not.toContain('Iniciativa aprobada');
    expect(fixture.componentInstance.transitionOptions()).not.toContain('No Aplicable');
  });

  it('does not expose destinations for a terminal project', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.projects.update((projects) => projects.map((project) => project.code === 'P-005-2026' ? { ...project, status: 'Finalizado' } : project));
    repository.portfolioRecords.update((records) => records.map((record) => record.code === 'P-005-2026' ? { ...record, status: 'Finalizado', closingDate: '2026-08-18' } : record));
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    expect(fixture.componentInstance.transitionOptions()).toEqual([]);
  });

  it('muestra referencias PEI y POI resueltas e identifica las históricas inactivas', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.portfolioRecords.update((records) => records.map((record) => record.code === 'P-005-2026' ? {
      ...record,
      peiObjectiveReference: { id: 20, code: 'PEI-02', name: 'Objetivo del proyecto', displayOrder: 1, active: true },
      poiActivityReference: { id: 30, code: 'POI-02', name: 'Actividad anterior', displayOrder: 1, active: false },
    } : record));
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('PEI-02 — Objetivo del proyecto');
    expect(text).toContain('POI-02 — Actividad anterior');
    expect(text).toContain('Inactivo');
  });

  it('presenta el estado, fecha y Unidad Ejecutora en formato humano', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([{ id: 1, code: 'UE-001', name: 'Unidad Ejecutora Demo', institutionId: 1 }]);
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Unidad Ejecutora Demo');
    expect(text).toContain('12/02/2026');
    expect(fixture.nativeElement.querySelector('.status-tag')?.getAttribute('data-tone')).toBe('progress');
    expect(fixture.nativeElement.querySelector('.status-tag mat-icon')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('abre un diálogo con los destinos permitidos y restaura el flujo al cerrarlo', async () => {
    open.mockReturnValue({ afterClosed: () => of({ targetStatus: 'Producto aprobado' }) });
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    fixture.componentInstance.openStatusDialog();
    await Promise.resolve();

    expect(open).toHaveBeenCalledWith(expect.any(Function), expect.objectContaining({
      maxWidth: 'calc(100vw - 24px)',
      maxHeight: '90dvh',
      restoreFocus: true,
      data: expect.objectContaining({
        projectCode: 'P-005-2026',
        currentStatus: 'Proyecto en ejecución',
        options: ['Producto aprobado', 'Producto no aprobado', 'Suspendido', 'Cancelado'],
      }),
    }));
  });

  it('resume solo los tres eventos más recientes y conserva el enlace al historial', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.auditEvents.set(Array.from({ length: 5 }, (_, index) => ({
      recordCode: 'P-005-2026', timestamp: `20/08/26, 8:${20 + index} a. m.`, event: index === 0 ? 'ESTADO_PROYECTO_CAMBIADO' : 'DOCUMENTO_CARGADO',
      user: 'Administrador PIIP', email: 'admin@example.pe', observation: `Evento ${index}`, icon: 'history',
    })));
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.recentTimeline()).toHaveLength(3);
    expect(fixture.nativeElement.querySelectorAll('.activity-item')).toHaveLength(3);
    expect(fixture.nativeElement.querySelector('.activity-link')?.getAttribute('href')).toBe('/auditoria?record=P-005-2026');
  });

  it('prioriza el nombre del archivo documental y conserva el evento como metadata', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.auditEvents.set([{
      recordCode: 'P-005-2026', timestamp: '20/08/2026, 8:20 a. m.', event: 'DOCUMENTO_CARGADO',
      user: 'Administrador PIIP', email: 'admin@example.pe',
      rawDetail: JSON.stringify({ tipoCodigo: 'PROJECT_MANAGEMENT_DOCUMENTATION', tipoNombre: 'Documentación de la gestión del proyecto', version: 1 }),
      observation: '', documentName: 'Gestion_Proyecto_P-005-2026.pdf', icon: 'history',
    }]);
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Gestion_Proyecto_P-005-2026.pdf');
    expect(text).toContain('Documento cargado');
  });

  it('muestra edición solo al Administrador PIIP con cobertura de la UE del proyecto', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    fixture.detectChanges();
    const edit = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a'))
      .find((link) => link.textContent?.includes('Editar'));
    expect(edit?.getAttribute('href')).toBe('/proyectos/P-005-2026/editar');

    const repository = TestBed.inject(PiipMockRepository);
    repository.currentUser.set({
      subject: 'external', fullName: 'Consulta', email: 'consulta@example.pe',
      roleScopes: [{ role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 }],
      roles: ['CONSULTA_EXTERNA'], institutionIds: [1], executingUnitIds: [1], institutionWide: false,
    });
    fixture.detectChanges();
    expect(Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a')).some((link) => link.textContent?.includes('Editar'))).toBe(false);
  });

  it('oculta edición para un proyecto fuera de la UE activa o en estado no editable', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([{ id: 1, code: 'UE-001', name: 'UE 001', institutionId: 1 }, { id: 2, code: 'UE-002', name: 'UE 002', institutionId: 1 }]);
    repository.portfolioRecords.update((records) => records.map((record) => record.code === 'P-005-2026' ? { ...record, executingUnitId: 2 } : record));
    repository.projects.update((projects) => projects.map((project) => project.code === 'P-005-2026' ? { ...project, executingUnitId: 2 } : project));
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.canEditRecord()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Editar');

    repository.portfolioRecords.update((records) => records.map((record) => record.code === 'P-005-2026' ? { ...record, executingUnitId: 1, status: 'Finalizado' } : record));
    repository.projects.update((projects) => projects.map((project) => project.code === 'P-005-2026' ? { ...project, executingUnitId: 1, status: 'Finalizado' } : project));
    fixture.detectChanges();
    expect(fixture.componentInstance.canEditRecord()).toBe(false);
  });
});
