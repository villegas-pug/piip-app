package pe.gob.midagri.piip.work.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import pe.gob.midagri.piip.work.application.WorkTaskReadModels.TaskView;
import pe.gob.midagri.piip.work.application.WorkTaskService;
import pe.gob.midagri.piip.work.domain.TaskPriority;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.domain.TaskType;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@RestController
@RequestMapping("/work-tasks")
public class WorkController {
    private final WorkTaskService service;

    @Autowired
    public WorkController(WorkTaskService service) { this.service = service; }

    /** Constructor de compatibilidad para pruebas unitarias existentes. */
    public WorkController(WorkTaskRepository tasks, UserRepository users, UserRoleScopeRepository ignoredScopes,
            LocalAuthorizationService authorization, AuditService audit) {
        this(new WorkTaskService(tasks, users, authorization, audit));
    }

    @GetMapping
    public List<TaskResponse> pending() { return service.pending().stream().map(WorkController::response).toList(); }

    @PutMapping("/{taskId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable("taskId") Long taskId, @RequestParam("version") long version) {
        service.complete(taskId, version);
    }

    @PutMapping("/{taskId}/assignee")
    public TaskResponse reassign(@PathVariable("taskId") Long taskId, @RequestBody ReassignRequest request) {
        return response(service.reassign(taskId, request.userSubject(), request.version()));
    }

    private static TaskResponse response(TaskView value) {
        return new TaskResponse(value.id(), value.recordCode(), value.type(), value.description(), value.assignedTo(),
            value.priority(), value.status(), value.dueDate(), value.alert(), value.version());
    }

    public record ReassignRequest(@NotNull String userSubject, @NotNull Long version) {}
    public record TaskResponse(Long id, String recordCode, TaskType type, String description, String assignedTo,
            TaskPriority priority, TaskStatus status, LocalDate dueDate, String alert, long version) {}
}
