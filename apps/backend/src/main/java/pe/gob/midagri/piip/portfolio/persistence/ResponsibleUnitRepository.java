package pe.gob.midagri.piip.portfolio.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResponsibleUnitRepository extends JpaRepository<ResponsibleUnitEntity, Long> {
    List<ResponsibleUnitEntity> findByRecordIdOrderByDisplayOrder(Long recordId);
}
