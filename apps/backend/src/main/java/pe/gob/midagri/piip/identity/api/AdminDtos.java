package pe.gob.midagri.piip.identity.api;

import jakarta.validation.constraints.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import java.time.Instant;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {}
    public record RoleAssignmentRequest(@NotBlank String userSubject, @NotNull RoleCode role,
            @NotNull Long institutionId, Long executingUnitId) {}
    public record RoleAssignmentUpdateRequest(@NotNull RoleCode role, @NotNull Long institutionId, Long executingUnitId) {}
    public record UserAssignmentCandidateResponse(Long id, String subject, String fullName, String email) {}
    public record UserResponse(Long id, String subject, String fullName, String email, List<ScopeResponse> scopes) {}
    public record ScopeResponse(Long id, RoleCode role, Long institutionId, String institution,
            Long executingUnitId, String executingUnit, boolean active, Instant validFrom, Instant validUntil, long version) {}
}
