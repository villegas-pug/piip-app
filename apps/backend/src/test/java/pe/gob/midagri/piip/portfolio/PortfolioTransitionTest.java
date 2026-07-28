package pe.gob.midagri.piip.portfolio;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import java.time.LocalDate;
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
}
