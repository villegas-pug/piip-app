package pe.gob.midagri.piip.identity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.audit.persistence.AccessAuditRepository;
import pe.gob.midagri.piip.audit.persistence.AuditEventEntity;
import pe.gob.midagri.piip.audit.persistence.AuditEventRepository;
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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = UserAdministrationTransactionalAuditTest.FailingAuditConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserAdministrationTransactionalAuditTest {
    @MockitoBean JwtDecoder jwtDecoder;

    @Autowired UserAdministrationService service;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired UserRoleScopeRepository scopes;
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository units;
    @Autowired AuditEventRepository auditEvents;
    @Autowired EntityManager entityManager;
    @Autowired FailingAuditService audit;

    private Actor actor;

    @BeforeEach
    void setUp() {
        actor = createFixture();
        authenticate(actor);
        audit.failAfterWrite(false);
    }

    @AfterEach
    void tearDown() {
        audit.failAfterWrite(false);
        SecurityContextHolder.clearContext();
    }

    @Test
    void rollsBackAssignmentStateAndFunctionalAuditWhenAuditFails() {
        audit.failAfterWrite(true);

        assertThatThrownBy(() -> service.suspend(actor.scopeId(), 0L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Falla simulada al guardar auditoría");

        audit.failAfterWrite(false);
        entityManager.clear();
        UserRoleScopeEntity persisted = scopes.findById(actor.scopeId()).orElseThrow();
        assertThat(persisted.isActive()).isTrue();
        assertThat(persisted.getValidUntil()).isNull();
        assertThat(persisted.getVersion()).isZero();
        assertThat(auditEvents.findByEntityCodeOrderByOccurredAtAsc(String.valueOf(actor.scopeId())))
            .isEmpty();
    }

    @Test
    void persistsExactlyOneCompleteEventForASuccessfulSuspension() {
        service.suspend(actor.scopeId(), 0L);

        var events = auditEvents.findByEntityCodeOrderByOccurredAtAsc(String.valueOf(actor.scopeId()));
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo("ROL_SUSPENDIDO");
            assertThat(event.getEntityType()).isEqualTo("USUARIO_ROL_AMBITO");
            assertThat(event.getActorSubject()).isEqualTo(actor.subject());
            assertThat(event.getOccurredAt()).isNotNull();
            assertThat(event.getDetailJson()).contains("actor", "action", "affectedUser", "before", "after", "result");
        });
    }

    private Actor createFixture() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        InstitutionEntity institution = institutions.save(new InstitutionEntity("INST-UA-" + suffix, "Institución UA " + suffix));
        ExecutingUnitEntity unit = units.save(new ExecutingUnitEntity(institution, "UE-UA-" + suffix, "Unidad UA " + suffix));
        RoleEntity administrator = roles.findByCode(RoleCode.ADMINISTRADOR_PIIP)
            .orElseGet(() -> roles.save(new RoleEntity(RoleCode.ADMINISTRADOR_PIIP, "Administrador PIIP")));
        RoleEntity consultation = roles.findByCode(RoleCode.CONSULTA_EXTERNA)
            .orElseGet(() -> roles.save(new RoleEntity(RoleCode.CONSULTA_EXTERNA, "Consulta externa")));
        UserEntity admin = users.save(new UserEntity("subject-admin-" + suffix, "Administrador UA", "admin-" + suffix + "@example.test"));
        UserEntity managed = users.save(new UserEntity("subject-managed-" + suffix, "Usuario UA", "managed-" + suffix + "@example.test"));
        UserRoleScopeEntity adminScope = scopes.save(new UserRoleScopeEntity(admin, administrator, institution, null, "fixture"));
        UserRoleScopeEntity managedScope = scopes.saveAndFlush(new UserRoleScopeEntity(managed, consultation, institution, unit, "fixture"));
        return new Actor(admin.getId(), admin.getKeycloakSubject(), managedScope.getId(), institution.getId());
    }

    private void authenticate(Actor fixture) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(fixture.subject(), "test");
        authentication.setAuthenticated(true);
        authentication.setDetails(new LocalAccessContext(fixture.userId(), fixture.subject(),
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, fixture.institutionId(), null))));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private record Actor(Long userId, String subject, Long scopeId, Long institutionId) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingAuditConfiguration {
        @Bean
        @Primary
        FailingAuditService failingAuditService(AccessAuditRepository accesses, AuditEventRepository events,
                UserRepository users, ObjectMapper objectMapper) {
            return new FailingAuditService(accesses, events, users, objectMapper);
        }
    }

    static class FailingAuditService extends AuditService {
        private volatile boolean failAfterWrite;

        FailingAuditService(AccessAuditRepository accesses, AuditEventRepository events, UserRepository users,
                ObjectMapper objectMapper) {
            super(accesses, events, users, objectMapper);
        }

        void failAfterWrite(boolean fail) {
            this.failAfterWrite = fail;
        }

        @Override
        public void event(String type, String entityType, String entityCode, Map<String, ?> detail,
                String actorSubject) {
            super.event(type, entityType, entityCode, detail, actorSubject);
            if (failAfterWrite) throw new IllegalStateException("Falla simulada al guardar auditoría");
        }
    }
}
