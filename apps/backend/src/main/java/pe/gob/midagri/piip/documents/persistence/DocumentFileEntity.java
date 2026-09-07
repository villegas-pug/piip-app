package pe.gob.midagri.piip.documents.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ARCHIVO_DOCUMENTO")
public class DocumentFileEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ARCHIVO") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_DOCUMENTO", nullable = false, foreignKey = @ForeignKey(name = "FK_ARCHDOC_DOC")) private DocumentEntity document;
    @Column(name = "ES_ORIGINAL", nullable = false) private boolean original;
    @Column(name = "ULTIMA_VERSION", nullable = false) private int latestVersion;
    @Column(name = "ELIMINADO", nullable = false) private boolean deleted;
    @Column(name = "FECHA_ELIMINACION") private Instant deletedAt;
    @Column(name = "ELIMINADO_POR", length = 100) private String deletedBy;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected DocumentFileEntity() {}
    public DocumentFileEntity(DocumentEntity document, boolean original) { this(document, original, 0); }
    public DocumentFileEntity(DocumentEntity document, boolean original, int latestVersion) { this.document = document; this.original = original; this.latestVersion = latestVersion; }
    public int registerUpload() { return ++latestVersion; }
    public void markDeleted(String actor) { deleted = true; deletedAt = Instant.now(); deletedBy = actor; }
    public Long getId() { return id; }
    public DocumentEntity getDocument() { return document; }
    public boolean isOriginal() { return original; }
    public int getLatestVersion() { return latestVersion; }
    public boolean isDeleted() { return deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public String getDeletedBy() { return deletedBy; }
}
