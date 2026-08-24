package pe.gob.midagri.piip.audit.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccessAuditRepositoryTest {
    @Autowired AccessAuditRepository accesses;
    @Autowired UserRepository users;

    @Test
    void persistsAndReadsNullableSafeReason() {
        UserEntity user = users.save(new UserEntity("audit-subject", "Auditoría", "audit@example.test"));
        accesses.save(new AccessAuditEntity(user, user.getKeycloakSubject(), "[]", "GET", "/admin/users",
            422, null, "127.0.0.1", "correlation-1", 4L, "LAST_ACTIVE_ADMIN"));
        accesses.save(new AccessAuditEntity(user, user.getKeycloakSubject(), "[]", "GET", "/admin/users",
            200, null, "127.0.0.1", "correlation-2", 2L));

        var rows = accesses.findTop100ByOrderByOccurredAtDesc();
        assertThat(rows).extracting(AccessAuditEntity::getSafeReason)
            .contains("LAST_ACTIVE_ADMIN").containsNull();
    }
}
