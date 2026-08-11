package pe.gob.midagri.piip.identity.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import java.time.Instant;
import java.util.*;

@Service
public class LocalAuthorizationService {
    private final UserRepository users;
    private final UserRoleScopeRepository scopes;
    private final ExecutingUnitRepository executingUnits;

    public LocalAuthorizationService(UserRepository users, UserRoleScopeRepository scopes, ExecutingUnitRepository executingUnits) { this.users = users; this.scopes = scopes; this.executingUnits = executingUnits; }

    @Transactional(readOnly = true)
    public LocalAccessContext resolve(String subject) {
        UserEntity user = users.findByKeycloakSubject(subject)
            .orElseThrow(() -> new AccessDeniedException("El usuario no está provisionado en PIIP"));
        List<UserRoleScopeEntity> activeScopes = scopes.findActiveBySubject(subject, Instant.now());
        Set<RoleScopeGrant> grants = activeScopes.stream()
            .map(scope -> new RoleScopeGrant(scope.getRole().getCode(), scope.getInstitution().getId(),
                scope.getExecutingUnit() == null ? null : scope.getExecutingUnit().getId()))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new LocalAccessContext(user.getId(), subject, grants);
    }

    @Transactional
    public void recordAuthentication(String subject, String name, String email) {
        UserEntity user = users.findByKeycloakSubjectForAuthenticationUpdate(subject)
            .orElseThrow(() -> new AccessDeniedException("El usuario no está provisionado en PIIP"));
        user.recordAuthentication(name, email);
    }

    public LocalAccessContext current() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof LocalAccessContext context) return context;
        throw new AccessDeniedException("El usuario no tiene una asignación local activa");
    }

    public LocalAccessContext require(RoleCode role) {
        LocalAccessContext context = current();
        if (!context.hasRole(role)) throw new AccessDeniedException("Se requiere el rol " + role);
        return context;
    }

    public LocalAccessContext requireAuthenticatedRole() {
        LocalAccessContext context = current();
        if (context.roles().isEmpty()) throw new AccessDeniedException("El usuario no tiene un rol PIIP activo");
        return context;
    }

    public LocalAccessContext requireReadableUnit(Long unitId) {
        LocalAccessContext context = requireAuthenticatedRole();
        if (!coversUnit(context, unitId)) throw new AccessDeniedException("La Unidad Ejecutora está fuera del ámbito autorizado");
        return context;
    }

    public LocalAccessContext requireUnit(RoleCode role, Long unitId) {
        LocalAccessContext context = require(role);
        ExecutingUnitEntity unit = unit(unitId);
        if (!context.coversExecutingUnit(role, unitId, unit.getInstitution().getId())) {
            throw new AccessDeniedException("La Unidad Ejecutora está fuera del ámbito autorizado para el rol " + role);
        }
        return context;
    }

    private boolean coversUnit(LocalAccessContext context, Long unitId) {
        ExecutingUnitEntity unit = unit(unitId);
        return context.coversExecutingUnit(unitId, unit.getInstitution().getId());
    }

    private ExecutingUnitEntity unit(Long unitId) {
        return executingUnits.findById(unitId).orElseThrow(() -> new AccessDeniedException("Unidad Ejecutora inexistente"));
    }
}
