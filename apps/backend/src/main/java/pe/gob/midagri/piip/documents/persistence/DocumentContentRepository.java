package pe.gob.midagri.piip.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentContentRepository extends JpaRepository<DocumentContentEntity, Long> {
    Optional<DocumentContentEntity> findByDocumentVersionId(Long versionId);
}
