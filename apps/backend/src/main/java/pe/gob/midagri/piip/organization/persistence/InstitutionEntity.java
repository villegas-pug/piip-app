package pe.gob.midagri.piip.organization.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "INSTITUCION", uniqueConstraints = @UniqueConstraint(name = "UK_INSTITUCION_CODIGO", columnNames = "CODIGO"))
public class InstitutionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_INSTITUCION") private Long id;
    @Column(name = "CODIGO", length = 30, nullable = false) private String code;
    @Column(name = "NOMBRE", length = 200, nullable = false) private String name;
    @Column(name = "ACTIVO", nullable = false) private boolean active = true;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected InstitutionEntity() {}
    public InstitutionEntity(String code, String name) { this.code = code; this.name = name; }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
