import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import {
  AdministrableScope, AuditAccess, AuditEvent, CurrentUser, DashboardSummary, DerivedProjectInput, DocumentDossier,
  DocumentDossierSummary, DocumentRecord, DocumentType, ExecutingUnit, InitiativeDecisionInput, InitiativeDetail,
  InitiativeInput, InitiativeRecord, InitiativeStatusTransitionInput, InitiativeUpdateInput, NotificationItem, OrganizationalUnit,
  PiipPortfolioRecord, PiipRecordType, PreexistingProjectInput, ProjectDetail, ProjectRecord,
  ProjectStatusTransitionInput, ProjectUpdateInput, UserRole, UserRoleCode, WorkItem, HomePortfolioQuery, HomePortfolioResult,
  HomePortfolioItem, HomePortfolioStatusCount, PiipStatus, CatalogBundle, PersistentCatalogOption,
  AssignmentMutationInput, AssignmentMutationResult, AssignmentRole, UserAdministrationSnapshot,
  UserAdministrationUser, UserAssignmentCandidate, UserAssignmentScope,
  TechnicalCatalogOption,
} from './piip.models';
import { CatalogControllerService, CurrentUserResponse, DashboardControllerService, DocumentControllerService, EventResponse, PortfolioControllerService, UserAdministrationControllerService } from '../api/generated';
import { PiipRepository } from './piip.repository';
import { PiipCatalogsStore } from './piip-catalogs.store';
import { resolveApiUrl as runtimeApiUrl } from './piip-runtime-config';
import {
  ApprovalRequest, DerivedProjectRequest, InitiativeCreateRequest, InitiativeStatusTransitionRequest,
  DossierSummary, InitiativeUpdateRequest, PersistentCatalogItemResponse, PreexistingProjectRequest,
  ProjectStatusTransitionRequest, ProjectUpdateRequest, ResponsibleUnitResponse, TechnicalCatalogItemResponse,
} from '../api/generated/models';

interface ApiPortfolioRecord {
  recordType: TechnicalCatalogItemResponse;
  code: string;
  originCode: string;
  name: string;
  solutionType: PersistentCatalogItemResponse;
  source: PersistentCatalogItemResponse;
  startDate: string;
  responsible: string;
  peiObjective: PersistentCatalogItemResponse | null;
  poiActivity: PersistentCatalogItemResponse | null;
  responsibleUnits: ResponsibleUnitResponse[];
  description: string;
  keyResults: string | null;
  note: string | null;
  status: PiipPortfolioRecord['status'];
  finalProductType: PiipPortfolioRecord['finalProductType'];
  digitalComponent: PiipPortfolioRecord['digitalComponent'];
  closingDate: string | null;
  technicalOpinionReport: string | null;
  formalApprovalDecision: string | null;
  finalProductApprovalDocument: string | null;
  projectManagementDocumentation: string | null;
  finalClosureReport: string | null;
  executingUnitId: number;
  executingUnit: string;
  updatedAt: string;
  version: number;
}

interface ApiPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface ApiDocumentVersion {
  id: number;
  version: number;
  filename: string;
  uploadedAt: string;
  externallyPublished: boolean;
  optimisticVersion: number;
}

interface ApiDocument {
  documentType: PersistentCatalogItemResponse;
  state: 'PENDING' | 'LOADED' | 'NOT_APPLICABLE';
  versions: ApiDocumentVersion[];
}

interface ApiWorkTask {
  id: number;
  recordCode: string;
  description: string;
  assignedTo: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  dueDate: string | null;
  alert: WorkItem['alert'];
  version: number;
}

interface ApiProblem {
  title?: string;
  detail?: string;
  status?: number;
  problemCode?: string;
}

export class PiipApiError extends Error {
  constructor(readonly status: number, message: string, readonly problemCode?: string) {
    super(message);
    this.name = 'PiipApiError';
  }
}

@Injectable({ providedIn: 'root' })
export class PiipHttpRepository extends PiipRepository {
  private readonly catalogStore = inject(PiipCatalogsStore);
  readonly demoMode = false;
  readonly currentUser = signal<CurrentUser | null>(null);
  readonly administrableScopes = signal<AdministrableScope[]>([]);
  readonly portfolioRecords = signal<PiipPortfolioRecord[]>([]);
  readonly initiatives = signal<InitiativeRecord[]>([]);
  readonly projects = signal<ProjectRecord[]>([]);
  readonly documentDossiers = signal<DocumentDossier[]>([]);
  readonly documentDossierSummaries = signal<DocumentDossierSummary[]>([]);
  readonly auditEvents = signal<AuditEvent[]>([]);
  readonly auditAccesses = signal<AuditAccess[]>([]);
  readonly workItems = signal<WorkItem[]>([]);
  readonly notifications = signal<NotificationItem[]>([]);
  readonly homePortfolio = signal<HomePortfolioResult>(emptyHomePortfolio());
  readonly homePortfolioLoading = signal(false);
  readonly homePortfolioError = signal<string | null>(null);
  readonly notificationsLoading = signal(false);
  readonly notificationsError = signal<string | null>(null);
  readonly dashboardSummary = signal<DashboardSummary>(emptyDashboard());
  readonly executingUnits = signal<ExecutingUnit[]>([]);
  readonly catalogs = this.catalogStore.catalogs;
  readonly organizationalUnitsState = this.catalogStore.organizationalUnits;
  readonly organizationalUnits = computed(() => this.organizationalUnitsState().value);
  readonly selectedExecutingUnitId = signal<number | null>(null);
  readonly role = computed(() => this.effectiveRoleForExecutingUnit(this.selectedExecutingUnitId()));
  readonly loading = signal(false);
  readonly lastError = signal<string | null>(null);

  private readonly http = inject(HttpClient);
  private readonly userAdministration = inject(UserAdministrationControllerService);
  private readonly portfolio = inject(PortfolioControllerService);
  private readonly dashboard = inject(DashboardControllerService);
  private readonly catalogApi = inject(CatalogControllerService);
  private readonly documentApi = inject(DocumentControllerService);
  private readonly apiUrl = runtimeApiUrl();
  private readonly recordVersions = new Map<string, number>();
  private readonly eligibleInitiatives = signal<InitiativeRecord[]>([]);
  private readonly loadingRecordCodes = new Set<string>();
  private homePortfolioRequestId = 0;
  private initialization?: Promise<void>;

  constructor() {
    super();
    this.userAdministration.rootUrl = this.apiUrl;
    this.portfolio.rootUrl = this.apiUrl;
    this.dashboard.rootUrl = this.apiUrl;
    this.catalogApi.rootUrl = this.apiUrl;
    this.documentApi.rootUrl = this.apiUrl;
    void this.initialize();
  }

  initialize(): Promise<void> {
    if (!this.initialization) {
      const attempt = this.initializeInternal();
      this.initialization = attempt;
      void attempt.finally(() => {
        if (!this.currentUser()) this.initialization = undefined;
      });
    }
    return this.initialization;
  }

  private async initializeInternal(): Promise<void> {
    this.loading.set(true);
    this.lastError.set(null);
    try {
      await this.loadIdentity();
      await this.loadExecutingUnits();
      await this.restoreExecutingUnit();
      await this.reloadCatalogs();
      await this.refreshAll();
    } catch (error) {
      this.captureError(error);
    } finally {
      this.loading.set(false);
    }
  }

  async refreshAll(): Promise<void> {
    const activeAdministrator = this.canAdministerExecutingUnit(this.selectedExecutingUnitId());
    if (!activeAdministrator) this.eligibleInitiatives.set([]);
    if (!this.hasAnyAdministratorScope()) {
      this.workItems.set([]);
      this.auditEvents.set([]);
      this.auditAccesses.set([]);
    }
    // Documentos y auditoría se acotan con los registros de la UE activa. Esperar
    // el portafolio evita que una respuesta concurrente observe una lista vacía
    // y termine mostrando datos de otra unidad.
    await this.loadPortfolio();
    await Promise.all([
      this.loadDocumentSummaries(),
      this.loadDashboard(),
      this.loadNotifications(),
      activeAdministrator ? this.loadEligibleInitiatives() : Promise.resolve(),
      this.hasAnyAdministratorScope() ? this.loadWorkItems() : Promise.resolve(),
      this.hasAnyAdministratorScope() ? this.loadAudit() : Promise.resolve(),
    ]);
  }

  async refreshAuthorizationContext(): Promise<void> {
    await Promise.all([this.loadIdentity(), this.loadExecutingUnits()]);
    await this.reconcileExecutingUnit(this.selectedExecutingUnitId());
    if (!this.canAdministerExecutingUnit(this.selectedExecutingUnitId())) this.administrableScopes.set([]);
    await this.refreshAll();
  }

  async reloadCatalogs(): Promise<void> {
    await this.catalogStore.loadCatalogs(async () => mapCatalogBundle(await this.request(this.catalogApi.get())));
  }

  async reloadOrganizationalUnits(): Promise<void> {
    const executingUnitId = this.selectedExecutingUnitId();
    if (executingUnitId === null) {
      this.catalogStore.clearOrganizationalUnits();
      return;
    }
    await this.loadOrganizationalUnits(executingUnitId);
  }

  clearError(): void {
    this.lastError.set(null);
  }

  async loadAdministrableScopes(): Promise<void> {
    const response = await this.request(this.userAdministration.administrableScopes());
    this.administrableScopes.set(response.flatMap((scope) =>
      scope.institutionId !== undefined && scope.institutionCode && scope.institutionName
        ? [{
            institutionId: scope.institutionId,
            institutionCode: scope.institutionCode,
            institutionName: scope.institutionName,
            institutionWideAllowed: scope.institutionWideAllowed ?? false,
            executingUnits: (scope.executingUnits ?? []).flatMap((unit) =>
              unit.id !== undefined && unit.code && unit.name
                ? [{ id: unit.id, code: unit.code, name: unit.name }]
                : [],
            ),
          }]
        : [],
    ));
  }

  async loadUserAdministration(): Promise<UserAdministrationSnapshot> {
    const [usersResponse, candidatesResponse] = await Promise.all([
      this.request(this.userAdministration.users()),
      this.request(this.userAdministration.assignmentCandidates()),
    ]);
    const users = await readGeneratedList(usersResponse);
    const assignmentCandidates = await readGeneratedList(candidatesResponse);
    return {
      users: users.flatMap(mapAdministrationUser),
      assignmentCandidates: assignmentCandidates.flatMap(mapAssignmentCandidate),
    };
  }

  async assignUserRole(input: AssignmentMutationInput): Promise<AssignmentMutationResult> {
    const response = await this.request(this.userAdministration.assign$Response({
      body: {
        userSubject: input.userSubject ?? '',
        role: input.role,
        institutionId: input.institutionId,
        executingUnitId: input.executingUnitId,
      },
    }));
    return this.mapMutationResponse(response, response.status === 201 ? 'CREATED' : 'REACTIVATED', [200, 201]);
  }

  async updateUserAssignment(scopeId: number, version: number, input: AssignmentMutationInput): Promise<AssignmentMutationResult> {
    const response = await this.request(this.userAdministration.update$Response({
      scopeId,
      version,
      body: { role: input.role, institutionId: input.institutionId, executingUnitId: input.executingUnitId },
    }));
    return this.mapMutationResponse(response, 'UPDATED', [200]);
  }

  async suspendUserAssignment(scopeId: number, version: number): Promise<AssignmentMutationResult> {
    const response = await this.request(this.userAdministration.suspend$Response({ scopeId, version }));
    if (response.status !== 204) throw new PiipApiError(response.status, 'La suspensión devolvió un estado HTTP inesperado.');
    return { outcome: 'SUSPENDED', status: response.status };
  }

  async reactivateUserAssignment(scopeId: number, version: number): Promise<AssignmentMutationResult> {
    const response = await this.request(this.userAdministration.reactivate$Response({ scopeId, version }));
    return this.mapMutationResponse(response, 'REACTIVATED', [200]);
  }

  private async mapMutationResponse(response: { status: number; body: unknown }, outcome: AssignmentMutationResult['outcome'], statuses: number[]): Promise<AssignmentMutationResult> {
    if (!statuses.includes(response.status)) throw new PiipApiError(response.status, 'La API devolvió un estado HTTP inesperado.');
    const body = await readGeneratedBody(response.body);
    if (!body || typeof body !== 'object') throw new PiipApiError(response.status, 'La API no devolvió la asignación confirmada.');
    const scope = mapScope(body);
    if (!scope) throw new PiipApiError(response.status, 'La API no devolvió una asignación confirmada válida.');
    return { outcome, status: response.status, scope };
  }

  canReadExecutingUnit(executingUnitId: number | null | undefined): boolean {
    return this.hasGrantForExecutingUnit(executingUnitId);
  }

  canAdministerExecutingUnit(executingUnitId: number | null | undefined): boolean {
    return this.hasGrantForExecutingUnit(executingUnitId, 'ADMINISTRADOR_PIIP');
  }

  hasAnyAdministratorScope(): boolean {
    return this.currentUser()?.roleScopes.some((scope) => scope.role === 'ADMINISTRADOR_PIIP') ?? false;
  }

  effectiveRoleForExecutingUnit(executingUnitId: number | null | undefined): UserRole | null {
    if (this.canAdministerExecutingUnit(executingUnitId)) return 'Administrador PIIP';
    return this.hasGrantForExecutingUnit(executingUnitId, 'CONSULTA_EXTERNA') ? 'Consulta externa' : null;
  }

  async selectExecutingUnit(executingUnitId: number): Promise<void> {
    if (!this.executingUnits().some((unit) => unit.id === executingUnitId)) {
      throw new PiipApiError(403, 'La Unidad Ejecutora no pertenece a los ambitos autorizados.');
    }
    this.portfolioRecords.set([]);
    this.initiatives.set([]);
    this.projects.set([]);
    this.documentDossiers.set([]);
    this.documentDossierSummaries.set([]);
    this.eligibleInitiatives.set([]);
    this.catalogStore.clearOrganizationalUnits();
    this.selectedExecutingUnitId.set(executingUnitId);
    if (!this.canAdministerExecutingUnit(executingUnitId)) this.administrableScopes.set([]);
    localStorage.setItem('piip-selected-executing-unit', String(executingUnitId));
    await Promise.all([this.loadOrganizationalUnits(executingUnitId), this.refreshAll()]);
  }

  toggleRole(): void {}

  getDocumentDossier(recordType: PiipRecordType, code: string): DocumentDossier | undefined {
    const dossier = this.documentDossiers().find((item) => item.recordType === recordType && item.code === code);
    if (!dossier && code) void this.loadDocuments(recordType, code).catch((error) => this.captureError(error));
    return dossier;
  }

  getDocumentDossierSummaries(): DocumentDossierSummary[] {
    return this.documentDossierSummaries();
  }

  getInitiativeDetail(code: string): InitiativeDetail | undefined {
    const initiative = this.initiatives().find((item) => item.code === code);
    const portfolioRecord = this.portfolioRecords().find((item) => item.recordType === 'Iniciativa' && item.code === code);
    if (!initiative || !portfolioRecord) {
      void this.loadPortfolioRecord('Iniciativa', code).catch((error) => this.captureError(error));
      return undefined;
    }
    return {
      initiative,
      portfolioRecord,
      dossier: this.getDocumentDossier('Iniciativa', code),
      derivedProject: this.getProjectByOrigin(code),
    };
  }

  getProjectDetail(code: string): ProjectDetail | undefined {
    const project = this.projects().find((item) => item.code === code);
    const portfolioRecord = this.portfolioRecords().find((item) => item.recordType === 'Proyecto' && item.code === code);
    if (!project || !portfolioRecord) {
      void this.loadPortfolioRecord('Proyecto', code).catch((error) => this.captureError(error));
      return undefined;
    }
    return {
      project,
      portfolioRecord,
      dossier: this.getDocumentDossier('Proyecto', code),
      originInitiative: project.originMode === 'DERIVED_FROM_INITIATIVE' ? this.initiatives().find((item) => item.code === project.originCode) : undefined,
    };
  }

  async reloadPortfolioRecord(recordType: PiipRecordType, code: string): Promise<void> {
    await this.loadPortfolioRecord(recordType, code, true);
  }

  getProjectByOrigin(initiativeCode: string): ProjectRecord | undefined {
    return this.projects().find((project) => project.originMode === 'DERIVED_FROM_INITIATIVE' && project.originCode === initiativeCode);
  }

  getInitiativesEligibleForProject(): InitiativeRecord[] {
    return this.eligibleInitiatives();
  }

  getNextProjectCode(initiativeCode: string): string {
    const year = initiativeCode.match(/(\d{4})$/)?.[1] ?? String(new Date().getFullYear());
    const maximum = this.projects().reduce((current, project) => Math.max(current, Number(project.code.match(/^P-(\d+)-/)?.[1] ?? 0)), 0);
    return `P-${String(maximum + 1).padStart(3, '0')}-${year}`;
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

  async registerInitiative(input: InitiativeInput): Promise<PiipPortfolioRecord> {
    const executingUnitId = this.requireSelectedExecutingUnit();
    this.requireAdministratorForExecutingUnit(executingUnitId);
    const request: InitiativeCreateRequest = {
      executingUnitId,
      startDate: input.startDate,
      name: input.name,
      solutionTypeId: input.solutionTypeId,
      sourceId: input.sourceId,
      responsible: input.responsible,
      responsibleUnits: [{ organizationalUnitId: input.organizationalUnitId }],
      peiObjectiveId: input.peiObjectiveId,
      poiActivityId: input.poiActivityId,
      description: input.description,
      note: input.note,
      digitalComponent: input.digitalComponent === 'Si' ? 'YES' : 'NO',
    };
    const created = await this.request(this.portfolio.createInitiative({ body: request })) as unknown as ApiPortfolioRecord;
    if (input.initialFile) {
      await this.uploadDocument(created.code, this.requireDocumentTypeId('PUBLIC_INNOVATION_INITIATIVE_SHEET'), input.initialFile);
    }
    await this.refreshAll();
    return toPortfolioRecord(created);
  }

  async approveInitiative(input: InitiativeDecisionInput): Promise<PiipPortfolioRecord> {
    this.requireAdministratorForExecutingUnit(this.executingUnitIdForRecord(input.initiativeCode));
    const version = this.recordVersions.get(input.initiativeCode);
    if (version === undefined) throw new PiipApiError(409, 'No se encontró la versión vigente de la iniciativa. Recarga el expediente.');
    const request: ApprovalRequest = {
      version,
      observation: input.observation,
    };
    const record = await this.request(this.http.post<ApiPortfolioRecord>(`${this.apiUrl}/initiatives/${input.initiativeCode}/approval`, request));
    await this.refreshAll();
    await this.loadDocuments('Iniciativa', input.initiativeCode);
    return toPortfolioRecord(record);
  }

  async transitionInitiativeStatus(input: InitiativeStatusTransitionInput): Promise<PiipPortfolioRecord> {
    this.requireAdministratorForExecutingUnit(this.executingUnitIdForRecord(input.initiativeCode));
    const version = this.recordVersions.get(input.initiativeCode);
    if (version === undefined) throw new PiipApiError(409, 'No se encontró la versión vigente de la iniciativa. Recarga el expediente.');
    const request: InitiativeStatusTransitionRequest = {
      version,
      targetStatus: portfolioStatusCode(input.targetStatus) as InitiativeStatusTransitionRequest['targetStatus'],
      observation: input.observation,
    };
    const record = await this.request(this.portfolio.transitionInitiative({ code: input.initiativeCode, body: request })) as unknown as ApiPortfolioRecord;
    this.recordVersions.set(record.code, record.version);
    await this.refreshAll();
    return toPortfolioRecord(record);
  }

  async transitionProjectStatus(input: ProjectStatusTransitionInput): Promise<PiipPortfolioRecord> {
    this.requireAdministratorForExecutingUnit(this.executingUnitIdForRecord(input.projectCode));
    const version = this.recordVersions.get(input.projectCode);
    if (version === undefined) throw new PiipApiError(409, 'No se encontró la versión vigente del proyecto. Recarga el expediente.');
    const request: ProjectStatusTransitionRequest = {
      version,
      targetStatus: portfolioStatusCode(input.targetStatus) as ProjectStatusTransitionRequest['targetStatus'],
      observation: input.observation,
    };
    const record = await this.request(this.portfolio.transitionProject({ code: input.projectCode, body: request })) as unknown as ApiPortfolioRecord;
    this.recordVersions.set(record.code, record.version);
    await this.refreshAll();
    return toPortfolioRecord(record);
  }

  async registerDerivedProject(input: DerivedProjectInput): Promise<PiipPortfolioRecord> {
    this.requireAdministratorForExecutingUnit(this.executingUnitIdForRecord(input.initiativeCode));
    const request: DerivedProjectRequest = {
      initiativeCode: input.initiativeCode,
      startDate: input.startDate,
      name: input.name,
      solutionTypeId: input.solutionTypeId,
      sourceId: input.sourceId,
      responsible: input.responsible,
      responsibleUnits: [{ organizationalUnitId: input.organizationalUnitId }],
      peiObjectiveId: input.peiObjectiveId,
      poiActivityId: input.poiActivityId,
      description: input.description,
      keyResults: input.keyResults,
      note: input.note,
      digitalComponent: input.digitalComponent === 'Si' ? 'YES' : 'NO',
    };
    const record = await this.request(this.portfolio.derived({ body: request })) as unknown as ApiPortfolioRecord;
    await this.refreshAll();
    return toPortfolioRecord(record);
  }

  async registerPreexistingProject(input: PreexistingProjectInput): Promise<PiipPortfolioRecord> {
    this.requireAdministratorForExecutingUnit(this.requireSelectedExecutingUnit());
    const request: PreexistingProjectRequest = {
      executingUnitId: this.requireSelectedExecutingUnit(),
      startDate: input.startDate,
      name: input.name,
      sourceId: input.sourceId,
      responsible: input.responsible,
      responsibleUnits: [{ organizationalUnitId: input.organizationalUnitId }],
      peiObjectiveId: input.peiObjectiveId,
      poiActivityId: input.poiActivityId,
      description: input.description,
      keyResults: input.keyResults,
      note: input.note,
      digitalComponent: input.digitalComponent === 'Si' ? 'YES' : 'NO',
    };
    const record = await this.request(this.portfolio.preexisting({ body: request })) as unknown as ApiPortfolioRecord;
    for (const attachment of input.documentAttachments ?? []) {
      if (attachment.mode === 'FILE' && attachment.file) await this.uploadDocument(record.code, attachment.documentTypeId, attachment.file);
      if (attachment.mode === 'NOT_APPLICABLE') await this.markDocumentNotApplicable(record.code, attachment.documentTypeId, 'Proyecto preexistente');
    }
    await this.refreshAll();
    return toPortfolioRecord(record);
  }

  async updateInitiative(code: string, input: InitiativeUpdateInput): Promise<PiipPortfolioRecord> {
    const body: InitiativeUpdateRequest = {
      ...updateFields(input),
      responsibleUnits: input.responsibleUnitIds?.map((organizationalUnitId) => ({ organizationalUnitId })),
      version: input.version,
    } as InitiativeUpdateRequest;
    const response = await this.request(this.portfolio.updateInitiative({ code, body })) as unknown as ApiPortfolioRecord;
    this.upsertUpdatedRecord(response);
    await this.refreshAuditAfterUpdate();
    return toPortfolioRecord(response);
  }

  async updateProject(code: string, input: ProjectUpdateInput): Promise<PiipPortfolioRecord> {
    const body: ProjectUpdateRequest = {
      ...updateFields(input),
      keyResults: input.keyResults,
      responsibleUnits: input.responsibleUnitIds?.map((organizationalUnitId) => ({ organizationalUnitId })),
      version: input.version,
    } as ProjectUpdateRequest;
    const response = await this.request(this.portfolio.updateProject({ code, body })) as unknown as ApiPortfolioRecord;
    this.upsertUpdatedRecord(response);
    await this.refreshAuditAfterUpdate();
    return toPortfolioRecord(response);
  }

  private async refreshAuditAfterUpdate(): Promise<void> {
    if (this.hasAnyAdministratorScope()) await this.loadAudit();
  }

  private upsertUpdatedRecord(response: ApiPortfolioRecord): void {
    const record = toPortfolioRecord(response);
    this.recordVersions.set(response.code, response.version);
    this.portfolioRecords.update((items) => upsertByCode(items, record));
    if (record.recordType === 'Iniciativa') {
      this.initiatives.update((items) => upsertByCode(items, toInitiativeRecord(response)));
    } else {
      this.projects.update((items) => upsertByCode(items, toProjectRecord(response)));
    }
  }

  async uploadDocument(code: string, documentTypeId: number, file: File): Promise<void> {
    await this.request(this.documentApi.upload({ recordCode: code, documentTypeId, body: { file } }));
    const recordType = this.portfolioRecords().find((record) => record.code === code)?.recordType;
    if (recordType) await this.loadDocuments(recordType, code);
    await this.loadDocumentSummaries();
  }

  async markDocumentNotApplicable(code: string, documentTypeId: number, reason: string): Promise<void> {
    await this.request(this.documentApi.notApplicable({ recordCode: code, documentTypeId, body: { reason } }));
    const recordType = this.portfolioRecords().find((record) => record.code === code)?.recordType;
    if (recordType) await this.loadDocuments(recordType, code);
    await this.loadDocumentSummaries();
  }

  async downloadDocument(code: string, versionId: number, filename: string): Promise<void> {
    const blob = await this.request(this.http.get(`${this.apiUrl}/portfolio-records/${code}/documents/versions/${versionId}/content`, { responseType: 'blob' }));
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  async setDocumentPublication(code: string, versionId: number, published: boolean, version: number): Promise<void> {
    await this.request(this.http.put(`${this.apiUrl}/portfolio-records/${code}/documents/versions/${versionId}/publication`, null, {
      params: { published, version },
    }));
    const recordType = this.portfolioRecords().find((record) => record.code === code)?.recordType;
    if (recordType) await this.loadDocuments(recordType, code);
  }

  async markNotificationRead(id: number): Promise<void> {
    await this.request(this.http.put(`${this.apiUrl}/notifications/${id}/read`, null));
    this.notifications.update((items) => items.map((item) => item.id === id ? { ...item, read: true } : item));
  }

  async refreshNotifications(): Promise<void> {
    this.notificationsLoading.set(true);
    this.notificationsError.set(null);
    try {
      this.notifications.set(await this.request(this.http.get<NotificationItem[]>(`${this.apiUrl}/notifications`)));
    } catch (error) {
      this.notificationsError.set(error instanceof Error ? error.message : 'No fue posible cargar las notificaciones.');
    } finally {
      this.notificationsLoading.set(false);
    }
  }

  async loadHomePortfolio(query: HomePortfolioQuery): Promise<void> {
    const requestId = ++this.homePortfolioRequestId;
    this.homePortfolioLoading.set(true);
    this.homePortfolioError.set(null);
    const params: Parameters<DashboardControllerService['portfolio']>[0] = {
      executingUnitId: query.executingUnitId,
      q: query.q.trim() || undefined,
      type: query.type === 'Todos' ? undefined : query.type === 'Iniciativa' ? 'INITIATIVE' : 'PROJECT',
      status: query.status === 'Todos' ? undefined : portfolioStatusCode(query.status) as NonNullable<Parameters<DashboardControllerService['portfolio']>[0]>['status'],
      page: query.page,
      size: query.size,
    };
    try {
      const response = await this.request(this.dashboard.portfolio(params));
      const result: HomePortfolioResult = {
        content: (response.content ?? []).flatMap((item): HomePortfolioItem[] => {
          const recordType = item.recordType === 'Iniciativa' || item.recordType === 'Proyecto' ? item.recordType : null;
          const status = item.status as PiipStatus | undefined;
          return recordType && status && item.code && item.name
            ? [{ recordType, code: item.code, name: item.name, status, executingUnitId: item.executingUnitId ?? query.executingUnitId, executingUnit: item.executingUnit ?? '', updatedAt: item.updatedAt ?? '' }]
            : [];
        }),
        page: response.page ?? 0,
        size: response.size ?? query.size,
        totalElements: response.totalElements ?? 0,
        totalPages: response.totalPages ?? 0,
        executingUnitTotalElements: response.executingUnitTotalElements ?? 0,
        statusCounts: (response.statusCounts ?? []).flatMap((item): HomePortfolioStatusCount[] =>
          item.status && item.count !== undefined ? [{ status: item.status as PiipStatus, count: item.count }] : []),
      };
      if (requestId === this.homePortfolioRequestId) this.homePortfolio.set(result);
    } catch (error) {
      if (requestId === this.homePortfolioRequestId) {
        this.homePortfolioError.set(error instanceof Error ? error.message : 'No fue posible cargar el portafolio.');
      }
    } finally {
      if (requestId === this.homePortfolioRequestId) this.homePortfolioLoading.set(false);
    }
  }

  private async loadIdentity(): Promise<void> {
    const response = await this.request(this.http.get<CurrentUserResponse>(`${this.apiUrl}/identity/me`));
    this.currentUser.set({
      subject: response.subject ?? '',
      fullName: response.fullName ?? '',
      email: response.email ?? '',
      roleScopes: (response.roleScopes ?? []).flatMap((scope) =>
        scope.role && scope.institutionId !== undefined
          ? [{ role: scope.role, institutionId: scope.institutionId, executingUnitId: scope.executingUnitId ?? null }]
          : [],
      ),
      roles: response.roles ?? [],
      institutionIds: response.institutionIds ?? [],
      executingUnitIds: response.executingUnitIds ?? [],
      institutionWide: response.institutionWide ?? false,
    });
  }

  private async loadExecutingUnits(): Promise<void> {
    this.executingUnits.set(await this.request(this.http.get<ExecutingUnit[]>(`${this.apiUrl}/executing-units`)));
  }

  private async restoreExecutingUnit(): Promise<void> {
    const storedValue = localStorage.getItem('piip-selected-executing-unit');
    const stored = storedValue === null ? null : Number(storedValue);
    await this.reconcileExecutingUnit(stored !== null && Number.isFinite(stored) ? stored : null);
  }

  private async reconcileExecutingUnit(preferredExecutingUnitId: number | null): Promise<void> {
    const units = this.executingUnits();
    const selected = preferredExecutingUnitId !== null && units.some((unit) => unit.id === preferredExecutingUnitId)
      ? preferredExecutingUnitId
      : units[0]?.id ?? null;
    this.selectedExecutingUnitId.set(selected);
    if (selected === null) {
      this.catalogStore.clearOrganizationalUnits();
      localStorage.removeItem('piip-selected-executing-unit');
      return;
    }
    localStorage.setItem('piip-selected-executing-unit', String(selected));
    await this.loadOrganizationalUnits(selected);
  }

  private async loadOrganizationalUnits(executingUnitId: number): Promise<void> {
    await this.catalogStore.loadOrganizationalUnits(executingUnitId, async () => {
      const values = await this.request(this.http.get<Array<Partial<OrganizationalUnit>>>(`${this.apiUrl}/organizational-units`, { params: { executingUnitId } }));
      return values.flatMap((value): OrganizationalUnit[] => value.id !== undefined && value.code && value.name
        ? [{ id: value.id, code: value.code, name: value.name, acronym: value.acronym ?? '', parentId: value.parentId ?? null, executingUnitId: value.executingUnitId ?? executingUnitId, active: value.active ?? false }]
        : []);
    });
  }

  private async loadPortfolio(): Promise<void> {
    let params = new HttpParams().set('size', 100);
    if (this.selectedExecutingUnitId() !== null) params = params.set('executingUnitId', this.selectedExecutingUnitId()!);
    const [initiativePage, projectPage] = await Promise.all([
      this.request(this.http.get<ApiPage<ApiPortfolioRecord>>(`${this.apiUrl}/initiatives`, { params })),
      this.request(this.http.get<ApiPage<ApiPortfolioRecord>>(`${this.apiUrl}/projects`, { params })),
    ]);
    const initiativeRecords = initiativePage.content.map(toPortfolioRecord);
    const projectRecords = projectPage.content.map(toPortfolioRecord);
    [...initiativePage.content, ...projectPage.content].forEach((record) => this.recordVersions.set(record.code, record.version));
    this.portfolioRecords.set([...initiativeRecords, ...projectRecords]);
    this.initiatives.set(initiativePage.content.map(toInitiativeRecord));
    this.projects.set(projectPage.content.map(toProjectRecord));
  }

  private async loadEligibleInitiatives(): Promise<void> {
    const records = await this.request(this.http.get<ApiPortfolioRecord[]>(`${this.apiUrl}/projects/eligible-initiatives`));
    const selected = this.selectedExecutingUnitId();
    this.eligibleInitiatives.set(records.filter((record) => selected === null || record.executingUnitId === selected).map(toInitiativeRecord));
  }

  private async loadDocumentSummaries(): Promise<void> {
    const selectedExecutingUnitId = this.selectedExecutingUnitId();
    let params = new HttpParams();
    if (selectedExecutingUnitId !== null) params = params.set('executingUnitId', selectedExecutingUnitId);
    const items = await this.request(this.http.get<DossierSummary[]>(`${this.apiUrl}/documents`, { params }));
    this.documentDossierSummaries.set(items.flatMap((item): DocumentDossierSummary[] => {
      if (!item.code || !item.name || !item.recordType || !item.status) return [];
      if (selectedExecutingUnitId !== null && item.executingUnitId !== selectedExecutingUnitId) return [];
      return [{ recordType: item.recordType as PiipRecordType, code: item.code, name: item.name, unit: item.unit ?? '', status: item.status as PiipStatus,
        loadedCount: item.loadedCount ?? 0, pendingCount: item.pendingCount ?? 0, notApplicableCount: item.notApplicableCount ?? 0,
        lastActivity: item.lastActivity ? formatDate(item.lastActivity) : '', executingUnitId: item.executingUnitId,
        organizationalUnits: (item.organizationalUnits ?? []).flatMap(mapOrganizationalUnit) }];
    }));
  }

  private async loadDocuments(recordType: PiipRecordType, code: string): Promise<void> {
    const items = await this.request(this.http.get<ApiDocument[]>(`${this.apiUrl}/portfolio-records/${code}/documents`));
    const record = this.portfolioRecords().find((candidate) => candidate.code === code)
      ?? await this.loadPortfolioRecord(recordType, code);
    if (!record) return;
    const dossier: DocumentDossier = {
      recordType,
      code,
      name: record.name,
      unit: record.responsibleUnits,
      status: record.status,
      lastActivity: formatDate(new Date().toISOString()),
      executingUnitId: record.executingUnitId,
      stages: [
        { title: '1. Registro inicial', records: mapDocuments(items, ['PUBLIC_INNOVATION_INITIATIVE_SHEET']) },
        { title: '2. Evaluación', records: mapDocuments(items, ['INITIATIVE_TECHNICAL_OPINION']) },
        { title: '3. Decisión', records: mapDocuments(items, ['FORMAL_APPROVAL_DECISION']) },
        { title: '4. Etapas posteriores', records: mapDocuments(items, ['FINAL_PRODUCT_APPROVAL', 'PROJECT_MANAGEMENT_DOCUMENTATION', 'FINAL_CLOSURE_REPORT']) },
      ],
    };
    this.documentDossiers.update((values) => [dossier, ...values.filter((value) => value.code !== code)]);
  }

  private async loadAudit(): Promise<void> {
    const selectedExecutingUnitId = this.selectedExecutingUnitId();
    let params = new HttpParams();
    if (selectedExecutingUnitId !== null) params = params.set('executingUnitId', selectedExecutingUnitId);
    const [items, accesses] = await Promise.all([
      this.request(this.http.get<EventResponse[]>(`${this.apiUrl}/audit/events`, { params })),
      this.request(this.http.get<AuditAccess[]>(`${this.apiUrl}/audit/accesses`, { params })),
    ]);
    const visibleCodes = new Set(this.portfolioRecords().map((record) => record.code));
    const scopedItems = selectedExecutingUnitId === null
      ? items
      : items.filter((item) => !item.entityCode || visibleCodes.has(item.entityCode));
    const scopedAccesses = selectedExecutingUnitId === null
      ? accesses
      : accesses.filter((access) => !access.recordCode || visibleCodes.has(access.recordCode));
    this.auditEvents.set(scopedItems.map((item) => ({
      recordCode: item.entityCode,
      timestamp: item.occurredAt ? formatDate(item.occurredAt) : 'Fecha no registrada',
      event: item.event ?? 'EVENTO_REGISTRADO',
      user: item.actorName || 'Usuario no identificado',
      email: item.actorEmail ?? '',
      observation: item.detail ?? '',
      actorSubject: item.actor,
      rawDetail: item.detail ?? '',
      icon: 'history',
    })));
    this.auditAccesses.set(scopedAccesses);
  }

  private async loadPortfolioRecord(recordType: PiipRecordType, code: string, force = false): Promise<PiipPortfolioRecord | undefined> {
    const existing = this.portfolioRecords().find((candidate) => candidate.code === code);
    if ((!force && existing) || !code || this.loadingRecordCodes.has(code)) return existing;
    this.loadingRecordCodes.add(code);
    try {
      const path = recordType === 'Iniciativa' ? 'initiatives' : 'projects';
      const response = await this.request(this.http.get<ApiPortfolioRecord>(`${this.apiUrl}/${path}/${code}`));
      const portfolioRecord = toPortfolioRecord(response);
      this.recordVersions.set(response.code, response.version);
      this.portfolioRecords.update((items) => [portfolioRecord, ...items.filter((item) => item.code !== code)]);
      if (recordType === 'Iniciativa') {
        this.initiatives.update((items) => [toInitiativeRecord(response), ...items.filter((item) => item.code !== code)]);
      } else {
        this.projects.update((items) => [toProjectRecord(response), ...items.filter((item) => item.code !== code)]);
      }
      return portfolioRecord;
    } finally {
      this.loadingRecordCodes.delete(code);
    }
  }

  private async loadWorkItems(): Promise<void> {
    const items = await this.request(this.http.get<ApiWorkTask[]>(`${this.apiUrl}/work-tasks`));
    this.workItems.set(items.map((item) => ({
      id: item.id,
      code: item.recordCode,
      action: item.description,
      assignedTo: item.assignedTo,
      priority: item.priority === 'HIGH' ? 'Alta' : item.priority === 'MEDIUM' ? 'Media' : 'Baja',
      dueDate: item.dueDate ? formatDateOnly(item.dueDate) : null,
      alert: item.alert,
      version: item.version,
    })));
  }

  private async loadNotifications(): Promise<void> {
    await this.refreshNotifications();
  }

  private async loadDashboard(): Promise<void> {
    const summary = await this.request(this.dashboard.summary());
    this.dashboardSummary.set({ ...emptyDashboard(), ...summary });
  }

  private requireSelectedExecutingUnit(): number {
    const value = this.selectedExecutingUnitId();
    if (value === null) throw new PiipApiError(422, 'Selecciona una Unidad Ejecutora antes de registrar.');
    return value;
  }

  private requireDocumentTypeId(code: DocumentType): number {
    const option = this.catalogs().value.documentTypes.find((item) => item.code === code && item.active);
    if (!option) throw new PiipApiError(422, 'El tipo documental requerido ya no está disponible. Recarga los catálogos.');
    return option.id;
  }

  private executingUnitIdForRecord(code: string): number | undefined {
    return this.portfolioRecords().find((record) => record.code === code)?.executingUnitId;
  }

  private requireAdministratorForExecutingUnit(executingUnitId: number | null | undefined): void {
    if (!this.canAdministerExecutingUnit(executingUnitId)) {
      throw new PiipApiError(403, 'No tienes permisos de Administrador PIIP para la Unidad Ejecutora del registro.');
    }
  }

  private hasGrantForExecutingUnit(executingUnitId: number | null | undefined, role?: UserRoleCode): boolean {
    if (executingUnitId == null) return false;
    const unit = this.executingUnits().find((candidate) => candidate.id === executingUnitId);
    if (!unit) return false;
    return this.currentUser()?.roleScopes.some((scope) =>
      (!role || scope.role === role)
      && scope.institutionId === unit.institutionId
      && (scope.executingUnitId === null || scope.executingUnitId === executingUnitId),
    ) ?? false;
  }

  private async request<T>(request: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(request) as T;
    } catch (error) {
      throw await this.captureHttpError(error);
    }
  }

  private async captureHttpError(error: unknown): Promise<PiipApiError> {
    if (!(error instanceof HttpErrorResponse) || !isBlobLike(error.error)) return this.captureError(error);
    const text = await error.error.text();
    let problem: ApiProblem | string = text;
    if (text.trim()) {
      try { problem = JSON.parse(text) as ApiProblem; } catch { /* texto no estructurado: usar fallback seguro */ }
    }
    return this.captureProblem(error.status, problem);
  }

  private captureError(error: unknown): PiipApiError {
    if (error instanceof PiipApiError) {
      this.lastError.set(error.message);
      return error;
    }
    if (error instanceof HttpErrorResponse) {
      return this.captureProblem(error.status, error.error as ApiProblem | string | null);
    }
    const apiError = new PiipApiError(0, error instanceof Error ? error.message : 'No fue posible comunicarse con el backend PIIP.');
    this.lastError.set(apiError.message);
    return apiError;
  }

  private captureProblem(status: number, problem: ApiProblem | string | null): PiipApiError {
    const detail = typeof problem === 'object' && problem?.detail ? problem.detail : typeof problem === 'string' && problem.trim() ? problem : undefined;
    const problemCode = typeof problem === 'object' && problem?.problemCode ? problem.problemCode : undefined;
    const message = problemCode
      ? problemMessage(problemCode) ?? detail ?? httpStatusMessage(status)
      : detail ?? httpStatusMessage(status);
    const apiError = new PiipApiError(status, message, problemCode);
    this.lastError.set(apiError.message);
    return apiError;
  }
}

async function readGeneratedBody<T>(body: T | Blob | null | undefined): Promise<T | undefined> {
  if (body === null || body === undefined) return undefined;
  if (!isBlobLike(body)) return body as T;
  const text = await body.text();
  if (!text.trim()) return undefined;
  return JSON.parse(text) as T;
}

function isBlobLike(value: unknown): value is Blob {
  return value instanceof Blob || (typeof value === 'object' && value !== null && typeof (value as { text?: unknown }).text === 'function');
}

async function readGeneratedList<T>(value: T[] | Blob): Promise<T[]> {
  const parsed = await readGeneratedBody<T[]>(value);
  return Array.isArray(parsed) ? parsed : [];
}

function mapAdministrationUser(value: any): UserAdministrationUser[] {
  if (!value || value.id === undefined || !value.subject) return [];
  return [{
    id: value.id,
    subject: value.subject,
    fullName: value.fullName ?? value.subject,
    email: value.email ?? '',
    scopes: (value.scopes ?? []).flatMap((scope: unknown) => {
      const mapped = mapScope(scope);
      return mapped ? [mapped] : [];
    }),
  }];
}

function mapAssignmentCandidate(value: any): UserAssignmentCandidate[] {
  if (!value || value.id === undefined || !value.subject) return [];
  return [{ id: value.id, subject: value.subject, fullName: value.fullName ?? value.subject, email: value.email ?? '' }];
}

function mapScope(value: any): UserAssignmentScope | undefined {
  if (!value || value.id === undefined || !value.role || value.institutionId === undefined || value.version === undefined) return undefined;
  return {
    id: value.id,
    role: value.role,
    institutionId: value.institutionId,
    institution: value.institution ?? 'Institución no disponible',
    executingUnitId: value.executingUnitId ?? undefined,
    executingUnit: value.executingUnit ?? 'Toda la institución',
    active: value.active === true,
    validFrom: value.validFrom,
    validUntil: value.validUntil,
    version: value.version,
  };
}

function problemMessage(problemCode: string): string | undefined {
  const messages: Record<string, string> = {
    INVALID_REQUEST: 'La solicitud no cumple el contrato esperado.',
    FORBIDDEN_SCOPE: 'No tienes autorización sobre el ámbito solicitado.',
    RESOURCE_NOT_FOUND: 'La asignación o el usuario indicado no existe.',
    STALE_VERSION: 'La información cambió. Actualiza la pantalla antes de volver a intentarlo.',
    ACTIVE_ASSIGNMENT_DUPLICATE: 'El usuario ya cuenta con una asignación activa igual.',
    SELF_ADMIN_SUSPENSION: 'No puedes suspender tu propia asignación de Administrador PIIP.',
    LAST_ACTIVE_ADMIN: 'La operación dejaría una Unidad Ejecutora sin administrador activo.',
    INCOMPATIBLE_ASSIGNMENT_STATE: 'La asignación no se encuentra en un estado compatible con la operación.',
    INVALID_ACTIVE_REFERENCE: 'La referencia de institución o Unidad Ejecutora ya no está activa.',
    BUSINESS_RULE_VIOLATION: 'La operación no cumple una regla de negocio.',
  };
  return messages[problemCode];
}

export function resolveApiUrl(): string {
  return runtimeApiUrl();
}

function updateFields(input: InitiativeUpdateInput | ProjectUpdateInput): Record<string, unknown> {
  return {
    name: input.name,
    solutionTypeId: input.solutionTypeId,
    sourceId: input.sourceId,
    startDate: input.startDate,
    responsible: input.responsible,
    peiObjectiveId: input.peiObjectiveId,
    poiActivityId: input.poiActivityId,
    description: input.description,
    note: input.note,
    digitalComponent: input.digitalComponent === undefined
      ? undefined
      : input.digitalComponent === 'Si' ? 'YES' : 'NO',
  };
}

function upsertByCode<T extends { code: string }>(items: T[], value: T): T[] {
  const index = items.findIndex((item) => item.code === value.code);
  if (index < 0) return [...items, value];
  const next = [...items];
  next[index] = value;
  return next;
}

function portfolioStatusCode(status: PiipPortfolioRecord['status']): string {
  const codes: Record<string, string> = {
    'Presentado': 'PRESENTED', 'Iniciativa aprobada': 'INITIATIVE_APPROVED', 'Iniciativa archivada': 'INITIATIVE_ARCHIVED',
    'Proyecto en ejecución': 'PROJECT_IN_PROGRESS', 'Producto aprobado': 'PRODUCT_APPROVED', 'Producto no aprobado': 'PRODUCT_NOT_APPROVED',
    Suspendido: 'SUSPENDED', Cancelado: 'CANCELLED', Finalizado: 'FINISHED', 'No Aplicable': 'NOT_APPLICABLE', 'No Admisible': 'NOT_ADMISSIBLE',
  };
  return codes[status] ?? status;
}

function toPortfolioRecord(value: ApiPortfolioRecord): PiipPortfolioRecord {
  const recordTypeReference = mapTechnicalOption(value.recordType);
  const solutionTypeReference = mapPersistentOption(value.solutionType);
  const sourceReference = mapPersistentOption(value.source);
  const peiObjectiveReference = value.peiObjective ? mapPersistentOption(value.peiObjective) : null;
  const poiActivityReference = value.poiActivity ? mapPersistentOption(value.poiActivity) : null;
  const responsibleUnitReferences = value.responsibleUnits.flatMap((item) => item.organizationalUnit ? mapOrganizationalUnit(item.organizationalUnit) : []);
  return {
    recordType: recordTypeReference.name,
    code: value.code,
    originCode: value.originCode,
    name: value.name,
    solutionType: solutionTypeReference.name as PiipPortfolioRecord['solutionType'],
    source: sourceReference.name,
    startDate: value.startDate,
    responsible: value.responsible,
    peiObjective: peiObjectiveReference?.name ?? '',
    poiActivity: poiActivityReference?.name ?? '',
    responsibleUnits: responsibleUnitReferences.map((item) => item.acronym || item.name).join(', '),
    description: value.description,
    keyResults: value.keyResults ?? '',
    note: value.note ?? '',
    status: value.status,
    finalProductType: value.finalProductType,
    digitalComponent: value.digitalComponent,
    closingDate: value.closingDate ?? '',
    technicalOpinionReport: value.technicalOpinionReport ?? '',
    formalApprovalDecision: value.formalApprovalDecision ?? '',
    finalProductApprovalDocument: value.finalProductApprovalDocument ?? '',
    projectManagementDocumentation: value.projectManagementDocumentation ?? '',
    finalClosureReport: value.finalClosureReport ?? '',
    executingUnitId: value.executingUnitId,
    executingUnit: value.executingUnit,
    version: value.version,
    updatedAt: value.updatedAt,
    recordTypeReference, solutionTypeReference, sourceReference, peiObjectiveReference, poiActivityReference,
    responsibleUnitReferences,
  };
}

function toInitiativeRecord(value: ApiPortfolioRecord): InitiativeRecord {
  const organizationalUnits = value.responsibleUnits.flatMap((item) => item.organizationalUnit ? mapOrganizationalUnit(item.organizationalUnit) : []);
  const sourceReference = mapPersistentOption(value.source);
  return {
    code: value.code,
    name: value.name,
    source: sourceReference.name,
    responsible: value.responsible,
    role: '',
    unit: value.responsibleUnits.flatMap((item) => item.organizationalUnit?.name ?? []).join(', '),
    status: value.status,
    updatedAt: formatDate(value.updatedAt),
    executingUnitId: value.executingUnitId,
    sourceReference,
    organizationalUnits,
  };
}

function toProjectRecord(value: ApiPortfolioRecord): ProjectRecord {
  const organizationalUnits = value.responsibleUnits.flatMap((item) => item.organizationalUnit ? mapOrganizationalUnit(item.organizationalUnit) : []);
  return {
    code: value.code,
    name: value.name,
    originCode: value.originCode,
    originMode: value.originCode === 'NA' ? 'PREEXISTING' : 'DERIVED_FROM_INITIATIVE',
    unit: value.responsibleUnits.flatMap((item) => item.organizationalUnit?.name ?? []).join(', '),
    responsible: value.responsible,
    status: value.status,
    digitalComponent: value.digitalComponent,
    executingUnitId: value.executingUnitId,
    organizationalUnits,
  };
}

function mapDocuments(items: ApiDocument[], types: DocumentType[]): DocumentRecord[] {
  return items.filter((item) => types.includes(item.documentType.code as DocumentType)).map((item) => {
    const documentType = mapPersistentOption(item.documentType);
    const version = item.versions[0];
    return {
      type: documentType.code as DocumentType,
      documentTypeId: documentType.id,
      documentType,
      name: documentType.name,
      required: false,
      filename: version?.filename ?? null,
      version: version ? `${version.version}.0` : null,
      uploadedAt: version ? formatDate(version.uploadedAt) : null,
      state: item.state === 'LOADED' ? 'Cargado' : item.state === 'NOT_APPLICABLE' ? 'No aplica' : 'Pendiente',
      versionId: version?.id,
      optimisticVersion: version?.optimisticVersion,
      externallyPublished: version?.externallyPublished,
    };
  });
}

function mapCatalogBundle(value: import('../api/generated/models').CatalogBundleResponse): CatalogBundle {
  return {
    recordTypes: (value.recordTypes ?? []).map(mapTechnicalOption),
    solutionTypes: (value.solutionTypes ?? []).map(mapPersistentOption),
    sources: (value.sources ?? []).map(mapPersistentOption),
    peiObjectives: (value.peiObjectives ?? []).map(mapPersistentOption),
    poiActivities: (value.poiActivities ?? []).map(mapPersistentOption),
    documentTypes: (value.documentTypes ?? []).map(mapPersistentOption),
  };
}

function mapPersistentOption(value: PersistentCatalogItemResponse): PersistentCatalogOption {
  if (value.id === undefined || !value.code || !value.name || value.displayOrder === undefined || value.active === undefined) {
    throw new PiipApiError(502, 'El backend devolvió una opción de catálogo incompleta.');
  }
  return { id: value.id, code: value.code, name: value.name, displayOrder: value.displayOrder, active: value.active };
}

function mapTechnicalOption(value: TechnicalCatalogItemResponse): TechnicalCatalogOption {
  if ((value.code !== 'INITIATIVE' && value.code !== 'PROJECT') || (value.name !== 'Iniciativa' && value.name !== 'Proyecto') || value.displayOrder === undefined || value.active === undefined) {
    throw new PiipApiError(502, 'El backend devolvió un tipo de registro incompleto.');
  }
  return { code: value.code, name: value.name, displayOrder: value.displayOrder, active: value.active };
}

function mapOrganizationalUnit(value: import('../api/generated/models').OrganizationalUnitResponse): OrganizationalUnit[] {
  return value.id !== undefined && value.code && value.name && value.executingUnitId !== undefined && value.active !== undefined
    ? [{ id: value.id, code: value.code, name: value.name, acronym: value.acronym ?? '', parentId: value.parentId ?? null, executingUnitId: value.executingUnitId, active: value.active }]
    : [];
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
}

function formatDateOnly(value: string): string {
  return new Intl.DateTimeFormat('es-PE').format(new Date(`${value}T00:00:00`));
}

function emptyDashboard(): DashboardSummary {
  return { initiatives: 0, projects: 0, alerts: 0, pendingTasks: 0, notifications: 0, portfolioByStatus: {} };
}

function emptyHomePortfolio(): HomePortfolioResult {
  return { content: [], page: 0, size: 5, totalElements: 0, totalPages: 0, executingUnitTotalElements: 0, statusCounts: [] };
}

function httpStatusMessage(status: number): string {
  if (status === 0) return 'No fue posible conectar con el backend PIIP.';
  if (status === 401) return 'La sesión expiró o no es válida.';
  if (status === 403) return 'No tienes permisos para realizar esta operación.';
  if (status === 404) return 'El recurso solicitado no existe.';
  if (status === 409) return 'El registro fue modificado por otro usuario. Actualiza la pantalla.';
  return 'La operación no pudo completarse.';
}
