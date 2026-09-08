package pe.gob.midagri.piip.portfolio.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ResponsibleUnitInput;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.ResponsibleUnitUpdate;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;

/**
 * Reglas de la lista de Unidades Orgánicas Involucradas (campo técnico responsibleUnits):
 * toda la lista se resuelve y valida antes de cualquier cambio de persistencia y el
 * conjunto se confirma de forma atómica con orden continuo 1..N y denominación del maestro.
 */
@Service
public class ResponsibleUnitService {
    private final ResponsibleUnitRepository responsibleUnits;
    private final OrganizationalUnitRepository organizationalUnits;

    public ResponsibleUnitService(ResponsibleUnitRepository responsibleUnits, OrganizationalUnitRepository organizationalUnits) {
        this.responsibleUnits = responsibleUnits;
        this.organizationalUnits = organizationalUnits;
    }

    /** Alta: todas las filas son incorporaciones nuevas; se resuelve y valida la lista completa antes de persistir. */
    public void save(PortfolioRecordEntity record, List<ResponsibleUnitInput> inputs) {
        List<Long> unitIds = requireValidList(identities(inputs));
        List<OrganizationalUnitEntity> resolved = new ArrayList<>(unitIds.size());
        for (int row = 1; row <= unitIds.size(); row++) {
            resolved.add(resolveIncorporation(record, unitIds.get(row - 1), row, "organizationalUnitId"));
        }
        int order = 1;
        for (OrganizationalUnitEntity unit : resolved) {
            responsibleUnits.save(new ResponsibleUnitEntity(record, unit, unit.getName(), order++));
        }
    }

    public List<ResponsibleUnitEntity> list(PortfolioRecordEntity record) {
        return responsibleUnits.findByRecordIdOrderByDisplayOrder(record.getId());
    }

    /**
     * Sustitución atómica del conjunto: resuelve y valida toda la lista antes de tocar las asociaciones
     * persistidas. Vigencia, pertenencia y sigla aplican solo a las nuevas incorporaciones; las asociaciones
     * históricas retenidas se conservan como contexto y la unicidad aplica al conjunto completo confirmado.
     */
    public void replace(PortfolioRecordEntity record, List<ResponsibleUnitUpdate> inputs) {
        List<Long> unitIds = requireValidList(updateIdentities(inputs));
        if (organizationalUnits == null) {
            throw new IllegalStateException("No se configuró el repositorio de Unidades Orgánicas");
        }
        var current = responsibleUnits.findByRecordIdOrderByDisplayOrder(record.getId());
        Set<Long> retainedUnitIds = new HashSet<>();
        for (ResponsibleUnitEntity association : current) {
            retainedUnitIds.add(association.getOrganizationalUnit().getId());
        }
        List<OrganizationalUnitEntity> resolved = new ArrayList<>(unitIds.size());
        for (int row = 1; row <= unitIds.size(); row++) {
            Long unitId = unitIds.get(row - 1);
            if (retainedUnitIds.contains(unitId)) {
                // Asociación histórica retenida: se conserva como contexto, sin revalidar vigencia ni sigla.
                resolved.add(resolveUnit(unitId, "responsibleUnits"));
            } else {
                resolved.add(resolveIncorporation(record, unitId, row, "responsibleUnits"));
            }
        }

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

    /**
     * Mínimo una unidad y sin duplicados en el conjunto completo. La lista vacía ya se rechaza con 400
     * en el DTO; este rechazo en el servicio es la defensa en profundidad para llamadores internos.
     */
    private static List<Long> requireValidList(List<Long> unitIds) {
        if (unitIds == null || unitIds.isEmpty()) {
            throw new InvalidReferenceException("La lista de Unidades Orgánicas Involucradas debe incluir al menos una unidad",
                "responsibleUnits", unitIds == null ? null : Long.valueOf(unitIds.size()), "INVALID_SIZE");
        }
        Map<Long, Integer> firstRowByUnitId = new HashMap<>();
        for (int row = 0; row < unitIds.size(); row++) {
            Long unitId = unitIds.get(row);
            Integer firstRow = firstRowByUnitId.putIfAbsent(unitId, row);
            if (firstRow != null) {
                throw new InvalidReferenceException("La Unidad Orgánica de la fila " + (row + 1)
                    + " está duplicada (ya seleccionada en la fila " + (firstRow + 1) + ")",
                    "responsibleUnits[" + (row + 1) + "]", unitId, "DUPLICATED_UNIT");
            }
        }
        return unitIds;
    }

    /** Resuelve la unidad por identidad, incluidas inactivas, para lectura o reconfirmación de contexto histórico. */
    private OrganizationalUnitEntity resolveUnit(Long unitId, String referenceField) {
        return organizationalUnits.findHistoricalById(unitId)
            .orElseThrow(() -> new InvalidReferenceException("La Unidad Orgánica no existe", referenceField,
                unitId, "NOT_FOUND"));
    }

    /** Nueva incorporación: existente, activa, de la misma Unidad Ejecutora del registro y con sigla registrada. */
    private OrganizationalUnitEntity resolveIncorporation(PortfolioRecordEntity record, Long unitId, int row, String referenceField) {
        var unit = resolveUnit(unitId, referenceField);
        if (!unit.isActive()) throw new InvalidReferenceException("La Unidad Orgánica está inactiva", referenceField,
            unitId, "INACTIVE");
        if (!unit.getExecutingUnit().getId().equals(record.getExecutingUnit().getId()))
            throw new InvalidReferenceException("La Unidad Orgánica pertenece a otra Unidad Ejecutora", referenceField,
                unitId, "OUTSIDE_EXECUTING_UNIT");
        requireAcronym(unit, row);
        return unit;
    }

    /** La sigla proviene del maestro y nunca se inventa ni completa; sin ella la unidad no puede incorporarse. */
    private static void requireAcronym(OrganizationalUnitEntity unit, int row) {
        if (unit.getAcronym() == null || unit.getAcronym().isBlank()) {
            throw new InvalidReferenceException("La Unidad Orgánica de la fila " + row + " no tiene sigla registrada",
                "responsibleUnits[" + row + "]", unit.getId(), "MISSING_ACRONYM");
        }
    }

    private static List<Long> identities(List<ResponsibleUnitInput> inputs) {
        return inputs == null ? null : inputs.stream().map(ResponsibleUnitInput::organizationalUnitId).toList();
    }

    // Renombrada respecto de identities: ambas sobrecargas tenían la misma erasure List y no compilaban.
    private static List<Long> updateIdentities(List<ResponsibleUnitUpdate> inputs) {
        return inputs == null ? null : inputs.stream().map(ResponsibleUnitUpdate::organizationalUnitId).toList();
    }
}
