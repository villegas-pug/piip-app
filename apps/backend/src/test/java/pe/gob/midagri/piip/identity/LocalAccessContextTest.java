package pe.gob.midagri.piip.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;

class LocalAccessContextTest {
    @Test
    void institutionWideScopeDoesNotLeakToAnotherInstitution() {
        LocalAccessContext context = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, null),
                new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 20L, 200L)));

        assertThat(context.coversExecutingUnit(100L, 10L)).isTrue();
        assertThat(context.coversExecutingUnit(200L, 20L)).isTrue();
        assertThat(context.coversExecutingUnit(300L, 20L)).isFalse();
    }

    @Test
    void doesNotCombineAdministratorRoleWithAnotherGrantScope() {
        LocalAccessContext context = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
                new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, 200L)));

        assertThat(context.coversExecutingUnit(100L, 10L)).isTrue();
        assertThat(context.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP, 100L, 10L)).isFalse();
        assertThat(context.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP, 200L, 20L)).isTrue();
        assertThat(context.institutionIds(RoleCode.ADMINISTRADOR_PIIP)).containsExactly(20L);
        assertThat(context.institutionIds(RoleCode.CONSULTA_EXTERNA)).containsExactly(10L);
    }
}
