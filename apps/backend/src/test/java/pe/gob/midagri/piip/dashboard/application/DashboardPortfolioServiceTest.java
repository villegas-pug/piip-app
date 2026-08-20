package pe.gob.midagri.piip.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.dashboard.persistence.DashboardPortfolioQueryRepository;
import pe.gob.midagri.piip.dashboard.persistence.DashboardPortfolioQueryRepository.QueryResult;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;

@ExtendWith(MockitoExtension.class)
class DashboardPortfolioServiceTest {
    @Mock DashboardPortfolioQueryRepository queries;
    @Mock LocalAuthorizationService authorization;

    @Test
    void mapsOnlyPositiveCanonicalStatusCountsAndKeepsUnitScope() {
        PortfolioRecordEntity record = record(10L, "INI-001", PortfolioStatus.PRESENTED);
        QueryResult result = new QueryResult(List.of(record), 0, 5, 1, 1, 1,
            Map.of(PortfolioStatus.PRESENTED, 1L, PortfolioStatus.INITIATIVE_APPROVED, 0L));
        when(queries.find(10L, "codigo", RecordType.INITIATIVE, PortfolioStatus.PRESENTED, 0, 5)).thenReturn(result);

        DashboardPortfolioService service = new DashboardPortfolioService(queries, authorization);
        var response = service.portfolio(10L, " codigo ", RecordType.INITIATIVE, PortfolioStatus.PRESENTED, 0, 5);
        verify(authorization).requireReadableUnit(10L);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.executingUnitTotalElements()).isEqualTo(1L);
        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.recordType()).isEqualTo("Iniciativa");
            assertThat(item.status()).isEqualTo("Presentado");
            assertThat(item.executingUnitId()).isEqualTo(10L);
        });
        assertThat(response.statusCounts()).extracting(item -> item.status()).containsExactly("Presentado");
    }

    @Test
    void authorizesTheExactUnitBeforeQueryingAndRepresentsARealEmptyPortfolio() {
        when(queries.find(20L, null, null, null, 0, 1))
            .thenReturn(new QueryResult(List.of(), 0, 1, 0, 0, 0, Map.of()));

        DashboardPortfolioService service = new DashboardPortfolioService(queries, authorization);
        var response = service.portfolio(20L, "  ", null, null, -1, 0);

        verify(authorization).requireReadableUnit(20L);
        assertThat(response.content()).isEmpty();
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isZero();
        assertThat(response.executingUnitTotalElements()).isZero();
        assertThat(response.statusCounts()).isEmpty();
    }

    @Test
    void doesNotQueryWhenTheUnitIsOutsideTheAuthorizedScope() {
        when(authorization.requireReadableUnit(99L))
            .thenThrow(new AccessDeniedException("fuera del ámbito autorizado"));

        DashboardPortfolioService service = new DashboardPortfolioService(queries, authorization);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            service.portfolio(99L, null, null, null, 0, 5))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(queries);
    }

    private PortfolioRecordEntity record(Long unitId, String code, PortfolioStatus status) {
        InstitutionEntity institution = new InstitutionEntity("INST-" + unitId, "Institución");
        ReflectionTestUtils.setField(institution, "id", unitId + 1000);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-" + unitId, "Unidad Ejecutora");
        ReflectionTestUtils.setField(unit, "id", unitId);
        PortfolioRecordEntity record = PortfolioRecordTestBuilder.transientReferences().initiative(code, unit, "Nombre");
        ReflectionTestUtils.setField(record, "status", status);
        ReflectionTestUtils.setField(record, "updatedAt", Instant.parse("2026-08-18T15:00:00Z"));
        return record;
    }
}
