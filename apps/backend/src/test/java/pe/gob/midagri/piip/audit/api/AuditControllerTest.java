package pe.gob.midagri.piip.audit.api;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.audit.application.AuditReadModels.EventView;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class AuditControllerTest {
    @Test
    void exposesTheActorPresentationWithoutChangingTheStoredDetail() {
        EventView event = new EventView("DOCUMENTO_CARGADO", "I-001-2026",
            "{\"tipo\":\"INITIATIVE_TECHNICAL_OPINION\"}", "actor-subject", "Ana Analista",
            "ana@midagri.gob.pe", Instant.now());

        AuditController.EventResponse response = AuditController.toEventResponse(event);

        assertThat(response.actor()).isEqualTo("actor-subject");
        assertThat(response.actorName()).isEqualTo("Ana Analista");
        assertThat(response.actorEmail()).isEqualTo("ana@midagri.gob.pe");
        assertThat(response.detail()).isEqualTo("{\"tipo\":\"INITIATIVE_TECHNICAL_OPINION\"}");
    }

    @Test
    void preservesTheTechnicalSubjectWhenTheHistoricalEventHasNoUser() {
        EventView event = new EventView("TAREA_CREADA", "10", "{\"registro\":\"I-001-2026\"}",
            "legacy-subject", null, null, Instant.now());

        AuditController.EventResponse response = AuditController.toEventResponse(event);

        assertThat(response.actor()).isEqualTo("legacy-subject");
        assertThat(response.actorName()).isNull();
        assertThat(response.actorEmail()).isNull();
        assertThat(response.occurredAt()).isInstanceOf(Instant.class);
    }
}
