package pe.gob.midagri.piip.portfolio.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.*;
import java.time.*;
import java.util.List;

public final class PortfolioDtos {
    private PortfolioDtos() {}

    public record OrganizationalUnitResponse(Long id, String code, String name, boolean active, String acronym,
            Long parentId, Long executingUnitId) {}
    public record ResponsibleUnitInput(@NotNull Long organizationalUnitId) {}
    public record ResponsibleUnitResponse(OrganizationalUnitResponse organizationalUnit, String originalDesignation, int displayOrder) {}

    public record InitiativeCreateRequest(
        @NotNull Long executingUnitId,
        @NotBlank @Size(max = 180) String name,
        @NotNull Long solutionTypeId,
        @NotNull Long sourceId,
        @NotNull LocalDate startDate,
        @NotBlank @Size(max = 300) String responsible,
        Long peiObjectiveId,
        Long poiActivityId,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 600) String note,
        @NotNull DigitalComponent digitalComponent,
        @NotEmpty List<@Valid ResponsibleUnitInput> responsibleUnits) {}

    public record ApprovalRequest(@NotNull Long version, @Size(max = 1000) String observation) {}

    public record InitiativeStatusTransitionRequest(
        @NotNull Long version,
        @NotNull PortfolioStatus targetStatus,
        @Size(max = 1000) String observation) {}

    public record ProjectStatusTransitionRequest(
        @NotNull Long version,
        @NotNull PortfolioStatus targetStatus,
        @Size(max = 1000) String observation) {}

    public record DerivedProjectRequest(
        @NotBlank String initiativeCode,
        @NotNull LocalDate startDate,
        @NotBlank @Size(max = 180) String name,
        @NotNull Long solutionTypeId,
        @NotNull Long sourceId,
        @NotBlank @Size(max = 300) String responsible,
        Long peiObjectiveId,
        Long poiActivityId,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 1000) String keyResults,
        @Size(max = 600) String note,
        @NotNull DigitalComponent digitalComponent,
        @NotEmpty List<@Valid ResponsibleUnitInput> responsibleUnits) {}

    public record PreexistingProjectRequest(
        @NotNull Long executingUnitId,
        @NotNull LocalDate startDate,
        @NotBlank @Size(max = 180) String name,
        @NotNull Long sourceId,
        @NotBlank @Size(max = 300) String responsible,
        Long peiObjectiveId,
        Long poiActivityId,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 1000) String keyResults,
        @Size(max = 600) String note,
        @NotNull DigitalComponent digitalComponent,
        @NotEmpty List<@Valid ResponsibleUnitInput> responsibleUnits) {}

    public record PortfolioRecordResponse(
        TechnicalCatalogItemResponse recordType, String code, String originCode, String name,
        PersistentCatalogItemResponse solutionType, PersistentCatalogItemResponse source,
        LocalDate startDate, String responsible, PersistentCatalogItemResponse peiObjective, PersistentCatalogItemResponse poiActivity,
        List<ResponsibleUnitResponse> responsibleUnits, String description, String keyResults, String note, String status,
        String finalProductType, String digitalComponent, LocalDate closingDate,
        String technicalOpinionReport, String formalApprovalDecision, String finalProductApprovalDocument,
        String projectManagementDocumentation, String finalClosureReport,
        Long executingUnitId, String executingUnit, Instant updatedAt, long version) {}
}
