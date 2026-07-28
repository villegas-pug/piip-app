package pe.gob.midagri.piip.portfolio.domain;

public enum PortfolioStatus implements LabeledCatalog {
    PRESENTED("Presentado"),
    INITIATIVE_APPROVED("Iniciativa aprobada"),
    INITIATIVE_ARCHIVED("Iniciativa archivada"),
    PROJECT_IN_PROGRESS("Proyecto en ejecución"),
    PRODUCT_APPROVED("Producto aprobado"),
    PRODUCT_NOT_APPROVED("Producto no aprobado"),
    SUSPENDED("Suspendido"),
    CANCELLED("Cancelado"),
    FINISHED("Finalizado"),
    NOT_APPLICABLE("No Aplicable"),
    NOT_ADMISSIBLE("No Admisible");
    private final String label;
    PortfolioStatus(String label) { this.label = label; }
    public String label() { return label; }
}
