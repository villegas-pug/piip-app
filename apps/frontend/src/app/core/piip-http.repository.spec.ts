import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { PortfolioControllerService } from '../api/generated';
import type { PortfolioRecordResponse } from '../api/generated/models';
import { PiipHttpRepository } from './piip-http.repository';

describe('PiipHttpRepository', () => {
  let http: HttpTestingController;
  let repository: PiipHttpRepository;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), PiipHttpRepository] });
    http = TestBed.inject(HttpTestingController);
    repository = TestBed.inject(PiipHttpRepository);
  });

  afterEach(() => {
    localStorage.removeItem('piip-selected-executing-unit');
    vi.restoreAllMocks();
    http.verify();
  });

  it('starts empty and never exposes mock portfolio data', () => {
    expect(repository.portfolioRecords()).toEqual([]);
    expect(repository.initiatives()).toEqual([]);
    expect(repository.projects()).toEqual([]);

    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { title: 'Acceso denegado', detail: 'Usuario sin asignación local activa', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
  });

  it('surfaces backend ProblemDetail errors', async () => {
    const initialization = repository.initialize();
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { title: 'Acceso denegado', detail: 'Usuario sin asignación local activa', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );

    await initialization;
    expect(repository.lastError()).toBe('Usuario sin asignación local activa');
    expect(repository.role()).toBeNull();
  });

  it('carga el bundle de catálogos como JSON y conserva sus opciones', async () => {
    const initialization = repository.initialize();
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de preparación', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await initialization;

    const reload = repository.reloadCatalogs();
    const request = http.expectOne('http://127.0.0.1:4001/api/v1/catalogs');
    expect(request.request.responseType).toBe('json');
    request.flush({
      recordTypes: [{ code: 'INITIATIVE', name: 'Iniciativa', displayOrder: 0, active: true }],
      solutionTypes: [{ id: 10, code: 'GOODS', name: 'Bienes', displayOrder: 1, active: true }],
      sources: [{ id: 20, code: 'INTERNAL', name: 'Interna', displayOrder: 1, active: true }],
      peiObjectives: [],
      poiActivities: [],
      documentTypes: [],
    });
    await reload;

    expect(repository.catalogs()).toMatchObject({
      phase: 'ready',
      value: {
        solutionTypes: [{ id: 10, code: 'GOODS', name: 'Bienes' }],
        sources: [{ id: 20, code: 'INTERNAL', name: 'Interna' }],
      },
    });
  });

  it('keeps role and scope in the same grant when resolving permissions', () => {
    repository.executingUnits.set([
      { id: 1, code: 'UE-001', name: 'UE-001', institutionId: 1 },
      { id: 2, code: 'UE-002', name: 'UE-002', institutionId: 1 },
    ]);
    repository.currentUser.set({
      subject: 'mixed', fullName: 'Usuario mixto', email: 'mixed@example.pe',
      roleScopes: [
        { role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 },
        { role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 2 },
      ],
      roles: ['CONSULTA_EXTERNA', 'ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1, 2], institutionWide: false,
    });

    expect(repository.canReadExecutingUnit(1)).toBe(true);
    expect(repository.canAdministerExecutingUnit(1)).toBe(false);
    expect(repository.effectiveRoleForExecutingUnit(1)).toBe('Consulta externa');
    expect(repository.canAdministerExecutingUnit(2)).toBe(true);
    expect(repository.effectiveRoleForExecutingUnit(2)).toBe('Administrador PIIP');

    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de prueba', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
  });

  it('maps the generated administrable scope catalog without expanding operational executing units', async () => {
    const loading = repository.loadAdministrableScopes();
    http.expectOne('http://127.0.0.1:4001/api/v1/admin/users/administrable-scopes').flush([{
      institutionId: 1,
      institutionCode: 'MIDAGRI',
      institutionName: 'Ministerio de Desarrollo Agrario y Riego',
      institutionWideAllowed: true,
      executingUnits: [
        { id: 1, code: 'UE-001', name: 'Unidad Ejecutora 001' },
        { id: 2, code: 'UE-002', name: 'Unidad Ejecutora 002' },
      ],
    }]);
    await loading;

    expect(repository.administrableScopes()).toEqual([{
      institutionId: 1,
      institutionCode: 'MIDAGRI',
      institutionName: 'Ministerio de Desarrollo Agrario y Riego',
      institutionWideAllowed: true,
      executingUnits: [
        { id: 1, code: 'UE-001', name: 'Unidad Ejecutora 001' },
        { id: 2, code: 'UE-002', name: 'Unidad Ejecutora 002' },
      ],
    }]);
    expect(repository.executingUnits()).toEqual([]);

    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de prueba', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
  });

  it('rehydrates authorization and reconciles an invalid selected executing unit', async () => {
    const initialization = repository.initialize();
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Sin acceso durante la preparación', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await initialization;
    repository.selectedExecutingUnitId.set(2);
    localStorage.setItem('piip-selected-executing-unit', '2');
    const refreshAll = vi.spyOn(repository, 'refreshAll').mockResolvedValue();

    const refresh = repository.refreshAuthorizationContext();
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush({
      subject: 'current-user',
      fullName: 'Usuario actual',
      email: 'current@example.pe',
      roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }],
      roles: ['ADMINISTRADOR_PIIP'],
      institutionIds: [1],
      executingUnitIds: [1],
      institutionWide: false,
    });
    http.expectOne('http://127.0.0.1:4001/api/v1/executing-units').flush([
      { id: 1, code: 'UE-001', name: 'Unidad Ejecutora 001', institutionId: 1 },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.match((request) => request.url.includes('/organizational-units'))[0].flush([]);
    await refresh;

    expect(repository.currentUser()?.subject).toBe('current-user');
    expect(repository.executingUnits().map((unit) => unit.id)).toEqual([1]);
    expect(repository.selectedExecutingUnitId()).toBe(1);
    expect(localStorage.getItem('piip-selected-executing-unit')).toBe('1');
    expect(refreshAll).toHaveBeenCalledOnce();
  });

  it('preserves a still-authorized selected executing unit while rehydrating the header context', async () => {
    const initialization = repository.initialize();
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Sin acceso durante la preparación', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await initialization;
    repository.selectedExecutingUnitId.set(2);
    localStorage.setItem('piip-selected-executing-unit', '2');
    vi.spyOn(repository, 'refreshAll').mockResolvedValue();

    const refresh = repository.refreshAuthorizationContext();
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush({
      subject: 'current-user',
      fullName: 'Usuario actual',
      email: 'current@example.pe',
      roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 2 }],
      roles: ['ADMINISTRADOR_PIIP'],
      institutionIds: [1],
      executingUnitIds: [1, 2],
      institutionWide: false,
    });
    http.expectOne('http://127.0.0.1:4001/api/v1/executing-units').flush([
      { id: 1, code: 'UE-001', name: 'Unidad Ejecutora 001', institutionId: 1 },
      { id: 2, code: 'UE-002', name: 'Unidad Ejecutora 002', institutionId: 1 },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.match((request) => request.url.includes('/organizational-units'))[0].flush([]);
    await refresh;

    expect(repository.selectedExecutingUnitId()).toBe(2);
    expect(localStorage.getItem('piip-selected-executing-unit')).toBe('2');
    expect(repository.role()).toBe('Administrador PIIP');
  });

  it('envía la UE por ID, filtra activo/pertenencia y descarta la respuesta tardía', async () => {
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de preparación', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await repository.initialize();
    const adapter = repository as unknown as { loadOrganizationalUnits(executingUnitId: number): Promise<void> };

    const first = adapter.loadOrganizationalUnits(1);
    const firstRequest = http.expectOne((request) => request.url.endsWith('/organizational-units') && request.params.get('executingUnitId') === '1');
    const second = adapter.loadOrganizationalUnits(2);
    const secondRequest = http.expectOne((request) => request.url.endsWith('/organizational-units') && request.params.get('executingUnitId') === '2');
    secondRequest.flush([
      { id: 201, code: 'UO-201', name: 'Unidad vigente', acronym: 'UV', executingUnitId: 2, active: true },
      { id: 202, code: 'UO-202', name: 'Unidad inactiva', acronym: 'UI', executingUnitId: 2, active: false },
      { id: 101, code: 'UO-101', name: 'Unidad ajena', acronym: 'UA', executingUnitId: 1, active: true },
    ]);
    await second;
    firstRequest.flush([{ id: 100, code: 'UO-100', name: 'Respuesta tardía', acronym: 'UT', executingUnitId: 1, active: true }]);
    await first;

    expect(repository.organizationalUnits()).toEqual([
      expect.objectContaining({ id: 201, executingUnitId: 2, active: true }),
    ]);
  });

  it('filtra la bandeja documental por la UE activa aunque el portafolio aún esté vacío', async () => {
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de preparación', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await repository.initialize();
    repository.selectedExecutingUnitId.set(1);

    const load = (repository as unknown as { loadDocumentSummaries(): Promise<void> }).loadDocumentSummaries.call(repository);
    const request = http.expectOne((candidate) => candidate.url === 'http://127.0.0.1:4001/api/v1/documents' && candidate.params.get('executingUnitId') === '1');
    request.flush([
      { code: 'I-001-2026', name: 'UE 001', recordType: 'Iniciativa', status: 'Presentado', executingUnitId: 1, loadedCount: 0, pendingCount: 1 },
      { code: 'I-002-2026', name: 'UE 002', recordType: 'Iniciativa', status: 'Presentado', executingUnitId: 2, loadedCount: 0, pendingCount: 1 },
    ]);
    await load;

    expect(repository.getDocumentDossierSummaries().map((item) => item.code)).toEqual(['I-001-2026']);
  });

  it('no muestra eventos ni accesos de otra UE cuando la UE activa no tiene registros', async () => {
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de preparación', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await repository.initialize();
    repository.selectedExecutingUnitId.set(1);
    repository.portfolioRecords.set([]);

    const load = (repository as unknown as { loadAudit(): Promise<void> }).loadAudit.call(repository);
    const events = http.expectOne((candidate) => candidate.url === 'http://127.0.0.1:4001/api/v1/audit/events' && candidate.params.get('executingUnitId') === '1');
    events.flush([
      { entityCode: 'I-002-2026', event: 'INICIATIVA_REGISTRADA', actorName: 'UE 002' },
      { event: 'ROL_ASIGNADO', actorName: 'Administrador' },
    ]);
    const accesses = http.expectOne((candidate) => candidate.url === 'http://127.0.0.1:4001/api/v1/audit/accesses' && candidate.params.get('executingUnitId') === '1');
    accesses.flush([
      { recordCode: 'I-002-2026', status: 403, occurredAt: '2026-08-20T10:00:00Z' },
      { status: 200, occurredAt: '2026-08-20T10:01:00Z' },
    ]);
    await load;

    expect(repository.auditEvents().map((item) => item.recordCode)).toEqual([undefined]);
    expect(repository.auditAccesses().map((item) => item.recordCode)).toEqual([undefined]);
  });

  it('sends the contextual project transition with the cached version and refreshes the visible version', async () => {
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de prueba', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await repository.initialize();
    repository.executingUnits.set([{ id: 1, code: 'UE-001', name: 'UE-001', institutionId: 1 }]);
    repository.currentUser.set({
      subject: 'admin', fullName: 'Administrador', email: 'admin@example.pe',
      roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }],
      roles: ['ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1], institutionWide: false,
    });
    const record = { recordType: 'Proyecto', code: 'P-001-2026', originCode: 'NA', name: 'Proyecto', solutionType: 'No aplica', source: 'Otros', startDate: '2026-08-18', responsible: 'Responsable', peiObjective: '', poiActivity: '', responsibleUnits: '', description: 'Descripción', keyResults: '', note: '', status: 'Proyecto en ejecución', finalProductType: 'NA', digitalComponent: 'No', closingDate: '', technicalOpinionReport: '', formalApprovalDecision: '', finalProductApprovalDocument: '', projectManagementDocumentation: '', finalClosureReport: '', executingUnitId: 1 } as const;
    repository.portfolioRecords.set([record]);
    (repository as unknown as { recordVersions: Map<string, number> }).recordVersions.set(record.code, 3);
    vi.spyOn(repository, 'refreshAll').mockResolvedValue();

    const operation = repository.transitionProjectStatus({ projectCode: record.code, targetStatus: 'Producto aprobado', observation: 'Producto revisado' });
    const request = http.expectOne('http://127.0.0.1:4001/api/v1/projects/P-001-2026/status-transitions');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ version: 3, targetStatus: 'PRODUCT_APPROVED', observation: 'Producto revisado' });
    request.flush({ ...record,
      recordType: { code: 'PROJECT', name: 'Proyecto', displayOrder: 2, active: true },
      solutionType: { id: 3, code: 'NOT_APPLICABLE', name: 'No aplica', displayOrder: 3, active: true },
      source: { id: 14, code: 'OTHER', name: 'Otros', displayOrder: 5, active: true },
      peiObjective: null, poiActivity: null,
      responsibleUnits: [{ originalDesignation: 'Responsable', displayOrder: 1, organizationalUnit: { id: 10, code: 'UO-10', name: 'Responsable', acronym: 'UO', active: true, executingUnitId: 1 } }],
      status: 'Producto aprobado', version: 4, updatedAt: '2026-08-18T12:00:00Z' });
    await operation;

    expect((repository as unknown as { recordVersions: Map<string, number> }).recordVersions.get(record.code)).toBe(4);
  });

  it('keeps the loaded project visible and surfaces a reload message after HTTP 409', async () => {
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de prueba', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await repository.initialize();
    repository.executingUnits.set([{ id: 1, code: 'UE-001', name: 'UE-001', institutionId: 1 }]);
    repository.currentUser.set({
      subject: 'admin', fullName: 'Administrador', email: 'admin@example.pe',
      roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }],
      roles: ['ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1], institutionWide: false,
    });
    const record = { recordType: 'Proyecto', code: 'P-002-2026', originCode: 'NA', name: 'Proyecto', solutionType: 'No aplica', source: 'Otros', startDate: '2026-08-18', responsible: 'Responsable', peiObjective: '', poiActivity: '', responsibleUnits: '', description: 'Descripción', keyResults: '', note: '', status: 'Proyecto en ejecución', finalProductType: 'NA', digitalComponent: 'No', closingDate: '', technicalOpinionReport: '', formalApprovalDecision: '', finalProductApprovalDocument: '', projectManagementDocumentation: '', finalClosureReport: '', executingUnitId: 1 } as const;
    repository.portfolioRecords.set([record]);
    (repository as unknown as { recordVersions: Map<string, number> }).recordVersions.set(record.code, 1);
    const refresh = vi.spyOn(repository, 'refreshAll').mockResolvedValue();

    const operation = repository.transitionProjectStatus({ projectCode: record.code, targetStatus: 'Producto aprobado', observation: '' });
    http.expectOne('http://127.0.0.1:4001/api/v1/projects/P-002-2026/status-transitions').flush(
      { title: 'Conflicto de versión', detail: 'Recarga el expediente', status: 409 },
      { status: 409, statusText: 'Conflict' },
    );
    await expect(operation).rejects.toMatchObject({ status: 409, message: 'Recarga el expediente' });

    expect(repository.portfolioRecords()[0].status).toBe('Proyecto en ejecución');
    expect(refresh).not.toHaveBeenCalled();
    expect((repository as unknown as { recordVersions: Map<string, number> }).recordVersions.get(record.code)).toBe(1);
    expect(repository.lastError()).not.toBe('Fin de prueba');
  });

  it('refreshes audit events after successful initiative and project updates', async () => {
    const initialization = repository.initialize();
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de preparación', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await initialization;
    repository.currentUser.set({
      subject: 'admin', fullName: 'Administrador', email: 'admin@example.pe',
      roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }],
      roles: ['ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1], institutionWide: false,
    });
    repository.executingUnits.set([{ id: 1, code: 'UE-001', name: 'UE-001', institutionId: 1 }]);
    const response = {
      recordType: { code: 'INITIATIVE', name: 'Iniciativa', displayOrder: 1, active: true },
      code: 'I-006-2026', originCode: 'NA', name: 'Iniciativa actualizada',
      solutionType: { id: 1, code: 'SOLUTION', name: 'Solución potencial o adaptable', displayOrder: 1, active: true },
      source: { id: 2, code: 'SOURCE', name: 'Fuente', displayOrder: 1, active: true },
      startDate: '2026-08-01', responsible: 'Responsable', peiObjective: null, poiActivity: null,
      responsibleUnits: [], description: 'Descripción', keyResults: null, note: null, status: 'Presentado',
      finalProductType: 'NA', digitalComponent: 'No', closingDate: null, technicalOpinionReport: null,
      formalApprovalDecision: null, finalProductApprovalDocument: null, projectManagementDocumentation: null,
      finalClosureReport: null, executingUnitId: 1, executingUnit: '  Unidad Ejecutora de Prueba  ', updatedAt: '2026-08-22T10:00:00Z', version: 2,
    };
    const portfolio = TestBed.inject(PortfolioControllerService);
    vi.spyOn(portfolio, 'updateInitiative').mockReturnValue(of(response as unknown as PortfolioRecordResponse));
    const refreshAudit = vi.spyOn(repository as unknown as { loadAudit: () => Promise<void> }, 'loadAudit').mockResolvedValue();

    const updatedInitiative = await repository.updateInitiative('I-006-2026', { version: 1, name: 'Iniciativa actualizada' });
    expect(updatedInitiative.executingUnit).toBe('  Unidad Ejecutora de Prueba  ');
    expect(updatedInitiative.updatedAt).toBe('2026-08-22T10:00:00Z');
    expect(repository.portfolioRecords().find((record) => record.code === 'I-006-2026')?.executingUnit).toBe('  Unidad Ejecutora de Prueba  ');
    expect(repository.portfolioRecords().find((record) => record.code === 'I-006-2026')?.updatedAt).toBe('2026-08-22T10:00:00Z');
    expect(refreshAudit).toHaveBeenCalledOnce();

    const projectResponse = { ...response, recordType: { ...response.recordType, code: 'PROJECT', name: 'Proyecto' }, code: 'P-004-2026', originCode: 'NA', status: 'Proyecto en ejecución' };
    vi.spyOn(portfolio, 'updateProject').mockReturnValue(of(projectResponse as unknown as PortfolioRecordResponse));
    await repository.updateProject('P-004-2026', { version: 1, name: 'Proyecto actualizado' });
    expect(refreshAudit).toHaveBeenCalledTimes(2);
  });

  it('loads the unified home portfolio with UE, filters and five-row pagination', async () => {
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de prueba', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await repository.initialize();
    repository.selectedExecutingUnitId.set(10);

    const operation = repository.loadHomePortfolio({
      executingUnitId: 10,
      q: 'riego',
      type: 'Todos',
      status: 'Todos',
      page: 1,
      size: 5,
    });
    const request = http.expectOne((candidate) => candidate.url.endsWith('/dashboard/portfolio'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('executingUnitId')).toBe('10');
    expect(request.request.params.get('q')).toBe('riego');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('5');
    request.flush({
      content: [{ recordType: 'Iniciativa', code: 'I-001-2026', name: 'Riego', status: 'Presentado', executingUnitId: 10, executingUnit: 'UE-010', updatedAt: '2026-08-18T12:00:00Z' }],
      page: 1, size: 5, totalElements: 6, totalPages: 2, executingUnitTotalElements: 8,
      statusCounts: [{ status: 'Presentado', count: 4 }, { status: 'Iniciativa aprobada', count: 2 }],
    });
    await operation;

    expect(repository.homePortfolio().content[0].code).toBe('I-001-2026');
    expect(repository.homePortfolio().totalElements).toBe(6);
    expect(repository.homePortfolio().statusCounts.reduce((total, item) => total + item.count, 0)).toBe(6);
  });

  it('loads personal notifications, preserves failures and marks only the requested row', async () => {
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { detail: 'Fin de prueba', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
    await repository.initialize();
    const loading = repository.refreshNotifications();
    http.expectOne('http://127.0.0.1:4001/api/v1/notifications').flush([
      { id: 7, type: 'Nueva iniciativa', message: 'Aviso', read: false, createdAt: '2026-08-18T12:00:00Z' },
      { id: 8, type: 'Proyecto actualizado', message: 'Otro aviso', read: true, createdAt: '2026-08-18T11:00:00Z' },
    ]);
    await loading;
    expect(repository.notifications().filter((item) => !item.read)).toHaveLength(1);

    const mark = repository.markNotificationRead(7);
    const request = http.expectOne('http://127.0.0.1:4001/api/v1/notifications/7/read');
    expect(request.request.method).toBe('PUT');
    request.flush(null);
    await mark;
    expect(repository.notifications().find((item) => item.id === 7)?.read).toBe(true);

    const failed = repository.refreshNotifications();
    http.expectOne('http://127.0.0.1:4001/api/v1/notifications').flush(
      { detail: 'Notificaciones no disponibles', status: 503 },
      { status: 503, statusText: 'Service Unavailable' },
    );
    await failed;
    expect(repository.notificationsError()).toBe('Notificaciones no disponibles');
  });
});
