package pe.gob.midagri.piip.portfolio.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import java.time.*;
import java.util.List;

public final class PortfolioDtos {
    private PortfolioDtos() {}

    public record ResponsibleUnitInput(Long organizationalUnitId, @NotBlank @Size(max = 300) String originalDesignation) {}

    public record InitiativeCreateRequest(
        @NotNull Long executingUnitId,
        @NotBlank @Size(max = 180) String name,
        @NotNull SolutionType solutionType,
        @NotNull SourceOrigin source,
        @NotNull LocalDate startDate,
        @NotBlank @Size(max = 300) String responsible,
        @Size(max = 500) String peiObjective,
        @Size(max = 500) String poiActivity,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 600) String note,
        @NotNull DigitalComponent digitalComponent,
        @NotEmpty List<@Valid ResponsibleUnitInput> responsibleUnits) {}

    public record ApprovalRequest(@NotNull Long version, @Size(max = 1000) String observation) {}

    public record DerivedProjectRequest(
        @NotBlank String initiativeCode,
        @NotNull LocalDate startDate,
        @NotBlank @Size(max = 180) String name,
        @NotNull SolutionType solutionType,
        @NotNull SourceOrigin source,
        @NotBlank @Size(max = 300) String responsible,
        @Size(max = 500) String peiObjective,
        @Size(max = 500) String poiActivity,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 1000) String keyResults,
        @Size(max = 600) String note,
        @NotNull DigitalComponent digitalComponent,
        @NotEmpty List<@Valid ResponsibleUnitInput> responsibleUnits) {}

    public record PreexistingProjectRequest(
        @NotNull Long executingUnitId,
        @NotNull LocalDate startDate,
        @NotBlank @Size(max = 180) String name,
        @NotNull SourceOrigin source,
        @NotBlank @Size(max = 300) String responsible,
        @Size(max = 500) String peiObjective,
        @Size(max = 500) String poiActivity,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 1000) String keyResults,
        @Size(max = 600) String note,
        @NotNull DigitalComponent digitalComponent,
        @NotEmpty List<@Valid ResponsibleUnitInput> responsibleUnits) {}

    public record PortfolioRecordResponse(
        String recordType, String code, String originCode, String name, String solutionType, String source,
        LocalDate startDate, String responsible, String peiObjective, String poiActivity,
        List<String> responsibleUnits, String description, String keyResults, String note, String status,
        String finalProductType, String digitalComponent, LocalDate closingDate,
        String technicalOpinionReport, String formalApprovalDecision, String finalProductApprovalDocument,
        String projectManagementDocumentation, String finalClosureReport,
        Long executingUnitId, String executingUnit, Instant updatedAt, long version) {}
}
