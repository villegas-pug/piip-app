package pe.gob.midagri.piip.documents.api;

import jakarta.validation.constraints.Size;
import pe.gob.midagri.piip.documents.domain.DocumentState;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.PersistentCatalogItemResponse;
import java.time.Instant;
import java.util.List;

public final class DocumentDtos {
    private DocumentDtos() {}
    public record DocumentResponse(Long id, PersistentCatalogItemResponse documentType, DocumentState state,
            String notApplicableReason, int latestVersion, List<VersionResponse> versions) {}
    public record VersionResponse(Long id, int version, String filename, String mimeType, long sizeBytes,
            String checksumSha256, Instant uploadedAt, boolean externallyPublished, long optimisticVersion) {}
    public record NotApplicableRequest(@Size(max = 500) String reason) {}
    public record DownloadResponse(String filename, String mimeType, byte[] content) {}
}
