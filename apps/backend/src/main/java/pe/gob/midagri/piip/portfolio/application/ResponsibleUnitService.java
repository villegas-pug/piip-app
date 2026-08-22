package pe.gob.midagri.piip.portfolio.application;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ResponsibleUnitInput;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;

@Service
public class ResponsibleUnitService {
    private final ResponsibleUnitRepository responsibleUnits;
    private final OrganizationalUnitRepository organizationalUnits;

    public ResponsibleUnitService(ResponsibleUnitRepository responsibleUnits, OrganizationalUnitRepository organizationalUnits) {
        this.responsibleUnits = responsibleUnits;
        this.organizationalUnits = organizationalUnits;
    }

    public void save(PortfolioRecordEntity record, List<ResponsibleUnitInput> inputs) {
        int order = 1;
        for (ResponsibleUnitInput input : inputs) {
            var unit = organizationalUnits.findHistoricalById(input.organizationalUnitId())
                .orElseThrow(() -> new InvalidReferenceException("La Unidad Orgánica no existe", "organizationalUnitId",
                    input.organizationalUnitId(), "NOT_FOUND"));
            if (!unit.isActive()) throw new InvalidReferenceException("La Unidad Orgánica está inactiva", "organizationalUnitId",
                input.organizationalUnitId(), "INACTIVE");
            if (!unit.getExecutingUnit().getId().equals(record.getExecutingUnit().getId()))
                throw new InvalidReferenceException("La Unidad Orgánica pertenece a otra Unidad Ejecutora", "organizationalUnitId",
                    input.organizationalUnitId(), "OUTSIDE_EXECUTING_UNIT");
            responsibleUnits.save(new ResponsibleUnitEntity(record, unit, unit.getName(), order++));
        }
    }
}
