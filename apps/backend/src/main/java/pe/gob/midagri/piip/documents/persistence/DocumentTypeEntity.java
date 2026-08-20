package pe.gob.midagri.piip.documents.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "ORDEN_PRESENTACION >= 0")
@Table(name = "TIPO_DOCUMENTO", uniqueConstraints = @UniqueConstraint(name = "UK_TIPO_DOCUMENTO_CODIGO", columnNames = "CODIGO"),
    indexes = @Index(name = "IDX_TIPO_DOCUMENTO_ORDEN", columnList = "ACTIVO,ORDEN_PRESENTACION,CODIGO"))
public class DocumentTypeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TIPO_DOCUMENTO") private Long id;
    @Column(name = "CODIGO", length = 60, nullable = false) private String code;
    @Column(name = "NOMBRE", length = 180, nullable = false) private String name;
    @Column(name = "ORDEN_PRESENTACION", nullable = false) private int displayOrder;
    @Column(name = "ACTIVO", nullable = false) private boolean active;

    protected DocumentTypeEntity() {}
    public DocumentTypeEntity(String code, String name, int displayOrder, boolean active) {
        this.code = code; this.name = name; this.displayOrder = displayOrder; this.active = active;
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
}
