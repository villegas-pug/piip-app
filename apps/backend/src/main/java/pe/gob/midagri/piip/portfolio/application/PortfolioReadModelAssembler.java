package pe.gob.midagri.piip.portfolio.application;

import org.springframework.stereotype.Service;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.PersistentCatalogItemResponse;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.TechnicalCatalogItemResponse;
import pe.gob.midagri.piip.catalogs.persistence.CatalogItemEntity;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.OrganizationalUnitResponse;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioRecordResponse;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ResponsibleUnitResponse;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.api.PageResponse;

/** Punto único para adaptar el modelo persistente al contrato HTTP congelado. */
@Service
public class PortfolioReadModelAssembler {
    private final ResponsibleUnitRepository responsibleUnits;

    public PortfolioReadModelAssembler(ResponsibleUnitRepository responsibleUnits) {
        this.responsibleUnits = responsibleUnits;
    }

    public PortfolioReadModels.PortfolioPageView page(PageResponse<PortfolioRecordResponse> value) {
        return new PortfolioReadModels.PortfolioPageView(value);
    }

    public PortfolioRecordResponse toResponse(PortfolioRecordEntity record) {
        var units = responsibleUnits.findByRecordIdOrderByDisplayOrder(record.getId()).stream().map(value -> {
            var unit = value.getOrganizationalUnit();
            return new ResponsibleUnitResponse(
                new OrganizationalUnitResponse(unit.getId(), unit.getCode(), unit.getName(), unit.isActive(), unit.getAcronym(),
                    unit.getParent() == null ? null : unit.getParent().getId(), unit.getExecutingUnit().getId()),
                value.getOriginalDesignation(), value.getDisplayOrder());
        }).toList();
        return new PortfolioRecordResponse(
            new TechnicalCatalogItemResponse(record.getRecordType().name(), record.getRecordType().label(), record.getRecordType().ordinal(), true),
            record.getCode(), record.getOriginCode(), record.getName(), catalog(record.getSolutionType()),
            catalog(record.getSourceOrigin()), record.getStartDate(), record.getResponsible(), catalog(record.getPeiObjective()),
            catalog(record.getPoiActivity()), units, record.getDescription(), record.getKeyResults(), record.getNote(),
            record.getStatus().label(), record.getFinalProductType().label(), record.getDigitalComponent().label(),
            record.getClosingDate(), null, null, null, null, null, record.getExecutingUnit().getId(),
            record.getExecutingUnit().getName(), record.getUpdatedAt(), record.getVersion());
    }

    private PersistentCatalogItemResponse catalog(CatalogItemEntity value) {
        return value == null ? null : new PersistentCatalogItemResponse(value.getId(), value.getCode(), value.getName(),
            value.getDisplayOrder(), value.isActive());
    }
}
