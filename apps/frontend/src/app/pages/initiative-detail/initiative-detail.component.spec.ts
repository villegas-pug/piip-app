import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { AuditEvent } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeApprovalDialogComponent } from './initiative-approval-dialog.component';
import { InitiativeDetailComponent } from './initiative-detail.component';
import { InitiativeStatusTransitionDialogComponent } from './initiative-status-transition-dialog.component';

describe('InitiativeDetailComponent', () => {
  let approvalAction = false;
  const open = vi.fn();
  const paramMap = convertToParamMap({ code: 'I-024-2026' });

  beforeEach(async () => {
    approvalAction = false;
    open.mockReset();
    open.mockReturnValue({ afterClosed: () => of(undefined) });
    await TestBed.configureTestingModule({
      imports: [InitiativeDetailComponent],
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatDialog, useValue: { open } },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
        { provide: ActivatedRoute, useValue: {
          paramMap: of(paramMap),
          snapshot: {
            paramMap,
            get queryParamMap() { return convertToParamMap(approvalAction ? { action: 'approve' } : {}); },
          },
        } },
      ],
    }).compileComponents();
  });

  it('abre el diálogo de aprobación desde la acción preservando contexto y accesibilidad', () => {
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;

    const approvalButton = Array.from(nativeElement.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Registrar aprobación'));
    if (!approvalButton) throw new Error('No se encontró el botón de aprobación.');
    approvalButton.click();

    expect(open).toHaveBeenCalledWith(InitiativeApprovalDialogComponent, expect.objectContaining({
      width: '600px',
      maxWidth: 'calc(100vw - 24px)',
      maxHeight: '90dvh',
      restoreFocus: true,
      panelClass: 'initiative-review-dialog-panel',
      backdropClass: 'initiative-review-dialog-backdrop',
      data: expect.objectContaining({ initiativeCode: 'I-024-2026', currentStatus: 'Presentado' }),
    }));
  });

  it('conserves the approval action and the real initiative status before derivation', () => {
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;

    expect(nativeElement.textContent).toContain('Presentado');
    expect(nativeElement.textContent).toContain('Registrar aprobación');
    expect(nativeElement.textContent).not.toContain('Crear proyecto');
  });

  it('opens the approval dialog when action=approve is present in the URL', () => {
    approvalAction = true;
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();

    expect(open).toHaveBeenCalledWith(InitiativeApprovalDialogComponent, expect.any(Object));
  });

  it('hides approval when Administrator and record coverage come from different grants', () => {
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
    repository.selectedExecutingUnitId.set(2);
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.canAdministerRecord()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Registrar aprobación');
  });

  it('hides status controls and explains the lock when a derived project is linked', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.projects.update((projects) => [
      { ...projects[0], originCode: 'I-024-2026', originMode: 'DERIVED_FROM_INITIATIVE' },
      ...projects.slice(1),
    ]);
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;

    expect(nativeElement.textContent).toContain('Iniciativa aprobada');
    expect(nativeElement.textContent).toContain('acciones de cambio de estado están bloqueadas');
    expect(nativeElement.querySelector('.hero-actions button')).toBeNull();
    expect(nativeElement.textContent).not.toContain('Registrar aprobación');
  });

  it('resume los eventos más recientes y prioriza el nombre documental', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const receivedEvents: AuditEvent[] = [
      {
        recordCode: 'I-024-2026', timestamp: '20/05/2026\n10:28:19', event: 'DOCUMENTO_CARGADO', user: 'Ana Analista', email: 'ana@midagri.gob.pe',
        observation: '{"tipoCodigo":"INITIATIVE_TECHNICAL_OPINION","tipoNombre":"Informe de opinión técnica de evaluación de iniciativa","version":1}', rawDetail: '{"tipoCodigo":"INITIATIVE_TECHNICAL_OPINION","tipoNombre":"Informe de opinión técnica de evaluación de iniciativa","version":1}', icon: 'cloud_upload',
      },
      {
        recordCode: 'I-024-2026', timestamp: '20/05/2026\n09:31:12', event: 'INICIATIVA_REGISTRADA', user: 'Ana Analista', email: 'ana@midagri.gob.pe',
        observation: '{"estado":"Presentado"}', rawDetail: '{"estado":"Presentado"}', icon: 'add',
      },
    ];
    repository.auditEvents.set(receivedEvents);
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();

    expect(repository.auditEvents()).toEqual(receivedEvents);
    expect(fixture.componentInstance.timeline().map((event) => event.source.event)).toEqual([
      'DOCUMENTO_CARGADO',
      'INICIATIVA_REGISTRADA',
    ]);
    expect(fixture.componentInstance.recentTimeline()).toHaveLength(2);
    const timeline = fixture.nativeElement.querySelector('.timeline') as HTMLElement;
    expect(timeline.querySelectorAll('.activity-item')).toHaveLength(2);
    expect(timeline.textContent).toContain('Iniciativa registrada');
    expect(timeline.textContent).toContain('Estado inicial: Presentado.');
    expect(timeline.textContent).toContain('Documento cargado');
    expect(timeline.textContent).toContain('Se cargó Informe de opinión técnica de evaluación de iniciativa, versión 1.');
    expect(timeline.textContent).toContain('Informe_tecnico_I-024-2026.pdf');
    expect(timeline.textContent).not.toContain('INITIATIVE_TECHNICAL_OPINION');
    expect(fixture.nativeElement.querySelector('.activity-link')?.getAttribute('href')).toBe('/auditoria?record=I-024-2026');
  });

  it('ofrece solo los destinos de iniciativa en un diálogo genérico', () => {
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.initiativeTransitionOptions()).toEqual(['Iniciativa archivada', 'No Admisible']);
    fixture.componentInstance.openStatusDialog();

    expect(open).toHaveBeenCalledWith(InitiativeStatusTransitionDialogComponent, expect.objectContaining({
      data: {
        initiativeCode: 'I-024-2026',
        currentStatus: 'Presentado',
        options: ['Iniciativa archivada', 'No Admisible'],
      },
    }));
  });

  it('conserva archivo como único destino de una iniciativa aprobada sin proyecto', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.initiatives.update((items) => items.map((item) => item.code === 'I-024-2026' ? { ...item, status: 'Iniciativa aprobada' } : item));
    repository.portfolioRecords.update((items) => items.map((item) => item.code === 'I-024-2026' ? { ...item, status: 'Iniciativa aprobada' } : item));
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.initiativeTransitionOptions()).toEqual(['Iniciativa archivada']);
    fixture.componentInstance.openStatusDialog();
    expect(open).toHaveBeenCalledWith(InitiativeStatusTransitionDialogComponent, expect.objectContaining({
      data: {
        initiativeCode: 'I-024-2026',
        currentStatus: 'Iniciativa aprobada',
        options: ['Iniciativa archivada'],
      },
    }));
  });

  it('presenta estado, fecha y Unidad Ejecutora en formato humano', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([{ id: 1, code: 'UE-001', name: 'Unidad Ejecutora Demo', institutionId: 1 }]);
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Unidad Ejecutora Demo');
    expect(text).toContain('20/05/2026');
    expect(fixture.nativeElement.querySelector('.status-tag')?.getAttribute('data-tone')).toBe('pending');
    expect(fixture.nativeElement.querySelector('.status-tag mat-icon')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('muestra código, nombre y estado activo de las referencias PEI y POI', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.portfolioRecords.update((records) => records.map((record) => record.code === 'I-024-2026' ? {
      ...record,
      peiObjectiveReference: { id: 20, code: 'PEI-01', name: 'Objetivo PEI vigente', displayOrder: 1, active: true },
      poiActivityReference: { id: 30, code: 'POI-01', name: 'Actividad POI histórica', displayOrder: 1, active: false },
    } : record));
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('PEI-01 — Objetivo PEI vigente');
    expect(text).toContain('POI-01 — Actividad POI histórica');
    expect(text).toContain('Inactivo');
  });
});
