package pe.gob.midagri.piip.organization.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Contratos HTTP administrativos estrictos; no contienen relaciones jerárquicas ni referencias editables. */
public final class OrganizationAdministrationDtos {
    private OrganizationAdministrationDtos() {}

    public record InstitutionResponse(Long id, String code, String name) {}

    public record InstitutionReferenceResponse(Long id, String code, String name) {}

    public record ExecutingUnitResponse(Long id, String code, String name, boolean active, int displayOrder,
            Instant registeredAt, Instant activatedAt, long version, InstitutionReferenceResponse institution) {}

    public record ExecutingUnitReferenceResponse(Long id, String code, String name) {}

    public record OrganizationalUnitResponse(Long id, String code, String name, String acronym, boolean active,
            long version, ExecutingUnitReferenceResponse executingUnit) {}

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record CreateExecutingUnitRequest(@NotBlank @Size(max = 200) String name,
            @PositiveOrZero Integer displayOrder) {}

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record UpdateExecutingUnitRequest(@NotBlank @Size(max = 200) String name,
            @NotNull @PositiveOrZero Integer displayOrder) {}

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record CreateOrganizationalUnitRequest(@NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 30) String acronym, @NotNull Boolean active) {}

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record UpdateOrganizationalUnitRequest(@NotBlank @Size(max = 200) String name,
            @Size(max = 30) String acronym) {}
}
