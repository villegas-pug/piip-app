package pe.gob.midagri.piip.config.reset;

import java.util.Arrays;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/** Rechaza el perfil destructivo antes de crear el contexto JPA. */
public final class TestResetStartupGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch("test-reset"::equals)) return;
        TestResetEnvironmentGuard.validateProfiles(Arrays.asList(environment.getActiveProfiles()));
        TestResetEnvironmentGuard.validateDdlAuto(environment);
    }
}
