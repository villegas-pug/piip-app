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

@Service
public class UserAdministrationService {
    private final UserRepository users; private final RoleRepository roles; private final UserRoleScopeRepository scopes;
    private final InstitutionRepository institutions; private final ExecutingUnitRepository units;
    private final LocalAuthorizationService authorization; private final AuditService audit;

    public UserAdministrationService(UserRepository users, RoleRepository roles, UserRoleScopeRepository scopes,
            InstitutionRepository institutions, ExecutingUnitRepository units, LocalAuthorizationService authorization, AuditService audit) {
        this.users = users; this.roles = roles; this.scopes = scopes; this.institutions = institutions; this.units = units; this.authorization = authorization; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        LocalAccessContext actor = authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        return users.findAll().stream().map(user -> toResponse(user, scopes.findActiveBySubject(user.getKeycloakSubject(), Instant.now()).stream()
            .filter(scope -> actor.institutionIds().contains(scope.getInstitution().getId()))
            .filter(scope -> scope.getExecutingUnit() == null ? actor.coversInstitutionWide(scope.getInstitution().getId()) : actor.coversExecutingUnit(scope.getExecutingUnit().getId(), scope.getInstitution().getId())).toList()))
            .toList();
    }

    @Transactional
    public ScopeResponse assign(RoleAssignmentRequest request) {
        LocalAccessContext actor = authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        InstitutionEntity institution = institutions.findById(request.institutionId()).orElseThrow(() -> new NotFoundException("Institución inexistente"));
        if (!actor.institutionIds().contains(institution.getId())) throw new AccessDeniedException("Institución fuera del ámbito autorizado");
        ExecutingUnitEntity unit = request.executingUnitId() == null ? null : units.findById(request.executingUnitId()).orElseThrow(() -> new NotFoundException("Unidad Ejecutora inexistente"));
        if (unit == null && !actor.coversInstitutionWide(institution.getId())) throw new AccessDeniedException("Solo un administrador institucional puede asignar ámbito institucional");
        if (unit != null && (!unit.getInstitution().getId().equals(institution.getId()) || !actor.coversExecutingUnit(unit.getId(), institution.getId()))) throw new AccessDeniedException("Unidad Ejecutora fuera del ámbito autorizado");
        UserEntity user = users.findByKeycloakSubject(request.userSubject()).orElseThrow(() -> new NotFoundException("El usuario debe autenticarse al menos una vez antes de recibir un rol"));
        RoleEntity role = roles.findByCode(request.role()).orElseThrow(() -> new IllegalStateException("Catálogo de roles incompleto"));
        if (scopes.existsActiveAssignment(user.getId(), role.getId(), institution.getId(), unit == null ? null : unit.getId(), Instant.now())) throw new BusinessRuleException("La asignación ya está activa");
        UserRoleScopeEntity scope = scopes.save(new UserRoleScopeEntity(user, role, institution, unit, actor.subject()));
        audit.event("ROL_ASIGNADO", "USUARIO", user.getKeycloakSubject(), Map.of("rol", request.role().name(), "institucion", institution.getCode(), "unidadEjecutora", unit == null ? "TODAS" : unit.getCode()), actor.subject());
        return toScope(scope);
    }

    @Transactional
    public void suspend(Long scopeId, long expectedVersion) {
        LocalAccessContext actor = authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        UserRoleScopeEntity scope = scopes.findById(scopeId).orElseThrow(() -> new NotFoundException("Asignación inexistente"));
        if (scope.getVersion() != expectedVersion) throw new StaleVersionException();
        if (!actor.institutionIds().contains(scope.getInstitution().getId()) || (scope.getExecutingUnit() == null && !actor.coversInstitutionWide(scope.getInstitution().getId())) || (scope.getExecutingUnit() != null && !actor.coversExecutingUnit(scope.getExecutingUnit().getId(), scope.getInstitution().getId()))) throw new AccessDeniedException("Asignación fuera del ámbito autorizado");
        Long unitId = scope.getExecutingUnit() == null ? null : scope.getExecutingUnit().getId();
        if (scope.getRole().getCode() == RoleCode.ADMINISTRADOR_PIIP && scopes.countActiveAdministrators(RoleCode.ADMINISTRADOR_PIIP, scope.getInstitution().getId(), unitId, Instant.now()) <= 1) throw new BusinessRuleException("No se puede desactivar el último administrador del ámbito");
        scope.suspend(Instant.now());
        audit.event("ROL_SUSPENDIDO", "USUARIO", scope.getUser().getKeycloakSubject(), Map.of("rol", scope.getRole().getCode().name()), actor.subject());
    }

    private UserResponse toResponse(UserEntity user, List<UserRoleScopeEntity> active) { return new UserResponse(user.getId(), user.getKeycloakSubject(), user.getFullName(), user.getEmail(), user.isActive(), active.stream().map(this::toScope).toList()); }
    private ScopeResponse toScope(UserRoleScopeEntity scope) { return new ScopeResponse(scope.getId(), scope.getRole().getCode(), scope.getInstitution().getId(), scope.getInstitution().getName(), scope.getExecutingUnit() == null ? null : scope.getExecutingUnit().getId(), scope.getExecutingUnit() == null ? "Todas" : scope.getExecutingUnit().getName(), scope.isActive(), scope.getValidFrom(), scope.getValidUntil(), scope.getVersion()); }
}
