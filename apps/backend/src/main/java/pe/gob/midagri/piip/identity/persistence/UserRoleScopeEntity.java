package pe.gob.midagri.piip.identity.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "USUARIO_ROL_AMBITO", indexes = {
    @Index(name = "IDX_URA_USUARIO_ACTIVO", columnList = "ID_USUARIO,ACTIVO"),
    @Index(name = "IDX_URA_EJECUTORA", columnList = "ID_UNIDAD_EJECUTORA")
})
public class UserRoleScopeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO_ROL_AMBITO") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_USUARIO", nullable = false) private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_ROL", nullable = false) private RoleEntity role;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_INSTITUCION", nullable = false) private InstitutionEntity institution;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_UNIDAD_EJECUTORA") private ExecutingUnitEntity executingUnit;
    @Column(name = "ACTIVO", nullable = false) private boolean active = true;
    @Column(name = "VIGENTE_DESDE", nullable = false) private Instant validFrom;
    @Column(name = "VIGENTE_HASTA") private Instant validUntil;
    @Column(name = "ASIGNADO_POR", length = 100, nullable = false) private String assignedBy;
    @Column(name = "FECHA_ASIGNACION", nullable = false) private Instant assignedAt;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected UserRoleScopeEntity() {}
    public UserRoleScopeEntity(UserEntity user, RoleEntity role, InstitutionEntity institution, ExecutingUnitEntity executingUnit, String assignedBy) {
        this.user = user; this.role = role; this.institution = institution; this.executingUnit = executingUnit;
        this.assignedBy = assignedBy; this.validFrom = Instant.now(); this.assignedAt = Instant.now();
    }
    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public RoleEntity getRole() { return role; }
    public InstitutionEntity getInstitution() { return institution; }
    public ExecutingUnitEntity getExecutingUnit() { return executingUnit; }
    public boolean isActive() { return active; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public long getVersion() { return version; }
    public boolean isActiveNow(Instant now) { return active && !validFrom.isAfter(now) && (validUntil == null || validUntil.isAfter(now)); }
    public void suspend(Instant when) { active = false; validUntil = when; }
    public void update(RoleEntity role, InstitutionEntity institution, ExecutingUnitEntity executingUnit) {
        this.role = role;
        this.institution = institution;
        this.executingUnit = executingUnit;
    }
    public void reactivate(Instant when) {
        active = true;
        validFrom = when;
        validUntil = null;
    }
}
