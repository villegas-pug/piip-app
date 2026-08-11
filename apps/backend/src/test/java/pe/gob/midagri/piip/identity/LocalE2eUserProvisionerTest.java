package pe.gob.midagri.piip.identity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Herramienta de provisión manual para la identidad E2E local autorizada.
 *
 * <p>No asigna roles ni ámbitos y nunca crea identidades en Keycloak. Se ejecuta
 * únicamente con el perfil {@code local-e2e} y la confirmación explícita indicada
 * en {@link #requireExplicitLocalExecution()}.</p>
 */
@DataJpaTest
@ActiveProfiles("local-e2e")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LocalE2eUserProvisionerTest {
    @Autowired private UserRepository users;
    @Autowired private EntityManager entityManager;

    @Test
    void provisionsAuthorizedLocalE2eUserIdempotently() {
        requireExplicitLocalExecution();
        ProvisionedUser expected = ProvisionedUser.fromSystemProperties();

        UserEntity user = users.findByKeycloakSubject(expected.subject())
            .map(existing -> verifyExisting(existing, expected))
            .orElseGet(() -> users.saveAndFlush(new UserEntity(expected.subject(), expected.fullName(), expected.email())));

        assertThat(user.getKeycloakSubject()).isEqualTo(expected.subject());
        assertThat(user.getFullName()).isEqualTo(expected.fullName());
        assertThat(user.getEmail()).isEqualTo(expected.email());

        entityManager.clear();
        UserEntity persisted = users.findByKeycloakSubject(expected.subject()).orElseThrow();
        assertThat(persisted.getFullName()).isEqualTo(expected.fullName());
        assertThat(persisted.getEmail()).isEqualTo(expected.email());
    }

    private UserEntity verifyExisting(UserEntity user, ProvisionedUser expected) {
        assertThat(user.getFullName()).isEqualTo(expected.fullName());
        assertThat(user.getEmail()).isEqualTo(expected.email());
        return user;
    }

    private void requireExplicitLocalExecution() {
        assertThat(System.getProperty("piip.provision.local-e2e")).isEqualTo("true");
        assertThat(System.getProperty("spring.profiles.active")).isEqualTo("local-e2e");
    }

    private record ProvisionedUser(String subject, String fullName, String email) {
        private static ProvisionedUser fromSystemProperties() {
            return new ProvisionedUser(
                requiredProperty("piip.provision.subject"),
                requiredProperty("piip.provision.full-name"),
                requiredProperty("piip.provision.email"));
        }

        private static String requiredProperty(String name) {
            String value = System.getProperty(name);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("La propiedad JVM " + name + " es obligatoria para la provisión local E2E");
            }
            return value;
        }
    }
}
