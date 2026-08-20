package pe.gob.midagri.piip.portfolio.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface ResponsibleUnitRepository extends JpaRepository<ResponsibleUnitEntity, Long> {
    @EntityGraph(attributePaths = {"organizationalUnit", "organizationalUnit.parent", "organizationalUnit.executingUnit"})
    List<ResponsibleUnitEntity> findByRecordIdOrderByDisplayOrder(Long recordId);
}
