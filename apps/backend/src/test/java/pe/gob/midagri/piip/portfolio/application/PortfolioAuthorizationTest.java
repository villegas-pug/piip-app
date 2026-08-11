package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.InitiativeCreateRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ResponsibleUnitInput;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.domain.SolutionType;
import pe.gob.midagri.piip.portfolio.domain.SourceOrigin;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioAuthorizationTest {
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
    private PortfolioService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioService(records, responsibleUnits, executingUnits, organizationalUnits, users, tasks,
            notifications, documents, codes, authorization, audit);
    }

    @Test
    void eligibleInitiativesUseAdministratorCoverageFromTheSameGrant() {
        PortfolioRecordEntity consultationInitiative = approvedInitiative("INI-UE1", 10L, 100L);
        PortfolioRecordEntity administratorInitiative = approvedInitiative("INI-UE2", 20L, 200L);
        LocalAccessContext actor = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
                new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, 200L)));
        when(authorization.require(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(actor);
        when(records.findByRecordTypeAndStatusOrderByUpdatedAtDesc(RecordType.INITIATIVE, PortfolioStatus.INITIATIVE_APPROVED))
            .thenReturn(List.of(consultationInitiative, administratorInitiative));
        when(records.existsByOriginRecordId(2L)).thenReturn(false);
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(2L)).thenReturn(List.of());

        assertThat(service.eligibleInitiatives()).extracting(response -> response.code()).containsExactly("INI-UE2");
    }

    @Test
    void institutionalUserAdministrationCoverageDoesNotAuthorizeFunctionalWritesInAnotherUnit() {
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 100L))
            .thenThrow(new AccessDeniedException("La Unidad Ejecutora está fuera del ámbito autorizado"));
        InitiativeCreateRequest request = new InitiativeCreateRequest(
            100L,
            "Iniciativa UE-001",
            SolutionType.TO_BE_DEFINED,
            SourceOrigin.OTHER,
            LocalDate.now(),
            "Responsable",
            null,
            null,
            "Descripción",
            null,
            DigitalComponent.NO,
            List.of(new ResponsibleUnitInput(null, "Unidad responsable")));

        assertThatThrownBy(() -> service.createInitiative(request))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(records, codes, audit);
    }

    private PortfolioRecordEntity approvedInitiative(String code, Long institutionId, Long unitId) {
        InstitutionEntity institution = new InstitutionEntity("INST-" + institutionId, "Institución");
        ReflectionTestUtils.setField(institution, "id", institutionId);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-" + unitId, "Unidad Ejecutora");
        ReflectionTestUtils.setField(unit, "id", unitId);
        PortfolioRecordEntity record = PortfolioRecordEntity.initiative(code, unit, "Iniciativa", SolutionType.TO_BE_DEFINED,
            SourceOrigin.OTHER, LocalDate.now(), "Responsable", null, null, "Descripción", null,
            DigitalComponent.NO, "subject");
        ReflectionTestUtils.setField(record, "id", unitId.equals(100L) ? 1L : 2L);
        record.approve();
        return record;
    }
}
