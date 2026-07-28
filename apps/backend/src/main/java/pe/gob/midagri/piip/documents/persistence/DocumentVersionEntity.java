package pe.gob.midagri.piip.documents.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "DOCUMENTO_VERSION", uniqueConstraints = @UniqueConstraint(name = "UK_DOC_VERSION", columnNames = {"ID_DOCUMENTO", "NUMERO_VERSION"}))
public class DocumentVersionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DOCUMENTO_VERSION") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_DOCUMENTO", nullable = false) private DocumentEntity document;
    @Column(name = "NUMERO_VERSION", nullable = false) private int versionNumber;
    @Column(name = "NOMBRE_ARCHIVO", length = 255, nullable = false) private String filename;
    @Column(name = "TIPO_MIME", length = 120, nullable = false) private String mimeType;
    @Column(name = "TAMANIO_BYTES", nullable = false) private long sizeBytes;
    @Column(name = "CHECKSUM_SHA256", length = 64, nullable = false) private String checksumSha256;
    @Column(name = "CARGADO_POR", length = 100, nullable = false) private String uploadedBy;
    @Column(name = "FECHA_CARGA", nullable = false) private Instant uploadedAt;
    @Column(name = "PUBLICADO_EXTERNO", nullable = false) private boolean externallyPublished;
    @Column(name = "PUBLICADO_POR", length = 100) private String publishedBy;
    @Column(name = "FECHA_PUBLICACION") private Instant publishedAt;
    @Version @Column(name = "VERSION_OPTIMISTA", nullable = false) private long optimisticVersion;

    protected DocumentVersionEntity() {}
    public DocumentVersionEntity(DocumentEntity document, int versionNumber, String filename, String mimeType,
            long sizeBytes, String checksumSha256, String uploadedBy) {
        this.document = document; this.versionNumber = versionNumber; this.filename = filename; this.mimeType = mimeType;
        this.sizeBytes = sizeBytes; this.checksumSha256 = checksumSha256; this.uploadedBy = uploadedBy; this.uploadedAt = Instant.now();
    }
    public void publish(String actor) { externallyPublished = true; publishedBy = actor; publishedAt = Instant.now(); }
    public void unpublish() { externallyPublished = false; publishedBy = null; publishedAt = null; }
    public Long getId() { return id; }
    public DocumentEntity getDocument() { return document; }
    public int getVersionNumber() { return versionNumber; }
    public String getFilename() { return filename; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getChecksumSha256() { return checksumSha256; }
    public Instant getUploadedAt() { return uploadedAt; }
    public boolean isExternallyPublished() { return externallyPublished; }
    public long getOptimisticVersion() { return optimisticVersion; }
}
