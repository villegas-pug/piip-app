package pe.gob.midagri.piip.documents.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.time.Instant;
import pe.gob.midagri.piip.documents.application.DocumentService;
import pe.gob.midagri.piip.documents.application.DocumentUploadInput;
import pe.gob.midagri.piip.documents.api.DocumentDtos.VersionResponse;

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
}
