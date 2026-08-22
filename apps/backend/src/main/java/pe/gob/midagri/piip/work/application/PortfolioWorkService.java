package pe.gob.midagri.piip.work.application;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.work.domain.TaskPriority;
import pe.gob.midagri.piip.work.domain.TaskType;
import pe.gob.midagri.piip.work.persistence.NotificationEntity;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskEntity;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@Service
public class PortfolioWorkService {
    private final WorkTaskRepository tasks;
    private final NotificationRepository notifications;
    private final AuditService audit;

    public PortfolioWorkService(WorkTaskRepository tasks, NotificationRepository notifications, AuditService audit) {
        this.tasks = tasks;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional
    public Long createDecisionTask(PortfolioRecordEntity record, UserEntity assignee, String actorSubject) {
        WorkTaskEntity task = tasks.save(new WorkTaskEntity(record, TaskType.REGISTER_DECISION,
            "Registrar decisión de la iniciativa", assignee, TaskPriority.HIGH, LocalDate.now().plusDays(20),
            "INICIATIVA_REGISTRADA"));
        notifications.save(new NotificationEntity(assignee, record, "TAREA_CREADA",
            "Tienes pendiente registrar la decisión de " + record.getCode()));
        audit.event("TAREA_CREADA", "TAREA_TRABAJO", String.valueOf(task.getId()),
            Map.of("registro", record.getCode()), actorSubject);
        return task.getId();
    }

    @Transactional
    public Long createDerivedProjectTask(PortfolioRecordEntity record, UserEntity assignee, String actorSubject) {
        WorkTaskEntity task = tasks.save(new WorkTaskEntity(record, TaskType.CREATE_DERIVED_PROJECT,
            "Crear proyecto derivado", assignee, TaskPriority.MEDIUM, null, "INICIATIVA_APROBADA"));
        notifications.save(new NotificationEntity(assignee, record, "TAREA_CREADA",
            "La iniciativa " + record.getCode() + " está aprobada y puede originar un proyecto"));
        audit.event("TAREA_CREADA", "TAREA_TRABAJO", String.valueOf(task.getId()),
            Map.of("registro", record.getCode()), actorSubject);
        return task.getId();
    }
}
