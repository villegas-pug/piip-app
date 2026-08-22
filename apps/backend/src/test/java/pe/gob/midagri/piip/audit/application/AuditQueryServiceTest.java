package pe.gob.midagri.piip.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.audit.persistence.AccessAuditRepository;
import pe.gob.midagri.piip.audit.persistence.AuditEventRepository;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {
    @Mock AccessAuditRepository accesses;
    @Mock AuditEventRepository events;
    @Mock PortfolioRecordRepository records;
    @Mock LocalAuthorizationService authorization;

    @Test
    void returnsEmptyGlobalViewsWithoutInventingActorData() {
        when(accesses.findTop100ByOrderByOccurredAtDesc()).thenReturn(List.of());
        when(events.findTop100ByOrderByOccurredAtDesc()).thenReturn(List.of());
        AuditQueryService service = new AuditQueryService(accesses, events, records, authorization);

        assertThat(service.accesses(null)).isEmpty();
        assertThat(service.events(null)).isEmpty();
    }
}
