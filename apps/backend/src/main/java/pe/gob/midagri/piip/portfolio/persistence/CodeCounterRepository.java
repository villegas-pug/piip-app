package pe.gob.midagri.piip.portfolio.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import java.util.Optional;

public interface CodeCounterRepository extends JpaRepository<CodeCounterEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select counter from CodeCounterEntity counter where counter.recordType = :type and counter.year = :year")
    Optional<CodeCounterEntity> findForUpdate(@Param("type") RecordType type, @Param("year") int year);
}
