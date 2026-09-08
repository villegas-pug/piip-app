package pe.gob.midagri.piip.portfolio.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitEntity;

/** Construye snapshots y diffs estables sin serializar el request HTTP. */
public final class PortfolioUpdateAuditDetail {
    private PortfolioUpdateAuditDetail() {}

    /**
     * Detalle de los eventos de alta (FR-018): contexto vigente del evento más la lista ordenada
     * confirmada de Unidades Orgánicas Involucradas bajo la clave técnica responsibleUnits.
     */
    public static Map<String, Object> registrationDetail(Map<String, Object> context, List<ResponsibleUnitEntity> units) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.putAll(context);
        detail.put("responsibleUnits", responsibleUnitsAuditValue(units));
        return detail;
    }

    public static Map<String, Object> snapshot(PortfolioRecordEntity record, List<ResponsibleUnitEntity> units) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", record.getName());
        snapshot.put("solutionType", catalog(record.getSolutionType()));
        snapshot.put("source", catalog(record.getSourceOrigin()));
        snapshot.put("startDate", record.getStartDate());
        snapshot.put("responsible", record.getResponsible());
        snapshot.put("peiObjective", catalog(record.getPeiObjective()));
        snapshot.put("poiActivity", catalog(record.getPoiActivity()));
        snapshot.put("responsibleUnits", responsibleUnitsAuditValue(units));
        snapshot.put("description", record.getDescription());
        snapshot.put("keyResults", record.getKeyResults());
        snapshot.put("note", record.getNote());
        snapshot.put("digitalComponent", record.getDigitalComponent() == null ? null : record.getDigitalComponent().name());
        return snapshot;
    }

    public static Map<String, Object> detail(PortfolioRecordEntity record, long previousVersion,
            long newVersion, Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("tipoRegistro", record.getRecordType().label());
        detail.put("unidadEjecutoraId", record.getExecutingUnit().getId());
        detail.put("unidadEjecutora", record.getExecutingUnit().getName());
        detail.put("versionAnterior", previousVersion);
        detail.put("versionNueva", newVersion);
        detail.put("cambios", diff(before, after));
        detail.put("resultado", "EXITOSO");
        return detail;
    }

    public static Map<String, Object> diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        before.forEach((key, oldValue) -> {
            Object newValue = after.get(key);
            if (!Objects.equals(oldValue, newValue)) {
                Map<String, Object> change = new LinkedHashMap<>();
                change.put("anterior", oldValue);
                change.put("nuevo", newValue);
                changes.put(key, change);
            }
        });
        return changes;
    }

    private static Map<String, Object> catalog(pe.gob.midagri.piip.catalogs.persistence.CatalogItemEntity item) {
        if (item == null) return null;
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", item.getId());
        value.put("code", item.getCode());
        value.put("name", item.getName());
        return value;
    }

    /**
     * Elemento auditado compartido por altas y ediciones (FR-020): identidad y código de la unidad,
     * nombre y sigla del maestro organizacional, y Nro de presentación, que corresponde exactamente
     * a la posición 1..N persistida en ORDEN_PRESENTACION. La sigla puede ser null solo cuando la
     * asociación histórica retenida se conserva como contexto; nunca se inventa ni completa.
     */
    public static List<Map<String, Object>> responsibleUnitsAuditValue(List<ResponsibleUnitEntity> values) {
        List<Map<String, Object>> result = new ArrayList<>(values.size());
        for (ResponsibleUnitEntity value : values) {
            var unit = value.getOrganizationalUnit();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", unit.getId());
            item.put("code", unit.getCode());
            item.put("name", unit.getName());
            item.put("sigla", unit.getAcronym());
            item.put("nro", value.getDisplayOrder());
            result.add(item);
        }
        return result;
    }
}
