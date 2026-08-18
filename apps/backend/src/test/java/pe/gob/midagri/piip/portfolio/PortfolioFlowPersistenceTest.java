package pe.gob.midagri.piip.portfolio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.SolutionType;
import pe.gob.midagri.piip.portfolio.domain.SourceOrigin;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;

@DataJpaTest
@ActiveProfiles("test")
class PortfolioFlowPersistenceTest {
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;
    @Autowired PortfolioRecordRepository records;

    @Test
    void preservesTheExistingRegistrationApprovalAndDerivationStates() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI-FLOW-JPA", "Institución de flujo"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-FLOW-JPA", "Unidad de flujo"));
        PortfolioRecordEntity initiative = records.save(PortfolioRecordEntity.initiative("I-FLOW-JPA-2026", unit, "Iniciativa",
            SolutionType.TO_BE_DEFINED, SourceOrigin.OTHER, LocalDate.of(2026, 8, 18), "Responsable", null, null,
            "Descripción", null, DigitalComponent.NO, "subject-flow"));

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.PRESENTED);
        initiative.approve();
        records.flush();

        PortfolioRecordEntity project = records.save(PortfolioRecordEntity.derivedProject("P-FLOW-JPA-2026", initiative, "Proyecto",
            SolutionType.TO_BE_DEFINED, SourceOrigin.OTHER, LocalDate.of(2026, 8, 18), "Responsable", null, null,
            "Descripción", null, null, DigitalComponent.NO, "subject-flow"));
        records.flush();

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);
        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.PROJECT_IN_PROGRESS);
        assertThat(project.getOriginRecord()).isSameAs(initiative);
    }

    @Test
    void enforcesOneDerivedProjectPerInitiativeAtPersistenceBoundary() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI-UNIQUE-JPA", "Institución única"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-UNIQUE-JPA", "Unidad única"));
        PortfolioRecordEntity initiative = records.save(PortfolioRecordEntity.initiative("I-UNIQUE-JPA-2026", unit, "Iniciativa",
            SolutionType.TO_BE_DEFINED, SourceOrigin.OTHER, LocalDate.of(2026, 8, 18), "Responsable", null, null,
            "Descripción", null, DigitalComponent.NO, "subject-flow"));
        initiative.approve();
        records.saveAndFlush(PortfolioRecordEntity.derivedProject("P-UNIQUE-JPA-2026", initiative, "Proyecto uno",
            SolutionType.TO_BE_DEFINED, SourceOrigin.OTHER, LocalDate.of(2026, 8, 18), "Responsable", null, null,
            "Descripción", null, null, DigitalComponent.NO, "subject-flow"));

        assertThatThrownBy(() -> records.saveAndFlush(PortfolioRecordEntity.derivedProject("P-UNIQUE-JPA-2027", initiative, "Proyecto dos",
            SolutionType.TO_BE_DEFINED, SourceOrigin.OTHER, LocalDate.of(2026, 8, 18), "Responsable", null, null,
            "Descripción", null, null, DigitalComponent.NO, "subject-flow")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
