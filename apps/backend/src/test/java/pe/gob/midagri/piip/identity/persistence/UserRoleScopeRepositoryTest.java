package pe.gob.midagri.piip.identity.persistence;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRoleScopeRepositoryTest {
    @Autowired UserRoleScopeRepository scopes;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired InstitutionRepository institutions;

    @Test
    void findsOnlyTheExactActiveAssignmentAndTheLatestSuspendedHistory() {
        UserEntity user = users.save(new UserEntity("scope-subject", "Scope", "scope@example.test"));
        RoleEntity role = roles.save(new RoleEntity(RoleCode.CONSULTA_EXTERNA, "Consulta externa"));
        InstitutionEntity institution = institutions.save(new InstitutionEntity("SCOPE", "Scope"));
        UserRoleScopeEntity suspendedOld = scopes.save(new UserRoleScopeEntity(user, role, institution, null, "admin"));
        suspendedOld.suspend(Instant.now().minusSeconds(60));
        UserRoleScopeEntity suspendedLatest = scopes.save(new UserRoleScopeEntity(user, role, institution, null, "admin"));
        suspendedLatest.suspend(Instant.now().minusSeconds(10));
        UserRoleScopeEntity active = scopes.save(new UserRoleScopeEntity(user, role, institution, null, "admin"));

        assertThat(scopes.findActiveDuplicatesForUpdate(user.getId(), role.getId(), institution.getId(), null, Instant.now()))
            .extracting(UserRoleScopeEntity::getId).containsExactly(active.getId());
        assertThat(scopes.findLatestSuspendedExactForUpdate(user.getId(), role.getId(), institution.getId(), null))
            .get().extracting(UserRoleScopeEntity::getId).isEqualTo(suspendedLatest.getId());
    }

    @Test
    void declaresPesimisticLocksForMutationQueries() throws NoSuchMethodException {
        assertThat(UserRoleScopeRepository.class.getDeclaredMethod("findActiveDuplicatesForUpdate", Long.class, Long.class,
            Long.class, Long.class, Instant.class).getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(UserRoleScopeRepository.class.getDeclaredMethod("findActiveAdministratorsForUpdate", RoleCode.class,
            Long.class, Long.class, Instant.class).getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
