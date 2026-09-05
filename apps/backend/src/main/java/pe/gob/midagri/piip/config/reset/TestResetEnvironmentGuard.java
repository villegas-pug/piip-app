package pe.gob.midagri.piip.config.reset;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import pe.gob.midagri.piip.config.PiipProperties;

@Component
@Profile("test-reset")
public class TestResetEnvironmentGuard {
    private final Environment environment;
    private final DataSource dataSource;
    private final PiipProperties.TestReset properties;
    public TestResetEnvironmentGuard(Environment environment, DataSource dataSource, PiipProperties.TestReset properties) {
        this.environment = environment; this.dataSource = dataSource; this.properties = properties;
    }

    public GuardResult preflight() {
        validateProfiles(List.of(environment.getActiveProfiles()));
        validateDdlAuto(environment);
        require("5888eca4876f8583aea30c29da4bcacd944f8d2529b4eef71945943906521428".equalsIgnoreCase(properties.allowedJdbcFingerprint()), "La huella JDBC configurada no está autorizada");
        require("SISPIIP".equalsIgnoreCase(properties.allowedSchema()), "El esquema configurado no está autorizado");
        try (Connection connection = dataSource.getConnection()) {
            String fingerprint = sha256(connection.getMetaData().getURL());
            String schema = Optional.ofNullable(connection.getSchema()).orElse(connection.getMetaData().getUserName());
            require(MessageDigest.isEqual(fingerprint.getBytes(StandardCharsets.US_ASCII), properties.allowedJdbcFingerprint().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)), "La huella JDBC no está autorizada");
            require(schema != null && schema.equalsIgnoreCase(properties.allowedSchema()), "El esquema JDBC no está autorizado");
            return new GuardResult(fingerprint, schema.toUpperCase(Locale.ROOT));
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo validar la conexión test-reset", exception);
        }
    }

    public static void validateProfiles(List<String> profiles) {
        if (!profiles.equals(List.of("test", "test-reset"))) throw new IllegalStateException("test-reset exige exclusivamente los perfiles test,test-reset");
    }
    static void validateDdlAuto(Environment environment) {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        require("none".equalsIgnoreCase(ddlAuto), "test-reset exige spring.jpa.hibernate.ddl-auto=none");
        String hibernateDdlAuto = environment.getProperty("spring.jpa.properties.hibernate.hbm2ddl.auto");
        if (hibernateDdlAuto != null) require("none".equalsIgnoreCase(hibernateDdlAuto), "test-reset rechaza el override hibernate.hbm2ddl.auto");
    }
    static String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    public record GuardResult(String jdbcFingerprint, String schema) {}
}
