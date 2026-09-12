package pe.gob.midagri.piip.organization.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationCommands.CreateExecutingUnit;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationCommands.CreateOrganizationalUnit;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationCommands.UpdateExecutingUnit;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationCommands.UpdateOrganizationalUnit;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.ProblemCode;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;

import static pe.gob.midagri.piip.organization.application.OrganizationAdministrationReadModels.*;

@Service
public class OrganizationAdministrationService {
    private static final Pattern EXECUTING_UNIT_CODE = Pattern.compile("^UE-(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORGANIZATIONAL_UNIT_CODE = Pattern.compile("^UO-(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final InstitutionRepository institutions;
    private final ExecutingUnitRepository executingUnits;
    private final OrganizationalUnitRepository organizationalUnits;
    private final LocalAuthorizationService authorization;
    private final AuditService audit;

    public OrganizationAdministrationService(InstitutionRepository institutions, ExecutingUnitRepository executingUnits,
            OrganizationalUnitRepository organizationalUnits, LocalAuthorizationService authorization, AuditService audit) {
        this.institutions = institutions;
        this.executingUnits = executingUnits;
        this.organizationalUnits = organizationalUnits;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Institution> institutions() {
        LocalAccessContext actor = authorization.requireFresh(pe.gob.midagri.piip.identity.domain.RoleCode.ADMINISTRADOR_PIIP);
        return institutions.findAllById(actor.institutionIds(pe.gob.midagri.piip.identity.domain.RoleCode.ADMINISTRADOR_PIIP)).stream()
            .filter(InstitutionEntity::isActive)
            .sorted(Comparator.comparing(InstitutionEntity::getName).thenComparing(InstitutionEntity::getId))
            .map(this::institutionView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ExecutingUnit> executingUnits(Long institutionId) {
        requireInstitution(institutionId);
        requireActiveInstitution(requireInstitutionEntity(institutionId));
        return executingUnits.findByInstitutionIdOrderByDisplayOrderAscNameAscIdAsc(institutionId).stream()
            .map(this::executingUnitView).toList();
    }

    @Transactional
    public ExecutingUnit createExecutingUnit(CreateExecutingUnit command) {
        requireName(command.name(), "El nombre de la Unidad Ejecutora es obligatorio");
        requireNonNegative(command.displayOrder(), "El orden de presentación no puede ser negativo");
        LocalAccessContext actor = requireInstitution(command.institutionId());
        InstitutionEntity institution = institutions.findByIdForUpdate(command.institutionId())
            .orElseThrow(() -> new NotFoundException("Institución inexistente"));
        requireActiveInstitution(institution);
        int displayOrder = command.displayOrder() == null ? nextDisplayOrder(command.institutionId()) : command.displayOrder();
        String code = nextCode(executingUnits.findByInstitutionIdOrderByDisplayOrderAscNameAscIdAsc(command.institutionId()),
            EXECUTING_UNIT_CODE, "UE-");
        rejectExistingExecutingUnitCode(command.institutionId(), code);
        Instant now = Instant.now();
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, code, command.name(), displayOrder, now));
        auditExecutingUnit("UE_CREADA", unit, null, unit, actor.subject());
        return executingUnitView(unit);
    }

    @Transactional
    public ExecutingUnit updateExecutingUnit(UpdateExecutingUnit command) {
        requireName(command.name(), "El nombre de la Unidad Ejecutora es obligatorio");
        if (command.displayOrder() == null || command.displayOrder() < 0) {
            throw new BusinessRuleException(ProblemCode.INVALID_REQUEST, "El orden de presentación es obligatorio y no puede ser negativo");
        }
        ExecutingUnitEntity unit = executingUnits.findByIdForUpdate(command.id())
            .orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        LocalAccessContext actor = requireInstitution(unit.getInstitution().getId());
        requireVersion(unit.getVersion(), command.expectedVersion());
        Snapshot before = snapshot(unit);
        unit.updateDetails(command.name(), command.displayOrder());
        auditExecutingUnit("UE_ACTUALIZADA", unit, before, unit, actor.subject());
        return executingUnitView(unit);
    }

    @Transactional
    public ExecutingUnit deactivateExecutingUnit(Long id, long expectedVersion) {
        ExecutingUnitEntity unit = executingUnits.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        LocalAccessContext actor = requireInstitution(unit.getInstitution().getId());
        requireVersion(unit.getVersion(), expectedVersion);
        if (!unit.isActive()) throw incompatibleState("La Unidad Ejecutora ya está inactiva");
        Snapshot before = snapshot(unit);
        unit.deactivate();
        auditExecutingUnit("UE_DESACTIVADA", unit, before, unit, actor.subject());
        return executingUnitView(unit);
    }

    @Transactional
    public ExecutingUnit reactivateExecutingUnit(Long id, long expectedVersion) {
        ExecutingUnitEntity unit = executingUnits.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        LocalAccessContext actor = requireInstitution(unit.getInstitution().getId());
        requireVersion(unit.getVersion(), expectedVersion);
        if (unit.isActive()) throw incompatibleState("La Unidad Ejecutora ya está activa");
        Snapshot before = snapshot(unit);
        unit.reactivate(Instant.now());
        auditExecutingUnit("UE_REACTIVADA", unit, before, unit, actor.subject());
        return executingUnitView(unit);
    }

    @Transactional(readOnly = true)
    public List<OrganizationalUnit> organizationalUnits(Long executingUnitId) {
        ExecutingUnitEntity unit = authorizedExecutingUnit(executingUnitId, false);
        return organizationalUnits.findByExecutingUnitIdOrderByNameAscIdAsc(unit.getId()).stream()
            .map(this::organizationalUnitView).toList();
    }

    @Transactional
    public OrganizationalUnit createOrganizationalUnit(CreateOrganizationalUnit command) {
        requireName(command.name(), "El nombre de la Unidad Orgánica es obligatorio");
        requireAcronym(command.acronym());
        if (command.active() == null) throw new BusinessRuleException(ProblemCode.INVALID_REQUEST, "El estado activo es obligatorio");
        ExecutingUnitEntity executingUnit = authorizedExecutingUnit(command.executingUnitId(), true);
        int next = nextOrganizationalUnitSequence(organizationalUnits.findByExecutingUnitIdOrderByNameAscIdAsc(executingUnit.getId()),
            ORGANIZATIONAL_UNIT_CODE) + 1;
        String code = String.format(Locale.ROOT, "UO-%03d", next);
        if (organizationalUnits.findByExecutingUnitIdAndCodeIgnoreCaseForUpdate(executingUnit.getId(), code).isPresent()) {
            throw duplicateCode(code);
        }
        OrganizationalUnitEntity unit = organizationalUnits.save(new OrganizationalUnitEntity(executingUnit, code, command.name(),
            command.acronym(), command.active()));
        LocalAccessContext actor = authorization.requireOrganizationAdministration(executingUnit.getInstitution().getId());
        auditOrganizationalUnit("UO_CREADA", unit, null, unit, actor.subject());
        return organizationalUnitView(unit);
    }

    @Transactional
    public OrganizationalUnit updateOrganizationalUnit(UpdateOrganizationalUnit command) {
        requireName(command.name(), "El nombre de la Unidad Orgánica es obligatorio");
        OrganizationalUnitEntity unit = organizationalUnits.findByIdForUpdate(command.id())
            .orElseThrow(() -> new NotFoundException("Unidad Orgánica inexistente"));
        LocalAccessContext actor = requireInstitution(unit.getExecutingUnit().getInstitution().getId());
        requireVersion(unit.getVersion(), command.expectedVersion());
        if (unit.isActive()) requireAcronym(command.acronym());
        SnapshotOrganizational before = snapshot(unit);
        unit.updateDetails(command.name(), command.acronym());
        auditOrganizationalUnit("UO_ACTUALIZADA", unit, before, unit, actor.subject());
        return organizationalUnitView(unit);
    }

    @Transactional
    public OrganizationalUnit deactivateOrganizationalUnit(Long id, long expectedVersion) {
        OrganizationalUnitEntity unit = organizationalUnits.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Unidad Orgánica inexistente"));
        LocalAccessContext actor = requireInstitution(unit.getExecutingUnit().getInstitution().getId());
        requireVersion(unit.getVersion(), expectedVersion);
        if (!unit.isActive()) throw incompatibleState("La Unidad Orgánica ya está inactiva");
        SnapshotOrganizational before = snapshot(unit);
        unit.deactivate();
        auditOrganizationalUnit("UO_DESACTIVADA", unit, before, unit, actor.subject());
        return organizationalUnitView(unit);
    }

    @Transactional
    public OrganizationalUnit reactivateOrganizationalUnit(Long id, long expectedVersion) {
        OrganizationalUnitEntity unit = organizationalUnits.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Unidad Orgánica inexistente"));
        LocalAccessContext actor = requireInstitution(unit.getExecutingUnit().getInstitution().getId());
        requireVersion(unit.getVersion(), expectedVersion);
        if (unit.isActive()) throw incompatibleState("La Unidad Orgánica ya está activa");
        requireAcronym(unit.getAcronym());
        SnapshotOrganizational before = snapshot(unit);
        unit.reactivate();
        auditOrganizationalUnit("UO_REACTIVADA", unit, before, unit, actor.subject());
        return organizationalUnitView(unit);
    }

    private LocalAccessContext requireInstitution(Long institutionId) {
        return authorization.requireOrganizationAdministration(institutionId);
    }

    private InstitutionEntity requireInstitutionEntity(Long institutionId) {
        return institutions.findById(institutionId).orElseThrow(() -> new NotFoundException("Institución inexistente"));
    }

    private ExecutingUnitEntity authorizedExecutingUnit(Long id, boolean lock) {
        Optional<ExecutingUnitEntity> located = lock ? executingUnits.findByIdForUpdate(id) : executingUnits.findById(id);
        ExecutingUnitEntity unit = located.orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        requireInstitution(unit.getInstitution().getId());
        requireActiveInstitution(unit.getInstitution());
        return unit;
    }

    private void requireActiveInstitution(InstitutionEntity institution) {
        if (!institution.isActive()) throw new BusinessRuleException(ProblemCode.INVALID_ACTIVE_REFERENCE, "La institución no está activa");
    }

    private int nextDisplayOrder(Long institutionId) {
        return executingUnits.findMaxDisplayOrderByInstitutionId(institutionId) + 1;
    }

    private String nextCode(List<ExecutingUnitEntity> units, Pattern pattern, String prefix) {
        int next = nextExecutingUnitSequence(units, pattern) + 1;
        return String.format(Locale.ROOT, "%s%03d", prefix, next);
    }

    private int nextOrganizationalUnitSequence(List<OrganizationalUnitEntity> units, Pattern pattern) {
        return units.stream().map(OrganizationalUnitEntity::getCode).mapToInt(code -> sequence(code, pattern)).max().orElse(0);
    }

    private int nextExecutingUnitSequence(List<ExecutingUnitEntity> units, Pattern pattern) {
        return units.stream().map(ExecutingUnitEntity::getCode).mapToInt(code -> sequence(code, pattern)).max().orElse(0);
    }

    private int sequence(String code, Pattern pattern) {
        if (code == null) return 0;
        Matcher matcher = pattern.matcher(code);
        if (!matcher.matches()) return 0;
        try { return Integer.parseInt(matcher.group(1)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private void rejectExistingExecutingUnitCode(Long institutionId, String code) {
        if (executingUnits.findByInstitutionIdAndCodeIgnoreCaseForUpdate(institutionId, code).isPresent()) throw duplicateCode(code);
    }

    private void requireVersion(long actual, long expected) {
        if (actual != expected) throw new StaleVersionException();
    }

    private void requireName(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessRuleException(ProblemCode.INVALID_REQUEST, message);
    }

    private void requireAcronym(String value) {
        if (value == null || value.isBlank()) throw new BusinessRuleException(ProblemCode.INVALID_REQUEST,
            "La sigla de la Unidad Orgánica es obligatoria y no puede estar vacía");
    }

    private void requireNonNegative(Integer value, String message) {
        if (value != null && value < 0) throw new BusinessRuleException(ProblemCode.INVALID_REQUEST, message);
    }

    private BusinessRuleException incompatibleState(String message) {
        return new BusinessRuleException(ProblemCode.INCOMPATIBLE_ASSIGNMENT_STATE, message);
    }

    private BusinessRuleException duplicateCode(String code) {
        return new BusinessRuleException(ProblemCode.ORGANIZATION_CODE_DUPLICATE, "El código generado ya existe: " + code);
    }

    private Institution institutionView(InstitutionEntity value) {
        return new Institution(value.getId(), value.getCode(), value.getName());
    }

    private InstitutionReference institutionReference(InstitutionEntity value) {
        return new InstitutionReference(value.getId(), value.getCode(), value.getName());
    }

    private ExecutingUnit executingUnitView(ExecutingUnitEntity value) {
        return new ExecutingUnit(value.getId(), value.getCode(), value.getName(), value.isActive(), value.getDisplayOrder(),
            value.getRegisteredAt(), value.getActivatedAt(), value.getVersion(), institutionReference(value.getInstitution()));
    }

    private OrganizationalUnit organizationalUnitView(OrganizationalUnitEntity value) {
        ExecutingUnitEntity context = value.getExecutingUnit();
        return new OrganizationalUnit(value.getId(), value.getCode(), value.getName(), value.getAcronym(), value.isActive(),
            value.getVersion(), new ExecutingUnitReference(context.getId(), context.getCode(), context.getName()));
    }

    private Snapshot snapshot(ExecutingUnitEntity value) {
        return new Snapshot(value.getId(), value.getCode(), value.getName(), value.isActive(), value.getDisplayOrder(),
            value.getRegisteredAt(), value.getActivatedAt(), value.getVersion());
    }

    private SnapshotOrganizational snapshot(OrganizationalUnitEntity value) {
        return new SnapshotOrganizational(value.getId(), value.getCode(), value.getName(), value.getAcronym(), value.isActive(), value.getVersion());
    }

    private void auditExecutingUnit(String action, ExecutingUnitEntity value, Object before, ExecutingUnitEntity after, String actorSubject) {
        Map<String, Object> detail = detail(action, before, snapshot(after), actorSubject);
        audit.event(action, "UNIDAD_EJECUTORA", value.getId(), value.getCode(), value.getInstitution().getId(), value.getId(), null,
            detail, actorSubject);
    }

    private void auditOrganizationalUnit(String action, OrganizationalUnitEntity value, Object before,
            OrganizationalUnitEntity after, String actorSubject) {
        Map<String, Object> detail = detail(action, before, snapshot(after), actorSubject);
        audit.event(action, "UNIDAD_ORGANICA", value.getId(), value.getCode(), value.getExecutingUnit().getInstitution().getId(),
            value.getExecutingUnit().getId(), value.getId(), detail, actorSubject);
    }

    private Map<String, Object> detail(String action, Object before, Object after, String actorSubject) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("actor", actorSubject);
        detail.put("action", action);
        detail.put("before", before);
        detail.put("after", after);
        detail.put("result", "SUCCESS");
        return detail;
    }

    private record Snapshot(Long id, String code, String name, boolean active, int displayOrder,
            Instant registeredAt, Instant activatedAt, long version) {}

    private record SnapshotOrganizational(Long id, String code, String name, String acronym, boolean active, long version) {}
}
