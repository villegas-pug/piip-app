package pe.gob.midagri.piip.audit.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import java.time.Instant;

@Entity
@Table(name = "EVENTO_AUDITORIA", indexes = @Index(name = "IDX_EVENTO_ENTIDAD", columnList = "TIPO_ENTIDAD,CODIGO_ENTIDAD"))
public class AuditEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EVENTO") private Long id;
    @Column(name = "TIPO_EVENTO", length = 100, nullable = false) private String eventType;
    @Column(name = "TIPO_ENTIDAD", length = 60, nullable = false) private String entityType;
    @Column(name = "CODIGO_ENTIDAD", length = 40, nullable = false) private String entityCode;
    @Lob @Column(name = "DETALLE_JSON", nullable = false) private String detailJson;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ID_USUARIO") private UserEntity user;
    @Column(name = "ACTOR_SUBJECT", length = 100, nullable = false) private String actorSubject;
    @Column(name = "FECHA_EVENTO", nullable = false) private Instant occurredAt = Instant.now();

    protected AuditEventEntity() {}
    public AuditEventEntity(String eventType, String entityType, String entityCode, String detailJson, UserEntity user, String actorSubject) {
        this.eventType = eventType; this.entityType = entityType; this.entityCode = entityCode; this.detailJson = detailJson; this.user = user; this.actorSubject = actorSubject;
    }
    public String getEventType() { return eventType; }
    public String getEntityCode() { return entityCode; }
    public String getDetailJson() { return detailJson; }
    public String getActorSubject() { return actorSubject; }
    public Instant getOccurredAt() { return occurredAt; }
}
