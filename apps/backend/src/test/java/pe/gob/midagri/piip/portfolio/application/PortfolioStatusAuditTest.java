package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.audit.persistence.AccessAuditRepository;
import pe.gob.midagri.piip.audit.persistence.AuditEventEntity;
import pe.gob.midagri.piip.audit.persistence.AuditEventRepository;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
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
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.InitiativeStatusTransitionRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ProjectStatusTransitionRequest;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PortfolioStatusAuditTest.FailingAuditConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PortfolioStatusAuditTest {
    @MockitoBean JwtDecoder jwtDecoder;
    @Autowired InitiativeApplicationService initiatives;
    @Autowired ProjectApplicationService projects;
    @Autowired PortfolioRecordRepository records;
    @Autowired AuditEventRepository auditEvents;
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired UserRoleScopeRepository scopes;
    @Autowired FailingAuditService audit;
    @Autowired ObjectMapper objectMapper;
    @Autowired CatalogRepository catalogs;
    @Autowired CatalogItemRepository catalogItems;

    private Actor actor;

    @BeforeEach
    void setUp() {
        actor = actor();
        authenticate(actor);
        audit.failAfterWrite(false);
    }

    @AfterEach
    void tearDown() {
        audit.failAfterWrite(false);
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsTheRequiredJsonEvidenceForInitiativeTransition() throws Exception {
        PortfolioRecordEntity initiative = initiative();

        initiatives.transitionInitiativeStatus(initiative.getCode(),
            new InitiativeStatusTransitionRequest(0L, PortfolioStatus.INITIATIVE_ARCHIVED, "archivada por evaluación"));

        AuditEventEntity event = eventFor(initiative.getCode(), "ESTADO_INICIATIVA_CAMBIADO");
        assertThat(event.getActorSubject()).isEqualTo(actor.subject);
        assertThat(event.getOccurredAt()).isNotNull();
        assertDetail(event, "Presentado", "Iniciativa archivada", "archivada por evaluación", actor.unit);
    }

    @Test
    void recordsTheRequiredJsonEvidenceForProjectTransition() throws Exception {
        PortfolioRecordEntity project = project();

        projects.transitionProjectStatus(project.getCode(),
            new ProjectStatusTransitionRequest(0L, PortfolioStatus.PRODUCT_APPROVED, "producto validado"));

        AuditEventEntity event = eventFor(project.getCode(), "ESTADO_PROYECTO_CAMBIADO");
        assertThat(event.getActorSubject()).isEqualTo(actor.subject);
        assertThat(event.getOccurredAt()).isNotNull();
        assertDetail(event, "Proyecto en ejecución", "Producto aprobado", "producto validado", actor.unit);
    }

    @Test
    void rollsBackInitiativeStateAndVersionWhenAuditFails() {
        PortfolioRecordEntity initiative = initiative();
        audit.failAfterWrite(true);

        assertThatThrownBy(() -> initiatives.transitionInitiativeStatus(initiative.getCode(),
            new InitiativeStatusTransitionRequest(0L, PortfolioStatus.INITIATIVE_ARCHIVED, "debe revertirse")))
            .isInstanceOf(IllegalStateException.class);

        audit.failAfterWrite(false);
        PortfolioRecordEntity persisted = records.findByCodeIgnoreCase(initiative.getCode()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PortfolioStatus.PRESENTED);
        assertThat(persisted.getVersion()).isZero();
        assertThat(auditEvents.findByEntityCodeOrderByOccurredAtAsc(initiative.getCode())).isEmpty();
    }

    @Test
    void rollsBackProjectStateClosingDateAndVersionWhenAuditFails() {
        PortfolioRecordEntity project = project();
        audit.failAfterWrite(true);

        assertThatThrownBy(() -> projects.transitionProjectStatus(project.getCode(),
            new ProjectStatusTransitionRequest(0L, PortfolioStatus.PRODUCT_APPROVED, "debe revertirse")))
            .isInstanceOf(IllegalStateException.class);

        audit.failAfterWrite(false);
        PortfolioRecordEntity persisted = records.findByCodeIgnoreCase(project.getCode()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PortfolioStatus.PROJECT_IN_PROGRESS);
        assertThat(persisted.getClosingDate()).isNull();
        assertThat(persisted.getVersion()).isZero();
        assertThat(auditEvents.findByEntityCodeOrderByOccurredAtAsc(project.getCode())).isEmpty();
    }

    private PortfolioRecordEntity initiative() {
        String suffix = suffix();
        return records.saveAndFlush(PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, suffix)
            .initiative("I-AUDIT-" + suffix, actor.unit, "Iniciativa auditada"));
    }

    private PortfolioRecordEntity project() {
        String suffix = suffix();
        return records.saveAndFlush(PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, suffix)
            .preexistingProject("P-AUDIT-" + suffix, actor.unit, "Proyecto auditado"));
    }

    private AuditEventEntity eventFor(String code, String eventType) {
        return auditEvents.findByEntityCodeOrderByOccurredAtAsc(code).stream()
            .filter(event -> event.getEventType().equals(eventType)).findFirst().orElseThrow();
    }

    private void assertDetail(AuditEventEntity event, String previous, String current, String observation,
            ExecutingUnitEntity unit) throws Exception {
        JsonNode detail = objectMapper.readTree(event.getDetailJson());
        assertThat(detail.fieldNames()).toIterable().containsExactlyInAnyOrder(
            "estadoAnterior", "estadoNuevo", "rol", "unidadEjecutoraId", "unidadEjecutora", "observacion", "resultado");
        assertThat(detail.get("estadoAnterior").asText()).isEqualTo(previous);
        assertThat(detail.get("estadoNuevo").asText()).isEqualTo(current);
        assertThat(detail.get("rol").asText()).isEqualTo(RoleCode.ADMINISTRADOR_PIIP.name());
        assertThat(detail.get("unidadEjecutoraId").asLong()).isEqualTo(unit.getId());
        assertThat(detail.get("unidadEjecutora").asText()).isEqualTo(unit.getName());
        assertThat(detail.get("observacion").asText()).isEqualTo(observation);
        assertThat(detail.get("resultado").asText()).isEqualTo("EXITOSO");
    }

    private Actor actor() {
        String suffix = suffix();
        InstitutionEntity institution = institutions.save(new InstitutionEntity("INST-AUDIT-" + suffix, "Institución auditada"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-AUDIT-" + suffix, "Unidad auditada"));
        RoleEntity role = roles.findByCode(RoleCode.ADMINISTRADOR_PIIP)
            .orElseGet(() -> roles.save(new RoleEntity(RoleCode.ADMINISTRADOR_PIIP, "Administrador PIIP")));
        UserEntity user = users.save(new UserEntity("subject-audit-" + suffix, "Usuario auditado", "audit-" + suffix + "@example.test"));
        scopes.saveAndFlush(new UserRoleScopeEntity(user, role, institution, unit, "test"));
        return new Actor(user.getId(), user.getKeycloakSubject(), institution, unit);
    }

    private void authenticate(Actor actor) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(actor.subject, "test");
        authentication.setAuthenticated(true);
        authentication.setDetails(new LocalAccessContext(actor.userId, actor.subject,
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, actor.institution.getId(), actor.unit.getId()))));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private record Actor(Long userId, String subject, InstitutionEntity institution, ExecutingUnitEntity unit) {}

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
        public void event(String type, String entityType, String entityCode, java.util.Map<String, ?> detail,
                String actorSubject) {
            super.event(type, entityType, entityCode, detail, actorSubject);
            if (failAfterWrite) throw new IllegalStateException("Falla simulada al guardar auditoría");
        }
    }
}
