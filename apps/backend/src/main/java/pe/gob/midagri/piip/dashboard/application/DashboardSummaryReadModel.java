package pe.gob.midagri.piip.dashboard.application;

import java.util.List;
import pe.gob.midagri.piip.dashboard.api.DashboardDtos.PortfolioStatusCountResponse;

public record DashboardSummaryReadModel(long initiatives, long projects, long alerts, long pendingTasks,
        long notifications, List<PortfolioStatusCountResponse> portfolioStatusCounts) {
    public DashboardSummaryReadModel {
        portfolioStatusCounts = List.copyOf(portfolioStatusCounts);
    }
}
