package pe.gob.midagri.piip.identity.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.shared.api.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/identity")
public class IdentityController {
    private final LocalAuthorizationService authorization; private final UserRepository users;
    public IdentityController(LocalAuthorizationService authorization, UserRepository users) { this.authorization = authorization; this.users = users; }
    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        LocalAccessContext context = authorization.requireAuthenticatedRole();
        authorization.recordAuthentication(jwt.getSubject(), jwt.getClaimAsString("name"), jwt.getClaimAsString("email"));
        UserEntity user = users.findById(context.userId()).orElseThrow(() -> new NotFoundException("Usuario inexistente"));
        List<RoleScopeResponse> roleScopes = context.grants().stream()
            .map(grant -> new RoleScopeResponse(grant.role(), grant.institutionId(), grant.executingUnitId()))
            .sorted(Comparator.comparing(RoleScopeResponse::institutionId)
                .thenComparing(RoleScopeResponse::executingUnitId, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(RoleScopeResponse::role))
            .toList();
        return new CurrentUserResponse(user.getKeycloakSubject(), user.getFullName(), user.getEmail(), roleScopes,
            context.roles(), context.institutionIds(), context.executingUnitIds(), context.institutionWide());
    }
    public record RoleScopeResponse(pe.gob.midagri.piip.identity.domain.RoleCode role, Long institutionId, Long executingUnitId) {}
    public record CurrentUserResponse(String subject, String fullName, String email, List<RoleScopeResponse> roleScopes,
            Set<pe.gob.midagri.piip.identity.domain.RoleCode> roles, Set<Long> institutionIds, Set<Long> executingUnitIds,
            boolean institutionWide) {}
}
