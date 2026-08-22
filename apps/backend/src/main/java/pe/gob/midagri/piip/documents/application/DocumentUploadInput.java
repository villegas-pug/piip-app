package pe.gob.midagri.piip.documents.application;

import java.util.Objects;
import java.util.function.Supplier;

public record DocumentUploadInput(String originalFilename, String contentType, long sizeBytes,
        Supplier<byte[]> bytes) {
    public DocumentUploadInput {
        Objects.requireNonNull(bytes, "bytes");
    }
}
