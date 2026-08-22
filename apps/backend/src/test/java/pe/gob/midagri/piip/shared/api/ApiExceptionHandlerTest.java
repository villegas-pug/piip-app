package pe.gob.midagri.piip.shared.api;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void translatesTypedBusinessRulesWithoutTreatingTechnicalIllegalStateAsBusiness() {
        var detail = handler.business(new BusinessRuleException("regla"));
        assertThat(detail.getStatus()).isEqualTo(422);
        assertThat(detail.getTitle()).isEqualTo("Regla de negocio");
    }

    @Test
    void preservesInvalidReferenceProperties() {
        var detail = handler.invalidReference(new InvalidReferenceException("inválida", "field", 7L, "INACTIVE"));
        assertThat(detail.getProperties()).containsEntry("referenceField", "field")
            .containsEntry("referenceId", 7L).containsEntry("reason", "INACTIVE");
    }
}
