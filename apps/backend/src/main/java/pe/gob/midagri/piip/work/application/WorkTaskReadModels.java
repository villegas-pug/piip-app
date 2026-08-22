package pe.gob.midagri.piip.work.application;

import java.time.LocalDate;
import pe.gob.midagri.piip.work.domain.TaskPriority;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.domain.TaskType;

public final class WorkTaskReadModels {
    private WorkTaskReadModels() {}
    public record TaskView(Long id, String recordCode, TaskType type, String description, String assignedTo,
            TaskPriority priority, TaskStatus status, LocalDate dueDate, String alert, long version) {}
}
