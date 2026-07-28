package pe.gob.midagri.piip.identity.application;

import pe.gob.midagri.piip.identity.domain.RoleCode;
import java.util.Set;

public record LocalAccessContext(Long userId, String subject, Set<RoleCode> roles,
        Set<Long> institutionIds, Set<Long> executingUnitIds, Set<Long> institutionWideIds) {
    public boolean hasRole(RoleCode role) { return roles.contains(role); }
    public boolean institutionWide() { return !institutionWideIds.isEmpty(); }
    public boolean coversInstitutionWide(Long institutionId) { return institutionWideIds.contains(institutionId); }
    public boolean coversExecutingUnit(Long unitId, Long institutionId) {
        return executingUnitIds.contains(unitId) || institutionWideIds.contains(institutionId);
    }
}
