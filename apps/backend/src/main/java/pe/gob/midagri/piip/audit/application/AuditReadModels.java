package pe.gob.midagri.piip.audit.application;

import java.time.Instant;

public final class AuditReadModels {
    private AuditReadModels() {}

    public record AccessView(String subject, String roles, String method, String path, int status,
            String recordCode, String correlationId, long durationMs, String safeReason, Instant occurredAt) {}

    public record EventView(String event, String entityCode, String detail, String actor,
            String actorName, String actorEmail, Instant occurredAt) {}
}
