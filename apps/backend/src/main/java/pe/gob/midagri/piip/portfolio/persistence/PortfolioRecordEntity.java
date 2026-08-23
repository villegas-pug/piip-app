package pe.gob.midagri.piip.portfolio.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.catalogs.persistence.CatalogItemEntity;
import pe.gob.midagri.piip.portfolio.domain.*;
import java.time.*;

@Entity
@Table(name = "REGISTRO_PORTAFOLIO", uniqueConstraints = {
    @UniqueConstraint(name = "UK_REGISTRO_CODIGO", columnNames = "CODIGO"),
    @UniqueConstraint(name = "UK_REGISTRO_ORIGEN", columnNames = "ID_REGISTRO_ORIGEN")
}, indexes = {
    @Index(name = "IDX_REGISTRO_TIPO_ESTADO", columnList = "TIPO_REGISTRO,ESTADO"),
    @Index(name = "IDX_REGISTRO_EJECUTORA", columnList = "ID_UNIDAD_EJECUTORA"),
    @Index(name = "IDX_REGISTRO_SOLUCION", columnList = "ID_TIPO_SOLUCION"),
    @Index(name = "IDX_REGISTRO_FUENTE", columnList = "ID_FUENTE_ORIGEN"),
    @Index(name = "IDX_REGISTRO_PEI", columnList = "ID_OBJETIVO_PEI"),
    @Index(name = "IDX_REGISTRO_POI", columnList = "ID_ACTIVIDAD_POI")
})
public class PortfolioRecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REGISTRO") private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "TIPO_REGISTRO", length = 20, nullable = false) private RecordType recordType;
    @Column(name = "CODIGO", length = 20, nullable = false) private String code;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ID_REGISTRO_ORIGEN") private PortfolioRecordEntity originRecord;
    @Enumerated(EnumType.STRING) @Column(name = "MODO_ORIGEN", length = 40) private ProjectOriginMode originMode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_UNIDAD_EJECUTORA", nullable = false) private ExecutingUnitEntity executingUnit;
    @Column(name = "NOMBRE", length = 180, nullable = false) private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ID_TIPO_SOLUCION", nullable = false) private CatalogItemEntity solutionType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ID_FUENTE_ORIGEN", nullable = false) private CatalogItemEntity sourceOrigin;
    @Column(name = "FECHA_INICIO", nullable = false) private LocalDate startDate;
    @Column(name = "RESPONSABLE", length = 300, nullable = false) private String responsible;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ID_OBJETIVO_PEI") private CatalogItemEntity peiObjective;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ID_ACTIVIDAD_POI") private CatalogItemEntity poiActivity;
    @Lob @Column(name = "DESCRIPCION", nullable = false) private String description;
    @Lob @Column(name = "RESULTADOS_CLAVE") private String keyResults;
    @Column(name = "NOTA", length = 600) private String note;
    @Enumerated(EnumType.STRING) @Column(name = "ESTADO", length = 40, nullable = false) private PortfolioStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "TIPO_PRODUCTO_FINAL", length = 40, nullable = false) private FinalProductType finalProductType = FinalProductType.NA;
    @Enumerated(EnumType.STRING) @Column(name = "COMPONENTE_DIGITAL", length = 10, nullable = false) private DigitalComponent digitalComponent;
    @Column(name = "FECHA_CIERRE") private LocalDate closingDate;
    @Column(name = "CREADO_POR", length = 100, nullable = false) private String createdBySubject;
    @Column(name = "FECHA_CREACION", nullable = false) private Instant createdAt;
    @Column(name = "FECHA_MODIFICACION", nullable = false) private Instant updatedAt;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected PortfolioRecordEntity() {}

    private PortfolioRecordEntity(RecordType recordType, String code, PortfolioRecordEntity originRecord,
            ProjectOriginMode originMode, ExecutingUnitEntity executingUnit, String name, CatalogItemEntity solutionType,
            CatalogItemEntity sourceOrigin, LocalDate startDate, String responsible, CatalogItemEntity peiObjective,
            CatalogItemEntity poiActivity, String description, String keyResults, String note, PortfolioStatus status,
            DigitalComponent digitalComponent, String actor) {
        this.recordType = recordType; this.code = code; this.originRecord = originRecord; this.originMode = originMode;
        this.executingUnit = executingUnit; this.name = name; this.solutionType = solutionType; this.sourceOrigin = sourceOrigin;
        this.startDate = startDate; this.responsible = responsible; this.peiObjective = peiObjective; this.poiActivity = poiActivity;
        this.description = description; this.keyResults = keyResults; this.note = note; this.status = status;
        this.digitalComponent = digitalComponent; this.createdBySubject = actor; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }

    public static PortfolioRecordEntity initiative(String code, ExecutingUnitEntity unit, String name,
            CatalogItemEntity solutionType, CatalogItemEntity source, LocalDate startDate, String responsible,
            CatalogItemEntity pei, CatalogItemEntity poi, String description, String note, DigitalComponent digital, String actor) {
        return new PortfolioRecordEntity(RecordType.INITIATIVE, code, null, null, unit, name, solutionType, source,
            startDate, responsible, pei, poi, description, null, note, PortfolioStatus.PRESENTED, digital, actor);
    }

    public static PortfolioRecordEntity derivedProject(String code, PortfolioRecordEntity origin, String name,
            CatalogItemEntity solutionType, CatalogItemEntity source, LocalDate startDate, String responsible, CatalogItemEntity pei,
            CatalogItemEntity poi, String description, String keyResults, String note, DigitalComponent digital, String actor) {
        return new PortfolioRecordEntity(RecordType.PROJECT, code, origin, ProjectOriginMode.DERIVED_FROM_INITIATIVE,
            origin.executingUnit, name, solutionType, source, startDate, responsible, pei, poi, description,
            keyResults, note, PortfolioStatus.PROJECT_IN_PROGRESS, digital, actor);
    }

    public static PortfolioRecordEntity preexistingProject(String code, ExecutingUnitEntity unit, String name,
            CatalogItemEntity solutionType, CatalogItemEntity source, LocalDate startDate, String responsible, CatalogItemEntity pei, CatalogItemEntity poi, String description,
            String keyResults, String note, DigitalComponent digital, String actor) {
        return new PortfolioRecordEntity(RecordType.PROJECT, code, null, ProjectOriginMode.PREEXISTING, unit, name,
            solutionType, source, startDate, responsible, pei, poi, description, keyResults, note,
            PortfolioStatus.PROJECT_IN_PROGRESS, digital, actor);
    }

    public void approve() {
        if (status != PortfolioStatus.PRESENTED || recordType != RecordType.INITIATIVE) {
            throw new IllegalStateException("Solo una iniciativa presentada puede aprobarse");
        }
        status = PortfolioStatus.INITIATIVE_APPROVED;
        updatedAt = Instant.now();
    }

    public void transitionInitiativeTo(PortfolioStatus targetStatus, Instant transitionAt) {
        if (recordType != RecordType.INITIATIVE) {
            throw new IllegalStateException("El registro no es una iniciativa");
        }
        boolean allowed = (status == PortfolioStatus.PRESENTED &&
                (targetStatus == PortfolioStatus.INITIATIVE_APPROVED
                    || targetStatus == PortfolioStatus.INITIATIVE_ARCHIVED
                    || targetStatus == PortfolioStatus.NOT_ADMISSIBLE))
            || (status == PortfolioStatus.INITIATIVE_APPROVED
                && targetStatus == PortfolioStatus.INITIATIVE_ARCHIVED);
        if (!allowed) {
            throw new IllegalStateException("La transición de iniciativa no está permitida");
        }
        status = targetStatus;
        updatedAt = transitionAt;
    }

    public void transitionProjectTo(PortfolioStatus targetStatus, Instant transitionAt, LocalDate closingDateAtTransition) {
        if (recordType != RecordType.PROJECT) {
            throw new IllegalStateException("El registro no es un proyecto");
        }
        boolean allowed = switch (status) {
            case PROJECT_IN_PROGRESS -> targetStatus == PortfolioStatus.PRODUCT_APPROVED
                || targetStatus == PortfolioStatus.PRODUCT_NOT_APPROVED
                || targetStatus == PortfolioStatus.SUSPENDED
                || targetStatus == PortfolioStatus.CANCELLED;
            case SUSPENDED -> targetStatus == PortfolioStatus.PROJECT_IN_PROGRESS
                || targetStatus == PortfolioStatus.CANCELLED;
            case PRODUCT_NOT_APPROVED -> targetStatus == PortfolioStatus.PROJECT_IN_PROGRESS
                || targetStatus == PortfolioStatus.CANCELLED;
            case PRODUCT_APPROVED -> targetStatus == PortfolioStatus.FINISHED;
            default -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("La transición de proyecto no está permitida");
        }
        status = targetStatus;
        if (targetStatus == PortfolioStatus.FINISHED) {
            closingDate = closingDateAtTransition;
        }
        updatedAt = transitionAt;
    }

    /** Aplica únicamente el candidato ya validado por el caso de uso. */
    public void applyEditableFields(String name, CatalogItemEntity solutionType, CatalogItemEntity sourceOrigin,
            LocalDate startDate, String responsible, CatalogItemEntity peiObjective, CatalogItemEntity poiActivity,
            String description, String keyResults, String note, DigitalComponent digitalComponent, Instant changedAt) {
        this.name = name;
        this.solutionType = solutionType;
        this.sourceOrigin = sourceOrigin;
        this.startDate = startDate;
        this.responsible = responsible;
        this.peiObjective = peiObjective;
        this.poiActivity = poiActivity;
        this.description = description;
        this.keyResults = keyResults;
        this.note = note;
        this.digitalComponent = digitalComponent;
        this.updatedAt = changedAt;
    }

    public Long getId() { return id; }
    public RecordType getRecordType() { return recordType; }
    public String getCode() { return code; }
    public String getOriginCode() { return originRecord == null ? "NA" : originRecord.getCode(); }
    public PortfolioRecordEntity getOriginRecord() { return originRecord; }
    public ProjectOriginMode getOriginMode() { return originMode; }
    public ExecutingUnitEntity getExecutingUnit() { return executingUnit; }
    public String getName() { return name; }
    public CatalogItemEntity getSolutionType() { return solutionType; }
    public CatalogItemEntity getSourceOrigin() { return sourceOrigin; }
    public LocalDate getStartDate() { return startDate; }
    public String getResponsible() { return responsible; }
    public CatalogItemEntity getPeiObjective() { return peiObjective; }
    public CatalogItemEntity getPoiActivity() { return poiActivity; }
    public String getDescription() { return description; }
    public String getKeyResults() { return keyResults; }
    public String getNote() { return note; }
    public PortfolioStatus getStatus() { return status; }
    public FinalProductType getFinalProductType() { return finalProductType; }
    public DigitalComponent getDigitalComponent() { return digitalComponent; }
    public LocalDate getClosingDate() { return closingDate; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
