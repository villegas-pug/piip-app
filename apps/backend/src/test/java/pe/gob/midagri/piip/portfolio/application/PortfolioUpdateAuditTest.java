package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.CatalogEntity;
import pe.gob.midagri.piip.catalogs.persistence.CatalogItemEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitEntity;

class PortfolioUpdateAuditTest {
    @Test
    void recordsOnlyEffectiveScalarChangesAndKeepsNullValuesExplicit() {
        PortfolioRecordEntity record = record();
        record.applyEditableFields(record.getName(), record.getSolutionType(), record.getSourceOrigin(), record.getStartDate(),
            record.getResponsible(), record.getPeiObjective(), record.getPoiActivity(), record.getDescription(),
            record.getKeyResults(), "Nota inicial", record.getDigitalComponent(), record.getUpdatedAt());
        Map<String, Object> before = PortfolioUpdateAuditDetail.snapshot(record, List.of());
        record.applyEditableFields("Nombre nuevo", record.getSolutionType(), record.getSourceOrigin(), record.getStartDate(),
            record.getResponsible(), null, record.getPoiActivity(), record.getDescription(), record.getKeyResults(),
            null, record.getDigitalComponent(), record.getUpdatedAt());
        Map<String, Object> after = PortfolioUpdateAuditDetail.snapshot(record, List.of());

        Map<String, Object> changes = PortfolioUpdateAuditDetail.diff(before, after);
        assertThat(changes).containsOnlyKeys("name", "note");
        assertThat(changes.get("name")).isEqualTo(Map.of("anterior", "Nombre", "nuevo", "Nombre nuevo"));
        assertThat(((Map<?, ?>) changes.get("note")).get("nuevo")).isNull();
    }

    @Test
    void recordsCatalogAndResponsibleUnitOrderAsStableStructuredValues() {
        PortfolioRecordEntity record = record();
        ExecutingUnitEntity unit = record.getExecutingUnit();
        OrganizationalUnitEntity first = organizationalUnit(8L, unit, "UO-1");
        OrganizationalUnitEntity second = organizationalUnit(9L, unit, "UO-2");
        List<ResponsibleUnitEntity> beforeUnits = List.of(
            new ResponsibleUnitEntity(record, first, "Unidad 1", 1),
            new ResponsibleUnitEntity(record, second, "Unidad 2", 2));
        List<ResponsibleUnitEntity> afterUnits = List.of(
            new ResponsibleUnitEntity(record, second, "Unidad 2", 1),
            new ResponsibleUnitEntity(record, first, "Unidad 1", 2));

        Map<String, Object> before = PortfolioUpdateAuditDetail.snapshot(record, beforeUnits);
        Map<String, Object> after = PortfolioUpdateAuditDetail.snapshot(record, afterUnits);
        Map<String, Object> changes = PortfolioUpdateAuditDetail.diff(before, after);

        assertThat(changes).containsOnlyKeys("responsibleUnits");
        Map<?, ?> unitChange = (Map<?, ?>) changes.get("responsibleUnits");
        assertThat((List<?>) unitChange.get("anterior")).hasSize(2);
        assertThat((List<?>) unitChange.get("nuevo")).hasSize(2);
        Map<String, Object> detail = PortfolioUpdateAuditDetail.detail(record, 2L, 3L, before, after);
        assertThat(detail).containsKeys("tipoRegistro", "unidadEjecutoraId", "versionAnterior", "versionNueva", "cambios", "resultado");
        assertThat(detail).doesNotContainKeys("request", "body", "token", "motivo");
        assertThat(detail.get("versionAnterior")).isEqualTo(2L);
        assertThat(detail.get("versionNueva")).isEqualTo(3L);
    }

    @Test
    void unchangedSnapshotProducesNoFunctionalAuditChange() {
        PortfolioRecordEntity record = record();
        Map<String, Object> snapshot = PortfolioUpdateAuditDetail.snapshot(record, List.of());
        assertThat(PortfolioUpdateAuditDetail.diff(snapshot, snapshot)).isEmpty();
    }

    private PortfolioRecordEntity record() {
        InstitutionEntity institution = new InstitutionEntity("INST-AUDIT-UPDATE", "Institución");
        ReflectionTestUtils.setField(institution, "id", 20L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-AUDIT-UPDATE", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 10L);
        CatalogEntity solutionCatalog = new CatalogEntity(CatalogCode.SOLUTION_TYPE, "Soluciones", 1, true);
        CatalogEntity sourceCatalog = new CatalogEntity(CatalogCode.SOURCE_ORIGIN, "Fuentes", 2, true);
        CatalogItemEntity solution = new CatalogItemEntity(solutionCatalog, "SOL", "Solución", 1, true);
        CatalogItemEntity source = new CatalogItemEntity(sourceCatalog, "SRC", "Fuente", 1, true);
        ReflectionTestUtils.setField(solution, "id", 101L);
        ReflectionTestUtils.setField(source, "id", 102L);
        return PortfolioRecordEntity.initiative("I-AUDIT-UPDATE", unit, "Nombre", solution, source,
            java.time.LocalDate.of(2026, 8, 22), "Responsable", null, null, "Descripción", null,
            pe.gob.midagri.piip.portfolio.domain.DigitalComponent.NO, "actor");
    }

    private OrganizationalUnitEntity organizationalUnit(Long id, ExecutingUnitEntity unit, String code) {
        OrganizationalUnitEntity value = new OrganizationalUnitEntity(unit, code, code, "U");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
