package pe.gob.midagri.piip.identity.application;

import pe.gob.midagri.piip.identity.domain.RoleCode;
import java.time.Instant;
import java.util.List;

/** Representaciones internas que el adapter HTTP transforma a DTOs públicos. */
public final class UserAdministrationReadModels {
    private UserAdministrationReadModels() {}

    public record UserAssignmentCandidate(Long id, String subject, String fullName, String email) {}

    public record AdministrableScope(Long institutionId, String institutionCode, String institutionName,
            boolean institutionWideAllowed, List<AdministrableExecutingUnit> executingUnits) {}

    public record AdministrableExecutingUnit(Long id, String code, String name) {}

    public record User(Long id, String subject, String fullName, String email, List<Scope> scopes) {}

    public record Scope(Long id, RoleCode role, Long institutionId, String institution,
            Long executingUnitId, String executingUnit, boolean active, Instant validFrom,
            Instant validUntil, long version) {}

    public enum AssignmentMutationStatus { CREATED, REACTIVATED }

    public record AssignmentMutationResult(AssignmentMutationStatus status, Scope scope) {
        public Long id() { return scope.id(); }
        public RoleCode role() { return scope.role(); }
        public Long executingUnitId() { return scope.executingUnitId(); }
        public String executingUnit() { return scope.executingUnit(); }
    }

    public record AssignmentSnapshot(Long id, String userSubject, RoleCode role, Long institutionId,
            String institution, Long executingUnitId, String executingUnit, boolean active,
            Instant validFrom, Instant validUntil, long version) {}
}
