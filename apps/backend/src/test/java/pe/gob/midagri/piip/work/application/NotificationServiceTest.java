package pe.gob.midagri.piip.work.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notifications;
    @Mock LocalAuthorizationService authorization;

    @Test
    void cannotReadAnotherRecipientsNotification() {
        when(authorization.requireAuthenticatedRole()).thenReturn(new LocalAccessContext(7L, "subject", java.util.Set.of()));
        when(notifications.findByIdAndRecipientId(9L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new NotificationService(notifications, authorization).read(9L))
            .isInstanceOf(NotFoundException.class);
    }
}
