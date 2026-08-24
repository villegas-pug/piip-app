package pe.gob.midagri.piip.shared.application.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemCodeTest {
    @Test
    void publishesTheStableProblemCodeVocabulary() {
        assertThat(ProblemCode.values()).extracting(Enum::name).containsExactly(
            "INVALID_REQUEST", "FORBIDDEN_SCOPE", "RESOURCE_NOT_FOUND", "STALE_VERSION",
            "ACTIVE_ASSIGNMENT_DUPLICATE", "SELF_ADMIN_SUSPENSION", "LAST_ACTIVE_ADMIN",
            "INCOMPATIBLE_ASSIGNMENT_STATE", "INVALID_ACTIVE_REFERENCE", "BUSINESS_RULE_VIOLATION");
    }

    @Test
    void preservesCompatibleExceptionConstructorsAndDiscriminators() {
        assertThat(new BusinessRuleException("x").getProblemCode()).isEqualTo(ProblemCode.BUSINESS_RULE_VIOLATION);
        assertThat(new NotFoundException("x").getProblemCode()).isEqualTo(ProblemCode.RESOURCE_NOT_FOUND);
        assertThat(new StaleVersionException().getProblemCode()).isEqualTo(ProblemCode.STALE_VERSION);
        assertThat(new InvalidReferenceException("x", "field", 1L, "INACTIVE").getProblemCode())
            .isEqualTo(ProblemCode.INVALID_ACTIVE_REFERENCE);
    }
}
