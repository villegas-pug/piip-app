package pe.gob.midagri.piip.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.audit.persistence.*;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.config.reset.TestResetCoordinator;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "test-reset"})
class TestResetOracleIntegrationTest {
    private final InstitutionRepository institutions; private final ExecutingUnitRepository executingUnits;
    private final OrganizationalUnitRepository organizationalUnits; private final RoleRepository roles;
    private final UserRepository users; private final UserRoleScopeRepository scopes;
    private final AuditEventRepository auditEvents; private final AccessAuditRepository accessAudits;
    private final NotificationRepository notifications;
    private final CatalogRepository catalogs; private final CatalogItemRepository catalogItems;
    private final DocumentTypeRepository documentTypes; private final TestResetCoordinator coordinator;

    @org.springframework.beans.factory.annotation.Autowired
    TestResetOracleIntegrationTest(InstitutionRepository institutions, ExecutingUnitRepository executingUnits,
            OrganizationalUnitRepository organizationalUnits, RoleRepository roles, UserRepository users,
            UserRoleScopeRepository scopes, AuditEventRepository auditEvents, AccessAuditRepository accessAudits,
            NotificationRepository notifications, CatalogRepository catalogs, CatalogItemRepository catalogItems,
            DocumentTypeRepository documentTypes, TestResetCoordinator coordinator) {
        this.institutions = institutions; this.executingUnits = executingUnits; this.organizationalUnits = organizationalUnits;
        this.roles = roles; this.users = users; this.scopes = scopes; this.auditEvents = auditEvents;
        this.accessAudits = accessAudits; this.notifications = notifications;
        this.catalogs = catalogs; this.catalogItems = catalogItems; this.documentTypes = documentTypes; this.coordinator = coordinator;
    }

    @Test void preservaIdentidadYDejaAuditoriaYNotificacionesVacias() {
        assertThat(institutions.count()).isEqualTo(1);
        assertThat(executingUnits.count()).isEqualTo(2);
        assertThat(organizationalUnits.count()).isEqualTo(4);
        assertThat(roles.count()).isEqualTo(2);
        assertThat(users.count()).isEqualTo(1);
        assertThat(scopes.count()).isEqualTo(2);
        assertThat(auditEvents.count()).isZero();
        assertThat(accessAudits.count()).isZero();
        assertThat(notifications.count()).isZero();
    }

    @Test void reejecucionCompletaEsIdempotenteYConservaElDataset() {
        DatasetCounts before = datasetCounts();

        org.assertj.core.api.Assertions.assertThatCode(() -> coordinator.run(new org.springframework.boot.DefaultApplicationArguments(new String[0])))
            .doesNotThrowAnyException();

        assertThat(datasetCounts()).isEqualTo(before);
        assertThat(catalogs.count()).isEqualTo(4);
        assertThat(catalogItems.count()).isEqualTo(17);
        assertThat(documentTypes.count()).isEqualTo(6);
        assertThat(auditEvents.count()).isZero();
        assertThat(accessAudits.count()).isZero();
        assertThat(notifications.count()).isZero();
    }

    private DatasetCounts datasetCounts() {
        return new DatasetCounts(institutions.count(), roles.count(), executingUnits.count(), organizationalUnits.count(),
            users.count(), scopes.count());
    }

    private record DatasetCounts(long institutions, long roles, long executingUnits, long organizationalUnits,
            long users, long scopes) {}
}
