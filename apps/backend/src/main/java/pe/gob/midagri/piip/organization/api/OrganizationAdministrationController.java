package pe.gob.midagri.piip.organization.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationService;

import static pe.gob.midagri.piip.organization.api.OrganizationAdministrationDtos.*;

@RestController
@RequestMapping("/admin/organization")
public class OrganizationAdministrationController {
    private final OrganizationAdministrationService service;
    private final OrganizationAdministrationHttpMapper mapper;

    public OrganizationAdministrationController(OrganizationAdministrationService service,
            OrganizationAdministrationHttpMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping(value = "/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<InstitutionResponse> institutions() {
        return service.institutions().stream().map(mapper::toResponse).toList();
    }

    @GetMapping(value = "/executing-units", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ExecutingUnitResponse> executingUnits(@RequestParam("institutionId") Long institutionId) {
        return service.executingUnits(institutionId).stream().map(mapper::toResponse).toList();
    }

    @Operation(summary = "Crea una Unidad Ejecutora con código generado por el backend")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Unidad Ejecutora creada"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PostMapping(value = "/executing-units", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExecutingUnitResponse> createExecutingUnit(@RequestParam("institutionId") Long institutionId,
            @Valid @RequestBody CreateExecutingUnitRequest request) {
        var value = service.createExecutingUnit(mapper.toCommand(institutionId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(value));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unidad Ejecutora actualizada"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PutMapping(value = "/executing-units/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExecutingUnitResponse updateExecutingUnit(@PathVariable Long id, @RequestParam("version") long version,
            @Valid @RequestBody UpdateExecutingUnitRequest request) {
        return mapper.toResponse(service.updateExecutingUnit(mapper.toCommand(id, version, request)));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unidad Ejecutora desactivada"),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PutMapping(value = "/executing-units/{id}/deactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExecutingUnitResponse deactivateExecutingUnit(@PathVariable Long id, @RequestParam("version") long version) {
        return mapper.toResponse(service.deactivateExecutingUnit(id, version));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unidad Ejecutora reactivada"),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PutMapping(value = "/executing-units/{id}/reactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExecutingUnitResponse reactivateExecutingUnit(@PathVariable Long id, @RequestParam("version") long version) {
        return mapper.toResponse(service.reactivateExecutingUnit(id, version));
    }

    @GetMapping(value = "/organizational-units", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrganizationalUnitResponse> organizationalUnits(@RequestParam("executingUnitId") Long executingUnitId) {
        return service.organizationalUnits(executingUnitId).stream().map(mapper::toResponse).toList();
    }

    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Unidad Orgánica creada"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PostMapping(value = "/organizational-units", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrganizationalUnitResponse> createOrganizationalUnit(@RequestParam("executingUnitId") Long executingUnitId,
            @Valid @RequestBody CreateOrganizationalUnitRequest request) {
        var value = service.createOrganizationalUnit(mapper.toCommand(executingUnitId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(value));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unidad Orgánica actualizada"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PutMapping(value = "/organizational-units/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrganizationalUnitResponse updateOrganizationalUnit(@PathVariable Long id, @RequestParam("version") long version,
            @Valid @RequestBody UpdateOrganizationalUnitRequest request) {
        return mapper.toResponse(service.updateOrganizationalUnit(mapper.toCommand(id, version, request)));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unidad Orgánica desactivada"),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PutMapping(value = "/organizational-units/{id}/deactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrganizationalUnitResponse deactivateOrganizationalUnit(@PathVariable Long id, @RequestParam("version") long version) {
        return mapper.toResponse(service.deactivateOrganizationalUnit(id, version));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unidad Orgánica reactivada"),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @PutMapping(value = "/organizational-units/{id}/reactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrganizationalUnitResponse reactivateOrganizationalUnit(@PathVariable Long id, @RequestParam("version") long version) {
        return mapper.toResponse(service.reactivateOrganizationalUnit(id, version));
    }
}
