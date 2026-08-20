package pe.gob.midagri.piip.organization.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrganizationalUnitRepository extends JpaRepository<OrganizationalUnitEntity, Long> {
    List<OrganizationalUnitEntity> findByExecutingUnitIdAndActiveTrueOrderByName(Long executingUnitId);
    @EntityGraph(attributePaths = {"executingUnit", "parent"})
    @Query("select unit from OrganizationalUnitEntity unit where unit.id = :id")
    Optional<OrganizationalUnitEntity> findHistoricalById(@Param("id") Long id);
}
