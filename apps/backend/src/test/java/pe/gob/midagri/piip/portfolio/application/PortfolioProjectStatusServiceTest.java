package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ProjectStatusTransitionRequest;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.SourceOrigin;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.api.BusinessRuleException;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioProjectStatusServiceTest {
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
            notifications, documents, codes, authorization, audit,
            Clock.fixed(Instant.parse("2026-08-18T23:30:00Z"), ZoneId.of("America/Lima")));
        lenient().when(responsibleUnits.findByRecordIdOrderByDisplayOrder(any())).thenReturn(List.of());
    }

    @Test
    void appliesProjectMatrixAndUsesLimaDateWhenFinished() {
        PortfolioRecordEntity project = project(2L, 10L);
        when(records.findByCodeIgnoreCase("P-001-2026")).thenReturn(Optional.of(project));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 10L)).thenReturn(actor());

        service.transitionProjectStatus("P-001-2026",
            new ProjectStatusTransitionRequest(0L, PortfolioStatus.PRODUCT_APPROVED, "producto"));
        service.transitionProjectStatus("P-001-2026",
            new ProjectStatusTransitionRequest(0L, PortfolioStatus.FINISHED, "cierre"));

        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.FINISHED);
        assertThat(project.getClosingDate()).isEqualTo(LocalDate.of(2026, 8, 18));
        verify(audit, times(2)).event(eq("ESTADO_PROYECTO_CAMBIADO"), eq("REGISTRO_PORTAFOLIO"), eq("P-001-2026"), anyMap(), eq("subject"));
        verifyNoInteractions(documents);
    }

    @Test
    void rejectsNoApplicableAndOtherContextStates() {
        PortfolioRecordEntity project = project(2L, 10L);
        when(records.findByCodeIgnoreCase("P-001-2026")).thenReturn(Optional.of(project));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 10L)).thenReturn(actor());

        assertThatThrownBy(() -> service.transitionProjectStatus("P-001-2026",
            new ProjectStatusTransitionRequest(0L, PortfolioStatus.NOT_APPLICABLE, null)))
            .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(audit);
    }

    private PortfolioRecordEntity project(Long id, Long unitId) {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ReflectionTestUtils.setField(institution, "id", 100L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-001", "Unidad Ejecutora");
        ReflectionTestUtils.setField(unit, "id", unitId);
        PortfolioRecordEntity project = PortfolioRecordEntity.preexistingProject("P-001-2026", unit, "Proyecto", SourceOrigin.OTHER,
            LocalDate.of(2026, 8, 1), "Responsable", null, null, "Descripción", null, null, DigitalComponent.NO, "subject");
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private LocalAccessContext actor() {
        return new LocalAccessContext(1L, "subject", Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 100L, 10L)));
    }
}
