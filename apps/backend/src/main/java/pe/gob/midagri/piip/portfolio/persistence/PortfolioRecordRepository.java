package pe.gob.midagri.piip.portfolio.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import pe.gob.midagri.piip.portfolio.domain.*;
import java.util.*;

public interface PortfolioRecordRepository extends JpaRepository<PortfolioRecordEntity, Long>, JpaSpecificationExecutor<PortfolioRecordEntity> {
    @Override
    @EntityGraph(attributePaths = {"executingUnit", "originRecord", "solutionType", "sourceOrigin", "peiObjective", "poiActivity"})
    Page<PortfolioRecordEntity> findAll(Specification<PortfolioRecordEntity> specification, Pageable pageable);
    @EntityGraph(attributePaths = {"executingUnit", "originRecord", "solutionType", "sourceOrigin", "peiObjective", "poiActivity"})
    Optional<PortfolioRecordEntity> findByCodeIgnoreCase(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"executingUnit", "originRecord", "solutionType", "sourceOrigin", "peiObjective", "poiActivity"})
    @Query("select record from PortfolioRecordEntity record where lower(record.code) = lower(:code)")
    Optional<PortfolioRecordEntity> findByCodeIgnoreCaseForUpdate(@Param("code") String code);
    @EntityGraph(attributePaths = {"executingUnit", "originRecord", "solutionType", "sourceOrigin", "peiObjective", "poiActivity"})
    List<PortfolioRecordEntity> findByRecordTypeOrderByUpdatedAtDesc(RecordType type);
    boolean existsByOriginRecordId(Long originId);
    @EntityGraph(attributePaths = {"executingUnit", "originRecord", "solutionType", "sourceOrigin", "peiObjective", "poiActivity"})
    List<PortfolioRecordEntity> findByRecordTypeAndStatusOrderByUpdatedAtDesc(RecordType type, PortfolioStatus status);
    @EntityGraph(attributePaths = {"executingUnit", "originRecord", "solutionType", "sourceOrigin", "peiObjective", "poiActivity"})
    List<PortfolioRecordEntity> findByExecutingUnit_IdOrderByUpdatedAtDesc(Long executingUnitId);
    List<PortfolioRecordEntity> findByExecutingUnit_Id(Long executingUnitId);
}
