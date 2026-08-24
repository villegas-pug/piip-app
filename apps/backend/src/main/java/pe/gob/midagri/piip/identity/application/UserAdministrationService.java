package pe.gob.midagri.piip.identity.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.UserAdministrationCommands.AssignCommand;
import pe.gob.midagri.piip.identity.application.UserAdministrationCommands.UpdateCommand;
import pe.gob.midagri.piip.identity.application.UserAdministrationReadModels.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.shared.application.error.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserAdministrationService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final UserRoleScopeRepository scopes;
    private final InstitutionRepository institutions;
    private final ExecutingUnitRepository units;
    private final LocalAuthorizationService authorization;
    private final AuditService audit;

    public UserAdministrationService(UserRepository users, RoleRepository roles, UserRoleScopeRepository scopes,
            InstitutionRepository institutions, ExecutingUnitRepository units, LocalAuthorizationService authorization,
            AuditService audit) {
        this.users = users; this.roles = roles; this.scopes = scopes; this.institutions = institutions;
        this.units = units; this.authorization = authorization; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        LocalAccessContext actor = currentAdministrator();
        return scopes.findForAdministration(actor.institutionIds(RoleCode.ADMINISTRADOR_PIIP)).stream()
            .collect(Collectors.groupingBy(UserRoleScopeEntity::getUser, LinkedHashMap::new, Collectors.toList()))
            .entrySet().stream()
            .map(entry -> new User(entry.getKey().getId(), entry.getKey().getKeycloakSubject(), entry.getKey().getFullName(),
                entry.getKey().getEmail(), entry.getValue().stream().map(this::toScope).toList())).toList();
    }

    @Transactional(readOnly = true)
    public List<AdministrableScope> listAdministrableScopes() {
        Set<Long> institutionIds = currentAdministrator().institutionIds(RoleCode.ADMINISTRADOR_PIIP);
        return institutions.findAllById(institutionIds).stream().filter(InstitutionEntity::isActive)
            .sorted(Comparator.comparing(InstitutionEntity::getName))
            .map(institution -> new AdministrableScope(institution.getId(), institution.getCode(), institution.getName(), true,
                units.findByInstitutionIdAndActiveTrueOrderByName(institution.getId()).stream()
                    .map(unit -> new AdministrableExecutingUnit(unit.getId(), unit.getCode(), unit.getName())).toList())).toList();
    }

    @Transactional(readOnly = true)
    public List<UserAssignmentCandidate> listAssignmentCandidates() {
        currentAdministrator();
        return users.findWithoutRoleScopeHistory().stream()
            .map(user -> new UserAssignmentCandidate(user.getId(), user.getKeycloakSubject(), user.getFullName(), user.getEmail())).toList();
    }

    @Transactional
    public AssignmentMutationResult assign(AssignCommand command) {
        LocalAccessContext actor = currentAdministrator();
        Instant now = Instant.now();
        TargetScope target = resolveTarget(command.role(), command.institutionId(), command.executingUnitId(), actor);
        UserEntity locatedUser = users.findByKeycloakSubject(command.userSubject())
            .orElseGet(() -> users.findByKeycloakSubjectForUpdate(command.userSubject())
                .orElseThrow(() -> new NotFoundException("El usuario debe autenticarse al menos una vez antes de recibir un rol")));
        UserEntity user = lockUsers(actor.userId(), locatedUser.getId()).stream().filter(value -> value.getId().equals(locatedUser.getId())).findFirst()
            .orElse(locatedUser);
        if (!scopes.findActiveDuplicatesForUpdate(user.getId(), target.role().getId(), target.institution().getId(), target.unitId(), now).isEmpty()) {
            throw new BusinessRuleException(ProblemCode.ACTIVE_ASSIGNMENT_DUPLICATE, "La asignación ya está activa");
        }
        Optional<UserRoleScopeEntity> suspended = Optional.ofNullable(scopes.findLatestSuspendedExactForUpdate(user.getId(), target.role().getId(),
            target.institution().getId(), target.unitId())).orElse(Optional.empty());
        if (suspended.isPresent()) {
            UserRoleScopeEntity scope = suspended.get();
            AssignmentSnapshot before = snapshot(scope);
            scope.reactivate(now);
            auditSuccess("ROL_REACTIVADO", scope, before, snapshot(scope), actor.subject());
            return new AssignmentMutationResult(AssignmentMutationStatus.REACTIVATED, toScope(scope));
        }
        UserRoleScopeEntity scope = scopes.save(new UserRoleScopeEntity(user, target.role(), target.institution(), target.unit(), actor.subject()));
        auditSuccess("ROL_ASIGNADO", scope, null, snapshot(scope), actor.subject());
        return new AssignmentMutationResult(AssignmentMutationStatus.CREATED, toScope(scope));
    }

    @Transactional
    public Scope update(Long scopeId, long expectedVersion, UpdateCommand command) {
        LocalAccessContext actor = currentAdministrator();
        Instant now = Instant.now();
        UserRoleScopeEntity scope = lockScopeForUserMutation(scopeId, actor.userId());
        requireVersion(scope.getVersion(), expectedVersion); requireCovered(actor, scope); requireActive(scope);
        AssignmentSnapshot before = snapshot(scope);
        TargetScope target = resolveTarget(command.role(), command.institutionId(), command.executingUnitId(), actor);
        boolean unchanged = sameTarget(scope, target);
        if (!unchanged && !scopes.findActiveDuplicatesForUpdate(scope.getUser().getId(), target.role().getId(), target.institution().getId(),
                target.unitId(), now).stream().allMatch(existing -> existing.getId().equals(scope.getId()))) {
            throw new BusinessRuleException(ProblemCode.ACTIVE_ASSIGNMENT_DUPLICATE, "La asignación ya está activa");
        }
        if (scope.getRole().getCode() == RoleCode.ADMINISTRADOR_PIIP && !unchanged) ensureCoverageAfterMutation(scope, target, now);
        scope.update(target.role(), target.institution(), target.unit());
        auditSuccess("ROL_ACTUALIZADO", scope, before, snapshot(scope), actor.subject());
        return toScope(scope);
    }

    @Transactional
    public void suspend(Long scopeId, long expectedVersion) {
        LocalAccessContext actor = currentAdministrator(); Instant now = Instant.now();
        UserRoleScopeEntity scope = lockScopeForUserMutation(scopeId, actor.userId());
        requireVersion(scope.getVersion(), expectedVersion); requireCovered(actor, scope); requireActive(scope);
        if (scope.getRole().getCode() == RoleCode.ADMINISTRADOR_PIIP && scope.getUser().getId().equals(actor.userId())) {
            throw new BusinessRuleException(ProblemCode.SELF_ADMIN_SUSPENSION, "No se puede suspender la propia asignación ADMINISTRADOR_PIIP");
        }
        if (scope.getRole().getCode() == RoleCode.ADMINISTRADOR_PIIP) ensureCoverageAfterMutation(scope, null, now);
        AssignmentSnapshot before = snapshot(scope); scope.suspend(now);
        auditSuccess("ROL_SUSPENDIDO", scope, before, snapshot(scope), actor.subject());
    }

    @Transactional
    public Scope reactivate(Long scopeId, long expectedVersion) {
        LocalAccessContext actor = currentAdministrator(); Instant now = Instant.now();
        UserRoleScopeEntity scope = lockScopeForUserMutation(scopeId, actor.userId());
        requireVersion(scope.getVersion(), expectedVersion); requireCovered(actor, scope);
        if (scope.isActiveNow(now)) throw new BusinessRuleException(ProblemCode.INCOMPATIBLE_ASSIGNMENT_STATE, "La asignación ya está activa");
        TargetScope target = resolveTarget(scope.getRole().getCode(), scope.getInstitution().getId(), idOf(scope.getExecutingUnit()), actor);
        if (!scopes.findActiveDuplicatesForUpdate(scope.getUser().getId(), target.role().getId(), target.institution().getId(), target.unitId(), now).isEmpty()) {
            throw new BusinessRuleException(ProblemCode.ACTIVE_ASSIGNMENT_DUPLICATE, "La asignación ya está activa");
        }
        AssignmentSnapshot before = snapshot(scope); scope.reactivate(now);
        auditSuccess("ROL_REACTIVADO", scope, before, snapshot(scope), actor.subject());
        return toScope(scope);
    }

    private LocalAccessContext currentAdministrator() {
        LocalAccessContext snapshot = authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        LocalAccessContext persistent = authorization.resolve(snapshot.subject());
        if (!persistent.hasRole(RoleCode.ADMINISTRADOR_PIIP)) throw new AccessDeniedException("Se requiere el rol ADMINISTRADOR_PIIP");
        return persistent;
    }

    private TargetScope resolveTarget(RoleCode roleCode, Long institutionId, Long unitId, LocalAccessContext actor) {
        RoleEntity role = roles.findByCode(roleCode).orElseThrow(() -> new NotFoundException("Rol inexistente"));
        if (!role.isActive()) throw new BusinessRuleException(ProblemCode.INVALID_ACTIVE_REFERENCE, "El rol no está activo");
        InstitutionEntity institution = institutions.findById(institutionId).orElseThrow(() -> new NotFoundException("Institución inexistente"));
        if (!institution.isActive()) throw new BusinessRuleException(ProblemCode.INVALID_ACTIVE_REFERENCE, "La institución no está activa");
        ExecutingUnitEntity unit = unitId == null ? null : units.findById(unitId).orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        if (unit != null && (!unit.isActive() || !unit.getInstitution().getId().equals(institution.getId()))) {
            throw new BusinessRuleException(ProblemCode.INVALID_ACTIVE_REFERENCE, "La Unidad Ejecutora debe estar activa y pertenecer a la institución");
        }
        if (!administersInstitution(actor, institution.getId())) throw new AccessDeniedException("Ámbito fuera de la cobertura autorizada");
        return new TargetScope(role, institution, unit);
    }

    private void ensureCoverageAfterMutation(UserRoleScopeEntity scope, TargetScope target, Instant now) {
        Set<Long> affectedUnits = new LinkedHashSet<>(); Long oldUnitId = idOf(scope.getExecutingUnit());
        if (oldUnitId != null) affectedUnits.add(oldUnitId);
        else {
            units.findByInstitutionIdAndActiveTrueOrderByName(scope.getInstitution().getId()).forEach(unit -> affectedUnits.add(unit.getId()));
            if (affectedUnits.isEmpty()) affectedUnits.add(null);
        }
        for (Long unitId : affectedUnits) {
            List<UserRoleScopeEntity> administrators = scopes.findActiveAdministratorsForUpdate(RoleCode.ADMINISTRADOR_PIIP,
                scope.getInstitution().getId(), unitId, now);
            long remaining = administrators.stream().filter(existing -> !existing.getId().equals(scope.getId())).count();
            boolean destinationCovers = target != null && target.role().getCode() == RoleCode.ADMINISTRADOR_PIIP
                && target.institution().getId().equals(scope.getInstitution().getId()) && (target.unitId() == null || Objects.equals(target.unitId(), unitId));
            if (remaining + (destinationCovers ? 1 : 0) == 0) throw new BusinessRuleException(ProblemCode.LAST_ACTIVE_ADMIN,
                "No se puede dejar una Unidad Ejecutora sin administrador activo");
        }
    }

    private UserRoleScopeEntity lockScopeForUserMutation(Long scopeId, Long actorId) {
        UserRoleScopeEntity located = scopes.findById(scopeId).orElseThrow(() -> new NotFoundException("Asignación inexistente"));
        lockUsers(actorId, located.getUser().getId());
        return scopes.findByIdForUpdate(scopeId).orElseThrow(() -> new NotFoundException("Asignación inexistente"));
    }

    private List<UserEntity> lockUsers(Long actorId, Long targetId) {
        return users.findAllByIdForUpdate(Stream.of(actorId, targetId).filter(Objects::nonNull).distinct().sorted().toList());
    }

    private boolean administersInstitution(LocalAccessContext actor, Long institutionId) { return actor.institutionIds(RoleCode.ADMINISTRADOR_PIIP).contains(institutionId); }
    private void requireCovered(LocalAccessContext actor, UserRoleScopeEntity scope) { if (!administersInstitution(actor, scope.getInstitution().getId())) throw new AccessDeniedException("Asignación fuera del ámbito autorizado"); }
    private void requireActive(UserRoleScopeEntity scope) { if (!scope.isActiveNow(Instant.now())) throw new BusinessRuleException(ProblemCode.INCOMPATIBLE_ASSIGNMENT_STATE, "La asignación no está activa"); }
    private void requireVersion(long actual, long expected) { if (actual != expected) throw new StaleVersionException(); }
    private boolean sameTarget(UserRoleScopeEntity scope, TargetScope target) { return scope.getRole().getId().equals(target.role().getId()) && scope.getInstitution().getId().equals(target.institution().getId()) && Objects.equals(idOf(scope.getExecutingUnit()), target.unitId()); }

    private Scope toScope(UserRoleScopeEntity scope) {
        return new Scope(scope.getId(), scope.getRole().getCode(), scope.getInstitution().getId(), scope.getInstitution().getName(), idOf(scope.getExecutingUnit()),
            scope.getExecutingUnit() == null ? "Todas" : scope.getExecutingUnit().getName(), scope.isActive(), scope.getValidFrom(), scope.getValidUntil(), scope.getVersion());
    }

    private AssignmentSnapshot snapshot(UserRoleScopeEntity scope) {
        return new AssignmentSnapshot(scope.getId(), scope.getUser().getKeycloakSubject(), scope.getRole().getCode(), scope.getInstitution().getId(), scope.getInstitution().getName(),
            idOf(scope.getExecutingUnit()), scope.getExecutingUnit() == null ? "Todas" : scope.getExecutingUnit().getName(), scope.isActive(), scope.getValidFrom(), scope.getValidUntil(), scope.getVersion());
    }

    private void auditSuccess(String action, UserRoleScopeEntity scope, AssignmentSnapshot before, AssignmentSnapshot after, String actorSubject) {
        Map<String, Object> detail = new LinkedHashMap<>(); detail.put("actor", actorSubject); detail.put("action", action);
        detail.put("affectedUser", scope.getUser().getKeycloakSubject()); detail.put("before", before); detail.put("after", after); detail.put("result", "SUCCESS");
        audit.event(action, "USUARIO_ROL_AMBITO", String.valueOf(scope.getId()), detail, actorSubject);
    }

    private Long idOf(ExecutingUnitEntity unit) { return unit == null ? null : unit.getId(); }
    private record TargetScope(RoleEntity role, InstitutionEntity institution, ExecutingUnitEntity unit) { Long unitId() { return unit == null ? null : unit.getId(); } }
}
