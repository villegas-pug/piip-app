package pe.gob.midagri.piip.identity.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;

class ProductionAdminGuardTest {
    @Test
    void rechazaProduccionSinAmbitoAdministradorActivo() {
        UserRoleScopeRepository scopes = Mockito.mock(UserRoleScopeRepository.class);
        when(scopes.countActiveByRole(Mockito.eq(RoleCode.ADMINISTRADOR_PIIP), Mockito.any(Instant.class))).thenReturn(0L);

        assertThatThrownBy(() -> new ProductionAdminGuard(scopes).run(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ADMINISTRADOR_PIIP");
        verify(scopes).countActiveByRole(Mockito.eq(RoleCode.ADMINISTRADOR_PIIP), Mockito.any(Instant.class));
        verifyNoMoreInteractions(scopes);
    }

    @Test
    void permiteProduccionConAmbitoAdministradorActivoSinMutarPersistencia() {
        UserRoleScopeRepository scopes = Mockito.mock(UserRoleScopeRepository.class);
        when(scopes.countActiveByRole(Mockito.eq(RoleCode.ADMINISTRADOR_PIIP), Mockito.any(Instant.class))).thenReturn(1L);

        assertThatCode(() -> new ProductionAdminGuard(scopes).run(null)).doesNotThrowAnyException();
        verify(scopes).countActiveByRole(Mockito.eq(RoleCode.ADMINISTRADOR_PIIP), Mockito.any(Instant.class));
        verifyNoMoreInteractions(scopes);
    }
}
