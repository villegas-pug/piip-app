package pe.gob.midagri.piip.documents.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.documents.persistence.*;
import java.util.*;

/** Migra las versiones previas a la multi-cardinalidad a archivos originales (research D4). */
@Service
public class DocumentMigrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentMigrationService.class);
    private final DocumentFileRepository files; private final DocumentVersionRepository versions;

    public DocumentMigrationService(DocumentFileRepository files, DocumentVersionRepository versions) {
        this.files = files; this.versions = versions;
    }

    @Transactional
    public DocumentMigrationReport migrate() {
        List<DocumentVersionEntity> pending = versions.findByFileIsNull();
        Map<Long, List<DocumentVersionEntity>> byDocument = new LinkedHashMap<>();
        for (DocumentVersionEntity version : pending) byDocument.computeIfAbsent(version.getDocument().getId(), key -> new ArrayList<>()).add(version);
        List<DocumentMigrationReport.Entry> entries = new ArrayList<>();
        for (List<DocumentVersionEntity> group : byDocument.values()) {
            DocumentEntity document = group.get(0).getDocument();
            int latest = group.stream().mapToInt(DocumentVersionEntity::getVersionNumber).max().orElse(0);
            DocumentFileEntity file = files.save(new DocumentFileEntity(document, true, latest));
            group.forEach(version -> version.assignFile(file));
            entries.add(new DocumentMigrationReport.Entry(document.getRecord().getCode(), document.getType().getCode(), document.getType().getName(), 1, group.size()));
            LOGGER.info("Migración documental: expediente={} tipo={} archivosCreados=1 versionesAsignadas={}",
                document.getRecord().getCode(), document.getType().getCode(), group.size());
        }
        DocumentMigrationReport report = new DocumentMigrationReport(byDocument.size(), entries.size(), pending.size(), List.copyOf(entries));
        LOGGER.info("Migración documental completada: posiciones={} archivosCreados={} versionesAsignadas={}",
            report.positions(), report.filesCreated(), report.versionsAssigned());
        return report;
    }
}
