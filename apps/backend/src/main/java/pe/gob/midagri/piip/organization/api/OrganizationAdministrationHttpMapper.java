package pe.gob.midagri.piip.organization.api;

import org.springframework.stereotype.Component;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationCommands;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationReadModels;

import static pe.gob.midagri.piip.organization.api.OrganizationAdministrationDtos.*;

/** Adaptador HTTP del vertical administrativo de organización. */
@Component
public class OrganizationAdministrationHttpMapper {
    public OrganizationAdministrationCommands.CreateExecutingUnit toCommand(Long institutionId,
            CreateExecutingUnitRequest request) {
        return new OrganizationAdministrationCommands.CreateExecutingUnit(institutionId, request.name(), request.displayOrder());
    }

    public OrganizationAdministrationCommands.UpdateExecutingUnit toCommand(Long id, long version,
            UpdateExecutingUnitRequest request) {
        return new OrganizationAdministrationCommands.UpdateExecutingUnit(id, version, request.name(), request.displayOrder());
    }

    public OrganizationAdministrationCommands.CreateOrganizationalUnit toCommand(Long executingUnitId,
            CreateOrganizationalUnitRequest request) {
        return new OrganizationAdministrationCommands.CreateOrganizationalUnit(executingUnitId, request.name(),
            request.acronym(), request.active());
    }

    public OrganizationAdministrationCommands.UpdateOrganizationalUnit toCommand(Long id, long version,
            UpdateOrganizationalUnitRequest request) {
        return new OrganizationAdministrationCommands.UpdateOrganizationalUnit(id, version, request.name(), request.acronym());
    }

    public InstitutionResponse toResponse(OrganizationAdministrationReadModels.Institution value) {
        return new InstitutionResponse(value.id(), value.code(), value.name());
    }

    public ExecutingUnitResponse toResponse(OrganizationAdministrationReadModels.ExecutingUnit value) {
        var institution = value.institution();
        return new ExecutingUnitResponse(value.id(), value.code(), value.name(), value.active(), value.displayOrder(),
            value.registeredAt(), value.activatedAt(), value.version(),
            new InstitutionReferenceResponse(institution.id(), institution.code(), institution.name()));
    }

    public OrganizationalUnitResponse toResponse(OrganizationAdministrationReadModels.OrganizationalUnit value) {
        var executingUnit = value.executingUnit();
        return new OrganizationalUnitResponse(value.id(), value.code(), value.name(), value.acronym(), value.active(),
            value.version(), new ExecutingUnitReferenceResponse(executingUnit.id(), executingUnit.code(), executingUnit.name()));
    }
}
