package pe.gob.midagri.piip.organization.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "UNIDAD_ORGANICA", uniqueConstraints = @UniqueConstraint(name = "UK_UO_EJECUTORA_CODIGO", columnNames = {"ID_UNIDAD_EJECUTORA", "CODIGO"}))
public class OrganizationalUnitEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_UNIDAD_ORGANICA") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_UNIDAD_EJECUTORA", nullable = false) private ExecutingUnitEntity executingUnit;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_UNIDAD_PADRE") private OrganizationalUnitEntity parent;
    @Column(name = "CODIGO", length = 30, nullable = false) private String code;
    @Column(name = "NOMBRE", length = 200, nullable = false) private String name;
    @Column(name = "SIGLA", length = 30) private String acronym;
    @Column(name = "ACTIVO", nullable = false) private boolean active = true;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected OrganizationalUnitEntity() {}
    public OrganizationalUnitEntity(ExecutingUnitEntity executingUnit, String code, String name, String acronym) {
        this.executingUnit = executingUnit; this.code = code; this.name = name; this.acronym = acronym;
    }
    public Long getId() { return id; }
    public ExecutingUnitEntity getExecutingUnit() { return executingUnit; }
    public OrganizationalUnitEntity getParent() { return parent; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAcronym() { return acronym; }
    public boolean isActive() { return active; }
}
