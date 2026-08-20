package pe.gob.midagri.piip.catalogs.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;

public interface CatalogItemRepository extends JpaRepository<CatalogItemEntity, Long> {
    List<CatalogItemEntity> findByCatalogCodeAndCatalogActiveTrueAndActiveTrueOrderByDisplayOrderAscCodeAsc(CatalogCode code);
    Optional<CatalogItemEntity> findByCatalogCodeAndCatalogActiveTrueAndCodeIgnoreCaseAndActiveTrue(CatalogCode catalogCode, String code);
}
