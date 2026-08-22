package pe.gob.midagri.piip.identity.application;

import java.util.List;
import java.util.Set;
import pe.gob.midagri.piip.identity.domain.RoleCode;

public record CurrentIdentityReadModel(String subject, String fullName, String email,
        List<RoleScopeGrant> roleScopes, Set<RoleCode> roles, Set<Long> institutionIds,
        Set<Long> executingUnitIds, boolean institutionWide) {
    public CurrentIdentityReadModel {
        roleScopes = List.copyOf(roleScopes);
        roles = Set.copyOf(roles);
        institutionIds = Set.copyOf(institutionIds);
        executingUnitIds = Set.copyOf(executingUnitIds);
    }
}
