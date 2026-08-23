package pe.gob.midagri.piip.portfolio;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import java.time.LocalDate;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class PortfolioTransitionTest {
    @Test
    void onlyPresentedInitiativeCanBeApproved() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-DEMO", "Unidad demostrativa");
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences().initiative("I-001-2026", unit, "Iniciativa");

        initiative.approve();
        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);
        assertThatThrownBy(initiative::approve).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initiativeTransitionsUseOnlyTheContextualMatrix() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-DEMO", "Unidad demostrativa");
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences().initiative("I-002-2026", unit, "Iniciativa");

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
        PortfolioRecordEntity project = PortfolioRecordTestBuilder.transientReferences().preexistingProject("P-001-2026", unit, "Proyecto");

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
        PortfolioRecordTestBuilder builder = PortfolioRecordTestBuilder.transientReferences();
        PortfolioRecordEntity initiative = builder.initiative("I-003-2026", unit, "Iniciativa");
        initiative.approve();

        PortfolioRecordEntity project = builder.derivedProject("P-002-2026", initiative, "Proyecto");

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);
        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.PROJECT_IN_PROGRESS);
        assertThat(project.getOriginRecord()).isSameAs(initiative);
    }

    @Test
    void registrationApprovalAndDerivationPreserveTheExistingJourney() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI-FLOW", "Ministerio de prueba");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-FLOW", "Unidad de flujo");
        PortfolioRecordTestBuilder builder = PortfolioRecordTestBuilder.transientReferences();
        PortfolioRecordEntity initiative = builder.initiative("I-FLOW-2026", unit, "Iniciativa de regresión");

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.PRESENTED);

        initiative.approve();
        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);

        PortfolioRecordEntity project = builder.derivedProject("P-FLOW-2026", initiative, "Proyecto derivado");

        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.PROJECT_IN_PROGRESS);
        assertThat(project.getOriginRecord()).isSameAs(initiative);
        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);
    }

    @Test
    void editingFieldsCannotCloseAProjectOrCreateAStatusTransition() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI-EDIT", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-EDIT", "Unidad edición");
        PortfolioRecordEntity project = PortfolioRecordTestBuilder.transientReferences()
            .preexistingProject("P-EDIT-2026", unit, "Proyecto");

        project.applyEditableFields("Proyecto actualizado", project.getSolutionType(), project.getSourceOrigin(),
            project.getStartDate(), project.getResponsible(), project.getPeiObjective(), project.getPoiActivity(),
            project.getDescription(), project.getKeyResults(), project.getNote(), project.getDigitalComponent(),
            Instant.parse("2026-08-22T12:00:00Z"));

        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.PROJECT_IN_PROGRESS);
        assertThat(project.getClosingDate()).isNull();
    }
}
