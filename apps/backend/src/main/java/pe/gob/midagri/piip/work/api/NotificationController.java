package pe.gob.midagri.piip.work.api;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.work.application.NotificationReadModels.NotificationView;
import pe.gob.midagri.piip.work.application.NotificationService;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService service;

    @Autowired
    public NotificationController(NotificationService service) { this.service = service; }

    /** Constructor de compatibilidad para pruebas unitarias existentes. */
    public NotificationController(NotificationRepository notifications, LocalAuthorizationService authorization) {
        this(new NotificationService(notifications, authorization));
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return service.list().stream().map(NotificationController::response).toList();
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable("id") Long id) { service.read(id); }

    private static NotificationResponse response(NotificationView value) {
        return new NotificationResponse(value.id(), value.type(), value.message(), value.read(), value.createdAt());
    }

    public record NotificationResponse(Long id, String type, String message, boolean read, Instant createdAt) {}
}
