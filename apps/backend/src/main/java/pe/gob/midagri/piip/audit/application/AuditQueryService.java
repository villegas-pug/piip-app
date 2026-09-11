package pe.gob.midagri.piip.audit.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioStatusReferenceResponse;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;

import static pe.gob.midagri.piip.audit.application.AuditReadModels.*;

@Service
public class AuditQueryService {
    private final AccessAuditRepository accesses;
    private final AuditEventRepository events;
    private final PortfolioRecordRepository records;
    private final LocalAuthorizationService authorization;
    private final PortfolioStatusRepository statuses;
    private final ObjectMapper objectMapper;

    public AuditQueryService(AccessAuditRepository accesses, AuditEventRepository events,
            PortfolioRecordRepository records, LocalAuthorizationService authorization,
            PortfolioStatusRepository statuses, ObjectMapper objectMapper) {
        this.accesses = accesses;
        this.events = events;
        this.records = records;
        this.authorization = authorization;
        this.statuses = statuses;
        this.objectMapper = objectMapper;
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
            value.getDurationMs(), value.getSafeReason(), value.getOccurredAt());
    }

    private EventView toEventView(AuditEventEntity value) {
        var actor = value.getUser();
        var codes = statusCodes(value.getDetailJson());
        return new EventView(value.getEventType(), value.getEntityCode(), value.getDetailJson(), value.getActorSubject(),
            actor == null ? null : actor.getFullName(), actor == null ? null : actor.getEmail(), value.getOccurredAt(),
            reference(codes.statusCode()), reference(codes.previousStatusCode()), reference(codes.newStatusCode()));
    }

    private StatusCodes statusCodes(String detailJson) {
        if (detailJson == null || detailJson.isBlank()) return StatusCodes.NONE;
        try {
            JsonNode node = objectMapper.readTree(detailJson);
            return new StatusCodes(text(node.get("statusCode")), text(node.get("previousStatusCode")),
                text(node.get("newStatusCode")));
        } catch (Exception ignored) {
            return StatusCodes.NONE;
        }
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    /** Resuelve metadata vigente por código; los legados sin código quedan como texto histórico (D13). */
    private PortfolioStatusReferenceResponse reference(String code) {
        if (code == null || code.isBlank()) return null;
        PortfolioStatus parsed;
        try {
            parsed = PortfolioStatus.valueOf(code);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return statuses.findByCode(parsed)
            .map(catalog -> new PortfolioStatusReferenceResponse(catalog.getCode().name(), catalog.getName(), catalog.isActive()))
            .orElse(null);
    }

    private record StatusCodes(String statusCode, String previousStatusCode, String newStatusCode) {
        static final StatusCodes NONE = new StatusCodes(null, null, null);
    }
}
