package pe.gob.midagri.piip.identity.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    @Autowired UserRepository users;
    @Autowired UserRoleScopeRepository scopes;
    @Autowired RoleRepository roles;
    @Autowired InstitutionRepository institutions;

    @Test
    void findsUsersWithoutAnyRoleScopeHistoryRegardlessOfLegacyActiveState() {
        UserEntity candidate = users.save(new UserEntity("candidate", "Candidata", "candidate@example.test"));
        UserEntity withSuspendedAssignment = users.save(new UserEntity("historical", "Histórico", "historical@example.test"));
        UserEntity disabled = users.save(new UserEntity("disabled", "Inhabilitado", "disabled@example.test"));
        disabled.changeActiveState(false);
        RoleEntity role = roles.save(new RoleEntity(RoleCode.CONSULTA_EXTERNA, "Consulta externa"));
        InstitutionEntity institution = institutions.save(new InstitutionEntity("INST", "Institución"));
        UserRoleScopeEntity historical = scopes.save(new UserRoleScopeEntity(withSuspendedAssignment, role, institution, null, "admin"));
        historical.suspend(java.time.Instant.now());

        List<UserEntity> candidates = users.findWithoutRoleScopeHistory();

        assertThat(candidates).extracting(UserEntity::getKeycloakSubject)
            .containsExactlyInAnyOrder("candidate", "disabled");
    }
}
