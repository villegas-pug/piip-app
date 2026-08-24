package pe.gob.midagri.piip.identity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.application.UserAdministrationService;
import pe.gob.midagri.piip.identity.application.UserAdministrationCommands.AssignCommand;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.RoleRepository;
import pe.gob.midagri.piip.identity.persistence.RoleEntity;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeEntity;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdministrationConcurrencyTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock UserRoleScopeRepository scopes;
    @Mock InstitutionRepository institutions;
    @Mock ExecutingUnitRepository units;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;

    @Test
    void ordersActorAndRecipientIdsBeforeTakingTheCombinedLock() throws Exception {
        when(users.findAllByIdForUpdate(any())).thenReturn(List.of());
        Method lockUsers = UserAdministrationService.class.getDeclaredMethod("lockUsers", Long.class, Long.class);
        lockUsers.setAccessible(true);
        lockUsers.invoke(new UserAdministrationService(users, roles, scopes, institutions, units, authorization, audit), 17L, 3L);

        verify(users).findAllByIdForUpdate(List.of(3L, 17L));
    }

    @Test
    void concurrentMutationEntrancesUseTheSameOrderedLockBoundary() throws Exception {
        CountDownLatch entered = new CountDownLatch(2);
        when(users.findAllByIdForUpdate(any())).thenAnswer(invocation -> {
            entered.countDown();
            return List.of();
        });
        Method lockUsers = UserAdministrationService.class.getDeclaredMethod("lockUsers", Long.class, Long.class);
        lockUsers.setAccessible(true);
        UserAdministrationService service = new UserAdministrationService(users, roles, scopes, institutions, units, authorization, audit);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            executor.submit(() -> invoke(lockUsers, service, 11L, 5L));
            executor.submit(() -> invoke(lockUsers, service, 5L, 11L));
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            verify(users, times(2)).findAllByIdForUpdate(List.of(5L, 11L));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void institutionalAdministratorCoverageIsCheckedForEveryActiveUnitBeforeSuspension() {
        UserAdministrationService service = serviceWithAdministrator();
        UserEntity managed = user(2L, "managed-subject");
        UserRoleScopeEntity grant = scope(20L, managed, RoleCode.ADMINISTRADOR_PIIP, 10L, null);
        UserRoleScopeEntity backup = scope(21L, user(3L, "backup-subject"), RoleCode.ADMINISTRADOR_PIIP, 10L, 101L);
        InstitutionEntity institution = grant.getInstitution();
        ExecutingUnitEntity first = unit(institution, 101L);
        ExecutingUnitEntity second = unit(institution, 102L);
        when(scopes.findById(20L)).thenReturn(Optional.of(grant));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(grant));
        when(users.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(user(1L, "admin-subject"), managed));
        when(units.findByInstitutionIdAndActiveTrueOrderByName(10L)).thenReturn(List.of(first, second));
        when(scopes.findActiveAdministratorsForUpdate(eq(RoleCode.ADMINISTRADOR_PIIP), eq(10L), eq(101L), any())).thenReturn(List.of(grant, backup));
        when(scopes.findActiveAdministratorsForUpdate(eq(RoleCode.ADMINISTRADOR_PIIP), eq(10L), eq(102L), any())).thenReturn(List.of(grant, backup));

        service.suspend(20L, 0L);

        verify(scopes).findActiveAdministratorsForUpdate(eq(RoleCode.ADMINISTRADOR_PIIP), eq(10L), eq(101L), any());
        verify(scopes).findActiveAdministratorsForUpdate(eq(RoleCode.ADMINISTRADOR_PIIP), eq(10L), eq(102L), any());
        assertThat(grant.isActive()).isFalse();
    }

    @Test
    void twoConcurrentSuspensionsOnTheSameUnitAllowOnlyOneMutation() throws Exception {
        UserAdministrationService service = serviceWithAdministrator();
        UserEntity managed = user(2L, "managed-subject");
        UserRoleScopeEntity grant = scope(20L, managed, RoleCode.ADMINISTRADOR_PIIP, 10L, 101L);
        UserRoleScopeEntity backup = scope(21L, user(3L, "backup-subject"), RoleCode.ADMINISTRADOR_PIIP, 10L, 101L);
        ReentrantLock lock = new ReentrantLock();
        when(scopes.findById(20L)).thenReturn(Optional.of(grant));
        when(scopes.findByIdForUpdate(20L)).thenReturn(Optional.of(grant));
        when(users.findAllByIdForUpdate(List.of(1L, 2L))).thenAnswer(invocation -> {
            lock.lock();
            return List.of(user(1L, "admin-subject"), managed);
        });
        when(scopes.findActiveAdministratorsForUpdate(eq(RoleCode.ADMINISTRADOR_PIIP), eq(10L), eq(101L), any()))
            .thenReturn(List.of(grant, backup));
        doAnswer(invocation -> {
            if (lock.isHeldByCurrentThread()) lock.unlock();
            return null;
        }).when(audit).event(anyString(), anyString(), anyString(), anyMap(), anyString());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> service.suspend(20L, 0L));
            Future<?> second = executor.submit(() -> service.suspend(20L, 0L));
            int successes = 0;
            int failures = 0;
            for (Future<?> result : List.of(first, second)) {
                try {
                    result.get(2, TimeUnit.SECONDS);
                    successes++;
                } catch (java.util.concurrent.ExecutionException expected) {
                    failures++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(failures).isEqualTo(1);
            verify(audit, times(1)).event(eq("ROL_SUSPENDIDO"), eq("USUARIO_ROL_AMBITO"), eq("20"), anyMap(), eq("admin-subject"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void twoConcurrentPostsCreateOneActiveAssignmentAndRejectTheDuplicate() throws Exception {
        UserAdministrationService service = serviceWithAdministrator();
        UserEntity managed = user(2L, "managed-subject");
        RoleEntity role = role(RoleCode.CONSULTA_EXTERNA);
        InstitutionEntity institution = institution(10L);
        List<UserRoleScopeEntity> active = new ArrayList<>();
        ReentrantLock lock = new ReentrantLock();
        when(roles.findByCode(RoleCode.CONSULTA_EXTERNA)).thenReturn(Optional.of(role));
        when(institutions.findById(10L)).thenReturn(Optional.of(institution));
        when(users.findByKeycloakSubject("managed-subject")).thenReturn(Optional.of(managed));
        when(users.findAllByIdForUpdate(List.of(1L, 2L))).thenAnswer(invocation -> {
            lock.lock();
            return List.of(user(1L, "admin-subject"), managed);
        });
        when(scopes.findActiveDuplicatesForUpdate(eq(2L), eq(2L), eq(10L), isNull(), any()))
            .thenAnswer(invocation -> {
                if (active.isEmpty()) return List.of();
                if (lock.isHeldByCurrentThread()) lock.unlock();
                return List.copyOf(active);
            });
        when(scopes.findLatestSuspendedExactForUpdate(2L, 2L, 10L, null)).thenReturn(Optional.empty());
        when(scopes.save(any(UserRoleScopeEntity.class))).thenAnswer(invocation -> {
            UserRoleScopeEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            active.add(saved);
            if (lock.isHeldByCurrentThread()) lock.unlock();
            return saved;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> service.assign(new AssignCommand("managed-subject", RoleCode.CONSULTA_EXTERNA, 10L, null)));
            Future<?> second = executor.submit(() -> service.assign(new AssignCommand("managed-subject", RoleCode.CONSULTA_EXTERNA, 10L, null)));
            int successes = 0;
            int failures = 0;
            for (Future<?> result : List.of(first, second)) {
                try {
                    result.get(2, TimeUnit.SECONDS);
                    successes++;
                } catch (java.util.concurrent.ExecutionException expected) {
                    failures++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(failures).isEqualTo(1);
            assertThat(active).hasSize(1);
            verify(audit, times(1)).event(eq("ROL_ASIGNADO"), eq("USUARIO_ROL_AMBITO"), eq("30"), anyMap(), eq("admin-subject"));
        } finally {
            executor.shutdownNow();
        }
    }

    private UserAdministrationService serviceWithAdministrator() {
        LocalAccessContext administrator = new LocalAccessContext(1L, "admin-subject",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, 100L)));
        when(authorization.require(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(administrator);
        when(authorization.resolve("admin-subject")).thenReturn(administrator);
        return new UserAdministrationService(users, roles, scopes, institutions, units, authorization, audit);
    }

    private UserEntity user(Long id, String subject) {
        UserEntity user = new UserEntity(subject, "Persona " + id, "persona" + id + "@example.test");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserRoleScopeEntity scope(Long id, UserEntity user, RoleCode code, Long institutionId, Long unitId) {
        InstitutionEntity institution = institution(institutionId);
        ExecutingUnitEntity unit = unitId == null ? null : unit(institution, unitId);
        UserRoleScopeEntity scope = new UserRoleScopeEntity(user, role(code), institution, unit, "admin-subject");
        ReflectionTestUtils.setField(scope, "id", id);
        return scope;
    }

    private InstitutionEntity institution(Long id) {
        InstitutionEntity institution = new InstitutionEntity("INST-" + id, "Institución " + id);
        ReflectionTestUtils.setField(institution, "id", id);
        return institution;
    }

    private ExecutingUnitEntity unit(InstitutionEntity institution, Long id) {
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-" + id, "Unidad Ejecutora " + id);
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }

    private RoleEntity role(RoleCode code) {
        RoleEntity role = new RoleEntity(code, code.name());
        ReflectionTestUtils.setField(role, "id", code == RoleCode.ADMINISTRADOR_PIIP ? 1L : 2L);
        return role;
    }

    private void invoke(Method method, UserAdministrationService service, Long actor, Long recipient) {
        try {
            method.invoke(service, actor, recipient);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
