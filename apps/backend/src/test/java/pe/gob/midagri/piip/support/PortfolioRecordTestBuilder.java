package pe.gob.midagri.piip.support;

import java.time.LocalDate;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;

/** Construye fixtures con el mismo modelo de referencias persistentes usado por producción. */
public final class PortfolioRecordTestBuilder {
    private final CatalogItemEntity solution;
    private final CatalogItemEntity source;

    private PortfolioRecordTestBuilder(CatalogItemEntity solution, CatalogItemEntity source) {
        this.solution = solution; this.source = source;
    }

    public static PortfolioRecordTestBuilder transientReferences() {
        return new PortfolioRecordTestBuilder(
            new CatalogItemEntity(new CatalogEntity(CatalogCode.SOLUTION_TYPE, "Tipo de solución", 10, true), "TO_BE_DEFINED", "Solución por definir", 10, true),
            new CatalogItemEntity(new CatalogEntity(CatalogCode.SOURCE_ORIGIN, "Fuente", 20, true), "OTHER", "Otros", 10, true));
    }

    public static PortfolioRecordTestBuilder persistedReferences(CatalogRepository catalogs, CatalogItemRepository items, String suffix) {
        CatalogEntity solutionCatalog = catalogs.findByCode(CatalogCode.SOLUTION_TYPE)
            .orElseGet(() -> catalogs.save(new CatalogEntity(CatalogCode.SOLUTION_TYPE, "Tipo de solución", 10, true)));
        CatalogEntity sourceCatalog = catalogs.findByCode(CatalogCode.SOURCE_ORIGIN)
            .orElseGet(() -> catalogs.save(new CatalogEntity(CatalogCode.SOURCE_ORIGIN, "Fuente", 20, true)));
        return new PortfolioRecordTestBuilder(
            items.save(new CatalogItemEntity(solutionCatalog, "TO_BE_DEFINED-" + suffix, "Solución por definir", 10, true)),
            items.save(new CatalogItemEntity(sourceCatalog, "OTHER-" + suffix, "Otros", 10, true)));
    }

    public PortfolioRecordEntity initiative(String code, ExecutingUnitEntity unit, String name) {
        return PortfolioRecordEntity.initiative(code, unit, name, solution, source, LocalDate.of(2026, 8, 18),
            "Responsable", null, null, "Descripción", null, DigitalComponent.NO, "subject");
    }
    public PortfolioRecordEntity derivedProject(String code, PortfolioRecordEntity origin, String name) {
        return PortfolioRecordEntity.derivedProject(code, origin, name, solution, source, LocalDate.of(2026, 8, 18),
            "Responsable", null, null, "Descripción", null, null, DigitalComponent.NO, "subject");
    }
    public PortfolioRecordEntity preexistingProject(String code, ExecutingUnitEntity unit, String name) {
        return PortfolioRecordEntity.preexistingProject(code, unit, name, solution, source, LocalDate.of(2026, 8, 18),
            "Responsable", null, null, "Descripción", null, null, DigitalComponent.NO, "subject");
    }
    public CatalogItemEntity solution() { return solution; }
    public CatalogItemEntity source() { return source; }
}
