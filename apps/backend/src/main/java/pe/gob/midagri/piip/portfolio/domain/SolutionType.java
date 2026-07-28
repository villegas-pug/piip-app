package pe.gob.midagri.piip.portfolio.domain;

public enum SolutionType implements LabeledCatalog {
    POTENTIAL_OR_ADAPTABLE("Solución potencial o adaptable"),
    TO_BE_DEFINED("Solución por definir"),
    NOT_APPLICABLE("No aplica");
    private final String label;
    SolutionType(String label) { this.label = label; }
    public String label() { return label; }
}
