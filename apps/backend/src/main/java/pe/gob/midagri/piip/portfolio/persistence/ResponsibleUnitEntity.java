package pe.gob.midagri.piip.portfolio.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;

@Entity
@Table(name = "REGISTRO_UNIDAD_RESPONSABLE", uniqueConstraints = @UniqueConstraint(name = "UK_RUR_REGISTRO_ORDEN", columnNames = {"ID_REGISTRO", "ORDEN_PRESENTACION"}))
public class ResponsibleUnitEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REGISTRO_UNIDAD") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_REGISTRO", nullable = false) private PortfolioRecordEntity record;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_UNIDAD_ORGANICA") private OrganizationalUnitEntity organizationalUnit;
    @Column(name = "DENOMINACION_ORIGINAL", length = 300, nullable = false) private String originalDesignation;
    @Column(name = "ORDEN_PRESENTACION", nullable = false) private int displayOrder;

    protected ResponsibleUnitEntity() {}
    public ResponsibleUnitEntity(PortfolioRecordEntity record, OrganizationalUnitEntity organizationalUnit, String originalDesignation, int displayOrder) {
        this.record = record; this.organizationalUnit = organizationalUnit; this.originalDesignation = originalDesignation; this.displayOrder = displayOrder;
    }
    public PortfolioRecordEntity getRecord() { return record; }
    public OrganizationalUnitEntity getOrganizationalUnit() { return organizationalUnit; }
    public String getOriginalDesignation() { return originalDesignation; }
    public int getDisplayOrder() { return displayOrder; }
}
