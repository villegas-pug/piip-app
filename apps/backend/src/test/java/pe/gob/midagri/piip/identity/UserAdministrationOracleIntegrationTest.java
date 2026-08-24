package pe.gob.midagri.piip.identity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pe.gob.midagri.piip.audit.persistence.AccessAuditEntity;
import pe.gob.midagri.piip.audit.persistence.AccessAuditRepository;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas Oracle reales de la feature 014.
 *
 * <p>No usa Docker, Testcontainers, {@code test-reset} ni DDL. Cada test usa
 * transacciones JPA y revierte sus fixtures al finalizar.</p>
 */
@Tag("integration")
@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserAdministrationOracleIntegrationTest {
    @Autowired AccessAuditRepository accessAudits;
    @Autowired UserRoleScopeRepository scopes;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;
    @Autowired PlatformTransactionManager transactionManager;

    private final String suffix = UUID.randomUUID().toString().replace("-", "");
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = newFixture();
    }

    @AfterEach
    void tearDown() {
        if (fixture == null) return;
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            scopes.deleteById(fixture.scopeId());
            users.deleteById(fixture.userId());
        });
    }

    @Test
    void persistsAndReadsMotivoSeguroThroughJpaOnOracle() {
        String correlationId = "T069-" + suffix;
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            AccessAuditEntity audit = accessAudits.saveAndFlush(new AccessAuditEntity(
                null, fixture.subject(), "ADMINISTRADOR_PIIP", "PUT",
                "/api/v1/admin/role-assignments/" + fixture.scopeId(), 403,
                null, "127.0.0.1", correlationId, 12L, "FORBIDDEN_SCOPE"));

            AccessAuditEntity persisted = accessAudits.findTop100ByOrderByOccurredAtDesc().stream()
                .filter(candidate -> candidate.getCorrelationId().equals(correlationId))
                .findFirst().orElseThrow();
            assertThat(persisted.getSafeReason()).isEqualTo("FORBIDDEN_SCOPE");
            assertThat(persisted.getCorrelationId()).isEqualTo(correlationId);
            status.setRollbackOnly();
        });
    }

    @Test
    void pessimisticScopeLockSerializesTwoOracleTransactions() throws Exception {
        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                scopes.findByIdForUpdate(fixture.scopeId()).orElseThrow();
                firstLockAcquired.countDown();
                await(releaseFirst);
            }));
            assertThat(firstLockAcquired.await(20, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> transaction.executeWithoutResult(status ->
                scopes.findByIdForUpdate(fixture.scopeId()).orElseThrow()));

            Thread.sleep(500L);
            assertThat(second.isDone()).as("la segunda transacción debe esperar el lock Oracle").isFalse();
            releaseFirst.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void orderedUserLockReturnsAscendingIdsOnOracle() {
        Fixture secondFixture = newFixture();
        try {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.executeWithoutResult(status -> {
                List<UserEntity> locked = users.findAllByIdForUpdate(List.of(secondFixture.userId(), fixture.userId()));
                assertThat(locked).extracting(UserEntity::getId)
                    .containsExactly(fixture.userId(), secondFixture.userId());
                status.setRollbackOnly();
            });
        } finally {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                scopes.deleteById(secondFixture.scopeId());
                users.deleteById(secondFixture.userId());
            });
        }
    }

    private Fixture newFixture() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            InstitutionEntity institution = institutions.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Oracle de pruebas no tiene instituciones"));
            ExecutingUnitEntity unit = executingUnits.findByInstitutionIdAndActiveTrueOrderByName(institution.getId()).stream()
                .findFirst().orElseThrow(() -> new IllegalStateException("Oracle de pruebas no tiene UEs activas"));
            RoleEntity role = roles.findByCode(RoleCode.CONSULTA_EXTERNA)
                .orElseThrow(() -> new IllegalStateException("Oracle de pruebas no tiene el rol CONSULTA_EXTERNA"));
            UserEntity user = users.saveAndFlush(new UserEntity(
                "t069-" + suffix + "-" + UUID.randomUUID(), "Fixture T069", "t069-" + suffix + "@example.test"));
            UserRoleScopeEntity scope = scopes.saveAndFlush(new UserRoleScopeEntity(user, role, institution, unit, "T069"));
            return new Fixture(user.getId(), user.getKeycloakSubject(), scope.getId());
        });
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) throw new AssertionError("timeout esperando la transacción Oracle");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private record Fixture(Long userId, String subject, Long scopeId) {}
}
