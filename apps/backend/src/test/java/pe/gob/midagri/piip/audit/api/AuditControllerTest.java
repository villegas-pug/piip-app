package pe.gob.midagri.piip.audit.api;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.audit.persistence.AuditEventEntity;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class AuditControllerTest {
    @Test
    void exposesTheActorPresentationWithoutChangingTheStoredDetail() {
        UserEntity user = new UserEntity("actor-subject", "Ana Analista", "ana@midagri.gob.pe");
        AuditEventEntity event = new AuditEventEntity("DOCUMENTO_CARGADO", "REGISTRO_PORTAFOLIO", "I-001-2026",
            "{\"tipo\":\"INITIATIVE_TECHNICAL_OPINION\"}", user, "actor-subject");

        AuditController.EventResponse response = AuditController.toEventResponse(event);

        assertThat(response.actor()).isEqualTo("actor-subject");
        assertThat(response.actorName()).isEqualTo("Ana Analista");
        assertThat(response.actorEmail()).isEqualTo("ana@midagri.gob.pe");
        assertThat(response.detail()).isEqualTo("{\"tipo\":\"INITIATIVE_TECHNICAL_OPINION\"}");
    }

    @Test
    void preservesTheTechnicalSubjectWhenTheHistoricalEventHasNoUser() {
        AuditEventEntity event = new AuditEventEntity("TAREA_CREADA", "TAREA_TRABAJO", "10", "{\"registro\":\"I-001-2026\"}", null, "legacy-subject");

        AuditController.EventResponse response = AuditController.toEventResponse(event);

        assertThat(response.actor()).isEqualTo("legacy-subject");
        assertThat(response.actorName()).isNull();
        assertThat(response.actorEmail()).isNull();
        assertThat(response.occurredAt()).isInstanceOf(Instant.class);
    }
}
