package pe.gob.midagri.piip.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.database-platform=org.hibernate.dialect.OracleDialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect",
    "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=none",
    "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
    "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/piip-oracle.sql"
})
@ActiveProfiles("test")
class OracleSchemaGenerationTest {
    private static final Path DDL = Path.of("target", "piip-oracle.sql");

    static {
        try {
            Files.deleteIfExists(DDL);
        } catch (java.io.IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Test
    void generatesReviewableOracleDdlFromJpaMetadata() {
        assertThat(DDL).exists();
        assertThat(read(DDL)).containsIgnoringCase("create table REGISTRO_PORTAFOLIO")
            .containsIgnoringCase("create table USUARIO_ROL_AMBITO")
            .containsIgnoringCase("create table CATALOGO")
            .containsIgnoringCase("create table CATALOGO_ITEM")
            .containsIgnoringCase("create table TIPO_DOCUMENTO")
            .containsIgnoringCase("ID_TIPO_SOLUCION")
            .containsIgnoringCase("ID_FUENTE_ORIGEN")
            .containsIgnoringCase("ID_TIPO_DOCUMENTO")
            .doesNotContainIgnoringCase(" TIPO_SOLUCION varchar")
            .doesNotContainIgnoringCase(" FUENTE_ORIGEN varchar")
            .doesNotContain("INSERT INTO");
        assertThat(read(DDL).lines().filter(line -> line.startsWith("create table ")).count()).isEqualTo(19);
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("No se pudo leer el DDL generado", exception);
        }
    }
}
