package pe.gob.midagri.piip.organization.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.midagri.piip.organization.application.OrganizationQueryService;
import pe.gob.midagri.piip.organization.application.OrganizationReadModels;

@RestController
public class OrganizationController {
    private final OrganizationQueryService service;

    @Autowired
    public OrganizationController(OrganizationQueryService service) {
        this.service = service;
    }

    @GetMapping("/institutions")
    public List<InstitutionResponse> institutions() {
        return service.institutions().stream()
            .map(item -> new InstitutionResponse(item.id(), item.code(), item.name())).toList();
    }

    @GetMapping("/executing-units")
    public List<ExecutingUnitResponse> executingUnits() {
        return service.executingUnits().stream()
            .map(item -> new ExecutingUnitResponse(item.id(), item.code(), item.name(), item.institutionId())).toList();
    }

    @GetMapping("/organizational-units")
    public List<OrganizationalUnitResponse> organizationalUnits(@RequestParam("executingUnitId") Long executingUnitId) {
        return service.organizationalUnits(executingUnitId).stream().map(OrganizationController::response).toList();
    }

    static OrganizationalUnitResponse response(OrganizationReadModels.OrganizationalUnitView item) {
        return new OrganizationalUnitResponse(item.id(), item.code(), item.name(), item.active(), item.acronym(),
            item.parentId(), item.executingUnitId());
    }

    public record InstitutionResponse(Long id, String code, String name) {}
    public record ExecutingUnitResponse(Long id, String code, String name, Long institutionId) {}
    public record OrganizationalUnitResponse(Long id, String code, String name, boolean active, String acronym,
            Long parentId, Long executingUnitId) {}
}
