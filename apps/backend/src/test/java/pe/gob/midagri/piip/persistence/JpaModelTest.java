package pe.gob.midagri.piip.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JpaModelTest {
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;
    @Autowired PortfolioRecordRepository records;
    @Autowired CatalogRepository catalogs;
    @Autowired CatalogItemRepository catalogItems;
    @Autowired EntityManager entityManager;

    @Test
    void persistsTheCanonicalPortfolioAggregate() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI", "Ministerio de Desarrollo Agrario y Riego"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-DEMO", "Unidad demostrativa"));
        PortfolioRecordEntity record = records.save(PortfolioRecordEntity.initiative("I-001-2026", unit, "Iniciativa", item(CatalogCode.SOLUTION_TYPE, "TO_BE_DEFINED"),
            item(CatalogCode.SOURCE_ORIGIN, "INITIATIVE_SHEET"), LocalDate.of(2026, 7, 1), "Responsable", null, null, "Descripción", "Nota", DigitalComponent.NO, "subject"));

        assertThat(records.findByCodeIgnoreCase(record.getCode())).get().extracting(PortfolioRecordEntity::getStatus).isEqualTo(PortfolioStatus.PRESENTED);
    }

    @Test
    void resolvesTheInitiativeCodeFromALazyProjectOrigin() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI-ORIGIN", "Institución de prueba"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-ORIGIN", "Unidad de prueba"));
        PortfolioRecordEntity initiative = records.save(PortfolioRecordEntity.initiative("I-002-2026", unit, "Iniciativa origen",
            item(CatalogCode.SOLUTION_TYPE, "TO_BE_DEFINED-2"), item(CatalogCode.SOURCE_ORIGIN, "INITIATIVE_SHEET-2"), LocalDate.of(2026, 7, 1), "Responsable", null, null,
            "Descripción", "Nota", DigitalComponent.YES, "subject"));
        records.save(PortfolioRecordEntity.derivedProject("P-001-2026", initiative, "Proyecto derivado", initiative.getSolutionType(),
            initiative.getSourceOrigin(), LocalDate.of(2026, 7, 2), "Responsable", null, null, "Descripción", "Resultados",
            "Nota", DigitalComponent.YES, "subject"));
        entityManager.flush();
        entityManager.clear();

        PortfolioRecordEntity project = records.findAll(PageRequest.of(0, 10)).stream()
            .filter(record -> record.getRecordType() == RecordType.PROJECT)
            .findFirst().orElseThrow();

        assertThat(project.getOriginCode()).isEqualTo("I-002-2026");
    }

    private CatalogItemEntity item(CatalogCode code, String itemCode) {
        CatalogEntity catalog = catalogs.findByCode(code).orElseGet(() -> catalogs.save(new CatalogEntity(code, code.name(), code.ordinal(), true)));
        return catalogItems.save(new CatalogItemEntity(catalog, itemCode, itemCode, 10, true));
    }
}
