package pe.gob.midagri.piip.catalogs.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "ORDEN_PRESENTACION >= 0")
@Table(name = "CATALOGO", uniqueConstraints = @UniqueConstraint(name = "UK_CATALOGO_CODIGO", columnNames = "CODIGO"))
public class CatalogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CATALOGO") private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "CODIGO", length = 40, nullable = false) private CatalogCode code;
    @Column(name = "NOMBRE", length = 180, nullable = false) private String name;
    @Column(name = "ORDEN_PRESENTACION", nullable = false) private int displayOrder;
    @Column(name = "ACTIVO", nullable = false) private boolean active;

    protected CatalogEntity() {}
    public CatalogEntity(CatalogCode code, String name, int displayOrder, boolean active) {
        this.code = code; this.name = name; this.displayOrder = displayOrder; this.active = active;
    }
    public Long getId() { return id; }
    public CatalogCode getCode() { return code; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
}
