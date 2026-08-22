package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioRecordResponse;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;

@ExtendWith(MockitoExtension.class)
class PortfolioQueryServiceTest {
    @Mock pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository records;
    @Mock PortfolioApplicationSupport support;
    @Mock PortfolioReadModelAssembler assembler;
    @Mock LocalAuthorizationService authorization;

    private PortfolioQueryService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioQueryService(records, support, assembler);
        when(support.authorization()).thenReturn(authorization);
        when(authorization.requireAuthenticatedRole()).thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        when(support.parseStatus("Presentado")).thenReturn(PortfolioStatus.PRESENTED);
        when(support.scopeSpecification(any())).thenReturn((root, query, builder) -> builder.conjunction());
        when(records.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));
    }

    @Test
    void preservesPaginationAndAppliesTheVisibilityScope() {
        var result = service.list(RecordType.PROJECT, "x", "Presentado", 100L, 1, 10, "updatedAt", "asc");

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
    }
}
