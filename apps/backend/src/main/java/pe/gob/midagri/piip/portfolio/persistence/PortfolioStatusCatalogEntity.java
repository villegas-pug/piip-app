package pe.gob.midagri.piip.portfolio.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;

@Entity
@Check(constraints = "ORDEN_PRESENTACION >= 0")
@Table(name = "ESTADO_PORTAFOLIO",
    indexes = @Index(name = "IDX_ESTADO_PORTAFOLIO_ORDEN", columnList = "ACTIVO,APLICABILIDAD,ORDEN_PRESENTACION,CODIGO"))
public class PortfolioStatusCatalogEntity {
    @Id @Enumerated(EnumType.STRING)
    @Column(name = "CODIGO", length = 40, nullable = false) private PortfolioStatus code;
    @Column(name = "NOMBRE", length = 180, nullable = false) private String name;
    @Column(name = "ORDEN_PRESENTACION", nullable = false) private int displayOrder;
    @Column(name = "ACTIVO", nullable = false) private boolean active;
    @Enumerated(EnumType.STRING)
    @Column(name = "APLICABILIDAD", length = 20, nullable = false) private PortfolioStatusApplicability applicability;

    protected PortfolioStatusCatalogEntity() {}
    public PortfolioStatusCatalogEntity(PortfolioStatus code, String name, int displayOrder, boolean active, PortfolioStatusApplicability applicability) {
        this.code = code; this.name = name; this.displayOrder = displayOrder; this.active = active; this.applicability = applicability;
    }
    public PortfolioStatus getCode() { return code; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public PortfolioStatusApplicability getApplicability() { return applicability; }
    public void rename(String name) { this.name = name; }
    public void deactivate() { this.active = false; }
}
