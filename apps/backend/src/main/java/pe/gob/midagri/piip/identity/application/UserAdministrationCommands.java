package pe.gob.midagri.piip.identity.application;

import pe.gob.midagri.piip.identity.domain.RoleCode;

/** Entradas de los casos de uso de administración, independientes de HTTP. */
public final class UserAdministrationCommands {
    private UserAdministrationCommands() {}

    public record AssignCommand(String userSubject, RoleCode role, Long institutionId, Long executingUnitId) {}

    public record UpdateCommand(RoleCode role, Long institutionId, Long executingUnitId) {}
}
