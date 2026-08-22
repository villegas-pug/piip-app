package pe.gob.midagri.piip.dashboard.api;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.midagri.piip.dashboard.application.DashboardPortfolioService;
import pe.gob.midagri.piip.dashboard.application.DashboardSummaryReadModel;
import pe.gob.midagri.piip.dashboard.application.DashboardSummaryService;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final DashboardSummaryService summaryService;
    private final DashboardPortfolioService portfolioService;

    @Autowired
    public DashboardController(DashboardSummaryService summaryService, DashboardPortfolioService portfolioService) {
        this.summaryService = summaryService;
        this.portfolioService = portfolioService;
    }

    /** Constructor de compatibilidad para pruebas unitarias existentes. */
    public DashboardController(PortfolioRecordRepository records, WorkTaskRepository tasks,
            NotificationRepository notifications, LocalAuthorizationService authorization,
            DashboardPortfolioService portfolioService) {
        this(new DashboardSummaryService(records, tasks, notifications, authorization), portfolioService);
    }

    public DashboardController(PortfolioRecordRepository records, WorkTaskRepository tasks,
            NotificationRepository notifications, LocalAuthorizationService authorization) {
        this(records, tasks, notifications, authorization, null);
    }

    @GetMapping
    public DashboardResponse summary() {
        DashboardSummaryReadModel value = summaryService.summary();
        return new DashboardResponse(value.initiatives(), value.projects(), value.alerts(), value.pendingTasks(),
            value.notifications(), value.portfolioByStatus());
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página global y conteos reconciliados del portafolio."),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "La Unidad Ejecutora está fuera del alcance legible.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/portfolio", produces = MediaType.APPLICATION_JSON_VALUE)
    public DashboardDtos.HomePortfolioResponse portfolio(
            @RequestParam("executingUnitId") Long executingUnitId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "type", required = false) RecordType type,
            @RequestParam(value = "status", required = false) PortfolioStatus status,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "5") @Min(1) @Max(100) int size) {
        return portfolioService.portfolio(executingUnitId, q, type, status, page, size);
    }

    public record DashboardResponse(long initiatives, long projects, long alerts, long pendingTasks,
            long notifications, Map<String, Long> portfolioByStatus) {}
}
