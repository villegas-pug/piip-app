package pe.gob.midagri.piip.dashboard.application;

import java.util.Map;
import java.util.LinkedHashMap;

public record DashboardSummaryReadModel(long initiatives, long projects, long alerts, long pendingTasks,
        long notifications, Map<String, Long> portfolioByStatus) {
    public DashboardSummaryReadModel {
        portfolioByStatus = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(portfolioByStatus));
    }
}
