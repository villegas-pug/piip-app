import { TestBed } from '@angular/core/testing';
import { PIIP_CATALOGS } from './piip.catalogs';
import { PiipMockRepository, resolveProjectOriginCode } from './piip-mock.repository';

describe('PIIP functional source', () => {
  it('conserva localmente solo los conceptos fuera de las siete fuentes centralizadas', () => {
    expect(Object.keys(PIIP_CATALOGS)).toEqual(['statuses', 'finalProductTypes', 'digitalComponents']);
    expect(PIIP_CATALOGS.statuses).toContain('No Aplicable');
    expect(PIIP_CATALOGS.statuses).toContain('No Admisible');
    expect(PIIP_CATALOGS.finalProductTypes).toContain('NA');
    expect(PIIP_CATALOGS.digitalComponents).toEqual(['Si', 'No']);
  });

  it('uses only confirmed document names in the mock repository', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const initiativeDossier = repository.getDocumentDossier('Iniciativa', 'I-024-2026');
    const names = initiativeDossier?.stages.flatMap((stage) => stage.records.map((record) => record.name));
    expect(names).toEqual([
      'Ficha de Iniciativa de Innovación Pública',
      'Informe de opinión técnica de evaluación de iniciativa',
      'Documento formal de decisión de aprobación',
      'Documento formal de aprobación de producto final',
      'Documentación de la gestión del proyecto',
      'Informe final de cierre',
    ]);
  });

  it('summarizes initiatives and projects without counting No aplica as pending', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const project = repository.getDocumentDossierSummaries().find((dossier) => dossier.code === 'P-005-2026');

    expect(project?.recordType).toBe('Proyecto');
    expect(project?.loadedCount).toBe(1);
    expect(project?.pendingCount).toBe(2);
    expect(project?.notApplicableCount).toBe(3);
  });

  it('does not silently fall back when a document dossier does not exist', () => {
    const repository = TestBed.inject(PiipMockRepository);
    expect(repository.getDocumentDossier('Iniciativa', 'I-999-2026')).toBeUndefined();
    expect(repository.getDocumentDossier('Proyecto', 'I-024-2026')).toBeUndefined();
  });

  it('models the 23 operational Excel fields without adding the initial ficha to that flat contract', () => {
    const repository = TestBed.inject(PiipMockRepository);
    expect(repository.portfolioRecords()[0].solutionTypeReference?.id).toBeTypeOf('number');
    expect(repository.portfolioRecords()[0].originCode).toBe('NA');
    const preexistingProject = repository.portfolioRecords().find((record) => record.code === 'P-005-2026');
    expect(preexistingProject?.technicalOpinionReport).toBe('No Aplica');
  });

  it('approves a presented initiative without creating a project automatically', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const projectCount = repository.projects().length;

    repository.approveInitiative({
      initiativeCode: 'I-024-2026',
      targetStatus: 'Iniciativa aprobada',
      observation: 'Aprobación funcional de prueba.',
    });

    expect(repository.getInitiativeDetail('I-024-2026')?.initiative.status).toBe('Iniciativa aprobada');
    expect(repository.getInitiativeDetail('I-024-2026')?.portfolioRecord.status).toBe('Iniciativa aprobada');
    expect(repository.getDocumentDossier('Iniciativa', 'I-024-2026')?.status).toBe('Iniciativa aprobada');
    expect(repository.projects()).toHaveLength(projectCount);
    expect(repository.auditEvents()[0].event).toBe('Iniciativa aprobada');
  });

  it('rejects approval from the external role and from a non-presented initiative', () => {
    const repository = TestBed.inject(PiipMockRepository);
    expect(() => repository.approveInitiative({ initiativeCode: 'I-019-2026', targetStatus: 'Iniciativa aprobada', observation: '' }))
      .toThrowError('Solo una iniciativa en estado Presentado puede aprobarse.');

    repository.toggleRole();
    expect(() => repository.approveInitiative({ initiativeCode: 'I-024-2026', targetStatus: 'Iniciativa aprobada', observation: '' }))
      .toThrowError('El perfil Consulta externa no puede aprobar iniciativas.');
  });

  it('registers one derived project from an approved initiative without copying documents', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const initiative = repository.getInitiativeDetail('I-019-2026')!;
    const code = repository.getNextProjectCode('I-019-2026');

    repository.registerDerivedProject({
      initiativeCode: initiative.initiative.code,
      code,
      startDate: '2026-06-01',
      name: initiative.portfolioRecord.name,
      solutionTypeId: 2,
      sourceId: 12,
      responsible: initiative.portfolioRecord.responsible,
      organizationalUnitId: 101,
      description: initiative.portfolioRecord.description,
      keyResults: 'Resultado propio del proyecto.',
      note: '',
      digitalComponent: initiative.portfolioRecord.digitalComponent,
    });

    const project = repository.projects().find((record) => record.code === code);
    const portfolioRecord = repository.portfolioRecords().find((record) => record.code === code);
    const dossier = repository.getDocumentDossier('Proyecto', code);
    expect(project?.originCode).toBe('I-019-2026');
    expect(project?.originMode).toBe('DERIVED_FROM_INITIATIVE');
    expect(portfolioRecord?.status).toBe('Proyecto en ejecución');
    expect(portfolioRecord?.solutionType).toBe('Solución por definir');
    expect(portfolioRecord?.technicalOpinionReport).toBe('');
    expect(dossier?.stages.flatMap((stage) => stage.records)).toHaveLength(3);
    expect(repository.auditEvents()[0].event).toBe('Proyecto derivado registrado');

    expect(() => repository.registerDerivedProject({} as never)).toThrowError('La iniciativa de origen no existe.');
  });

  it('returns only approved initiatives without a derived project as eligible', () => {
    const repository = TestBed.inject(PiipMockRepository);

    expect(repository.getInitiativesEligibleForProject().map((initiative) => initiative.code)).toEqual(['I-019-2026']);

    const initiative = repository.getInitiativeDetail('I-019-2026')!;
    repository.registerDerivedProject({
      initiativeCode: initiative.initiative.code,
      code: repository.getNextProjectCode(initiative.initiative.code),
      startDate: '2026-06-01',
      name: initiative.portfolioRecord.name,
      solutionTypeId: 2,
      sourceId: 12,
      responsible: initiative.portfolioRecord.responsible,
      organizationalUnitId: 101,
      description: initiative.portfolioRecord.description,
      keyResults: '',
      note: '',
      digitalComponent: initiative.portfolioRecord.digitalComponent,
    });

    expect(repository.getInitiativesEligibleForProject()).toEqual([]);
  });

  it('prevents duplicate derived projects and requires an approved origin', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const baseInput = {
      code: 'P-011-2026', startDate: '2026-06-01', name: 'Proyecto de prueba',
      solutionTypeId: 2, sourceId: 14, responsible: 'Responsable',
      organizationalUnitId: 101, description: 'Descripción',
      keyResults: '', note: '', digitalComponent: 'No' as const,
    };

    expect(() => repository.registerDerivedProject({ ...baseInput, initiativeCode: 'I-024-2026' }))
      .toThrowError('El proyecto requiere una iniciativa en estado Iniciativa aprobada.');

    repository.registerDerivedProject({ ...baseInput, initiativeCode: 'I-019-2026' });
    expect(() => repository.registerDerivedProject({ ...baseInput, code: 'P-012-2026', initiativeCode: 'I-019-2026' }))
      .toThrowError('La iniciativa ya tiene un proyecto derivado.');
  });

  it('switches between the two presentation profiles', () => {
    const repository = TestBed.inject(PiipMockRepository);
    expect(repository.role()).toBe('Administrador PIIP');
    repository.toggleRole();
    expect(repository.role()).toBe('Consulta externa');
  });

  it('requires an initiative code for derived projects', () => {
    expect(() => resolveProjectOriginCode({ mode: 'DERIVED_FROM_INITIATIVE', initiativeCode: '' })).toThrowError(
      'Un proyecto derivado requiere una iniciativa aprobada.',
    );
    expect(resolveProjectOriginCode({ mode: 'DERIVED_FROM_INITIATIVE', initiativeCode: 'I-019-2026' })).toBe('I-019-2026');
  });

  it('registers a preexisting project with fixed Excel values and an audit event', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.registerPreexistingProject({
      code: 'P-011-2026',
      name: 'Proyecto preexistente de prueba',
      startDate: '2025-08-01',
      sourceId: 14,
      responsible: 'Responsable de prueba',
      organizationalUnitId: 101,
      peiObjectiveId: 20,
      poiActivityId: 30,
      description: 'Proyecto iniciado antes del flujo de iniciativas.',
      keyResults: 'Resultado de prueba',
      note: '',
      digitalComponent: 'No',
      technicalOpinionReport: 'No Aplica',
      formalApprovalDecision: 'No Aplica',
      finalProductApprovalDocument: 'No Aplica',
      projectManagementDocumentation: '',
      finalClosureReport: 'No Aplica',
    });

    const project = repository.projects()[0];
    const portfolioRecord = repository.portfolioRecords()[0];
    expect(project.originMode).toBe('PREEXISTING');
    expect(project.originCode).toBe('NA');
    expect(project.status).toBe('Proyecto en ejecución');
    expect(portfolioRecord.solutionType).toBe('No aplica');
    expect(portfolioRecord.source).toBe('Otros');
    expect(repository.auditEvents()[0].event).toBe('Proyecto preexistente registrado');
    expect(repository.getDocumentDossier('Proyecto', 'P-011-2026')?.stages[0].records[0].state).toBe('No aplica');
  });

  it('prevents the external consultation profile from registering projects', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.toggleRole();
    expect(() => repository.registerPreexistingProject({} as never)).toThrowError(
      'El perfil Consulta externa no puede registrar proyectos.',
    );
  });
});
