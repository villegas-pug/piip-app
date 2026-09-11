package pe.gob.midagri.piip.dashboard.application;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.dashboard.api.DashboardDtos.HomePortfolioItemResponse;
import pe.gob.midagri.piip.dashboard.api.DashboardDtos.HomePortfolioResponse;
import pe.gob.midagri.piip.dashboard.api.DashboardDtos.PortfolioStatusCountResponse;
import pe.gob.midagri.piip.dashboard.persistence.DashboardPortfolioQueryRepository;
import pe.gob.midagri.piip.dashboard.persistence.DashboardPortfolioQueryRepository.QueryResult;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioStatusReferenceResponse;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;

@Service
public class DashboardPortfolioService {
    private static final int MAX_SIZE = 100;
    private final DashboardPortfolioQueryRepository queries;
    private final LocalAuthorizationService authorization;
    private final PortfolioStatusRepository statuses;

    public DashboardPortfolioService(DashboardPortfolioQueryRepository queries,
            LocalAuthorizationService authorization, PortfolioStatusRepository statuses) {
        this.queries = queries;
        this.authorization = authorization;
        this.statuses = statuses;
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
                record.getName(), statusReference(record), record.getExecutingUnit().getId(),
                record.getExecutingUnit().getName(), record.getUpdatedAt()))
            .toList();
        List<PortfolioStatusCountResponse> statusCounts = statuses.findAllByOrderByDisplayOrderAscCodeAsc().stream()
            .filter(catalog -> result.statusCounts().getOrDefault(catalog.getCode(), 0L) > 0)
            .map(catalog -> new PortfolioStatusCountResponse(statusReference(catalog),
                result.statusCounts().get(catalog.getCode())))
            .toList();
        return new HomePortfolioResponse(content, result.page(), result.size(), result.totalElements(),
            result.totalPages(), result.executingUnitTotalElements(), statusCounts);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    private static PortfolioStatusReferenceResponse statusReference(PortfolioRecordEntity record) {
        PortfolioStatusCatalogEntity catalog = record.getStatusCatalog();
        if (catalog == null) {
            return new PortfolioStatusReferenceResponse(record.getStatus().name(), record.getStatus().label(), true);
        }
        return statusReference(catalog);
    }

    private static PortfolioStatusReferenceResponse statusReference(PortfolioStatusCatalogEntity catalog) {
        return new PortfolioStatusReferenceResponse(catalog.getCode().name(), catalog.getName(), catalog.isActive());
    }
}
