package pe.gob.midagri.piip.portfolio.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;

/** Utilidades compartidas por los casos de uso de portfolio sin concentrar su orquestación. */
@Service
public class PortfolioApplicationSupport {
    private final LocalAuthorizationService authorization;
    private final Clock clock;
    private final PortfolioStatusValidationService statuses;

    @Autowired
    public PortfolioApplicationSupport(LocalAuthorizationService authorization, Clock clock,
            PortfolioStatusValidationService statuses) {
        this.authorization = authorization;
        this.clock = clock;
        this.statuses = statuses;
    }

    /** Constructor de compatibilidad para pruebas de solo lectura; no resuelve metadata del catálogo. */
    public PortfolioApplicationSupport(LocalAuthorizationService authorization, Clock clock) {
        this(authorization, clock, new PortfolioStatusValidationService(null));
    }

    public LocalAuthorizationService authorization() {
        return authorization;
    }

    public Clock clock() {
        return clock;
    }

    public PortfolioRecordEntity readAllowed(PortfolioRecordRepository records, String code) {
        PortfolioRecordEntity record = records.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new NotFoundException("Registro inexistente"));
        authorization.requireReadableUnit(record.getExecutingUnit().getId());
        return record;
    }

    public Specification<PortfolioRecordEntity> scopeSpecification(LocalAccessContext access) {
        return (root, ignored, builder) -> {
            var unit = root.get("executingUnit");
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (!access.executingUnitIds().isEmpty()) predicates.add(unit.get("id").in(access.executingUnitIds()));
            if (!access.institutionWideIds().isEmpty()) predicates.add(unit.get("institution").get("id").in(access.institutionWideIds()));
            return predicates.isEmpty() ? builder.disjunction()
                : builder.or(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    /** Filtro de lectura exclusivamente por código; un código del catálogo inactivo sigue consultable (D6). */
    public PortfolioStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        return statuses.parseCode(value);
    }

    /** Valida el destino de una transición en orden existencia → actividad → aplicabilidad; la matriz la aplica el caso de uso. */
    public PortfolioStatus validateTransitionTarget(String targetStatus, RecordType recordType) {
        return statuses.validateTarget(targetStatus, recordType).getCode();
    }

    /** Valida un estado inicial de creación o aprobación (existencia → actividad → aplicabilidad). */
    public void requireAssignableTarget(PortfolioStatus code, RecordType recordType) {
        statuses.validateTarget(code, recordType);
    }

    public Map<String, ?> transitionAuditDetail(PortfolioStatus previous, PortfolioStatus current,
            PortfolioRecordEntity record, String observation) {
        return Map.of(
            "estadoAnterior", previous.label(),
            "estadoNuevo", current.label(),
            "previousStatusCode", previous.name(),
            "newStatusCode", current.name(),
            "rol", RoleCode.ADMINISTRADOR_PIIP.name(),
            "unidadEjecutoraId", record.getExecutingUnit().getId(),
            "unidadEjecutora", record.getExecutingUnit().getName(),
            "observacion", observation == null ? "" : observation,
            "resultado", "EXITOSO");
    }
}
