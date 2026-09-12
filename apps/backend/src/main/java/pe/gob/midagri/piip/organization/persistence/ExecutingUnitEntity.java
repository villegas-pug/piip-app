package pe.gob.midagri.piip.organization.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "UNIDAD_EJECUTORA", uniqueConstraints = @UniqueConstraint(name = "UK_UE_INSTITUCION_CODIGO", columnNames = {"ID_INSTITUCION", "CODIGO"}))
public class ExecutingUnitEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_UNIDAD_EJECUTORA") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_INSTITUCION", nullable = false, foreignKey = @ForeignKey(name = "FK_UE_INSTITUCION")) private InstitutionEntity institution;
    @Column(name = "CODIGO", length = 30, nullable = false) private String code;
    @Column(name = "NOMBRE", length = 200, nullable = false) private String name;
    @Column(name = "ACTIVO", nullable = false) private boolean active = true;
    @Column(name = "ORDEN_PRESENTACION", nullable = false) private int displayOrder;
    @Column(name = "FECHA_REGISTRO", nullable = false) private Instant registeredAt;
    @Column(name = "FECHA_ACTIVACION", nullable = false) private Instant activatedAt;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected ExecutingUnitEntity() {}
    public ExecutingUnitEntity(InstitutionEntity institution, String code, String name) {
        this(institution, code, name, 0, Instant.now());
    }
    public ExecutingUnitEntity(InstitutionEntity institution, String code, String name, int displayOrder, Instant registeredAt) {
        this.institution = institution;
        this.code = code;
        this.name = name;
        this.displayOrder = displayOrder;
        this.registeredAt = registeredAt;
        this.activatedAt = registeredAt;
    }
    public Long getId() { return id; }
    public InstitutionEntity getInstitution() { return institution; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public int getDisplayOrder() { return displayOrder; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public long getVersion() { return version; }

    public void updateDetails(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public void deactivate() { active = false; }

    public void reactivate(Instant when) {
        active = true;
        activatedAt = when;
    }
}
