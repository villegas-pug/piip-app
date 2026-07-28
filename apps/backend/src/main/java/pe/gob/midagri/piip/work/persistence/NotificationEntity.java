package pe.gob.midagri.piip.work.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import java.time.Instant;

@Entity
@Table(name = "NOTIFICACION", indexes = @Index(name = "IDX_NOTIF_DESTINATARIO_LEIDA", columnList = "ID_USUARIO_DESTINATARIO,LEIDA"))
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_NOTIFICACION") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ID_USUARIO_DESTINATARIO", nullable = false) private UserEntity recipient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ID_REGISTRO") private PortfolioRecordEntity record;
    @Column(name = "TIPO", length = 60, nullable = false) private String type;
    @Column(name = "MENSAJE", length = 500, nullable = false) private String message;
    @Column(name = "LEIDA", nullable = false) private boolean read;
    @Column(name = "FECHA_CREACION", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "FECHA_LECTURA") private Instant readAt;

    protected NotificationEntity() {}
    public NotificationEntity(UserEntity recipient, PortfolioRecordEntity record, String type, String message) { this.recipient = recipient; this.record = record; this.type = type; this.message = message; }
    public void markRead() { read = true; readAt = Instant.now(); }
    public Long getId() { return id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
