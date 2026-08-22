package pe.gob.midagri.piip.work.application;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

import static pe.gob.midagri.piip.work.application.NotificationReadModels.NotificationView;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final LocalAuthorizationService authorization;

    public NotificationService(NotificationRepository notifications, LocalAuthorizationService authorization) {
        this.notifications = notifications;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<NotificationView> list() {
        Long userId = authorization.requireAuthenticatedRole().userId();
        return notifications.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
            .map(item -> new NotificationView(item.getId(), item.getType(), item.getMessage(), item.isRead(), item.getCreatedAt()))
            .toList();
    }

    @Transactional
    public void read(Long id) {
        Long userId = authorization.requireAuthenticatedRole().userId();
        notifications.findByIdAndRecipientId(id, userId)
            .orElseThrow(() -> new NotFoundException("Notificación inexistente")).markRead();
    }
}
