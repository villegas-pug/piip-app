package pe.gob.midagri.piip.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    private static final List<String> EXPECTED_FK_NAMES = List.of(
        "FK_AUDACCESO_USUARIO",
        "FK_CATITEM_CATALOGO",
        "FK_DOC_REGISTRO",
        "FK_DOC_TIPODOC",
        "FK_DOCCONT_VERSION",
        "FK_DOCVER_DOC",
        "FK_EVENTO_USUARIO",
        "FK_NOTIF_USUARIO",
        "FK_NOTIF_REGISTRO",
        "FK_REG_UE",
        "FK_REG_ORIGEN",
        "FK_REG_PEI",
        "FK_REG_POI",
        "FK_REG_SOLUCION",
        "FK_REG_FUENTE",
        "FK_RUR_UO",
        "FK_RUR_REGISTRO",
        "FK_TAREA_USUARIO",
        "FK_TAREA_REGISTRO",
        "FK_UE_INSTITUCION",
        "FK_UO_EJECUTORA",
        "FK_UO_PADRE",
        "FK_URA_UE",
        "FK_URA_INSTITUCION",
        "FK_URA_ROL",
        "FK_URA_USUARIO"
    );

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

    @Test
    void emitsDescriptiveForeignKeyNames() {
        String ddl = read(DDL);
        for (String fkName : EXPECTED_FK_NAMES) {
            assertThat(ddl.lines().filter(line -> line.contains("constraint " + fkName + " ")).count())
                .as("FK %s esperada en DDL generado", fkName)
                .isEqualTo(1L);
        }
        assertThat(ddl.lines().filter(line -> line.contains(" foreign key (") && line.contains(" constraint FK")).count())
            .as("numero total de constraints FK descriptivas")
            .isEqualTo((long) EXPECTED_FK_NAMES.size());
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("No se pudo leer el DDL generado", exception);
        }
    }
}
