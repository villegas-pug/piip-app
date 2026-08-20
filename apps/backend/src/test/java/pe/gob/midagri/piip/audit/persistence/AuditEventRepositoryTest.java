package pe.gob.midagri.piip.audit.persistence;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.identity.persistence.UserEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuditEventRepositoryTest {
    @Autowired AuditEventRepository events;
    @Autowired EntityManager entityManager;

    @Test
    void preloadsActorForGlobalAuditQuery() {
        persistEvent("P-001-2026");
        entityManager.clear();

        AuditEventEntity loaded = events.findTop100ByOrderByOccurredAtDesc().getFirst();

        assertActorRemainsReadableAfterEntityManagerIsCleared(loaded);
    }

    @Test
    void preloadsActorForScopedAuditQuery() {
        persistEvent("P-002-2026");
        entityManager.clear();

        List<AuditEventEntity> loaded = events.findTop100ByEntityCodeInOrderByOccurredAtDesc(List.of("P-002-2026"));

        assertThat(loaded).hasSize(1);
        assertActorRemainsReadableAfterEntityManagerIsCleared(loaded.getFirst());
    }

    private void assertActorRemainsReadableAfterEntityManagerIsCleared(AuditEventEntity event) {
        assertThat(Hibernate.isInitialized(event.getUser())).isTrue();
        entityManager.clear();

        assertThat(event.getUser().getFullName()).isEqualTo("Ana Analista");
        assertThat(event.getUser().getEmail()).isEqualTo("ana@midagri.gob.pe");
    }

    private void persistEvent(String entityCode) {
        UserEntity user = new UserEntity("actor-subject", "Ana Analista", "ana@midagri.gob.pe");
        AuditEventEntity event = new AuditEventEntity("DOCUMENTO_CARGADO", "REGISTRO_PORTAFOLIO", entityCode,
            "{\"tipo\":\"INITIATIVE_TECHNICAL_OPINION\"}", user, "actor-subject");
        entityManager.persist(user);
        entityManager.persist(event);
        entityManager.flush();
    }
}
