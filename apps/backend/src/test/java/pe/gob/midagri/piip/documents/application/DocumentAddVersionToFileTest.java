package pe.gob.midagri.piip.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.config.PiipProperties;
import pe.gob.midagri.piip.documents.api.DocumentDtos.VersionResponse;
import pe.gob.midagri.piip.documents.domain.DocumentState;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class DocumentAddVersionToFileTest {
    @Mock PortfolioRecordRepository records;
    @Mock DocumentRepository documents;
    @Mock DocumentFileRepository files;
    @Mock DocumentVersionRepository versions;
    @Mock DocumentContentRepository contents;
    @Mock UserRoleScopeRepository scopes;
    @Mock NotificationRepository notifications;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;
    @Mock DocumentTypeRepository documentTypes;
    @Captor ArgumentCaptor<Map<String, ?>> auditDetail;
    private PortfolioRecordEntity record; private DocumentEntity slot;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(records, documents, files, versions, contents, scopes, notifications,
            authorization, audit, new PiipProperties.Documents(1024), documentTypes);
        InstitutionEntity institution = new InstitutionEntity("I2", "Institución");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE2", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 15L);
        record = PortfolioRecordTestBuilder.transientReferences().initiative("I-02", unit, "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 17L);
        DocumentTypeEntity type = new DocumentTypeEntity("OPINION", "Informe de opinión", 20, true);
        ReflectionTestUtils.setField(type, "id", 19L);
        slot = new DocumentEntity(record, type);
        ReflectionTestUtils.setField(slot, "id", 20L);
    }

    @Test void nuevaVersionDeUnArchivoNoTocaElOtroArchivo() {
        DocumentFileEntity fileA = file(slot, 21L, true);
        DocumentVersionEntity firstA = version(fileA, 31L, 1, "a1.pdf", "sha-a1");
        DocumentFileEntity fileB = file(slot, 22L, false);
        DocumentVersionEntity firstB = version(fileB, 32L, 1, "b1.pdf", "sha-b1");
        firstB.publish("subject");
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(21L, 17L)).thenReturn(Optional.of(fileA));
        List<DocumentVersionEntity> saved = new ArrayList<>();
        when(versions.save(any())).thenAnswer(invocation -> {
            DocumentVersionEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 40L);
            saved.add(value); return value;
        });

        VersionResponse response = service.addVersionToFile("I-02", 21L, upload("a2.pdf", new byte[] {2}));

        assertThat(response.version()).isEqualTo(2);
        assertThat(fileA.getLatestVersion()).isEqualTo(2);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getVersionNumber()).isEqualTo(2);
        assertThat(saved.get(0).getFile()).isSameAs(fileA);
        assertThat(List.of(firstA, saved.get(0))).allSatisfy(value -> assertThat(value.getFile()).isSameAs(fileA));
        assertThat(fileB.getLatestVersion()).isEqualTo(1);
        assertThat(firstB.isExternallyPublished()).isTrue();
        assertThat(firstB.getFilename()).isEqualTo("b1.pdf");
        assertThat(firstB.getChecksumSha256()).isEqualTo("sha-b1");
        assertThat(firstB.getSizeBytes()).isEqualTo(1L);
        assertThat(firstB.getOptimisticVersion()).isZero();
        assertThat(slot.getState()).isEqualTo(DocumentState.LOADED);
        verify(contents, times(1)).save(any());
        verify(audit, times(1)).event(eq("DOCUMENTO_CARGADO"), eq("REGISTRO_PORTAFOLIO"), eq("I-02"), auditDetail.capture(), eq("subject"));
        assertThat(auditDetail.getAllValues().get(0).get("archivoId")).isEqualTo(21L);
    }

    @Test void rechazaArchivoInexistenteEliminadoODeOtroExpediente() {
        DocumentFileEntity deleted = file(slot, 23L, false);
        deleted.markDeleted("subject");
        assertThat(deleted.isDeleted()).isTrue();
        PortfolioRecordEntity otherRecord = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-99", slot.getRecord().getExecutingUnit(), "Otro expediente");
        ReflectionTestUtils.setField(otherRecord, "id", 99L);
        DocumentEntity otherSlot = new DocumentEntity(otherRecord, slot.getType());
        ReflectionTestUtils.setField(otherSlot, "id", 98L);
        DocumentFileEntity foreign = file(otherSlot, 24L, true);
        assertThat(foreign.getDocument().getRecord().getId()).isEqualTo(99L);
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(99L, 17L)).thenReturn(Optional.empty());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(23L, 17L)).thenReturn(Optional.empty());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(24L, 17L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addVersionToFile("I-02", 99L, upload("informe.pdf", new byte[] {1})))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("no existe");
        assertThatThrownBy(() -> service.addVersionToFile("I-02", 23L, upload("informe.pdf", new byte[] {1})))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("no existe");
        assertThatThrownBy(() -> service.addVersionToFile("I-02", 24L, upload("informe.pdf", new byte[] {1})))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("no existe");

        verify(files, never()).save(any());
        verifyNoInteractions(documents, versions, contents, audit);
    }

    @Test void rechazaEscrituraSinAdministracionDeLaUnidadEjecutora() {
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L))
            .thenThrow(new AccessDeniedException("La Unidad Ejecutora está fuera del ámbito autorizado para el rol ADMINISTRADOR_PIIP"));

        assertThatThrownBy(() -> service.addVersionToFile("I-02", 21L, upload("informe.pdf", new byte[] {1})))
            .isInstanceOf(AccessDeniedException.class).hasMessageContaining("fuera del ámbito");

        verifyNoInteractions(documents, files, versions, contents, audit);
    }

    private DocumentFileEntity file(DocumentEntity document, Long id, boolean original) {
        DocumentFileEntity value = new DocumentFileEntity(document, original);
        ReflectionTestUtils.setField(value, "id", id);
        value.registerUpload();
        return value;
    }
    private DocumentVersionEntity version(DocumentFileEntity file, Long id, int number, String filename, String checksum) {
        DocumentVersionEntity value = new DocumentVersionEntity(file, number, filename, "application/pdf", 1L, checksum, "subject");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
    private DocumentUploadInput upload(String filename, byte[] content) { return new DocumentUploadInput(filename, "application/pdf", content.length, () -> content); }
}
