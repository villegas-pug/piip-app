package pe.gob.midagri.piip.dashboard.application;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@Service
public class DashboardSummaryService {
    private final PortfolioRecordRepository records;
    private final WorkTaskRepository tasks;
    private final NotificationRepository notifications;
    private final LocalAuthorizationService authorization;

    public DashboardSummaryService(PortfolioRecordRepository records, WorkTaskRepository tasks,
            NotificationRepository notifications, LocalAuthorizationService authorization) {
        this.records = records;
        this.tasks = tasks;
        this.notifications = notifications;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryReadModel summary() {
        LocalAccessContext actor = authorization.requireAuthenticatedRole();
        var visibleRecords = records.findAll().stream()
            .filter(item -> actor.coversExecutingUnit(item.getExecutingUnit().getId(),
                item.getExecutingUnit().getInstitution().getId())).toList();
        long initiatives = visibleRecords.stream().filter(item -> item.getRecordType().name().equals("INITIATIVE")).count();
        long projects = visibleRecords.stream().filter(item -> item.getRecordType().name().equals("PROJECT")).count();
        var byStatus = new LinkedHashMap<String, Long>();
        visibleRecords.forEach(item -> byStatus.merge(item.getStatus().label(), 1L, Long::sum));

        var pendingTasks = actor.hasRole(RoleCode.ADMINISTRADOR_PIIP)
            ? tasks.findByAssignedUserIdAndStatusOrderByDueDateAsc(actor.userId(), TaskStatus.PENDING).stream()
                .filter(task -> actor.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP,
                    task.getRecord().getExecutingUnit().getId(), task.getRecord().getExecutingUnit().getInstitution().getId()))
                .toList()
            : java.util.List.<pe.gob.midagri.piip.work.persistence.WorkTaskEntity>of();
        long alerts = pendingTasks.stream().filter(task -> task.getDueDate() != null
            && !task.getDueDate().isAfter(LocalDate.now().plusDays(3))).count();
        long unread = notifications.findByRecipientIdOrderByCreatedAtDesc(actor.userId()).stream()
            .filter(item -> !item.isRead()).count();
        return new DashboardSummaryReadModel(initiatives, projects, alerts, pendingTasks.size(), unread, byStatus);
    }
}
