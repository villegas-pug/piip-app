package pe.gob.midagri.piip.work.api;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.shared.api.NotFoundException;
import pe.gob.midagri.piip.work.persistence.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationRepository notifications; private final LocalAuthorizationService authorization;
    public NotificationController(NotificationRepository notifications, LocalAuthorizationService authorization) { this.notifications = notifications; this.authorization = authorization; }
    @GetMapping @Transactional(readOnly = true) public List<NotificationResponse> list() { Long userId = authorization.requireAuthenticatedRole().userId(); return notifications.findByRecipientIdOrderByCreatedAtDesc(userId).stream().map(value -> new NotificationResponse(value.getId(), value.getType(), value.getMessage(), value.isRead(), value.getCreatedAt())).toList(); }
    @PutMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void read(@PathVariable("id") Long id) { Long userId = authorization.requireAuthenticatedRole().userId(); NotificationEntity value = notifications.findById(id).orElseThrow(() -> new NotFoundException("Notificación inexistente")); if (!notifications.findByRecipientIdOrderByCreatedAtDesc(userId).contains(value)) throw new NotFoundException("Notificación inexistente"); value.markRead(); }
    public record NotificationResponse(Long id, String type, String message, boolean read, Instant createdAt) {}
}
