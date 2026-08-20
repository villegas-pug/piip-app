package pe.gob.midagri.piip.audit.api;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.audit.application.AuditQueryService;
import pe.gob.midagri.piip.audit.persistence.*;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {
    private final AuditQueryService service;
    public AuditController(AuditQueryService service) { this.service = service; }
    @GetMapping(value = "/accesses", produces = MediaType.APPLICATION_JSON_VALUE) public List<AccessResponse> accesses(@RequestParam(value = "executingUnitId", required = false) Long executingUnitId) { return service.accesses(executingUnitId).stream().map(value -> new AccessResponse(value.getKeycloakSubject(), value.getRoleSnapshot(), value.getHttpMethod(), value.getNormalizedPath(), value.getResponseCode(), value.getRecordCode(), value.getCorrelationId(), value.getDurationMs(), value.getOccurredAt())).toList(); }
    @GetMapping(value = "/events", produces = MediaType.APPLICATION_JSON_VALUE) public List<EventResponse> events(@RequestParam(value = "executingUnitId", required = false) Long executingUnitId) { return service.events(executingUnitId).stream().map(AuditController::toEventResponse).toList(); }
    public record AccessResponse(String subject, String roles, String method, String path, int status, String recordCode, String correlationId, long durationMs, Instant occurredAt) {}
    public record EventResponse(String event, String entityCode, String detail, String actor, String actorName, String actorEmail, Instant occurredAt) {}

    static EventResponse toEventResponse(AuditEventEntity event) {
        UserEntity actor = event.getUser();
        return new EventResponse(event.getEventType(), event.getEntityCode(), event.getDetailJson(), event.getActorSubject(),
            actor == null ? null : actor.getFullName(), actor == null ? null : actor.getEmail(), event.getOccurredAt());
    }
}
