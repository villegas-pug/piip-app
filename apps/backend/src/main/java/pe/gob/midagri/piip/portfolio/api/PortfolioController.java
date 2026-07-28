package pe.gob.midagri.piip.portfolio.api;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.*;
import pe.gob.midagri.piip.portfolio.application.PortfolioService;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.shared.api.PageResponse;
import java.util.List;

@RestController
public class PortfolioController {
    private final PortfolioService service;
    public PortfolioController(PortfolioService service) { this.service = service; }

    @GetMapping("/initiatives") public PageResponse<PortfolioRecordResponse> initiatives(
            @RequestParam(required = false) String q, @RequestParam(required = false) String status,
            @RequestParam(required = false) Long executingUnitId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return service.list(RecordType.INITIATIVE, q, status, executingUnitId, page, size, sort, direction);
    }
    @PostMapping("/initiatives") @ResponseStatus(HttpStatus.CREATED) public PortfolioRecordResponse createInitiative(@Valid @RequestBody InitiativeCreateRequest request) { return service.createInitiative(request); }
    @GetMapping("/initiatives/{code}") public PortfolioRecordResponse initiative(@PathVariable String code) { return service.get(code); }
    @PostMapping("/initiatives/{code}/approval") public PortfolioRecordResponse approve(@PathVariable String code, @Valid @RequestBody ApprovalRequest request) { return service.approve(code, request); }

    @GetMapping("/projects") public PageResponse<PortfolioRecordResponse> projects(
            @RequestParam(required = false) String q, @RequestParam(required = false) String status,
            @RequestParam(required = false) Long executingUnitId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return service.list(RecordType.PROJECT, q, status, executingUnitId, page, size, sort, direction);
    }
    @GetMapping("/projects/eligible-initiatives") public List<PortfolioRecordResponse> eligible() { return service.eligibleInitiatives(); }
    @PostMapping("/projects/derived") @ResponseStatus(HttpStatus.CREATED) public PortfolioRecordResponse derived(@Valid @RequestBody DerivedProjectRequest request) { return service.createDerived(request); }
    @PostMapping("/projects/preexisting") @ResponseStatus(HttpStatus.CREATED) public PortfolioRecordResponse preexisting(@Valid @RequestBody PreexistingProjectRequest request) { return service.createPreexisting(request); }
    @GetMapping("/projects/{code}") public PortfolioRecordResponse project(@PathVariable String code) { return service.get(code); }
}
