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
    void seedEsDmlIdempotenteSinDdlNiIdsDeIdentidadLiterales() throws IOException {
        String sql = new ClassPathResource("db/test/catalog-data.sql").getContentAsString(StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
        assertThat(sql).contains("MERGE INTO CATALOGO", "MERGE INTO CATALOGO_ITEM", "MERGE INTO TIPO_DOCUMENTO");
        assertThat(sql).doesNotContain("CREATE TABLE", "DROP TABLE", "TRUNCATE", "ALTER TABLE", "BEGIN", "DECLARE", "INSERT INTO CATALOGO (ID_");
        assertThat(sql).doesNotMatch("(?s).*\\bID_(?:CATALOGO|CATALOGO_ITEM|TIPO_DOCUMENTO|UNIDAD_ORGANICA)\\s*=\\s*\\d+.*");
    }

    @Test
    void seedNoRepiteCodigosDentroDeCadaCatalogoNiTiposDocumentales() throws IOException {
        String sql = new ClassPathResource("db/test/catalog-data.sql").getContentAsString(StandardCharsets.UTF_8);
        assertUnique(sql, "(?m)SELECT\\s+'([^']+)'\\s+codigo,\\s*'[^']+'\\s+nombre,\\s*\\d+\\s+orden\\s+FROM\\s+dual", 4);
        assertUnique(sql, "(?m)SELECT\\s+'([^']+)'\\s+codigo,'[^']+'\\s+nombre,\\d+\\s+orden\\s+FROM\\s+dual|SELECT\\s+'([^']+)','[^']+',\\d+\\s+FROM\\s+dual", 17);
    }

    private static void assertUnique(String sql, String expression, int minimumExpected) {
        Matcher matcher = Pattern.compile(expression, Pattern.CASE_INSENSITIVE).matcher(sql);
        Set<String> codes = new HashSet<>();
        int matches = 0;
        while (matcher.find()) {
            String code = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            assertThat(codes.add(code)).as("código duplicado: %s", code).isTrue();
            matches++;
        }
        assertThat(matches).isGreaterThanOrEqualTo(minimumExpected);
    }
}
