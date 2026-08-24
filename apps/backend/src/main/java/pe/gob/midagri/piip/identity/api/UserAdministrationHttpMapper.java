package pe.gob.midagri.piip.identity.api;

import org.springframework.stereotype.Component;
import pe.gob.midagri.piip.identity.application.UserAdministrationCommands.AssignCommand;
import pe.gob.midagri.piip.identity.application.UserAdministrationCommands.UpdateCommand;
import pe.gob.midagri.piip.identity.application.UserAdministrationReadModels;
import pe.gob.midagri.piip.identity.application.UserAdministrationReadModels.*;

import java.util.List;

/** Adapter exclusivo para convertir el contrato HTTP a entradas y salidas de aplicación. */
@Component
public class UserAdministrationHttpMapper {
    public AssignCommand toCommand(AdminDtos.RoleAssignmentRequest request) {
        return new AssignCommand(request.userSubject(), request.role(), request.institutionId(), request.executingUnitId());
    }

    public UpdateCommand toCommand(AdminDtos.RoleAssignmentUpdateRequest request) {
        return new UpdateCommand(request.role(), request.institutionId(), request.executingUnitId());
    }

    public AdminDtos.UserResponse toResponse(User user) {
        return new AdminDtos.UserResponse(user.id(), user.subject(), user.fullName(), user.email(),
            user.scopes().stream().map(this::toResponse).toList());
    }

    public AdminDtos.ScopeResponse toResponse(Scope scope) {
        return new AdminDtos.ScopeResponse(scope.id(), scope.role(), scope.institutionId(), scope.institution(),
            scope.executingUnitId(), scope.executingUnit(), scope.active(), scope.validFrom(), scope.validUntil(), scope.version());
    }

    public AdminDtos.UserAssignmentCandidateResponse toResponse(UserAssignmentCandidate candidate) {
        return new AdminDtos.UserAssignmentCandidateResponse(candidate.id(), candidate.subject(), candidate.fullName(), candidate.email());
    }

    public AdminDtos.AdministrableScopeResponse toResponse(AdministrableScope scope) {
        List<AdminDtos.AdministrableExecutingUnitResponse> units = scope.executingUnits().stream()
            .map(unit -> new AdminDtos.AdministrableExecutingUnitResponse(unit.id(), unit.code(), unit.name())).toList();
        return new AdminDtos.AdministrableScopeResponse(scope.institutionId(), scope.institutionCode(), scope.institutionName(),
            scope.institutionWideAllowed(), units);
    }

    public AdminDtos.ScopeResponse toResponse(AssignmentMutationResult result) {
        return toResponse(result.scope());
    }
}
