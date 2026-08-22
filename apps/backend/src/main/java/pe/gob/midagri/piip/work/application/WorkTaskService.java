package pe.gob.midagri.piip.work.application;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.persistence.WorkTaskEntity;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

import static pe.gob.midagri.piip.work.application.WorkTaskReadModels.TaskView;

@Service
public class WorkTaskService {
    private final WorkTaskRepository tasks;
    private final UserRepository users;
    private final LocalAuthorizationService authorization;
    private final AuditService audit;

    public WorkTaskService(WorkTaskRepository tasks, UserRepository users,
            LocalAuthorizationService authorization, AuditService audit) {
        this.tasks = tasks;
        this.users = users;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TaskView> pending() {
        LocalAccessContext actor = authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        return tasks.findByAssignedUserIdAndStatusOrderByDueDateAsc(actor.userId(), TaskStatus.PENDING).stream()
            .filter(task -> actor.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP,
                task.getRecord().getExecutingUnit().getId(), task.getRecord().getExecutingUnit().getInstitution().getId()))
            .map(this::view).toList();
    }

    @Transactional
    public void complete(Long taskId, long expectedVersion) {
        WorkTaskEntity task = task(taskId);
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP,
            task.getRecord().getExecutingUnit().getId());
        if (task.getVersion() != expectedVersion) throw new StaleVersionException();
        task.complete();
        audit.event("TAREA_COMPLETADA", "TAREA_TRABAJO", taskId.toString(),
            java.util.Map.of("registro", task.getRecord().getCode()), actor.subject());
    }

    @Transactional
    public TaskView reassign(Long taskId, String userSubject, long expectedVersion) {
        WorkTaskEntity task = task(taskId);
        LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP,
            task.getRecord().getExecutingUnit().getId());
        if (task.getVersion() != expectedVersion) throw new StaleVersionException();
        var target = users.findByKeycloakSubject(userSubject)
            .orElseThrow(() -> new NotFoundException("Usuario inexistente"));
        Long institutionId = task.getRecord().getExecutingUnit().getInstitution().getId();
        Long unitId = task.getRecord().getExecutingUnit().getId();
        authorization.requireReassignmentEligible(target.getKeycloakSubject(), institutionId, unitId);
        try {
            task.reassign(target);
        } catch (IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        audit.event("TAREA_REASIGNADA", "TAREA_TRABAJO", taskId.toString(),
            java.util.Map.of("registro", task.getRecord().getCode(), "asignadoA", target.getKeycloakSubject()), actor.subject());
        return view(task);
    }

    private WorkTaskEntity task(Long id) {
        return tasks.findById(id).orElseThrow(() -> new NotFoundException("Tarea inexistente"));
    }

    private TaskView view(WorkTaskEntity task) {
        LocalDate today = LocalDate.now();
        String alert = task.getDueDate() == null ? "SIN_PLAZO"
            : task.getDueDate().isBefore(today) ? "VENCIDA"
            : !task.getDueDate().isAfter(today.plusDays(3)) ? "PROXIMA" : "EN_PLAZO";
        return new TaskView(task.getId(), task.getRecord().getCode(), task.getType(), task.getDescription(),
            task.getAssignedUser().getFullName(), task.getPriority(), task.getStatus(), task.getDueDate(), alert,
            task.getVersion());
    }
}
