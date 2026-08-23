package pe.gob.midagri.piip.portfolio.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.*;
import pe.gob.midagri.piip.portfolio.application.InitiativeApplicationService;
import pe.gob.midagri.piip.portfolio.application.PortfolioQueryService;
import pe.gob.midagri.piip.portfolio.application.ProjectApplicationService;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.*;
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

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Iniciativa actualizada y auditada"),
        @ApiResponse(responseCode = "400", description = "Request inválido", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Sin autorización o fuera de ámbito", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Iniciativa inexistente", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Conflicto de versión", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Regla de actualización no permitida", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping(value = "/initiatives/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PortfolioRecordResponse updateInitiative(@PathVariable("code") String code,
            @Valid @RequestBody InitiativeUpdateRequest request) {
        return initiatives.update(code, initiativeCommand(request));
    }

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
        @ApiResponse(responseCode = "200", description = "Proyecto actualizado y auditado"),
        @ApiResponse(responseCode = "400", description = "Request inválido", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Sin autorización o fuera de ámbito", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Proyecto inexistente", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Conflicto de versión", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Regla de actualización no permitida", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping(value = "/projects/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PortfolioRecordResponse updateProject(@PathVariable("code") String code,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return projects.update(code, projectCommand(request));
    }

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

    private static InitiativeUpdateCommand initiativeCommand(InitiativeUpdateRequest request) {
        return new InitiativeUpdateCommand(request.getVersion(), field(request, "name", request.getName()),
            field(request, "solutionTypeId", request.getSolutionTypeId()), field(request, "sourceId", request.getSourceId()),
            field(request, "startDate", request.getStartDate()), field(request, "responsible", request.getResponsible()),
            field(request, "peiObjectiveId", request.getPeiObjectiveId()), field(request, "poiActivityId", request.getPoiActivityId()),
            units(request.has("responsibleUnits"), request.getResponsibleUnits()), field(request, "description", request.getDescription()),
            field(request, "note", request.getNote()), field(request, "digitalComponent", request.getDigitalComponent()));
    }

    private static ProjectUpdateCommand projectCommand(ProjectUpdateRequest request) {
        return new ProjectUpdateCommand(request.getVersion(), field(request, "name", request.getName()),
            field(request, "solutionTypeId", request.getSolutionTypeId()), field(request, "sourceId", request.getSourceId()),
            field(request, "startDate", request.getStartDate()), field(request, "responsible", request.getResponsible()),
            field(request, "peiObjectiveId", request.getPeiObjectiveId()), field(request, "poiActivityId", request.getPoiActivityId()),
            units(request.has("responsibleUnits"), request.getResponsibleUnits()), field(request, "description", request.getDescription()),
            field(request, "keyResults", request.getKeyResults()), field(request, "note", request.getNote()),
            field(request, "digitalComponent", request.getDigitalComponent()));
    }

    private static <T> FieldUpdate<T> field(InitiativeUpdateRequest request, String name, T value) {
        return request.has(name) ? FieldUpdate.of(value) : FieldUpdate.absent();
    }

    private static <T> FieldUpdate<T> field(ProjectUpdateRequest request, String name, T value) {
        return request.has(name) ? FieldUpdate.of(value) : FieldUpdate.absent();
    }

    private static FieldUpdate<List<ResponsibleUnitUpdate>> units(boolean present, List<ResponsibleUnitInput> values) {
        if (!present) return FieldUpdate.absent();
        List<ResponsibleUnitUpdate> mapped = values == null ? null : values.stream()
            .map(value -> new ResponsibleUnitUpdate(value.organizationalUnitId())).toList();
        return FieldUpdate.of(mapped);
    }
}
