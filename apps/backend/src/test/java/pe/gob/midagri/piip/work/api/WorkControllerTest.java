package pe.gob.midagri.piip.work.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.domain.TaskPriority;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.domain.TaskType;
import pe.gob.midagri.piip.work.persistence.WorkTaskEntity;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class WorkControllerTest {
    @Mock WorkTaskRepository tasks;
    @Mock UserRepository users;
    @Mock UserRoleScopeRepository scopes;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;
    @InjectMocks WorkController controller;

    @Test
    void pendingTasksOnlyIncludeUnitsCoveredByAnAdministratorGrant() {
        UserEntity assigned = new UserEntity("subject", "Persona", "persona@example.test");
        WorkTaskEntity consultationTask = task(1L, assigned, "INI-UE1", 10L, 100L);
        WorkTaskEntity administratorTask = task(2L, assigned, "INI-UE2", 20L, 200L);
        LocalAccessContext actor = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
                new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, 200L)));
        when(authorization.require(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(actor);
        when(tasks.findByAssignedUserIdAndStatusOrderByDueDateAsc(1L, TaskStatus.PENDING))
            .thenReturn(List.of(consultationTask, administratorTask));

        assertThat(controller.pending()).extracting(WorkController.TaskResponse::recordCode).containsExactly("INI-UE2");
    }

    private WorkTaskEntity task(Long id, UserEntity assigned, String code, Long institutionId, Long unitId) {
        InstitutionEntity institution = new InstitutionEntity("INST-" + institutionId, "Institución");
        ReflectionTestUtils.setField(institution, "id", institutionId);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-" + unitId, "Unidad Ejecutora");
        ReflectionTestUtils.setField(unit, "id", unitId);
        PortfolioRecordEntity record = PortfolioRecordTestBuilder.transientReferences().initiative(code, unit, "Iniciativa");
        WorkTaskEntity task = new WorkTaskEntity(record, TaskType.REGISTER_DECISION, "Decidir", assigned,
            TaskPriority.HIGH, LocalDate.now().plusDays(1), "TEST");
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
