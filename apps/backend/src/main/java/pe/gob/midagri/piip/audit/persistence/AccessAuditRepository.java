package pe.gob.midagri.piip.audit.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccessAuditRepository extends JpaRepository<AccessAuditEntity, Long> {
    java.util.List<AccessAuditEntity> findTop100ByOrderByOccurredAtDesc();
}
