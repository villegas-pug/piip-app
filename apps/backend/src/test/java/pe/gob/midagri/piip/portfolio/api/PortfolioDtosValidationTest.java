package pe.gob.midagri.piip.portfolio.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.InitiativeCreateRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.InitiativeUpdateRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ResponsibleUnitInput;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;

class PortfolioDtosValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsMoreThanOneResponsibleUnitOnCreate() {
        var request = new InitiativeCreateRequest(1L, "Iniciativa", 2L, 3L,
            LocalDate.of(2026, 8, 22), "Responsable", null, null, "Descripción", null,
            DigitalComponent.NO, List.of(new ResponsibleUnitInput(8L), new ResponsibleUnitInput(9L)));

        assertThat(validator.validate(request)).anyMatch(violation ->
            violation.getPropertyPath().toString().equals("responsibleUnits"));
    }

    @Test
    void rejectsMoreThanOneResponsibleUnitOnPatchWhenFieldIsPresent() {
        var request = new InitiativeUpdateRequest();
        request.setVersion(0L);
        request.setResponsibleUnits(List.of(new ResponsibleUnitInput(8L), new ResponsibleUnitInput(9L)));

        assertThat(validator.validate(request)).anyMatch(violation ->
            violation.getPropertyPath().toString().equals("responsibleUnits"));
    }
}
