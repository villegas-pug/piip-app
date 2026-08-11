package pe.gob.midagri.piip.identity.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.identity.api.AdminDtos.*;
import pe.gob.midagri.piip.identity.application.UserAdministrationService;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class UserAdministrationController {
    private final UserAdministrationService service;
    public UserAdministrationController(UserAdministrationService service) { this.service = service; }
    @GetMapping("/users") public List<UserResponse> users() { return service.list(); }
    @GetMapping(value = "/users/administrable-scopes", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AdministrableScopeResponse> administrableScopes() { return service.listAdministrableScopes(); }
    @GetMapping(value = "/users/assignment-candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserAssignmentCandidateResponse> assignmentCandidates() { return service.listAssignmentCandidates(); }
    @PostMapping("/role-assignments") @ResponseStatus(HttpStatus.CREATED) public ScopeResponse assign(@Valid @RequestBody RoleAssignmentRequest request) { return service.assign(request); }
    @DeleteMapping("/role-assignments/{scopeId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void suspend(@PathVariable("scopeId") Long scopeId, @RequestParam("version") long version) { service.suspend(scopeId, version); }
    @PutMapping("/role-assignments/{scopeId}") public ScopeResponse update(@PathVariable("scopeId") Long scopeId,
            @RequestParam("version") long version, @Valid @RequestBody RoleAssignmentUpdateRequest request) { return service.update(scopeId, version, request); }
    @PutMapping("/role-assignments/{scopeId}/reactivation") public ScopeResponse reactivate(@PathVariable("scopeId") Long scopeId,
            @RequestParam("version") long version) { return service.reactivate(scopeId, version); }
}
