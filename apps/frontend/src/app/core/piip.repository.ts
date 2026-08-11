import { Signal, WritableSignal } from '@angular/core';
import {
  AdministrableScope, AuditAccess, AuditEvent, CurrentUser, DashboardSummary, DerivedProjectInput, DocumentDossier,
  DocumentDossierSummary, DocumentType, ExecutingUnit, InitiativeDecisionInput, InitiativeDetail, InitiativeInput,
  InitiativeRecord, NotificationItem, OrganizationalUnit, PiipPortfolioRecord, PreexistingProjectInput, ProjectRecord,
  PiipRecordType, UserRole, WorkItem,
} from './piip.models';

export type RepositoryOperation<T> = T | Promise<T>;

export abstract class PiipRepository {
  abstract readonly demoMode: boolean;
  abstract readonly role: Signal<UserRole | null>;
  abstract readonly portfolioRecords: WritableSignal<PiipPortfolioRecord[]>;
  abstract readonly initiatives: WritableSignal<InitiativeRecord[]>;
  abstract readonly projects: WritableSignal<ProjectRecord[]>;
  abstract readonly documentDossiers: WritableSignal<DocumentDossier[]>;
  abstract readonly documentDossierSummaries: WritableSignal<DocumentDossierSummary[]>;
  abstract readonly auditEvents: WritableSignal<AuditEvent[]>;
  abstract readonly auditAccesses: WritableSignal<AuditAccess[]>;
  abstract readonly workItems: WritableSignal<WorkItem[]>;
  abstract readonly notifications: WritableSignal<NotificationItem[]>;
  abstract readonly dashboardSummary: WritableSignal<DashboardSummary>;
  abstract readonly currentUser: WritableSignal<CurrentUser | null>;
  abstract readonly executingUnits: WritableSignal<ExecutingUnit[]>;
  abstract readonly administrableScopes: WritableSignal<AdministrableScope[]>;
  abstract readonly organizationalUnits: WritableSignal<OrganizationalUnit[]>;
  abstract readonly selectedExecutingUnitId: WritableSignal<number | null>;
  abstract readonly loading: WritableSignal<boolean>;
  abstract readonly lastError: WritableSignal<string | null>;
  abstract initialize(): RepositoryOperation<void>;
  abstract refreshAll(): RepositoryOperation<void>;
  abstract refreshAuthorizationContext(): RepositoryOperation<void>;
  abstract loadAdministrableScopes(): RepositoryOperation<void>;
  abstract clearError(): void;
  abstract canReadExecutingUnit(executingUnitId: number | null | undefined): boolean;
  abstract canAdministerExecutingUnit(executingUnitId: number | null | undefined): boolean;
  abstract hasAnyAdministratorScope(): boolean;
  abstract effectiveRoleForExecutingUnit(executingUnitId: number | null | undefined): UserRole | null;
  abstract selectExecutingUnit(executingUnitId: number): RepositoryOperation<void>;
  abstract toggleRole(): void;
  abstract getDocumentDossier(recordType: PiipRecordType, code: string): DocumentDossier | undefined;
  abstract getDocumentDossierSummaries(): DocumentDossierSummary[];
  abstract getInitiativeDetail(code: string): InitiativeDetail | undefined;
  abstract getProjectByOrigin(initiativeCode: string): ProjectRecord | undefined;
  abstract getInitiativesEligibleForProject(): InitiativeRecord[];
  abstract getNextProjectCode(initiativeCode: string): string;
  abstract saveDraft(value: unknown): void;
  abstract savePreexistingProjectDraft(value: unknown): void;
  abstract saveDerivedProjectDraft(value: unknown): void;
  abstract registerInitiative(input: InitiativeInput): RepositoryOperation<PiipPortfolioRecord>;
  abstract approveInitiative(input: InitiativeDecisionInput): RepositoryOperation<PiipPortfolioRecord>;
  abstract registerDerivedProject(input: DerivedProjectInput): RepositoryOperation<PiipPortfolioRecord>;
  abstract registerPreexistingProject(input: PreexistingProjectInput): RepositoryOperation<PiipPortfolioRecord>;
  abstract uploadDocument(code: string, type: DocumentType, file: File): RepositoryOperation<void>;
  abstract markDocumentNotApplicable(code: string, type: DocumentType, reason: string): RepositoryOperation<void>;
  abstract downloadDocument(code: string, versionId: number, filename: string): RepositoryOperation<void>;
  abstract setDocumentPublication(code: string, versionId: number, published: boolean, version: number): RepositoryOperation<void>;
  abstract markNotificationRead(id: number): RepositoryOperation<void>;
}
