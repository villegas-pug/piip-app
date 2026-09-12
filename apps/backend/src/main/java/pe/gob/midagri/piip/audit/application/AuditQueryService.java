package pe.gob.midagri.piip.audit.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.audit.persistence.AccessAuditEntity;
import pe.gob.midagri.piip.audit.persistence.AccessAuditRepository;
import pe.gob.midagri.piip.audit.persistence.AuditEventEntity;
import pe.gob.midagri.piip.audit.persistence.AuditEventRepository;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
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
    private final ExecutingUnitRepository executingUnits;

    @Autowired
    public AuditQueryService(AccessAuditRepository accesses, AuditEventRepository events,
            PortfolioRecordRepository records, LocalAuthorizationService authorization,
            PortfolioStatusRepository statuses, ObjectMapper objectMapper, ExecutingUnitRepository executingUnits) {
        this.accesses = accesses;
        this.events = events;
        this.records = records;
        this.authorization = authorization;
        this.statuses = statuses;
        this.objectMapper = objectMapper;
        this.executingUnits = executingUnits;
    }

    /** Constructor de compatibilidad para consumidores de aplicación existentes. */
    public AuditQueryService(AccessAuditRepository accesses, AuditEventRepository events,
            PortfolioRecordRepository records, LocalAuthorizationService authorization,
            PortfolioStatusRepository statuses, ObjectMapper objectMapper) {
        this(accesses, events, records, authorization, statuses, objectMapper, null);
    }

    @Transactional(readOnly = true)
    public List<AccessView> accesses(Long executingUnitId) {
        return query(executingUnitId, accesses::findTop100ByOrderByOccurredAtDesc,
            accesses::findTop100ByRecordCodeInOrderByOccurredAtDesc).stream()
            .map(this::toAccessView).toList();
    }

    @Transactional(readOnly = true)
    public List<EventView> events(Long executingUnitId) {
        return events(null, null, null, executingUnitId, null);
    }

    @Transactional(readOnly = true)
    public List<EventView> events(String entityType, Long entityId, Long institutionId,
            Long executingUnitId, Long organizationalUnitId) {
        if (executingUnits == null) {
            return query(executingUnitId, events::findTop100ByOrderByOccurredAtDesc,
                events::findTop100ByEntityCodeInOrderByOccurredAtDesc).stream().map(this::toEventView).toList();
        }
        var actor = authorization.requireFresh(RoleCode.ADMINISTRADOR_PIIP);
        if (institutionId != null && !actor.institutionIds(RoleCode.ADMINISTRADOR_PIIP).contains(institutionId)) {
            throw new org.springframework.security.access.AccessDeniedException("La institución está fuera del ámbito autorizado");
        }
        var authorizedUnitIds = authorizedExecutingUnitIds(actor);
        if (authorizedUnitIds.isEmpty()) return List.of();
        if (executingUnitId != null && !authorizedUnitIds.contains(executingUnitId)) {
            throw new org.springframework.security.access.AccessDeniedException("La Unidad Ejecutora está fuera del ámbito autorizado");
        }
        List<AuditEventEntity> scopedEvents = new ArrayList<>(events.findForAuthorizedOrganizationScope(authorizedUnitIds, entityType, entityId, institutionId,
            executingUnitId, organizationalUnitId, org.springframework.data.domain.PageRequest.of(0, 100)).stream()
            .toList());
        // La consulta histórica por UE se conserva cuando no se solicitan filtros nuevos.
        // Sin UE explícita nunca se incorporan eventos legados ni se devuelve un conjunto global.
        if (executingUnitId != null && entityType == null && entityId == null && institutionId == null && organizationalUnitId == null) {
            List<String> recordCodes = records.findByExecutingUnit_Id(executingUnitId).stream()
                .map(record -> record.getCode()).toList();
            if (!recordCodes.isEmpty()) scopedEvents.addAll(events.findTop100ByEntityCodeInOrderByOccurredAtDesc(recordCodes));
            scopedEvents.sort(Comparator.comparing(AuditEventEntity::getOccurredAt).reversed());
        }
        return scopedEvents.stream().limit(100).map(this::toEventView).toList();
    }

    private java.util.Set<Long> authorizedExecutingUnitIds(pe.gob.midagri.piip.identity.application.LocalAccessContext actor) {
        var result = new java.util.LinkedHashSet<Long>();
        actor.grants().forEach(grant -> {
            if (grant.role() != RoleCode.ADMINISTRADOR_PIIP) return;
            if (grant.isInstitutionWide()) {
                if (executingUnits != null) executingUnits.findByInstitutionIdOrderByDisplayOrderAscNameAscIdAsc(grant.institutionId())
                    .forEach(unit -> result.add(unit.getId()));
            } else {
                result.add(grant.executingUnitId());
            }
        });
        return result;
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
            reference(codes.statusCode()), reference(codes.previousStatusCode()), reference(codes.newStatusCode()),
            value.getEntityType(), value.getEntityId(), value.getInstitutionId(), value.getExecutingUnitId(), value.getOrganizationalUnitId());
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
