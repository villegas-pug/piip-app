package pe.gob.midagri.piip.documents.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "DOCUMENTO_CONTENIDO", uniqueConstraints = @UniqueConstraint(name = "UK_DOC_CONTENIDO_VERSION", columnNames = "ID_DOCUMENTO_VERSION"))
public class DocumentContentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DOCUMENTO_CONTENIDO") private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_DOCUMENTO_VERSION", nullable = false) private DocumentVersionEntity documentVersion;
    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(name = "CONTENIDO", nullable = false) private byte[] content;

    protected DocumentContentEntity() {}
    public DocumentContentEntity(DocumentVersionEntity documentVersion, byte[] content) { this.documentVersion = documentVersion; this.content = content.clone(); }
    public byte[] getContent() { return content.clone(); }
}
