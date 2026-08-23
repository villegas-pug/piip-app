package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
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
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.FieldUpdate;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.InitiativeUpdateCommand;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

class PortfolioUpdateAuthorizationTest {
    @Test
    void doesNotCombineRoleAndUnitCoverageFromDifferentGrants() {
        LocalAccessContext context = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, 100L),
                new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 20L, 200L)));

        assertThat(context.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP, 100L, 10L)).isTrue();
        assertThat(context.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP, 200L, 20L)).isFalse();
        assertThat(context.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP, 200L, 10L)).isFalse();
    }

    @Test
    void rejectsARevokedGrantAtConfirmationBeforeAnyMutation() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        ExecutingUnitEntity unit = unit(100L);
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-AUTH-2026", unit, "Iniciativa");
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-AUTH-2026", RecordType.INITIATIVE))
            .thenReturn(Optional.of(initiative));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 100L))
            .thenThrow(new AccessDeniedException("asignación revocada"));

        InitiativeApplicationService service = service(records, responsible, authorization);
        assertThatThrownBy(() -> service.update("I-AUTH-2026", nameCommand()))
            .isInstanceOf(AccessDeniedException.class);
        org.mockito.Mockito.verifyNoInteractions(responsible);
    }

    @Test
    void rejectsInitiativeOutsideTheEditableStateOrWithADerivedProject() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        ExecutingUnitEntity unit = unit(100L);
        PortfolioRecordEntity approved = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-APPROVED-2026", unit, "Aprobada");
        approved.approve();
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-APPROVED-2026", RecordType.INITIATIVE))
            .thenReturn(Optional.of(approved));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 100L))
            .thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        InitiativeApplicationService service = service(records, responsible, authorization);

        assertThatThrownBy(() -> service.update("I-APPROVED-2026", nameCommand()))
            .isInstanceOf(pe.gob.midagri.piip.shared.application.error.BusinessRuleException.class)
            .hasMessageContaining("estado Presentado");

        PortfolioRecordEntity presented = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-DERIVED-2026", unit, "Con derivado");
        ReflectionTestUtils.setField(presented, "id", 9L);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-DERIVED-2026", RecordType.INITIATIVE))
            .thenReturn(Optional.of(presented));
        when(records.existsByOriginRecordId(9L)).thenReturn(true);
        assertThatThrownBy(() -> service.update("I-DERIVED-2026", nameCommand()))
            .isInstanceOf(pe.gob.midagri.piip.shared.application.error.BusinessRuleException.class)
            .hasMessageContaining("proyecto derivado");
    }

    private InitiativeApplicationService service(PortfolioRecordRepository records,
            ResponsibleUnitRepository responsible, LocalAuthorizationService authorization) {
        return new InitiativeApplicationService(records, responsible, mock(ExecutingUnitRepository.class),
            mock(OrganizationalUnitRepository.class), mock(UserRepository.class), mock(WorkTaskRepository.class),
            mock(NotificationRepository.class), mock(DocumentRepository.class), mock(CodeGeneratorService.class),
            authorization, mock(AuditService.class), mock(CatalogReferenceService.class), mock(DocumentTypeRepository.class));
    }

    private InitiativeUpdateCommand nameCommand() {
        return new InitiativeUpdateCommand(0L, FieldUpdate.of("Nuevo nombre"), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent());
    }

    private ExecutingUnitEntity unit(Long id) {
        InstitutionEntity institution = new InstitutionEntity("INST-AUTH-" + id, "Institución");
        ReflectionTestUtils.setField(institution, "id", id + 1000L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-AUTH-" + id, "Unidad");
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }
}
