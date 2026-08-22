package pe.gob.midagri.piip.portfolio.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.catalogs.application.CatalogReferenceService;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.documents.application.PortfolioDocumentService;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.DerivedProjectRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioRecordResponse;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PreexistingProjectRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ProjectStatusTransitionRequest;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import pe.gob.midagri.piip.work.application.PortfolioWorkService;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.domain.TaskType;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@Service
public class ProjectApplicationService {
    private final PortfolioRecordRepository records;
    private final WorkTaskRepository tasks;
    private final ExecutingUnitRepository executingUnits;
    private final CodeGeneratorService codes;
    private final LocalAuthorizationService authorization;
    private final AuditService audit;
    private final CatalogReferenceService catalogReferences;
    private final ResponsibleUnitService responsibleUnitService;
    private final PortfolioDocumentService portfolioDocumentService;
    private final PortfolioWorkService portfolioWorkService;
    private final PortfolioApplicationSupport support;
    private final PortfolioReadModelAssembler assembler;

    @Autowired
    public ProjectApplicationService(PortfolioRecordRepository records, ExecutingUnitRepository executingUnits,
            WorkTaskRepository tasks, CodeGeneratorService codes, LocalAuthorizationService authorization, AuditService audit,
            CatalogReferenceService catalogReferences, ResponsibleUnitService responsibleUnitService,
            PortfolioDocumentService portfolioDocumentService, PortfolioWorkService portfolioWorkService,
            PortfolioApplicationSupport support, PortfolioReadModelAssembler assembler) {
        this.records = records;
        this.tasks = tasks;
        this.executingUnits = executingUnits;
        this.codes = codes;
        this.authorization = authorization;
        this.audit = audit;
        this.catalogReferences = catalogReferences;
        this.responsibleUnitService = responsibleUnitService;
        this.portfolioDocumentService = portfolioDocumentService;
        this.portfolioWorkService = portfolioWorkService;
        this.support = support;
        this.assembler = assembler;
    }

    /** Constructor de compatibilidad para pruebas unitarias existentes; no concentra lógica de negocio. */
    public ProjectApplicationService(PortfolioRecordRepository records, ResponsibleUnitRepository responsibleUnits,
            ExecutingUnitRepository executingUnits, WorkTaskRepository tasks, NotificationRepository notifications,
            DocumentRepository documents, CodeGeneratorService codes, LocalAuthorizationService authorization,
            AuditService audit, CatalogReferenceService catalogReferences, DocumentTypeRepository documentTypes) {
        this(records, responsibleUnits, executingUnits, tasks, notifications, documents, codes, authorization, audit,
            catalogReferences, documentTypes, Clock.systemUTC());
    }

    public ProjectApplicationService(PortfolioRecordRepository records, ResponsibleUnitRepository responsibleUnits,
            ExecutingUnitRepository executingUnits, WorkTaskRepository tasks, NotificationRepository notifications,
            DocumentRepository documents, CodeGeneratorService codes, LocalAuthorizationService authorization,
            AuditService audit, CatalogReferenceService catalogReferences, DocumentTypeRepository documentTypes, Clock clock) {
        this(records, executingUnits, tasks, codes, authorization, audit, catalogReferences,
            new ResponsibleUnitService(responsibleUnits, null), new PortfolioDocumentService(records, documents, documentTypes),
            new PortfolioWorkService(tasks, notifications, audit), new PortfolioApplicationSupport(authorization, clock),
            new PortfolioReadModelAssembler(responsibleUnits));
    }

    public ProjectApplicationService(PortfolioRecordRepository records, ResponsibleUnitRepository responsibleUnits,
            ExecutingUnitRepository executingUnits, WorkTaskRepository tasks, NotificationRepository notifications,
            DocumentRepository documents, CodeGeneratorService codes, LocalAuthorizationService authorization,
            AuditService audit, Clock clock, CatalogReferenceService catalogReferences, DocumentTypeRepository documentTypes) {
        this(records, responsibleUnits, executingUnits, tasks, notifications, documents, codes, authorization, audit,
            catalogReferences, documentTypes, clock);
    }

    @Transactional
    public PortfolioRecordResponse createDerived(DerivedProjectRequest request) {
        PortfolioRecordEntity initiative = records.findByCodeIgnoreCaseForUpdate(request.initiativeCode())
            .orElseThrow(() -> new NotFoundException("Iniciativa inexistente"));
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, initiative.getExecutingUnit().getId());
        if (initiative.getStatus() != PortfolioStatus.INITIATIVE_APPROVED)
            throw new BusinessRuleException("La iniciativa debe estar aprobada");
        if (records.existsByOriginRecordId(initiative.getId()))
            throw new BusinessRuleException("La iniciativa ya tiene un proyecto derivado");
        String code = codes.next(RecordType.PROJECT, request.startDate().getYear());
        PortfolioRecordEntity project = records.save(PortfolioRecordEntity.derivedProject(code, initiative, request.name(),
            catalogReferences.resolveActive(request.solutionTypeId(), CatalogCode.SOLUTION_TYPE, "solutionTypeId"),
            catalogReferences.resolveActive(request.sourceId(), CatalogCode.SOURCE_ORIGIN, "sourceId"), request.startDate(),
            request.responsible(), catalogReferences.resolveActive(request.peiObjectiveId(), CatalogCode.PEI_OBJECTIVE, "peiObjectiveId"),
            catalogReferences.resolveActive(request.poiActivityId(), CatalogCode.POI_ACTIVITY, "poiActivityId"), request.description(),
            request.keyResults(), request.note(), request.digitalComponent(), actor.subject()));
        responsibleUnitService.save(project, request.responsibleUnits());
        portfolioDocumentService.initializeSlots(project.getId());
        tasks.findFirstByRecordIdAndTypeAndStatus(initiative.getId(), TaskType.CREATE_DERIVED_PROJECT, TaskStatus.PENDING).ifPresent(task -> {
            task.complete();
            audit.event("TAREA_COMPLETADA", "TAREA_TRABAJO", task.getId().toString(), Map.of("registro", initiative.getCode()), actor.subject());
        });
        audit.event("PROYECTO_DERIVADO_REGISTRADO", "REGISTRO_PORTAFOLIO", code,
            Map.of("iniciativaOrigen", initiative.getCode()), actor.subject());
        return assembler.toResponse(project);
    }

    @Transactional
    public PortfolioRecordResponse createPreexisting(PreexistingProjectRequest request) {
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, request.executingUnitId());
        ExecutingUnitEntity unit = executingUnits.findById(request.executingUnitId())
            .orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        String code = codes.next(RecordType.PROJECT, request.startDate().getYear());
        PortfolioRecordEntity project = records.save(PortfolioRecordEntity.preexistingProject(code, unit, request.name(),
            catalogReferences.resolveActiveByCode(CatalogCode.SOLUTION_TYPE, "NOT_APPLICABLE", "solutionTypeId"),
            catalogReferences.resolveActive(request.sourceId(), CatalogCode.SOURCE_ORIGIN, "sourceId"), request.startDate(),
            request.responsible(), catalogReferences.resolveActive(request.peiObjectiveId(), CatalogCode.PEI_OBJECTIVE, "peiObjectiveId"),
            catalogReferences.resolveActive(request.poiActivityId(), CatalogCode.POI_ACTIVITY, "poiActivityId"), request.description(),
            request.keyResults(), request.note(), request.digitalComponent(), actor.subject()));
        responsibleUnitService.save(project, request.responsibleUnits());
        portfolioDocumentService.initializeSlots(project.getId());
        audit.event("PROYECTO_PREEXISTENTE_REGISTRADO", "REGISTRO_PORTAFOLIO", code,
            Map.of("origen", "NA"), actor.subject());
        return assembler.toResponse(project);
    }

    @Transactional
    public PortfolioRecordResponse transition(String code, ProjectStatusTransitionRequest request) {
        PortfolioRecordEntity project = records.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new NotFoundException("Proyecto inexistente"));
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, project.getExecutingUnit().getId());
        if (project.getVersion() != request.version()) throw new StaleVersionException();
        PortfolioStatus previous = project.getStatus();
        try {
            project.transitionProjectTo(request.targetStatus(), support.clock().instant(), LocalDate.now(support.clock()));
        } catch (IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        audit.event("ESTADO_PROYECTO_CAMBIADO", "REGISTRO_PORTAFOLIO", project.getCode(),
            support.transitionAuditDetail(previous, project.getStatus(), project, request.observation()), actor.subject());
        records.flush();
        return assembler.toResponse(project);
    }

    @Transactional
    public PortfolioRecordResponse transitionProjectStatus(String code, ProjectStatusTransitionRequest request) {
        return transition(code, request);
    }
}
