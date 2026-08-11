package pe.gob.midagri.piip.portfolio.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.documents.domain.DocumentType;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.shared.api.*;
import pe.gob.midagri.piip.work.domain.*;
import pe.gob.midagri.piip.work.persistence.*;
import java.time.*;
import java.util.*;

@Service
public class PortfolioService {
    private final PortfolioRecordRepository records;
    private final ResponsibleUnitRepository responsibleUnits;
    private final ExecutingUnitRepository executingUnits;
    private final OrganizationalUnitRepository organizationalUnits;
    private final UserRepository users;
    private final WorkTaskRepository tasks;
    private final NotificationRepository notifications;
    private final DocumentRepository documents;
    private final CodeGeneratorService codes;
    private final LocalAuthorizationService authorization;
    private final AuditService audit;

    public PortfolioService(PortfolioRecordRepository records, ResponsibleUnitRepository responsibleUnits,
            ExecutingUnitRepository executingUnits, OrganizationalUnitRepository organizationalUnits,
            UserRepository users, WorkTaskRepository tasks, NotificationRepository notifications, DocumentRepository documents,
            CodeGeneratorService codes, LocalAuthorizationService authorization, AuditService audit) {
        this.records = records; this.responsibleUnits = responsibleUnits; this.executingUnits = executingUnits;
        this.organizationalUnits = organizationalUnits; this.users = users; this.tasks = tasks; this.notifications = notifications; this.documents = documents;
        this.codes = codes; this.authorization = authorization; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<PortfolioRecordResponse> list(RecordType type, String query, String status, Long executingUnitId,
            int page, int size, String sort, String direction) {
        LocalAccessContext access = authorization.requireAuthenticatedRole();
        Specification<PortfolioRecordEntity> specification = (root, ignored, builder) -> builder.equal(root.get("recordType"), type);
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, ignored, builder) -> builder.or(
                builder.like(builder.lower(root.get("code")), pattern),
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("responsible")), pattern)));
        }
        PortfolioStatus parsedStatus = parseStatus(status);
        if (parsedStatus != null) specification = specification.and((root, ignored, builder) -> builder.equal(root.get("status"), parsedStatus));
        if (executingUnitId != null) specification = specification.and((root, ignored, builder) -> builder.equal(root.get("executingUnit").get("id"), executingUnitId));
        specification = specification.and(scopeSpecification(access));
        String sortProperty = Set.of("code", "name", "startDate", "updatedAt").contains(sort) ? sort : "updatedAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(sortDirection, sortProperty));
        return PageResponse.from(records.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PortfolioRecordResponse get(String code) { return toResponse(readAllowed(code)); }

    @Transactional
    public PortfolioRecordResponse createInitiative(InitiativeCreateRequest request) {
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, request.executingUnitId());
        ExecutingUnitEntity unit = executingUnits.findById(request.executingUnitId()).orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        String code = codes.next(RecordType.INITIATIVE, request.startDate().getYear());
        PortfolioRecordEntity record = records.save(PortfolioRecordEntity.initiative(code, unit, request.name(), request.solutionType(), request.source(),
            request.startDate(), request.responsible(), request.peiObjective(), request.poiActivity(), request.description(), request.note(), request.digitalComponent(), actor.subject()));
        saveResponsibleUnits(record, request.responsibleUnits()); createDocumentSlots(record);
        UserEntity assigned = users.findById(actor.userId()).orElseThrow();
        WorkTaskEntity decisionTask = tasks.save(new WorkTaskEntity(record, TaskType.REGISTER_DECISION, "Registrar decisión de la iniciativa", assigned, TaskPriority.HIGH, LocalDate.now().plusDays(20), "INICIATIVA_REGISTRADA"));
        notifications.save(new NotificationEntity(assigned, record, "TAREA_CREADA", "Tienes pendiente registrar la decisión de " + code));
        audit.event("TAREA_CREADA", "TAREA_TRABAJO", decisionTask.getId().toString(), Map.of("registro", code), actor.subject());
        audit.event("INICIATIVA_REGISTRADA", "REGISTRO_PORTAFOLIO", code, Map.of("estado", record.getStatus().label()), actor.subject());
        return toResponse(record);
    }

    @Transactional
    public PortfolioRecordResponse approve(String code, ApprovalRequest request) {
        PortfolioRecordEntity record = records.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Iniciativa inexistente"));
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        if (record.getVersion() != request.version()) throw new StaleVersionException();
        record.approve();
        tasks.findFirstByRecordIdAndTypeAndStatus(record.getId(), TaskType.REGISTER_DECISION, TaskStatus.PENDING).ifPresent(task -> {
            task.complete();
            audit.event("TAREA_COMPLETADA", "TAREA_TRABAJO", task.getId().toString(), Map.of("registro", code), actor.subject());
        });
        UserEntity assigned = users.findById(actor.userId()).orElseThrow();
        WorkTaskEntity projectTask = tasks.save(new WorkTaskEntity(record, TaskType.CREATE_DERIVED_PROJECT, "Crear proyecto derivado", assigned, TaskPriority.MEDIUM, null, "INICIATIVA_APROBADA"));
        notifications.save(new NotificationEntity(assigned, record, "TAREA_CREADA", "La iniciativa " + code + " está aprobada y puede originar un proyecto"));
        audit.event("TAREA_CREADA", "TAREA_TRABAJO", projectTask.getId().toString(), Map.of("registro", code), actor.subject());
        audit.event("INICIATIVA_APROBADA", "REGISTRO_PORTAFOLIO", code, Map.of("observacion", request.observation() == null ? "" : request.observation()), actor.subject());
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<PortfolioRecordResponse> eligibleInitiatives() {
        LocalAccessContext actor = authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        return records.findByRecordTypeAndStatusOrderByUpdatedAtDesc(RecordType.INITIATIVE, PortfolioStatus.INITIATIVE_APPROVED).stream()
            .filter(record -> actor.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP,
                record.getExecutingUnit().getId(), record.getExecutingUnit().getInstitution().getId()))
            .filter(record -> !records.existsByOriginRecordId(record.getId())).map(this::toResponse).toList();
    }

    @Transactional
    public PortfolioRecordResponse createDerived(DerivedProjectRequest request) {
        PortfolioRecordEntity initiative = records.findByCodeIgnoreCase(request.initiativeCode()).orElseThrow(() -> new NotFoundException("Iniciativa inexistente"));
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, initiative.getExecutingUnit().getId());
        if (initiative.getStatus() != PortfolioStatus.INITIATIVE_APPROVED) throw new BusinessRuleException("La iniciativa debe estar aprobada");
        if (records.existsByOriginRecordId(initiative.getId())) throw new BusinessRuleException("La iniciativa ya tiene un proyecto derivado");
        String code = codes.next(RecordType.PROJECT, request.startDate().getYear());
        PortfolioRecordEntity project = records.save(PortfolioRecordEntity.derivedProject(code, initiative, request.name(), request.solutionType(), request.source(),
            request.startDate(), request.responsible(), request.peiObjective(), request.poiActivity(), request.description(), request.keyResults(), request.note(), request.digitalComponent(), actor.subject()));
        saveResponsibleUnits(project, request.responsibleUnits()); createDocumentSlots(project);
        tasks.findFirstByRecordIdAndTypeAndStatus(initiative.getId(), TaskType.CREATE_DERIVED_PROJECT, TaskStatus.PENDING).ifPresent(task -> {
            task.complete();
            audit.event("TAREA_COMPLETADA", "TAREA_TRABAJO", task.getId().toString(), Map.of("registro", initiative.getCode()), actor.subject());
        });
        audit.event("PROYECTO_DERIVADO_REGISTRADO", "REGISTRO_PORTAFOLIO", code, Map.of("iniciativaOrigen", initiative.getCode()), actor.subject());
        return toResponse(project);
    }

    @Transactional
    public PortfolioRecordResponse createPreexisting(PreexistingProjectRequest request) {
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, request.executingUnitId());
        ExecutingUnitEntity unit = executingUnits.findById(request.executingUnitId()).orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        String code = codes.next(RecordType.PROJECT, request.startDate().getYear());
        PortfolioRecordEntity project = records.save(PortfolioRecordEntity.preexistingProject(code, unit, request.name(), request.source(), request.startDate(), request.responsible(),
            request.peiObjective(), request.poiActivity(), request.description(), request.keyResults(), request.note(), request.digitalComponent(), actor.subject()));
        saveResponsibleUnits(project, request.responsibleUnits()); createDocumentSlots(project);
        audit.event("PROYECTO_PREEXISTENTE_REGISTRADO", "REGISTRO_PORTAFOLIO", code, Map.of("origen", "NA"), actor.subject());
        return toResponse(project);
    }

    private PortfolioRecordEntity readAllowed(String code) {
        PortfolioRecordEntity record = records.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Registro inexistente"));
        authorization.requireReadableUnit(record.getExecutingUnit().getId()); return record;
    }

    private Specification<PortfolioRecordEntity> scopeSpecification(LocalAccessContext access) {
        return (root, ignored, builder) -> {
            var unit = root.get("executingUnit");
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (!access.executingUnitIds().isEmpty()) predicates.add(unit.get("id").in(access.executingUnitIds()));
            if (!access.institutionWideIds().isEmpty()) predicates.add(unit.get("institution").get("id").in(access.institutionWideIds()));
            return predicates.isEmpty() ? builder.disjunction() : builder.or(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private PortfolioStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        return Arrays.stream(PortfolioStatus.values()).filter(status -> status.name().equalsIgnoreCase(value) || status.label().equalsIgnoreCase(value))
            .findFirst().orElseThrow(() -> new BusinessRuleException("Estado de filtro inválido"));
    }

    private void saveResponsibleUnits(PortfolioRecordEntity record, List<ResponsibleUnitInput> inputs) {
        int order = 1;
        for (ResponsibleUnitInput input : inputs) {
            OrganizationalUnitEntity unit = input.organizationalUnitId() == null ? null : organizationalUnits.findById(input.organizationalUnitId())
                .orElseThrow(() -> new NotFoundException("Unidad orgánica inexistente"));
            if (unit != null && !unit.getExecutingUnit().getId().equals(record.getExecutingUnit().getId())) throw new BusinessRuleException("La unidad orgánica pertenece a otra Unidad Ejecutora");
            responsibleUnits.save(new ResponsibleUnitEntity(record, unit, input.originalDesignation(), order++));
        }
    }

    private void createDocumentSlots(PortfolioRecordEntity record) { for (DocumentType type : DocumentType.values()) documents.save(new DocumentEntity(record, type)); }

    private PortfolioRecordResponse toResponse(PortfolioRecordEntity record) {
        List<String> units = responsibleUnits.findByRecordIdOrderByDisplayOrder(record.getId()).stream().map(ResponsibleUnitEntity::getOriginalDesignation).toList();
        return new PortfolioRecordResponse(record.getRecordType().label(), record.getCode(), record.getOriginCode(), record.getName(), record.getSolutionType().label(),
            record.getSourceOrigin().label(), record.getStartDate(), record.getResponsible(), record.getPeiObjective(), record.getPoiActivity(), units,
            record.getDescription(), record.getKeyResults(), record.getNote(), record.getStatus().label(), record.getFinalProductType().label(),
            record.getDigitalComponent().label(), record.getClosingDate(), null, null, null, null, null,
            record.getExecutingUnit().getId(), record.getExecutingUnit().getName(), record.getUpdatedAt(), record.getVersion());
    }
}
