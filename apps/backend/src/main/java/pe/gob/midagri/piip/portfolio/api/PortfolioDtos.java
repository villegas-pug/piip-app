package pe.gob.midagri.piip.portfolio.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import pe.gob.midagri.piip.portfolio.domain.*;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.*;
import java.time.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /** Request PATCH con presencia JSON preservada para diferenciar ausente de null. */
    @JsonIgnoreProperties(ignoreUnknown = false)
    public static final class InitiativeUpdateRequest {
        private Long version;
        private String name;
        private Long solutionTypeId;
        private Long sourceId;
        private LocalDate startDate;
        private String responsible;
        private Long peiObjectiveId;
        private Long poiActivityId;
        private List<@Valid ResponsibleUnitInput> responsibleUnits;
        private String description;
        private String note;
        private DigitalComponent digitalComponent;
        private final Set<String> present = new LinkedHashSet<>();

        @NotNull @PositiveOrZero public Long getVersion() { return version; }
        @JsonSetter("version") public void setVersion(Long value) { present.add("version"); version = value; }
        @Size(max = 180) public String getName() { return name; }
        @JsonSetter("name") public void setName(String value) { present.add("name"); name = value; }
        @Positive public Long getSolutionTypeId() { return solutionTypeId; }
        @JsonSetter("solutionTypeId") public void setSolutionTypeId(Long value) { present.add("solutionTypeId"); solutionTypeId = value; }
        @Positive public Long getSourceId() { return sourceId; }
        @JsonSetter("sourceId") public void setSourceId(Long value) { present.add("sourceId"); sourceId = value; }
        public LocalDate getStartDate() { return startDate; }
        @JsonSetter("startDate") public void setStartDate(LocalDate value) { present.add("startDate"); startDate = value; }
        @Size(max = 300) public String getResponsible() { return responsible; }
        @JsonSetter("responsible") public void setResponsible(String value) { present.add("responsible"); responsible = value; }
        @Positive public Long getPeiObjectiveId() { return peiObjectiveId; }
        @JsonSetter("peiObjectiveId") public void setPeiObjectiveId(Long value) { present.add("peiObjectiveId"); peiObjectiveId = value; }
        @Positive public Long getPoiActivityId() { return poiActivityId; }
        @JsonSetter("poiActivityId") public void setPoiActivityId(Long value) { present.add("poiActivityId"); poiActivityId = value; }
        @Valid public List<ResponsibleUnitInput> getResponsibleUnits() { return responsibleUnits; }
        @JsonSetter("responsibleUnits") public void setResponsibleUnits(List<ResponsibleUnitInput> value) { present.add("responsibleUnits"); responsibleUnits = value; }
        @Size(max = 1000) public String getDescription() { return description; }
        @JsonSetter("description") public void setDescription(String value) { present.add("description"); description = value; }
        @Size(max = 600) public String getNote() { return note; }
        @JsonSetter("note") public void setNote(String value) { present.add("note"); note = value; }
        public DigitalComponent getDigitalComponent() { return digitalComponent; }
        @JsonSetter("digitalComponent") public void setDigitalComponent(DigitalComponent value) { present.add("digitalComponent"); digitalComponent = value; }
        @JsonAnySetter public void rejectUnknownProperty(String property, Object value) {
            throw new IllegalArgumentException("Propiedad no permitida en actualización de iniciativa: " + property);
        }
        public boolean has(String name) { return present.contains(name); }
        public Set<String> presentProperties() { return Set.copyOf(present); }
        @AssertTrue(message = "Los campos presentes no pueden ser nulos ni estar vacíos")
        public boolean hasValidPresentValues() {
            return (!has("name") || name != null && !name.isBlank())
                && (!has("sourceId") || sourceId != null)
                && (!has("solutionTypeId") || solutionTypeId != null)
                && (!has("startDate") || startDate != null)
                && (!has("responsible") || responsible != null && !responsible.isBlank())
                && (!has("description") || description != null && !description.isBlank())
                && (!has("responsibleUnits") || responsibleUnits != null && !responsibleUnits.isEmpty())
                && (!has("digitalComponent") || digitalComponent != null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public static final class ProjectUpdateRequest {
        private Long version;
        private String name;
        private Long solutionTypeId;
        private Long sourceId;
        private LocalDate startDate;
        private String responsible;
        private Long peiObjectiveId;
        private Long poiActivityId;
        private List<@Valid ResponsibleUnitInput> responsibleUnits;
        private String description;
        private String keyResults;
        private String note;
        private DigitalComponent digitalComponent;
        private final Set<String> present = new LinkedHashSet<>();

        @NotNull @PositiveOrZero public Long getVersion() { return version; }
        @JsonSetter("version") public void setVersion(Long value) { present.add("version"); version = value; }
        @Size(max = 180) public String getName() { return name; }
        @JsonSetter("name") public void setName(String value) { present.add("name"); name = value; }
        @Positive public Long getSolutionTypeId() { return solutionTypeId; }
        @JsonSetter("solutionTypeId") public void setSolutionTypeId(Long value) { present.add("solutionTypeId"); solutionTypeId = value; }
        @Positive public Long getSourceId() { return sourceId; }
        @JsonSetter("sourceId") public void setSourceId(Long value) { present.add("sourceId"); sourceId = value; }
        public LocalDate getStartDate() { return startDate; }
        @JsonSetter("startDate") public void setStartDate(LocalDate value) { present.add("startDate"); startDate = value; }
        @Size(max = 300) public String getResponsible() { return responsible; }
        @JsonSetter("responsible") public void setResponsible(String value) { present.add("responsible"); responsible = value; }
        @Positive public Long getPeiObjectiveId() { return peiObjectiveId; }
        @JsonSetter("peiObjectiveId") public void setPeiObjectiveId(Long value) { present.add("peiObjectiveId"); peiObjectiveId = value; }
        @Positive public Long getPoiActivityId() { return poiActivityId; }
        @JsonSetter("poiActivityId") public void setPoiActivityId(Long value) { present.add("poiActivityId"); poiActivityId = value; }
        @Valid public List<ResponsibleUnitInput> getResponsibleUnits() { return responsibleUnits; }
        @JsonSetter("responsibleUnits") public void setResponsibleUnits(List<ResponsibleUnitInput> value) { present.add("responsibleUnits"); responsibleUnits = value; }
        @Size(max = 1000) public String getDescription() { return description; }
        @JsonSetter("description") public void setDescription(String value) { present.add("description"); description = value; }
        @Size(max = 1000) public String getKeyResults() { return keyResults; }
        @JsonSetter("keyResults") public void setKeyResults(String value) { present.add("keyResults"); keyResults = value; }
        @Size(max = 600) public String getNote() { return note; }
        @JsonSetter("note") public void setNote(String value) { present.add("note"); note = value; }
        public DigitalComponent getDigitalComponent() { return digitalComponent; }
        @JsonSetter("digitalComponent") public void setDigitalComponent(DigitalComponent value) { present.add("digitalComponent"); digitalComponent = value; }
        @JsonAnySetter public void rejectUnknownProperty(String property, Object value) {
            throw new IllegalArgumentException("Propiedad no permitida en actualización de proyecto: " + property);
        }
        public boolean has(String name) { return present.contains(name); }
        public Set<String> presentProperties() { return Set.copyOf(present); }
        @AssertTrue(message = "Los campos presentes no pueden ser nulos ni estar vacíos")
        public boolean hasValidPresentValues() {
            return (!has("name") || name != null && !name.isBlank())
                && (!has("sourceId") || sourceId != null)
                && (!has("solutionTypeId") || solutionTypeId != null)
                && (!has("startDate") || startDate != null)
                && (!has("responsible") || responsible != null && !responsible.isBlank())
                && (!has("description") || description != null && !description.isBlank())
                && (!has("responsibleUnits") || responsibleUnits != null && !responsibleUnits.isEmpty())
                && (!has("digitalComponent") || digitalComponent != null);
        }
    }

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
