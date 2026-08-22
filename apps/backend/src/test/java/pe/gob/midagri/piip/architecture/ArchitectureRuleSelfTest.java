package pe.gob.midagri.piip.architecture;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureRuleSelfTest {
    @Test
    void ruleMessagesIdentifyTheBoundaryBeingProtected() {
        assertThat("ControllerLayeringTest: @Transactional/repository/entity authorization boundary")
            .contains("ControllerLayeringTest", "boundary");
        assertThat("ApplicationBoundaryTest: persistence types must not cross into API")
            .contains("ApplicationBoundaryTest", "API");
        assertThat("SharedModelOwnershipTest: every shared model has a modular owner")
            .contains("SharedModelOwnershipTest", "owner");
    }
}
