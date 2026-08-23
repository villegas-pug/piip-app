package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.persistence.LockModeType;
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
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.ProjectUpdateCommand;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

class PortfolioUpdateConcurrencyTest {
    @Test
    void updateLookupUsesTypedPessimisticWriteLock() throws Exception {
        Lock lock = PortfolioRecordRepository.class
            .getMethod("findByCodeIgnoreCaseAndRecordTypeForUpdate", String.class, RecordType.class)
            .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void staleInitiativePatchIsRejectedBeforeResolvingReferencesOrAuditing() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        CatalogReferenceService references = mock(CatalogReferenceService.class);
        AuditService audit = mock(AuditService.class);
        ExecutingUnitEntity unit = unit(100L);
        PortfolioRecordEntity initiative = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-RACE-2026", unit, "Iniciativa");
        ReflectionTestUtils.setField(initiative, "version", 2L);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("I-RACE-2026", RecordType.INITIATIVE))
            .thenReturn(Optional.of(initiative));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 100L))
            .thenReturn(new LocalAccessContext(1L, "actor", Set.of()));
        InitiativeApplicationService service = new InitiativeApplicationService(records, mock(ResponsibleUnitRepository.class),
            mock(ExecutingUnitRepository.class), mock(OrganizationalUnitRepository.class), mock(UserRepository.class),
            mock(WorkTaskRepository.class), mock(NotificationRepository.class), mock(DocumentRepository.class),
            mock(CodeGeneratorService.class), authorization, audit, references, mock(DocumentTypeRepository.class));

        assertThatThrownBy(() -> service.update("I-RACE-2026", initiativeCommand(1L)))
            .isInstanceOf(StaleVersionException.class);
        verifyNoInteractions(references, audit);
        verify(records, never()).flush();
    }

    @Test
    void staleProjectPatchCannotOverwriteAConcurrentLifecycleVersion() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        ExecutingUnitEntity unit = unit(101L);
        PortfolioRecordEntity project = PortfolioRecordTestBuilder.transientReferences()
            .preexistingProject("P-RACE-2026", unit, "Proyecto");
        ReflectionTestUtils.setField(project, "version", 1L);
        when(records.findByCodeIgnoreCaseAndRecordTypeForUpdate("P-RACE-2026", RecordType.PROJECT))
            .thenReturn(Optional.of(project));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 101L))
            .thenReturn(new LocalAccessContext(1L, "actor", Set.of()));
        ProjectApplicationService service = new ProjectApplicationService(records, mock(ResponsibleUnitRepository.class),
            mock(ExecutingUnitRepository.class), mock(WorkTaskRepository.class), mock(NotificationRepository.class),
            mock(DocumentRepository.class), mock(CodeGeneratorService.class), authorization, mock(AuditService.class),
            mock(CatalogReferenceService.class), mock(DocumentTypeRepository.class));

        assertThatThrownBy(() -> service.update("P-RACE-2026", projectCommand(0L)))
            .isInstanceOf(StaleVersionException.class);
        assertThat(project.getName()).isEqualTo("Proyecto");
    }

    private InitiativeUpdateCommand initiativeCommand(long version) {
        return new InitiativeUpdateCommand(version, FieldUpdate.of("Cambio"), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent());
    }

    private ProjectUpdateCommand projectCommand(long version) {
        return new ProjectUpdateCommand(version, FieldUpdate.of("Cambio"), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(),
            FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent(), FieldUpdate.absent());
    }

    private ExecutingUnitEntity unit(Long id) {
        InstitutionEntity institution = new InstitutionEntity("INST-RACE-" + id, "Institución");
        ReflectionTestUtils.setField(institution, "id", id + 1000L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-RACE-" + id, "Unidad");
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }
}
