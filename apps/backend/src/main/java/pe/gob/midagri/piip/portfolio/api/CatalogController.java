package pe.gob.midagri.piip.portfolio.api;

import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import java.util.*;

@RestController
@RequestMapping("/catalogs")
public class CatalogController {
    @GetMapping
    public Map<String, List<String>> catalogs() {
        return Map.of(
            "recordTypes", labels(RecordType.values()),
            "solutionTypes", labels(SolutionType.values()),
            "sources", labels(SourceOrigin.values()),
            "statuses", labels(PortfolioStatus.values()),
            "finalProductTypes", labels(FinalProductType.values()),
            "digitalComponents", labels(DigitalComponent.values()));
    }
    private List<String> labels(LabeledCatalog[] values) { return Arrays.stream(values).map(LabeledCatalog::label).toList(); }
}
