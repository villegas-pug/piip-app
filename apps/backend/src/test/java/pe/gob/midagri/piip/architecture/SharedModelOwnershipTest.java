package pe.gob.midagri.piip.architecture;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SharedModelOwnershipTest {
    @Test
    void reusableOrganizationModelDoesNotRemainNestedInOrganizationController() throws Exception {
        Path root = Path.of("src", "main", "java");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertThat(Files.readString(file)).as(file.toString()).doesNotContain("OrganizationController.OrganizationalUnitResponse");
            }
        }
    }
}
