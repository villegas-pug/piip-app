package pe.gob.midagri.piip.audit.application;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.persistence.AccessAuditEntity;
import pe.gob.midagri.piip.audit.persistence.AccessAuditRepository;
import pe.gob.midagri.piip.audit.persistence.AuditEventEntity;
import pe.gob.midagri.piip.audit.persistence.AuditEventRepository;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;

import static pe.gob.midagri.piip.audit.application.AuditReadModels.*;

@Service
public class AuditQueryService {
    private final AccessAuditRepository accesses;
    private final AuditEventRepository events;
    private final PortfolioRecordRepository records;
    private final LocalAuthorizationService authorization;

    public AuditQueryService(AccessAuditRepository accesses, AuditEventRepository events,
            PortfolioRecordRepository records, LocalAuthorizationService authorization) {
        this.accesses = accesses;
        this.events = events;
        this.records = records;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<AccessView> accesses(Long executingUnitId) {
        return query(executingUnitId, accesses::findTop100ByOrderByOccurredAtDesc,
            accesses::findTop100ByRecordCodeInOrderByOccurredAtDesc).stream()
            .map(this::toAccessView).toList();
    }

    @Transactional(readOnly = true)
    public List<EventView> events(Long executingUnitId) {
        return query(executingUnitId, events::findTop100ByOrderByOccurredAtDesc,
            events::findTop100ByEntityCodeInOrderByOccurredAtDesc).stream()
            .map(this::toEventView).toList();
    }

    private <T> List<T> query(Long executingUnitId, Supplier<List<T>> all,
            Function<List<String>, List<T>> filtered) {
        authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        if (executingUnitId == null) return all.get();
        authorization.requireReadableUnit(executingUnitId);
        List<String> recordCodes = records.findByExecutingUnit_Id(executingUnitId).stream()
            .map(record -> record.getCode()).toList();
        return recordCodes.isEmpty() ? List.of() : filtered.apply(recordCodes);
    }

    private AccessView toAccessView(AccessAuditEntity value) {
        return new AccessView(value.getKeycloakSubject(), value.getRoleSnapshot(), value.getHttpMethod(),
            value.getNormalizedPath(), value.getResponseCode(), value.getRecordCode(), value.getCorrelationId(),
            value.getDurationMs(), value.getOccurredAt());
    }

    private EventView toEventView(AuditEventEntity value) {
        var actor = value.getUser();
        return new EventView(value.getEventType(), value.getEntityCode(), value.getDetailJson(), value.getActorSubject(),
            actor == null ? null : actor.getFullName(), actor == null ? null : actor.getEmail(), value.getOccurredAt());
    }
}
