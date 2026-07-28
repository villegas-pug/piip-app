package pe.gob.midagri.piip.portfolio.domain;

public enum FinalProductType implements LabeledCatalog {
    CONCEPTUAL_PROTOTYPE("Prototipo de solución conceptualizada"),
    FUNCTIONAL_SOLUTION("Solución funcional"),
    NA("NA");
    private final String label;
    FinalProductType(String label) { this.label = label; }
    public String label() { return label; }
}
