package pe.gob.midagri.piip.portfolio.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.gob.midagri.piip.portfolio.domain.*;
import java.util.*;

public interface PortfolioRecordRepository extends JpaRepository<PortfolioRecordEntity, Long>, JpaSpecificationExecutor<PortfolioRecordEntity> {
    @EntityGraph(attributePaths = {"executingUnit", "originRecord"})
    Optional<PortfolioRecordEntity> findByCodeIgnoreCase(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"executingUnit", "originRecord"})
    @Query("select record from PortfolioRecordEntity record where lower(record.code) = lower(:code)")
    Optional<PortfolioRecordEntity> findByCodeIgnoreCaseForUpdate(@Param("code") String code);
    List<PortfolioRecordEntity> findByRecordTypeOrderByUpdatedAtDesc(RecordType type);
    boolean existsByOriginRecordId(Long originId);
    List<PortfolioRecordEntity> findByRecordTypeAndStatusOrderByUpdatedAtDesc(RecordType type, PortfolioStatus status);
}
