package pe.gob.midagri.piip.organization.api;

import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.organization.persistence.*;
import java.util.List;

@RestController
public class OrganizationController {
    private final InstitutionRepository institutions;
    private final ExecutingUnitRepository executingUnits;
    private final OrganizationalUnitRepository organizationalUnits;
    private final LocalAuthorizationService authorization;

    public OrganizationController(InstitutionRepository institutions, ExecutingUnitRepository executingUnits,
            OrganizationalUnitRepository organizationalUnits, LocalAuthorizationService authorization) {
        this.institutions = institutions; this.executingUnits = executingUnits; this.organizationalUnits = organizationalUnits; this.authorization = authorization;
    }

    @GetMapping("/institutions")
    public List<InstitutionResponse> institutions() {
        LocalAccessContext access = authorization.requireAuthenticatedRole();
        return institutions.findAll().stream().filter(item -> access.institutionIds().contains(item.getId()))
            .map(item -> new InstitutionResponse(item.getId(), item.getCode(), item.getName())).toList();
    }

    @GetMapping("/executing-units")
    public List<ExecutingUnitResponse> executingUnits() {
        LocalAccessContext access = authorization.requireAuthenticatedRole();
        return executingUnits.findAll().stream().filter(item -> access.institutionIds().contains(item.getInstitution().getId()))
            .filter(item -> access.coversExecutingUnit(item.getId(), item.getInstitution().getId()))
            .map(item -> new ExecutingUnitResponse(item.getId(), item.getCode(), item.getName(), item.getInstitution().getId())).toList();
    }

    @GetMapping("/organizational-units")
    public List<OrganizationalUnitResponse> organizationalUnits(@RequestParam("executingUnitId") Long executingUnitId) {
        authorization.requireReadableUnit(executingUnitId);
        return organizationalUnits.findByExecutingUnitIdAndActiveTrueOrderByName(executingUnitId).stream()
            .map(item -> response(item)).toList();
    }

    static OrganizationalUnitResponse response(OrganizationalUnitEntity item) {
        return new OrganizationalUnitResponse(item.getId(), item.getCode(), item.getName(), item.isActive(), item.getAcronym(),
            item.getParent() == null ? null : item.getParent().getId(), item.getExecutingUnit().getId());
    }

    public record InstitutionResponse(Long id, String code, String name) {}
    public record ExecutingUnitResponse(Long id, String code, String name, Long institutionId) {}
    public record OrganizationalUnitResponse(Long id, String code, String name, boolean active, String acronym, Long parentId, Long executingUnitId) {}
}
