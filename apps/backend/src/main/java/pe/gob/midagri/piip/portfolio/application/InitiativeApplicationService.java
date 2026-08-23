package pe.gob.midagri.piip.portfolio.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ApprovalRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.InitiativeCreateRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.InitiativeStatusTransitionRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioRecordResponse;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.*;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import pe.gob.midagri.piip.work.application.PortfolioWorkService;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.domain.TaskType;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@Service
public class InitiativeApplicationService {
    private final PortfolioRecordRepository records;
    private final WorkTaskRepository tasks;
    private final ExecutingUnitRepository executingUnits;
    private final UserRepository users;
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
    public InitiativeApplicationService(PortfolioRecordRepository records, ExecutingUnitRepository executingUnits,
            UserRepository users, WorkTaskRepository tasks, CodeGeneratorService codes, LocalAuthorizationService authorization, AuditService audit,
            CatalogReferenceService catalogReferences, ResponsibleUnitService responsibleUnitService,
            PortfolioDocumentService portfolioDocumentService, PortfolioWorkService portfolioWorkService,
            PortfolioApplicationSupport support, PortfolioReadModelAssembler assembler) {
        this.records = records;
        this.executingUnits = executingUnits;
        this.users = users;
        this.tasks = tasks;
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
    public InitiativeApplicationService(PortfolioRecordRepository records, ResponsibleUnitRepository responsibleUnits,
            ExecutingUnitRepository executingUnits, OrganizationalUnitRepository organizationalUnits, UserRepository users,
            WorkTaskRepository tasks, NotificationRepository notifications, DocumentRepository documents, CodeGeneratorService codes,
            LocalAuthorizationService authorization, AuditService audit, CatalogReferenceService catalogReferences,
            DocumentTypeRepository documentTypes) {
        this(records, executingUnits, users, tasks, codes, authorization, audit, catalogReferences,
            new ResponsibleUnitService(responsibleUnits, organizationalUnits), new PortfolioDocumentService(records, documents, documentTypes),
            new PortfolioWorkService(tasks, notifications, audit), new PortfolioApplicationSupport(authorization, Clock.systemUTC()),
            new PortfolioReadModelAssembler(responsibleUnits));
    }

    public InitiativeApplicationService(PortfolioRecordRepository records, ResponsibleUnitRepository responsibleUnits,
            ExecutingUnitRepository executingUnits, UserRepository users, WorkTaskRepository tasks,
            NotificationRepository notifications, DocumentRepository documents, CodeGeneratorService codes,
            LocalAuthorizationService authorization, AuditService audit, CatalogReferenceService catalogReferences,
            DocumentTypeRepository documentTypes) {
        this(records, executingUnits, users, tasks, codes, authorization, audit, catalogReferences,
            new ResponsibleUnitService(responsibleUnits, null),
            new PortfolioDocumentService(records, documents, documentTypes), new PortfolioWorkService(tasks, notifications, audit),
            new PortfolioApplicationSupport(authorization, Clock.systemUTC()), new PortfolioReadModelAssembler(responsibleUnits));
    }

    @Transactional
    public PortfolioRecordResponse create(InitiativeCreateRequest request) {
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, request.executingUnitId());
        ExecutingUnitEntity unit = executingUnits.findById(request.executingUnitId())
            .orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        String code = codes.next(RecordType.INITIATIVE, request.startDate().getYear());
        PortfolioRecordEntity record = records.save(PortfolioRecordEntity.initiative(code, unit, request.name(),
            catalogReferences.resolveActive(request.solutionTypeId(), CatalogCode.SOLUTION_TYPE, "solutionTypeId"),
            catalogReferences.resolveActive(request.sourceId(), CatalogCode.SOURCE_ORIGIN, "sourceId"), request.startDate(),
            request.responsible(), catalogReferences.resolveActive(request.peiObjectiveId(), CatalogCode.PEI_OBJECTIVE, "peiObjectiveId"),
            catalogReferences.resolveActive(request.poiActivityId(), CatalogCode.POI_ACTIVITY, "poiActivityId"), request.description(),
            request.note(), request.digitalComponent(), actor.subject()));
        responsibleUnitService.save(record, request.responsibleUnits());
        portfolioDocumentService.initializeSlots(record.getId());
        UserEntity assigned = users.findById(actor.userId()).orElseThrow();
        portfolioWorkService.createDecisionTask(record, assigned, actor.subject());
        audit.event("INICIATIVA_REGISTRADA", "REGISTRO_PORTAFOLIO", code,
            Map.of("estado", record.getStatus().label()), actor.subject());
        return assembler.toResponse(record);
    }

    @Transactional
    public PortfolioRecordResponse createInitiative(InitiativeCreateRequest request) { return create(request); }

    @Transactional
    public PortfolioRecordResponse update(String code, InitiativeUpdateCommand command) {
        PortfolioRecordEntity record = records.findByCodeIgnoreCaseAndRecordTypeForUpdate(code, RecordType.INITIATIVE)
            .orElseThrow(() -> new NotFoundException("Iniciativa inexistente"));
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        if (record.getVersion() != command.version()) throw new StaleVersionException();
        if (record.getStatus() != PortfolioStatus.PRESENTED)
            throw new BusinessRuleException("La iniciativa solo puede editarse en estado Presentado");
        if (records.existsByOriginRecordId(record.getId()))
            throw new BusinessRuleException("La iniciativa tiene un proyecto derivado y no admite edición");
        if (!command.hasEditableField()) throw new BusinessRuleException("La actualización no contiene campos editables");

        var currentUnits = responsibleUnitService.list(record);
        var before = PortfolioUpdateAuditDetail.snapshot(record, currentUnits);
        var solution = command.solutionTypeId().present()
            ? catalogReferences.resolveActive(command.solutionTypeId().value(), CatalogCode.SOLUTION_TYPE, "solutionTypeId")
            : record.getSolutionType();
        var source = command.sourceId().present()
            ? catalogReferences.resolveActive(command.sourceId().value(), CatalogCode.SOURCE_ORIGIN, "sourceId")
            : record.getSourceOrigin();
        var pei = command.peiObjectiveId().present()
            ? catalogReferences.resolveActive(command.peiObjectiveId().value(), CatalogCode.PEI_OBJECTIVE, "peiObjectiveId")
            : record.getPeiObjective();
        var poi = command.poiActivityId().present()
            ? catalogReferences.resolveActive(command.poiActivityId().value(), CatalogCode.POI_ACTIVITY, "poiActivityId")
            : record.getPoiActivity();
        if (command.responsibleUnits().present()) responsibleUnitService.replace(record, command.responsibleUnits().value());
        record.applyEditableFields(value(command.name(), record.getName()), solution, source,
            value(command.startDate(), record.getStartDate()), value(command.responsible(), record.getResponsible()), pei, poi,
            value(command.description(), record.getDescription()), null, value(command.note(), record.getNote()),
            value(command.digitalComponent(), record.getDigitalComponent()), support.clock().instant());
        var after = PortfolioUpdateAuditDetail.snapshot(record, responsibleUnitService.list(record));
        var changes = PortfolioUpdateAuditDetail.diff(before, after);
        if (changes.isEmpty()) throw new BusinessRuleException("La actualización no produce cambios efectivos");
        long previousVersion = record.getVersion();
        records.flush();
        audit.event("INICIATIVA_ACTUALIZADA", "REGISTRO_PORTAFOLIO", record.getCode(),
            PortfolioUpdateAuditDetail.detail(record, previousVersion, record.getVersion(), before, after), actor.subject());
        return assembler.toResponse(record);
    }

    private static <T> T value(FieldUpdate<T> update, T current) { return update.present() ? update.value() : current; }

    @Transactional
    public PortfolioRecordResponse approve(String code, ApprovalRequest request) {
        PortfolioRecordEntity record = records.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new NotFoundException("Iniciativa inexistente"));
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        if (record.getVersion() != request.version()) throw new StaleVersionException();
        record.approve();
        tasks.findFirstByRecordIdAndTypeAndStatus(record.getId(), TaskType.REGISTER_DECISION, TaskStatus.PENDING).ifPresent(task -> {
            task.complete();
            audit.event("TAREA_COMPLETADA", "TAREA_TRABAJO", task.getId().toString(), Map.of("registro", code), actor.subject());
        });
        UserEntity assigned = users.findById(actor.userId()).orElseThrow();
        portfolioWorkService.createDerivedProjectTask(record, assigned, actor.subject());
        audit.event("INICIATIVA_APROBADA", "REGISTRO_PORTAFOLIO", code,
            Map.of("observacion", request.observation() == null ? "" : request.observation()), actor.subject());
        return assembler.toResponse(record);
    }

    @Transactional
    public PortfolioRecordResponse transition(String code, InitiativeStatusTransitionRequest request) {
        PortfolioRecordEntity initiative = records.findByCodeIgnoreCaseForUpdate(code)
            .orElseThrow(() -> new NotFoundException("Iniciativa inexistente"));
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, initiative.getExecutingUnit().getId());
        if (initiative.getVersion() != request.version()) throw new StaleVersionException();
        if (records.existsByOriginRecordId(initiative.getId()))
            throw new BusinessRuleException("La iniciativa tiene un proyecto vinculado y no admite cambios de estado");
        if (request.targetStatus() == PortfolioStatus.INITIATIVE_APPROVED)
            throw new BusinessRuleException("La aprobación debe realizarse mediante la operación existente");
        PortfolioStatus previous = initiative.getStatus();
        try {
            initiative.transitionInitiativeTo(request.targetStatus(), support.clock().instant());
        } catch (IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        audit.event("ESTADO_INICIATIVA_CAMBIADO", "REGISTRO_PORTAFOLIO", initiative.getCode(),
            support.transitionAuditDetail(previous, initiative.getStatus(), initiative, request.observation()), actor.subject());
        records.flush();
        return assembler.toResponse(initiative);
    }

    @Transactional
    public PortfolioRecordResponse transitionInitiativeStatus(String code, InitiativeStatusTransitionRequest request) {
        return transition(code, request);
    }
}
