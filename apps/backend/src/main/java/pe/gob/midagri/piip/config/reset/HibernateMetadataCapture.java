package pe.gob.midagri.piip.config.reset;

import java.util.List;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test-reset")
public class HibernateMetadataCapture implements Integrator, HibernatePropertiesCustomizer {
    private volatile Metadata metadata;

    @Override public void customize(java.util.Map<String, Object> properties) {
        properties.put("hibernate.integrator_provider", (IntegratorProvider) () -> List.of(this));
    }
    @Override public void integrate(Metadata metadata, BootstrapContext context, SessionFactoryImplementor sessionFactory) {
        this.metadata = metadata;
    }
    public Metadata requireMetadata() {
        if (metadata == null) throw new IllegalStateException("Hibernate Metadata no está disponible para test-reset");
        return metadata;
    }
}
