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

    @SuppressWarnings("unchecked")
    @Test
    void recordsCatalogAndResponsibleUnitOrderAsStableStructuredValues() {
        PortfolioRecordEntity record = record();
        ExecutingUnitEntity unit = record.getExecutingUnit();
        OrganizationalUnitEntity first = organizationalUnit(8L, unit, "UO-1", "UOA");
        OrganizationalUnitEntity second = organizationalUnit(9L, unit, "UO-2", "UOB");
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
        List<?> previous = (List<?>) unitChange.get("anterior");
        List<?> current = (List<?>) unitChange.get("nuevo");
        assertThat(previous).hasSize(2);
        assertThat(current).hasSize(2);
        // FR-020: cada elemento identifica unidad, nombre, sigla y Nro de presentación (K=String para contains*).
        assertThat((Map<String, Object>) previous.get(0)).containsOnlyKeys("id", "code", "name", "sigla", "nro")
            .containsEntry("id", 8L).containsEntry("code", "UO-1").containsEntry("name", "UO-1")
            .containsEntry("sigla", "UOA").containsEntry("nro", 1);
        assertThat((Map<String, Object>) previous.get(1)).containsEntry("id", 9L).containsEntry("sigla", "UOB").containsEntry("nro", 2);
        assertThat((Map<String, Object>) current.get(0)).containsEntry("id", 9L).containsEntry("sigla", "UOB").containsEntry("nro", 1);
        assertThat((Map<String, Object>) current.get(1)).containsEntry("id", 8L).containsEntry("sigla", "UOA").containsEntry("nro", 2);
        Map<String, Object> detail = PortfolioUpdateAuditDetail.detail(record, 2L, 3L, before, after);
        assertThat(detail).containsKeys("tipoRegistro", "unidadEjecutoraId", "versionAnterior", "versionNueva", "cambios", "resultado");
        assertThat(detail).doesNotContainKeys("request", "body", "token", "motivo");
        assertThat(detail.get("versionAnterior")).isEqualTo(2L);
        assertThat(detail.get("versionNueva")).isEqualTo(3L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void addingAndRemovingUnitsIsReflectedInTheResponsibleUnitsDiff() {
        PortfolioRecordEntity record = record();
        ExecutingUnitEntity unit = record.getExecutingUnit();
        OrganizationalUnitEntity first = organizationalUnit(8L, unit, "UO-1", "UOA");
        OrganizationalUnitEntity second = organizationalUnit(9L, unit, "UO-2", "UOB");
        List<ResponsibleUnitEntity> single = List.of(new ResponsibleUnitEntity(record, first, "Unidad 1", 1));
        List<ResponsibleUnitEntity> pair = List.of(
            new ResponsibleUnitEntity(record, first, "Unidad 1", 1),
            new ResponsibleUnitEntity(record, second, "Unidad 2", 2));

        Map<?, ?> addition = (Map<?, ?>) PortfolioUpdateAuditDetail
            .diff(PortfolioUpdateAuditDetail.snapshot(record, single), PortfolioUpdateAuditDetail.snapshot(record, pair))
            .get("responsibleUnits");
        assertThat((List<?>) addition.get("anterior")).hasSize(1);
        assertThat((List<?>) addition.get("nuevo")).hasSize(2);
        assertThat((Map<String, Object>) ((List<?>) addition.get("nuevo")).get(1)).containsEntry("id", 9L)
            .containsEntry("sigla", "UOB").containsEntry("nro", 2);

        Map<?, ?> removal = (Map<?, ?>) PortfolioUpdateAuditDetail
            .diff(PortfolioUpdateAuditDetail.snapshot(record, pair), PortfolioUpdateAuditDetail.snapshot(record, single))
            .get("responsibleUnits");
        assertThat((List<?>) removal.get("anterior")).hasSize(2);
        assertThat((List<?>) removal.get("nuevo")).hasSize(1);
        assertThat((Map<String, Object>) ((List<?>) removal.get("nuevo")).get(0)).containsEntry("id", 8L).containsEntry("nro", 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void registrationDetailCarriesContextAndOrderedListWithoutRequestBodies() {
        PortfolioRecordEntity record = record();
        ExecutingUnitEntity unit = record.getExecutingUnit();
        List<ResponsibleUnitEntity> units = List.of(
            new ResponsibleUnitEntity(record, organizationalUnit(8L, unit, "UO-1", "UOA"), "Unidad 1", 1),
            new ResponsibleUnitEntity(record, organizationalUnit(9L, unit, "UO-2", "UOB"), "Unidad 2", 2));

        Map<String, Object> detail = PortfolioUpdateAuditDetail.registrationDetail(Map.of("estado", "Presentado"), units);

        assertThat(detail).containsOnlyKeys("estado", "responsibleUnits");
        assertThat(detail.get("estado")).isEqualTo("Presentado");
        List<?> audited = (List<?>) detail.get("responsibleUnits");
        assertThat(audited).hasSize(2);
        assertThat((Map<String, Object>) audited.get(0)).containsOnlyKeys("id", "code", "name", "sigla", "nro")
            .containsEntry("id", 8L).containsEntry("sigla", "UOA").containsEntry("nro", 1);
        assertThat((Map<String, Object>) audited.get(1)).containsEntry("id", 9L).containsEntry("sigla", "UOB").containsEntry("nro", 2);
        assertThat(detail).doesNotContainKeys("request", "body", "token", "motivo");
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

    private OrganizationalUnitEntity organizationalUnit(Long id, ExecutingUnitEntity unit, String code, String acronym) {
        OrganizationalUnitEntity value = new OrganizationalUnitEntity(unit, code, code, acronym);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
