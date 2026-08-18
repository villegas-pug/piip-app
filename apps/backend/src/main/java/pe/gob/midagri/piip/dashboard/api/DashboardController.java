package pe.gob.midagri.piip.dashboard.api;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.dashboard.application.DashboardPortfolioService;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final PortfolioRecordRepository records; private final WorkTaskRepository tasks;
    private final NotificationRepository notifications; private final LocalAuthorizationService authorization;
    private final DashboardPortfolioService portfolioService;
    @Autowired
    public DashboardController(PortfolioRecordRepository records, WorkTaskRepository tasks, NotificationRepository notifications,
            LocalAuthorizationService authorization, DashboardPortfolioService portfolioService) {
        this.records = records; this.tasks = tasks; this.notifications = notifications; this.authorization = authorization;
        this.portfolioService = portfolioService;
    }
    public DashboardController(PortfolioRecordRepository records, WorkTaskRepository tasks, NotificationRepository notifications,
            LocalAuthorizationService authorization) {
        this(records, tasks, notifications, authorization, null);
    }
    @GetMapping @Transactional(readOnly = true)
    public DashboardResponse summary() {
        LocalAccessContext actor = authorization.requireAuthenticatedRole();
        List<PortfolioRecordEntity> visibleRecords = records.findAll().stream()
            .filter(item -> actor.coversExecutingUnit(item.getExecutingUnit().getId(), item.getExecutingUnit().getInstitution().getId())).toList();
        long initiatives = visibleRecords.stream().filter(item -> item.getRecordType() == RecordType.INITIATIVE).count();
        long projects = visibleRecords.stream().filter(item -> item.getRecordType() == RecordType.PROJECT).count();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        visibleRecords.forEach(item -> byStatus.merge(item.getStatus().label(), 1L, Long::sum));

        var pendingTasks = actor.hasRole(pe.gob.midagri.piip.identity.domain.RoleCode.ADMINISTRADOR_PIIP)
            ? tasks.findByAssignedUserIdAndStatusOrderByDueDateAsc(actor.userId(), TaskStatus.PENDING).stream()
                .filter(task -> actor.coversExecutingUnit(pe.gob.midagri.piip.identity.domain.RoleCode.ADMINISTRADOR_PIIP,
                    task.getRecord().getExecutingUnit().getId(), task.getRecord().getExecutingUnit().getInstitution().getId()))
                .toList()
            : List.<pe.gob.midagri.piip.work.persistence.WorkTaskEntity>of();
        long alerts = pendingTasks.stream().filter(task -> task.getDueDate() != null && !task.getDueDate().isAfter(LocalDate.now().plusDays(3))).count();
        long unread = notifications.findByRecipientIdOrderByCreatedAtDesc(actor.userId()).stream().filter(item -> !item.isRead()).count();
        return new DashboardResponse(initiatives, projects, alerts, pendingTasks.size(), unread, byStatus);
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
