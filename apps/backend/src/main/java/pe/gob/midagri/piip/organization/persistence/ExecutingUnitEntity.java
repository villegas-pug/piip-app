package pe.gob.midagri.piip.organization.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "UNIDAD_EJECUTORA", uniqueConstraints = @UniqueConstraint(name = "UK_UE_INSTITUCION_CODIGO", columnNames = {"ID_INSTITUCION", "CODIGO"}))
public class ExecutingUnitEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_UNIDAD_EJECUTORA") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_INSTITUCION", nullable = false) private InstitutionEntity institution;
    @Column(name = "CODIGO", length = 30, nullable = false) private String code;
    @Column(name = "NOMBRE", length = 200, nullable = false) private String name;
    @Column(name = "ACTIVO", nullable = false) private boolean active = true;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected ExecutingUnitEntity() {}
    public ExecutingUnitEntity(InstitutionEntity institution, String code, String name) { this.institution = institution; this.code = code; this.name = name; }
    public Long getId() { return id; }
    public InstitutionEntity getInstitution() { return institution; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
