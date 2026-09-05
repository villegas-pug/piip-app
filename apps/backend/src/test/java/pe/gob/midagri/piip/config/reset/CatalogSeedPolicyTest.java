package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CatalogSeedPolicyTest {
    @Test
    void seedEsDmlOnlySinDdlNiIdsDeIdentidadLiterales() throws IOException {
        String sql = new ClassPathResource("db/test/catalog-data.sql").getContentAsString(StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
        assertThat(sql).contains("MERGE INTO CATALOGO", "MERGE INTO CATALOGO_ITEM", "MERGE INTO TIPO_DOCUMENTO");
        assertThat(sql).contains("MERGE INTO ROL", "MERGE INTO INSTITUCION", "MERGE INTO UNIDAD_EJECUTORA",
            "MERGE INTO UNIDAD_ORGANICA", "MERGE INTO USUARIO", "MERGE INTO USUARIO_ROL_AMBITO");
        assertThat(sql).contains("ED3742BC-F2C2-4884-AE09-07E3F9AB98FC", "CRISTOPHER GUEVARA VILLEGAS", "RGUEVARAV@MIDAGRI.GOB.PE")
            .doesNotContain("__PIIP_BOOTSTRAP_SUBJECT__", "__PIIP_BOOTSTRAP_NAME__", "__PIIP_BOOTSTRAP_EMAIL__");
        assertThat(sql).containsPattern("ASIGNADO_POR\\s*=\\s*'BOOTSTRAP'").doesNotContain("TEST-SEED");
        assertThat(sql).doesNotContain("CREATE TABLE", "DROP TABLE", "TRUNCATE", "ALTER TABLE", "BEGIN", "DECLARE", "INSERT INTO CATALOGO (ID_");
        assertThat(sql).doesNotMatch("(?s).*\\bID_(?:CATALOGO|CATALOGO_ITEM|TIPO_DOCUMENTO|UNIDAD_ORGANICA|USUARIO|ROL)\\s*=\\s*\\d+.*");
        assertThat(sql).doesNotContain("PASSWORD", "TOKEN", "WALLET", "KEYCLOAK_REALM", "CREATE USER");
    }

    @Test
    void seedNoRepiteCodigosDentroDeCadaCatalogoNiTiposDocumentales() throws IOException {
        String sql = new ClassPathResource("db/test/catalog-data.sql").getContentAsString(StandardCharsets.UTF_8);
        String catalogSection = section(sql, "-- Catalogos", "-- Items de catalogo");
        String itemSection = section(sql, "-- Items de catalogo", "-- Tipos documentales");
        assertUnique(catalogSection, "MERGE\\s+INTO\\s+CATALOGO\\s+\\w+\\s+USING\\s*\\(\\s*SELECT\\s+'([^']+)'\\s+codigo\\b", 4);
        assertUnique(itemSection, "CROSS\\s+JOIN\\s*\\(\\s*SELECT\\s+'([^']+)'\\s+codigo\\b|UNION\\s+ALL\\s+SELECT\\s+'([^']+)'", 17);
    }

    private static String section(String sql, String startMarker, String endMarker) {
        Matcher matcher = Pattern.compile("(?is)" + Pattern.quote(startMarker) + "(.*?)" + Pattern.quote(endMarker)).matcher(sql);
        assertThat(matcher.find()).as("sección no encontrada: %s -> %s", startMarker, endMarker).isTrue();
        return matcher.group(1);
    }

    private static void assertUnique(String sql, String expression, int minimumExpected) {
        Matcher matcher = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(sql);
        Set<String> codes = new HashSet<>();
        int matches = 0;
        while (matcher.find()) {
            String code = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            assertThat(codes.add(code)).as("código duplicado: %s", code).isTrue();
            matches++;
        }
        assertThat(matches).isEqualTo(minimumExpected);
    }
}
