package pe.gob.midagri.piip.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class DashboardSummaryServiceTest {
    @Mock PortfolioRecordRepository records;
    @Mock WorkTaskRepository tasks;
    @Mock NotificationRepository notifications;
    @Mock LocalAuthorizationService authorization;
    @Mock PortfolioStatusRepository statuses;

    @Test
    void emptyScopeProducesZeroCountsAndPreservesStatusOrder() {
        when(authorization.requireAuthenticatedRole()).thenReturn(new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L))));
        when(records.findAll()).thenReturn(List.of());
        when(notifications.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        DashboardSummaryReadModel result = new DashboardSummaryService(records, tasks, notifications, authorization, statuses).summary();

        assertThat(result.initiatives()).isZero();
        assertThat(result.projects()).isZero();
        assertThat(result.portfolioStatusCounts()).isEmpty();
    }

    @Test
    void groupsCountsByCodeOrderedByCatalog() {
        when(authorization.requireAuthenticatedRole()).thenReturn(new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 100L, 10L))));
        when(records.findAll()).thenReturn(List.of(initiative("I-01", 10L, 100L), project("P-01", 10L, 100L)));
        when(statuses.findAllByOrderByDisplayOrderAscCodeAsc()).thenReturn(List.of(
            catalog(PortfolioStatus.PRESENTED, PortfolioStatusApplicability.INITIATIVE),
            catalog(PortfolioStatus.PROJECT_IN_PROGRESS, PortfolioStatusApplicability.PROJECT)));
        when(notifications.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        DashboardSummaryReadModel result = new DashboardSummaryService(records, tasks, notifications, authorization, statuses).summary();

        assertThat(result.initiatives()).isEqualTo(1L);
        assertThat(result.projects()).isEqualTo(1L);
        assertThat(result.portfolioStatusCounts()).extracting(count -> count.status().code())
            .containsExactly("PRESENTED", "PROJECT_IN_PROGRESS");
    }

    private PortfolioRecordEntity initiative(String code, Long unitId, Long institutionId) {
        return record(code, unitId, institutionId, false);
    }

    private PortfolioRecordEntity project(String code, Long unitId, Long institutionId) {
        return record(code, unitId, institutionId, true);
    }

    private PortfolioRecordEntity record(String code, Long unitId, Long institutionId, boolean project) {
        InstitutionEntity institution = new InstitutionEntity("INST", "Institución");
        ReflectionTestUtils.setField(institution, "id", institutionId);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE", "Unidad");
        ReflectionTestUtils.setField(unit, "id", unitId);
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.transientReferences();
        return project ? fixtures.preexistingProject(code, unit, "Proyecto") : fixtures.initiative(code, unit, "Iniciativa");
    }

    private PortfolioStatusCatalogEntity catalog(PortfolioStatus code, PortfolioStatusApplicability applicability) {
        return new PortfolioStatusCatalogEntity(code, code.label(), code.ordinal() + 1, true, applicability);
    }
}
