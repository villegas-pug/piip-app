package pe.gob.midagri.piip.portfolio.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.ProblemCode;

/**
 * Validación canónica de los estados del portafolio destinados a asignaciones y transiciones.
 * Resuelve el código en el catálogo persistente y valida en orden: existencia, actividad y aplicabilidad.
 * La matriz de transiciones permanece en el dominio; el caso de uso encadena esa validación
 * con {@link ProblemCode#PORTFOLIO_STATUS_TRANSITION_NOT_ALLOWED} sobre el código ya resuelto aquí.
 */
@Service
@Transactional(readOnly = true)
public class PortfolioStatusValidationService {
    private final PortfolioStatusRepository statuses;

    public PortfolioStatusValidationService(PortfolioStatusRepository statuses) {
        this.statuses = statuses;
    }

    /** Parseo estricto del código técnico; no acepta denominaciones ni códigos ajenos al enum. */
    public PortfolioStatus parseCode(String code) {
        if (code == null) throw notFound();
        try {
            return PortfolioStatus.valueOf(code);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
    }

    /** Resuelve la metadata vigente del estado por código; causa {@link ProblemCode#PORTFOLIO_STATUS_NOT_FOUND}. */
    public PortfolioStatusCatalogEntity resolveExisting(String code) {
        return resolveExisting(parseCode(code));
    }

    public PortfolioStatusCatalogEntity resolveExisting(PortfolioStatus code) {
        return statuses.findByCode(code).orElseThrow(PortfolioStatusValidationService::notFound);
    }

    /**
     * Valida el destino de una asignación o transición en orden existencia, actividad y aplicabilidad.
     * La aplicabilidad {@link PortfolioStatusApplicability#NONE} nunca es asignable a iniciativas ni proyectos.
     */
    public PortfolioStatusCatalogEntity validateTarget(String code, RecordType recordType) {
        return validateTarget(parseCode(code), recordType);
    }

    public PortfolioStatusCatalogEntity validateTarget(PortfolioStatus code, RecordType recordType) {
        PortfolioStatusCatalogEntity status = resolveExisting(code);
        if (!status.isActive()) {
            throw new BusinessRuleException(ProblemCode.PORTFOLIO_STATUS_INACTIVE, "El estado destino está inactivo");
        }
        if (status.getApplicability() != applicabilityOf(recordType)) {
            throw new BusinessRuleException(ProblemCode.PORTFOLIO_STATUS_NOT_APPLICABLE,
                "El estado no es aplicable a este tipo de registro");
        }
        return status;
    }

    private static PortfolioStatusApplicability applicabilityOf(RecordType recordType) {
        return switch (recordType) {
            case INITIATIVE -> PortfolioStatusApplicability.INITIATIVE;
            case PROJECT -> PortfolioStatusApplicability.PROJECT;
        };
    }

    private static BusinessRuleException notFound() {
        return new BusinessRuleException(ProblemCode.PORTFOLIO_STATUS_NOT_FOUND,
            "El estado del portafolio no existe en el catálogo");
    }
}
