package pe.gob.midagri.piip.documents.domain;

public enum DocumentType {
    PUBLIC_INNOVATION_INITIATIVE_SHEET("Ficha de Iniciativa de Innovación Pública"),
    INITIATIVE_TECHNICAL_OPINION("Informe de opinión técnica de evaluación de iniciativa"),
    FORMAL_APPROVAL_DECISION("Documento formal de decisión de aprobación"),
    FINAL_PRODUCT_APPROVAL("Documento formal de aprobación de producto final"),
    PROJECT_MANAGEMENT_DOCUMENTATION("Documentación de la gestión del proyecto"),
    FINAL_CLOSURE_REPORT("Informe final de cierre");

    private final String label;
    DocumentType(String label) { this.label = label; }
    public String label() { return label; }
}
