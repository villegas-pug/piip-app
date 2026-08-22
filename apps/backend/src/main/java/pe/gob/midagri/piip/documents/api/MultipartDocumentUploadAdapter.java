package pe.gob.midagri.piip.documents.api;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.midagri.piip.documents.application.DocumentUploadInput;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;

public final class MultipartDocumentUploadAdapter {
    private MultipartDocumentUploadAdapter() {}

    public static DocumentUploadInput adapt(MultipartFile file) {
        return new DocumentUploadInput(file.getOriginalFilename(), file.getContentType(), file.getSize(), () -> {
            try {
                return file.getBytes();
            } catch (IOException exception) {
                throw new BusinessRuleException("No se pudo leer el archivo");
            }
        });
    }
}
