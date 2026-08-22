package pe.gob.midagri.piip.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class CurrentIdentityServiceTest {
    @Mock LocalAuthorizationService authorization;
    @Mock UserRepository users;

    @Test
    void recordsAuthenticationAndReturnsTheVisibleUserAndSortedGrant() {
        LocalAccessContext context = new LocalAccessContext(7L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, 100L)));
        UserEntity user = new UserEntity("subject", "Persona", "persona@example.test");
        ReflectionTestUtils.setField(user, "id", 7L);
        when(authorization.requireAuthenticatedRole()).thenReturn(context);
        when(users.findById(7L)).thenReturn(Optional.of(user));

        CurrentIdentityReadModel result = new CurrentIdentityService(authorization, users)
            .me("subject", "Persona", "persona@example.test");

        assertThat(result.subject()).isEqualTo("subject");
        assertThat(result.roleScopes()).containsExactly(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, 100L));
        verify(authorization).recordAuthentication("subject", "Persona", "persona@example.test");
    }
}
