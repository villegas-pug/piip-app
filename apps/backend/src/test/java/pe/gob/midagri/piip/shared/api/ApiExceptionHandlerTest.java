package pe.gob.midagri.piip.shared.api;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.shared.application.error.ProblemCode;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void translatesTypedBusinessRulesWithoutTreatingTechnicalIllegalStateAsBusiness() {
        var detail = handler.business(new BusinessRuleException("regla"));
        assertThat(detail.getStatus()).isEqualTo(422);
        assertThat(detail.getTitle()).isEqualTo("Regla de negocio");
        assertThat(detail.getProperties()).containsEntry("problemCode", "BUSINESS_RULE_VIOLATION");
    }

    @Test
    void publishesTheStableCodeAndSafeReasonOnTheRequestForControlledFailures() {
        var request = new MockHttpServletRequest();
        var detail = handler.business(new BusinessRuleException(ProblemCode.LAST_ACTIVE_ADMIN, "cobertura"), request);

        assertThat(detail.getProperties()).containsEntry("problemCode", "LAST_ACTIVE_ADMIN");
        assertThat(request.getAttribute(ApiExceptionHandler.SAFE_REASON_ATTRIBUTE)).isEqualTo("LAST_ACTIVE_ADMIN");
    }

    @Test
    void preservesInvalidReferenceProperties() {
        var detail = handler.invalidReference(new InvalidReferenceException("inválida", "field", 7L, "INACTIVE"));
        assertThat(detail.getProperties()).containsEntry("referenceField", "field")
            .containsEntry("referenceId", 7L).containsEntry("reason", "INACTIVE")
            .containsEntry("problemCode", "INVALID_ACTIVE_REFERENCE");
    }
}
