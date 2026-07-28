package pe.gob.midagri.piip.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;

class PersistencePolicyTest {
    @Test
    void applicationDoesNotUseNativeSqlApis() throws Exception {
        Path root = Path.of("src", "main", "java");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertThat(source).as(file.toString()).doesNotContain("nativeQuery = true", "JdbcTemplate", "createNativeQuery(");
            }
        }
    }
}
