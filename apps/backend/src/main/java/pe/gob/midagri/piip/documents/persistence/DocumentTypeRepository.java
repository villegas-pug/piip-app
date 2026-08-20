package pe.gob.midagri.piip.documents.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTypeRepository extends JpaRepository<DocumentTypeEntity, Long> {
    List<DocumentTypeEntity> findByActiveTrueOrderByDisplayOrderAscCodeAsc();
    Optional<DocumentTypeEntity> findByCodeIgnoreCaseAndActiveTrue(String code);
}
