package pe.gob.midagri.piip.work.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import pe.gob.midagri.piip.work.persistence.WorkTaskEntity;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class WorkTaskServiceTest {
    @Mock WorkTaskRepository tasks;
    @Mock UserRepository users;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;

    @Test
    void rejectsStaleCompletionBeforeChangingTheTask() {
        WorkTaskEntity task = mock(WorkTaskEntity.class);
        when(task.getVersion()).thenReturn(2L);
        PortfolioRecordEntity record = mock(PortfolioRecordEntity.class);
        ExecutingUnitEntity unit = mock(ExecutingUnitEntity.class);
        when(task.getRecord()).thenReturn(record);
        when(record.getExecutingUnit()).thenReturn(unit);
        when(unit.getId()).thenReturn(100L);
        when(authorization.requireUnit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(100L)))
            .thenReturn(new LocalAccessContext(1L, "actor", java.util.Set.of()));
        when(tasks.findById(1L)).thenReturn(Optional.of(task));

        WorkTaskService service = new WorkTaskService(tasks, users, authorization, audit);

        assertThatThrownBy(() -> service.complete(1L, 1L)).isInstanceOf(StaleVersionException.class);
    }
}
