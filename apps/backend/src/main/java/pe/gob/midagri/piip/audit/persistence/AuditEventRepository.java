package pe.gob.midagri.piip.audit.persistence;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
    @EntityGraph(attributePaths = "user")
    List<AuditEventEntity> findTop100ByOrderByOccurredAtDesc();
    @EntityGraph(attributePaths = "user")
    List<AuditEventEntity> findTop100ByEntityCodeInOrderByOccurredAtDesc(java.util.Collection<String> entityCodes);
    List<AuditEventEntity> findByEntityCodeOrderByOccurredAtAsc(String entityCode);
}
