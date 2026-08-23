export type PiipStatus =
  | 'Presentado'
  | 'Iniciativa aprobada'
  | 'Iniciativa archivada'
  | 'Proyecto en ejecución'
  | 'Producto aprobado'
  | 'Producto no aprobado'
  | 'Suspendido'
  | 'Cancelado'
  | 'Finalizado'
  | 'No Aplicable'
  | 'No Admisible';

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
  status: PiipStatus;
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
  status: PiipStatus;
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
  status: PiipStatus;
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
  organizationalUnitId: number;
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
  organizationalUnitId: number;
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
  targetStatus: 'Iniciativa aprobada';
  observation: string;
}

export interface InitiativeStatusTransitionInput {
  initiativeCode: string;
  targetStatus: Extract<PiipStatus, 'Iniciativa archivada' | 'No Admisible'>;
  observation: string;
}

export interface ProjectStatusTransitionInput {
  projectCode: string;
  targetStatus: Extract<PiipStatus, 'Proyecto en ejecución' | 'Producto aprobado' | 'Producto no aprobado' | 'Suspendido' | 'Cancelado' | 'Finalizado'>;
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
  organizationalUnitId: number;
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
  status: PiipStatus;
  lastActivity: string;
  executingUnitId?: number;
  stages: DocumentStage[];
}

export interface DocumentDossierSummary {
  recordType: PiipRecordType;
  code: string;
  name: string;
  unit: string;
  status: PiipStatus;
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
  correlationId: string;
  durationMs: number;
  occurredAt: string;
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

export interface OrganizationalUnit {
  id: number;
  code: string;
  name: string;
  acronym: string;
  parentId: number | null;
  executingUnitId: number;
  active: boolean;
}

export interface DashboardSummary {
  initiatives: number;
  projects: number;
  alerts: number;
  pendingTasks: number;
  notifications: number;
  portfolioByStatus: Record<string, number>;
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
  status: PiipStatus;
  executingUnitId: number;
  executingUnit: string;
  updatedAt: string;
}

export interface HomePortfolioStatusCount {
  status: PiipStatus;
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
