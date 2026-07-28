package pe.gob.midagri.piip.audit.api;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.audit.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {
    private final AccessAuditRepository accesses; private final AuditEventRepository events; private final LocalAuthorizationService authorization;
    public AuditController(AccessAuditRepository accesses, AuditEventRepository events, LocalAuthorizationService authorization) { this.accesses = accesses; this.events = events; this.authorization = authorization; }
    @GetMapping("/accesses") @Transactional(readOnly = true) public List<AccessResponse> accesses() { authorization.require(RoleCode.ADMINISTRADOR_PIIP); return accesses.findTop100ByOrderByOccurredAtDesc().stream().map(value -> new AccessResponse(value.getKeycloakSubject(), value.getRoleSnapshot(), value.getHttpMethod(), value.getNormalizedPath(), value.getResponseCode(), value.getRecordCode(), value.getCorrelationId(), value.getDurationMs(), value.getOccurredAt())).toList(); }
    @GetMapping("/events") @Transactional(readOnly = true) public List<EventResponse> events() { authorization.require(RoleCode.ADMINISTRADOR_PIIP); return events.findTop100ByOrderByOccurredAtDesc().stream().map(value -> new EventResponse(value.getEventType(), value.getEntityCode(), value.getDetailJson(), value.getActorSubject(), value.getOccurredAt())).toList(); }
    public record AccessResponse(String subject, String roles, String method, String path, int status, String recordCode, String correlationId, long durationMs, Instant occurredAt) {}
    public record EventResponse(String event, String entityCode, String detail, String actor, Instant occurredAt) {}
}
