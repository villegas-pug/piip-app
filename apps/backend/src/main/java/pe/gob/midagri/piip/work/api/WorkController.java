package pe.gob.midagri.piip.work.api;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.shared.api.*;
import pe.gob.midagri.piip.work.domain.*;
import pe.gob.midagri.piip.work.persistence.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/work-tasks")
public class WorkController {
    private final WorkTaskRepository tasks; private final UserRepository users; private final UserRoleScopeRepository scopes;
    private final LocalAuthorizationService authorization; private final AuditService audit;
    public WorkController(WorkTaskRepository tasks, UserRepository users, UserRoleScopeRepository scopes, LocalAuthorizationService authorization, AuditService audit) {
        this.tasks = tasks; this.users = users; this.scopes = scopes; this.authorization = authorization; this.audit = audit;
    }

    @GetMapping @Transactional(readOnly = true)
    public List<TaskResponse> pending() {
        LocalAccessContext actor = authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        return tasks.findByAssignedUserIdAndStatusOrderByDueDateAsc(actor.userId(), TaskStatus.PENDING).stream()
            .filter(task -> actor.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP,
                task.getRecord().getExecutingUnit().getId(), task.getRecord().getExecutingUnit().getInstitution().getId()))
            .map(this::response).toList();
    }

    @PutMapping("/{taskId}/complete") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void complete(@PathVariable("taskId") Long taskId, @RequestParam("version") long version) {
        WorkTaskEntity task = task(taskId); LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, task.getRecord().getExecutingUnit().getId());
        if (task.getVersion() != version) throw new StaleVersionException();
        task.complete(); audit.event("TAREA_COMPLETADA", "TAREA_TRABAJO", taskId.toString(), Map.of("registro", task.getRecord().getCode()), actor.subject());
    }

    @PutMapping("/{taskId}/assignee") @Transactional
    public TaskResponse reassign(@PathVariable("taskId") Long taskId, @RequestBody ReassignRequest request) {
        WorkTaskEntity task = task(taskId); LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, task.getRecord().getExecutingUnit().getId());
        if (task.getVersion() != request.version()) throw new StaleVersionException();
        UserEntity target = users.findByKeycloakSubject(request.userSubject()).orElseThrow(() -> new NotFoundException("Usuario inexistente"));
        boolean allowed = scopes.findActiveBySubject(target.getKeycloakSubject(), Instant.now()).stream().anyMatch(scope ->
            scope.getRole().getCode() == RoleCode.ADMINISTRADOR_PIIP
                && scope.getInstitution().getId().equals(task.getRecord().getExecutingUnit().getInstitution().getId())
                && (scope.getExecutingUnit() == null || scope.getExecutingUnit().getId().equals(task.getRecord().getExecutingUnit().getId())));
        if (!allowed) throw new BusinessRuleException("El usuario no es administrador del mismo ámbito");
        task.reassign(target); audit.event("TAREA_REASIGNADA", "TAREA_TRABAJO", taskId.toString(), Map.of("registro", task.getRecord().getCode(), "asignadoA", target.getKeycloakSubject()), actor.subject());
        return response(task);
    }

    private WorkTaskEntity task(Long id) { return tasks.findById(id).orElseThrow(() -> new NotFoundException("Tarea inexistente")); }
    private TaskResponse response(WorkTaskEntity task) { LocalDate today = LocalDate.now(); String alert = task.getDueDate() == null ? "SIN_PLAZO" : task.getDueDate().isBefore(today) ? "VENCIDA" : !task.getDueDate().isAfter(today.plusDays(3)) ? "PROXIMA" : "EN_PLAZO"; return new TaskResponse(task.getId(), task.getRecord().getCode(), task.getType(), task.getDescription(), task.getAssignedUser().getFullName(), task.getPriority(), task.getStatus(), task.getDueDate(), alert, task.getVersion()); }
    public record ReassignRequest(@NotNull String userSubject, @NotNull Long version) {}
    public record TaskResponse(Long id, String recordCode, TaskType type, String description, String assignedTo, TaskPriority priority, TaskStatus status, LocalDate dueDate, String alert, long version) {}
}
