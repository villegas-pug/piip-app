package pe.gob.midagri.piip.documents.application;

import java.util.List;

/** Conteos de evidencia de la migración documental por expediente y tipo (FR-023). */
public record DocumentMigrationReport(int positions, int filesCreated, int versionsAssigned, List<Entry> entries) {
    public record Entry(String recordCode, String documentTypeCode, String documentTypeName, int filesCreated, int versionsAssigned) {}
}
