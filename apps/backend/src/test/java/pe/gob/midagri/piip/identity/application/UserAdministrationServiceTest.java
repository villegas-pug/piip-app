package pe.gob.midagri.piip.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.UserAdministrationReadModels.User;
import pe.gob.midagri.piip.identity.application.UserAdministrationReadModels.UserAssignmentCandidate;
import pe.gob.midagri.piip.identity.application.UserAdministrationReadModels.AssignmentSnapshot;
import pe.gob.midagri.piip.identity.application.UserAdministrationCommands.AssignCommand;
import pe.gob.midagri.piip.identity.application.UserAdministrationCommands.UpdateCommand;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.ProblemCode;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock UserRoleScopeRepository scopes;
    @Mock InstitutionRepository institutions;
    @Mock ExecutingUnitRepository units;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;
    private UserAdministrationService service;
    private LocalAccessContext administrator;

    @BeforeEach
    void setUp() {
        service = new UserAdministrationService(users, roles, scopes, institutions, units, authorization, audit);
        administrator = new LocalAccessContext(1L, "admin-subject",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, 100L)));
        when(authorization.require(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(administrator);
        lenient().when(authorization.resolve("admin-subject")).thenReturn(administrator);
    }

    @Test
    void listsUsersAndSuspendedScopesWithoutExposingLegacyAccountState() {
        UserEntity user = user(2L, "managed-subject", false);
        UserRoleScopeEntity active = scope(20L, user, RoleCode.CONSULTA_EXTERNA, 10L);
        UserRoleScopeEntity suspended = scope(21L, user, RoleCode.CONSULTA_EXTERNA, 10L);
        suspended.suspend(Instant.now());
        when(scopes.findForAdministration(Set.of(10L))).thenReturn(List.of(active, suspended));

        List<User> response = service.list();

        assertThat(response).singleElement().satisfies(managed -> {
            assertThat(managed.scopes()).extracting(scope -> scope.active()).containsExactly(true, false);
        });
    }

    @Test
    void listsAssignmentsFromEveryExecutingUnitInAnAdministrableInstitution() {
        UserEntity user = user(2L, "managed-subject", true);
        UserRoleScopeEntity otherUnitScope = scope(20L, user, RoleCode.CONSULTA_EXTERNA, 10L, 101L);
        when(scopes.findForAdministration(Set.of(10L))).thenReturn(List.of(otherUnitScope));

        List<User> response = service.list();

        assertThat(response).singleElement().satisfies(managed ->
            assertThat(managed.scopes()).singleElement().satisfies(scope ->
                assertThat(scope.executingUnitId()).isEqualTo(101L)));
    }

    @Test
    void listsActiveExecutingUnitsAndInstitutionWideOptionForAdministrableInstitutions() {
        InstitutionEntity institution = institution(10L);
        ExecutingUnitEntity unit = unit(institution, 101L, true);
        when(institutions.findAllById(Set.of(10L))).thenReturn(List.of(institution));
        when(units.findByInstitutionIdAndActiveTrueOrderByName(10L)).thenReturn(List.of(unit));

        var response = service.listAdministrableScopes();

        assertThat(response).singleElement().satisfies(scope -> {
            assertThat(scope.institutionId()).isEqualTo(10L);
            assertThat(scope.institutionWideAllowed()).isTrue();
            assertThat(scope.executingUnits()).singleElement().satisfies(executingUnit ->
                assertThat(executingUnit.id()).isEqualTo(101L));
        });
    }

    @Test
    void omitsInactiveInstitutionsFromAdministrableScopes() {
        InstitutionEntity institution = institution(10L);
        ReflectionTestUtils.setField(institution, "active", false);
        when(institutions.findAllById(Set.of(10L))).thenReturn(List.of(institution));

        assertThat(service.listAdministrableScopes()).isEmpty();
        verifyNoInteractions(units);
    }

    @Test
    void listsUsersWithoutAssignmentHistoryForAdministrators() {
        UserEntity candidate = user(2L, "candidate-subject", false);
        when(users.findWithoutRoleScopeHistory()).thenReturn(List.of(candidate));

        List<UserAssignmentCandidate> response = service.listAssignmentCandidates();

        assertThat(response).containsExactly(new UserAssignmentCandidate(2L, "candidate-subject",
            "Persona administrable", "persona@example.test"));
        verify(authorization).require(RoleCode.ADMINISTRADOR_PIIP);
        verify(authorization).resolve("admin-subject");
        verifyNoInteractions(audit);
    }

    @Test
    void rejectsCandidateListingWhenTheActorIsNotAnAdministrator() {
        when(authorization.require(RoleCode.ADMINISTRADOR_PIIP))
            .thenThrow(new org.springframework.security.access.AccessDeniedException("Se requiere el rol ADMINISTRADOR_PIIP"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.listAssignmentCandidates())
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(users, never()).findWithoutRoleScopeHistory();
        verifyNoInteractions(audit);
    }

    @Test
    void rejectsSuspensionWhenTheScopeIsTheLastAdministrator() {
        UserEntity user = user(2L, "managed-subject", true);
        UserRoleScopeEntity scope = scope(20L, user, RoleCode.ADMINISTRADOR_PIIP, 10L);
        when(scopes.findById(20L)).thenReturn(Optional.of(scope));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(scope));
        when(scopes.findActiveAdministratorsForUpdate(eq(RoleCode.ADMINISTRADOR_PIIP), eq(10L), isNull(), any())).thenReturn(List.of(scope));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.suspend(20L, 0L))
            .isInstanceOf(BusinessRuleException.class);
        verify(audit, never()).event(anyString(), anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    void updatesTheSameAssignmentAndAuditsTheBeforeAndAfterValues() {
        UserEntity user = user(2L, "managed-subject", true);
        UserRoleScopeEntity scope = scope(20L, user, RoleCode.CONSULTA_EXTERNA, 10L);
        RoleEntity administratorRole = new RoleEntity(RoleCode.ADMINISTRADOR_PIIP, "Administrador PIIP");
        ReflectionTestUtils.setField(administratorRole, "id", 1L);
        InstitutionEntity institution = scope.getInstitution();
        when(scopes.findById(20L)).thenReturn(Optional.of(scope));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(scope));
        when(roles.findByCode(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(Optional.of(administratorRole));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(scopes.findActiveDuplicatesForUpdate(eq(2L), eq(1L), eq(10L), isNull(), any())).thenReturn(List.of());

        var response = service.update(20L, 0L, new UpdateCommand(RoleCode.ADMINISTRADOR_PIIP, 10L, null));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.role()).isEqualTo(RoleCode.ADMINISTRADOR_PIIP);
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<java.util.Map<String, Object>> detail = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(audit).event(eq("ROL_ACTUALIZADO"), eq("USUARIO_ROL_AMBITO"), eq("20"), detail.capture(), eq("admin-subject"));
        assertThat(detail.getValue()).containsKeys("actor", "action", "affectedUser", "before", "after", "result");
        assertThat(detail.getValue().get("actor")).isEqualTo("admin-subject");
        assertThat(detail.getValue().get("action")).isEqualTo("ROL_ACTUALIZADO");
        assertThat(detail.getValue().get("affectedUser")).isEqualTo("managed-subject");
        assertThat(detail.getValue().get("result")).isEqualTo("SUCCESS");
        assertThat(detail.getValue().get("before")).isInstanceOf(AssignmentSnapshot.class);
        assertThat(detail.getValue().get("after")).isInstanceOf(AssignmentSnapshot.class);
        verifyNoMoreInteractions(audit);
    }

    @Test
    void assignsAndEmitsExactlyOneCompleteFunctionalAuditEvent() {
        UserEntity managed = user(2L, "managed-subject", true);
        RoleEntity role = role(RoleCode.CONSULTA_EXTERNA);
        InstitutionEntity institution = institution(10L);
        when(roles.findByCode(RoleCode.CONSULTA_EXTERNA)).thenReturn(Optional.of(role));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(users.findByKeycloakSubject("managed-subject")).thenReturn(Optional.of(managed));
        when(users.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(user(1L, "admin-subject", true), managed));
        when(scopes.findActiveDuplicatesForUpdate(eq(2L), eq(2L), eq(10L), isNull(), any())).thenReturn(List.of());
        when(scopes.findLatestSuspendedExactForUpdate(2L, 2L, 10L, null)).thenReturn(Optional.empty());
        when(scopes.save(any(UserRoleScopeEntity.class))).thenAnswer(invocation -> {
            UserRoleScopeEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });

        var response = service.assign(new AssignCommand("managed-subject", RoleCode.CONSULTA_EXTERNA, 10L, null));

        @SuppressWarnings("unchecked")
        var detail = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(audit).event(eq("ROL_ASIGNADO"), eq("USUARIO_ROL_AMBITO"), eq("30"), detail.capture(), eq("admin-subject"));
        verifyNoMoreInteractions(audit);
        assertThat(response.id()).isEqualTo(30L);
        assertThat(detail.getValue()).containsKeys("actor", "action", "affectedUser", "before", "after", "result");
        assertThat(detail.getValue().get("before")).isNull();
        assertThat(detail.getValue().get("after")).isInstanceOf(AssignmentSnapshot.class);
        assertThat(detail.getValue().get("result")).isEqualTo("SUCCESS");
    }

    @Test
    void suspendsAndEmitsOneEventWithBeforeAndAfterSnapshots() {
        UserEntity managed = user(2L, "managed-subject", true);
        UserRoleScopeEntity scope = scope(20L, managed, RoleCode.CONSULTA_EXTERNA, 10L);
        when(scopes.findById(20L)).thenReturn(Optional.of(scope));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(scope));
        when(users.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(user(1L, "admin-subject", true), managed));

        service.suspend(20L, 0L);

        @SuppressWarnings("unchecked")
        var detail = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(audit).event(eq("ROL_SUSPENDIDO"), eq("USUARIO_ROL_AMBITO"), eq("20"), detail.capture(), eq("admin-subject"));
        verifyNoMoreInteractions(audit);
        assertThat(detail.getValue().get("before")).isInstanceOf(AssignmentSnapshot.class);
        assertThat(detail.getValue().get("after")).isInstanceOf(AssignmentSnapshot.class);
        assertThat(((AssignmentSnapshot) detail.getValue().get("after")).active()).isFalse();
    }

    @Test
    void rejectsMutationsWithoutFunctionalAuditEvents() {
        UserEntity managed = user(2L, "managed-subject", true);
        UserRoleScopeEntity scope = scope(20L, managed, RoleCode.CONSULTA_EXTERNA, 10L);
        ReflectionTestUtils.setField(scope, "version", 2L);
        when(scopes.findById(20L)).thenReturn(Optional.of(scope));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(scope));
        when(users.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(user(1L, "admin-subject", true), managed));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.update(20L, 1L,
            new UpdateCommand(RoleCode.CONSULTA_EXTERNA, 10L, null)))
            .isInstanceOf(StaleVersionException.class);

        verifyNoInteractions(audit);
    }

    @Test
    void propagatesAuditFailureFromMutationBoundary() {
        UserEntity managed = user(2L, "managed-subject", true);
        UserRoleScopeEntity scope = scope(20L, managed, RoleCode.CONSULTA_EXTERNA, 10L);
        when(scopes.findById(20L)).thenReturn(Optional.of(scope));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(scope));
        when(users.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(user(1L, "admin-subject", true), managed));
        doThrow(new IllegalStateException("audit unavailable"))
            .when(audit).event(anyString(), anyString(), anyString(), anyMap(), anyString());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.suspend(20L, 0L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("audit unavailable");

        verify(audit).event(eq("ROL_SUSPENDIDO"), eq("USUARIO_ROL_AMBITO"), eq("20"), anyMap(), eq("admin-subject"));
        verifyNoMoreInteractions(audit);
    }

    @Test
    void allowsSelfAssignmentToAnotherUnitOfTheAdministrableInstitutionAndAuditsIt() {
        InstitutionEntity institution = institution(10L);
        ExecutingUnitEntity otherUnit = unit(institution, 101L, true);
        RoleEntity role = role(RoleCode.CONSULTA_EXTERNA);
        UserEntity actorUser = user(1L, "admin-subject", true);
        when(roles.findByCode(RoleCode.CONSULTA_EXTERNA)).thenReturn(Optional.of(role));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(units.findById(101L)).thenReturn(Optional.of(otherUnit));
        when(users.findByKeycloakSubjectForUpdate("admin-subject")).thenReturn(Optional.of(actorUser));
        when(scopes.findActiveDuplicatesForUpdate(eq(1L), eq(2L), eq(10L), eq(101L), any())).thenReturn(List.of());
        when(scopes.save(any(UserRoleScopeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.assign(new AssignCommand(
            "admin-subject", RoleCode.CONSULTA_EXTERNA, 10L, 101L));

        assertThat(response.executingUnitId()).isEqualTo(101L);
        verify(users).findByKeycloakSubjectForUpdate("admin-subject");
        verify(audit).event(eq("ROL_ASIGNADO"), eq("USUARIO_ROL_AMBITO"), anyString(), anyMap(), eq("admin-subject"));
    }

    @Test
    void allowsInstitutionWideDestinationFromAnExecutingUnitAdministratorGrant() {
        InstitutionEntity institution = institution(10L);
        RoleEntity role = role(RoleCode.CONSULTA_EXTERNA);
        UserEntity user = user(2L, "managed-subject", true);
        when(roles.findByCode(RoleCode.CONSULTA_EXTERNA)).thenReturn(Optional.of(role));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(users.findByKeycloakSubjectForUpdate("managed-subject")).thenReturn(Optional.of(user));
        when(scopes.findActiveDuplicatesForUpdate(eq(2L), eq(2L), eq(10L), isNull(), any())).thenReturn(List.of());
        when(scopes.save(any(UserRoleScopeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.assign(new AssignCommand(
            "managed-subject", RoleCode.CONSULTA_EXTERNA, 10L, null));

        assertThat(response.executingUnitId()).isNull();
        assertThat(response.executingUnit()).isEqualTo("Todas");
    }

    @Test
    void preservesAuthoritativeDuplicateValidationForInstitutionWideSelfAssignment() {
        InstitutionEntity institution = institution(10L);
        RoleEntity role = role(RoleCode.ADMINISTRADOR_PIIP);
        UserEntity actorUser = user(1L, "admin-subject", true);
        UserRoleScopeEntity duplicate = scope(20L, actorUser, RoleCode.ADMINISTRADOR_PIIP, 10L);
        when(roles.findByCode(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(Optional.of(role));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(users.findByKeycloakSubjectForUpdate("admin-subject")).thenReturn(Optional.of(actorUser));
        when(scopes.findActiveDuplicatesForUpdate(eq(1L), eq(1L), eq(10L), isNull(), any()))
            .thenReturn(List.of(duplicate));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.assign(new AssignCommand(
                "admin-subject", RoleCode.ADMINISTRADOR_PIIP, 10L, null)))
            .isInstanceOf(BusinessRuleException.class);

        verify(scopes, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void rejectsDestinationFromAnotherInstitution() {
        InstitutionEntity institution = institution(20L);
        when(roles.findByCode(RoleCode.CONSULTA_EXTERNA)).thenReturn(Optional.of(role(RoleCode.CONSULTA_EXTERNA)));
        when(institutions.findById(20L)).thenReturn(Optional.of(institution));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.assign(new AssignCommand(
                "managed-subject", RoleCode.CONSULTA_EXTERNA, 20L, null)))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(users, never()).findByKeycloakSubjectForUpdate(anyString());
        verifyNoInteractions(audit);
    }

    @Test
    void rejectsInactiveExecutingUnitEvenInsideAnAdministrableInstitution() {
        InstitutionEntity institution = institution(10L);
        ExecutingUnitEntity inactiveUnit = unit(institution, 101L, false);
        when(roles.findByCode(RoleCode.CONSULTA_EXTERNA)).thenReturn(Optional.of(role(RoleCode.CONSULTA_EXTERNA)));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(units.findById(101L)).thenReturn(Optional.of(inactiveUnit));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.assign(new AssignCommand(
                "managed-subject", RoleCode.CONSULTA_EXTERNA, 10L, 101L)))
            .isInstanceOf(BusinessRuleException.class);

        verify(users, never()).findByKeycloakSubjectForUpdate(anyString());
    }

    @Test
    void rejectsStaleVersionBeforeUpdatingTheAssignment() {
        UserEntity user = user(2L, "managed-subject", true);
        UserRoleScopeEntity scope = scope(20L, user, RoleCode.CONSULTA_EXTERNA, 10L);
        ReflectionTestUtils.setField(scope, "version", 2L);
        when(scopes.findById(20L)).thenReturn(Optional.of(scope));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(scope));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.update(20L, 1L,
                new UpdateCommand(RoleCode.CONSULTA_EXTERNA, 10L, 101L)))
            .isInstanceOf(StaleVersionException.class);

        verifyNoInteractions(roles, institutions, units, audit);
    }

    @Test
    void consultationScopeDoesNotExpandAdministrativeListingCoverage() {
        LocalAccessContext mixed = new LocalAccessContext(1L, "admin-subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
                new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, 200L)));
        when(authorization.require(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(mixed);
        when(authorization.resolve("admin-subject")).thenReturn(mixed);
        when(scopes.findForAdministration(Set.of(20L))).thenReturn(List.of());

        assertThat(service.list()).isEmpty();
        verify(scopes).findForAdministration(Set.of(20L));
        verify(scopes, never()).findForAdministration(argThat(ids -> ids.contains(10L)));
    }

    @Test
    void reactivatesTheLatestSuspendedExactAssignmentWithoutCreatingANewRow() {
        InstitutionEntity institution = institution(10L);
        RoleEntity role = role(RoleCode.CONSULTA_EXTERNA);
        UserEntity managed = user(2L, "managed-subject", true);
        UserRoleScopeEntity suspended = scope(20L, managed, RoleCode.CONSULTA_EXTERNA, 10L);
        suspended.suspend(Instant.now().minusSeconds(30));
        when(roles.findByCode(RoleCode.CONSULTA_EXTERNA)).thenReturn(Optional.of(role));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(users.findByKeycloakSubjectForUpdate("managed-subject")).thenReturn(Optional.of(managed));
        when(scopes.findActiveDuplicatesForUpdate(eq(2L), eq(2L), eq(10L), isNull(), any())).thenReturn(List.of());
        when(scopes.findLatestSuspendedExactForUpdate(2L, 2L, 10L, null)).thenReturn(Optional.of(suspended));

        var result = service.assign(new AssignCommand("managed-subject", RoleCode.CONSULTA_EXTERNA, 10L, null));

        assertThat(result.status()).isEqualTo(UserAdministrationReadModels.AssignmentMutationStatus.REACTIVATED);
        assertThat(result.id()).isEqualTo(20L);
        verify(scopes, never()).save(any());
        @SuppressWarnings("unchecked")
        var detail = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(audit).event(eq("ROL_REACTIVADO"), eq("USUARIO_ROL_AMBITO"), eq("20"), detail.capture(), eq("admin-subject"));
        verifyNoMoreInteractions(audit);
        assertThat(detail.getValue()).containsKeys("actor", "action", "affectedUser", "before", "after", "result");
        assertThat(detail.getValue().get("before")).isInstanceOf(AssignmentSnapshot.class);
        assertThat(detail.getValue().get("after")).isInstanceOf(AssignmentSnapshot.class);
        assertThat(detail.getValue().get("result")).isEqualTo("SUCCESS");
    }

    @Test
    void rejectsSelfSuspensionOfTheAdministrativeGrantWithTheSpecificCode() {
        UserEntity actor = user(1L, "admin-subject", true);
        UserRoleScopeEntity ownGrant = scope(20L, actor, RoleCode.ADMINISTRADOR_PIIP, 10L);
        when(scopes.findById(20L)).thenReturn(Optional.of(ownGrant));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(ownGrant));

        var exception = org.assertj.core.api.Assertions.catchThrowable(() -> service.suspend(20L, 0L));

        assertThat(exception).isInstanceOf(BusinessRuleException.class);
        assertThat(((BusinessRuleException) exception).getProblemCode()).isEqualTo(ProblemCode.SELF_ADMIN_SUSPENSION);
        verifyNoInteractions(audit);
    }

    private UserEntity user(Long id, String subject, boolean active) {
        UserEntity user = new UserEntity(subject, "Persona administrable", "persona@example.test");
        ReflectionTestUtils.setField(user, "id", id);
        user.changeActiveState(active);
        return user;
    }

    private UserRoleScopeEntity scope(Long id, UserEntity user, RoleCode code, Long institutionId) {
        return scope(id, user, code, institutionId, null);
    }

    private UserRoleScopeEntity scope(Long id, UserEntity user, RoleCode code, Long institutionId, Long unitId) {
        InstitutionEntity institution = institution(institutionId);
        RoleEntity role = role(code);
        ExecutingUnitEntity unit = unitId == null ? null : unit(institution, unitId, true);
        UserRoleScopeEntity scope = new UserRoleScopeEntity(user, role, institution, unit, "admin-subject");
        ReflectionTestUtils.setField(scope, "id", id);
        return scope;
    }

    private InstitutionEntity institution(Long id) {
        InstitutionEntity institution = new InstitutionEntity("INST-" + id, "Institución " + id);
        ReflectionTestUtils.setField(institution, "id", id);
        return institution;
    }

    private ExecutingUnitEntity unit(InstitutionEntity institution, Long id, boolean active) {
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-" + id, "Unidad Ejecutora " + id);
        ReflectionTestUtils.setField(unit, "id", id);
        ReflectionTestUtils.setField(unit, "active", active);
        return unit;
    }

    private RoleEntity role(RoleCode code) {
        RoleEntity role = new RoleEntity(code, code.name());
        ReflectionTestUtils.setField(role, "id", code == RoleCode.ADMINISTRADOR_PIIP ? 1L : 2L);
        return role;
    }
}
