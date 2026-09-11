package pe.gob.midagri.piip.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.audit.persistence.AccessAuditRepository;
import pe.gob.midagri.piip.audit.persistence.AuditEventEntity;
import pe.gob.midagri.piip.audit.persistence.AuditEventRepository;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {
    @Mock AccessAuditRepository accesses;
    @Mock AuditEventRepository events;
    @Mock PortfolioRecordRepository records;
    @Mock LocalAuthorizationService authorization;
    @Mock PortfolioStatusRepository statuses;

    @Test
    void returnsEmptyGlobalViewsWithoutInventingActorData() {
        when(accesses.findTop100ByOrderByOccurredAtDesc()).thenReturn(List.of());
        when(events.findTop100ByOrderByOccurredAtDesc()).thenReturn(List.of());
        AuditQueryService service = new AuditQueryService(accesses, events, records, authorization, statuses, new ObjectMapper());

        assertThat(service.accesses(null)).isEmpty();
        assertThat(service.events(null)).isEmpty();
    }

    @Test
    void enrichesStatusCodesWithCurrentMetadata() {
        AuditEventEntity event = new AuditEventEntity("ESTADO_INICIATIVA_CAMBIADO", "REGISTRO_PORTAFOLIO", "I-001",
            "{\"previousStatusCode\":\"PRESENTED\",\"newStatusCode\":\"INITIATIVE_ARCHIVED\"}", null, "subject");
        when(events.findTop100ByOrderByOccurredAtDesc()).thenReturn(List.of(event));
        when(statuses.findByCode(PortfolioStatus.PRESENTED)).thenReturn(Optional.of(
            new PortfolioStatusCatalogEntity(PortfolioStatus.PRESENTED, "Presentado", 1, true, PortfolioStatusApplicability.INITIATIVE)));
        when(statuses.findByCode(PortfolioStatus.INITIATIVE_ARCHIVED)).thenReturn(Optional.of(
            new PortfolioStatusCatalogEntity(PortfolioStatus.INITIATIVE_ARCHIVED, "Iniciativa archivada", 3, true, PortfolioStatusApplicability.INITIATIVE)));
        AuditQueryService service = new AuditQueryService(accesses, events, records, authorization, statuses, new ObjectMapper());

        assertThat(service.events(null)).singleElement().satisfies(view -> {
            assertThat(view.status()).isNull();
            assertThat(view.previousStatus().code()).isEqualTo("PRESENTED");
            assertThat(view.newStatus().code()).isEqualTo("INITIATIVE_ARCHIVED");
        });
    }

    @Test
    void leavesLegacyEventsAsHistoricalTextWithoutMappingLabels() {
        AuditEventEntity event = new AuditEventEntity("DOCUMENTO_CARGADO", "REGISTRO_PORTAFOLIO", "I-001",
            "{\"tipo\":\"INITIATIVE_TECHNICAL_OPINION\"}", null, "subject");
        when(events.findTop100ByOrderByOccurredAtDesc()).thenReturn(List.of(event));
        AuditQueryService service = new AuditQueryService(accesses, events, records, authorization, statuses, new ObjectMapper());

        assertThat(service.events(null)).singleElement().satisfies(view -> {
            assertThat(view.status()).isNull();
            assertThat(view.previousStatus()).isNull();
            assertThat(view.newStatus()).isNull();
            assertThat(view.detail()).contains("INITIATIVE_TECHNICAL_OPINION");
        });
    }
}
