package pe.gob.midagri.piip.identity.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.identity.api.AdminDtos.*;
import pe.gob.midagri.piip.identity.application.UserAdministrationService;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class UserAdministrationController {
    private final UserAdministrationService service;
    private final UserAdministrationHttpMapper mapper;
    public UserAdministrationController(UserAdministrationService service, UserAdministrationHttpMapper mapper) { this.service = service; this.mapper = mapper; }
    @GetMapping("/users") public List<UserResponse> users() { return service.list().stream().map(mapper::toResponse).toList(); }
    @GetMapping(value = "/users/administrable-scopes", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AdministrableScopeResponse> administrableScopes() { return service.listAdministrableScopes().stream().map(mapper::toResponse).toList(); }
    @GetMapping(value = "/users/assignment-candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserAssignmentCandidateResponse> assignmentCandidates() { return service.listAssignmentCandidates().stream().map(mapper::toResponse).toList(); }
    @Operation(summary = "Crea una asignación o reactiva una coincidencia suspendida")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Asignación creada"),
        @ApiResponse(responseCode = "200", description = "Coincidencia suspendida reactivada"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PostMapping("/role-assignments") public ResponseEntity<ScopeResponse> assign(@Valid @RequestBody RoleAssignmentRequest request) {
        var result = service.assign(mapper.toCommand(request));
        HttpStatus status = result.status() == pe.gob.midagri.piip.identity.application.UserAdministrationReadModels.AssignmentMutationStatus.CREATED
            ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(mapper.toResponse(result));
    }
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Asignación suspendida"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @DeleteMapping("/role-assignments/{scopeId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void suspend(@PathVariable("scopeId") Long scopeId, @RequestParam("version") long version) { service.suspend(scopeId, version); }
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asignación actualizada"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PutMapping("/role-assignments/{scopeId}") public ScopeResponse update(@PathVariable("scopeId") Long scopeId,
            @RequestParam("version") long version, @Valid @RequestBody RoleAssignmentUpdateRequest request) { return mapper.toResponse(service.update(scopeId, version, mapper.toCommand(request))); }
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asignación reactivada"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PutMapping("/role-assignments/{scopeId}/reactivation") public ScopeResponse reactivate(@PathVariable("scopeId") Long scopeId,
            @RequestParam("version") long version) { return mapper.toResponse(service.reactivate(scopeId, version)); }
}
