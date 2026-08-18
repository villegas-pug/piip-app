package pe.gob.midagri.piip.dashboard.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;

/** Consulta unificada del portafolio de Inicio; no cambia el modelo persistente. */
@Repository
public class DashboardPortfolioQueryRepository {
    private static final int MAX_SIZE = 100;

    @PersistenceContext
    private EntityManager entityManager;

    public QueryResult find(Long executingUnitId, String query, RecordType type, PortfolioStatus status,
            int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizeSize(size);
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        long executingUnitTotalElements = count(builder, executingUnitId, null, null, null);
        Map<PortfolioStatus, Long> statusCounts = groupedByStatus(builder, executingUnitId, query, type, status);
        long totalElements = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        int totalPages = totalPages(totalElements, normalizedSize);
        if (normalizedPage >= totalPages) {
            normalizedPage = 0;
        }

        List<PortfolioRecordEntity> content = page(builder, executingUnitId, query, type, status,
            normalizedPage, normalizedSize);
        return new QueryResult(content, normalizedPage, normalizedSize, totalElements, totalPages,
            executingUnitTotalElements, statusCounts);
    }

    private long count(CriteriaBuilder builder, Long executingUnitId, String query, RecordType type,
            PortfolioStatus status) {
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PortfolioRecordEntity> root = criteria.from(PortfolioRecordEntity.class);
        criteria.select(builder.count(root));
        criteria.where(predicates(builder, root, executingUnitId, query, type, status).toArray(Predicate[]::new));
        return entityManager.createQuery(criteria).getSingleResult();
    }

    private List<PortfolioRecordEntity> page(CriteriaBuilder builder, Long executingUnitId, String query,
            RecordType type, PortfolioStatus status, int page, int size) {
        CriteriaQuery<PortfolioRecordEntity> criteria = builder.createQuery(PortfolioRecordEntity.class);
        Root<PortfolioRecordEntity> root = criteria.from(PortfolioRecordEntity.class);
        root.fetch("executingUnit", JoinType.INNER);
        criteria.select(root).where(predicates(builder, root, executingUnitId, query, type, status)
            .toArray(Predicate[]::new));
        criteria.orderBy(builder.desc(root.get("updatedAt")), builder.desc(root.get("id")));
        TypedQuery<PortfolioRecordEntity> typed = entityManager.createQuery(criteria);
        long offset = (long) page * size;
        typed.setFirstResult((int) Math.min(offset, Integer.MAX_VALUE));
        typed.setMaxResults(size);
        return typed.getResultList();
    }

    private Map<PortfolioStatus, Long> groupedByStatus(CriteriaBuilder builder, Long executingUnitId,
            String query, RecordType type, PortfolioStatus status) {
        CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
        Root<PortfolioRecordEntity> root = criteria.from(PortfolioRecordEntity.class);
        criteria.multiselect(root.get("status").alias("status"), builder.count(root).alias("count"));
        criteria.where(predicates(builder, root, executingUnitId, query, type, status).toArray(Predicate[]::new));
        criteria.groupBy(root.get("status"));
        Map<PortfolioStatus, Long> counts = new EnumMap<>(PortfolioStatus.class);
        for (Tuple tuple : entityManager.createQuery(criteria).getResultList()) {
            PortfolioStatus groupedStatus = tuple.get("status", PortfolioStatus.class);
            Number groupedCount = (Number) tuple.get("count");
            if (groupedStatus != null && groupedCount != null && groupedCount.longValue() > 0) {
                counts.put(groupedStatus, groupedCount.longValue());
            }
        }
        return counts;
    }

    private List<Predicate> predicates(CriteriaBuilder builder, Root<PortfolioRecordEntity> root,
            Long executingUnitId, String query, RecordType type, PortfolioStatus status) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(root.get("executingUnit").get("id"), executingUnitId));
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            predicates.add(builder.or(
                builder.like(builder.lower(root.get("code")), pattern),
                builder.like(builder.lower(root.get("name")), pattern)));
        }
        if (type != null) {
            predicates.add(builder.equal(root.get("recordType"), type));
        }
        if (status != null) {
            predicates.add(builder.equal(root.get("status"), status));
        }
        return predicates;
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    private int totalPages(long totalElements, int size) {
        return totalElements == 0 ? 0 : (int) Math.min((totalElements + size - 1) / size, Integer.MAX_VALUE);
    }

    public record QueryResult(
        List<PortfolioRecordEntity> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long executingUnitTotalElements,
        Map<PortfolioStatus, Long> statusCounts) {}
}
