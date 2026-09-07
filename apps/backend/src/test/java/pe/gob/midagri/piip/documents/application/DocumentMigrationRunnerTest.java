package pe.gob.midagri.piip.documents.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import pe.gob.midagri.piip.config.PiipProperties;

class DocumentMigrationRunnerTest {
    @Test void arranqueNormalNoEjecutaLaMigracion() {
        DocumentMigrationService migration = mock(DocumentMigrationService.class);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        DocumentMigrationRunner runner = new DocumentMigrationRunner(migration, new PiipProperties.Migration(false), context);

        runner.run(mock(ApplicationArguments.class));

        verifyNoInteractions(migration, context);
    }
}
