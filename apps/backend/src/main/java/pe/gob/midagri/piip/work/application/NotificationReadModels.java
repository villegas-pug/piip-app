package pe.gob.midagri.piip.work.application;

import java.time.Instant;

public final class NotificationReadModels {
    private NotificationReadModels() {}
    public record NotificationView(Long id, String type, String message, boolean read, Instant createdAt) {}
}
