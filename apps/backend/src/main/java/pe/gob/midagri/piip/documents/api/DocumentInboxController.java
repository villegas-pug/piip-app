package pe.gob.midagri.piip.documents.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.documents.application.DocumentInboxService;
import pe.gob.midagri.piip.documents.application.DocumentInboxService.DossierSummary;
import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentInboxController {
    private final DocumentInboxService service;
    public DocumentInboxController(DocumentInboxService service) { this.service = service; }
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DossierSummary> list(@RequestParam(value = "executingUnitId", required = false) Long executingUnitId) {
        return service.list(executingUnitId);
    }
}
