export type PiipStatus =
  | 'PRESENTED'
  | 'INITIATIVE_APPROVED'
  | 'INITIATIVE_ARCHIVED'
  | 'PROJECT_IN_PROGRESS'
  | 'PRODUCT_APPROVED'
  | 'PRODUCT_NOT_APPROVED'
  | 'SUSPENDED'
  | 'CANCELLED'
  | 'FINISHED'
  | 'NOT_APPLICABLE'
  | 'NOT_ADMISSIBLE';

export type PortfolioStatusApplicability = 'INITIATIVE' | 'PROJECT' | 'NONE';

/** Entrada activa del catálogo persistente de estados del portafolio (identidad por código, sin id). */
export interface PortfolioStatusOption {
  code: string;
  name: string;
  displayOrder: number;
  active: boolean;
  applicability: string;
}

/** Metadata vigente de un estado referenciado por un registro histórico, incluso si está inactivo. */
export interface PortfolioStatusReference {
  code?: PiipStatus | string;
  name?: string;
  active?: boolean;
}

export type UserRole = 'Administrador PIIP' | 'Consulta externa';

export type UserRoleCode = 'ADMINISTRADOR_PIIP' | 'CONSULTA_EXTERNA';

export interface RoleScope {
  role: UserRoleCode;
  institutionId: number;
  executingUnitId: number | null;
}

export type PiipRecordType = 'Iniciativa' | 'Proyecto';

export interface PersistentCatalogOption {
  id: number;
  code: string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface TechnicalCatalogOption {
  code: 'INITIATIVE' | 'PROJECT';
  name: PiipRecordType;
  displayOrder: number;
  active: boolean;
}

export type HistoricalCatalogReference = PersistentCatalogOption;

export interface CatalogBundle {
  recordTypes: TechnicalCatalogOption[];
  solutionTypes: PersistentCatalogOption[];
  sources: PersistentCatalogOption[];
  peiObjectives: PersistentCatalogOption[];
  poiActivities: PersistentCatalogOption[];
  documentTypes: PersistentCatalogOption[];
  portfolioStatuses: PortfolioStatusOption[];
}

export type ResourcePhase = 'idle' | 'loading' | 'ready' | 'error';

export interface ResourceState<T> {
  phase: ResourcePhase;
  value: T;
  error: string | null;
  requestId: number;
}

export type ProjectOriginMode = 'DERIVED_FROM_INITIATIVE' | 'PREEXISTING';

export type ProjectOrigin =
  | { mode: 'DERIVED_FROM_INITIATIVE'; initiativeCode: string }
  | { mode: 'PREEXISTING'; initiativeCode: 'NA' };

/** Contrato plano equivalente a los 23 campos operativos del Excel PIIP. */
export interface PiipPortfolioRecord {
  recordType: PiipRecordType;
  code: string;
  originCode: string;
  name: string;
  solutionType: 'Solución potencial o adaptable' | 'Solución por definir' | 'No aplica';
  source: string;
  startDate: string;
  responsible: string;
  peiObjective: string;
  poiActivity: string;
  responsibleUnits: string;
  description: string;
  keyResults: string;
  note: string;
  status: PortfolioStatusReference;
  finalProductType: 'Prototipo de solución conceptualizada' | 'Solución funcional' | 'NA';
  digitalComponent: 'Si' | 'No';
  closingDate: string;
  technicalOpinionReport: string;
  formalApprovalDecision: string;
  finalProductApprovalDocument: string;
  projectManagementDocumentation: string;
  finalClosureReport: string;
  executingUnitId?: number;
  /** Nombre de la Unidad Ejecutora entregado por el contrato. */
  executingUnit?: string;
  recordTypeReference?: TechnicalCatalogOption;
  solutionTypeReference?: HistoricalCatalogReference;
  sourceReference?: HistoricalCatalogReference;
  peiObjectiveReference?: HistoricalCatalogReference | null;
  poiActivityReference?: HistoricalCatalogReference | null;
  responsibleUnitReferences?: OrganizationalUnit[];
  /** Versión de concurrencia entregada por el backend. */
  version?: number;
  /** Instante ISO de la última modificación confirmada. */
  updatedAt?: string;
}

export interface InitiativeRecord {
  code: string;
  name: string;
  source: string;
  responsible: string;
  role: string;
  unit: string;
  status: PortfolioStatusReference;
  updatedAt: string;
  executingUnitId?: number;
  sourceReference?: HistoricalCatalogReference;
  organizationalUnits?: OrganizationalUnit[];
}

export interface ProjectRecord {
  code: string;
  name: string;
  originCode: string;
  originMode: ProjectOriginMode;
  unit: string;
  responsible: string;
  status: PortfolioStatusReference;
  digitalComponent: 'Si' | 'No';
  executingUnitId?: number;
  organizationalUnits?: OrganizationalUnit[];
}

export interface PreexistingProjectInput {
  code: string;
  name: string;
  startDate: string;
  sourceId: number;
  responsible: string;
  responsibleUnitIds?: readonly number[];
  /** Compatibilidad transitoria para consumidores de prueba pendientes de migrar. */
  organizationalUnitId?: number;
  peiObjectiveId?: number;
  poiActivityId?: number;
  description: string;
  keyResults: string;
  note: string;
  digitalComponent: 'Si' | 'No';
  technicalOpinionReport: string;
  formalApprovalDecision: string;
  finalProductApprovalDocument: string;
  projectManagementDocumentation: string;
  finalClosureReport: string;
  documentAttachments?: DocumentAttachment[];
}

export interface InitiativeInput {
  code: string;
  startDate: string;
  name: string;
  solutionTypeId: number;
  sourceId: number;
  responsible: string;
  responsibleUnitIds?: readonly number[];
  /** Compatibilidad transitoria para consumidores de prueba pendientes de migrar. */
  organizationalUnitId?: number;
  peiObjectiveId?: number;
  poiActivityId?: number;
  description: string;
  note: string;
  digitalComponent: PiipPortfolioRecord['digitalComponent'];
  initialFilename: string;
  initialFile?: File;
}

export interface InitiativeDecisionInput {
  initiativeCode: string;
  targetStatus: Extract<PiipStatus, 'INITIATIVE_APPROVED'>;
  observation: string;
}

export interface InitiativeStatusTransitionInput {
  initiativeCode: string;
  targetStatus: Extract<PiipStatus, 'INITIATIVE_ARCHIVED' | 'NOT_ADMISSIBLE'>;
  observation: string;
}

export interface ProjectStatusTransitionInput {
  projectCode: string;
  targetStatus: Extract<PiipStatus, 'PROJECT_IN_PROGRESS' | 'PRODUCT_APPROVED' | 'PRODUCT_NOT_APPROVED' | 'SUSPENDED' | 'CANCELLED' | 'FINISHED'>;
  observation: string;
}

export interface DerivedProjectInput {
  initiativeCode: string;
  code: string;
  startDate: string;
  name: string;
  solutionTypeId: number;
  sourceId: number;
  responsible: string;
  responsibleUnitIds?: readonly number[];
  /** Compatibilidad transitoria para consumidores de prueba pendientes de migrar. */
  organizationalUnitId?: number;
  peiObjectiveId?: number;
  poiActivityId?: number;
  description: string;
  keyResults: string;
  note: string;
  digitalComponent: PiipPortfolioRecord['digitalComponent'];
}

export interface InitiativeDetail {
  initiative: InitiativeRecord;
  portfolioRecord: PiipPortfolioRecord;
  dossier?: DocumentDossier;
  derivedProject?: ProjectRecord;
}

export interface InitiativeUpdateInput {
  version: number;
  name?: string;
  solutionTypeId?: number;
  sourceId?: number;
  startDate?: string;
  responsible?: string;
  peiObjectiveId?: number | null;
  poiActivityId?: number | null;
  responsibleUnitIds?: readonly number[];
  description?: string;
  note?: string;
  digitalComponent?: PiipPortfolioRecord['digitalComponent'];
}

export interface ProjectUpdateInput {
  version: number;
  name?: string;
  solutionTypeId?: number;
  sourceId?: number;
  startDate?: string;
  responsible?: string;
  peiObjectiveId?: number | null;
  poiActivityId?: number | null;
  responsibleUnitIds?: readonly number[];
  description?: string;
  keyResults?: string | null;
  note?: string;
  digitalComponent?: PiipPortfolioRecord['digitalComponent'];
}

export interface ProjectDetail {
  project: ProjectRecord;
  portfolioRecord: PiipPortfolioRecord;
  dossier?: DocumentDossier;
  originInitiative?: InitiativeRecord;
}

export interface DocumentRecord {
  type?: DocumentType;
  documentTypeId?: number;
  documentType?: HistoricalCatalogReference;
  name: string;
  required: boolean;
  filename: string | null;
  version: string | null;
  uploadedAt: string | null;
  state: 'Cargado' | 'Pendiente' | 'No aplica';
  versionId?: number;
  optimisticVersion?: number;
  externallyPublished?: boolean;
  /** Archivos independientes activos del tipo documental. */
  files?: DocumentFile[];
}

export interface DocumentVersion {
  id?: number;
  number: number;
  filename: string;
  uploadedAt: string;
  externallyPublished: boolean;
  optimisticVersion?: number;
}

export interface DocumentFile {
  /** Nulo únicamente para la representación compatible de una respuesta legada. */
  id: number | null;
  original: boolean;
  latestVersion: number;
  current: DocumentVersion | null;
  versions: DocumentVersion[];
}

export type DocumentType =
  | 'PUBLIC_INNOVATION_INITIATIVE_SHEET'
  | 'INITIATIVE_TECHNICAL_OPINION'
  | 'FORMAL_APPROVAL_DECISION'
  | 'FINAL_PRODUCT_APPROVAL'
  | 'PROJECT_MANAGEMENT_DOCUMENTATION'
  | 'FINAL_CLOSURE_REPORT';

export interface DocumentStage {
  title: string;
  records: DocumentRecord[];
}

export interface DocumentDossier {
  recordType: PiipRecordType;
  code: string;
  name: string;
  unit: string;
  status: PortfolioStatusReference;
  lastActivity: string;
  executingUnitId?: number;
  stages: DocumentStage[];
}

export interface DocumentDossierSummary {
  recordType: PiipRecordType;
  code: string;
  name: string;
  unit: string;
  status: PortfolioStatusReference;
  loadedCount: number;
  pendingCount: number;
  notApplicableCount: number;
  lastActivity: string;
  executingUnitId?: number;
  organizationalUnits?: OrganizationalUnit[];
}

export interface AuditEvent {
  recordCode?: string;
  timestamp: string;
  event: string;
  user: string;
  email: string;
  observation: string;
  actorSubject?: string;
  rawDetail?: string;
  documentName?: string;
  icon: string;
  status?: PortfolioStatusReference;
  previousStatus?: PortfolioStatusReference;
  newStatus?: PortfolioStatusReference;
}

export interface WorkItem {
  id: number;
  code: string;
  action: string;
  priority: 'Alta' | 'Media' | 'Baja';
  assignedTo: string;
  dueDate: string | null;
  alert: 'VENCIDA' | 'PROXIMA' | 'EN_PLAZO' | 'SIN_PLAZO';
  version: number;
}

export interface AuditAccess {
  subject: string;
  roles: string;
  method: string;
  path: string;
  status: number;
  recordCode?: string;
  safeReason?: string | null;
  correlationId: string;
  durationMs: number;
  occurredAt: string;
}

export type AssignmentRole = 'ADMINISTRADOR_PIIP' | 'CONSULTA_EXTERNA';

export interface UserAssignmentScope {
  id: number;
  role: AssignmentRole;
  institutionId: number;
  institution: string;
  executingUnitId?: number;
  executingUnit?: string;
  active: boolean;
  validFrom?: string;
  validUntil?: string;
  version: number;
}

export interface UserAdministrationUser {
  id: number;
  subject: string;
  fullName: string;
  email: string;
  scopes: UserAssignmentScope[];
}

export interface UserAssignmentCandidate {
  id: number;
  subject: string;
  fullName: string;
  email: string;
}

export interface UserAdministrationSnapshot {
  users: UserAdministrationUser[];
  assignmentCandidates: UserAssignmentCandidate[];
}

export interface AssignmentMutationInput {
  userSubject?: string;
  role: AssignmentRole;
  institutionId: number;
  executingUnitId?: number;
}

export interface AssignmentMutationResult {
  outcome: 'CREATED' | 'REACTIVATED' | 'UPDATED' | 'SUSPENDED';
  scope?: UserAssignmentScope;
  status: number;
}

export interface CurrentUser {
  subject: string;
  fullName: string;
  email: string;
  roleScopes: RoleScope[];
  roles: UserRoleCode[];
  institutionIds: number[];
  executingUnitIds: number[];
  institutionWide: boolean;
}

export interface ExecutingUnit {
  id: number;
  code: string;
  name: string;
  institutionId: number;
}

export interface AdministrableExecutingUnit {
  id: number;
  code: string;
  name: string;
}

export interface AdministrableScope {
  institutionId: number;
  institutionCode: string;
  institutionName: string;
  institutionWideAllowed: boolean;
  executingUnits: AdministrableExecutingUnit[];
}

/** Contexto institucional devuelto por las operaciones administrativas. */
export interface AdministrativeInstitution {
  id: number;
  code: string;
  name: string;
}

/** Referencia heredada de una Unidad Ejecutora en la administración de UO. */
export interface AdministrativeExecutingUnitReference {
  id: number;
  code: string;
  name: string;
}

/** Modelo administrativo de UE, separado del catálogo legado. */
export interface AdministrativeExecutingUnit {
  id: number;
  code: string;
  name: string;
  active: boolean;
  displayOrder: number;
  registeredAt: string;
  activatedAt: string;
  version: number;
  institution: AdministrativeInstitution;
}

/** Modelo administrativo de UO; deliberadamente no contiene parentId. */
export interface AdministrativeOrganizationalUnit {
  id: number;
  code: string;
  name: string;
  acronym: string;
  active: boolean;
  version: number;
  executingUnit: AdministrativeExecutingUnitReference;
}

export interface CreateAdministrativeExecutingUnitInput {
  institutionId: number;
  name: string;
  displayOrder?: number;
}

export interface UpdateAdministrativeExecutingUnitInput {
  name: string;
  displayOrder: number;
}

export interface CreateAdministrativeOrganizationalUnitInput {
  executingUnitId: number;
  name: string;
  acronym: string;
  active: boolean;
}

export interface UpdateAdministrativeOrganizationalUnitInput {
  name: string;
  acronym: string;
}

export interface OrganizationalUnit {
  id: number;
  code: string;
  name: string;
  acronym: string;
  parentId: number | null;
  executingUnitId: number;
  active: boolean;
}

export interface PortfolioStatusCount {
  status: PortfolioStatusReference;
  count: number;
}

export interface DashboardSummary {
  initiatives: number;
  projects: number;
  alerts: number;
  pendingTasks: number;
  notifications: number;
  portfolioStatusCounts: PortfolioStatusCount[];
}

export interface HomePortfolioQuery {
  executingUnitId: number;
  q: string;
  type: PiipRecordType | 'Todos';
  status: PiipStatus | 'Todos';
  page: number;
  size: number;
}

export interface HomePortfolioItem {
  recordType: PiipRecordType;
  code: string;
  name: string;
  status: PortfolioStatusReference;
  executingUnitId: number;
  executingUnit: string;
  updatedAt: string;
}

export interface HomePortfolioStatusCount {
  status: PortfolioStatusReference;
  count: number;
}

export interface HomePortfolioResult {
  content: HomePortfolioItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  executingUnitTotalElements: number;
  statusCounts: HomePortfolioStatusCount[];
}

export interface NotificationItem {
  id: number;
  type: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface DocumentAttachment {
  type: DocumentType;
  documentTypeId: number;
  mode: 'FILE' | 'NOT_APPLICABLE' | 'PENDING';
  file?: File;
}
