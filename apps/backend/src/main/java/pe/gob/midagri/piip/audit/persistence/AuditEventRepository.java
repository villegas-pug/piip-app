package pe.gob.midagri.piip.audit.persistence;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
    @EntityGraph(attributePaths = "user")
    List<AuditEventEntity> findTop100ByOrderByOccurredAtDesc();
    @EntityGraph(attributePaths = "user")
    List<AuditEventEntity> findTop100ByEntityCodeInOrderByOccurredAtDesc(java.util.Collection<String> entityCodes);
    List<AuditEventEntity> findByEntityCodeOrderByOccurredAtAsc(String entityCode);

    @EntityGraph(attributePaths = "user")
    @Query("select event from AuditEventEntity event where event.executingUnitId in :authorizedExecutingUnitIds "
        + "and (:entityType is null or event.entityType = :entityType) "
        + "and (:entityId is null or event.entityId = :entityId) "
        + "and (:institutionId is null or event.institutionId = :institutionId) "
        + "and (:executingUnitId is null or event.executingUnitId = :executingUnitId) "
        + "and (:organizationalUnitId is null or event.organizationalUnitId = :organizationalUnitId) "
        + "order by event.occurredAt desc")
    List<AuditEventEntity> findForAuthorizedOrganizationScope(
            @Param("authorizedExecutingUnitIds") java.util.Collection<Long> authorizedExecutingUnitIds,
            @Param("entityType") String entityType, @Param("entityId") Long entityId,
            @Param("institutionId") Long institutionId, @Param("executingUnitId") Long executingUnitId,
            @Param("organizationalUnitId") Long organizationalUnitId, Pageable pageable);
}
