package pe.gob.midagri.piip.documents.application;

import org.springframework.boot.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.midagri.piip.config.PiipProperties;

/** Arranque dedicado de migración: con la propiedad activa ejecuta la operación y termina (research D4). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentMigrationRunner implements ApplicationRunner {
    private final DocumentMigrationService migration; private final PiipProperties.Migration properties;
    private final ConfigurableApplicationContext context;

    public DocumentMigrationRunner(DocumentMigrationService migration, PiipProperties.Migration properties, ConfigurableApplicationContext context) {
        this.migration = migration; this.properties = properties; this.context = context;
    }

    @Override public void run(ApplicationArguments args) {
        if (!properties.enabled()) return;
        migration.migrate();
        System.exit(SpringApplication.exit(context));
    }
}
