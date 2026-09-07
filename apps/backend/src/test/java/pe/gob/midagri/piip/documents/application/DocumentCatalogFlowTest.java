package pe.gob.midagri.piip.documents.application;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.config.PiipProperties;
import pe.gob.midagri.piip.documents.api.DocumentDtos.VersionResponse;
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
        DocumentFileRepository files = mock(DocumentFileRepository.class);
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
        AtomicReference<DocumentFileEntity> original = new AtomicReference<>();
        when(files.findFirstByDocumentIdAndOriginalTrueAndDeletedFalseOrderByIdAsc(20L))
            .thenAnswer(invocation -> Optional.ofNullable(original.get()));
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 21L);
            original.set(value); return value;
        });
        AtomicInteger ids = new AtomicInteger(30);
        List<DocumentVersionEntity> saved = new ArrayList<>();
        AtomicReference<DocumentVersionEntity> latest = new AtomicReference<>();
        when(versions.save(any())).thenAnswer(invocation -> {
            DocumentVersionEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", (long) ids.incrementAndGet());
            saved.add(value); latest.set(value); return value;
        });
        var scopes = mock(pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository.class);
        when(scopes.findActiveRecipients(any(), any(), any(), any())).thenReturn(List.of());
        DocumentService service = new DocumentService(records, documents, files, versions, contents, scopes,
            mock(NotificationRepository.class), authorization, audit, new PiipProperties.Documents(1024), mock(DocumentTypeRepository.class));
        MockMultipartFile first = new MockMultipartFile("file", "uno.pdf", "application/pdf", new byte[] {1});
        MockMultipartFile second = new MockMultipartFile("file", "dos.pdf", "application/pdf", new byte[] {2});

        service.upload("I-02", 19L, first);
        assertThat(slot.getState()).isEqualTo(pe.gob.midagri.piip.documents.domain.DocumentState.LOADED);
        service.upload("I-02", 19L, second);
        when(versions.findById(latest.get().getId())).thenReturn(Optional.of(latest.get()));
        service.publish(latest.get().getId(), true, 0L);
        service.markNotApplicable("I-02", 19L, "No aplica en esta etapa");

        verify(files, times(1)).save(any(DocumentFileEntity.class));
        assertThat(saved).extracting(DocumentVersionEntity::getVersionNumber).containsExactly(1, 2);
        assertThat(saved).allSatisfy(value -> assertThat(value.getFile()).isSameAs(original.get()));
        assertThat(original.get().isOriginal()).isTrue();
        assertThat(original.get().getLatestVersion()).isEqualTo(2);
        assertThat(latest.get().isExternallyPublished()).isTrue();
        assertThat(slot.getState()).isEqualTo(pe.gob.midagri.piip.documents.domain.DocumentState.NOT_APPLICABLE);
        verify(contents, times(2)).save(any());
    }

    @Test void cargaPorTipoSinOriginalActivoCreaNuevoArchivoOriginal() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentFileRepository files = mock(DocumentFileRepository.class);
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
        AtomicReference<DocumentFileEntity> activeOriginal = new AtomicReference<>();
        when(files.findFirstByDocumentIdAndOriginalTrueAndDeletedFalseOrderByIdAsc(20L))
            .thenAnswer(invocation -> Optional.ofNullable(activeOriginal.get()).filter(file -> !file.isDeleted()));
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(21L, 17L))
            .thenAnswer(invocation -> Optional.ofNullable(activeOriginal.get()).filter(file -> !file.isDeleted()));
        when(files.findByDocumentIdAndDeletedFalseOrderByIdAsc(20L)).thenReturn(List.of());
        List<DocumentFileEntity> createdFiles = new ArrayList<>();
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 21L + createdFiles.size());
            createdFiles.add(value); activeOriginal.set(value); return value;
        });
        List<DocumentVersionEntity> savedVersions = new ArrayList<>();
        when(versions.save(any())).thenAnswer(invocation -> {
            DocumentVersionEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 30L + savedVersions.size());
            savedVersions.add(value); return value;
        });
        when(versions.findFirstByFileIdOrderByVersionNumberDesc(21L))
            .thenAnswer(invocation -> savedVersions.stream().filter(value -> value.getFile().getId() == 21L).findFirst());
        var scopes = mock(pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository.class);
        DocumentService service = new DocumentService(records, documents, files, versions, contents, scopes,
            mock(NotificationRepository.class), authorization, audit, new PiipProperties.Documents(1024), mock(DocumentTypeRepository.class));

        service.upload("I-02", 19L, new MockMultipartFile("file", "uno.pdf", "application/pdf", new byte[] {1}));
        assertThat(slot.getState()).isEqualTo(pe.gob.midagri.piip.documents.domain.DocumentState.LOADED);
        service.deleteFile("I-02", 21L);
        assertThat(slot.getState()).isEqualTo(pe.gob.midagri.piip.documents.domain.DocumentState.PENDING);
        VersionResponse response = service.upload("I-02", 19L, new MockMultipartFile("file", "dos.pdf", "application/pdf", new byte[] {2}));

        assertThat(createdFiles).hasSize(2);
        assertThat(createdFiles).allSatisfy(file -> assertThat(file.isOriginal()).isTrue());
        assertThat(createdFiles.get(0).isDeleted()).isTrue();
        assertThat(createdFiles.get(0).getLatestVersion()).isEqualTo(1);
        assertThat(createdFiles.get(1).isDeleted()).isFalse();
        assertThat(createdFiles.get(1).getLatestVersion()).isEqualTo(1);
        assertThat(savedVersions).extracting(DocumentVersionEntity::getVersionNumber).containsExactly(1, 1);
        assertThat(savedVersions.get(0).getFile()).isSameAs(createdFiles.get(0));
        assertThat(savedVersions.get(1).getFile()).isSameAs(createdFiles.get(1));
        assertThat(response.version()).isEqualTo(1);
        assertThat(slot.getState()).isEqualTo(pe.gob.midagri.piip.documents.domain.DocumentState.LOADED);
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

        DocumentService service = new DocumentService(records, documents, mock(DocumentFileRepository.class),
            mock(DocumentVersionRepository.class), mock(DocumentContentRepository.class),
            mock(pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository.class),
            mock(NotificationRepository.class), authorization, audit, new PiipProperties.Documents(1024), types);
        service.markNotApplicable("I-01", 9L, "histórico");

        verify(types, never()).findById(anyLong());
        verify(documents, never()).save(any());
    }
}
