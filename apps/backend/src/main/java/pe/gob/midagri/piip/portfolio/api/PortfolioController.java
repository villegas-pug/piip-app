package pe.gob.midagri.piip.portfolio.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.*;
import pe.gob.midagri.piip.portfolio.application.InitiativeApplicationService;
import pe.gob.midagri.piip.portfolio.application.PortfolioQueryService;
import pe.gob.midagri.piip.portfolio.application.ProjectApplicationService;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.shared.api.PageResponse;
import java.util.List;

@RestController
public class PortfolioController {
    private final PortfolioQueryService queries;
    private final InitiativeApplicationService initiatives;
    private final ProjectApplicationService projects;

    @Autowired
    public PortfolioController(PortfolioQueryService queries, InitiativeApplicationService initiatives,
            ProjectApplicationService projects) {
        this.queries = queries;
        this.initiatives = initiatives;
        this.projects = projects;
    }

    @GetMapping(value = "/initiatives", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<PortfolioRecordResponse> initiatives(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "executingUnitId", required = false) Long executingUnitId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return queries.list(RecordType.INITIATIVE, q, status, executingUnitId, page, size, "updatedAt", "desc");
    }

    @PostMapping(value = "/initiatives", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioRecordResponse createInitiative(@Valid @RequestBody InitiativeCreateRequest request) {
        return initiatives.create(request);
    }

    @GetMapping(value = "/initiatives/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PortfolioRecordResponse initiative(@PathVariable("code") String code) { return queries.get(code); }

    @PostMapping(value = "/initiatives/{code}/approval", produces = MediaType.APPLICATION_JSON_VALUE)
    public PortfolioRecordResponse approve(@PathVariable("code") String code, @Valid @RequestBody ApprovalRequest request) {
        return initiatives.approve(code, request);
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transición aplicada"),
        @ApiResponse(responseCode = "400", description = "Request inválido"),
        @ApiResponse(responseCode = "403", description = "Sin autorización o fuera de ámbito"),
        @ApiResponse(responseCode = "404", description = "Registro inexistente"),
        @ApiResponse(responseCode = "409", description = "Conflicto de versión"),
        @ApiResponse(responseCode = "422", description = "Transición no permitida")
    })
    @PostMapping(value = "/initiatives/{code}/status-transitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public PortfolioRecordResponse transitionInitiative(@PathVariable("code") String code,
            @Valid @RequestBody InitiativeStatusTransitionRequest request) {
        return initiatives.transition(code, request);
    }

    @GetMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<PortfolioRecordResponse> projects(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "executingUnitId", required = false) Long executingUnitId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return queries.list(RecordType.PROJECT, q, status, executingUnitId, page, size, "updatedAt", "desc");
    }

    @GetMapping(value = "/projects/eligible-initiatives", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PortfolioRecordResponse> eligible() { return queries.eligibleInitiatives(); }

    @PostMapping(value = "/projects/derived", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioRecordResponse derived(@Valid @RequestBody DerivedProjectRequest request) {
        return projects.createDerived(request);
    }

    @PostMapping(value = "/projects/preexisting", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioRecordResponse preexisting(@Valid @RequestBody PreexistingProjectRequest request) {
        return projects.createPreexisting(request);
    }

    @GetMapping(value = "/projects/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PortfolioRecordResponse project(@PathVariable("code") String code) { return queries.get(code); }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transición aplicada"),
        @ApiResponse(responseCode = "400", description = "Request inválido"),
        @ApiResponse(responseCode = "403", description = "Sin autorización o fuera de ámbito"),
        @ApiResponse(responseCode = "404", description = "Registro inexistente"),
        @ApiResponse(responseCode = "409", description = "Conflicto de versión"),
        @ApiResponse(responseCode = "422", description = "Transición no permitida")
    })
    @PostMapping(value = "/projects/{code}/status-transitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public PortfolioRecordResponse transitionProject(@PathVariable("code") String code,
            @Valid @RequestBody ProjectStatusTransitionRequest request) {
        return projects.transition(code, request);
    }
}
