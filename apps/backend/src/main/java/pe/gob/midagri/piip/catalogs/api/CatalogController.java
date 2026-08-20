package pe.gob.midagri.piip.catalogs.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.CatalogBundleResponse;
import pe.gob.midagri.piip.catalogs.application.CatalogQueryService;

@RestController
@RequestMapping("/catalogs")
public class CatalogController {
    private final CatalogQueryService service;
    public CatalogController(CatalogQueryService service) { this.service = service; }
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CatalogBundleResponse get() { return service.bundle(); }
}
