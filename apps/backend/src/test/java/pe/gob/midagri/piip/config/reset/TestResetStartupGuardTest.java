package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

class TestResetStartupGuardTest {
    @Test
    void rechazaPerfilesNoExactosAntesDelRefresh() {
        assertThatThrownBy(() -> initialize(new String[] {"test-reset", "test"}, "none"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> initialize(new String[] {"test", "test-reset", "prod"}, "none"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaAccionesDeEsquemaDeEscritura() {
        assertThatThrownBy(() -> initialize(new String[] {"test", "test-reset"}, "create-drop"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> initialize(new String[] {"test", "test-reset"}, "update"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> initializeWithoutDdlAuto())
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> initializeWithHibernateOverride("create"))
            .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> initialize(new String[] {"test", "test-reset"}, "none"))
            .doesNotThrowAnyException();
    }

    private static void initialize(String[] profiles, String ddlAuto) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        environment.setProperty("spring.jpa.hibernate.ddl-auto", ddlAuto);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        new TestResetStartupGuard().initialize(context);
    }

    private static void initializeWithoutDdlAuto() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test", "test-reset");
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        new TestResetStartupGuard().initialize(context);
    }

    private static void initializeWithHibernateOverride(String ddlAuto) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test", "test-reset");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        environment.setProperty("spring.jpa.properties.hibernate.hbm2ddl.auto", ddlAuto);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        new TestResetStartupGuard().initialize(context);
    }
}
