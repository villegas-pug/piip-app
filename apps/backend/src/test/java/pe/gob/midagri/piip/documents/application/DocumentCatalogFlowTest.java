package pe.gob.midagri.piip.documents.application;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.config.PiipProperties;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

class DocumentCatalogFlowTest {
    @Test void conservaCargaDeDosVersionesPublicacionYNoAplica() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentVersionRepository versions = mock(DocumentVersionRepository.class);
        DocumentContentRepository contents = mock(DocumentContentRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        AuditService audit = mock(AuditService.class);
        InstitutionEntity institution = new InstitutionEntity("I2", "Institución");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE2", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 15L);
        PortfolioRecordEntity record = PortfolioRecordTestBuilder.transientReferences().initiative("I-02", unit, "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 17L);
        DocumentTypeEntity type = new DocumentTypeEntity("OPINION", "Informe de opinión", 20, true);
        ReflectionTestUtils.setField(type, "id", 19L);
        DocumentEntity slot = new DocumentEntity(record, type);
        ReflectionTestUtils.setField(slot, "id", 20L);
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(documents.findByRecordIdAndTypeId(17L, 19L)).thenReturn(Optional.of(slot));
        when(authorization.requireUnit(pe.gob.midagri.piip.identity.domain.RoleCode.ADMINISTRADOR_PIIP, 15L))
            .thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        AtomicInteger ids = new AtomicInteger(30);
        AtomicReference<DocumentVersionEntity> latest = new AtomicReference<>();
        when(versions.save(any())).thenAnswer(invocation -> {
            DocumentVersionEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", (long) ids.incrementAndGet());
            latest.set(value); return value;
        });
        var scopes = mock(pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository.class);
        when(scopes.findActiveRecipients(any(), any(), any(), any())).thenReturn(List.of());
        DocumentService service = new DocumentService(records, documents, versions, contents, scopes,
            mock(NotificationRepository.class), authorization, audit, new PiipProperties.Documents(1024), mock(DocumentTypeRepository.class));
        MockMultipartFile first = new MockMultipartFile("file", "uno.pdf", "application/pdf", new byte[] {1});
        MockMultipartFile second = new MockMultipartFile("file", "dos.pdf", "application/pdf", new byte[] {2});

        service.upload("I-02", 19L, first);
        service.upload("I-02", 19L, second);
        when(versions.findById(latest.get().getId())).thenReturn(Optional.of(latest.get()));
        service.publish(latest.get().getId(), true, 0L);
        service.markNotApplicable("I-02", 19L, "No aplica en esta etapa");

        assertThat(slot.getLatestVersion()).isEqualTo(2);
        assertThat(latest.get().isExternallyPublished()).isTrue();
        assertThat(slot.getState()).isEqualTo(pe.gob.midagri.piip.documents.domain.DocumentState.NOT_APPLICABLE);
        verify(contents, times(2)).save(any());
    }

    @Test void operaSlotHistoricoInactivoSinRevalidarloComoReferenciaNueva() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentTypeRepository types = mock(DocumentTypeRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        AuditService audit = mock(AuditService.class);
        InstitutionEntity institution = new InstitutionEntity("I", "Institución");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 5L);
        PortfolioRecordEntity record = PortfolioRecordTestBuilder.transientReferences().initiative("I-01", unit, "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 7L);
        DocumentTypeEntity inactive = new DocumentTypeEntity("OLD", "Tipo histórico", 10, false);
        ReflectionTestUtils.setField(inactive, "id", 9L);
        DocumentEntity slot = new DocumentEntity(record, inactive);
        when(records.findByCodeIgnoreCase("I-01")).thenReturn(Optional.of(record));
        when(documents.findByRecordIdAndTypeId(7L, 9L)).thenReturn(Optional.of(slot));
        when(authorization.requireUnit(pe.gob.midagri.piip.identity.domain.RoleCode.ADMINISTRADOR_PIIP, 5L))
            .thenReturn(new LocalAccessContext(1L, "subject", Set.of()));

        DocumentService service = new DocumentService(records, documents, mock(DocumentVersionRepository.class),
            mock(DocumentContentRepository.class), mock(pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository.class),
            mock(NotificationRepository.class), authorization, audit, new PiipProperties.Documents(1024), types);
        service.markNotApplicable("I-01", 9L, "histórico");

        verify(types, never()).findById(anyLong());
        verify(documents, never()).save(any());
    }
}
