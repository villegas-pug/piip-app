package pe.gob.midagri.piip.work.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.work.domain.*;
import java.time.LocalDate;
import java.util.*;

public interface WorkTaskRepository extends JpaRepository<WorkTaskEntity, Long> {
    List<WorkTaskEntity> findByAssignedUserIdAndStatusOrderByDueDateAsc(Long userId, TaskStatus status);
    Optional<WorkTaskEntity> findFirstByRecordIdAndTypeAndStatus(Long recordId, TaskType type, TaskStatus status);
    long countByStatusAndDueDateLessThanEqual(TaskStatus status, LocalDate limit);
}
