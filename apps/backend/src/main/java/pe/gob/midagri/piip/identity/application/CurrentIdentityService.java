package pe.gob.midagri.piip.identity.application;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;

@Service
public class CurrentIdentityService {
    private final LocalAuthorizationService authorization;
    private final UserRepository users;

    public CurrentIdentityService(LocalAuthorizationService authorization, UserRepository users) {
        this.authorization = authorization;
        this.users = users;
    }

    public CurrentIdentityReadModel me(String subject, String name, String email) {
        LocalAccessContext context = authorization.requireAuthenticatedRole();
        authorization.recordAuthentication(subject, name, email);
        UserEntity user = users.findById(context.userId())
            .orElseThrow(() -> new NotFoundException("Usuario inexistente"));
        List<RoleScopeGrant> grants = context.grants().stream()
            .sorted(Comparator.comparing(RoleScopeGrant::institutionId)
                .thenComparing(RoleScopeGrant::executingUnitId, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(RoleScopeGrant::role))
            .toList();
        return new CurrentIdentityReadModel(user.getKeycloakSubject(), user.getFullName(), user.getEmail(), grants,
            context.roles(), context.institutionIds(), context.executingUnitIds(), context.institutionWide());
    }
}
