package pe.gob.midagri.piip.portfolio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.application.PortfolioApplicationSupport;
import pe.gob.midagri.piip.portfolio.application.PortfolioStatusValidationService;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.ProblemCode;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @ParameterizedTest(name = "iniciativa {0} -> {1} permitido={2}")
    @MethodSource("initiativeMatrix")
    void initiativeMatrixAppliesContextualTransitionsOnly(PortfolioStatus source, PortfolioStatus target, boolean allowed) {
        PortfolioRecordEntity initiative = initiativeAt(source);
        if (allowed) {
            initiative.transitionInitiativeTo(target, Instant.parse("2026-08-18T12:00:00Z"));
            assertThat(initiative.getStatus()).isEqualTo(target);
        } else {
            assertThatThrownBy(() -> initiative.transitionInitiativeTo(target, Instant.parse("2026-08-18T12:00:00Z")))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @ParameterizedTest(name = "proyecto {0} -> {1} permitido={2}")
    @MethodSource("projectMatrix")
    void projectMatrixAppliesContextualTransitionsOnly(PortfolioStatus source, PortfolioStatus target, boolean allowed) {
        PortfolioRecordEntity project = projectAt(source);
        if (allowed) {
            project.transitionProjectTo(target, Instant.parse("2026-08-18T12:00:00Z"), LocalDate.of(2026, 8, 18));
            assertThat(project.getStatus()).isEqualTo(target);
        } else {
            assertThatThrownBy(() -> project.transitionProjectTo(target, Instant.parse("2026-08-18T12:00:00Z"), LocalDate.of(2026, 8, 18)))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void transitionTargetValidationDistinguishesExistenceActivityAndApplicability() {
        PortfolioStatusRepository repo = mock(PortfolioStatusRepository.class);
        PortfolioApplicationSupport support = new PortfolioApplicationSupport(mock(LocalAuthorizationService.class),
            Clock.systemUTC(), new PortfolioStatusValidationService(repo));

        assertThatThrownBy(() -> support.validateTransitionTarget("DESCONOCIDO", RecordType.INITIATIVE))
            .isInstanceOfSatisfying(BusinessRuleException.class, exception ->
                assertThat(exception.getProblemCode()).isEqualTo(ProblemCode.PORTFOLIO_STATUS_NOT_FOUND));

        when(repo.findByCode(PortfolioStatus.INITIATIVE_ARCHIVED))
            .thenReturn(Optional.of(catalog(PortfolioStatus.INITIATIVE_ARCHIVED, PortfolioStatusApplicability.INITIATIVE, false)));
        assertThatThrownBy(() -> support.validateTransitionTarget("INITIATIVE_ARCHIVED", RecordType.INITIATIVE))
            .isInstanceOfSatisfying(BusinessRuleException.class, exception ->
                assertThat(exception.getProblemCode()).isEqualTo(ProblemCode.PORTFOLIO_STATUS_INACTIVE));

        when(repo.findByCode(PortfolioStatus.NOT_APPLICABLE))
            .thenReturn(Optional.of(catalog(PortfolioStatus.NOT_APPLICABLE, PortfolioStatusApplicability.NONE, true)));
        assertThatThrownBy(() -> support.validateTransitionTarget("NOT_APPLICABLE", RecordType.INITIATIVE))
            .isInstanceOfSatisfying(BusinessRuleException.class, exception ->
                assertThat(exception.getProblemCode()).isEqualTo(ProblemCode.PORTFOLIO_STATUS_NOT_APPLICABLE));
    }

    @Test
    void inactiveOriginDoesNotBlockALeaveTowardAValidMatrixTarget() {
        PortfolioStatusRepository repo = mock(PortfolioStatusRepository.class);
        when(repo.findByCode(PortfolioStatus.CANCELLED))
            .thenReturn(Optional.of(catalog(PortfolioStatus.CANCELLED, PortfolioStatusApplicability.PROJECT, true)));
        PortfolioApplicationSupport support = new PortfolioApplicationSupport(mock(LocalAuthorizationService.class),
            Clock.systemUTC(), new PortfolioStatusValidationService(repo));

        // La validación del destino no consulta el origen: CANCELLED (activo y aplicable) se resuelve.
        assertThat(support.validateTransitionTarget("CANCELLED", RecordType.PROJECT)).isEqualTo(PortfolioStatus.CANCELLED);

        // La matriz del dominio permite SUSPENDED -> CANCELLED; el origen inactivo puede abandonarse.
        PortfolioRecordEntity project = projectAt(PortfolioStatus.SUSPENDED);
        assertThatCode(() -> project.transitionProjectTo(PortfolioStatus.CANCELLED,
            Instant.parse("2026-08-18T12:00:00Z"), LocalDate.of(2026, 8, 18))).doesNotThrowAnyException();
        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.CANCELLED);
    }

    @Test
    void inactiveOriginStillRejectsAnInvalidTargetByCause() {
        PortfolioStatusRepository repo = mock(PortfolioStatusRepository.class);
        when(repo.findByCode(PortfolioStatus.NOT_APPLICABLE))
            .thenReturn(Optional.of(catalog(PortfolioStatus.NOT_APPLICABLE, PortfolioStatusApplicability.NONE, true)));
        PortfolioApplicationSupport support = new PortfolioApplicationSupport(mock(LocalAuthorizationService.class),
            Clock.systemUTC(), new PortfolioStatusValidationService(repo));

        // Aunque el origen esté inactivo, el destino NOT_APPLICABLE se rechaza por aplicabilidad (NONE).
        assertThatThrownBy(() -> support.validateTransitionTarget("NOT_APPLICABLE", RecordType.PROJECT))
            .isInstanceOfSatisfying(BusinessRuleException.class, exception ->
                assertThat(exception.getProblemCode()).isEqualTo(ProblemCode.PORTFOLIO_STATUS_NOT_APPLICABLE));
    }

    static Stream<Arguments> initiativeMatrix() {
        return List.of(PortfolioStatus.PRESENTED, PortfolioStatus.INITIATIVE_APPROVED,
            PortfolioStatus.INITIATIVE_ARCHIVED, PortfolioStatus.NOT_ADMISSIBLE).stream()
            .flatMap(source -> Arrays.stream(PortfolioStatus.values())
                .map(target -> Arguments.of(source, target, initiativeAllowed(source, target))));
    }

    static Stream<Arguments> projectMatrix() {
        return List.of(PortfolioStatus.PROJECT_IN_PROGRESS, PortfolioStatus.PRODUCT_APPROVED,
            PortfolioStatus.PRODUCT_NOT_APPROVED, PortfolioStatus.SUSPENDED, PortfolioStatus.CANCELLED,
            PortfolioStatus.FINISHED).stream()
            .flatMap(source -> Arrays.stream(PortfolioStatus.values())
                .map(target -> Arguments.of(source, target, projectAllowed(source, target))));
    }

    private static boolean initiativeAllowed(PortfolioStatus source, PortfolioStatus target) {
        return (source == PortfolioStatus.PRESENTED
                && (target == PortfolioStatus.INITIATIVE_APPROVED
                    || target == PortfolioStatus.INITIATIVE_ARCHIVED
                    || target == PortfolioStatus.NOT_ADMISSIBLE))
            || (source == PortfolioStatus.INITIATIVE_APPROVED
                && target == PortfolioStatus.INITIATIVE_ARCHIVED);
    }

    private static boolean projectAllowed(PortfolioStatus source, PortfolioStatus target) {
        return switch (source) {
            case PROJECT_IN_PROGRESS -> target == PortfolioStatus.PRODUCT_APPROVED
                || target == PortfolioStatus.PRODUCT_NOT_APPROVED
                || target == PortfolioStatus.SUSPENDED
                || target == PortfolioStatus.CANCELLED;
            case SUSPENDED -> target == PortfolioStatus.PROJECT_IN_PROGRESS
                || target == PortfolioStatus.CANCELLED;
            case PRODUCT_NOT_APPROVED -> target == PortfolioStatus.PROJECT_IN_PROGRESS
                || target == PortfolioStatus.CANCELLED;
            case PRODUCT_APPROVED -> target == PortfolioStatus.FINISHED;
            default -> false;
        };
    }

    private PortfolioRecordEntity initiativeAt(PortfolioStatus source) {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE", "Unidad");
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences().initiative("I-MATRIX", unit, "Iniciativa");
        ReflectionTestUtils.setField(initiative, "status", source);
        return initiative;
    }

    private PortfolioRecordEntity projectAt(PortfolioStatus source) {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE", "Unidad");
        PortfolioRecordEntity project = PortfolioRecordTestBuilder.transientReferences().preexistingProject("P-MATRIX", unit, "Proyecto");
        ReflectionTestUtils.setField(project, "status", source);
        return project;
    }

    private PortfolioStatusCatalogEntity catalog(PortfolioStatus code, PortfolioStatusApplicability applicability, boolean active) {
        return new PortfolioStatusCatalogEntity(code, code.label(), code.ordinal() + 1, active, applicability);
    }
}
