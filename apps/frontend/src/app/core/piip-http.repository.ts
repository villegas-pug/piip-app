import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import {
  AuditAccess, AuditEvent, CurrentUser, DashboardSummary, DerivedProjectInput, DocumentDossier, DocumentDossierSummary,
  DocumentRecord, DocumentType, ExecutingUnit, InitiativeDecisionInput, InitiativeDetail, InitiativeInput,
  InitiativeRecord, NotificationItem, OrganizationalUnit, PiipPortfolioRecord, PiipRecordType,
  PreexistingProjectInput, ProjectRecord, UserRole, WorkItem,
} from './piip.models';
import { EventResponse } from '../api/generated';
import { PiipRepository } from './piip.repository';
import { resolveApiUrl as runtimeApiUrl } from './piip-runtime-config';
import {
  ApprovalRequest, DerivedProjectRequest, InitiativeCreateRequest, PreexistingProjectRequest, ResponsibleUnitInput,
} from '../api/generated/models';

interface ApiPortfolioRecord {
  recordType: PiipRecordType;
  code: string;
  originCode: string;
  name: string;
  solutionType: PiipPortfolioRecord['solutionType'];
  source: string;
  startDate: string;
  responsible: string;
  peiObjective: string | null;
  poiActivity: string | null;
  responsibleUnits: string[];
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
  type: DocumentType;
  name: string;
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
}

export class PiipApiError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
    this.name = 'PiipApiError';
  }
}

@Injectable({ providedIn: 'root' })
export class PiipHttpRepository extends PiipRepository {
  readonly demoMode = false;
  readonly role = signal<UserRole>('Consulta externa');
  readonly currentUser = signal<CurrentUser | null>(null);
  readonly portfolioRecords = signal<PiipPortfolioRecord[]>([]);
  readonly initiatives = signal<InitiativeRecord[]>([]);
  readonly projects = signal<ProjectRecord[]>([]);
  readonly documentDossiers = signal<DocumentDossier[]>([]);
  readonly documentDossierSummaries = signal<DocumentDossierSummary[]>([]);
  readonly auditEvents = signal<AuditEvent[]>([]);
  readonly auditAccesses = signal<AuditAccess[]>([]);
  readonly workItems = signal<WorkItem[]>([]);
  readonly notifications = signal<NotificationItem[]>([]);
  readonly dashboardSummary = signal<DashboardSummary>(emptyDashboard());
  readonly executingUnits = signal<ExecutingUnit[]>([]);
  readonly organizationalUnits = signal<OrganizationalUnit[]>([]);
  readonly selectedExecutingUnitId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly lastError = signal<string | null>(null);

  private readonly http = inject(HttpClient);
  private readonly apiUrl = runtimeApiUrl();
  private readonly recordVersions = new Map<string, number>();
  private readonly eligibleInitiatives = signal<InitiativeRecord[]>([]);
  private initialization?: Promise<void>;

  constructor() {
    super();
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
      await this.refreshAll();
    } catch (error) {
      this.captureError(error);
    } finally {
      this.loading.set(false);
    }
  }

  async refreshAll(): Promise<void> {
    await Promise.all([
      this.loadPortfolio(),
      this.loadDocumentSummaries(),
      this.loadDashboard(),
      this.loadNotifications(),
      this.loadEligibleInitiatives(),
      this.role() === 'Administrador PIIP' ? this.loadWorkItems() : Promise.resolve(),
      this.role() === 'Administrador PIIP' ? this.loadAudit() : Promise.resolve(),
    ]);
  }

  clearError(): void {
    this.lastError.set(null);
  }

  async selectExecutingUnit(executingUnitId: number): Promise<void> {
    if (!this.executingUnits().some((unit) => unit.id === executingUnitId)) {
      throw new PiipApiError(403, 'La Unidad Ejecutora no pertenece a los ambitos autorizados.');
    }
    this.selectedExecutingUnitId.set(executingUnitId);
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
    if (!initiative || !portfolioRecord) return undefined;
    return {
      initiative,
      portfolioRecord,
      dossier: this.getDocumentDossier('Iniciativa', code),
      derivedProject: this.getProjectByOrigin(code),
    };
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
    const request: InitiativeCreateRequest = {
      executingUnitId,
      startDate: input.startDate,
      name: input.name,
      solutionType: solutionCode(input.solutionType),
      source: sourceCode(input.source),
      responsible: input.responsible,
      responsibleUnits: [responsibleUnitInput(input.responsibleUnits, this.organizationalUnits())],
      peiObjective: input.peiObjective,
      poiActivity: input.poiActivity,
      description: input.description,
      note: input.note,
      digitalComponent: input.digitalComponent === 'Si' ? 'YES' : 'NO',
    };
    const created = await this.request(this.http.post<ApiPortfolioRecord>(`${this.apiUrl}/initiatives`, request));
    if (input.initialFile) {
      await this.uploadDocument(created.code, 'PUBLIC_INNOVATION_INITIATIVE_SHEET', input.initialFile);
    }
    await this.refreshAll();
    return toPortfolioRecord(created);
  }

  async approveInitiative(input: InitiativeDecisionInput): Promise<PiipPortfolioRecord> {
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

  async registerDerivedProject(input: DerivedProjectInput): Promise<PiipPortfolioRecord> {
    const request: DerivedProjectRequest = {
      initiativeCode: input.initiativeCode,
      startDate: input.startDate,
      name: input.name,
      solutionType: solutionCode(input.solutionType),
      source: sourceCode(input.source),
      responsible: input.responsible,
      responsibleUnits: [responsibleUnitInput(input.responsibleUnits, this.organizationalUnits())],
      peiObjective: input.peiObjective,
      poiActivity: input.poiActivity,
      description: input.description,
      keyResults: input.keyResults,
      note: input.note,
      digitalComponent: input.digitalComponent === 'Si' ? 'YES' : 'NO',
    };
    const record = await this.request(this.http.post<ApiPortfolioRecord>(`${this.apiUrl}/projects/derived`, request));
    await this.refreshAll();
    return toPortfolioRecord(record);
  }

  async registerPreexistingProject(input: PreexistingProjectInput): Promise<PiipPortfolioRecord> {
    const request: PreexistingProjectRequest = {
      executingUnitId: this.requireSelectedExecutingUnit(),
      startDate: input.startDate,
      name: input.name,
      source: sourceCode(input.source),
      responsible: input.responsible,
      responsibleUnits: [responsibleUnitInput(input.responsibleUnits, this.organizationalUnits())],
      peiObjective: input.peiObjective,
      poiActivity: input.poiActivity,
      description: input.description,
      keyResults: input.keyResults,
      note: input.note,
      digitalComponent: input.digitalComponent === 'Si' ? 'YES' : 'NO',
    };
    const record = await this.request(this.http.post<ApiPortfolioRecord>(`${this.apiUrl}/projects/preexisting`, request));
    for (const attachment of input.documentAttachments ?? []) {
      if (attachment.mode === 'FILE' && attachment.file) await this.uploadDocument(record.code, attachment.type, attachment.file);
      if (attachment.mode === 'NOT_APPLICABLE') await this.markDocumentNotApplicable(record.code, attachment.type, 'Proyecto preexistente');
    }
    await this.refreshAll();
    return toPortfolioRecord(record);
  }

  async uploadDocument(code: string, type: DocumentType, file: File): Promise<void> {
    const form = new FormData();
    form.append('file', file, file.name);
    await this.request(this.http.post(`${this.apiUrl}/portfolio-records/${code}/documents/${type}/versions`, form));
    const recordType = this.portfolioRecords().find((record) => record.code === code)?.recordType;
    if (recordType) await this.loadDocuments(recordType, code);
    await this.loadDocumentSummaries();
  }

  async markDocumentNotApplicable(code: string, type: DocumentType, reason: string): Promise<void> {
    await this.request(this.http.put(`${this.apiUrl}/portfolio-records/${code}/documents/${type}/not-applicable`, { reason }));
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
    await this.loadDashboard();
  }

  private async loadIdentity(): Promise<void> {
    const user = await this.request(this.http.get<CurrentUser>(`${this.apiUrl}/identity/me`));
    this.currentUser.set(user);
    this.role.set(user.roles.includes('ADMINISTRADOR_PIIP') ? 'Administrador PIIP' : 'Consulta externa');
  }

  private async loadExecutingUnits(): Promise<void> {
    this.executingUnits.set(await this.request(this.http.get<ExecutingUnit[]>(`${this.apiUrl}/executing-units`)));
  }

  private async restoreExecutingUnit(): Promise<void> {
    const stored = Number(localStorage.getItem('piip-selected-executing-unit'));
    const units = this.executingUnits();
    const selected = units.some((unit) => unit.id === stored) ? stored : units[0]?.id ?? null;
    this.selectedExecutingUnitId.set(selected);
    if (selected !== null) await this.loadOrganizationalUnits(selected);
  }

  private async loadOrganizationalUnits(executingUnitId: number): Promise<void> {
    this.organizationalUnits.set(await this.request(this.http.get<OrganizationalUnit[]>(`${this.apiUrl}/organizational-units`, {
      params: { executingUnitId },
    })));
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
    const items = await this.request(this.http.get<DocumentDossierSummary[]>(`${this.apiUrl}/documents`));
    const visibleCodes = new Set(this.portfolioRecords().map((record) => record.code));
    this.documentDossierSummaries.set(items.filter((item) => !visibleCodes.size || visibleCodes.has(item.code)).map((item) => ({ ...item, lastActivity: formatDate(item.lastActivity) })));
  }

  private async loadDocuments(recordType: PiipRecordType, code: string): Promise<void> {
    const items = await this.request(this.http.get<ApiDocument[]>(`${this.apiUrl}/portfolio-records/${code}/documents`));
    const record = this.portfolioRecords().find((candidate) => candidate.code === code);
    if (!record) return;
    const dossier: DocumentDossier = {
      recordType,
      code,
      name: record.name,
      unit: record.responsibleUnits,
      status: record.status,
      lastActivity: formatDate(new Date().toISOString()),
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
    const [items, accesses] = await Promise.all([
      this.request(this.http.get<EventResponse[]>(`${this.apiUrl}/audit/events`)),
      this.request(this.http.get<AuditAccess[]>(`${this.apiUrl}/audit/accesses`)),
    ]);
    this.auditEvents.set(items.map((item) => ({
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
    this.auditAccesses.set(accesses);
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
    this.notifications.set(await this.request(this.http.get<NotificationItem[]>(`${this.apiUrl}/notifications`)));
  }

  private async loadDashboard(): Promise<void> {
    const summary = await this.request(this.http.get<DashboardSummary>(`${this.apiUrl}/dashboard`));
    this.dashboardSummary.set({ ...emptyDashboard(), ...summary });
  }

  private requireSelectedExecutingUnit(): number {
    const value = this.selectedExecutingUnitId();
    if (value === null) throw new PiipApiError(422, 'Selecciona una Unidad Ejecutora antes de registrar.');
    return value;
  }

  private async request<T>(request: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(request) as T;
    } catch (error) {
      throw this.captureError(error);
    }
  }

  private captureError(error: unknown): PiipApiError {
    if (error instanceof PiipApiError) {
      this.lastError.set(error.message);
      return error;
    }
    if (error instanceof HttpErrorResponse) {
      const problem = error.error as ApiProblem | string | null;
      const detail = typeof problem === 'object' && problem?.detail ? problem.detail : undefined;
      const message = detail ?? httpStatusMessage(error.status);
      const apiError = new PiipApiError(error.status, message);
      this.lastError.set(apiError.message);
      return apiError;
    }
    const apiError = new PiipApiError(0, error instanceof Error ? error.message : 'No fue posible comunicarse con el backend PIIP.');
    this.lastError.set(apiError.message);
    return apiError;
  }
}

export function resolveApiUrl(): string {
  return runtimeApiUrl();
}

function toPortfolioRecord(value: ApiPortfolioRecord): PiipPortfolioRecord {
  return {
    recordType: value.recordType,
    code: value.code,
    originCode: value.originCode,
    name: value.name,
    solutionType: value.solutionType,
    source: value.source,
    startDate: value.startDate,
    responsible: value.responsible,
    peiObjective: value.peiObjective ?? '',
    poiActivity: value.poiActivity ?? '',
    responsibleUnits: value.responsibleUnits.join(', '),
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
  };
}

function toInitiativeRecord(value: ApiPortfolioRecord): InitiativeRecord {
  return {
    code: value.code,
    name: value.name,
    source: value.source,
    responsible: value.responsible,
    role: '',
    unit: value.responsibleUnits.join(', '),
    status: value.status,
    updatedAt: formatDate(value.updatedAt),
  };
}

function toProjectRecord(value: ApiPortfolioRecord): ProjectRecord {
  return {
    code: value.code,
    name: value.name,
    originCode: value.originCode,
    originMode: value.originCode === 'NA' ? 'PREEXISTING' : 'DERIVED_FROM_INITIATIVE',
    unit: value.responsibleUnits.join(', '),
    responsible: value.responsible,
    status: value.status,
    digitalComponent: value.digitalComponent,
  };
}

function mapDocuments(items: ApiDocument[], types: DocumentType[]): DocumentRecord[] {
  return items.filter((item) => types.includes(item.type)).map((item) => {
    const version = item.versions[0];
    return {
      type: item.type,
      name: item.name,
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

function responsibleUnitInput(value: string, units: OrganizationalUnit[]): ResponsibleUnitInput {
  const unit = units.find((candidate) => candidate.id === Number(value) || candidate.acronym === value || candidate.name === value);
  return { organizationalUnitId: unit?.id, originalDesignation: unit?.name ?? value };
}

function solutionCode(value: PiipPortfolioRecord['solutionType']): InitiativeCreateRequest['solutionType'] {
  if (value === 'Solución potencial o adaptable') return 'POTENTIAL_OR_ADAPTABLE';
  if (value === 'Solución por definir') return 'TO_BE_DEFINED';
  return 'NOT_APPLICABLE';
}

function sourceCode(value: string): InitiativeCreateRequest['source'] {
  const values: Record<string, InitiativeCreateRequest['source']> = {
    'Ficha de iniciativa de innovación pública': 'INITIATIVE_SHEET',
    'Concurso interno': 'INTERNAL_CONTEST',
    'Innovación abierta': 'OPEN_INNOVATION',
    'Propuesta de jefatura o directivos': 'MANAGEMENT_PROPOSAL',
    Otros: 'OTHER',
    Convocatoria: 'CALL',
  };
  return values[value] ?? 'OTHER';
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

function httpStatusMessage(status: number): string {
  if (status === 0) return 'No fue posible conectar con el backend PIIP.';
  if (status === 401) return 'La sesión expiró o no es válida.';
  if (status === 403) return 'No tienes permisos para realizar esta operación.';
  if (status === 404) return 'El recurso solicitado no existe.';
  if (status === 409) return 'El registro fue modificado por otro usuario. Actualiza la pantalla.';
  return 'La operación no pudo completarse.';
}
