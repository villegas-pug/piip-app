package pe.gob.midagri.piip.documents.api;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.midagri.piip.documents.api.DocumentDtos.*;
import pe.gob.midagri.piip.documents.application.DocumentService;
import pe.gob.midagri.piip.documents.domain.DocumentType;
import java.util.List;

@RestController
@RequestMapping("/portfolio-records/{recordCode}/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service = service; }
    @GetMapping public List<DocumentResponse> list(@PathVariable String recordCode) { return service.list(recordCode); }
    @PostMapping(value = "/{type}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse upload(@PathVariable String recordCode, @PathVariable DocumentType type, @RequestPart MultipartFile file) { return service.upload(recordCode, type, file); }
    @PutMapping("/{type}/not-applicable") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void notApplicable(@PathVariable String recordCode, @PathVariable DocumentType type, @Valid @RequestBody NotApplicableRequest request) { service.markNotApplicable(recordCode, type, request.reason()); }
    @PutMapping("/versions/{versionId}/publication") public VersionResponse publication(@PathVariable String recordCode, @PathVariable Long versionId, @RequestParam boolean published, @RequestParam long version) { return service.publish(versionId, published, version); }
    @GetMapping("/versions/{versionId}/content")
    public ResponseEntity<byte[]> download(@PathVariable String recordCode, @PathVariable Long versionId) {
        DownloadResponse value = service.download(versionId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(value.mimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(value.filename()).build().toString()).body(value.content());
    }
}
