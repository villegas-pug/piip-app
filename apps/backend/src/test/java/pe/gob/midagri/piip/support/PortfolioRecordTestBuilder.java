package pe.gob.midagri.piip.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;

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

    /** Siembra los once estados del catálogo (idempotente) para respaldar la FK por código natural de REGISTRO_PORTAFOLIO.ESTADO. */
    public static List<PortfolioStatusCatalogEntity> seedPortfolioStatuses(PortfolioStatusRepository statuses) {
        List<PortfolioStatusCatalogEntity> result = new ArrayList<>();
        for (PortfolioStatus code : PortfolioStatus.values()) {
            result.add(statuses.findByCode(code).orElseGet(() -> statuses.saveAndFlush(
                new PortfolioStatusCatalogEntity(code, code.label(), code.ordinal() + 1, true, applicabilityOf(code)))));
        }
        return result;
    }

    private static PortfolioStatusApplicability applicabilityOf(PortfolioStatus code) {
        return switch (code) {
            case PRESENTED, INITIATIVE_APPROVED, INITIATIVE_ARCHIVED, NOT_ADMISSIBLE -> PortfolioStatusApplicability.INITIATIVE;
            case PROJECT_IN_PROGRESS, PRODUCT_APPROVED, PRODUCT_NOT_APPROVED, SUSPENDED, CANCELLED, FINISHED -> PortfolioStatusApplicability.PROJECT;
            case NOT_APPLICABLE -> PortfolioStatusApplicability.NONE;
        };
    }
}
