package pe.gob.midagri.piip.portfolio.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;

public interface PortfolioStatusRepository extends JpaRepository<PortfolioStatusCatalogEntity, PortfolioStatus> {
    Optional<PortfolioStatusCatalogEntity> findByCode(PortfolioStatus code);
    List<PortfolioStatusCatalogEntity> findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();
    List<PortfolioStatusCatalogEntity> findAllByOrderByDisplayOrderAscCodeAsc();
}
