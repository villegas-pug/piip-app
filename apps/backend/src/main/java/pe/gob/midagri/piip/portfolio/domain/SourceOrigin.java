package pe.gob.midagri.piip.portfolio.domain;

public enum SourceOrigin implements LabeledCatalog {
    INITIATIVE_SHEET("Ficha de iniciativa de innovación pública"),
    INTERNAL_CONTEST("Concurso interno"),
    OPEN_INNOVATION("Innovación abierta"),
    MANAGEMENT_PROPOSAL("Propuesta de jefatura o directivos"),
    OTHER("Otros"),
    CALL("Convocatoria");
    private final String label;
    SourceOrigin(String label) { this.label = label; }
    public String label() { return label; }
}
