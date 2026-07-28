package pe.gob.midagri.piip.portfolio.persistence;

import org.springframework.data.jpa.repository.*;
import pe.gob.midagri.piip.portfolio.domain.*;
import java.util.*;

public interface PortfolioRecordRepository extends JpaRepository<PortfolioRecordEntity, Long>, JpaSpecificationExecutor<PortfolioRecordEntity> {
    @EntityGraph(attributePaths = {"executingUnit", "originRecord"})
    Optional<PortfolioRecordEntity> findByCodeIgnoreCase(String code);
    List<PortfolioRecordEntity> findByRecordTypeOrderByUpdatedAtDesc(RecordType type);
    boolean existsByOriginRecordId(Long originId);
    List<PortfolioRecordEntity> findByRecordTypeAndStatusOrderByUpdatedAtDesc(RecordType type, PortfolioStatus status);
}
