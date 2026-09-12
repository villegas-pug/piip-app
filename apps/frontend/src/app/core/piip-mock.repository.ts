import { Injectable, computed, signal } from '@angular/core';
import {
  AdministrativeExecutingUnit,
  AdministrativeInstitution,
  AdministrativeOrganizationalUnit,
  AdministrableScope,
  AuditEvent,
  AuditAccess,
  CurrentUser,
  DerivedProjectInput,
  DocumentDossier,
  DocumentDossierSummary,
  DocumentFile, DocumentRecord, DocumentVersion,
  InitiativeDecisionInput,
  InitiativeDetail,
  InitiativeStatusTransitionInput,
  InitiativeInput,
  InitiativeUpdateInput,
  InitiativeRecord,
  PiipPortfolioRecord,
  PreexistingProjectInput,
  ProjectOrigin,
  ProjectRecord,
  ProjectDetail,
  ProjectStatusTransitionInput,
  ProjectUpdateInput,
  PiipRecordType,
  UserRole,
  WorkItem,
  HomePortfolioQuery, HomePortfolioResult, HomePortfolioItem, HomePortfolioStatusCount, NotificationItem, PiipStatus,
  PortfolioStatusReference, PortfolioStatusOption, CatalogBundle, OrganizationalUnit, ResourceState,
  AssignmentMutationInput, AssignmentMutationResult, UserAdministrationSnapshot,
  UserAdministrationUser, UserAssignmentCandidate, UserAssignmentScope,
  CreateAdministrativeExecutingUnitInput, UpdateAdministrativeExecutingUnitInput,
  CreateAdministrativeOrganizationalUnitInput, UpdateAdministrativeOrganizationalUnitInput,
} from './piip.models';
import { INITIATIVE_STATUS_TRANSITIONS, PROJECT_STATUS_TRANSITIONS, type InitiativeStatus, type ProjectStatus } from './piip.catalogs';
import { PiipRepository } from './piip.repository';

function emptyHomePortfolio(): HomePortfolioResult {
  return { content: [], page: 0, size: 5, totalElements: 0, totalPages: 0, executingUnitTotalElements: 0, statusCounts: [] };
}

function mockRepositoryError(status: number, message: string): Error & { status: number } {
  const error = new Error(message) as Error & { status: number };
  error.status = status;
  return error;
}

@Injectable({ providedIn: 'root' })
export class PiipMockRepository extends PiipRepository {
  readonly demoMode: boolean = true;
  readonly currentUser = signal<CurrentUser | null>({ subject: 'demo-admin', fullName: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }], roles: ['ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1], institutionWide: false });
  readonly executingUnits = signal([{ id: 1, code: 'UE-DEMO', name: 'Unidad Ejecutora de demostracion', institutionId: 1 }]);
  readonly administrableScopes = signal<AdministrableScope[]>([{
    institutionId: 1,
    institutionCode: 'INST-DEMO',
    institutionName: 'Institución de demostración',
    institutionWideAllowed: true,
    executingUnits: [{ id: 1, code: 'UE-DEMO', name: 'Unidad Ejecutora de demostración' }],
  }]);
  readonly administrativeInstitutions = signal<AdministrativeInstitution[]>([
    { id: 1, code: 'INST-DEMO', name: 'Institución de demostración' },
  ]);
  readonly administrativeExecutingUnits = signal<AdministrativeExecutingUnit[]>([
    {
      id: 1,
      code: 'UE-DEMO',
      name: 'Unidad Ejecutora de demostración',
      active: true,
      displayOrder: 0,
      registeredAt: '2026-01-01T00:00:00Z',
      activatedAt: '2026-01-01T00:00:00Z',
      version: 0,
      institution: { id: 1, code: 'INST-DEMO', name: 'Institución de demostración' },
    },
  ]);
  readonly administrativeOrganizationalUnits = signal<AdministrativeOrganizationalUnit[]>([
    {
      id: 101,
      code: 'UO-DEMO',
      name: 'Unidad Orgánica de demostración',
      acronym: 'UO',
      active: true,
      version: 0,
      executingUnit: { id: 1, code: 'UE-DEMO', name: 'Unidad Ejecutora de demostración' },
    },
  ]);
  readonly catalogs = signal<ResourceState<CatalogBundle>>({ phase: 'ready', value: mockCatalogBundle(), error: null, requestId: 1 });
  readonly organizationalUnits = signal<OrganizationalUnit[]>([
    { id: 101, code: 'UO-DEMO', name: 'Unidad Orgánica de demostración', acronym: 'UO', parentId: null, executingUnitId: 1, active: true },
  ]);
  readonly organizationalUnitsState = signal<ResourceState<OrganizationalUnit[]>>({ phase: 'ready', value: this.organizationalUnits(), error: null, requestId: 1 });
  readonly selectedExecutingUnitId = signal<number | null>(1);
  readonly role = computed(() => this.effectiveRoleForExecutingUnit(this.selectedExecutingUnitId()));
  readonly loading = signal(false);
  readonly lastError = signal<string | null>(null);
  readonly documentDossierSummaries = signal<DocumentDossierSummary[]>([]);
  readonly auditAccesses = signal<AuditAccess[]>([]);
  readonly userAdministrationUsers = signal<UserAdministrationUser[]>([
    {
      id: 1,
      subject: 'demo-admin',
      fullName: 'Administrador PIIP',
      email: 'admin.piip@midagri.gob.pe',
      scopes: [{
        id: 1,
        role: 'ADMINISTRADOR_PIIP',
        institutionId: 1,
        institution: 'Institución de demostración',
        executingUnitId: 1,
        executingUnit: 'Unidad Ejecutora de demostración',
        active: true,
        version: 0,
      }],
    },
  ]);
  readonly assignmentCandidates = signal<UserAssignmentCandidate[]>([
    { id: 2, subject: 'demo-consulta', fullName: 'Usuario de consulta', email: 'consulta.piip@midagri.gob.pe' },
  ]);
  readonly notifications = signal<NotificationItem[]>([
    { id: 1, type: 'Nueva iniciativa registrada', message: 'Se ha registrado una iniciativa para revisión.', read: false, createdAt: new Date().toISOString() },
    { id: 2, type: 'Proyecto actualizado', message: 'El proyecto de demostración cambió de estado.', read: false, createdAt: new Date(Date.now() - 3600000).toISOString() },
    { id: 3, type: 'Iniciativa archivada', message: 'Una iniciativa fue archivada.', read: true, createdAt: new Date(Date.now() - 7200000).toISOString() },
    { id: 4, type: 'Recordatorio', message: 'Tienes un aviso pendiente de revisión.', read: false, createdAt: new Date(Date.now() - 10800000).toISOString() },
  ]);
  readonly dashboardSummary = signal({ initiatives: 3, projects: 8, alerts: 2, pendingTasks: 2, notifications: 1, portfolioStatusCounts: [] });
  readonly homePortfolio = signal<HomePortfolioResult>(emptyHomePortfolio());
  readonly homePortfolioLoading = signal(false);
  readonly homePortfolioError = signal<string | null>(null);
  readonly notificationsLoading = signal(false);
  readonly notificationsError = signal<string | null>(null);

  readonly portfolioRecords = signal<PiipPortfolioRecord[]>(([
    {
      recordType: 'Iniciativa', code: 'I-024-2026', originCode: 'NA',
      name: 'Mejoramiento del servicio de riego tecnificado en el valle de Ica',
      solutionType: 'Solución potencial o adaptable', source: 'Ficha de iniciativa de innovación pública',
      startDate: '2026-05-20', responsible: 'María López', peiObjective: 'Objetivo PEI declarado en el registro',
      poiActivity: 'Actividad POI declarada en el registro', responsibleUnits: 'DGIA',
      description: 'Necesidad de mejorar el acceso al riego tecnificado.', keyResults: '', note: '',
      status: mockStatus('PRESENTED'), finalProductType: 'NA', digitalComponent: 'No', closingDate: '',
      technicalOpinionReport: 'Informe_Opinion_I-024-2026.pdf', formalApprovalDecision: '',
      finalProductApprovalDocument: '', projectManagementDocumentation: '', finalClosureReport: '',
      executingUnitId: 1,
    },
    {
      recordType: 'Iniciativa', code: 'I-019-2026', originCode: 'NA',
      name: 'Fortalecimiento de capacidades para la gestión de la innovación agraria',
      solutionType: 'Solución por definir', source: 'Innovación abierta', startDate: '2026-05-06',
      responsible: 'Carlos Rojas', peiObjective: 'Fortalecer la gestión institucional de la innovación agraria',
      poiActivity: 'Desarrollo de capacidades institucionales', responsibleUnits: 'DIPNA',
      description: 'Fortalecimiento de capacidades para gestionar iniciativas de innovación agraria.',
      keyResults: '', note: '', status: mockStatus('INITIATIVE_APPROVED'), finalProductType: 'NA', digitalComponent: 'No',
      closingDate: '', technicalOpinionReport: 'Informe_Opinion_I-019-2026.pdf',
      formalApprovalDecision: 'Decision_I-019-2026.pdf', finalProductApprovalDocument: '',
      projectManagementDocumentation: '', finalClosureReport: '',
      executingUnitId: 1,
    },
    {
      recordType: 'Iniciativa', code: 'I-014-2026', originCode: 'NA',
      name: 'Adquisición de equipamiento para la estación experimental agraria Santa Ana',
      solutionType: 'Solución potencial o adaptable', source: 'Propuesta de jefatura o directivos',
      startDate: '2026-05-02', responsible: 'Lucía Fernández',
      peiObjective: 'Objetivo PEI declarado en el registro', poiActivity: 'Actividad POI declarada en el registro',
      responsibleUnits: 'DGA', description: 'Necesidad de equipamiento para la estación experimental agraria.',
      keyResults: '', note: 'Iniciativa archivada con comentarios.', status: mockStatus('INITIATIVE_ARCHIVED'),
      finalProductType: 'NA', digitalComponent: 'No', closingDate: '', technicalOpinionReport: '',
      formalApprovalDecision: '', finalProductApprovalDocument: '', projectManagementDocumentation: '',
      finalClosureReport: '',
      executingUnitId: 1,
    },
    {
      recordType: 'Proyecto', code: 'P-005-2026', originCode: 'NA',
      name: 'Red de Estaciones Agrometeorológicas', solutionType: 'No aplica', source: 'Otros',
      startDate: '2026-02-12', responsible: 'Carmen Rojas', peiObjective: 'Objetivo PEI declarado en el registro',
      poiActivity: 'Actividad POI declarada en el registro', responsibleUnits: 'DCLIMA',
      description: 'Proyecto preexistente registrado sin iniciativa formal de origen.', keyResults: '',
      note: 'Proyecto preexistente de demostración.', status: mockStatus('PROJECT_IN_PROGRESS'), finalProductType: 'NA',
      digitalComponent: 'Si', closingDate: '', technicalOpinionReport: 'No Aplica', formalApprovalDecision: 'No Aplica',
      finalProductApprovalDocument: '', projectManagementDocumentation: '', finalClosureReport: '',
      executingUnitId: 1,
    },
  ] satisfies PiipPortfolioRecord[]).map(enrichMockRecord));

  readonly workItems = signal<WorkItem[]>([
    { id: 1, code: 'I-024-2026', action: 'Registrar decision', priority: 'Alta', assignedTo: 'DGIA', dueDate: '27/05/2026', alert: 'PROXIMA', version: 0 },
    { id: 2, code: 'I-019-2026', action: 'Revisar informe tecnico', priority: 'Media', assignedTo: 'DIPNA', dueDate: '30/05/2026', alert: 'EN_PLAZO', version: 0 },
  ]);

  readonly initiatives = signal<InitiativeRecord[]>(([
    {
      code: 'I-024-2026',
      name: 'Mejoramiento del servicio de riego tecnificado en el valle de Ica',
      source: 'Ficha de iniciativa de innovación pública',
      responsible: 'María López',
      role: 'Analista de Inversiones',
      unit: 'DGIA',
      status: mockStatus('PRESENTED'),
      updatedAt: '20/05/2026 10:15',
      executingUnitId: 1,
    },
    {
      code: 'I-019-2026',
      name: 'Fortalecimiento de capacidades para la gestión de la innovación agraria',
      source: 'Innovación abierta',
      responsible: 'Carlos Rojas',
      role: 'Especialista en Innovación',
      unit: 'DIPNA',
      status: mockStatus('INITIATIVE_APPROVED'),
      updatedAt: '18/05/2026 16:45',
      executingUnitId: 1,
    },
    {
      code: 'I-014-2026',
      name: 'Adquisición de equipamiento para la estación experimental agraria Santa Ana',
      source: 'Propuesta de jefatura o directivos',
      responsible: 'Lucía Fernández',
      role: 'Analista de Adquisiciones',
      unit: 'DGA',
      status: mockStatus('INITIATIVE_ARCHIVED'),
      updatedAt: '15/05/2026 09:30',
      executingUnitId: 1,
    },
  ] satisfies InitiativeRecord[]).map(enrichMockInitiative));

  readonly projects = signal<ProjectRecord[]>(([
    { code: 'P-003-2026', name: 'Plataforma de Innovación Agraria Sostenible', originCode: 'I-012-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'DIPNA', responsible: 'María Quintana', status: mockStatus('PROJECT_IN_PROGRESS'), digitalComponent: 'Si' },
    { code: 'P-004-2026', name: 'Sistema de Información de Riego', originCode: 'I-010-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'DGA', responsible: 'Luis Calderón', status: mockStatus('PRODUCT_APPROVED'), digitalComponent: 'Si' },
    { code: 'P-005-2026', name: 'Red de Estaciones Agrometeorológicas', originCode: 'NA', originMode: 'PREEXISTING', unit: 'DCLIMA', responsible: 'Carmen Rojas', status: mockStatus('PROJECT_IN_PROGRESS'), digitalComponent: 'Si' },
    { code: 'P-006-2026', name: 'Capacitación Digital para Productores', originCode: 'I-008-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'DIPNA', responsible: 'José Vílchez', status: mockStatus('SUSPENDED'), digitalComponent: 'No' },
    { code: 'P-007-2026', name: 'Trazabilidad de Productos Agrarios', originCode: 'I-003-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'DGESEP', responsible: 'Ana Lucía Prado', status: mockStatus('PRODUCT_APPROVED'), digitalComponent: 'Si' },
    { code: 'P-008-2026', name: 'Gestión de Suelos Degradados', originCode: 'NA', originMode: 'PREEXISTING', unit: 'DGIA', responsible: 'Miguel Torres', status: mockStatus('PROJECT_IN_PROGRESS'), digitalComponent: 'No' },
    { code: 'P-009-2026', name: 'Sanidad Vegetal con Monitoreo Digital', originCode: 'I-011-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'SENASA', responsible: 'Elena Paredes', status: mockStatus('PRODUCT_APPROVED'), digitalComponent: 'Si' },
    { code: 'P-010-2026', name: 'Módulo de Seguros Agrarios', originCode: 'NA', originMode: 'PREEXISTING', unit: 'DGA', responsible: 'Ricardo Salazar', status: mockStatus('SUSPENDED'), digitalComponent: 'No' },
  ] satisfies ProjectRecord[]).map(enrichMockProject));

  readonly documentDossiers = signal<DocumentDossier[]>(([
    {
      recordType: 'Iniciativa',
      code: 'I-024-2026',
      name: 'Mejoramiento del servicio de riego tecnificado en el valle de Ica',
      unit: 'DGIA',
      status: mockStatus('PRESENTED'),
      lastActivity: '23/05/2026 10:28',
      executingUnitId: 1,
      stages: [
        {
          title: '1. Registro inicial',
          records: [
            { name: 'Ficha de Iniciativa de Innovación Pública', required: true, filename: 'Ficha_Iniciativa_I-024-2026.pdf', version: '1.0', uploadedAt: '20/05/2026', state: 'Cargado' },
          ],
        },
        {
          title: '2. Evaluación',
          records: [
            { name: 'Informe de opinión técnica de evaluación de iniciativa', required: false, filename: 'Informe_Opinion_I-024-2026.pdf', version: '1.0', uploadedAt: '23/05/2026', state: 'Cargado' },
          ],
        },
        {
          title: '3. Decisión',
          records: [
            { name: 'Documento formal de decisión de aprobación', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
          ],
        },
        {
          title: '4. Etapas posteriores',
          records: [
            { name: 'Documento formal de aprobación de producto final', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
            { name: 'Documentación de la gestión del proyecto', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
            { name: 'Informe final de cierre', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
          ],
        },
      ],
    },
    {
      recordType: 'Iniciativa',
      code: 'I-019-2026',
      name: 'Fortalecimiento de capacidades para la gestión de la innovación agraria',
      unit: 'DIPNA',
      status: mockStatus('INITIATIVE_APPROVED'),
      lastActivity: '18/05/2026 16:45',
      executingUnitId: 1,
      stages: [
        { title: '1. Registro inicial', records: [{ name: 'Ficha de Iniciativa de Innovación Pública', required: true, filename: 'Ficha_Iniciativa_I-019-2026.pdf', version: '1.0', uploadedAt: '06/05/2026', state: 'Cargado' }] },
        { title: '2. Evaluación', records: [{ name: 'Informe de opinión técnica de evaluación de iniciativa', required: false, filename: 'Informe_Opinion_I-019-2026.pdf', version: '1.0', uploadedAt: '14/05/2026', state: 'Cargado' }] },
        { title: '3. Decisión', records: [{ name: 'Documento formal de decisión de aprobación', required: false, filename: 'Decision_I-019-2026.pdf', version: '1.0', uploadedAt: '18/05/2026', state: 'Cargado' }] },
        { title: '4. Etapas posteriores', records: [
          { name: 'Documento formal de aprobación de producto final', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
          { name: 'Documentación de la gestión del proyecto', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
          { name: 'Informe final de cierre', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
        ] },
      ],
    },
    {
      recordType: 'Proyecto',
      code: 'P-005-2026',
      name: 'Red de Estaciones Agrometeorológicas',
      unit: 'DCLIMA',
      status: mockStatus('PROJECT_IN_PROGRESS'),
      lastActivity: '24/05/2026 11:10',
      executingUnitId: 1,
      stages: [
        { title: '1. Registro inicial', records: [{ name: 'Ficha de Iniciativa de Innovación Pública', required: false, filename: null, version: null, uploadedAt: null, state: 'No aplica' }] },
        { title: '2. Evaluación', records: [{ name: 'Informe de opinión técnica de evaluación de iniciativa', required: false, filename: null, version: null, uploadedAt: null, state: 'No aplica' }] },
        { title: '3. Decisión', records: [{ name: 'Documento formal de decisión de aprobación', required: false, filename: null, version: null, uploadedAt: null, state: 'No aplica' }] },
        { title: '4. Etapas posteriores', records: [
          { name: 'Documento formal de aprobación de producto final', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
          { name: 'Documentación de la gestión del proyecto', required: false, filename: 'Gestion_Proyecto_P-005-2026.pdf', version: '1.0', uploadedAt: '24/05/2026', state: 'Cargado' },
          { name: 'Informe final de cierre', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
        ] },
      ],
    },
  ] as DocumentDossier[]).map(hydrateMockDossier));

  readonly auditEvents = signal<AuditEvent[]>([
    { recordCode: 'I-024-2026', timestamp: '20/05/2026\n10:28:19', event: 'Informe técnico cargado', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'Se cargó el informe técnico de evaluación.', documentName: 'Informe_tecnico_I-024-2026.pdf', icon: 'cloud_upload' },
    { recordCode: 'I-024-2026', timestamp: '20/05/2026\n10:02:44', event: 'Estado cambiado a Presentado', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'La iniciativa fue presentada para evaluación.', icon: 'check' },
    { recordCode: 'I-024-2026', timestamp: '20/05/2026\n09:45:27', event: 'Ficha inicial cargada', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'Se cargó la ficha inicial de la iniciativa.', documentName: 'Ficha_inicial_I-024-2026.pdf', icon: 'description' },
    { recordCode: 'I-024-2026', timestamp: '20/05/2026\n09:31:12', event: 'Iniciativa creada', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'Se inició un borrador local del registro.', icon: 'add' },
  ]);

  toggleRole(): void {
    const user = this.currentUser();
    const executingUnitId = this.selectedExecutingUnitId();
    if (!user || executingUnitId === null) return;
    const nextRole = this.role() === 'Administrador PIIP' ? 'CONSULTA_EXTERNA' : 'ADMINISTRADOR_PIIP';
    const unit = this.executingUnits().find((candidate) => candidate.id === executingUnitId);
    if (!unit) return;
    this.currentUser.set({
      ...user,
      roles: [nextRole],
      roleScopes: [{ role: nextRole, institutionId: unit.institutionId, executingUnitId }],
    });
  }

  initialize(): void {}
  refreshAll(): void {}
  refreshAuthorizationContext(): void {}
  reloadCatalogs(): void {}
  reloadOrganizationalUnits(): void {}
  loadAdministrableScopes(): void {}
  loadAdministrativeInstitutions(): AdministrativeInstitution[] { return this.administrativeInstitutions(); }
  loadAdministrativeExecutingUnits(institutionId: number): AdministrativeExecutingUnit[] {
    return this.administrativeExecutingUnits().filter((unit) => unit.institution.id === institutionId)
      .sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name) || a.id - b.id);
  }
  loadAdministrativeExecutingUnit(executingUnitId: number): AdministrativeExecutingUnit | undefined {
    return this.administrativeExecutingUnits().find((unit) => unit.id === executingUnitId);
  }
  createAdministrativeExecutingUnit(input: CreateAdministrativeExecutingUnitInput): AdministrativeExecutingUnit {
    const institution = this.administrativeInstitutions().find((item) => item.id === input.institutionId);
    if (!institution) throw mockRepositoryError(403, 'La institución no pertenece a tu ámbito administrativo.');
    const inInstitution = this.loadAdministrativeExecutingUnits(input.institutionId);
    const displayOrder = input.displayOrder ?? (inInstitution.length ? Math.max(...inInstitution.map((item) => item.displayOrder)) + 1 : 0);
    const unit: AdministrativeExecutingUnit = {
      id: Math.max(0, ...this.administrativeExecutingUnits().map((item) => item.id)) + 1,
      code: `UE-${String(inInstitution.length + 1).padStart(3, '0')}`,
      name: input.name,
      active: true,
      displayOrder,
      registeredAt: new Date().toISOString(),
      activatedAt: new Date().toISOString(),
      version: 0,
      institution,
    };
    this.administrativeExecutingUnits.update((items) => [...items, unit]);
    return unit;
  }
  updateAdministrativeExecutingUnit(id: number, version: number, input: UpdateAdministrativeExecutingUnitInput): AdministrativeExecutingUnit {
    const current = this.requireMockAdministrativeExecutingUnit(id, version);
    const updated = { ...current, name: input.name, displayOrder: input.displayOrder, version: current.version + 1 };
    this.administrativeExecutingUnits.update((items) => items.map((item) => item.id === id ? updated : item));
    return updated;
  }
  deactivateAdministrativeExecutingUnit(id: number, version: number): AdministrativeExecutingUnit {
    const current = this.requireMockAdministrativeExecutingUnit(id, version);
    if (!current.active) throw mockRepositoryError(422, 'La Unidad Ejecutora ya está inactiva.');
    const updated = { ...current, active: false, version: current.version + 1 };
    this.administrativeExecutingUnits.update((items) => items.map((item) => item.id === id ? updated : item));
    return updated;
  }
  reactivateAdministrativeExecutingUnit(id: number, version: number): AdministrativeExecutingUnit {
    const current = this.requireMockAdministrativeExecutingUnit(id, version);
    if (current.active) throw mockRepositoryError(422, 'La Unidad Ejecutora ya está activa.');
    const updated = { ...current, active: true, activatedAt: new Date().toISOString(), version: current.version + 1 };
    this.administrativeExecutingUnits.update((items) => items.map((item) => item.id === id ? updated : item));
    return updated;
  }
  loadAdministrativeOrganizationalUnits(executingUnitId: number): AdministrativeOrganizationalUnit[] {
    return this.administrativeOrganizationalUnits().filter((unit) => unit.executingUnit.id === executingUnitId);
  }
  createAdministrativeOrganizationalUnit(input: CreateAdministrativeOrganizationalUnitInput): AdministrativeOrganizationalUnit {
    const executingUnit = this.administrativeExecutingUnits().find((item) => item.id === input.executingUnitId);
    if (!executingUnit) throw mockRepositoryError(404, 'La Unidad Ejecutora indicada no existe.');
    const siblings = this.loadAdministrativeOrganizationalUnits(input.executingUnitId);
    const unit: AdministrativeOrganizationalUnit = {
      id: Math.max(0, ...this.administrativeOrganizationalUnits().map((item) => item.id)) + 1,
      code: `UO-${String(siblings.length + 1).padStart(3, '0')}`,
      name: input.name,
      acronym: input.acronym,
      active: input.active,
      version: 0,
      executingUnit: { id: executingUnit.id, code: executingUnit.code, name: executingUnit.name },
    };
    this.administrativeOrganizationalUnits.update((items) => [...items, unit]);
    return unit;
  }
  updateAdministrativeOrganizationalUnit(id: number, version: number, input: UpdateAdministrativeOrganizationalUnitInput): AdministrativeOrganizationalUnit {
    const current = this.requireMockAdministrativeOrganizationalUnit(id, version);
    const updated = { ...current, name: input.name, acronym: input.acronym, version: current.version + 1 };
    this.administrativeOrganizationalUnits.update((items) => items.map((item) => item.id === id ? updated : item));
    return updated;
  }
  deactivateAdministrativeOrganizationalUnit(id: number, version: number): AdministrativeOrganizationalUnit {
    const current = this.requireMockAdministrativeOrganizationalUnit(id, version);
    if (!current.active) throw mockRepositoryError(422, 'La Unidad Orgánica ya está inactiva.');
    const updated = { ...current, active: false, version: current.version + 1 };
    this.administrativeOrganizationalUnits.update((items) => items.map((item) => item.id === id ? updated : item));
    return updated;
  }
  reactivateAdministrativeOrganizationalUnit(id: number, version: number): AdministrativeOrganizationalUnit {
    const current = this.requireMockAdministrativeOrganizationalUnit(id, version);
    if (current.active) throw mockRepositoryError(422, 'La Unidad Orgánica ya está activa.');
    if (!current.acronym.trim()) throw mockRepositoryError(422, 'La Unidad Orgánica requiere una sigla para reactivarse.');
    const updated = { ...current, active: true, version: current.version + 1 };
    this.administrativeOrganizationalUnits.update((items) => items.map((item) => item.id === id ? updated : item));
    return updated;
  }
  private requireMockAdministrativeExecutingUnit(id: number, version: number): AdministrativeExecutingUnit {
    const unit = this.administrativeExecutingUnits().find((item) => item.id === id);
    if (!unit) throw mockRepositoryError(404, 'La Unidad Ejecutora indicada no existe.');
    if (unit.version !== version) throw mockRepositoryError(409, 'La Unidad Ejecutora cambió.');
    return unit;
  }
  private requireMockAdministrativeOrganizationalUnit(id: number, version: number): AdministrativeOrganizationalUnit {
    const unit = this.administrativeOrganizationalUnits().find((item) => item.id === id);
    if (!unit) throw mockRepositoryError(404, 'La Unidad Orgánica indicada no existe.');
    if (unit.version !== version) throw mockRepositoryError(409, 'La Unidad Orgánica cambió.');
    return unit;
  }
  async loadUserAdministration(): Promise<UserAdministrationSnapshot> {
    return { users: this.userAdministrationUsers(), assignmentCandidates: this.assignmentCandidates() };
  }
  async assignUserRole(input: AssignmentMutationInput): Promise<AssignmentMutationResult> {
    const subject = input.userSubject ?? '';
    const candidate = this.assignmentCandidates().find((item) => item.subject === subject);
    if (!candidate) throw mockRepositoryError(404, 'El usuario seleccionado no está disponible.');
    const existingUser = this.userAdministrationUsers().find((item) => item.subject === subject);
    const existingScope = existingUser?.scopes.find((scope) => scope.institutionId === input.institutionId && scope.executingUnitId === input.executingUnitId);
    const scope: UserAssignmentScope = existingScope
      ? { ...existingScope, role: input.role, active: true, version: existingScope.version + 1 }
      : {
        id: Math.max(0, ...this.userAdministrationUsers().flatMap((item) => item.scopes.map((scopeItem) => scopeItem.id))) + 1,
        role: input.role,
        institutionId: input.institutionId,
        institution: this.administrableScopes().find((item) => item.institutionId === input.institutionId)?.institutionName ?? 'Institución de demostración',
        executingUnitId: input.executingUnitId,
        executingUnit: this.executingUnits().find((item) => item.id === input.executingUnitId)?.name,
        active: true,
        version: 0,
      };
    const user = existingUser ?? { id: candidate.id, subject: candidate.subject, fullName: candidate.fullName, email: candidate.email, scopes: [] };
    this.userAdministrationUsers.update((items) => [
      { ...user, scopes: [...user.scopes.filter((item) => item.id !== scope.id), scope] },
      ...items.filter((item) => item.subject !== subject),
    ]);
    this.assignmentCandidates.update((items) => items.filter((item) => item.subject !== subject));
    return { outcome: existingScope ? 'REACTIVATED' : 'CREATED', status: existingScope ? 200 : 201, scope };
  }
  async updateUserAssignment(scopeId: number, version: number, input: AssignmentMutationInput): Promise<AssignmentMutationResult> {
    const current = this.findMockScope(scopeId, version);
    const scope: UserAssignmentScope = { ...current, role: input.role, institutionId: input.institutionId, executingUnitId: input.executingUnitId, version: current.version + 1 };
    this.replaceMockScope(scopeId, scope);
    return { outcome: 'UPDATED', status: 200, scope };
  }
  async suspendUserAssignment(scopeId: number, version: number): Promise<AssignmentMutationResult> {
    const current = this.findMockScope(scopeId, version);
    const scope = { ...current, active: false, version: current.version + 1 };
    this.replaceMockScope(scopeId, scope);
    return { outcome: 'SUSPENDED', status: 204, scope };
  }
  async reactivateUserAssignment(scopeId: number, version: number): Promise<AssignmentMutationResult> {
    const current = this.findMockScope(scopeId, version);
    const scope = { ...current, active: true, version: current.version + 1 };
    this.replaceMockScope(scopeId, scope);
    return { outcome: 'REACTIVATED', status: 200, scope };
  }
  private findMockScope(scopeId: number, version: number): UserAssignmentScope {
    const scope = this.userAdministrationUsers().flatMap((item) => item.scopes).find((item) => item.id === scopeId);
    if (!scope) throw mockRepositoryError(404, 'La asignación no existe.');
    if (scope.version !== version) throw mockRepositoryError(409, 'La asignación cambió.');
    return scope;
  }
  private replaceMockScope(scopeId: number, replacement: UserAssignmentScope): void {
    this.userAdministrationUsers.update((items) => items.map((item) => ({
      ...item,
      scopes: item.scopes.map((scope) => scope.id === scopeId ? replacement : scope),
    })));
  }
  clearError(): void { this.lastError.set(null); }
  canReadExecutingUnit(executingUnitId: number | null | undefined): boolean { return this.hasGrantForExecutingUnit(executingUnitId); }
  canAdministerExecutingUnit(executingUnitId: number | null | undefined): boolean { return this.hasGrantForExecutingUnit(executingUnitId, 'ADMINISTRADOR_PIIP'); }
  hasAnyAdministratorScope(): boolean { return this.currentUser()?.roleScopes.some((scope) => scope.role === 'ADMINISTRADOR_PIIP') ?? false; }
  effectiveRoleForExecutingUnit(executingUnitId: number | null | undefined): UserRole | null {
    if (this.canAdministerExecutingUnit(executingUnitId)) return 'Administrador PIIP';
    return this.hasGrantForExecutingUnit(executingUnitId, 'CONSULTA_EXTERNA') ? 'Consulta externa' : null;
  }
  selectExecutingUnit(executingUnitId: number): void {
    this.selectedExecutingUnitId.set(executingUnitId);
    if (!this.canAdministerExecutingUnit(executingUnitId)) this.administrableScopes.set([]);
  }

  loadHomePortfolio(query: HomePortfolioQuery): void {
    this.homePortfolioLoading.set(true);
    this.homePortfolioError.set(null);
    try {
      const unit = this.executingUnits().find((candidate) => candidate.id === query.executingUnitId);
      const records = this.portfolioRecords()
        .filter((record) => record.executingUnitId === query.executingUnitId)
        .filter((record) => query.type === 'Todos' || record.recordType === query.type)
        .filter((record) => query.status === 'Todos' || record.status.code === query.status)
        .filter((record) => !query.q || `${record.code} ${record.name}`.toLocaleLowerCase().includes(query.q.toLocaleLowerCase()))
        .sort((a, b) => mockUpdatedAt(b, this.initiatives(), this.documentDossiers()).localeCompare(mockUpdatedAt(a, this.initiatives(), this.documentDossiers())) || b.code.localeCompare(a.code));
      const statusCounts = new Map<string, number>();
      const statusReferences = new Map<string, PortfolioStatusReference>();
      records.forEach((record) => {
        const code = record.status.code;
        if (!code) return;
        statusCounts.set(code, (statusCounts.get(code) ?? 0) + 1);
        statusReferences.set(code, record.status);
      });
      const totalPages = Math.ceil(records.length / query.size);
      const page = totalPages > 0 && query.page >= totalPages ? 0 : query.page;
      const content = records.slice(page * query.size, (page + 1) * query.size).map((record): HomePortfolioItem => ({
        recordType: record.recordType,
        code: record.code,
        name: record.name,
        status: record.status,
        executingUnitId: record.executingUnitId ?? query.executingUnitId,
        executingUnit: unit?.name ?? 'Unidad Ejecutora activa',
        updatedAt: mockUpdatedAt(record, this.initiatives(), this.documentDossiers()),
      }));
      this.homePortfolio.set({
        content,
        page,
        size: query.size,
        totalElements: records.length,
        totalPages,
        executingUnitTotalElements: this.portfolioRecords().filter((record) => record.executingUnitId === query.executingUnitId).length,
        statusCounts: [...statusCounts.entries()].map(([code, count]): HomePortfolioStatusCount => ({ status: statusReferences.get(code) ?? mockStatus(code as PiipStatus), count })),
      });
    } catch (error) {
      this.homePortfolioError.set(error instanceof Error ? error.message : 'No fue posible cargar el portafolio.');
    } finally {
      this.homePortfolioLoading.set(false);
    }
  }

  refreshNotifications(): void {
    this.notificationsLoading.set(false);
    this.notificationsError.set(null);
  }

  getDocumentDossier(recordType: PiipRecordType, code: string): DocumentDossier | undefined {
    return this.documentDossiers().find((dossier) => dossier.recordType === recordType && dossier.code === code);
  }

  getDocumentDossierSummaries(): DocumentDossierSummary[] {
    return this.documentDossiers().map(summarizeDocumentDossier);
  }

  getInitiativeDetail(code: string): InitiativeDetail | undefined {
    const initiative = this.initiatives().find((record) => record.code === code);
    const portfolioRecord = this.portfolioRecords().find(
      (record) => record.recordType === 'Iniciativa' && record.code === code,
    );
    if (!initiative || !portfolioRecord) return undefined;

    return {
      initiative,
      portfolioRecord,
      dossier: this.getDocumentDossier('Iniciativa', code),
      derivedProject: this.getProjectByOrigin(code),
    };
  }

  getProjectDetail(code: string): ProjectDetail | undefined {
    const project = this.projects().find((record) => record.code === code);
    const portfolioRecord = this.portfolioRecords().find((record) => record.recordType === 'Proyecto' && record.code === code);
    if (!project || !portfolioRecord) return undefined;
    return {
      project,
      portfolioRecord,
      dossier: this.getDocumentDossier('Proyecto', code),
      originInitiative: project.originMode === 'DERIVED_FROM_INITIATIVE'
        ? this.initiatives().find((initiative) => initiative.code === project.originCode)
        : undefined,
    };
  }

  reloadPortfolioRecord(_recordType: PiipRecordType, _code: string): void {}

  getProjectByOrigin(initiativeCode: string): ProjectRecord | undefined {
    return this.projects().find(
      (project) => project.originMode === 'DERIVED_FROM_INITIATIVE' && project.originCode === initiativeCode,
    );
  }

  getInitiativesEligibleForProject(): InitiativeRecord[] {
    return this.initiatives().filter(
      (initiative) => initiative.status.code === 'INITIATIVE_APPROVED' && !this.getProjectByOrigin(initiative.code),
    );
  }

  getNextProjectCode(initiativeCode: string): string {
    const year = initiativeCode.match(/(\d{4})$/)?.[1] ?? String(new Date().getFullYear());
    const nextSequence = this.projects().reduce((maximum, project) => {
      const match = project.code.match(/^P-(\d+)-\d{4}$/);
      return Math.max(maximum, match ? Number(match[1]) : 0);
    }, 0) + 1;
    return `P-${String(nextSequence).padStart(3, '0')}-${year}`;
  }

  saveDraft(value: unknown): void {
    localStorage.setItem('piip-web2-initiative-draft', JSON.stringify(value));
  }

  savePreexistingProjectDraft(value: unknown): void {
    localStorage.setItem('piip-web2-preexisting-project-draft', JSON.stringify(value));
  }

  saveDerivedProjectDraft(value: unknown): void {
    localStorage.setItem('piip-web2-derived-project-draft', JSON.stringify(value));
  }

  approveInitiative(input: InitiativeDecisionInput): PiipPortfolioRecord {
    this.assertAdministrator('El perfil Consulta externa no puede aprobar iniciativas.');
    const detail = this.getInitiativeDetail(input.initiativeCode);
    if (!detail) throw new Error('La iniciativa indicada no existe.');
    if (detail.initiative.status.code !== 'PRESENTED') {
      throw new Error('Solo una iniciativa en estado Presentado puede aprobarse.');
    }

    const now = new Date();
    const updatedAt = formatDateTime(now);
    this.initiatives.update((initiatives) => initiatives.map((initiative) =>
      initiative.code === input.initiativeCode
        ? { ...initiative, status: mockStatus('INITIATIVE_APPROVED'), updatedAt }
        : initiative,
    ));
    this.portfolioRecords.update((records) => records.map((record) =>
      record.recordType === 'Iniciativa' && record.code === input.initiativeCode
        ? { ...record, status: mockStatus('INITIATIVE_APPROVED') }
        : record,
    ));
    this.documentDossiers.update((dossiers) => dossiers.map((dossier) =>
      dossier.recordType === 'Iniciativa' && dossier.code === input.initiativeCode
        ? { ...dossier, status: mockStatus('INITIATIVE_APPROVED'), lastActivity: updatedAt }
        : dossier,
    ));
    this.auditEvents.update((events) => [
      {
        recordCode: input.initiativeCode,
        timestamp: formatAuditTimestamp(now),
        event: 'Iniciativa aprobada',
        user: 'Administrador PIIP',
        email: 'admin.piip@midagri.gob.pe',
        observation: input.observation.trim() || `${input.initiativeCode} cambió de Presentado a Iniciativa aprobada.`,
        icon: 'verified',
      },
      ...events,
    ]);
    return this.portfolioRecords().find((record) => record.code === input.initiativeCode)!;
  }

  registerInitiative(input: InitiativeInput): PiipPortfolioRecord {
    this.assertAdministrator('El perfil Consulta externa no puede registrar iniciativas.');
    const solutionType = this.catalogName('solutionTypes', input.solutionTypeId);
    const source = this.catalogName('sources', input.sourceId);
    const unitIds = input.responsibleUnitIds ?? (input.organizationalUnitId === undefined ? [] : [input.organizationalUnitId]);
    const unit = this.unitNames(unitIds);
    const responsibleUnitReferences = this.unitsByIds(unitIds);
    const record: PiipPortfolioRecord = {
      recordType: 'Iniciativa', code: input.code, originCode: 'NA', name: input.name,
      solutionType: solutionType as PiipPortfolioRecord['solutionType'], source, startDate: input.startDate,
      responsible: input.responsible, peiObjective: this.optionalCatalogName('peiObjectives', input.peiObjectiveId), poiActivity: this.optionalCatalogName('poiActivities', input.poiActivityId),
       responsibleUnits: unit, responsibleUnitReferences, description: input.description, keyResults: '', note: input.note,
      status: mockStatus('PRESENTED'), finalProductType: 'NA', digitalComponent: input.digitalComponent, closingDate: '',
      technicalOpinionReport: '', formalApprovalDecision: '', finalProductApprovalDocument: '',
      projectManagementDocumentation: '', finalClosureReport: '',
    };
    const structuredRecord = enrichMockRecord(record);
    this.portfolioRecords.update((records) => [structuredRecord, ...records]);
    this.initiatives.update((items) => [enrichMockInitiative({ code: input.code, name: input.name, source,
      responsible: input.responsible, role: '', unit, status: mockStatus('PRESENTED'),
      updatedAt: formatDateTime(new Date()) }), ...items]);
    return structuredRecord;
  }

  transitionInitiativeStatus(input: InitiativeStatusTransitionInput): PiipPortfolioRecord {
    this.assertAdministrator('El perfil Consulta externa no puede cambiar estados.');
    const detail = this.getInitiativeDetail(input.initiativeCode);
    if (!detail) throw new Error('La iniciativa indicada no existe.');
    if (detail.derivedProject) throw new Error('La iniciativa tiene un proyecto vinculado y está bloqueada.');
    const allowed = INITIATIVE_STATUS_TRANSITIONS[detail.initiative.status.code as InitiativeStatus] ?? [];
    if (!allowed.includes(input.targetStatus)) throw new Error('La transición de iniciativa no está permitida.');
    const now = formatDateTime(new Date());
    const previous = detail.initiative.status;
    const target = mockStatus(input.targetStatus);
    this.initiatives.update((items) => items.map((item) => item.code === input.initiativeCode ? { ...item, status: target, updatedAt: now } : item));
    this.portfolioRecords.update((items) => items.map((item) => item.code === input.initiativeCode ? { ...item, status: target } : item));
    this.documentDossiers.update((items) => items.map((item) => item.code === input.initiativeCode ? { ...item, status: target, lastActivity: now } : item));
    this.auditEvents.update((items) => [{ recordCode: input.initiativeCode, timestamp: formatAuditTimestamp(new Date()), event: 'ESTADO_INICIATIVA_CAMBIADO', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: JSON.stringify({ estadoAnterior: previous.name, estadoNuevo: target.name, previousStatusCode: previous.code, newStatusCode: target.code, observacion: input.observation.trim() }), previousStatus: previous, newStatus: target, icon: 'swap_horiz' }, ...items]);
    return this.portfolioRecords().find((record) => record.code === input.initiativeCode)!;
  }

  transitionProjectStatus(input: ProjectStatusTransitionInput): PiipPortfolioRecord {
    this.assertAdministrator('El perfil Consulta externa no puede cambiar estados.');
    const detail = this.getProjectDetail(input.projectCode);
    if (!detail) throw new Error('El proyecto indicado no existe.');
    const allowed = PROJECT_STATUS_TRANSITIONS[detail.project.status.code as ProjectStatus] ?? [];
    if (!allowed.includes(input.targetStatus)) throw new Error('La transición de proyecto no está permitida.');
    const now = formatDateTime(new Date());
    const previous = detail.project.status;
    const target = mockStatus(input.targetStatus);
    this.projects.update((items) => items.map((item) => item.code === input.projectCode ? { ...item, status: target } : item));
    this.portfolioRecords.update((items) => items.map((item) => item.code === input.projectCode ? { ...item, status: target, closingDate: input.targetStatus === 'FINISHED' ? now : item.closingDate } : item));
    this.documentDossiers.update((items) => items.map((item) => item.code === input.projectCode ? { ...item, status: target, lastActivity: now } : item));
    this.auditEvents.update((items) => [{ recordCode: input.projectCode, timestamp: formatAuditTimestamp(new Date()), event: 'ESTADO_PROYECTO_CAMBIADO', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: JSON.stringify({ estadoAnterior: previous.name, estadoNuevo: target.name, previousStatusCode: previous.code, newStatusCode: target.code, observacion: input.observation.trim() }), previousStatus: previous, newStatus: target, icon: 'swap_horiz' }, ...items]);
    return this.portfolioRecords().find((record) => record.code === input.projectCode)!;
  }

  registerDerivedProject(input: DerivedProjectInput): PiipPortfolioRecord {
    this.assertAdministrator('El perfil Consulta externa no puede registrar proyectos.');
    const detail = this.getInitiativeDetail(input.initiativeCode);
    if (!detail) throw new Error('La iniciativa de origen no existe.');
    if (detail.initiative.status.code !== 'INITIATIVE_APPROVED') {
      throw new Error('El proyecto requiere una iniciativa en estado Iniciativa aprobada.');
    }
    if (this.getProjectByOrigin(input.initiativeCode)) {
      throw new Error('La iniciativa ya tiene un proyecto derivado.');
    }
    if (!input.startDate) throw new Error('La fecha de inicio del proyecto es obligatoria.');
    if (this.projects().some((project) => project.code === input.code)) {
      throw new Error('El código de proyecto ya se encuentra registrado.');
    }

    const originCode = resolveProjectOriginCode({ mode: 'DERIVED_FROM_INITIATIVE', initiativeCode: input.initiativeCode });
    const unitIds = input.responsibleUnitIds ?? (input.organizationalUnitId === undefined ? [] : [input.organizationalUnitId]);
    const unit = this.unitNames(unitIds);
    const responsibleUnitReferences = this.unitsByIds(unitIds);
    const portfolioRecord: PiipPortfolioRecord = {
      recordType: 'Proyecto', code: input.code, originCode, name: input.name,
      solutionType: this.catalogName('solutionTypes', input.solutionTypeId) as PiipPortfolioRecord['solutionType'], source: this.catalogName('sources', input.sourceId), startDate: input.startDate,
      responsible: input.responsible, peiObjective: this.optionalCatalogName('peiObjectives', input.peiObjectiveId), poiActivity: this.optionalCatalogName('poiActivities', input.poiActivityId),
       responsibleUnits: unit, responsibleUnitReferences, description: input.description, keyResults: input.keyResults,
      note: input.note, status: mockStatus('PROJECT_IN_PROGRESS'), finalProductType: 'NA',
      digitalComponent: input.digitalComponent, closingDate: '', technicalOpinionReport: '',
      formalApprovalDecision: '', finalProductApprovalDocument: '', projectManagementDocumentation: '',
      finalClosureReport: '',
    };
    const project: ProjectRecord = {
      code: input.code, name: input.name, originCode, originMode: 'DERIVED_FROM_INITIATIVE',
      unit, responsible: input.responsible, status: mockStatus('PROJECT_IN_PROGRESS'),
      digitalComponent: input.digitalComponent,
    };

    const structuredRecord = enrichMockRecord(portfolioRecord);
    this.portfolioRecords.update((records) => [structuredRecord, ...records]);
    this.projects.update((projects) => [enrichMockProject(project), ...projects]);
    this.documentDossiers.update((dossiers) => [hydrateMockDossier(createDerivedProjectDocumentDossier(input, unit), dossiers.length), ...dossiers]);
    this.auditEvents.update((events) => [
      {
        recordCode: input.code,
        timestamp: formatAuditTimestamp(new Date()),
        event: 'Proyecto derivado registrado',
        user: 'Administrador PIIP',
        email: 'admin.piip@midagri.gob.pe',
        observation: `${input.code} se creó a partir de ${input.initiativeCode}.`,
        icon: 'account_tree',
      },
      ...events,
    ]);
    return structuredRecord;
  }

  updateInitiative(code: string, input: InitiativeUpdateInput): PiipPortfolioRecord {
    const detail = this.getInitiativeDetail(code);
    if (!detail) throw mockRepositoryError(404, 'La iniciativa indicada no existe.');
    if (!this.canAdministerExecutingUnit(detail.portfolioRecord.executingUnitId)) throw mockRepositoryError(403, 'No tienes autorización sobre la Unidad Ejecutora del registro.');
    if (detail.initiative.status.code !== 'PRESENTED' || detail.derivedProject) throw mockRepositoryError(422, 'La iniciativa no se encuentra en un estado editable.');
    return this.applyMockUpdate(code, input.version, {
      name: input.name,
      solutionTypeId: input.solutionTypeId,
      sourceId: input.sourceId,
      startDate: input.startDate,
      responsible: input.responsible,
      peiObjectiveId: input.peiObjectiveId,
      poiActivityId: input.poiActivityId,
      responsibleUnitIds: input.responsibleUnitIds,
      description: input.description,
      note: input.note,
      digitalComponent: input.digitalComponent,
    });
  }

  updateProject(code: string, input: ProjectUpdateInput): PiipPortfolioRecord {
    const detail = this.getProjectDetail(code);
    if (!detail) throw mockRepositoryError(404, 'El proyecto indicado no existe.');
    if (!this.canAdministerExecutingUnit(detail.portfolioRecord.executingUnitId)) throw mockRepositoryError(403, 'No tienes autorización sobre la Unidad Ejecutora del registro.');
    if (detail.project.status.code !== 'PROJECT_IN_PROGRESS') throw mockRepositoryError(422, 'El proyecto no se encuentra en un estado editable.');
    return this.applyMockUpdate(code, input.version, {
      name: input.name,
      solutionTypeId: input.solutionTypeId,
      sourceId: input.sourceId,
      startDate: input.startDate,
      responsible: input.responsible,
      peiObjectiveId: input.peiObjectiveId,
      poiActivityId: input.poiActivityId,
      responsibleUnitIds: input.responsibleUnitIds,
      description: input.description,
      keyResults: input.keyResults,
      note: input.note,
      digitalComponent: input.digitalComponent,
    });
  }

  private applyMockUpdate(code: string, version: number, changes: Partial<{
    name: string; solutionTypeId: number; sourceId: number; startDate: string; responsible: string;
    peiObjectiveId: number | null; poiActivityId: number | null; responsibleUnitIds: readonly number[];
    description: string; keyResults: string | null; note: string; digitalComponent: PiipPortfolioRecord['digitalComponent'];
  }>): PiipPortfolioRecord {
    const current = this.portfolioRecords().find((record) => record.code === code);
    if (!current) throw mockRepositoryError(404, 'El registro indicado no existe.');
    const currentVersion = current.version ?? 0;
    if (currentVersion !== version) throw mockRepositoryError(409, 'La copia abierta está desactualizada. Recarga la versión vigente.');
    const nextUnits = changes.responsibleUnitIds === undefined
      ? current.responsibleUnitReferences ?? []
      : changes.responsibleUnitIds.map((id) => this.organizationalUnits().find((unit) => unit.id === id)).filter((unit): unit is OrganizationalUnit => Boolean(unit));
    if (!nextUnits.length) throw mockRepositoryError(422, 'Selecciona al menos una Unidad Orgánica responsable.');
    if (new Set(nextUnits.map((unit) => unit.id)).size !== nextUnits.length) throw mockRepositoryError(422, 'No puedes repetir una Unidad Orgánica responsable.');
    const catalog = this.catalogs().value;
    const solutionType = changes.solutionTypeId === undefined ? current.solutionTypeReference : catalog.solutionTypes.find((item) => item.id === changes.solutionTypeId && item.active);
    const source = changes.sourceId === undefined ? current.sourceReference : catalog.sources.find((item) => item.id === changes.sourceId && item.active);
    if (!solutionType || !source) throw mockRepositoryError(422, 'Selecciona referencias activas válidas.');
    const pei = changes.peiObjectiveId === undefined ? current.peiObjectiveReference : changes.peiObjectiveId === null ? null : catalog.peiObjectives.find((item) => item.id === changes.peiObjectiveId && item.active) ?? null;
    const poi = changes.poiActivityId === undefined ? current.poiActivityReference : changes.poiActivityId === null ? null : catalog.poiActivities.find((item) => item.id === changes.poiActivityId && item.active) ?? null;
    const next: PiipPortfolioRecord = {
      ...current,
      ...(changes.name === undefined ? {} : { name: changes.name }),
      ...(changes.startDate === undefined ? {} : { startDate: changes.startDate }),
      ...(changes.responsible === undefined ? {} : { responsible: changes.responsible }),
      ...(changes.description === undefined ? {} : { description: changes.description }),
      ...(changes.keyResults === undefined ? {} : { keyResults: changes.keyResults ?? '' }),
      ...(changes.note === undefined ? {} : { note: changes.note }),
      ...(changes.digitalComponent === undefined ? {} : { digitalComponent: changes.digitalComponent }),
      solutionType: solutionType.name as PiipPortfolioRecord['solutionType'],
      solutionTypeReference: solutionType,
      source: source.name,
      sourceReference: source,
      peiObjective: pei?.name ?? '', peiObjectiveReference: pei,
      poiActivity: poi?.name ?? '', poiActivityReference: poi,
      responsibleUnits: nextUnits.map((unit) => unit.acronym || unit.name).join(', '),
      responsibleUnitReferences: nextUnits,
      version: currentVersion + 1,
    };
    if (JSON.stringify({ ...current, version: undefined }) === JSON.stringify({ ...next, version: undefined })) {
      throw mockRepositoryError(422, 'La actualización no contiene cambios efectivos.');
    }
    this.portfolioRecords.update((records) => records.map((record) => record.code === code ? next : record));
    if (next.recordType === 'Iniciativa') {
      this.initiatives.update((items) => items.map((item) => item.code === code ? { ...item, name: next.name, source: next.source, responsible: next.responsible, unit: next.responsibleUnits, status: next.status, organizationalUnits: next.responsibleUnitReferences, updatedAt: formatDateTime(new Date()), executingUnitId: next.executingUnitId } : item));
    } else {
      this.projects.update((items) => items.map((item) => item.code === code ? { ...item, name: next.name, responsible: next.responsible, unit: next.responsibleUnits, organizationalUnits: next.responsibleUnitReferences } : item));
    }
    this.auditEvents.update((events) => [{ recordCode: code, timestamp: formatAuditTimestamp(new Date()), event: `${next.recordType} actualizado`, user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'Se actualizaron campos editables del registro.', icon: 'edit' }, ...events]);
    return next;
  }

  registerPreexistingProject(input: PreexistingProjectInput): PiipPortfolioRecord {
    this.assertAdministrator('El perfil Consulta externa no puede registrar proyectos.');

    const originCode = resolveProjectOriginCode({ mode: 'PREEXISTING', initiativeCode: 'NA' });
    const unitIds = input.responsibleUnitIds ?? (input.organizationalUnitId === undefined ? [] : [input.organizationalUnitId]);
    const unit = this.unitNames(unitIds);
    const responsibleUnitReferences = this.unitsByIds(unitIds);
    const portfolioRecord: PiipPortfolioRecord = {
      recordType: 'Proyecto',
      code: input.code,
      originCode,
      name: input.name,
      solutionType: 'No aplica',
      source: this.catalogName('sources', input.sourceId),
      startDate: input.startDate,
      responsible: input.responsible,
      peiObjective: this.optionalCatalogName('peiObjectives', input.peiObjectiveId),
      poiActivity: this.optionalCatalogName('poiActivities', input.poiActivityId),
       responsibleUnits: unit, responsibleUnitReferences,
      description: input.description,
      keyResults: input.keyResults,
      note: input.note,
      status: mockStatus('PROJECT_IN_PROGRESS'),
      finalProductType: 'NA',
      digitalComponent: input.digitalComponent,
      closingDate: '',
      technicalOpinionReport: input.technicalOpinionReport,
      formalApprovalDecision: input.formalApprovalDecision,
      finalProductApprovalDocument: input.finalProductApprovalDocument,
      projectManagementDocumentation: input.projectManagementDocumentation,
      finalClosureReport: input.finalClosureReport,
    };

    const structuredRecord = enrichMockRecord(portfolioRecord);
    this.portfolioRecords.update((records) => [structuredRecord, ...records]);
    this.projects.update((projects) => [
      enrichMockProject({
        code: input.code,
        name: input.name,
        originCode,
        originMode: 'PREEXISTING',
        unit,
        responsible: input.responsible,
        status: mockStatus('PROJECT_IN_PROGRESS'),
        digitalComponent: input.digitalComponent,
      }),
      ...projects,
    ]);
    this.documentDossiers.update((dossiers) => [hydrateMockDossier(createPreexistingDocumentDossier(input, unit), dossiers.length), ...dossiers]);
    this.auditEvents.update((events) => [
      {
        recordCode: input.code,
        timestamp: formatAuditTimestamp(new Date()),
        event: 'Proyecto preexistente registrado',
        user: 'Administrador PIIP',
        email: 'admin.piip@midagri.gob.pe',
        observation: `${input.code} se incorporó al portafolio sin iniciativa predecesora.`,
        icon: 'inventory_2',
      },
      ...events,
    ]);
    return structuredRecord;
  }

  uploadDocument(code: string, documentTypeId: number, file: File): void {
    this.addFileVersion(code, documentTypeId, file, true);
  }

  addDocumentFile(code: string, documentTypeId: number, file: File): void {
    this.addFileVersion(code, documentTypeId, file, false);
  }

  addDocumentFileVersion(code: string, fileId: number, file: File): void {
    this.assertDocumentAdministrator(code);
    this.updateMockDocument(code, (document) => {
      const files = document.files ?? [];
      const target = files.find((item) => item.id === fileId);
      if (!target) throw mockRepositoryError(404, 'El archivo indicado no existe en este expediente.');
      return { ...document, files: files.map((item) => item.id === fileId ? appendMockVersion(item, file) : item), state: 'Cargado' };
    }, undefined, (document) => (document.files ?? []).some((item) => item.id === fileId));
  }

  deleteDocumentFile(code: string, fileId: number): void {
    this.assertDocumentAdministrator(code);
    this.updateMockDocument(code, (document) => {
      const files = document.files ?? [];
      if (!files.some((item) => item.id === fileId)) throw mockRepositoryError(404, 'El archivo indicado no existe en este expediente.');
      const remaining = files.filter((item) => item.id !== fileId);
      return { ...document, files: remaining, state: remaining.length ? 'Cargado' : document.state === 'No aplica' ? 'No aplica' : 'Pendiente' };
    }, undefined, (document) => (document.files ?? []).some((item) => item.id === fileId));
  }

  markDocumentNotApplicable(code: string, documentTypeId: number): void {
    this.assertDocumentAdministrator(code);
    this.updateMockDocument(code, (document) => ({ ...document, state: 'No aplica' }), documentTypeId);
  }

  downloadDocument(): void {}

  setDocumentPublication(code: string, versionId: number, published: boolean): void {
    this.assertDocumentAdministrator(code);
    this.updateMockDocument(code, (document) => ({
      ...document,
      files: (document.files ?? []).map((file) => ({
        ...file,
        current: file.current?.id === versionId ? { ...file.current, externallyPublished: published } : file.current,
        versions: file.versions.map((version) => version.id === versionId ? { ...version, externallyPublished: published } : version),
      })),
    }), undefined, (document) => (document.files ?? []).some((file) => file.versions.some((version) => version.id === versionId)));
  }
  markNotificationRead(id: number): void {
    this.notifications.update((items) => items.map((item) => item.id === id ? { ...item, read: true } : item));
  }

  private assertAdministrator(message: string): void {
    if (this.role() !== 'Administrador PIIP') throw new Error(message);
  }

  private addFileVersion(code: string, documentTypeId: number, file: File, original: boolean): void {
    this.assertDocumentAdministrator(code);
    this.updateMockDocument(code, (document) => {
      const files = document.files ?? [];
      const currentOriginal = files.find((item) => item.original);
      if (original && currentOriginal) return { ...document, files: files.map((item) => item.id === currentOriginal.id ? appendMockVersion(item, file) : item), state: 'Cargado' };
      const nextFile = createMockFile(file, nextMockFileId(this.documentDossiers()), original);
      return { ...document, files: [...files, nextFile], state: 'Cargado' };
    }, documentTypeId);
  }

  private assertDocumentAdministrator(code: string): void {
    const dossier = this.documentDossiers().find((item) => item.code === code);
    if (!dossier) throw mockRepositoryError(404, 'El expediente documental no existe.');
    if (!this.canAdministerExecutingUnit(dossier.executingUnitId)) throw mockRepositoryError(403, 'No tienes autorización sobre la Unidad Ejecutora del registro.');
  }

  private updateMockDocument(code: string, update: (document: DocumentRecord) => DocumentRecord, documentTypeId?: number, predicate?: (document: DocumentRecord) => boolean): void {
    let found = false;
    this.documentDossiers.update((dossiers) => dossiers.map((dossier) => dossier.code !== code ? dossier : {
      ...dossier,
      lastActivity: formatDateTime(new Date()),
      stages: dossier.stages.map((stage) => ({
        ...stage,
        records: stage.records.map((document) => {
          const matches = documentTypeId !== undefined ? document.documentTypeId === documentTypeId : predicate?.(document) ?? false;
          if (!matches) return document;
          found = true;
          return update(document);
        }),
      })),
    }));
    if (!found) throw mockRepositoryError(404, 'El documento indicado no existe en este expediente.');
  }

  private catalogName(key: 'solutionTypes' | 'sources', id: number): string {
    return this.catalogs().value[key].find((item) => item.id === id)?.name ?? '';
  }

  private optionalCatalogName(key: 'peiObjectives' | 'poiActivities', id?: number): string {
    return id === undefined ? '' : this.catalogs().value[key].find((item) => item.id === id)?.name ?? '';
  }

  private unitsByIds(ids: readonly number[]): OrganizationalUnit[] {
    return ids.flatMap((id) => this.organizationalUnits().filter((item) => item.id === id));
  }

  private unitNames(ids: readonly number[]): string {
    return this.unitsByIds(ids).map((unit) => unit.name).join(', ');
  }

  private hasGrantForExecutingUnit(executingUnitId: number | null | undefined, role?: 'ADMINISTRADOR_PIIP' | 'CONSULTA_EXTERNA'): boolean {
    if (executingUnitId == null) return false;
    const unit = this.executingUnits().find((candidate) => candidate.id === executingUnitId);
    if (!unit) return false;
    return this.currentUser()?.roleScopes.some((scope) =>
      (!role || scope.role === role)
      && scope.institutionId === unit.institutionId
      && (scope.executingUnitId === null || scope.executingUnitId === executingUnitId),
    ) ?? false;
  }
}

function hydrateMockDossier(dossier: DocumentDossier, dossierIndex: number): DocumentDossier {
  return {
    ...dossier,
    stages: dossier.stages.map((stage, stageIndex) => ({
      ...stage,
      records: stage.records.map((document, recordIndex) => hydrateMockDocument(document, dossierIndex * 100 + stageIndex * 10 + recordIndex + 1)),
    })),
  };
}

function hydrateMockDocument(document: DocumentRecord, seed: number): DocumentRecord {
  const documentType = mockCatalogBundle().documentTypes.find((item) => item.name === document.name);
  const current = document.filename && document.version
    ? mockVersion(document.filename, Number.parseInt(document.version, 10) || 1, seed * 10, document.uploadedAt ?? formatDateTime(new Date()), document.externallyPublished ?? false)
    : null;
  const files = current ? [{ id: seed, original: true, latestVersion: current.number, current, versions: [current] }] : [];
  return { ...document, type: documentType?.code as DocumentRecord['type'], documentTypeId: documentType?.id, documentType, files };
}

function nextMockFileId(dossiers: DocumentDossier[]): number {
  return Math.max(0, ...dossiers.flatMap((dossier) => dossier.stages.flatMap((stage) => stage.records.flatMap((document) => document.files ?? []).map((file) => file.id ?? 0)))) + 1;
}

function createMockFile(file: File, id: number, original: boolean): DocumentFile {
  const version = mockVersion(file.name, 1, id * 10, formatDateTime(new Date()), false);
  return { id, original, latestVersion: 1, current: version, versions: [version] };
}

function appendMockVersion(file: DocumentFile, upload: File): DocumentFile {
  const number = file.latestVersion + 1;
  const version = mockVersion(upload.name, number, (file.id ?? 0) * 10 + number, formatDateTime(new Date()), false);
  return { ...file, latestVersion: number, current: version, versions: [version, ...file.versions] };
}

function mockVersion(filename: string, number: number, id: number, uploadedAt: string, externallyPublished: boolean): DocumentVersion {
  return { id, number, filename, uploadedAt, externallyPublished, optimisticVersion: number - 1 };
}

export function summarizeDocumentDossier(dossier: DocumentDossier): DocumentDossierSummary {
  const documents = dossier.stages.flatMap((stage) => stage.records);
  return {
    recordType: dossier.recordType,
    code: dossier.code,
    name: dossier.name,
    unit: dossier.unit,
    status: dossier.status,
    loadedCount: documents.filter((document) => document.state === 'Cargado').length,
    pendingCount: documents.filter((document) => document.state === 'Pendiente').length,
    notApplicableCount: documents.filter((document) => document.state === 'No aplica').length,
    lastActivity: dossier.lastActivity,
    executingUnitId: dossier.executingUnitId,
  };
}

function createPreexistingProjectDocument(name: string, value: string): DocumentRecord {
  if (value === 'No Aplica') {
    return { name, required: false, filename: null, version: null, uploadedAt: null, state: 'No aplica' };
  }

  return {
    name,
    required: false,
    filename: value || null,
    version: value ? '1.0' : null,
    uploadedAt: value ? new Intl.DateTimeFormat('es-PE').format(new Date()) : null,
    state: value ? 'Cargado' : 'Pendiente',
  };
}

function createPreexistingDocumentDossier(input: PreexistingProjectInput, unit: string): DocumentDossier {
  return {
    recordType: 'Proyecto',
    code: input.code,
    name: input.name,
    unit,
    status: mockStatus('PROJECT_IN_PROGRESS'),
    lastActivity: new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(new Date()),
    stages: [
      { title: '1. Registro inicial', records: [{ name: 'Ficha de Iniciativa de Innovación Pública', required: false, filename: null, version: null, uploadedAt: null, state: 'No aplica' }] },
      { title: '2. Evaluación', records: [createPreexistingProjectDocument('Informe de opinión técnica de evaluación de iniciativa', input.technicalOpinionReport)] },
      { title: '3. Decisión', records: [createPreexistingProjectDocument('Documento formal de decisión de aprobación', input.formalApprovalDecision)] },
      { title: '4. Etapas posteriores', records: [
        createPreexistingProjectDocument('Documento formal de aprobación de producto final', input.finalProductApprovalDocument),
        createPreexistingProjectDocument('Documentación de la gestión del proyecto', input.projectManagementDocumentation),
        createPreexistingProjectDocument('Informe final de cierre', input.finalClosureReport),
      ] },
    ],
  };
}

function createDerivedProjectDocumentDossier(input: DerivedProjectInput, unit: string): DocumentDossier {
  return {
    recordType: 'Proyecto', code: input.code, name: input.name, unit,
    status: mockStatus('PROJECT_IN_PROGRESS'), lastActivity: formatDateTime(new Date()),
    stages: [
      { title: 'Documentos del proyecto', records: [
        { name: 'Documento formal de aprobación de producto final', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
        { name: 'Documentación de la gestión del proyecto', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
        { name: 'Informe final de cierre', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
      ] },
    ],
  };
}

function formatDateTime(date: Date): string {
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(date);
}

function formatAuditTimestamp(date: Date): string {
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'medium' }).format(date).replace(', ', '\n');
}

function mockCatalogBundle(): CatalogBundle {
  const option = (id: number, code: string, name: string, displayOrder: number) => ({ id, code, name, displayOrder, active: true });
  return {
    recordTypes: [
      { code: 'INITIATIVE', name: 'Iniciativa', displayOrder: 1, active: true },
      { code: 'PROJECT', name: 'Proyecto', displayOrder: 2, active: true },
    ],
    solutionTypes: [option(1, 'POTENTIAL_OR_ADAPTABLE', 'Solución potencial o adaptable', 1), option(2, 'TO_BE_DEFINED', 'Solución por definir', 2), option(3, 'NOT_APPLICABLE', 'No aplica', 3)],
    sources: [option(10, 'INITIATIVE_SHEET', 'Ficha de iniciativa de innovación pública', 1), option(11, 'INTERNAL_CONTEST', 'Concurso interno', 2), option(12, 'OPEN_INNOVATION', 'Innovación abierta', 3), option(13, 'MANAGEMENT_PROPOSAL', 'Propuesta de jefatura o directivos', 4), option(14, 'OTHER', 'Otros', 5), option(15, 'CALL', 'Convocatoria', 6)],
    peiObjectives: [option(20, 'PEI-001', 'Fortalecer la gestión institucional orientada a resultados.', 1)],
    poiActivities: [option(30, 'POI-001', 'Ejecutar acciones de mejora de procesos institucionales.', 1)],
    documentTypes: [
      option(40, 'PUBLIC_INNOVATION_INITIATIVE_SHEET', 'Ficha de Iniciativa de Innovación Pública', 1),
      option(41, 'INITIATIVE_TECHNICAL_OPINION', 'Informe de opinión técnica de evaluación de iniciativa', 2),
      option(42, 'FORMAL_APPROVAL_DECISION', 'Documento formal de decisión de aprobación', 3),
      option(43, 'FINAL_PRODUCT_APPROVAL', 'Documento formal de aprobación de producto final', 4),
      option(44, 'PROJECT_MANAGEMENT_DOCUMENTATION', 'Documentación de la gestión del proyecto', 5),
      option(45, 'FINAL_CLOSURE_REPORT', 'Informe final de cierre', 6),
    ],
    portfolioStatuses: mockStatusCatalog(),
  };
}

function mockStatusCatalog(): PortfolioStatusOption[] {
  const status = (code: PiipStatus, name: string, displayOrder: number, applicability: string): PortfolioStatusOption => ({ code, name, displayOrder, active: true, applicability });
  return [
    status('PRESENTED', 'Presentado', 1, 'INITIATIVE'),
    status('INITIATIVE_APPROVED', 'Iniciativa aprobada', 2, 'INITIATIVE'),
    status('INITIATIVE_ARCHIVED', 'Iniciativa archivada', 3, 'INITIATIVE'),
    status('PROJECT_IN_PROGRESS', 'Proyecto en ejecución', 4, 'PROJECT'),
    status('PRODUCT_APPROVED', 'Producto aprobado', 5, 'PROJECT'),
    status('PRODUCT_NOT_APPROVED', 'Producto no aprobado', 6, 'PROJECT'),
    status('SUSPENDED', 'Suspendido', 7, 'PROJECT'),
    status('CANCELLED', 'Cancelado', 8, 'PROJECT'),
    status('FINISHED', 'Finalizado', 9, 'PROJECT'),
    status('NOT_APPLICABLE', 'No Aplicable', 10, 'NONE'),
    status('NOT_ADMISSIBLE', 'No Admisible', 11, 'INITIATIVE'),
  ];
}

function mockStatus(code: PiipStatus): PortfolioStatusReference {
  const option = mockStatusCatalog().find((item) => item.code === code);
  return { code, name: option?.name ?? code, active: option?.active ?? false };
}

function enrichMockRecord(record: PiipPortfolioRecord): PiipPortfolioRecord {
  const catalogs = mockCatalogBundle();
  const solutionTypeReference = catalogs.solutionTypes.find((item) => item.name === record.solutionType);
  const sourceReference = catalogs.sources.find((item) => item.name === record.source);
  const organizationalUnit: OrganizationalUnit = { id: 101, code: 'UO-DEMO', name: record.responsibleUnits, acronym: record.responsibleUnits, parentId: null, executingUnitId: 1, active: true };
  return {
    ...record,
    recordTypeReference: catalogs.recordTypes.find((item) => item.name === record.recordType),
    solutionTypeReference,
    sourceReference,
    peiObjectiveReference: record.peiObjective ? catalogs.peiObjectives[0] : null,
    poiActivityReference: record.poiActivity ? catalogs.poiActivities[0] : null,
    responsibleUnitReferences: [organizationalUnit],
  };
}

function enrichMockInitiative(initiative: InitiativeRecord): InitiativeRecord {
  const sourceReference = mockCatalogBundle().sources.find((item) => item.name === initiative.source);
  return { ...initiative, sourceReference, organizationalUnits: [mockUnit(initiative.unit)] };
}

function enrichMockProject(project: ProjectRecord): ProjectRecord {
  return { ...project, organizationalUnits: [mockUnit(project.unit)] };
}

function mockUnit(name: string): OrganizationalUnit {
  return { id: 101, code: 'UO-DEMO', name, acronym: name, parentId: null, executingUnitId: 1, active: true };
}

function mockUpdatedAt(record: PiipPortfolioRecord, initiatives: InitiativeRecord[], dossiers: DocumentDossier[]): string {
  const source = record.recordType === 'Iniciativa'
    ? initiatives.find((item) => item.code === record.code)?.updatedAt
    : dossiers.find((item) => item.code === record.code)?.lastActivity;
  if (!source) return '';
  const match = source.match(/^(\d{2})\/(\d{2})\/(\d{4})(?:\s+(\d{2}):(\d{2}))?/);
  if (!match) return source;
  const [, day, month, year, hour = '00', minute = '00'] = match;
  return `${year}-${month}-${day}T${hour}:${minute}:00-05:00`;
}

export function resolveProjectOriginCode(origin: ProjectOrigin): string {
  if (origin.mode === 'PREEXISTING') return 'NA';
  const initiativeCode = origin.initiativeCode.trim();
  if (!initiativeCode) throw new Error('Un proyecto derivado requiere una iniciativa aprobada.');
  return initiativeCode;
}
