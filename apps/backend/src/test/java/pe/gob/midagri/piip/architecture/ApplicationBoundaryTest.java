package pe.gob.midagri.piip.architecture;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationBoundaryTest {
    @Test
    void controllersDoNotExposePersistenceTypesInTheirPublicMethods() throws Exception {
        try (var files = Files.walk(Path.of("src", "main", "java"))) {
            for (Path file : files.filter(path -> path.toString().contains("\\api\\") && path.toString().endsWith("Controller.java")).toList()) {
                String source = Files.readString(file);
                assertThat(source).as(file.toString()).doesNotContain("Entity>", "List<Entity", "ResponseEntity<.*Entity");
            }
        }
    }
}
