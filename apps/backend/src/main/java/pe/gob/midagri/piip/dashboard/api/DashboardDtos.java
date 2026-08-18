package pe.gob.midagri.piip.dashboard.api;

import java.time.Instant;
import java.util.List;

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
        String status,
        Long executingUnitId,
        String executingUnit,
        Instant updatedAt) {}

    public record PortfolioStatusCountResponse(String status, long count) {}
}
