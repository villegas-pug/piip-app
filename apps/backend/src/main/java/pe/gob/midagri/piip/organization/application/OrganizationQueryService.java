package pe.gob.midagri.piip.organization.application;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;

import static pe.gob.midagri.piip.organization.application.OrganizationReadModels.*;

@Service
public class OrganizationQueryService {
    private final InstitutionRepository institutions;
    private final ExecutingUnitRepository executingUnits;
    private final OrganizationalUnitRepository organizationalUnits;
    private final LocalAuthorizationService authorization;

    public OrganizationQueryService(InstitutionRepository institutions, ExecutingUnitRepository executingUnits,
            OrganizationalUnitRepository organizationalUnits, LocalAuthorizationService authorization) {
        this.institutions = institutions;
        this.executingUnits = executingUnits;
        this.organizationalUnits = organizationalUnits;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<InstitutionView> institutions() {
        LocalAccessContext access = authorization.requireAuthenticatedRole();
        return institutions.findAll().stream()
            .filter(item -> access.institutionIds().contains(item.getId()))
            .map(item -> new InstitutionView(item.getId(), item.getCode(), item.getName()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ExecutingUnitView> executingUnits() {
        LocalAccessContext access = authorization.requireAuthenticatedRole();
        return executingUnits.findAll().stream()
            .filter(item -> access.institutionIds().contains(item.getInstitution().getId()))
            .filter(item -> access.coversExecutingUnit(item.getId(), item.getInstitution().getId()))
            .map(item -> new ExecutingUnitView(item.getId(), item.getCode(), item.getName(), item.getInstitution().getId()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationalUnitView> organizationalUnits(Long executingUnitId) {
        authorization.requireReadableUnit(executingUnitId);
        return organizationalUnits.findByExecutingUnitIdAndActiveTrueOrderByName(executingUnitId).stream()
            .map(item -> new OrganizationalUnitView(item.getId(), item.getCode(), item.getName(), item.isActive(),
                item.getAcronym(), item.getParent() == null ? null : item.getParent().getId(),
                item.getExecutingUnit().getId()))
            .toList();
    }
}
