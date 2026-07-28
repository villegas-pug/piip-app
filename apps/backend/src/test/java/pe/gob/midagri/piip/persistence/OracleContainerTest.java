package pe.gob.midagri.piip.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.OracleContainer;

@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class OracleContainerTest {
    @Container
    static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-free:23-slim-faststart");

    @Test
    void connectsToSupportedOracleRuntime() throws Exception {
        try (var connection = DriverManager.getConnection(ORACLE.getJdbcUrl(), ORACLE.getUsername(), ORACLE.getPassword())) {
            assertThat(connection.isValid(5)).isTrue();
            assertThat(connection.getMetaData().getDatabaseProductName()).containsIgnoringCase("Oracle");
        }
    }
}
