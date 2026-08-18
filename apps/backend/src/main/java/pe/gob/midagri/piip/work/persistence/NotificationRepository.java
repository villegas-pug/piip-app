package pe.gob.midagri.piip.work.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(Long userId);
    Optional<NotificationEntity> findByIdAndRecipientId(Long id, Long userId);
}
