package pe.gob.midagri.piip.identity.application;

import java.util.Objects;
import pe.gob.midagri.piip.identity.domain.RoleCode;

public record RoleScopeGrant(RoleCode role, Long institutionId, Long executingUnitId) {
    public RoleScopeGrant {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(institutionId, "institutionId");
    }

    public boolean isInstitutionWide() {
        return executingUnitId == null;
    }

    public boolean coversInstitutionWide(RoleCode requiredRole, Long requiredInstitutionId) {
        return role == requiredRole && isInstitutionWide() && institutionId.equals(requiredInstitutionId);
    }

    public boolean coversExecutingUnit(Long requiredExecutingUnitId, Long requiredInstitutionId) {
        return institutionId.equals(requiredInstitutionId)
            && (isInstitutionWide() || executingUnitId.equals(requiredExecutingUnitId));
    }

    public boolean coversExecutingUnit(RoleCode requiredRole, Long requiredExecutingUnitId, Long requiredInstitutionId) {
        return role == requiredRole && coversExecutingUnit(requiredExecutingUnitId, requiredInstitutionId);
    }
}
