package pe.gob.midagri.piip.dashboard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.AccessDeniedException;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.dashboard.application.DashboardPortfolioService;
import pe.gob.midagri.piip.dashboard.api.DashboardDtos.HomePortfolioResponse;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.work.domain.TaskPriority;
import pe.gob.midagri.piip.work.domain.TaskStatus;
import pe.gob.midagri.piip.work.domain.TaskType;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskEntity;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {
    @Mock PortfolioRecordRepository records;
    @Mock WorkTaskRepository tasks;
    @Mock NotificationRepository notifications;
    @Mock LocalAuthorizationService authorization;
    @Mock DashboardPortfolioService portfolioService;
    @InjectMocks DashboardController controller;

    @Test
    void dashboardDoesNotCountAdministrativeTasksFromConsultationOnlyUnits() {
        UserEntity assigned = new UserEntity("subject", "Persona", "persona@example.test");
        WorkTaskEntity consultationTask = task(1L, assigned, "INI-UE1", 10L, 100L);
        WorkTaskEntity administratorTask = task(2L, assigned, "INI-UE2", 20L, 200L);
        LocalAccessContext actor = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
                new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, 200L)));
        when(authorization.requireAuthenticatedRole()).thenReturn(actor);
        when(records.findAll()).thenReturn(List.of(consultationTask.getRecord(), administratorTask.getRecord()));
        when(tasks.findByAssignedUserIdAndStatusOrderByDueDateAsc(1L, TaskStatus.PENDING))
            .thenReturn(List.of(consultationTask, administratorTask));
        when(notifications.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        DashboardController.DashboardResponse response = controller.summary();

        assertThat(response.initiatives()).isEqualTo(2L);
        assertThat(response.pendingTasks()).isEqualTo(1L);
    }

    @Test
    void portfolioEndpointDelegatesAllPublicFiltersToTheApplicationService() {
        HomePortfolioResponse expected = new HomePortfolioResponse(List.of(), 0, 5, 0, 0, 0, List.of());
        when(portfolioService.portfolio(10L, "q", RecordType.INITIATIVE, PortfolioStatus.PRESENTED, 2, 5))
            .thenReturn(expected);

        DashboardDtos.HomePortfolioResponse response = controller.portfolio(10L, "q", RecordType.INITIATIVE,
            PortfolioStatus.PRESENTED, 2, 5);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void portfolioEndpointDeclaresRequiredDefaultsAndPassesOptionalFilters() throws NoSuchMethodException {
        var method = DashboardController.class.getDeclaredMethod("portfolio", Long.class, String.class,
            RecordType.class, PortfolioStatus.class, int.class, int.class);
        var parameters = method.getParameters();
        assertThat(parameters[0].getAnnotation(RequestParam.class).value()).isEqualTo("executingUnitId");
        assertThat(parameters[1].getAnnotation(RequestParam.class).value()).isEqualTo("q");
        assertThat(parameters[2].getAnnotation(RequestParam.class).value()).isEqualTo("type");
        assertThat(parameters[3].getAnnotation(RequestParam.class).value()).isEqualTo("status");
        assertThat(parameters[1].getAnnotation(RequestParam.class).required()).isFalse();
        assertThat(parameters[4].getAnnotation(RequestParam.class).defaultValue()).isEqualTo("0");
        assertThat(parameters[5].getAnnotation(RequestParam.class).defaultValue()).isEqualTo("5");

        HomePortfolioResponse expected = new HomePortfolioResponse(List.of(), 0, 1, 0, 0, 0, List.of());
        when(portfolioService.portfolio(10L, null, null, null, -2, 101)).thenReturn(expected);

        assertThat(controller.portfolio(10L, null, null, null, -2, 101)).isSameAs(expected);
    }

    @Test
    void portfolioEndpointPropagatesUnauthorizedUnitAccess() {
        when(portfolioService.portfolio(99L, null, null, null, 0, 5))
            .thenThrow(new AccessDeniedException("fuera del ámbito autorizado"));

        assertThatThrownBy(() -> controller.portfolio(99L, null, null, null, 0, 5))
            .isInstanceOf(AccessDeniedException.class);
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
