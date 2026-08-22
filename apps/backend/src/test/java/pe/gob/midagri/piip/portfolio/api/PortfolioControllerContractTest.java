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
}
