package pe.gob.midagri.piip.identity.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.api.AdminDtos.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.shared.api.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
            InstitutionRepository institutions, ExecutingUnitRepository units, LocalAuthorizationService authorization, AuditService audit) {
        this.users = users;
        this.roles = roles;
        this.scopes = scopes;
        this.institutions = institutions;
        this.units = units;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        LocalAccessContext actor = currentAdministrator();
        return scopes.findForAdministration(actor.institutionIds()).stream()
            .filter(scope -> isCovered(actor, scope))
            .collect(Collectors.groupingBy(UserRoleScopeEntity::getUser, LinkedHashMap::new, Collectors.toList()))
            .entrySet().stream()
            .map(entry -> toResponse(entry.getKey(), entry.getValue()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<UserAssignmentCandidateResponse> listAssignmentCandidates() {
        currentAdministrator();
        return users.findWithoutRoleScopeHistory().stream()
            .map(user -> new UserAssignmentCandidateResponse(user.getId(), user.getKeycloakSubject(), user.getFullName(), user.getEmail()))
            .toList();
    }

    @Transactional
    public ScopeResponse assign(RoleAssignmentRequest request) {
        LocalAccessContext actor = currentAdministrator();
        Instant now = Instant.now();
        TargetScope target = resolveTarget(request.role(), request.institutionId(), request.executingUnitId(), actor);
        // El bloqueo del usuario es el punto de serialización incluso cuando aún no existe una fila duplicada.
        UserEntity user = users.findByKeycloakSubjectForUpdate(request.userSubject())
            .orElseThrow(() -> new NotFoundException("El usuario debe autenticarse al menos una vez antes de recibir un rol"));
        if (!scopes.findActiveDuplicatesForUpdate(user.getId(), target.role().getId(), target.institution().getId(), target.unitId(), now).isEmpty()) {
            throw new BusinessRuleException("La asignación ya está activa");
        }
        UserRoleScopeEntity scope = scopes.save(new UserRoleScopeEntity(user, target.role(), target.institution(), target.unit(), actor.subject()));
        audit.event("ROL_ASIGNADO", "USUARIO", user.getKeycloakSubject(), Map.of("despues", describe(scope)), actor.subject());
        return toScope(scope);
    }

    @Transactional
    public ScopeResponse update(Long scopeId, long expectedVersion, RoleAssignmentUpdateRequest request) {
        LocalAccessContext actor = currentAdministrator();
        Instant now = Instant.now();
        UserRoleScopeEntity scope = lockScopeForUserMutation(scopeId);
        requireVersion(scope.getVersion(), expectedVersion);
        requireCovered(actor, scope);
        if (!scope.isActiveNow(now)) throw new BusinessRuleException("Solo se pueden actualizar asignaciones activas");
        String before = describe(scope);
        TargetScope target = resolveTarget(request.role(), request.institutionId(), request.executingUnitId(), actor);
        boolean unchanged = scope.getRole().getId().equals(target.role().getId())
            && scope.getInstitution().getId().equals(target.institution().getId())
            && Objects.equals(idOf(scope.getExecutingUnit()), target.unitId());
        if (!unchanged && !scopes.findActiveDuplicatesForUpdate(scope.getUser().getId(), target.role().getId(),
                target.institution().getId(), target.unitId(), now).stream().allMatch(existing -> existing.getId().equals(scope.getId()))) {
            throw new BusinessRuleException("La asignación ya está activa");
        }
        if (scope.getRole().getCode() == RoleCode.ADMINISTRADOR_PIIP && !unchanged) ensureNotLastAdministrator(scope, now);
        scope.update(target.role(), target.institution(), target.unit());
        audit.event("ROL_ACTUALIZADO", "USUARIO", scope.getUser().getKeycloakSubject(),
            Map.of("antes", before, "despues", describe(scope)), actor.subject());
        return toScope(scope);
    }

    @Transactional
    public void suspend(Long scopeId, long expectedVersion) {
        LocalAccessContext actor = currentAdministrator();
        Instant now = Instant.now();
        UserRoleScopeEntity scope = lockScopeForUserMutation(scopeId);
        requireVersion(scope.getVersion(), expectedVersion);
        requireCovered(actor, scope);
        if (!scope.isActiveNow(now)) throw new BusinessRuleException("La asignación no está activa");
        ensureNotLastAdministrator(scope, now);
        String before = describe(scope);
        scope.suspend(now);
        audit.event("ROL_SUSPENDIDO", "USUARIO", scope.getUser().getKeycloakSubject(), Map.of("antes", before), actor.subject());
    }

    @Transactional
    public ScopeResponse reactivate(Long scopeId, long expectedVersion) {
        LocalAccessContext actor = currentAdministrator();
        Instant now = Instant.now();
        UserRoleScopeEntity scope = lockScopeForUserMutation(scopeId);
        requireVersion(scope.getVersion(), expectedVersion);
        requireCovered(actor, scope);
        if (scope.isActiveNow(now)) throw new BusinessRuleException("La asignación ya está activa");
        TargetScope target = resolveTarget(scope.getRole().getCode(), scope.getInstitution().getId(), idOf(scope.getExecutingUnit()), actor);
        if (!scopes.findActiveDuplicatesForUpdate(scope.getUser().getId(), target.role().getId(), target.institution().getId(), target.unitId(), now).isEmpty()) {
            throw new BusinessRuleException("La asignación ya está activa");
        }
        scope.reactivate(now);
        audit.event("ROL_REACTIVADO", "USUARIO", scope.getUser().getKeycloakSubject(), Map.of("despues", describe(scope)), actor.subject());
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
        if (!role.isActive()) throw new BusinessRuleException("El rol no está activo");
        InstitutionEntity institution = institutions.findById(institutionId).orElseThrow(() -> new NotFoundException("Institución inexistente"));
        if (!institution.isActive()) throw new BusinessRuleException("La institución no está activa");
        ExecutingUnitEntity unit = unitId == null ? null : units.findById(unitId).orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        if (unit != null && (!unit.isActive() || !unit.getInstitution().getId().equals(institution.getId()))) {
            throw new BusinessRuleException("La Unidad Ejecutora debe estar activa y pertenecer a la institución");
        }
        if (unit == null ? !actor.coversInstitutionWide(institution.getId()) : !actor.coversExecutingUnit(unit.getId(), institution.getId())) {
            throw new AccessDeniedException("Ámbito fuera de la cobertura autorizada");
        }
        return new TargetScope(role, institution, unit);
    }

    private void ensureNotLastAdministrator(UserRoleScopeEntity scope, Instant now) {
        if (scope.getRole().getCode() != RoleCode.ADMINISTRADOR_PIIP) return;
        if (scopes.findActiveAdministratorsForUpdate(RoleCode.ADMINISTRADOR_PIIP, scope.getInstitution().getId(), idOf(scope.getExecutingUnit()), now).size() <= 1) {
            throw new BusinessRuleException("No se puede desactivar el último administrador del ámbito");
        }
    }

    private boolean isCovered(LocalAccessContext actor, UserRoleScopeEntity scope) {
        return scope.getExecutingUnit() == null
            ? actor.coversInstitutionWide(scope.getInstitution().getId())
            : actor.coversExecutingUnit(scope.getExecutingUnit().getId(), scope.getInstitution().getId());
    }

    private void requireCovered(LocalAccessContext actor, UserRoleScopeEntity scope) {
        if (!isCovered(actor, scope)) throw new AccessDeniedException("Asignación fuera del ámbito autorizado");
    }

    private void requireVersion(long actual, long expected) {
        if (actual != expected) throw new StaleVersionException();
    }

    private UserRoleScopeEntity lockScopeForUserMutation(Long scopeId) {
        UserRoleScopeEntity located = scopes.findById(scopeId).orElseThrow(() -> new NotFoundException("Asignación inexistente"));
        users.findByIdForUpdate(located.getUser().getId()).orElseThrow(() -> new NotFoundException("Usuario inexistente"));
        if (located.getRole().getCode() == RoleCode.ADMINISTRADOR_PIIP) lockAdministrationCoverage(located, Instant.now());
        return scopes.findByIdForUpdate(scopeId).orElseThrow(() -> new NotFoundException("Asignación inexistente"));
    }

    private void lockAdministrationCoverage(UserRoleScopeEntity scope, Instant now) {
        scopes.findActiveAdministratorsForUpdate(RoleCode.ADMINISTRADOR_PIIP, scope.getInstitution().getId(), idOf(scope.getExecutingUnit()), now);
    }

    private Long idOf(ExecutingUnitEntity unit) { return unit == null ? null : unit.getId(); }
    private String describe(UserRoleScopeEntity scope) {
        return scope.getRole().getCode() + ":" + scope.getInstitution().getCode() + ":" + (scope.getExecutingUnit() == null ? "TODAS" : scope.getExecutingUnit().getCode());
    }
    private UserResponse toResponse(UserEntity user, List<UserRoleScopeEntity> userScopes) {
        return new UserResponse(user.getId(), user.getKeycloakSubject(), user.getFullName(), user.getEmail(), userScopes.stream().map(this::toScope).toList());
    }
    private ScopeResponse toScope(UserRoleScopeEntity scope) {
        return new ScopeResponse(scope.getId(), scope.getRole().getCode(), scope.getInstitution().getId(), scope.getInstitution().getName(),
            idOf(scope.getExecutingUnit()), scope.getExecutingUnit() == null ? "Todas" : scope.getExecutingUnit().getName(),
            scope.isActive(), scope.getValidFrom(), scope.getValidUntil(), scope.getVersion());
    }

    private record TargetScope(RoleEntity role, InstitutionEntity institution, ExecutingUnitEntity unit) {
        Long unitId() { return unit == null ? null : unit.getId(); }
    }
}
