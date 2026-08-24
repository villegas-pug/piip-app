package pe.gob.midagri.piip.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import pe.gob.midagri.piip.audit.persistence.*;
import pe.gob.midagri.piip.identity.persistence.*;
import java.util.Map;

@Service
public class AuditService {
    private final AccessAuditRepository accesses;
    private final AuditEventRepository events;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    public AuditService(AccessAuditRepository accesses, AuditEventRepository events, UserRepository users, ObjectMapper objectMapper) {
        this.accesses = accesses; this.events = events; this.users = users; this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void access(String subject, String roles, String method, String path, int status, String recordCode, String ip, String correlationId, long durationMs) {
        access(subject, roles, method, path, status, recordCode, ip, correlationId, durationMs, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void access(String subject, String roles, String method, String path, int status, String recordCode, String ip, String correlationId, long durationMs, String safeReason) {
        UserEntity user = subject == null ? null : users.findByKeycloakSubject(subject).orElse(null);
        accesses.save(new AccessAuditEntity(user, subject, roles, method, path, status, recordCode, ip, correlationId, durationMs, safeReason));
    }

    @Transactional
    public void event(String type, String entityType, String entityCode, Map<String, ?> detail, String actorSubject) {
        UserEntity user = users.findByKeycloakSubject(actorSubject).orElse(null);
        events.save(new AuditEventEntity(type, entityType, entityCode, toJson(detail), user, actorSubject));
    }

    private String toJson(Map<String, ?> detail) {
        try { return objectMapper.writeValueAsString(detail); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("No se pudo serializar el evento de auditoría", exception); }
    }
}
