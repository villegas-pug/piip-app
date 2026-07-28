package pe.gob.midagri.piip.organization.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrganizationalUnitRepository extends JpaRepository<OrganizationalUnitEntity, Long> {
    List<OrganizationalUnitEntity> findByExecutingUnitIdAndActiveTrueOrderByName(Long executingUnitId);
}
