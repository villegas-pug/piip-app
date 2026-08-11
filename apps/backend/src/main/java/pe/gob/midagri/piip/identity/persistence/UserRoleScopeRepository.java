package pe.gob.midagri.piip.identity.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import java.time.Instant;
import java.util.*;

public interface UserRoleScopeRepository extends JpaRepository<UserRoleScopeEntity, Long> {
    @EntityGraph(attributePaths = {"role", "institution", "executingUnit", "user"})
    @Query("select scope from UserRoleScopeEntity scope where scope.user.keycloakSubject = :subject and scope.role.active = true and scope.active = true and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now)")
    List<UserRoleScopeEntity> findActiveBySubject(@Param("subject") String subject, @Param("now") Instant now);

    @Query("select count(scope) from UserRoleScopeEntity scope where scope.role.code = :role and scope.role.active = true and scope.active = true and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now) and scope.institution.id = :institutionId and (scope.executingUnit is null or scope.executingUnit.id = :unitId)")
    long countActiveAdministrators(@Param("role") RoleCode role, @Param("institutionId") Long institutionId, @Param("unitId") Long unitId, @Param("now") Instant now);

    @Query("select count(scope) from UserRoleScopeEntity scope where scope.role.code = :role and scope.role.active = true and scope.active = true and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now)")
    long countActiveByRole(@Param("role") RoleCode role, @Param("now") Instant now);

    @Query("select count(scope) > 0 from UserRoleScopeEntity scope where scope.user.id = :userId and scope.role.id = :roleId and scope.institution.id = :institutionId and ((:unitId is null and scope.executingUnit is null) or scope.executingUnit.id = :unitId) and scope.active = true and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now)")
    boolean existsActiveAssignment(@Param("userId") Long userId, @Param("roleId") Long roleId, @Param("institutionId") Long institutionId, @Param("unitId") Long unitId, @Param("now") Instant now);

    @EntityGraph(attributePaths = {"user", "role", "institution", "executingUnit"})
    @Query("select scope from UserRoleScopeEntity scope where scope.role.code = :role and scope.role.active = true and scope.active = true and scope.institution.id = :institutionId and (scope.executingUnit is null or scope.executingUnit.id = :unitId) and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now)")
    List<UserRoleScopeEntity> findActiveRecipients(@Param("role") RoleCode role, @Param("institutionId") Long institutionId, @Param("unitId") Long unitId, @Param("now") Instant now);

    @EntityGraph(attributePaths = {"user", "role", "institution", "executingUnit"})
    @Query("select scope from UserRoleScopeEntity scope where scope.institution.id in :institutionIds")
    List<UserRoleScopeEntity> findForAdministration(@Param("institutionIds") Collection<Long> institutionIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "role", "institution", "executingUnit"})
    @Query("select scope from UserRoleScopeEntity scope where scope.id = :scopeId")
    Optional<UserRoleScopeEntity> findByIdForUpdate(@Param("scopeId") Long scopeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select scope from UserRoleScopeEntity scope where scope.user.id = :userId and scope.role.id = :roleId and scope.institution.id = :institutionId and ((:unitId is null and scope.executingUnit is null) or scope.executingUnit.id = :unitId) and scope.active = true and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now)")
    List<UserRoleScopeEntity> findActiveDuplicatesForUpdate(@Param("userId") Long userId, @Param("roleId") Long roleId,
            @Param("institutionId") Long institutionId, @Param("unitId") Long unitId, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"role", "institution", "executingUnit"})
    @Query("select scope from UserRoleScopeEntity scope where scope.user.id = :userId and scope.active = true and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now)")
    List<UserRoleScopeEntity> findCurrentScopesByUserForUpdate(@Param("userId") Long userId, @Param("now") Instant now);

    @EntityGraph(attributePaths = {"role", "institution", "executingUnit"})
    @Query("select scope from UserRoleScopeEntity scope where scope.user.id = :userId and scope.active = true and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now)")
    List<UserRoleScopeEntity> findCurrentScopesByUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "role", "institution", "executingUnit"})
    @Query("select scope from UserRoleScopeEntity scope where scope.role.code = :role and scope.role.active = true and scope.active = true and scope.institution.id = :institutionId and ((:unitId is null and scope.executingUnit is null) or scope.executingUnit is null or scope.executingUnit.id = :unitId) and scope.validFrom <= :now and (scope.validUntil is null or scope.validUntil > :now) order by scope.id")
    List<UserRoleScopeEntity> findActiveAdministratorsForUpdate(@Param("role") RoleCode role, @Param("institutionId") Long institutionId,
            @Param("unitId") Long unitId, @Param("now") Instant now);
}
