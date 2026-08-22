package pe.gob.midagri.piip.documents.api;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.midagri.piip.documents.api.DocumentDtos.*;
import pe.gob.midagri.piip.documents.application.DocumentService;
import java.util.List;

@RestController
@RequestMapping("/portfolio-records/{recordCode}/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service = service; }
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE) public List<DocumentResponse> list(@PathVariable("recordCode") String recordCode) { return service.list(recordCode); }
    @PostMapping(value = "/{documentTypeId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse upload(@PathVariable("recordCode") String recordCode, @PathVariable("documentTypeId") Long documentTypeId, @RequestPart("file") MultipartFile file) { return service.upload(recordCode, documentTypeId, MultipartDocumentUploadAdapter.adapt(file)); }
    @PutMapping("/{documentTypeId}/not-applicable") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void notApplicable(@PathVariable("recordCode") String recordCode, @PathVariable("documentTypeId") Long documentTypeId, @Valid @RequestBody NotApplicableRequest request) { service.markNotApplicable(recordCode, documentTypeId, request.reason()); }
    @PutMapping(value = "/versions/{versionId}/publication", produces = MediaType.APPLICATION_JSON_VALUE) public VersionResponse publication(@PathVariable("recordCode") String recordCode, @PathVariable("versionId") Long versionId, @RequestParam("published") boolean published, @RequestParam("version") long version) { return service.publish(versionId, published, version); }
    @GetMapping("/versions/{versionId}/content")
    public ResponseEntity<byte[]> download(@PathVariable("recordCode") String recordCode, @PathVariable("versionId") Long versionId) {
        DownloadResponse value = service.download(versionId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(value.mimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(value.filename()).build().toString()).body(value.content());
    }
}
