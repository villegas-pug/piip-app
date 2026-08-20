package pe.gob.midagri.piip.catalogs.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;

public interface CatalogRepository extends JpaRepository<CatalogEntity, Long> {
    Optional<CatalogEntity> findByCode(CatalogCode code);
}
