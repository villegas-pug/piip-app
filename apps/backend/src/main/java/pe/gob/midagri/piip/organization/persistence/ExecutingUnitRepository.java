package pe.gob.midagri.piip.organization.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ExecutingUnitRepository extends JpaRepository<ExecutingUnitEntity, Long> {
    List<ExecutingUnitEntity> findByInstitutionIdAndActiveTrueOrderByName(Long institutionId);
    @EntityGraph(attributePaths = "institution")
    List<ExecutingUnitEntity> findByInstitutionIdOrderByDisplayOrderAscNameAscIdAsc(Long institutionId);
    @Query("select coalesce(max(unit.displayOrder), -1) from ExecutingUnitEntity unit where unit.institution.id = :institutionId")
    int findMaxDisplayOrderByInstitutionId(@Param("institutionId") Long institutionId);
    List<ExecutingUnitEntity> findByInstitutionIdIn(Collection<Long> institutionIds);
    Optional<ExecutingUnitEntity> findByInstitutionIdAndCodeIgnoreCase(Long institutionId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "institution")
    @Query("select unit from ExecutingUnitEntity unit where unit.id = :id")
    Optional<ExecutingUnitEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "institution")
    @Query("select unit from ExecutingUnitEntity unit where unit.institution.id = :institutionId and lower(unit.code) = lower(:code)")
    Optional<ExecutingUnitEntity> findByInstitutionIdAndCodeIgnoreCaseForUpdate(@Param("institutionId") Long institutionId,
            @Param("code") String code);
}
