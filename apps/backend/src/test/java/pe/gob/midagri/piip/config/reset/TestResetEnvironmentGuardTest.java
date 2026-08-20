package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import javax.sql.DataSource;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import pe.gob.midagri.piip.config.PiipProperties;
import static org.mockito.Mockito.*;

class TestResetEnvironmentGuardTest {
    @Test
    void exigeExactamenteLosPerfilesDePruebaYLaConfirmacion() {
        assertThatThrownBy(() -> TestResetEnvironmentGuard.validateProfiles(Set.of("test-reset")))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> TestResetEnvironmentGuard.validateProfiles(Set.of("test", "test-reset", "prod")))
            .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> TestResetEnvironmentGuard.validateProfiles(Set.of("test", "test-reset")))
            .doesNotThrowAnyException();
    }

    @Test void validaConfirmacionHuellaYEsquemaContraMetadataJdbc() throws Exception {
        Environment environment = mock(Environment.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"test", "test-reset"});
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn("jdbc:oracle:thin:@test_low");
        when(connection.getSchema()).thenReturn("PIIP_TEST");
        String fingerprint = TestResetEnvironmentGuard.sha256("jdbc:oracle:thin:@test_low");
        PiipProperties.TestReset properties = new PiipProperties.TestReset(true, "RESET-PIIP-TEST", "RESET-PIIP-TEST", fingerprint, "PIIP_TEST");

        assertThatCode(() -> new TestResetEnvironmentGuard(environment, dataSource, properties).preflight()).doesNotThrowAnyException();
        PiipProperties.TestReset wrong = new PiipProperties.TestReset(true, "RESET-PIIP-TEST", "RESET-PIIP-TEST", fingerprint, "PROD");
        assertThatThrownBy(() -> new TestResetEnvironmentGuard(environment, dataSource, wrong).preflight())
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("esquema");
    }
}
