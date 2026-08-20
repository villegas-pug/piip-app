package pe.gob.midagri.piip.catalogs.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "ORDEN_PRESENTACION >= 0")
@Table(name = "CATALOGO_ITEM",
    uniqueConstraints = @UniqueConstraint(name = "UK_CATALOGO_ITEM_CODIGO", columnNames = {"ID_CATALOGO", "CODIGO"}),
    indexes = @Index(name = "IDX_CATALOGO_ITEM_ORDEN", columnList = "ID_CATALOGO,ACTIVO,ORDEN_PRESENTACION,CODIGO"))
public class CatalogItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CATALOGO_ITEM") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_CATALOGO", nullable = false) private CatalogEntity catalog;
    @Column(name = "CODIGO", length = 60, nullable = false) private String code;
    @Column(name = "NOMBRE", length = 500, nullable = false) private String name;
    @Column(name = "ORDEN_PRESENTACION", nullable = false) private int displayOrder;
    @Column(name = "ACTIVO", nullable = false) private boolean active;

    protected CatalogItemEntity() {}
    public CatalogItemEntity(CatalogEntity catalog, String code, String name, int displayOrder, boolean active) {
        this.catalog = catalog; this.code = code; this.name = name; this.displayOrder = displayOrder; this.active = active;
    }
    public Long getId() { return id; }
    public CatalogEntity getCatalog() { return catalog; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public void rename(String name) { this.name = name; }
    public void deactivate() { this.active = false; }
}
