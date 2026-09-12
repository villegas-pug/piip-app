package pe.gob.midagri.piip.audit.application;

import java.time.Instant;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioStatusReferenceResponse;

public final class AuditReadModels {
    private AuditReadModels() {}

    public record AccessView(String subject, String roles, String method, String path, int status,
            String recordCode, String correlationId, long durationMs, String safeReason, Instant occurredAt) {}

    public record EventView(String event, String entityCode, String detail, String actor,
            String actorName, String actorEmail, Instant occurredAt,
            PortfolioStatusReferenceResponse status, PortfolioStatusReferenceResponse previousStatus,
            PortfolioStatusReferenceResponse newStatus, String entityType, Long entityId,
            Long institutionId, Long executingUnitId, Long organizationalUnitId) {
        public EventView(String event, String entityCode, String detail, String actor,
                String actorName, String actorEmail, Instant occurredAt,
                PortfolioStatusReferenceResponse status, PortfolioStatusReferenceResponse previousStatus,
                PortfolioStatusReferenceResponse newStatus) {
            this(event, entityCode, detail, actor, actorName, actorEmail, occurredAt, status, previousStatus,
                newStatus, null, null, null, null, null);
        }
    }
}
