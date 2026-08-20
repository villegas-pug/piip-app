package pe.gob.midagri.piip.documents.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.documents.domain.*;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;

@Entity
@Table(name = "DOCUMENTO", uniqueConstraints = @UniqueConstraint(name = "UK_DOCUMENTO_REGISTRO_TIPO", columnNames = {"ID_REGISTRO", "ID_TIPO_DOCUMENTO"}),
    indexes = @Index(name = "IDX_DOCUMENTO_TIPO", columnList = "ID_TIPO_DOCUMENTO"))
public class DocumentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DOCUMENTO") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_REGISTRO", nullable = false) private PortfolioRecordEntity record;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_TIPO_DOCUMENTO", nullable = false) private DocumentTypeEntity type;
    @Enumerated(EnumType.STRING) @Column(name = "ESTADO", length = 30, nullable = false) private DocumentState state = DocumentState.PENDING;
    @Column(name = "MOTIVO_NO_APLICA", length = 500) private String notApplicableReason;
    @Column(name = "ULTIMA_VERSION", nullable = false) private int latestVersion;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected DocumentEntity() {}
    public DocumentEntity(PortfolioRecordEntity record, DocumentTypeEntity type) { this.record = record; this.type = type; }
    public int registerUpload() { state = DocumentState.LOADED; return ++latestVersion; }
    public void markNotApplicable(String reason) { state = DocumentState.NOT_APPLICABLE; notApplicableReason = reason; }
    public Long getId() { return id; }
    public PortfolioRecordEntity getRecord() { return record; }
    public DocumentTypeEntity getType() { return type; }
    public DocumentState getState() { return state; }
    public String getNotApplicableReason() { return notApplicableReason; }
    public int getLatestVersion() { return latestVersion; }
}
