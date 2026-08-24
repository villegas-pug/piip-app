package pe.gob.midagri.piip.audit.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import java.time.Instant;

@Entity
@Table(name = "AUDITORIA_ACCESO", indexes = @Index(name = "IDX_AUDIT_ACCESO_FECHA", columnList = "FECHA_HORA"))
public class AccessAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_AUDITORIA_ACCESO") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ID_USUARIO") private UserEntity user;
    @Column(name = "KEYCLOAK_SUBJECT", length = 100) private String keycloakSubject;
    @Column(name = "ROL_SNAPSHOT", length = 100) private String roleSnapshot;
    @Column(name = "METODO_HTTP", length = 10, nullable = false) private String httpMethod;
    @Column(name = "RUTA_NORMALIZADA", length = 300, nullable = false) private String normalizedPath;
    @Column(name = "CODIGO_RESPUESTA", nullable = false) private int responseCode;
    @Column(name = "MOTIVO_SEGURO", length = 100) private String safeReason;
    @Column(name = "CODIGO_REGISTRO", length = 30) private String recordCode;
    @Column(name = "IP_CLIENTE", length = 45) private String clientIp;
    @Column(name = "CORRELATION_ID", length = 80, nullable = false) private String correlationId;
    @Column(name = "DURACION_MS", nullable = false) private long durationMs;
    @Column(name = "FECHA_HORA", nullable = false) private Instant occurredAt;

    protected AccessAuditEntity() {}
    public AccessAuditEntity(UserEntity user, String subject, String roles, String method, String path, int status, String recordCode, String ip, String correlationId, long durationMs) {
        this(user, subject, roles, method, path, status, recordCode, ip, correlationId, durationMs, null);
    }
    public AccessAuditEntity(UserEntity user, String subject, String roles, String method, String path, int status, String recordCode, String ip, String correlationId, long durationMs, String safeReason) {
        this.user = user; this.keycloakSubject = subject; this.roleSnapshot = roles; this.httpMethod = method; this.normalizedPath = path;
        this.responseCode = status; this.safeReason = safeReason; this.recordCode = recordCode; this.clientIp = ip; this.correlationId = correlationId; this.durationMs = durationMs; this.occurredAt = Instant.now();
    }
    public String getKeycloakSubject() { return keycloakSubject; }
    public String getRoleSnapshot() { return roleSnapshot; }
    public String getHttpMethod() { return httpMethod; }
    public String getNormalizedPath() { return normalizedPath; }
    public int getResponseCode() { return responseCode; }
    public String getSafeReason() { return safeReason; }
    public String getRecordCode() { return recordCode; }
    public String getCorrelationId() { return correlationId; }
    public long getDurationMs() { return durationMs; }
    public Instant getOccurredAt() { return occurredAt; }
}
