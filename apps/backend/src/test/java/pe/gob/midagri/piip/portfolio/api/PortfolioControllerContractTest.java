package pe.gob.midagri.piip.portfolio.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.portfolio.application.InitiativeApplicationService;
import pe.gob.midagri.piip.portfolio.application.PortfolioQueryService;
import pe.gob.midagri.piip.portfolio.application.ProjectApplicationService;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.shared.api.PageResponse;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerContractTest {
    @Mock PortfolioQueryService queries;
    @Mock InitiativeApplicationService initiatives;
    @Mock ProjectApplicationService projects;

    @Test
    void preservesPageShapeAndDelegatesFilteringToApplication() {
        PageResponse<PortfolioDtos.PortfolioRecordResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0);
        when(queries.list(RecordType.INITIATIVE, "q", null, 100L, 0, 20, "updatedAt", "desc")).thenReturn(page);

        var result = new PortfolioController(queries, initiatives, projects)
            .initiatives("q", null, 100L, 0, 20);

        assertThat(result).isSameAs(page);
    }

    @Test
    void preservesSparsePatchPresenceAndExplicitNull() {
        PortfolioDtos.InitiativeUpdateRequest absent = new PortfolioDtos.InitiativeUpdateRequest();
        absent.setVersion(0L);
        absent.setName("Nombre actualizado");

        PortfolioDtos.InitiativeUpdateRequest explicitNull = new PortfolioDtos.InitiativeUpdateRequest();
        explicitNull.setVersion(0L);
        explicitNull.setPeiObjectiveId(null);

        assertThat(absent.presentProperties()).containsExactlyInAnyOrder("version", "name");
        assertThat(explicitNull.presentProperties()).containsExactlyInAnyOrder("version", "peiObjectiveId");
        assertThat(absent.has("description")).isFalse();
        assertThat(explicitNull.has("peiObjectiveId")).isTrue();
    }

    @Test
    void keepsPortfolioResponseVersionAndTechnicalIdentityInTheContract() {
        var response = new PortfolioDtos.PortfolioRecordResponse(
            new pe.gob.midagri.piip.catalogs.api.CatalogDtos.TechnicalCatalogItemResponse(
                "INITIATIVE", "Iniciativa", 0, true),
            "I-001-2026", "NA", "Iniciativa", null, null, null, null, null, null,
            List.of(), "Descripción", null, null, "Presentado", "No aplica", "No", null,
            null, null, null, null, null, 100L, "UE", null, 4L);

        assertThat(response.code()).isEqualTo("I-001-2026");
        assertThat(response.version()).isEqualTo(4L);
        assertThat(response.executingUnitId()).isEqualTo(100L);
        assertThat(response.responsibleUnits()).isEmpty();
    }
}
