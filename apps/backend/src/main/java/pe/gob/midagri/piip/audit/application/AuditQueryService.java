package pe.gob.midagri.piip.audit.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.persistence.AccessAuditEntity;
import pe.gob.midagri.piip.audit.persistence.AccessAuditRepository;
import pe.gob.midagri.piip.audit.persistence.AuditEventEntity;
import pe.gob.midagri.piip.audit.persistence.AuditEventRepository;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;

import java.util.List;

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
    public List<AccessAuditEntity> accesses(Long executingUnitId) {
        return query(executingUnitId, accesses::findTop100ByOrderByOccurredAtDesc,
            accesses::findTop100ByRecordCodeInOrderByOccurredAtDesc);
    }

    @Transactional(readOnly = true)
    public List<AuditEventEntity> events(Long executingUnitId) {
        return query(executingUnitId, events::findTop100ByOrderByOccurredAtDesc,
            events::findTop100ByEntityCodeInOrderByOccurredAtDesc);
    }

    private <T> List<T> query(Long executingUnitId, java.util.function.Supplier<List<T>> all,
            java.util.function.Function<List<String>, List<T>> filtered) {
        authorization.require(RoleCode.ADMINISTRADOR_PIIP);
        if (executingUnitId == null) return all.get();
        authorization.requireReadableUnit(executingUnitId);
        List<String> recordCodes = records.findByExecutingUnit_Id(executingUnitId).stream()
            .map(record -> record.getCode()).toList();
        return recordCodes.isEmpty() ? List.of() : filtered.apply(recordCodes);
    }
}
