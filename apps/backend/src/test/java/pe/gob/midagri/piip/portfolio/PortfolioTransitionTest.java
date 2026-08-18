package pe.gob.midagri.piip.portfolio;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import java.time.LocalDate;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class PortfolioTransitionTest {
    @Test
    void onlyPresentedInitiativeCanBeApproved() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-DEMO", "Unidad demostrativa");
        PortfolioRecordEntity initiative = PortfolioRecordEntity.initiative("I-001-2026", unit, "Iniciativa", SolutionType.TO_BE_DEFINED,
            SourceOrigin.OTHER, LocalDate.of(2026, 7, 1), "Responsable", null, null, "Descripción", null, DigitalComponent.NO, "subject");

        initiative.approve();
        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);
        assertThatThrownBy(initiative::approve).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initiativeTransitionsUseOnlyTheContextualMatrix() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-DEMO", "Unidad demostrativa");
        PortfolioRecordEntity initiative = PortfolioRecordEntity.initiative("I-002-2026", unit, "Iniciativa", SolutionType.TO_BE_DEFINED,
            SourceOrigin.OTHER, LocalDate.of(2026, 7, 1), "Responsable", null, null, "Descripción", null, DigitalComponent.NO, "subject");

        initiative.transitionInitiativeTo(PortfolioStatus.INITIATIVE_ARCHIVED, Instant.parse("2026-08-18T12:00:00Z"));

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_ARCHIVED);
        assertThatThrownBy(() -> initiative.transitionInitiativeTo(PortfolioStatus.INITIATIVE_APPROVED,
            Instant.parse("2026-08-18T12:00:00Z"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> initiative.transitionInitiativeTo(PortfolioStatus.NOT_APPLICABLE,
            Instant.parse("2026-08-18T12:00:00Z"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void projectTransitionToFinishedSetsClosingDateOnlyAtCompletion() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-DEMO", "Unidad demostrativa");
        PortfolioRecordEntity project = PortfolioRecordEntity.preexistingProject("P-001-2026", unit, "Proyecto", SourceOrigin.OTHER,
            LocalDate.of(2026, 7, 1), "Responsable", null, null, "Descripción", null, null, DigitalComponent.NO, "subject");

        project.transitionProjectTo(PortfolioStatus.PRODUCT_APPROVED, Instant.parse("2026-08-18T12:00:00Z"), LocalDate.of(2026, 8, 18));
        assertThat(project.getClosingDate()).isNull();
        project.transitionProjectTo(PortfolioStatus.FINISHED, Instant.parse("2026-08-19T12:00:00Z"), LocalDate.of(2026, 8, 19));

        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.FINISHED);
        assertThat(project.getClosingDate()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThatThrownBy(() -> project.transitionProjectTo(PortfolioStatus.PROJECT_IN_PROGRESS,
            Instant.parse("2026-08-20T12:00:00Z"), LocalDate.of(2026, 8, 20))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void derivedProjectKeepsInitiativeApprovedAndUsesOriginRelation() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-DEMO", "Unidad demostrativa");
        PortfolioRecordEntity initiative = PortfolioRecordEntity.initiative("I-003-2026", unit, "Iniciativa", SolutionType.TO_BE_DEFINED,
            SourceOrigin.OTHER, LocalDate.of(2026, 7, 1), "Responsable", null, null, "Descripción", null, DigitalComponent.NO, "subject");
        initiative.approve();

        PortfolioRecordEntity project = PortfolioRecordEntity.derivedProject("P-002-2026", initiative, "Proyecto", SolutionType.TO_BE_DEFINED,
            SourceOrigin.OTHER, LocalDate.of(2026, 7, 1), "Responsable", null, null, "Descripción", null, null, DigitalComponent.NO, "subject");

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);
        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.PROJECT_IN_PROGRESS);
        assertThat(project.getOriginRecord()).isSameAs(initiative);
    }

    @Test
    void registrationApprovalAndDerivationPreserveTheExistingJourney() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI-FLOW", "Ministerio de prueba");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-FLOW", "Unidad de flujo");
        PortfolioRecordEntity initiative = PortfolioRecordEntity.initiative("I-FLOW-2026", unit, "Iniciativa de regresión",
            SolutionType.TO_BE_DEFINED, SourceOrigin.OTHER, LocalDate.of(2026, 8, 18), "Responsable", null, null,
            "Descripción", null, DigitalComponent.NO, "subject-flow");

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.PRESENTED);

        initiative.approve();
        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);

        PortfolioRecordEntity project = PortfolioRecordEntity.derivedProject("P-FLOW-2026", initiative, "Proyecto derivado",
            SolutionType.TO_BE_DEFINED, SourceOrigin.OTHER, LocalDate.of(2026, 8, 18), "Responsable", null, null,
            "Descripción", null, null, DigitalComponent.NO, "subject-flow");

        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.PROJECT_IN_PROGRESS);
        assertThat(project.getOriginRecord()).isSameAs(initiative);
        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);
    }
}
