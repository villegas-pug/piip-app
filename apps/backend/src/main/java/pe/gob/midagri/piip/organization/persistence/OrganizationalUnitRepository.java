package pe.gob.midagri.piip.organization.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrganizationalUnitRepository extends JpaRepository<OrganizationalUnitEntity, Long> {
    List<OrganizationalUnitEntity> findByExecutingUnitIdAndActiveTrueOrderByName(Long executingUnitId);
    /** Opciones para nuevas asociaciones: solo unidades activas de la Unidad Ejecutora con sigla registrada. */
    List<OrganizationalUnitEntity> findByExecutingUnitIdAndActiveTrueAndAcronymIsNotNullOrderByName(Long executingUnitId);
    /** Todas las unidades de la Unidad Ejecutora (activas e inactivas), para postvalidación del reset. */
    List<OrganizationalUnitEntity> findByExecutingUnitIdOrderByCode(Long executingUnitId);
    @EntityGraph(attributePaths = "executingUnit")
    List<OrganizationalUnitEntity> findByExecutingUnitIdOrderByNameAscIdAsc(Long executingUnitId);
    List<OrganizationalUnitEntity> findByExecutingUnitId(Long executingUnitId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "executingUnit")
    @Query("select unit from OrganizationalUnitEntity unit where unit.id = :id")
    Optional<OrganizationalUnitEntity> findByIdForUpdate(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "executingUnit")
    @Query("select unit from OrganizationalUnitEntity unit where unit.executingUnit.id = :executingUnitId and lower(unit.code) = lower(:code)")
    Optional<OrganizationalUnitEntity> findByExecutingUnitIdAndCodeIgnoreCaseForUpdate(@Param("executingUnitId") Long executingUnitId,
            @Param("code") String code);
    @EntityGraph(attributePaths = {"executingUnit", "parent"})
    @Query("select unit from OrganizationalUnitEntity unit where unit.id = :id")
    Optional<OrganizationalUnitEntity> findHistoricalById(@Param("id") Long id);
}
