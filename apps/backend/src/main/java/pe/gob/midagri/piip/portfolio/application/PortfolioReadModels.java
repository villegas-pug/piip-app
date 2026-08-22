package pe.gob.midagri.piip.portfolio.application;

import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioRecordResponse;
import pe.gob.midagri.piip.shared.api.PageResponse;

public final class PortfolioReadModels {
    private PortfolioReadModels() {}
    public record PortfolioPageView(PageResponse<PortfolioRecordResponse> page) {}
}
