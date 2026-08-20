package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.catalogs.application.CatalogReferenceService;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.InitiativeStatusTransitionRequest;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.api.BusinessRuleException;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioInitiativeStatusServiceTest {
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
    private PortfolioService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioService(records, responsibleUnits, executingUnits, organizationalUnits, users, tasks,
            notifications, documents, codes, authorization, audit, catalogReferences, documentTypes);
        lenient().when(responsibleUnits.findByRecordIdOrderByDisplayOrder(any())).thenReturn(List.of());
    }

    @Test
    void allowsValidTransitionWithoutInspectingPendingDocuments() {
        PortfolioRecordEntity initiative = initiative(1L, 10L);
        when(records.findByCodeIgnoreCaseForUpdate("I-001-2026")).thenReturn(Optional.of(initiative));
        when(records.existsByOriginRecordId(1L)).thenReturn(false);
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 10L)).thenReturn(actor());

        var response = service.transitionInitiativeStatus("I-001-2026",
            new InitiativeStatusTransitionRequest(0L, PortfolioStatus.INITIATIVE_ARCHIVED, "observación"));

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_ARCHIVED);
        assertThat(response.status()).isEqualTo("Iniciativa archivada");
        verify(audit).event(eq("ESTADO_INICIATIVA_CAMBIADO"), eq("REGISTRO_PORTAFOLIO"), eq("I-001-2026"), anyMap(), eq("subject"));
        verifyNoInteractions(documents);
    }

    @Test
    void rejectsAnyTransitionAfterProjectIsLinkedWithoutSuccessAudit() {
        PortfolioRecordEntity initiative = initiative(1L, 10L);
        when(records.findByCodeIgnoreCaseForUpdate("I-001-2026")).thenReturn(Optional.of(initiative));
        when(records.existsByOriginRecordId(1L)).thenReturn(true);
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 10L)).thenReturn(actor());

        assertThatThrownBy(() -> service.transitionInitiativeStatus("I-001-2026",
            new InitiativeStatusTransitionRequest(0L, PortfolioStatus.INITIATIVE_ARCHIVED, null)))
            .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(audit);
    }

    @Test
    void rejectsAnUnauthorizedExecutingUnitBeforeMutation() {
        PortfolioRecordEntity initiative = initiative(1L, 10L);
        when(records.findByCodeIgnoreCaseForUpdate("I-001-2026")).thenReturn(Optional.of(initiative));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 10L))
            .thenThrow(new AccessDeniedException("fuera de ámbito"));

        assertThatThrownBy(() -> service.transitionInitiativeStatus("I-001-2026",
            new InitiativeStatusTransitionRequest(0L, PortfolioStatus.INITIATIVE_ARCHIVED, null)))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.PRESENTED);
        verifyNoInteractions(audit);
    }

    private PortfolioRecordEntity initiative(Long id, Long unitId) {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ReflectionTestUtils.setField(institution, "id", 100L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-001", "Unidad Ejecutora");
        ReflectionTestUtils.setField(unit, "id", unitId);
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences().initiative("I-001-2026", unit, "Iniciativa");
        ReflectionTestUtils.setField(initiative, "id", id);
        return initiative;
    }

    private LocalAccessContext actor() {
        return new LocalAccessContext(1L, "subject", Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 100L, 10L)));
    }
}
