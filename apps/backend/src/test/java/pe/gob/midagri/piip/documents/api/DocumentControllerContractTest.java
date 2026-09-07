package pe.gob.midagri.piip.documents.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.time.Instant;
import java.util.List;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.PersistentCatalogItemResponse;
import pe.gob.midagri.piip.documents.application.DocumentService;
import pe.gob.midagri.piip.documents.application.DocumentUploadInput;
import pe.gob.midagri.piip.documents.api.DocumentDtos.DocumentResponse;
import pe.gob.midagri.piip.documents.api.DocumentDtos.FileResponse;
import pe.gob.midagri.piip.documents.api.DocumentDtos.VersionResponse;
import pe.gob.midagri.piip.documents.domain.DocumentState;

@ExtendWith(MockitoExtension.class)
class DocumentControllerContractTest {
    @Mock DocumentService service;

    @Test
    void adaptsMultipartMetadataToTheApplicationUploadInput() {
        VersionResponse expected = new VersionResponse(1L, 1, "file.pdf", "application/pdf", 3L, "sha", Instant.now(), false, 0L);
        when(service.upload(any(), any(), any(DocumentUploadInput.class))).thenReturn(expected);

        var result = new DocumentController(service).upload("INI-001", 2L,
            new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[] {1, 2, 3}));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void adaptsMultipartMetadataToTheApplicationAddFileInput() {
        VersionResponse current = new VersionResponse(9L, 1, "informe.pdf", "application/pdf", 3L, "sha", Instant.now(), false, 0L);
        FileResponse expected = new FileResponse(5L, false, 1, current, List.of(current));
        when(service.addFile(any(), any(), any(DocumentUploadInput.class))).thenReturn(expected);

        var result = new DocumentController(service).addFile("INI-001", 2L,
            new MockMultipartFile("file", "informe.pdf", "application/pdf", new byte[] {1, 2, 3}));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void adaptsMultipartMetadataToTheApplicationAddVersionToFileInput() {
        VersionResponse expected = new VersionResponse(11L, 2, "informe.pdf", "application/pdf", 3L, "sha", Instant.now(), false, 0L);
        when(service.addVersionToFile(any(), any(), any(DocumentUploadInput.class))).thenReturn(expected);

        var result = new DocumentController(service).addVersionToFile("INI-001", 5L,
            new MockMultipartFile("file", "informe.pdf", "application/pdf", new byte[] {1, 2, 3}));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void delegatesFileDeletionToTheApplicationService() {
        new DocumentController(service).deleteFile("INI-001", 5L);

        verify(service).deleteFile("INI-001", 5L);
    }

    @Test
    void keepsTheLegacyTypeUploadRouteEffectAcrossTwoUploads() {
        VersionResponse first = new VersionResponse(31L, 1, "uno.pdf", "application/pdf", 1L, "sha-1", Instant.now(), false, 0L);
        VersionResponse second = new VersionResponse(32L, 2, "dos.pdf", "application/pdf", 2L, "sha-2", Instant.now(), false, 0L);
        when(service.upload(any(), any(), any(DocumentUploadInput.class))).thenReturn(first, second);
        DocumentController controller = new DocumentController(service);

        var firstResult = controller.upload("INI-001", 2L,
            new MockMultipartFile("file", "uno.pdf", "application/pdf", new byte[] {1}));
        var secondResult = controller.upload("INI-001", 2L,
            new MockMultipartFile("file", "dos.pdf", "application/pdf", new byte[] {2}));

        assertThat(firstResult).isEqualTo(first);
        assertThat(firstResult.version()).isEqualTo(1);
        assertThat(secondResult).isEqualTo(second);
        assertThat(secondResult.version()).isEqualTo(2);
    }

    @Test
    void keepsTheLegacyListingShapeWhileExposingFiles() {
        VersionResponse first = new VersionResponse(31L, 1, "uno.pdf", "application/pdf", 1L, "sha-1", Instant.now(), false, 0L);
        VersionResponse second = new VersionResponse(32L, 2, "dos.pdf", "application/pdf", 2L, "sha-2", Instant.now(), false, 0L);
        FileResponse original = new FileResponse(21L, true, 2, second, List.of(second, first));
        DocumentResponse expected = new DocumentResponse(20L,
            new PersistentCatalogItemResponse(19L, "OPINION", "Informe de opinión", 20, true),
            DocumentState.LOADED, null, 2, List.of(second, first), List.of(original));
        when(service.list("INI-001")).thenReturn(List.of(expected));

        var result = new DocumentController(service).list("INI-001");

        assertThat(result).isEqualTo(List.of(expected));
        assertThat(result.get(0).versions()).containsExactly(second, first);
        assertThat(result.get(0).latestVersion()).isEqualTo(2);
        assertThat(result.get(0).files()).containsExactly(original);
    }
}
