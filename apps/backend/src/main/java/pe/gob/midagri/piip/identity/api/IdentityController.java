package pe.gob.midagri.piip.identity.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.shared.api.NotFoundException;
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
        return new CurrentUserResponse(user.getKeycloakSubject(), user.getFullName(), user.getEmail(), context.roles(), context.institutionIds(), context.executingUnitIds(), context.institutionWide());
    }
    public record CurrentUserResponse(String subject, String fullName, String email, Set<pe.gob.midagri.piip.identity.domain.RoleCode> roles, Set<Long> institutionIds, Set<Long> executingUnitIds, boolean institutionWide) {}
}
