package pe.gob.midagri.piip.identity.api;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.midagri.piip.identity.application.CurrentIdentityReadModel;
import pe.gob.midagri.piip.identity.application.CurrentIdentityService;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRepository;

@RestController
@RequestMapping("/identity")
public class IdentityController {
    private final CurrentIdentityService service;

    @Autowired
    public IdentityController(CurrentIdentityService service) { this.service = service; }

    /** Constructor de compatibilidad para pruebas unitarias existentes. */
    public IdentityController(LocalAuthorizationService authorization, UserRepository users) {
        this(new CurrentIdentityService(authorization, users));
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        CurrentIdentityReadModel model = service.me(jwt.getSubject(), jwt.getClaimAsString("name"),
            jwt.getClaimAsString("email"));
        List<RoleScopeResponse> roleScopes = model.roleScopes().stream()
            .map(grant -> new RoleScopeResponse(grant.role(), grant.institutionId(), grant.executingUnitId())).toList();
        return new CurrentUserResponse(model.subject(), model.fullName(), model.email(), roleScopes, model.roles(),
            model.institutionIds(), model.executingUnitIds(), model.institutionWide());
    }

    public record RoleScopeResponse(RoleCode role, Long institutionId, Long executingUnitId) {}
    public record CurrentUserResponse(String subject, String fullName, String email, List<RoleScopeResponse> roleScopes,
            Set<RoleCode> roles, Set<Long> institutionIds, Set<Long> executingUnitIds, boolean institutionWide) {}
}
