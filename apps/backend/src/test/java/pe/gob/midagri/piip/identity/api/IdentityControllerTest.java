package pe.gob.midagri.piip.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {
    @Mock LocalAuthorizationService authorization;
    @Mock UserRepository users;
    @Mock Jwt jwt;
    @InjectMocks IdentityController controller;

    @Test
    void publishesEachActiveGrantWithoutCombiningRoleAndScope() {
        LocalAccessContext context = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
                new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, null)));
        UserEntity user = new UserEntity("subject", "Persona", "persona@example.test");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(authorization.requireAuthenticatedRole()).thenReturn(context);
        when(jwt.getSubject()).thenReturn("subject");
        when(jwt.getClaimAsString("name")).thenReturn("Persona");
        when(jwt.getClaimAsString("email")).thenReturn("persona@example.test");
        when(users.findById(1L)).thenReturn(Optional.of(user));

        IdentityController.CurrentUserResponse response = controller.me(jwt);

        assertThat(response.roleScopes()).containsExactly(
            new IdentityController.RoleScopeResponse(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
            new IdentityController.RoleScopeResponse(RoleCode.ADMINISTRADOR_PIIP, 20L, null));
        assertThat(response.roles()).containsExactlyInAnyOrder(RoleCode.CONSULTA_EXTERNA, RoleCode.ADMINISTRADOR_PIIP);
        verify(authorization).recordAuthentication("subject", "Persona", "persona@example.test");
    }
}
