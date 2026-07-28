package pe.gob.midagri.piip.portfolio.domain;

public enum RecordType implements LabeledCatalog {
    INITIATIVE("Iniciativa", "I"), PROJECT("Proyecto", "P");
    private final String label;
    private final String prefix;
    RecordType(String label, String prefix) { this.label = label; this.prefix = prefix; }
    public String label() { return label; }
    public String prefix() { return prefix; }
}
