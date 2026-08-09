package pe.gob.midagri.piip.identity.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    @PostMapping("/role-assignments") @ResponseStatus(HttpStatus.CREATED) public ScopeResponse assign(@Valid @RequestBody RoleAssignmentRequest request) { return service.assign(request); }
    @DeleteMapping("/role-assignments/{scopeId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void suspend(@PathVariable("scopeId") Long scopeId, @RequestParam("version") long version) { service.suspend(scopeId, version); }
}
