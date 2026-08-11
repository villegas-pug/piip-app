package pe.gob.midagri.piip.identity.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByKeycloakSubject(String subject);

    @Query("select user from UserEntity user where not exists (select scope from UserRoleScopeEntity scope where scope.user = user)")
    List<UserEntity> findWithoutRoleScopeHistory();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.keycloakSubject = :subject")
    Optional<UserEntity> findByKeycloakSubjectForAuthenticationUpdate(@Param("subject") String subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.keycloakSubject = :subject")
    Optional<UserEntity> findByKeycloakSubjectForUpdate(@Param("subject") String subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);
}
