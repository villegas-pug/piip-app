package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import javax.sql.DataSource;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import pe.gob.midagri.piip.config.PiipProperties;
import static org.mockito.Mockito.*;

class TestResetEnvironmentGuardTest {
    @Test
    void exigeExactamenteLosPerfilesDePruebaEnOrden() {
        assertThatThrownBy(() -> TestResetEnvironmentGuard.validateProfiles(List.of("test-reset")))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> TestResetEnvironmentGuard.validateProfiles(List.of("test-reset", "test")))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> TestResetEnvironmentGuard.validateProfiles(List.of("test", "test-reset", "prod")))
            .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> TestResetEnvironmentGuard.validateProfiles(List.of("test", "test-reset")))
            .doesNotThrowAnyException();
    }

    @Test void validaHuellaYEsquemaContraMetadataJdbc() throws Exception {
        Environment environment = mock(Environment.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"test", "test-reset"});
        when(environment.getProperty("spring.jpa.hibernate.ddl-auto")).thenReturn("none");
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn("jdbc:oracle:thin:@srvdb-oracle-desa.domainminag.gob:1521:DEVELOPER");
        when(connection.getSchema()).thenReturn("SISPIIP");
        PiipProperties.TestReset properties = new PiipProperties.TestReset("5888eca4876f8583aea30c29da4bcacd944f8d2529b4eef71945943906521428", "SISPIIP");

        assertThatCode(() -> new TestResetEnvironmentGuard(environment, dataSource, properties).preflight()).doesNotThrowAnyException();
        PiipProperties.TestReset wrong = new PiipProperties.TestReset(properties.allowedJdbcFingerprint(), "PROD");
        assertThatThrownBy(() -> new TestResetEnvironmentGuard(environment, dataSource, wrong).preflight())
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("esquema");
    }
}
