package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.ResponsibleUnitUpdate;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
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
    @Captor ArgumentCaptor<Map<String, ?>> auditDetailCaptor;

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

    @Test
    void editingOtherFieldsRetainsInactiveHistoricalAssociationAsContextWithoutRevalidation() {
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-HIST-INA-2026", unit, "Iniciativa histórica con inactiva");
        ReflectionTestUtils.setField(initiative, "id", 106L);
        OrganizationalUnitEntity inactiveUnit = new OrganizationalUnitEntity(unit, "UO-HIST-INA", "Unidad histórica inactiva", "UHI");
        OrganizationalUnitEntity activeUnit = new OrganizationalUnitEntity(unit, "UO-HIST-ACT", "Unidad histórica activa", "UHA");
        ReflectionTestUtils.setField(inactiveUnit, "id", 83L);
        ReflectionTestUtils.setField(activeUnit, "id", 84L);
        ReflectionTestUtils.setField(inactiveUnit, "active", false);
        ResponsibleUnitEntity first = new ResponsibleUnitEntity(initiative, inactiveUnit, inactiveUnit.getName(), 1);
        ResponsibleUnitEntity second = new ResponsibleUnitEntity(initiative, activeUnit, activeUnit.getName(), 2);
        List<ResponsibleUnitEntity> historical = List.of(first, second);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-HIST-INA-2026", pe.gob.midagri.piip.portfolio.domain.RecordType.INITIATIVE))
            .thenReturn(Optional.of(initiative));
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(106L)).thenReturn(historical);

        initiatives.update("I-HIST-INA-2026", new InitiativeUpdateCommand(0L,
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.of("Nota sobre histórico con inactiva"), FieldUpdate.absent()));

        // La asociación histórica inactiva permanece como contexto: sin revalidación, sin migración ni renumeración.
        assertThat(initiative.getNote()).isEqualTo("Nota sobre histórico con inactiva");
        assertThat(historical).containsExactly(first, second);
        assertThat(historical).extracting(ResponsibleUnitEntity::getDisplayOrder).containsExactly(1, 2);
        verify(responsibleUnits, never()).save(any());
        verify(responsibleUnits, never()).deleteAll(any(Iterable.class));
        verifyNoInteractions(organizationalUnits);
    }

    @Test
    @SuppressWarnings("unchecked")
    void replacingListAuditsPreviousAndNewConfirmedValuesWithSiglaAndNro() {
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-REPL-2026", unit, "Iniciativa con reemplazo auditado");
        ReflectionTestUtils.setField(initiative, "id", 107L);
        OrganizationalUnitEntity retainedInactive = new OrganizationalUnitEntity(unit, "UO-REPL-1", "Unidad retenida", "UR1");
        OrganizationalUnitEntity nueva = new OrganizationalUnitEntity(unit, "UO-REPL-2", "Unidad nueva", "UR2");
        ReflectionTestUtils.setField(retainedInactive, "id", 81L);
        ReflectionTestUtils.setField(nueva, "id", 82L);
        ReflectionTestUtils.setField(retainedInactive, "active", false);
        ResponsibleUnitEntity historical = new ResponsibleUnitEntity(initiative, retainedInactive, retainedInactive.getName(), 1);
        ResponsibleUnitEntity confirmedFirst = new ResponsibleUnitEntity(initiative, retainedInactive, retainedInactive.getName(), 1);
        ResponsibleUnitEntity confirmedSecond = new ResponsibleUnitEntity(initiative, nueva, nueva.getName(), 2);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-REPL-2026", pe.gob.midagri.piip.portfolio.domain.RecordType.INITIATIVE))
            .thenReturn(Optional.of(initiative));
        when(organizationalUnits.findHistoricalById(81L)).thenReturn(Optional.of(retainedInactive));
        when(organizationalUnits.findHistoricalById(82L)).thenReturn(Optional.of(nueva));
        // Tres lecturas consecutivas: snapshot anterior, conjunto vigente en replace y snapshot nuevo.
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(107L))
            .thenReturn(List.of(historical), List.of(historical), List.of(confirmedFirst, confirmedSecond));

        initiatives.update("I-REPL-2026", new InitiativeUpdateCommand(0L,
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.of(List.of(
                new ResponsibleUnitUpdate(81L), new ResponsibleUnitUpdate(82L))),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent()));

        var saved = ArgumentCaptor.forClass(ResponsibleUnitEntity.class);
        verify(responsibleUnits).deleteAll(List.of(historical));
        verify(responsibleUnits, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(unitValue -> unitValue.getOrganizationalUnit().getId())
            .containsExactly(81L, 82L);
        assertThat(saved.getAllValues()).extracting(ResponsibleUnitEntity::getDisplayOrder).containsExactly(1, 2);
        verify(audit).event(eq("INICIATIVA_ACTUALIZADA"), eq("REGISTRO_PORTAFOLIO"), eq("I-REPL-2026"),
            auditDetailCaptor.capture(), eq("actor-update"));
        // K=String en los maps auditados para que AssertJ acepte las claves varargs de contains*.
        Map<String, ?> detail = auditDetailCaptor.getValue();
        Map<String, ?> changes = (Map<String, ?>) detail.get("cambios");
        assertThat(changes).containsOnlyKeys("responsibleUnits");
        Map<?, ?> unitChange = (Map<?, ?>) changes.get("responsibleUnits");
        List<?> anterior = (List<?>) unitChange.get("anterior");
        List<?> nuevo = (List<?>) unitChange.get("nuevo");
        assertThat(anterior).hasSize(1);
        assertThat(nuevo).hasSize(2);
        assertThat((Map<String, Object>) anterior.get(0)).containsEntry("id", 81L).containsEntry("sigla", "UR1").containsEntry("nro", 1);
        assertThat((Map<String, Object>) nuevo.get(0)).containsEntry("id", 81L).containsEntry("sigla", "UR1").containsEntry("nro", 1);
        assertThat((Map<String, Object>) nuevo.get(1)).containsEntry("id", 82L).containsEntry("sigla", "UR2").containsEntry("nro", 2);
        assertThat(detail).doesNotContainKeys("request", "body", "token", "motivo");
    }

    @Test
    void rejectedIncorporationsLeaveAssociationsIntactAndEmitNoSuccessfulUpdateEvent() {
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-REJ-2026", unit, "Iniciativa con incorporaciones rechazadas");
        ReflectionTestUtils.setField(initiative, "id", 108L);
        OrganizationalUnitEntity retained = new OrganizationalUnitEntity(unit, "UO-REJ-1", "Unidad retenida", "URJ");
        OrganizationalUnitEntity inactive = new OrganizationalUnitEntity(unit, "UO-REJ-2", "Unidad inactiva", "UIN");
        OrganizationalUnitEntity outside = new OrganizationalUnitEntity(otherExecutingUnit(), "UO-REJ-3", "Unidad ajena", "UAJ");
        OrganizationalUnitEntity withoutAcronym = new OrganizationalUnitEntity(unit, "UO-REJ-4", "Unidad sin sigla", null);
        ReflectionTestUtils.setField(retained, "id", 85L);
        ReflectionTestUtils.setField(inactive, "id", 91L);
        ReflectionTestUtils.setField(outside, "id", 92L);
        ReflectionTestUtils.setField(withoutAcronym, "id", 93L);
        ReflectionTestUtils.setField(inactive, "active", false);
        ResponsibleUnitEntity historical = new ResponsibleUnitEntity(initiative, retained, retained.getName(), 1);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-REJ-2026", pe.gob.midagri.piip.portfolio.domain.RecordType.INITIATIVE))
            .thenReturn(Optional.of(initiative));
        when(organizationalUnits.findHistoricalById(85L)).thenReturn(Optional.of(retained));
        when(organizationalUnits.findHistoricalById(91L)).thenReturn(Optional.of(inactive));
        when(organizationalUnits.findHistoricalById(92L)).thenReturn(Optional.of(outside));
        when(organizationalUnits.findHistoricalById(93L)).thenReturn(Optional.of(withoutAcronym));
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(108L)).thenReturn(List.of(historical));
        ResponsibleUnitUpdate retainedInput = new ResponsibleUnitUpdate(85L);

        // Las nuevas incorporaciones sí se validan: vigencia, pertenencia y sigla (FR-024).
        assertThatThrownBy(() -> initiatives.update("I-REJ-2026", replacementCommand(retainedInput, new ResponsibleUnitUpdate(91L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception ->
                assertThat(exception.getReason()).isEqualTo("INACTIVE"));
        assertThatThrownBy(() -> initiatives.update("I-REJ-2026", replacementCommand(retainedInput, new ResponsibleUnitUpdate(92L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception ->
                assertThat(exception.getReason()).isEqualTo("OUTSIDE_EXECUTING_UNIT"));
        assertThatThrownBy(() -> initiatives.update("I-REJ-2026", replacementCommand(retainedInput, new ResponsibleUnitUpdate(93L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception ->
                assertThat(exception.getReason()).isEqualTo("MISSING_ACRONYM"));

        // Operación rechazada: sin cambios parciales y sin evento de modificación exitosa (FR-021).
        verify(responsibleUnits, never()).deleteAll(any(Iterable.class));
        verify(responsibleUnits, never()).save(any());
        verify(audit, never()).event(any(), any(), any(), any(), any());
    }

    private InitiativeUpdateCommand replacementCommand(ResponsibleUnitUpdate first, ResponsibleUnitUpdate second) {
        return new InitiativeUpdateCommand(0L, FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.of(List.of(first, second)), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent());
    }

    private ExecutingUnitEntity otherExecutingUnit() {
        InstitutionEntity institution = new InstitutionEntity("INST-OTHER", "Institución ajena");
        ReflectionTestUtils.setField(institution, "id", 99L);
        ExecutingUnitEntity value = new ExecutingUnitEntity(institution, "UE-OTHER", "Unidad Ejecutora ajena");
        ReflectionTestUtils.setField(value, "id", 9L);
        return value;
    }
}
