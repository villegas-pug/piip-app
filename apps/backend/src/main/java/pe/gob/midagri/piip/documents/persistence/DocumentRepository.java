package pe.gob.midagri.piip.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.*;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    @EntityGraph(attributePaths = "type")
    List<DocumentEntity> findByRecordIdOrderByTypeDisplayOrderAscTypeCodeAsc(Long recordId);
    @EntityGraph(attributePaths = "type")
    Optional<DocumentEntity> findByRecordIdAndTypeId(Long recordId, Long typeId);
}
