package pe.gob.midagri.piip.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByCode(RoleCode code);
}
