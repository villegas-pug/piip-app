package pe.gob.midagri.piip.dashboard.application;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.dashboard.api.DashboardDtos.HomePortfolioItemResponse;
import pe.gob.midagri.piip.dashboard.api.DashboardDtos.HomePortfolioResponse;
import pe.gob.midagri.piip.dashboard.api.DashboardDtos.PortfolioStatusCountResponse;
import pe.gob.midagri.piip.dashboard.persistence.DashboardPortfolioQueryRepository;
import pe.gob.midagri.piip.dashboard.persistence.DashboardPortfolioQueryRepository.QueryResult;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;

@Service
public class DashboardPortfolioService {
    private static final int MAX_SIZE = 100;
    private final DashboardPortfolioQueryRepository queries;
    private final LocalAuthorizationService authorization;

    public DashboardPortfolioService(DashboardPortfolioQueryRepository queries,
            LocalAuthorizationService authorization) {
        this.queries = queries;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public HomePortfolioResponse portfolio(Long executingUnitId, String query, RecordType type,
            PortfolioStatus status, int page, int size) {
        authorization.requireReadableUnit(executingUnitId);
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        QueryResult result = queries.find(executingUnitId, normalizedQuery, type, status,
            Math.max(page, 0), normalizeSize(size));
        List<HomePortfolioItemResponse> content = result.content().stream()
            .map(record -> new HomePortfolioItemResponse(record.getRecordType().label(), record.getCode(),
                record.getName(), record.getStatus().label(), record.getExecutingUnit().getId(),
                record.getExecutingUnit().getName(), record.getUpdatedAt()))
            .toList();
        List<PortfolioStatusCountResponse> statusCounts = Arrays.stream(PortfolioStatus.values())
            .filter(value -> result.statusCounts().getOrDefault(value, 0L) > 0)
            .map(value -> new PortfolioStatusCountResponse(value.label(), result.statusCounts().get(value)))
            .toList();
        return new HomePortfolioResponse(content, result.page(), result.size(), result.totalElements(),
            result.totalPages(), result.executingUnitTotalElements(), statusCounts);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
