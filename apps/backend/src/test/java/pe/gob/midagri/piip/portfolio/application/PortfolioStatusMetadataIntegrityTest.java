package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;

/**
 * Integridad de la metadata de estados (FR-019/FR-020/FR-021): cambiar actividad, orden o
 * aplicabilidad no reasigna registros existentes ni habilita transiciones; la FK por código natural
 * rechaza la eliminación de un estado referenciado.
 */
@DataJpaTest
@ActiveProfiles("test")
class PortfolioStatusMetadataIntegrityTest {
    @Autowired PortfolioStatusRepository statuses;
    @Autowired PortfolioRecordRepository records;
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;
    @Autowired CatalogRepository catalogs;
    @Autowired CatalogItemRepository catalogItems;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void seedStatusCatalog() {
        PortfolioRecordTestBuilder.seedPortfolioStatuses(statuses);
    }

    @Test
    void cambiarMetadataNoReasignaRegistrosExistentes() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI-INT", "Institución"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-INT", "Unidad"));
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, "int");
        PortfolioRecordEntity initiative = records.saveAndFlush(fixtures.initiative("I-INT-2026", unit, "Iniciativa"));

        PortfolioStatusCatalogEntity presentado = statuses.findByCode(PortfolioStatus.PRESENTED).orElseThrow();
        presentado.rename("Presentado (renombrado)");
        presentado.deactivate();
        ReflectionTestUtils.setField(presentado, "displayOrder", 99);
        ReflectionTestUtils.setField(presentado, "applicability", PortfolioStatusApplicability.PROJECT);
        statuses.saveAndFlush(presentado);
        entityManager.clear();

        PortfolioRecordEntity persistido = records.findByCodeIgnoreCase("I-INT-2026").orElseThrow();
        // El registro no se reasigna: conserva su código de estado.
        assertThat(persistido.getStatus()).isEqualTo(PortfolioStatus.PRESENTED);
        // La metadata vigente se resuelve por el mismo código; solo cambian los atributos de presentación.
        assertThat(persistido.getStatusCatalog()).isNotNull();
        assertThat(persistido.getStatusCatalog().getCode()).isEqualTo(PortfolioStatus.PRESENTED);
        assertThat(persistido.getStatusCatalog().getName()).isEqualTo("Presentado (renombrado)");
        assertThat(persistido.getStatusCatalog().isActive()).isFalse();
    }

    @Test
    void cambiarMetadataNoHabilitaTransiciones() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI-INT2", "Institución"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-INT2", "Unidad"));
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, "int2");
        PortfolioRecordEntity initiative = records.saveAndFlush(fixtures.initiative("I-INT2-2026", unit, "Iniciativa"));

        // Cambiar la aplicabilidad de FINISHED a INITIATIVE no habilita la transición iniciativa -> FINISHED:
        // la matriz permanece en el dominio y no se deriva de la metadata del catálogo.
        PortfolioStatusCatalogEntity finished = statuses.findByCode(PortfolioStatus.FINISHED).orElseThrow();
        ReflectionTestUtils.setField(finished, "applicability", PortfolioStatusApplicability.INITIATIVE);
        statuses.saveAndFlush(finished);

        assertThatThrownBy(() -> initiative.transitionInitiativeTo(PortfolioStatus.FINISHED,
            Instant.parse("2026-08-18T12:00:00Z"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void eliminarEstadoReferenciadoEsRechazadoPorLaFk() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI-INT3", "Institución"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-INT3", "Unidad"));
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, "int3");
        records.saveAndFlush(fixtures.initiative("I-INT3-2026", unit, "Iniciativa"));

        PortfolioStatusCatalogEntity presentado = statuses.findByCode(PortfolioStatus.PRESENTED).orElseThrow();
        statuses.delete(presentado);
        assertThatThrownBy(() -> statuses.flush()).isInstanceOf(DataIntegrityViolationException.class);
    }
}
