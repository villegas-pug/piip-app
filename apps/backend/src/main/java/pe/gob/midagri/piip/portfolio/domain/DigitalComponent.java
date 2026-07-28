package pe.gob.midagri.piip.portfolio.domain;

public enum DigitalComponent implements LabeledCatalog {
    YES("Si"), NO("No");
    private final String label;
    DigitalComponent(String label) { this.label = label; }
    public String label() { return label; }
}
