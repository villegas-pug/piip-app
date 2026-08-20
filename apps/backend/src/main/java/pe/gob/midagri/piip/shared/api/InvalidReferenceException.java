package pe.gob.midagri.piip.shared.api;

public class InvalidReferenceException extends BusinessRuleException {
    private final String referenceField;
    private final Long referenceId;
    private final String reason;

    public InvalidReferenceException(String message, String referenceField, Long referenceId, String reason) {
        super(message); this.referenceField = referenceField; this.referenceId = referenceId; this.reason = reason;
    }
    public String getReferenceField() { return referenceField; }
    public Long getReferenceId() { return referenceId; }
    public String getReason() { return reason; }
}
