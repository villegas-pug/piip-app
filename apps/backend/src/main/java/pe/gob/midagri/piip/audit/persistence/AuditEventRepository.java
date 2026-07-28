package pe.gob.midagri.piip.audit.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
    List<AuditEventEntity> findTop100ByOrderByOccurredAtDesc();
    List<AuditEventEntity> findByEntityCodeOrderByOccurredAtAsc(String entityCode);
}
