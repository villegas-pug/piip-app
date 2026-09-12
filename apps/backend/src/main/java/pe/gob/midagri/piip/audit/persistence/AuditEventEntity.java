package pe.gob.midagri.piip.audit.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import java.time.Instant;

@Entity
@Table(name = "EVENTO_AUDITORIA", indexes = {
    @Index(name = "IDX_EVENTO_ENTIDAD", columnList = "TIPO_ENTIDAD,CODIGO_ENTIDAD"),
    @Index(name = "IDX_EVENTO_AMBITO_UE", columnList = "ID_UNIDAD_EJECUTORA,FECHA_EVENTO")
})
public class AuditEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EVENTO") private Long id;
    @Column(name = "TIPO_EVENTO", length = 100, nullable = false) private String eventType;
    @Column(name = "TIPO_ENTIDAD", length = 60, nullable = false) private String entityType;
    @Column(name = "CODIGO_ENTIDAD", length = 40, nullable = false) private String entityCode;
    @Lob @Column(name = "DETALLE_JSON", nullable = false) private String detailJson;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ID_USUARIO", foreignKey = @ForeignKey(name = "FK_EVENTO_USUARIO")) private UserEntity user;
    @Column(name = "ACTOR_SUBJECT", length = 100, nullable = false) private String actorSubject;
    @Column(name = "FECHA_EVENTO", nullable = false) private Instant occurredAt = Instant.now();
    @Column(name = "ID_ENTIDAD") private Long entityId;
    @Column(name = "ID_INSTITUCION") private Long institutionId;
    @Column(name = "ID_UNIDAD_EJECUTORA") private Long executingUnitId;
    @Column(name = "ID_UNIDAD_ORGANICA") private Long organizationalUnitId;

    protected AuditEventEntity() {}
    public AuditEventEntity(String eventType, String entityType, String entityCode, String detailJson, UserEntity user, String actorSubject) {
        this.eventType = eventType; this.entityType = entityType; this.entityCode = entityCode; this.detailJson = detailJson; this.user = user; this.actorSubject = actorSubject;
    }
    public AuditEventEntity(String eventType, String entityType, Long entityId, String entityCode,
            Long institutionId, Long executingUnitId, Long organizationalUnitId, String detailJson,
            UserEntity user, String actorSubject) {
        this(eventType, entityType, entityCode, detailJson, user, actorSubject);
        this.entityId = entityId;
        this.institutionId = institutionId;
        this.executingUnitId = executingUnitId;
        this.organizationalUnitId = organizationalUnitId;
    }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public String getEntityCode() { return entityCode; }
    public String getDetailJson() { return detailJson; }
    public UserEntity getUser() { return user; }
    public String getActorSubject() { return actorSubject; }
    public Instant getOccurredAt() { return occurredAt; }
    public Long getEntityId() { return entityId; }
    public Long getInstitutionId() { return institutionId; }
    public Long getExecutingUnitId() { return executingUnitId; }
    public Long getOrganizationalUnitId() { return organizationalUnitId; }
}
