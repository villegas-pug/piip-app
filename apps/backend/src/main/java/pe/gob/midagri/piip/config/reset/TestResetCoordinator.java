package pe.gob.midagri.piip.config.reset;

import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.gob.midagri.piip.audit.persistence.*;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.work.persistence.*;

@Component
@Profile("test-reset")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestResetCoordinator implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestResetCoordinator.class);
    private final TestResetEnvironmentGuard guard;
    private final HibernateMetadataCapture capture;
    private final TestResetSchemaFilterProvider filters;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final InstitutionRepository institutions; private final ExecutingUnitRepository executingUnits;
    private final OrganizationalUnitRepository organizationalUnits; private final RoleRepository roles;
    private final UserRepository users; private final UserRoleScopeRepository scopes;
    private final AuditEventRepository auditEvents; private final AccessAuditRepository accessAudits;
    private final NotificationRepository notifications; private final CatalogRepository catalogs;
    private final CatalogItemRepository catalogItems; private final DocumentTypeRepository documentTypes;
    private final PortfolioRecordRepository portfolios; private final ResponsibleUnitRepository responsibleUnits;
    private final CodeCounterRepository codeCounters; private final DocumentRepository documents;
    private final DocumentVersionRepository documentVersions; private final DocumentContentRepository documentContents;
    private final WorkTaskRepository workTasks;
    private volatile boolean preflightCompleted;

    public TestResetCoordinator(TestResetEnvironmentGuard guard, HibernateMetadataCapture capture,
            TestResetSchemaFilterProvider filters, EntityManagerFactory entityManagerFactory, DataSource dataSource,
            InstitutionRepository institutions, ExecutingUnitRepository executingUnits, OrganizationalUnitRepository organizationalUnits,
            RoleRepository roles, UserRepository users, UserRoleScopeRepository scopes, AuditEventRepository auditEvents,
            AccessAuditRepository accessAudits, NotificationRepository notifications, CatalogRepository catalogs,
            CatalogItemRepository catalogItems, DocumentTypeRepository documentTypes, PortfolioRecordRepository portfolios,
            ResponsibleUnitRepository responsibleUnits, CodeCounterRepository codeCounters, DocumentRepository documents,
            DocumentVersionRepository documentVersions, DocumentContentRepository documentContents, WorkTaskRepository workTasks) {
        this.guard = guard; this.capture = capture; this.filters = filters; this.entityManagerFactory = entityManagerFactory; this.dataSource = dataSource;
        this.institutions = institutions; this.executingUnits = executingUnits; this.organizationalUnits = organizationalUnits;
        this.roles = roles; this.users = users; this.scopes = scopes; this.auditEvents = auditEvents; this.accessAudits = accessAudits;
        this.notifications = notifications; this.catalogs = catalogs; this.catalogItems = catalogItems; this.documentTypes = documentTypes;
        this.portfolios = portfolios; this.responsibleUnits = responsibleUnits; this.codeCounters = codeCounters;
        this.documents = documents; this.documentVersions = documentVersions; this.documentContents = documentContents; this.workTasks = workTasks;
    }

    @Override public void run(ApplicationArguments args) {
        stage(TestResetStage.PREFLIGHT.name(), () -> guard.preflight());
        Metadata metadata = capture.requireMetadata();
        stage(TestResetStage.PREFLIGHT.name() + ":METADATA", () -> validateMetadata(metadata));
        preflightCompleted = true;
        SessionFactoryImplementor factory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        SchemaManagementTool schema = factory.getServiceRegistry().getService(SchemaManagementTool.class);
        if (schema == null) throw new IllegalStateException("Hibernate SchemaManagementTool no está disponible");
        Map<String, Object> configuration = new HashMap<>(factory.getProperties());
        SourceDescriptor source = source(); TargetDescriptor target = target();

        for (String table : TestResetSchemaFilterProvider.DROP_ORDER) {
            filters.select(table);
            stage(TestResetStage.DROP.name() + ":" + table, () -> schema.getSchemaDropper(configuration).doDrop(metadata,
                options(configuration, TestResetStage.DROP, table), ContributableMatcher.ALL, source, target));
        }
        for (String table : TestResetSchemaFilterProvider.CREATE_ORDER) {
            filters.select(table);
            stage(TestResetStage.CREATE.name() + ":" + table, () -> schema.getSchemaCreator(configuration).doCreation(metadata,
                options(configuration, TestResetStage.CREATE, table), ContributableMatcher.ALL, source, target));
        }
        stage(TestResetStage.VALIDATE_EMPTY.name(), () -> {
            if (!operationalTablesAreEmpty()) {
                throw new IllegalStateException("Las tablas operativas, auditoría y notificaciones deben quedar vacías antes del seed");
            }
        });
        stage(TestResetStage.SEED.name(), () -> new ResourceDatabasePopulator(new ClassPathResource("db/test/catalog-data.sql")).execute(dataSource));
        stage(TestResetStage.POST_VALIDATION.name(), () -> {
            if (institutions.count() != 1 || executingUnits.count() != 2 || organizationalUnits.count() != 4
                    || roles.count() != 2 || users.count() != 1 || scopes.count() != 2
                    || catalogs.count() != 4 || catalogItems.count() != 17 || documentTypes.count() != 6) {
                throw new IllegalStateException("El seed de identidad, organización o catálogos quedó incompleto");
            }
            if (!operationalTablesAreEmpty()) throw new IllegalStateException("Las tablas operativas, auditoría o notificaciones recibieron datos durante test-reset");
        });
        LOGGER.info("test-reset completado correctamente sobre el esquema allowlisted");
    }

    private void validateMetadata(Metadata metadata) {
        Set<String> mapped = new TreeSet<>();
        metadata.collectTableMappings().forEach(table -> mapped.add(table.getName().toUpperCase(Locale.ROOT)));
        Set<String> expected = new TreeSet<>(TestResetSchemaFilterProvider.ALLOWLIST);
        if (expected.size() != 19 || !mapped.equals(expected)) throw new IllegalStateException("El Metadata JPA no coincide con las 19 tablas del reset");
        metadata.collectTableMappings().forEach(source -> source.getForeignKeyCollection().forEach(foreignKey -> {
            String targetName = foreignKey.getReferencedTable().getName().toUpperCase(Locale.ROOT);
            if (!expected.contains(targetName)) throw new IllegalStateException("La FK del Metadata apunta fuera de la matriz test-reset");
        }));
    }

    private ExecutionOptions options(Map<String, Object> configuration, TestResetStage currentStage, String currentTable) {
        return new ExecutionOptions() {
            @Override public Map<String, Object> getConfigurationValues() { return configuration; }
            @Override public boolean shouldManageNamespaces() { return false; }
            @Override public ExceptionHandler getExceptionHandler() {
                return exception -> {
                    if (isRecoverableMissingTable(preflightCompleted, currentStage, currentTable, filters.currentTable(), exception)) return;
                    throw exception;
                };
            }
        };
    }
    static int oracleCode(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) return sqlException.getErrorCode();
        }
        return 0;
    }
    static boolean isRecoverableMissingTable(boolean preflightCompleted, TestResetStage stage,
            String requestedTable, String selectedTable, Throwable throwable) {
        return preflightCompleted && stage == TestResetStage.DROP
            && requestedTable != null && requestedTable.equalsIgnoreCase(selectedTable)
            && TestResetSchemaFilterProvider.ALLOWLIST.contains(requestedTable.toUpperCase(Locale.ROOT))
            && oracleCode(throwable) == 942;
    }
    private SourceDescriptor source() { return new SourceDescriptor() {
        @Override public SourceType getSourceType() { return SourceType.METADATA; }
        @Override public ScriptSourceInput getScriptSourceInput() { return null; }
    }; }
    private TargetDescriptor target() { return new TargetDescriptor() {
        @Override public EnumSet<TargetType> getTargetTypes() { return EnumSet.of(TargetType.DATABASE); }
        @Override public ScriptTargetOutput getScriptTargetOutput() { return null; }
    }; }
    private boolean operationalTablesAreEmpty() {
        return portfolios.count() == 0 && responsibleUnits.count() == 0 && codeCounters.count() == 0
            && documents.count() == 0 && documentVersions.count() == 0 && documentContents.count() == 0
            && workTasks.count() == 0 && auditEvents.count() == 0 && accessAudits.count() == 0 && notifications.count() == 0;
    }
    static void stage(String name, Runnable action) {
        try { action.run(); }
        catch (RuntimeException exception) { throw new IllegalStateException("Falló la etapa test-reset " + name, exception); }
    }
}
