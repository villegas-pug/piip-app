import { Injectable, signal } from '@angular/core';
import {
  AuditEvent,
  AuditAccess,
  CurrentUser,
  DerivedProjectInput,
  DocumentDossier,
  DocumentDossierSummary,
  DocumentRecord,
  InitiativeDecisionInput,
  InitiativeDetail,
  InitiativeInput,
  InitiativeRecord,
  PiipPortfolioRecord,
  PreexistingProjectInput,
  ProjectOrigin,
  ProjectRecord,
  PiipRecordType,
  UserRole,
  WorkItem,
} from './piip.models';
import { PiipRepository } from './piip.repository';

@Injectable({ providedIn: 'root' })
export class PiipMockRepository extends PiipRepository {
  readonly demoMode: boolean = true;
  readonly role = signal<UserRole>('Administrador PIIP');
  readonly currentUser = signal<CurrentUser | null>({ subject: 'demo-admin', fullName: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', roles: ['ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1], institutionWide: false });
  readonly executingUnits = signal([{ id: 1, code: 'UE-DEMO', name: 'Unidad Ejecutora de demostracion', institutionId: 1 }]);
  readonly organizationalUnits = signal([]);
  readonly selectedExecutingUnitId = signal<number | null>(1);
  readonly loading = signal(false);
  readonly lastError = signal<string | null>(null);
  readonly documentDossierSummaries = signal<DocumentDossierSummary[]>([]);
  readonly auditAccesses = signal<AuditAccess[]>([]);
  readonly notifications = signal([{ id: 1, type: 'DEMO', message: 'Notificacion de demostracion', read: false, createdAt: new Date().toISOString() }]);
  readonly dashboardSummary = signal({ initiatives: 3, projects: 8, alerts: 2, pendingTasks: 2, notifications: 1, portfolioByStatus: {} });

  readonly portfolioRecords = signal<PiipPortfolioRecord[]>([
    {
      recordType: 'Iniciativa', code: 'I-024-2026', originCode: 'NA',
      name: 'Mejoramiento del servicio de riego tecnificado en el valle de Ica',
      solutionType: 'Solución potencial o adaptable', source: 'Ficha de iniciativa de innovación pública',
      startDate: '2026-05-20', responsible: 'María López', peiObjective: 'Objetivo PEI declarado en el registro',
      poiActivity: 'Actividad POI declarada en el registro', responsibleUnits: 'DGIA',
      description: 'Necesidad de mejorar el acceso al riego tecnificado.', keyResults: '', note: '',
      status: 'Presentado', finalProductType: 'NA', digitalComponent: 'No', closingDate: '',
      technicalOpinionReport: 'Informe_Opinion_I-024-2026.pdf', formalApprovalDecision: '',
      finalProductApprovalDocument: '', projectManagementDocumentation: '', finalClosureReport: '',
    },
    {
      recordType: 'Iniciativa', code: 'I-019-2026', originCode: 'NA',
      name: 'Fortalecimiento de capacidades para la gestión de la innovación agraria',
      solutionType: 'Solución por definir', source: 'Innovación abierta', startDate: '2026-05-06',
      responsible: 'Carlos Rojas', peiObjective: 'Fortalecer la gestión institucional de la innovación agraria',
      poiActivity: 'Desarrollo de capacidades institucionales', responsibleUnits: 'DIPNA',
      description: 'Fortalecimiento de capacidades para gestionar iniciativas de innovación agraria.',
      keyResults: '', note: '', status: 'Iniciativa aprobada', finalProductType: 'NA', digitalComponent: 'No',
      closingDate: '', technicalOpinionReport: 'Informe_Opinion_I-019-2026.pdf',
      formalApprovalDecision: 'Decision_I-019-2026.pdf', finalProductApprovalDocument: '',
      projectManagementDocumentation: '', finalClosureReport: '',
    },
    {
      recordType: 'Iniciativa', code: 'I-014-2026', originCode: 'NA',
      name: 'Adquisición de equipamiento para la estación experimental agraria Santa Ana',
      solutionType: 'Solución potencial o adaptable', source: 'Propuesta de jefatura o directivos',
      startDate: '2026-05-02', responsible: 'Lucía Fernández',
      peiObjective: 'Objetivo PEI declarado en el registro', poiActivity: 'Actividad POI declarada en el registro',
      responsibleUnits: 'DGA', description: 'Necesidad de equipamiento para la estación experimental agraria.',
      keyResults: '', note: 'Iniciativa archivada con comentarios.', status: 'Iniciativa archivada',
      finalProductType: 'NA', digitalComponent: 'No', closingDate: '', technicalOpinionReport: '',
      formalApprovalDecision: '', finalProductApprovalDocument: '', projectManagementDocumentation: '',
      finalClosureReport: '',
    },
    {
      recordType: 'Proyecto', code: 'P-005-2026', originCode: 'NA',
      name: 'Red de Estaciones Agrometeorológicas', solutionType: 'No aplica', source: 'Otros',
      startDate: '2026-02-12', responsible: 'Carmen Rojas', peiObjective: 'Objetivo PEI declarado en el registro',
      poiActivity: 'Actividad POI declarada en el registro', responsibleUnits: 'DCLIMA',
      description: 'Proyecto preexistente registrado sin iniciativa formal de origen.', keyResults: '',
      note: 'Proyecto preexistente de demostración.', status: 'Proyecto en ejecución', finalProductType: 'NA',
      digitalComponent: 'Si', closingDate: '', technicalOpinionReport: 'No Aplica', formalApprovalDecision: 'No Aplica',
      finalProductApprovalDocument: '', projectManagementDocumentation: '', finalClosureReport: '',
    },
  ]);

  readonly workItems = signal<WorkItem[]>([
    { id: 1, code: 'I-024-2026', action: 'Registrar decision', priority: 'Alta', assignedTo: 'DGIA', dueDate: '27/05/2026', alert: 'PROXIMA', version: 0 },
    { id: 2, code: 'I-019-2026', action: 'Revisar informe tecnico', priority: 'Media', assignedTo: 'DIPNA', dueDate: '30/05/2026', alert: 'EN_PLAZO', version: 0 },
  ]);

  readonly initiatives = signal<InitiativeRecord[]>([
    {
      code: 'I-024-2026',
      name: 'Mejoramiento del servicio de riego tecnificado en el valle de Ica',
      source: 'Ficha de iniciativa de innovación pública',
      responsible: 'María López',
      role: 'Analista de Inversiones',
      unit: 'DGIA',
      status: 'Presentado',
      updatedAt: '20/05/2026 10:15',
    },
    {
      code: 'I-019-2026',
      name: 'Fortalecimiento de capacidades para la gestión de la innovación agraria',
      source: 'Innovación abierta',
      responsible: 'Carlos Rojas',
      role: 'Especialista en Innovación',
      unit: 'DIPNA',
      status: 'Iniciativa aprobada',
      updatedAt: '18/05/2026 16:45',
    },
    {
      code: 'I-014-2026',
      name: 'Adquisición de equipamiento para la estación experimental agraria Santa Ana',
      source: 'Propuesta de jefatura o directivos',
      responsible: 'Lucía Fernández',
      role: 'Analista de Adquisiciones',
      unit: 'DGA',
      status: 'Iniciativa archivada',
      updatedAt: '15/05/2026 09:30',
    },
  ]);

  readonly projects = signal<ProjectRecord[]>([
    { code: 'P-003-2026', name: 'Plataforma de Innovación Agraria Sostenible', originCode: 'I-012-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'DIPNA', responsible: 'María Quintana', status: 'Proyecto en ejecución', digitalComponent: 'Si' },
    { code: 'P-004-2026', name: 'Sistema de Información de Riego', originCode: 'I-010-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'DGA', responsible: 'Luis Calderón', status: 'Producto aprobado', digitalComponent: 'Si' },
    { code: 'P-005-2026', name: 'Red de Estaciones Agrometeorológicas', originCode: 'NA', originMode: 'PREEXISTING', unit: 'DCLIMA', responsible: 'Carmen Rojas', status: 'Proyecto en ejecución', digitalComponent: 'Si' },
    { code: 'P-006-2026', name: 'Capacitación Digital para Productores', originCode: 'I-008-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'DIPNA', responsible: 'José Vílchez', status: 'Suspendido', digitalComponent: 'No' },
    { code: 'P-007-2026', name: 'Trazabilidad de Productos Agrarios', originCode: 'I-003-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'DGESEP', responsible: 'Ana Lucía Prado', status: 'Producto aprobado', digitalComponent: 'Si' },
    { code: 'P-008-2026', name: 'Gestión de Suelos Degradados', originCode: 'NA', originMode: 'PREEXISTING', unit: 'DGIA', responsible: 'Miguel Torres', status: 'Proyecto en ejecución', digitalComponent: 'No' },
    { code: 'P-009-2026', name: 'Sanidad Vegetal con Monitoreo Digital', originCode: 'I-011-2026', originMode: 'DERIVED_FROM_INITIATIVE', unit: 'SENASA', responsible: 'Elena Paredes', status: 'Producto aprobado', digitalComponent: 'Si' },
    { code: 'P-010-2026', name: 'Módulo de Seguros Agrarios', originCode: 'NA', originMode: 'PREEXISTING', unit: 'DGA', responsible: 'Ricardo Salazar', status: 'Suspendido', digitalComponent: 'No' },
  ]);

  readonly documentDossiers = signal<DocumentDossier[]>([
    {
      recordType: 'Iniciativa',
      code: 'I-024-2026',
      name: 'Mejoramiento del servicio de riego tecnificado en el valle de Ica',
      unit: 'DGIA',
      status: 'Presentado',
      lastActivity: '23/05/2026 10:28',
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
      status: 'Iniciativa aprobada',
      lastActivity: '18/05/2026 16:45',
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
      status: 'Proyecto en ejecución',
      lastActivity: '24/05/2026 11:10',
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
  ]);

  readonly auditEvents = signal<AuditEvent[]>([
    { recordCode: 'I-024-2026', timestamp: '20/05/2026\n09:31:12', event: 'Iniciativa creada', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'Se inició un borrador local del registro.', icon: 'add' },
    { recordCode: 'I-024-2026', timestamp: '20/05/2026\n09:45:27', event: 'Ficha inicial cargada', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'Se cargó la ficha inicial de la iniciativa.', documentName: 'Ficha_inicial_I-024-2026.pdf', icon: 'description' },
    { recordCode: 'I-024-2026', timestamp: '20/05/2026\n10:02:44', event: 'Estado cambiado a Presentado', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'La iniciativa fue presentada para evaluación.', icon: 'check' },
    { recordCode: 'I-024-2026', timestamp: '20/05/2026\n10:28:19', event: 'Informe técnico cargado', user: 'Administrador PIIP', email: 'admin.piip@midagri.gob.pe', observation: 'Se cargó el informe técnico de evaluación.', documentName: 'Informe_tecnico_I-024-2026.pdf', icon: 'cloud_upload' },
  ]);

  toggleRole(): void {
    this.role.update((role) => (role === 'Administrador PIIP' ? 'Consulta externa' : 'Administrador PIIP'));
  }

  initialize(): void {}
  refreshAll(): void {}
  clearError(): void { this.lastError.set(null); }
  selectExecutingUnit(executingUnitId: number): void { this.selectedExecutingUnitId.set(executingUnitId); }

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

  getProjectByOrigin(initiativeCode: string): ProjectRecord | undefined {
    return this.projects().find(
      (project) => project.originMode === 'DERIVED_FROM_INITIATIVE' && project.originCode === initiativeCode,
    );
  }

  getInitiativesEligibleForProject(): InitiativeRecord[] {
    return this.initiatives().filter(
      (initiative) => initiative.status === 'Iniciativa aprobada' && !this.getProjectByOrigin(initiative.code),
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
    if (detail.initiative.status !== 'Presentado') {
      throw new Error('Solo una iniciativa en estado Presentado puede aprobarse.');
    }

    const now = new Date();
    const updatedAt = formatDateTime(now);
    this.initiatives.update((initiatives) => initiatives.map((initiative) =>
      initiative.code === input.initiativeCode
        ? { ...initiative, status: 'Iniciativa aprobada', updatedAt }
        : initiative,
    ));
    this.portfolioRecords.update((records) => records.map((record) =>
      record.recordType === 'Iniciativa' && record.code === input.initiativeCode
        ? { ...record, status: 'Iniciativa aprobada' }
        : record,
    ));
    this.documentDossiers.update((dossiers) => dossiers.map((dossier) =>
      dossier.recordType === 'Iniciativa' && dossier.code === input.initiativeCode
        ? { ...dossier, status: 'Iniciativa aprobada', lastActivity: updatedAt }
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
    const record: PiipPortfolioRecord = {
      recordType: 'Iniciativa', code: input.code, originCode: 'NA', name: input.name,
      solutionType: input.solutionType, source: input.source, startDate: input.startDate,
      responsible: input.responsible, peiObjective: input.peiObjective, poiActivity: input.poiActivity,
      responsibleUnits: input.responsibleUnits, description: input.description, keyResults: '', note: input.note,
      status: 'Presentado', finalProductType: 'NA', digitalComponent: input.digitalComponent, closingDate: '',
      technicalOpinionReport: '', formalApprovalDecision: '', finalProductApprovalDocument: '',
      projectManagementDocumentation: '', finalClosureReport: '',
    };
    this.portfolioRecords.update((records) => [record, ...records]);
    this.initiatives.update((items) => [{ code: input.code, name: input.name, source: input.source,
      responsible: input.responsible, role: '', unit: input.responsibleUnits, status: 'Presentado',
      updatedAt: formatDateTime(new Date()) }, ...items]);
    return record;
  }

  registerDerivedProject(input: DerivedProjectInput): PiipPortfolioRecord {
    this.assertAdministrator('El perfil Consulta externa no puede registrar proyectos.');
    const detail = this.getInitiativeDetail(input.initiativeCode);
    if (!detail) throw new Error('La iniciativa de origen no existe.');
    if (detail.initiative.status !== 'Iniciativa aprobada') {
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
    const portfolioRecord: PiipPortfolioRecord = {
      recordType: 'Proyecto', code: input.code, originCode, name: input.name,
      solutionType: input.solutionType, source: input.source, startDate: input.startDate,
      responsible: input.responsible, peiObjective: input.peiObjective, poiActivity: input.poiActivity,
      responsibleUnits: input.responsibleUnits, description: input.description, keyResults: input.keyResults,
      note: input.note, status: 'Proyecto en ejecución', finalProductType: 'NA',
      digitalComponent: input.digitalComponent, closingDate: '', technicalOpinionReport: '',
      formalApprovalDecision: '', finalProductApprovalDocument: '', projectManagementDocumentation: '',
      finalClosureReport: '',
    };
    const project: ProjectRecord = {
      code: input.code, name: input.name, originCode, originMode: 'DERIVED_FROM_INITIATIVE',
      unit: input.responsibleUnits, responsible: input.responsible, status: 'Proyecto en ejecución',
      digitalComponent: input.digitalComponent,
    };

    this.portfolioRecords.update((records) => [portfolioRecord, ...records]);
    this.projects.update((projects) => [project, ...projects]);
    this.documentDossiers.update((dossiers) => [createDerivedProjectDocumentDossier(input), ...dossiers]);
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
    return portfolioRecord;
  }

  registerPreexistingProject(input: PreexistingProjectInput): PiipPortfolioRecord {
    this.assertAdministrator('El perfil Consulta externa no puede registrar proyectos.');

    const originCode = resolveProjectOriginCode({ mode: 'PREEXISTING', initiativeCode: 'NA' });
    const portfolioRecord: PiipPortfolioRecord = {
      recordType: 'Proyecto',
      code: input.code,
      originCode,
      name: input.name,
      solutionType: 'No aplica',
      source: input.source,
      startDate: input.startDate,
      responsible: input.responsible,
      peiObjective: input.peiObjective,
      poiActivity: input.poiActivity,
      responsibleUnits: input.responsibleUnits,
      description: input.description,
      keyResults: input.keyResults,
      note: input.note,
      status: 'Proyecto en ejecución',
      finalProductType: 'NA',
      digitalComponent: input.digitalComponent,
      closingDate: '',
      technicalOpinionReport: input.technicalOpinionReport,
      formalApprovalDecision: input.formalApprovalDecision,
      finalProductApprovalDocument: input.finalProductApprovalDocument,
      projectManagementDocumentation: input.projectManagementDocumentation,
      finalClosureReport: input.finalClosureReport,
    };

    this.portfolioRecords.update((records) => [portfolioRecord, ...records]);
    this.projects.update((projects) => [
      {
        code: input.code,
        name: input.name,
        originCode,
        originMode: 'PREEXISTING',
        unit: input.responsibleUnits,
        responsible: input.responsible,
        status: 'Proyecto en ejecución',
        digitalComponent: input.digitalComponent,
      },
      ...projects,
    ]);
    this.documentDossiers.update((dossiers) => [createPreexistingDocumentDossier(input), ...dossiers]);
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
    return portfolioRecord;
  }

  uploadDocument(): void {}
  markDocumentNotApplicable(): void {}
  downloadDocument(): void {}
  setDocumentPublication(): void {}
  markNotificationRead(id: number): void {
    this.notifications.update((items) => items.map((item) => item.id === id ? { ...item, read: true } : item));
  }

  private assertAdministrator(message: string): void {
    if (this.role() !== 'Administrador PIIP') throw new Error(message);
  }
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

function createPreexistingDocumentDossier(input: PreexistingProjectInput): DocumentDossier {
  return {
    recordType: 'Proyecto',
    code: input.code,
    name: input.name,
    unit: input.responsibleUnits,
    status: 'Proyecto en ejecución',
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

function createDerivedProjectDocumentDossier(input: DerivedProjectInput): DocumentDossier {
  return {
    recordType: 'Proyecto', code: input.code, name: input.name, unit: input.responsibleUnits,
    status: 'Proyecto en ejecución', lastActivity: formatDateTime(new Date()),
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

export function resolveProjectOriginCode(origin: ProjectOrigin): string {
  if (origin.mode === 'PREEXISTING') return 'NA';
  const initiativeCode = origin.initiativeCode.trim();
  if (!initiativeCode) throw new Error('Un proyecto derivado requiere una iniciativa aprobada.');
  return initiativeCode;
}
