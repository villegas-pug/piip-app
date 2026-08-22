package pe.gob.midagri.piip.audit.api;

import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.midagri.piip.audit.application.AuditQueryService;
import pe.gob.midagri.piip.audit.application.AuditReadModels;

@RestController
@RequestMapping("/audit")
public class AuditController {
    private final AuditQueryService service;

    public AuditController(AuditQueryService service) { this.service = service; }

    @GetMapping(value = "/accesses", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AccessResponse> accesses(@RequestParam(value = "executingUnitId", required = false) Long executingUnitId) {
        return service.accesses(executingUnitId).stream()
            .map(value -> new AccessResponse(value.subject(), value.roles(), value.method(), value.path(), value.status(),
                value.recordCode(), value.correlationId(), value.durationMs(), value.occurredAt())).toList();
    }

    @GetMapping(value = "/events", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EventResponse> events(@RequestParam(value = "executingUnitId", required = false) Long executingUnitId) {
        return service.events(executingUnitId).stream().map(AuditController::toEventResponse).toList();
    }

    static EventResponse toEventResponse(AuditReadModels.EventView event) {
        return new EventResponse(event.event(), event.entityCode(), event.detail(), event.actor(), event.actorName(),
            event.actorEmail(), event.occurredAt());
    }

    public record AccessResponse(String subject, String roles, String method, String path, int status,
            String recordCode, String correlationId, long durationMs, Instant occurredAt) {}
    public record EventResponse(String event, String entityCode, String detail, String actor, String actorName,
            String actorEmail, Instant occurredAt) {}
}
