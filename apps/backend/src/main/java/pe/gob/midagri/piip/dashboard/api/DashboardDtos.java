package pe.gob.midagri.piip.dashboard.api;

import java.time.Instant;
import java.util.List;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioStatusReferenceResponse;

/** Contratos HTTP propios de la consulta de portafolio de Inicio. */
public final class DashboardDtos {
    private DashboardDtos() {}

    public record HomePortfolioResponse(
        List<HomePortfolioItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long executingUnitTotalElements,
        List<PortfolioStatusCountResponse> statusCounts) {}

    public record HomePortfolioItemResponse(
        String recordType,
        String code,
        String name,
        PortfolioStatusReferenceResponse status,
        Long executingUnitId,
        String executingUnit,
        Instant updatedAt) {}

    public record PortfolioStatusCountResponse(PortfolioStatusReferenceResponse status, long count) {}
}
