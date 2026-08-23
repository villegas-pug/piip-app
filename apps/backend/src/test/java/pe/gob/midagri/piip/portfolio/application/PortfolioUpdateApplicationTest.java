package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.catalogs.application.CatalogReferenceService;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.FieldUpdate;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.InitiativeUpdateCommand;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.ProjectUpdateCommand;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioUpdateApplicationTest {
    @Mock PortfolioRecordRepository records;
    @Mock ResponsibleUnitRepository responsibleUnits;
    @Mock ExecutingUnitRepository executingUnits;
    @Mock OrganizationalUnitRepository organizationalUnits;
    @Mock UserRepository users;
    @Mock WorkTaskRepository tasks;
    @Mock NotificationRepository notifications;
    @Mock DocumentRepository documents;
    @Mock CodeGeneratorService codes;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;
    @Mock CatalogReferenceService catalogReferences;
    @Mock DocumentTypeRepository documentTypes;

    private InitiativeApplicationService initiatives;
    private ProjectApplicationService projects;
    private ExecutingUnitEntity unit;
    private LocalAccessContext actor;

    @BeforeEach
    void setUp() {
        initiatives = new InitiativeApplicationService(records, responsibleUnits, executingUnits, organizationalUnits,
            users, tasks, notifications, documents, codes, authorization, audit, catalogReferences, documentTypes);
        projects = new ProjectApplicationService(records, responsibleUnits, executingUnits, tasks, notifications,
            documents, codes, authorization, audit, catalogReferences, documentTypes);
        InstitutionEntity institution = new InstitutionEntity("INST-UPDATE", "Institución actualización");
        ReflectionTestUtils.setField(institution, "id", 11L);
        unit = new ExecutingUnitEntity(institution, "UE-UPDATE", "UE actualización");
        ReflectionTestUtils.setField(unit, "id", 7L);
        actor = new LocalAccessContext(3L, "actor-update",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 11L, 7L)));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 7L)).thenReturn(actor);
    }

    @Test
    void updatesInitiativeWithoutChangingTechnicalIdentity() {
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-UPD-2026", unit, "Nombre anterior");
        ReflectionTestUtils.setField(initiative, "id", 101L);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-UPD-2026", pe.gob.midagri.piip.portfolio.domain.RecordType.INITIATIVE))
            .thenReturn(Optional.of(initiative));
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(101L)).thenReturn(List.of());

        var result = initiatives.update("I-UPD-2026", new InitiativeUpdateCommand(0L,
            FieldUpdate.of("Nombre nuevo"), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent()));

        assertThat(initiative.getName()).isEqualTo("Nombre nuevo");
        assertThat(result.code()).isEqualTo("I-UPD-2026");
        assertThat(result.originCode()).isEqualTo("NA");
        assertThat(result.executingUnitId()).isEqualTo(7L);
        verify(records).flush();
        verify(audit).event(eq("INICIATIVA_ACTUALIZADA"), eq("REGISTRO_PORTAFOLIO"), eq("I-UPD-2026"), any(),
            eq("actor-update"));
    }

    @Test
    void editingOtherFieldsPreservesHistoricalMultipleResponsibleUnits() {
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-HIST-2026", unit, "Iniciativa histórica");
        ReflectionTestUtils.setField(initiative, "id", 105L);
        OrganizationalUnitEntity firstUnit = new OrganizationalUnitEntity(unit, "UO-HIST-1", "Unidad histórica 1", "UH1");
        OrganizationalUnitEntity secondUnit = new OrganizationalUnitEntity(unit, "UO-HIST-2", "Unidad histórica 2", "UH2");
        ReflectionTestUtils.setField(firstUnit, "id", 81L);
        ReflectionTestUtils.setField(secondUnit, "id", 82L);
        ResponsibleUnitEntity first = new ResponsibleUnitEntity(initiative, firstUnit, firstUnit.getName(), 1);
        ResponsibleUnitEntity second = new ResponsibleUnitEntity(initiative, secondUnit, secondUnit.getName(), 2);
        List<ResponsibleUnitEntity> historical = List.of(first, second);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-HIST-2026", pe.gob.midagri.piip.portfolio.domain.RecordType.INITIATIVE))
            .thenReturn(Optional.of(initiative));
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(105L)).thenReturn(historical);

        initiatives.update("I-HIST-2026", new InitiativeUpdateCommand(0L,
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.of("Nota histórica actualizada"), FieldUpdate.absent()));

        assertThat(initiative.getNote()).isEqualTo("Nota histórica actualizada");
        assertThat(historical).containsExactly(first, second);
        verify(responsibleUnits, never()).save(any());
        verify(responsibleUnits, never()).deleteAll(any(Iterable.class));
    }

    @Test
    void updatesDerivedProjectKeepingOriginAndExecutingUnit() {
        var fixtures = PortfolioRecordTestBuilder.transientReferences();
        PortfolioRecordEntity initiative = fixtures.initiative("I-ORIGIN-2026", unit, "Origen");
        ReflectionTestUtils.setField(initiative, "id", 102L);
        initiative.approve();
        PortfolioRecordEntity project = fixtures.derivedProject("P-DERIVED-2026", initiative, "Proyecto anterior");
        ReflectionTestUtils.setField(project, "id", 103L);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("P-DERIVED-2026", pe.gob.midagri.piip.portfolio.domain.RecordType.PROJECT))
            .thenReturn(Optional.of(project));
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(103L)).thenReturn(List.of());

        var result = projects.update("P-DERIVED-2026", new ProjectUpdateCommand(0L,
            FieldUpdate.of("Proyecto actualizado"), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent()));

        assertThat(project.getName()).isEqualTo("Proyecto actualizado");
        assertThat(project.getOriginRecord()).isSameAs(initiative);
        assertThat(project.getOriginCode()).isEqualTo("I-ORIGIN-2026");
        assertThat(result.originCode()).isEqualTo("I-ORIGIN-2026");
        assertThat(project.getExecutingUnit()).isSameAs(unit);
    }

    @Test
    void updatesPreexistingProjectWithoutInventingAnOrigin() {
        PortfolioRecordEntity project = PortfolioRecordTestBuilder.transientReferences()
            .preexistingProject("P-PRE-2026", unit, "Proyecto anterior");
        ReflectionTestUtils.setField(project, "id", 104L);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("P-PRE-2026", pe.gob.midagri.piip.portfolio.domain.RecordType.PROJECT))
            .thenReturn(Optional.of(project));
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(104L)).thenReturn(List.of());

        var result = projects.update("P-PRE-2026", new ProjectUpdateCommand(0L,
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.of("Resultado actualizado"), FieldUpdate.absent(), FieldUpdate.absent()));

        assertThat(project.getOriginRecord()).isNull();
        assertThat(result.originCode()).isEqualTo("NA");
        assertThat(project.getKeyResults()).isEqualTo("Resultado actualizado");
    }
}
