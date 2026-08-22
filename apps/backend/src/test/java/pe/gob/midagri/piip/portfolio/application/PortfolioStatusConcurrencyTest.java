package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.DerivedProjectRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ProjectStatusTransitionRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PortfolioStatusConcurrencyTest {
    @MockitoBean JwtDecoder jwtDecoder;
    @Autowired InitiativeApplicationService initiatives;
    @Autowired ProjectApplicationService projects;
    @Autowired PortfolioRecordRepository records;
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired UserRoleScopeRepository scopes;
    @Autowired CatalogRepository catalogs;
    @Autowired CatalogItemRepository catalogItems;
    @Autowired OrganizationalUnitRepository organizationalUnits;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void serializesDerivationAndInitiativeArchivingOnTheSameOrigin() throws Exception {
        Actor actor = actor();
        String suffix = suffix();
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, suffix);
        PortfolioRecordEntity initiative = records.saveAndFlush(fixtures.initiative("I-RACE-" + suffix, actor.unit, "Iniciativa concurrente"));
        initiative.approve();
        records.saveAndFlush(initiative);

        Callable<Boolean> archive = () -> {
            authenticate(actor);
            try {
                initiatives.transitionInitiativeStatus(initiative.getCode(),
                    new PortfolioDtos.InitiativeStatusTransitionRequest(0L, PortfolioStatus.INITIATIVE_ARCHIVED, "archivar"));
                return true;
            } catch (RuntimeException exception) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        Callable<Boolean> derive = () -> {
            authenticate(actor);
            try {
                projects.createDerived(new DerivedProjectRequest(initiative.getCode(), LocalDate.of(2026, 8, 18),
                    "Proyecto concurrente", fixtures.solution().getId(), fixtures.source().getId(), "Responsable", null, null,
                    "Descripción", null, null, DigitalComponent.NO,
                    List.of(new pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ResponsibleUnitInput(actor.organizationalUnit.getId()))));
                return true;
            } catch (RuntimeException exception) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        List<Boolean> results = invokeConcurrently(archive, derive);

        assertThat(results).containsExactlyInAnyOrder(true, false);
        PortfolioRecordEntity persisted = records.findByCodeIgnoreCase(initiative.getCode()).orElseThrow();
        boolean hasProject = records.existsByOriginRecordId(persisted.getId());
        assertThat(persisted.getStatus() == PortfolioStatus.INITIATIVE_APPROVED || persisted.getStatus() == PortfolioStatus.INITIATIVE_ARCHIVED).isTrue();
        assertThat(!(persisted.getStatus() == PortfolioStatus.INITIATIVE_ARCHIVED && hasProject)).isTrue();
    }

    @Test
    void usesTheExistingOptimisticVersionForTwoTransitionsWithTheSameVersion() throws Exception {
        Actor actor = actor();
        String suffix = suffix();
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, suffix);
        PortfolioRecordEntity project = records.saveAndFlush(fixtures.preexistingProject("P-RACE-" + suffix, actor.unit, "Proyecto concurrente"));

        Callable<Boolean> approveProduct = transition(actor, project.getCode(), PortfolioStatus.PRODUCT_APPROVED);
        Callable<Boolean> suspend = transition(actor, project.getCode(), PortfolioStatus.SUSPENDED);
        List<Boolean> results = invokeConcurrently(approveProduct, suspend);

        assertThat(results).containsExactlyInAnyOrder(true, false);
        PortfolioRecordEntity persisted = records.findByCodeIgnoreCase(project.getCode()).orElseThrow();
        assertThat(persisted.getVersion()).isEqualTo(1L);
        assertThat(persisted.getStatus()).isIn(PortfolioStatus.PRODUCT_APPROVED, PortfolioStatus.SUSPENDED);
    }

    private Callable<Boolean> transition(Actor actor, String code, PortfolioStatus target) {
        return () -> {
            authenticate(actor);
            try {
                projects.transitionProjectStatus(code, new ProjectStatusTransitionRequest(0L, target, "decisión concurrente"));
                return true;
            } catch (RuntimeException exception) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private List<Boolean> invokeConcurrently(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("No se preparó la carrera");
                    start.await(10, TimeUnit.SECONDS);
                    return task.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(futures.get(0).get(30, TimeUnit.SECONDS), futures.get(1).get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Actor actor() {
        String suffix = suffix();
        InstitutionEntity institution = institutions.save(new InstitutionEntity("INST-RACE-" + suffix, "Institución concurrente"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-RACE-" + suffix, "Unidad concurrente"));
        OrganizationalUnitEntity organizationalUnit = organizationalUnits.save(new OrganizationalUnitEntity(unit, "UO-RACE-" + suffix, "Unidad responsable", "UR"));
        RoleEntity role = roles.findByCode(RoleCode.ADMINISTRADOR_PIIP)
            .orElseGet(() -> roles.save(new RoleEntity(RoleCode.ADMINISTRADOR_PIIP, "Administrador PIIP")));
        UserEntity user = users.save(new UserEntity("subject-race-" + suffix, "Usuario concurrente", "race-" + suffix + "@example.test"));
        scopes.saveAndFlush(new UserRoleScopeEntity(user, role, institution, unit, "test"));
        return new Actor(user.getId(), user.getKeycloakSubject(), institution, unit, organizationalUnit);
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

    private record Actor(Long userId, String subject, InstitutionEntity institution, ExecutingUnitEntity unit, OrganizationalUnitEntity organizationalUnit) {}
}
