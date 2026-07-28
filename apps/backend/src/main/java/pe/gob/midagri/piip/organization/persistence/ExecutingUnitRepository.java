package pe.gob.midagri.piip.organization.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ExecutingUnitRepository extends JpaRepository<ExecutingUnitEntity, Long> {
    List<ExecutingUnitEntity> findByInstitutionIdAndActiveTrueOrderByName(Long institutionId);
    Optional<ExecutingUnitEntity> findByInstitutionIdAndCodeIgnoreCase(Long institutionId, String code);
}
