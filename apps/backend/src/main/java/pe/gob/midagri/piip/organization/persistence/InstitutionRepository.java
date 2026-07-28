package pe.gob.midagri.piip.organization.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<InstitutionEntity, Long> {
    Optional<InstitutionEntity> findByCodeIgnoreCase(String code);
}
