package pe.gob.midagri.piip.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DocumentFileRepository extends JpaRepository<DocumentFileEntity, Long> {
    List<DocumentFileEntity> findByDocumentIdAndDeletedFalseOrderByIdAsc(Long documentId);
    Optional<DocumentFileEntity> findFirstByDocumentIdAndOriginalTrueAndDeletedFalseOrderByIdAsc(Long documentId);
    Optional<DocumentFileEntity> findByIdAndDocumentRecordIdAndDeletedFalse(Long id, Long recordId);
}
