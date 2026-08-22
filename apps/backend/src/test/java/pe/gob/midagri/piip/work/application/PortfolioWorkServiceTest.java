package pe.gob.midagri.piip.work.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskEntity;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioWorkServiceTest {
    @Mock WorkTaskRepository tasks;
    @Mock NotificationRepository notifications;
    @Mock AuditService audit;

    @Test
    void createsTaskNotificationAndAuditInTheSameUseCase() {
        WorkTaskEntity task = org.mockito.Mockito.mock(WorkTaskEntity.class);
        when(tasks.save(any(WorkTaskEntity.class))).thenReturn(task);
        PortfolioRecordEntity record = org.mockito.Mockito.mock(PortfolioRecordEntity.class);
        UserEntity user = org.mockito.Mockito.mock(UserEntity.class);
        when(record.getCode()).thenReturn("INI-001");

        new PortfolioWorkService(tasks, notifications, audit).createDecisionTask(record, user, "actor");

        verify(tasks).save(any(WorkTaskEntity.class));
        verify(notifications).save(any());
        verify(audit).event(org.mockito.ArgumentMatchers.eq("TAREA_CREADA"),
            org.mockito.ArgumentMatchers.eq("TAREA_TRABAJO"), any(), any(), org.mockito.ArgumentMatchers.eq("actor"));
    }
}
