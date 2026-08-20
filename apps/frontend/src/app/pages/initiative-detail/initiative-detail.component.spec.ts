import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { AuditEvent, PiipPortfolioRecord } from '../../core/piip.models';
import { PiipRepository } from '../../core/piip.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeDetailComponent } from './initiative-detail.component';

describe('InitiativeDetailComponent', () => {
  let approvalAction = false;
  const paramMap = convertToParamMap({ code: 'I-024-2026' });

  beforeEach(async () => {
    approvalAction = false;
    await TestBed.configureTestingModule({
      imports: [InitiativeDetailComponent],
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
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

  it('opens the approval dialog from the action button and closes it', () => {
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;

    const approvalButton = Array.from(nativeElement.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Registrar aprobación'));
    if (!approvalButton) throw new Error('No se encontró el botón de aprobación.');
    approvalButton.click();
    fixture.detectChanges();

    expect(nativeElement.querySelector('[role="dialog"]')).not.toBeNull();

    nativeElement.querySelector<HTMLButtonElement>('[aria-label="Cerrar"]')?.click();
    fixture.detectChanges();

    expect(nativeElement.querySelector('[role="dialog"]')).toBeNull();
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
    const nativeElement = fixture.nativeElement as HTMLElement;

    expect(nativeElement.querySelector('[role="dialog"]')).not.toBeNull();
  });

  it('shows the approval submission state until the operation completes', async () => {
    const mockRepository = TestBed.inject(PiipMockRepository);
    const repository = TestBed.inject(PIIP_REPOSITORY) as PiipRepository;
    const approvedRecord = mockRepository.portfolioRecords().find((record) => record.code === 'I-024-2026');
    if (!approvedRecord) throw new Error('No se encontró la iniciativa de prueba.');

    let resolveApproval!: (record: PiipPortfolioRecord) => void;
    const pendingApproval = new Promise<PiipPortfolioRecord>((resolve) => { resolveApproval = resolve; });
    vi.spyOn(repository, 'approveInitiative').mockReturnValue(pendingApproval);

    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;
    const approvalButton = Array.from(nativeElement.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Registrar aprobación'));
    if (!approvalButton) throw new Error('No se encontró el botón de aprobación.');
    approvalButton.click();
    fixture.detectChanges();

    const confirmButton = Array.from(nativeElement.querySelectorAll<HTMLButtonElement>('.approval-dialog footer button'))
      .find((button) => button.textContent?.includes('Confirmar aprobación'));
    if (!confirmButton) throw new Error('No se encontró el botón de confirmación.');
    confirmButton.click();
    fixture.detectChanges();

    const approvalDialog = nativeElement.querySelector<HTMLElement>('.approval-dialog');
    expect(approvalDialog?.getAttribute('aria-busy')).toBe('true');
    expect(approvalDialog?.textContent).toContain('Registrando...');
    expect(approvalDialog?.querySelector('mat-icon.button-spinner')?.textContent?.trim()).toBe('progress_activity');
    expect(confirmButton.disabled).toBe(true);
    expect(nativeElement.querySelector<HTMLButtonElement>('[aria-label="Cerrar"]')?.disabled).toBe(true);
    expect(Array.from(nativeElement.querySelectorAll<HTMLButtonElement>('.approval-dialog footer button'))
      .find((button) => button.textContent?.includes('Cancelar'))?.disabled).toBe(true);

    resolveApproval(approvedRecord);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(nativeElement.querySelector('.approval-dialog')).toBeNull();
    expect(nativeElement.querySelector('.success-dialog')).not.toBeNull();
    expect(nativeElement.textContent).toContain('Iniciativa aprobada');
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

  it('preserves the received descending events and renders their presented copy chronologically', () => {
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
    expect(fixture.componentInstance.descendingAuditEvents()).toEqual(receivedEvents);
    expect(fixture.componentInstance.timeline().map((event) => event.source.event)).toEqual([
      'INICIATIVA_REGISTRADA',
      'DOCUMENTO_CARGADO',
    ]);
    const timeline = fixture.nativeElement.querySelector('.timeline') as HTMLElement;
    expect(timeline.textContent).toContain('Iniciativa registrada');
    expect(timeline.textContent).toContain('Estado inicial: Presentado.');
    expect(timeline.textContent).toContain('Documento cargado');
    expect(timeline.textContent).toContain('Se cargó Informe de opinión técnica de evaluación de iniciativa, versión 1.');
    expect(timeline.textContent).not.toContain('INITIATIVE_TECHNICAL_OPINION');
  });

  it('offers only archive and inadmissible actions for an unlinked presented initiative', () => {
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.initiativeTransitionOptions()).toEqual(['Iniciativa archivada', 'No Admisible']);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Iniciativa archivada');
    expect(text).toContain('No Admisible');
    expect(text).not.toContain('Producto aprobado');
    expect(text).not.toContain('No Aplicable');
  });

  it('allows archiving an approved unlinked initiative but no terminal action afterwards', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.initiatives.update((items) => items.map((item) => item.code === 'I-024-2026' ? { ...item, status: 'Iniciativa aprobada' } : item));
    repository.portfolioRecords.update((items) => items.map((item) => item.code === 'I-024-2026' ? { ...item, status: 'Iniciativa aprobada' } : item));
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.initiativeTransitionOptions()).toEqual(['Iniciativa archivada']);
    await fixture.componentInstance.transitionStatus();
    expect(repository.getInitiativeDetail('I-024-2026')?.initiative.status).toBe('Iniciativa aprobada');
    fixture.componentInstance.openStatusTransition('Iniciativa archivada');
    await fixture.componentInstance.transitionStatus();
    expect(repository.getInitiativeDetail('I-024-2026')?.initiative.status).toBe('Iniciativa archivada');
    expect(fixture.componentInstance.initiativeTransitionOptions()).toEqual([]);
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
