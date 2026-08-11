package pe.gob.midagri.piip.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.api.AdminDtos.UserResponse;
import pe.gob.midagri.piip.identity.api.AdminDtos.UserAssignmentCandidateResponse;
import pe.gob.midagri.piip.identity.api.AdminDtos.RoleAssignmentUpdateRequest;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;

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
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, null)));
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

        List<UserResponse> response = service.list();

        assertThat(response).singleElement().satisfies(managed -> {
            assertThat(managed.scopes()).extracting(scope -> scope.active()).containsExactly(true, false);
        });
    }

    @Test
    void listsUsersWithoutAssignmentHistoryForAdministrators() {
        UserEntity candidate = user(2L, "candidate-subject", false);
        when(users.findWithoutRoleScopeHistory()).thenReturn(List.of(candidate));

        List<UserAssignmentCandidateResponse> response = service.listAssignmentCandidates();

        assertThat(response).containsExactly(new UserAssignmentCandidateResponse(2L, "candidate-subject",
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
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(user));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(scope));
        when(scopes.findActiveAdministratorsForUpdate(eq(RoleCode.ADMINISTRADOR_PIIP), eq(10L), isNull(), any())).thenReturn(List.of(scope));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.suspend(20L, 0L))
            .isInstanceOf(pe.gob.midagri.piip.shared.api.BusinessRuleException.class);
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
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(user));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(scope));
        when(roles.findByCode(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(Optional.of(administratorRole));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(scopes.findActiveDuplicatesForUpdate(eq(2L), eq(1L), eq(10L), isNull(), any())).thenReturn(List.of());

        var response = service.update(20L, 0L, new RoleAssignmentUpdateRequest(RoleCode.ADMINISTRADOR_PIIP, 10L, null));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.role()).isEqualTo(RoleCode.ADMINISTRADOR_PIIP);
        verify(audit).event(eq("ROL_ACTUALIZADO"), eq("USUARIO"), eq("managed-subject"), anyMap(), eq("admin-subject"));
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

    private UserEntity user(Long id, String subject, boolean active) {
        UserEntity user = new UserEntity(subject, "Persona administrable", "persona@example.test");
        ReflectionTestUtils.setField(user, "id", id);
        user.changeActiveState(active);
        return user;
    }

    private UserRoleScopeEntity scope(Long id, UserEntity user, RoleCode code, Long institutionId) {
        InstitutionEntity institution = new InstitutionEntity("INST-" + institutionId, "Institución");
        ReflectionTestUtils.setField(institution, "id", institutionId);
        RoleEntity role = new RoleEntity(code, code.name());
        ReflectionTestUtils.setField(role, "id", code == RoleCode.ADMINISTRADOR_PIIP ? 1L : 2L);
        UserRoleScopeEntity scope = new UserRoleScopeEntity(user, role, institution, null, "admin-subject");
        ReflectionTestUtils.setField(scope, "id", id);
        return scope;
    }
}
