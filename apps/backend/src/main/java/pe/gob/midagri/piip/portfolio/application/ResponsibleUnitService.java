package pe.gob.midagri.piip.portfolio.application;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ResponsibleUnitInput;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.ResponsibleUnitUpdate;
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

    public List<ResponsibleUnitEntity> list(PortfolioRecordEntity record) {
        return responsibleUnits.findByRecordIdOrderByDisplayOrder(record.getId());
    }

    /** Valida la lista completa antes de tocar las asociaciones persistidas. */
    public void replace(PortfolioRecordEntity record, List<ResponsibleUnitUpdate> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new InvalidReferenceException("Debe existir al menos una Unidad Orgánica responsable",
                "responsibleUnits", null, "EMPTY");
        }
        if (organizationalUnits == null) {
            throw new IllegalStateException("No se configuró el repositorio de Unidades Orgánicas");
        }
        Set<Long> seen = new HashSet<>();
        var resolved = inputs.stream().map(input -> {
            if (!seen.add(input.organizationalUnitId())) {
                throw new InvalidReferenceException("La Unidad Orgánica está repetida", "responsibleUnits",
                    input.organizationalUnitId(), "DUPLICATE");
            }
            var unit = organizationalUnits.findHistoricalById(input.organizationalUnitId())
                .orElseThrow(() -> new InvalidReferenceException("La Unidad Orgánica no existe", "responsibleUnits",
                    input.organizationalUnitId(), "NOT_FOUND"));
            if (!unit.isActive()) throw new InvalidReferenceException("La Unidad Orgánica está inactiva", "responsibleUnits",
                input.organizationalUnitId(), "INACTIVE");
            if (!unit.getExecutingUnit().getId().equals(record.getExecutingUnit().getId()))
                throw new InvalidReferenceException("La Unidad Orgánica pertenece a otra Unidad Ejecutora", "responsibleUnits",
                    input.organizationalUnitId(), "OUTSIDE_EXECUTING_UNIT");
            return unit;
        }).toList();

        var current = responsibleUnits.findByRecordIdOrderByDisplayOrder(record.getId());
        boolean same = current.size() == resolved.size();
        for (int i = 0; same && i < resolved.size(); i++) {
            same = current.get(i).getOrganizationalUnit().getId().equals(resolved.get(i).getId());
        }
        if (same) return;

        responsibleUnits.deleteAll(current);
        responsibleUnits.flush();
        for (int i = 0; i < resolved.size(); i++) {
            var unit = resolved.get(i);
            responsibleUnits.save(new ResponsibleUnitEntity(record, unit, unit.getName(), i + 1));
        }
        responsibleUnits.flush();
    }
}
