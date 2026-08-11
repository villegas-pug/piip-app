package pe.gob.midagri.piip.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.RoleEntity;
import pe.gob.midagri.piip.identity.persistence.RoleRepository;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeEntity;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(LocalAuthorizationService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LocalAuthorizationConcurrencyTest {
    @Autowired LocalAuthorizationService authorization;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired UserRoleScopeRepository scopes;
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;

    private String subject;

    @BeforeEach
    void provisionAdministrator() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        subject = "subject-" + suffix;
        InstitutionEntity institution = institutions.save(new InstitutionEntity("INST-" + suffix, "Institución de prueba"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-" + suffix, "Unidad de prueba"));
        RoleEntity role = roles.findByCode(RoleCode.ADMINISTRADOR_PIIP)
            .orElseGet(() -> roles.save(new RoleEntity(RoleCode.ADMINISTRADOR_PIIP, "Administrador PIIP")));
        UserEntity user = users.save(new UserEntity(subject, "Usuario original", "original@example.test"));
        scopes.save(new UserRoleScopeEntity(user, role, institution, unit, "TEST"));
    }

    @Test
    void resolvesParallelRequestsWithoutCompetingForTheUserVersion() throws Exception {
        List<LocalAccessContext> contexts = invokeConcurrently(8, () -> authorization.resolve(subject));

        assertThat(contexts).allSatisfy(context -> {
            assertThat(context.subject()).isEqualTo(subject);
            assertThat(context.roles()).containsExactly(RoleCode.ADMINISTRADOR_PIIP);
        });
        UserEntity unchanged = users.findByKeycloakSubject(subject).orElseThrow();
        assertThat(unchanged.getVersion()).isZero();
        assertThat(unchanged.getLastAuthenticatedAt()).isNull();
    }

    @Test
    void resolvesActiveRoleScopeWhenTheLegacyUserStateIsInactive() {
        UserEntity user = users.findByKeycloakSubject(subject).orElseThrow();
        user.changeActiveState(false);
        users.saveAndFlush(user);

        LocalAccessContext context = authorization.resolve(subject);

        assertThat(context.roles()).containsExactly(RoleCode.ADMINISTRADOR_PIIP);
    }

    @Test
    void serializesParallelAuthenticationUpdates() throws Exception {
        invokeConcurrently(4, () -> {
            authorization.recordAuthentication(subject, "Usuario autenticado", "autenticado@example.test");
            return null;
        });

        UserEntity updated = users.findByKeycloakSubject(subject).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(4);
        assertThat(updated.getLastAuthenticatedAt()).isNotNull();
        assertThat(updated.getFullName()).isEqualTo("Usuario autenticado");
        assertThat(updated.getEmail()).isEqualTo("autenticado@example.test");
    }

    private <T> List<T> invokeConcurrently(int count, Callable<T> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < count; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No se inició la prueba concurrente");
                    return action.call();
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) results.add(future.get(10, TimeUnit.SECONDS));
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
