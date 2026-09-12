package pe.gob.midagri.piip.organization.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<InstitutionEntity, Long> {
    Optional<InstitutionEntity> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select institution from InstitutionEntity institution where institution.id = :id")
    Optional<InstitutionEntity> findByIdForUpdate(@Param("id") Long id);
}
