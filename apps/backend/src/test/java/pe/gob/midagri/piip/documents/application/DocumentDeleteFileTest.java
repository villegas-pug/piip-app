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
import pe.gob.midagri.piip.documents.api.DocumentDtos.DocumentResponse;
import pe.gob.midagri.piip.documents.api.DocumentDtos.DownloadResponse;
import pe.gob.midagri.piip.documents.api.DocumentDtos.VersionResponse;
import pe.gob.midagri.piip.documents.domain.DocumentState;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
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
class DocumentDeleteFileTest {
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
        ReflectionTestUtils.setField(institution, "id", 14L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE2", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 15L);
        record = PortfolioRecordTestBuilder.transientReferences().initiative("I-02", unit, "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 17L);
        DocumentTypeEntity type = new DocumentTypeEntity("OPINION", "Informe de opinión", 20, true);
        ReflectionTestUtils.setField(type, "id", 19L);
        slot = new DocumentEntity(record, type);
        ReflectionTestUtils.setField(slot, "id", 20L);
    }

    @Test void eliminarUnArchivoOcultaSoloSusVersionesSinTocarElOtro() {
        slot.registerUpload();
        DocumentFileEntity fileA = file(slot, 21L, true);
        DocumentVersionEntity firstA = version(fileA, 31L, 1, "a1.pdf", "sha-a1");
        DocumentFileEntity fileB = file(slot, 22L, false);
        DocumentVersionEntity firstB = version(fileB, 32L, 1, "b1.pdf", "sha-b1");
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(internal());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(21L, 17L)).thenReturn(Optional.of(fileA));
        when(versions.findFirstByFileIdOrderByVersionNumberDesc(21L)).thenReturn(Optional.of(firstA));
        when(files.findByDocumentIdAndDeletedFalseOrderByIdAsc(20L)).thenReturn(List.of(fileB));
        when(authorization.requireReadableUnit(15L)).thenReturn(internal());
        when(documents.findByRecordIdOrderByTypeDisplayOrderAscTypeCodeAsc(17L)).thenReturn(List.of(slot));
        when(versions.findByDocumentIdOrderByVersionNumberDesc(20L)).thenReturn(List.of(firstB, firstA));
        when(versions.findById(31L)).thenReturn(Optional.of(firstA));
        when(versions.findById(32L)).thenReturn(Optional.of(firstB));
        when(contents.findByDocumentVersionId(32L)).thenReturn(Optional.of(new DocumentContentEntity(firstB, new byte[] {1})));
        when(scopes.findActiveRecipients(any(), any(), any(), any())).thenReturn(List.of());

        service.deleteFile("I-02", 21L);

        assertThat(fileA.isDeleted()).isTrue();
        assertThat(fileA.getDeletedBy()).isEqualTo("subject");
        assertThat(fileA.getDeletedAt()).isNotNull();
        assertThat(fileB.isDeleted()).isFalse();
        assertThat(fileB.getLatestVersion()).isEqualTo(1);
        assertThat(firstA.getFilename()).isEqualTo("a1.pdf");
        assertThat(firstA.getChecksumSha256()).isEqualTo("sha-a1");
        assertThat(firstA.isExternallyPublished()).isFalse();
        assertThat(slot.getState()).isEqualTo(DocumentState.LOADED);
        verify(contents, never()).save(any());
        assertThatThrownBy(() -> service.download(31L)).isInstanceOf(InvalidReferenceException.class);
        assertThatThrownBy(() -> service.publish(31L, true, 0L)).isInstanceOf(InvalidReferenceException.class);

        List<DocumentResponse> listing = service.list("I-02");

        assertThat(listing).hasSize(1);
        DocumentResponse position = listing.get(0);
        assertThat(position.state()).isEqualTo(DocumentState.LOADED);
        assertThat(position.files()).hasSize(1);
        assertThat(position.files().get(0).id()).isEqualTo(22L);
        assertThat(position.files().get(0).current().version()).isEqualTo(1);
        assertThat(position.files().get(0).versions()).hasSize(1);
        assertThat(position.versions()).isEmpty();
        assertThat(position.latestVersion()).isZero();

        DownloadResponse download = service.download(32L);
        assertThat(download.filename()).isEqualTo("b1.pdf");
        VersionResponse published = service.publish(32L, true, 0L);
        assertThat(published.externallyPublished()).isTrue();
    }

    @Test void consultaExternaNoObtieneVersionesDelArchivoEliminado() {
        slot.registerUpload();
        DocumentFileEntity fileA = file(slot, 21L, true);
        DocumentVersionEntity firstA = version(fileA, 31L, 1, "a1.pdf", "sha-a1");
        firstA.publish("subject");
        DocumentFileEntity fileB = file(slot, 22L, false);
        DocumentVersionEntity firstB = version(fileB, 32L, 1, "b1.pdf", "sha-b1");
        firstB.publish("subject");
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(internal());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(21L, 17L)).thenReturn(Optional.of(fileA));
        when(versions.findFirstByFileIdOrderByVersionNumberDesc(21L)).thenReturn(Optional.of(firstA));
        when(files.findByDocumentIdAndDeletedFalseOrderByIdAsc(20L)).thenReturn(List.of(fileB));
        when(authorization.requireReadableUnit(15L)).thenReturn(external());
        when(documents.findByRecordIdOrderByTypeDisplayOrderAscTypeCodeAsc(17L)).thenReturn(List.of(slot));
        when(versions.findByDocumentIdOrderByVersionNumberDesc(20L)).thenReturn(List.of(firstB, firstA));
        when(versions.findById(31L)).thenReturn(Optional.of(firstA));
        when(versions.findById(32L)).thenReturn(Optional.of(firstB));
        when(contents.findByDocumentVersionId(32L)).thenReturn(Optional.of(new DocumentContentEntity(firstB, new byte[] {1})));

        service.deleteFile("I-02", 21L);

        List<DocumentResponse> listing = service.list("I-02");
        assertThat(listing.get(0).files()).hasSize(1);
        assertThat(listing.get(0).files().get(0).id()).isEqualTo(22L);
        assertThat(listing.get(0).files().get(0).current().version()).isEqualTo(1);
        assertThat(listing.get(0).versions()).isEmpty();
        assertThatThrownBy(() -> service.download(31L)).isInstanceOf(InvalidReferenceException.class);
        DownloadResponse download = service.download(32L);
        assertThat(download.filename()).isEqualTo("b1.pdf");
    }

    @Test void auditaLaEliminacionConElDetalleCompleto() {
        slot.registerUpload();
        DocumentFileEntity fileA = file(slot, 21L, true);
        DocumentVersionEntity firstA = version(fileA, 31L, 1, "a1.pdf", "sha-a1");
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(internal());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(21L, 17L)).thenReturn(Optional.of(fileA));
        when(versions.findFirstByFileIdOrderByVersionNumberDesc(21L)).thenReturn(Optional.of(firstA));
        when(files.findByDocumentIdAndDeletedFalseOrderByIdAsc(20L)).thenReturn(List.of());

        service.deleteFile("I-02", 21L);

        verify(audit, times(1)).event(any(), any(), any(), any(), any());
        verify(audit, times(1)).event(eq("DOCUMENTO_ARCHIVO_ELIMINADO"), eq("REGISTRO_PORTAFOLIO"), eq("I-02"), auditDetail.capture(), eq("subject"));
        assertThat(auditDetail.getValue().get("tipoCodigo")).isEqualTo("OPINION");
        assertThat(auditDetail.getValue().get("tipoNombre")).isEqualTo("Informe de opinión");
        assertThat(auditDetail.getValue().get("archivoId")).isEqualTo(21L);
        assertThat(auditDetail.getValue().get("versionVigente")).isEqualTo(1);
        assertThat(auditDetail.getValue().get("nombreVigente")).isEqualTo("a1.pdf");
    }

    @Test void eliminarElUnicoArchivoActivoDejaLaPosicionPendiente() {
        slot.registerUpload();
        DocumentFileEntity fileA = file(slot, 21L, true);
        DocumentVersionEntity firstA = version(fileA, 31L, 1, "a1.pdf", "sha-a1");
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(internal());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(21L, 17L)).thenReturn(Optional.of(fileA));
        when(versions.findFirstByFileIdOrderByVersionNumberDesc(21L)).thenReturn(Optional.of(firstA));
        when(files.findByDocumentIdAndDeletedFalseOrderByIdAsc(20L)).thenReturn(List.of());

        service.deleteFile("I-02", 21L);

        assertThat(fileA.isDeleted()).isTrue();
        assertThat(slot.getState()).isEqualTo(DocumentState.PENDING);
    }

    @Test void conservaLaDeclaracionNoAplicaAlEliminarElUnicoArchivo() {
        slot.markNotApplicable("No aplica en esta etapa");
        DocumentFileEntity fileA = file(slot, 21L, false);
        DocumentVersionEntity firstA = version(fileA, 31L, 1, "a1.pdf", "sha-a1");
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(internal());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(21L, 17L)).thenReturn(Optional.of(fileA));
        when(versions.findFirstByFileIdOrderByVersionNumberDesc(21L)).thenReturn(Optional.of(firstA));
        when(files.findByDocumentIdAndDeletedFalseOrderByIdAsc(20L)).thenReturn(List.of());

        service.deleteFile("I-02", 21L);

        assertThat(fileA.isDeleted()).isTrue();
        assertThat(slot.getState()).isEqualTo(DocumentState.NOT_APPLICABLE);
        assertThat(slot.getNotApplicableReason()).isEqualTo("No aplica en esta etapa");
    }

    @Test void rechazaEliminacionSinAdministracionDeLaUnidadEjecutora() {
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L))
            .thenThrow(new AccessDeniedException("La Unidad Ejecutora está fuera del ámbito autorizado para el rol ADMINISTRADOR_PIIP"));

        assertThatThrownBy(() -> service.deleteFile("I-02", 21L))
            .isInstanceOf(AccessDeniedException.class).hasMessageContaining("fuera del ámbito");

        verifyNoInteractions(files, versions, contents, audit);
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
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(internal());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(99L, 17L)).thenReturn(Optional.empty());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(23L, 17L)).thenReturn(Optional.empty());
        when(files.findByIdAndDocumentRecordIdAndDeletedFalse(24L, 17L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteFile("I-02", 99L))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("no existe");
        assertThatThrownBy(() -> service.deleteFile("I-02", 23L))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("no existe");
        assertThatThrownBy(() -> service.deleteFile("I-02", 24L))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("no existe");

        verify(files, never()).save(any());
        verifyNoInteractions(versions, contents, audit);
    }

    private LocalAccessContext internal() { return new LocalAccessContext(1L, "subject", Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 14L, 15L))); }
    private LocalAccessContext external() { return new LocalAccessContext(2L, "externo", Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 14L, 15L))); }
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
}
