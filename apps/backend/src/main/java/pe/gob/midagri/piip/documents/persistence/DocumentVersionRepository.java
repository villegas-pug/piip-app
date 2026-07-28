package pe.gob.midagri.piip.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, Long> {
    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNumberDesc(Long documentId);
    Optional<DocumentVersionEntity> findFirstByDocumentIdOrderByVersionNumberDesc(Long documentId);
}
