package pe.gob.midagri.piip.identity.application;

import pe.gob.midagri.piip.identity.domain.RoleCode;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record LocalAccessContext(Long userId, String subject, Set<RoleScopeGrant> grants) {
    public LocalAccessContext {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(subject, "subject");
        grants = Set.copyOf(grants);
    }

    public Set<RoleCode> roles() {
        return grants.stream().map(RoleScopeGrant::role).collect(Collectors.toUnmodifiableSet());
    }

    public Set<Long> institutionIds() {
        return grants.stream().map(RoleScopeGrant::institutionId).collect(Collectors.toUnmodifiableSet());
    }

    public Set<Long> institutionIds(RoleCode role) {
        return grants.stream().filter(grant -> grant.role() == role).map(RoleScopeGrant::institutionId)
            .collect(Collectors.toUnmodifiableSet());
    }

    public Set<Long> executingUnitIds() {
        return grants.stream().map(RoleScopeGrant::executingUnitId).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    public Set<Long> institutionWideIds() {
        return grants.stream().filter(RoleScopeGrant::isInstitutionWide).map(RoleScopeGrant::institutionId).collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasRole(RoleCode role) { return grants.stream().anyMatch(grant -> grant.role() == role); }
    public boolean institutionWide() { return !institutionWideIds().isEmpty(); }
    public boolean coversInstitutionWide(Long institutionId) { return grants.stream().anyMatch(grant -> grant.isInstitutionWide() && grant.institutionId().equals(institutionId)); }
    public boolean coversInstitutionWide(RoleCode role, Long institutionId) {
        return grants.stream().anyMatch(grant -> grant.coversInstitutionWide(role, institutionId));
    }
    public boolean coversExecutingUnit(Long unitId, Long institutionId) {
        return grants.stream().anyMatch(grant -> grant.coversExecutingUnit(unitId, institutionId));
    }
    public boolean coversExecutingUnit(RoleCode role, Long unitId, Long institutionId) {
        return grants.stream().anyMatch(grant -> grant.coversExecutingUnit(role, unitId, institutionId));
    }
}
