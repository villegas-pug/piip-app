package pe.gob.midagri.piip.work.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.work.domain.*;
import java.time.*;

@Entity
@Table(name = "TAREA_TRABAJO", indexes = @Index(name = "IDX_TAREA_ASIGNADO_ESTADO", columnList = "ID_USUARIO_ASIGNADO,ESTADO"))
public class WorkTaskEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TAREA") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ID_REGISTRO", nullable = false) private PortfolioRecordEntity record;
    @Enumerated(EnumType.STRING) @Column(name = "TIPO_TAREA", length = 40, nullable = false) private TaskType type;
    @Column(name = "DESCRIPCION", length = 400, nullable = false) private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ID_USUARIO_ASIGNADO", nullable = false) private UserEntity assignedUser;
    @Enumerated(EnumType.STRING) @Column(name = "PRIORIDAD", length = 20, nullable = false) private TaskPriority priority;
    @Enumerated(EnumType.STRING) @Column(name = "ESTADO", length = 20, nullable = false) private TaskStatus status = TaskStatus.PENDING;
    @Column(name = "FECHA_VENCIMIENTO") private LocalDate dueDate;
    @Column(name = "EVENTO_ORIGEN", length = 100, nullable = false) private String sourceEvent;
    @Column(name = "FECHA_CREACION", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "FECHA_COMPLETADO") private Instant completedAt;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected WorkTaskEntity() {}
    public WorkTaskEntity(PortfolioRecordEntity record, TaskType type, String description, UserEntity assignedUser, TaskPriority priority, LocalDate dueDate, String sourceEvent) {
        this.record = record; this.type = type; this.description = description; this.assignedUser = assignedUser;
        this.priority = priority; this.dueDate = dueDate; this.sourceEvent = sourceEvent;
    }
    public void complete() { if (status == TaskStatus.PENDING) { status = TaskStatus.COMPLETED; completedAt = Instant.now(); } }
    public void reassign(UserEntity user) { if (status != TaskStatus.PENDING) throw new IllegalStateException("Solo se reasignan tareas pendientes"); assignedUser = user; }
    public Long getId() { return id; }
    public PortfolioRecordEntity getRecord() { return record; }
    public TaskType getType() { return type; }
    public String getDescription() { return description; }
    public UserEntity getAssignedUser() { return assignedUser; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public long getVersion() { return version; }
}
