package pe.gob.midagri.piip.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.documents.domain.DocumentType;
import java.util.*;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByRecordIdOrderByType(Long recordId);
    Optional<DocumentEntity> findByRecordIdAndType(Long recordId, DocumentType type);
}
