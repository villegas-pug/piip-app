package pe.gob.midagri.piip.dashboard.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.domain.SolutionType;
import pe.gob.midagri.piip.portfolio.domain.SourceOrigin;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;

@DataJpaTest
@ActiveProfiles("test")
@Import(DashboardPortfolioQueryRepository.class)
class DashboardPortfolioQueryRepositoryTest {
    @Autowired DashboardPortfolioQueryRepository queries;
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;
    @Autowired PortfolioRecordRepository records;
    @Autowired EntityManager entityManager;

    @Test
    void filtersOneUnitAndReconcilesPositiveStatusCounts() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("INST-DASH", "Institución"));
        ExecutingUnitEntity first = executingUnits.save(new ExecutingUnitEntity(institution, "UE-DASH-1", "UE uno"));
        ExecutingUnitEntity second = executingUnits.save(new ExecutingUnitEntity(institution, "UE-DASH-2", "UE dos"));
        initiative("INI-001", first, "Primera");
        initiative("INI-002", first, "Segunda");
        initiative("INI-003", second, "Otra");
        entityManager.flush();
        entityManager.clear();

        DashboardPortfolioQueryRepository.QueryResult result = queries.find(first.getId(), null, null, null, 0, 5);

        assertThat(result.executingUnitTotalElements()).isEqualTo(2L);
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.content()).extracting(PortfolioRecordEntity::getCode)
            .containsExactly("INI-002", "INI-001");
        assertThat(result.statusCounts()).containsEntry(PortfolioStatus.PRESENTED, 2L);
    }

    @Test
    void appliesSharedSearchTypeStatusAndGlobalStableOrderAcrossPages() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("INST-DASH-FILTER", "Institución"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-DASH-FILTER", "UE filtros"));
        PortfolioRecordEntity first = initiative("INI-FILTER-1", unit, "Nombre alfa");
        PortfolioRecordEntity approved = initiative("INI-FILTER-2", unit, "Nombre beta");
        approved.approve();
        PortfolioRecordEntity project = records.save(PortfolioRecordEntity.derivedProject("PRY-FILTER-1", approved,
            "Proyecto gamma", SolutionType.TO_BE_DEFINED, SourceOrigin.OTHER, LocalDate.of(2026, 1, 3),
            "Responsable", null, null, "Descripción", "Resultados", null, DigitalComponent.NO, "subject"));
        ReflectionTestUtils.setField(first, "updatedAt", Instant.parse("2026-08-18T10:00:00Z"));
        ReflectionTestUtils.setField(approved, "updatedAt", Instant.parse("2026-08-18T11:00:00Z"));
        ReflectionTestUtils.setField(project, "updatedAt", Instant.parse("2026-08-18T12:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        var all = queries.find(unit.getId(), null, null, null, 0, 2);
        var searchedByCode = queries.find(unit.getId(), "filter-1", null, null, 0, 5);
        var searchedByName = queries.find(unit.getId(), "gamma", null, null, 0, 5);
        var initiatives = queries.find(unit.getId(), null, RecordType.INITIATIVE, null, 0, 5);
        var approvedOnly = queries.find(unit.getId(), null, null, PortfolioStatus.INITIATIVE_APPROVED, 0, 5);
        var secondPage = queries.find(unit.getId(), null, null, null, 1, 2);
        var outsidePage = queries.find(unit.getId(), null, null, null, 99, 1);

        assertThat(all.totalElements()).isEqualTo(3L);
        assertThat(all.content()).extracting(PortfolioRecordEntity::getCode)
            .containsExactly("PRY-FILTER-1", "INI-FILTER-2");
        assertThat(searchedByCode.content()).extracting(PortfolioRecordEntity::getCode)
            .containsExactly("PRY-FILTER-1", "INI-FILTER-1");
        assertThat(searchedByName.content()).extracting(PortfolioRecordEntity::getCode)
            .containsExactly("PRY-FILTER-1");
        assertThat(initiatives.totalElements()).isEqualTo(2L);
        assertThat(initiatives.statusCounts()).containsEntry(PortfolioStatus.PRESENTED, 1L)
            .containsEntry(PortfolioStatus.INITIATIVE_APPROVED, 1L);
        assertThat(approvedOnly.totalElements()).isEqualTo(1L);
        assertThat(approvedOnly.statusCounts()).containsEntry(PortfolioStatus.INITIATIVE_APPROVED, 1L);
        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.content()).extracting(PortfolioRecordEntity::getCode)
            .containsExactly("INI-FILTER-1");
        assertThat(outsidePage.page()).isZero();
        assertThat(outsidePage.size()).isEqualTo(1);
        assertThat(outsidePage.content()).extracting(PortfolioRecordEntity::getCode)
            .containsExactly("PRY-FILTER-1");
    }

    private PortfolioRecordEntity initiative(String code, ExecutingUnitEntity unit, String name) {
        return records.save(PortfolioRecordEntity.initiative(code, unit, name, SolutionType.TO_BE_DEFINED,
            SourceOrigin.OTHER, LocalDate.of(2026, 1, 1), "Responsable", null, null, "Descripción", null,
            DigitalComponent.NO, "subject"));
    }
}
