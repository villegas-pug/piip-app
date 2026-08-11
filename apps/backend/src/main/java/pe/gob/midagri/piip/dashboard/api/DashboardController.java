package pe.gob.midagri.piip.dashboard.api;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final PortfolioRecordRepository records; private final WorkTaskRepository tasks;
    private final NotificationRepository notifications; private final LocalAuthorizationService authorization;
    public DashboardController(PortfolioRecordRepository records, WorkTaskRepository tasks, NotificationRepository notifications,
            LocalAuthorizationService authorization) {
        this.records = records; this.tasks = tasks; this.notifications = notifications; this.authorization = authorization;
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

    public record DashboardResponse(long initiatives, long projects, long alerts, long pendingTasks,
            long notifications, Map<String, Long> portfolioByStatus) {}
}
