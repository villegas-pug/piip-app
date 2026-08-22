package pe.gob.midagri.piip.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class DashboardSummaryServiceTest {
    @Mock PortfolioRecordRepository records;
    @Mock WorkTaskRepository tasks;
    @Mock NotificationRepository notifications;
    @Mock LocalAuthorizationService authorization;

    @Test
    void emptyScopeProducesZeroCountsAndPreservesStatusOrder() {
        when(authorization.requireAuthenticatedRole()).thenReturn(new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L))));
        when(records.findAll()).thenReturn(List.of());
        when(notifications.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        DashboardSummaryReadModel result = new DashboardSummaryService(records, tasks, notifications, authorization).summary();

        assertThat(result.initiatives()).isZero();
        assertThat(result.projects()).isZero();
        assertThat(result.portfolioByStatus()).isEmpty();
    }
}
