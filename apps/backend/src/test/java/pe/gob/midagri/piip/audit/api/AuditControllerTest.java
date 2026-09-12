package pe.gob.midagri.piip.audit.api;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.audit.application.AuditReadModels.EventView;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioStatusReferenceResponse;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class AuditControllerTest {
    @Test
    void exposesTheActorPresentationWithoutChangingTheStoredDetail() {
        EventView event = new EventView("DOCUMENTO_CARGADO", "I-001-2026",
            "{\"tipo\":\"INITIATIVE_TECHNICAL_OPINION\"}", "actor-subject", "Ana Analista",
            "ana@midagri.gob.pe", Instant.now(), null, null, null);

        AuditController.EventResponse response = AuditController.toEventResponse(event);

        assertThat(response.actor()).isEqualTo("actor-subject");
        assertThat(response.actorName()).isEqualTo("Ana Analista");
        assertThat(response.actorEmail()).isEqualTo("ana@midagri.gob.pe");
        assertThat(response.detail()).isEqualTo("{\"tipo\":\"INITIATIVE_TECHNICAL_OPINION\"}");
        assertThat(response.status()).isNull();
        assertThat(response.previousStatus()).isNull();
        assertThat(response.newStatus()).isNull();
    }

    @Test
    void preservesTheTechnicalSubjectWhenTheHistoricalEventHasNoUser() {
        EventView event = new EventView("TAREA_CREADA", "10", "{\"registro\":\"I-001-2026\"}",
            "legacy-subject", null, null, Instant.now(), null, null, null);

        AuditController.EventResponse response = AuditController.toEventResponse(event);

        assertThat(response.actor()).isEqualTo("legacy-subject");
        assertThat(response.actorName()).isNull();
        assertThat(response.actorEmail()).isNull();
        assertThat(response.occurredAt()).isInstanceOf(Instant.class);
    }

    @Test
    void exposesOptionalStatusReferencesAlongsideTheStoredDetail() {
        PortfolioStatusReferenceResponse previous = new PortfolioStatusReferenceResponse("PRESENTED", "Presentado", true);
        PortfolioStatusReferenceResponse current = new PortfolioStatusReferenceResponse("INITIATIVE_ARCHIVED", "Iniciativa archivada", true);
        EventView event = new EventView("ESTADO_INICIATIVA_CAMBIADO", "I-001-2026",
            "{\"previousStatusCode\":\"PRESENTED\",\"newStatusCode\":\"INITIATIVE_ARCHIVED\"}", "actor-subject",
            "Ana Analista", "ana@midagri.gob.pe", Instant.now(), null, previous, current);

        AuditController.EventResponse response = AuditController.toEventResponse(event);

        assertThat(response.previousStatus().code()).isEqualTo("PRESENTED");
        assertThat(response.newStatus().code()).isEqualTo("INITIATIVE_ARCHIVED");
    }

    @Test
    void exposesStructuredOrganizationScopeAlongsideLegacyAuditFields() {
        EventView event = new EventView("UE_CREADA", "UE-001", "{\"result\":\"SUCCESS\"}", "actor-subject",
            "Ana Analista", "ana@midagri.gob.pe", Instant.now(), null, null, null, "UNIDAD_EJECUTORA", 10L,
            1L, 10L, null);

        AuditController.EventResponse response = AuditController.toEventResponse(event);

        assertThat(response.entityType()).isEqualTo("UNIDAD_EJECUTORA");
        assertThat(response.entityId()).isEqualTo(10L);
        assertThat(response.institutionId()).isEqualTo(1L);
        assertThat(response.executingUnitId()).isEqualTo(10L);
    }
}
