package pe.gob.midagri.piip.portfolio.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;

/** Utilidades compartidas por los casos de uso de portfolio sin concentrar su orquestación. */
@Service
public class PortfolioApplicationSupport {
    private final LocalAuthorizationService authorization;
    private final Clock clock;

    public PortfolioApplicationSupport(LocalAuthorizationService authorization, Clock clock) {
        this.authorization = authorization;
        this.clock = clock;
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

    public PortfolioStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        return Arrays.stream(PortfolioStatus.values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.label().equalsIgnoreCase(value))
            .findFirst().orElseThrow(() -> new BusinessRuleException("Estado de filtro inválido"));
    }

    public Map<String, ?> transitionAuditDetail(PortfolioStatus previous, PortfolioStatus current,
            PortfolioRecordEntity record, String observation) {
        return Map.of(
            "estadoAnterior", previous.label(),
            "estadoNuevo", current.label(),
            "rol", RoleCode.ADMINISTRADOR_PIIP.name(),
            "unidadEjecutoraId", record.getExecutingUnit().getId(),
            "unidadEjecutora", record.getExecutingUnit().getName(),
            "observacion", observation == null ? "" : observation,
            "resultado", "EXITOSO");
    }
}
