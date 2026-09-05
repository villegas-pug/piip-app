package pe.gob.midagri.piip.identity.application;

import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;

/** Impide arrancar producción sin un ámbito administrador vigente. */
@Component
@Profile("prod & !test-reset")
@Order(Ordered.LOWEST_PRECEDENCE)
public final class ProductionAdminGuard implements ApplicationRunner {
    private final UserRoleScopeRepository scopes;

    public ProductionAdminGuard(UserRoleScopeRepository scopes) {
        this.scopes = scopes;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (scopes.countActiveByRole(RoleCode.ADMINISTRADOR_PIIP, Instant.now()) == 0) {
            throw new IllegalStateException("Producción requiere un ámbito activo ADMINISTRADOR_PIIP");
        }
    }
}
