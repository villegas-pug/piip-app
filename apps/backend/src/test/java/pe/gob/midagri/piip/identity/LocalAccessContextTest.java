package pe.gob.midagri.piip.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.domain.RoleCode;

class LocalAccessContextTest {
    @Test
    void institutionWideScopeDoesNotLeakToAnotherInstitution() {
        LocalAccessContext context = new LocalAccessContext(1L, "subject",
            Set.of(RoleCode.ADMINISTRADOR_PIIP), Set.of(10L, 20L), Set.of(200L), Set.of(10L));

        assertThat(context.coversExecutingUnit(100L, 10L)).isTrue();
        assertThat(context.coversExecutingUnit(200L, 20L)).isTrue();
        assertThat(context.coversExecutingUnit(300L, 20L)).isFalse();
    }
}
