import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
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
    request.flush({ ...record, responsibleUnits: ['Responsable'], status: 'Producto aprobado', version: 4, updatedAt: '2026-08-18T12:00:00Z' });
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
    expect(repository.lastError()).not.toBe('Fin de prueba');
  });
});
