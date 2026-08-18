package pe.gob.midagri.piip.work.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.shared.api.NotFoundException;
import pe.gob.midagri.piip.work.persistence.NotificationEntity;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock NotificationRepository notifications;
    @Mock LocalAuthorizationService authorization;

    @Test
    void listsOnlyTheAuthenticatedRecipientWithoutMutatingReadState() {
        UserEntity recipient = user(10L, "subject-10");
        NotificationEntity unread = notification(1L, recipient, false);
        NotificationEntity read = notification(2L, recipient, true);
        when(authorization.requireAuthenticatedRole()).thenReturn(context(10L));
        when(notifications.findByRecipientIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(unread, read));

        NotificationController controller = new NotificationController(notifications, authorization);
        var response = controller.list();

        assertThat(response).extracting(item -> item.id())
            .containsExactly(1L, 2L);
        assertThat(response).extracting(item -> item.read())
            .containsExactly(false, true);
        assertThat(unread.isRead()).isFalse();
        verify(notifications).findByRecipientIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void marksOnlyAnOwnedNotificationAndLeavesAnotherRecipientUntouched() {
        UserEntity recipient = user(10L, "subject-10");
        UserEntity otherRecipient = user(20L, "subject-20");
        NotificationEntity own = notification(1L, recipient, false);
        NotificationEntity foreign = notification(2L, otherRecipient, false);
        when(authorization.requireAuthenticatedRole()).thenReturn(context(10L));
        when(notifications.findByIdAndRecipientId(1L, 10L)).thenReturn(java.util.Optional.of(own));
        when(notifications.findByIdAndRecipientId(2L, 10L)).thenReturn(java.util.Optional.empty());

        NotificationController controller = new NotificationController(notifications, authorization);
        controller.read(1L);

        assertThat(own.isRead()).isTrue();
        assertThat(foreign.isRead()).isFalse();
        assertThatThrownBy(() -> controller.read(2L)).isInstanceOf(NotFoundException.class);
        assertThat(foreign.isRead()).isFalse();
        verify(notifications).findByIdAndRecipientId(1L, 10L);
        verify(notifications).findByIdAndRecipientId(2L, 10L);
    }

    @Test
    void doesNotListOrReadWhenThereIsNoActiveLocalAuthorization() {
        when(authorization.requireAuthenticatedRole())
            .thenThrow(new AccessDeniedException("sin asignación local activa"));
        NotificationController controller = new NotificationController(notifications, authorization);

        assertThatThrownBy(controller::list).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.read(1L)).isInstanceOf(AccessDeniedException.class);
    }

    private LocalAccessContext context(Long userId) {
        return new LocalAccessContext(userId, "subject-" + userId,
            java.util.Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 100L, 10L)));
    }

    private UserEntity user(Long id, String subject) {
        UserEntity user = new UserEntity(subject, "Persona", subject + "@example.test");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private NotificationEntity notification(Long id, UserEntity recipient, boolean read) {
        NotificationEntity notification = new NotificationEntity(recipient, null, "AVISO", "Mensaje");
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "createdAt", Instant.parse("2026-08-18T15:00:00Z"));
        if (read) notification.markRead();
        return notification;
    }
}
