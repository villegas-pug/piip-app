package pe.gob.midagri.piip.portfolio.domain;

/** Aplicabilidad de un estado del catálogo de portafolio; restringe selección y asignación, nunca autoriza transiciones. */
public enum PortfolioStatusApplicability {
    INITIATIVE, PROJECT, NONE
}
