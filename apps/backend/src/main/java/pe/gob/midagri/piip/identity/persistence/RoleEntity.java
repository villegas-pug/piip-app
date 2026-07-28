package pe.gob.midagri.piip.identity.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;

@Entity
@Table(name = "ROL", uniqueConstraints = @UniqueConstraint(name = "UK_ROL_CODIGO", columnNames = "CODIGO"))
public class RoleEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ROL") private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "CODIGO", length = 40, nullable = false) private RoleCode code;
    @Column(name = "NOMBRE", length = 100, nullable = false) private String name;
    @Column(name = "ACTIVO", nullable = false) private boolean active = true;
    @Column(name = "SISTEMA", nullable = false) private boolean system = true;

    protected RoleEntity() {}
    public RoleEntity(RoleCode code, String name) { this.code = code; this.name = name; }
    public Long getId() { return id; }
    public RoleCode getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
