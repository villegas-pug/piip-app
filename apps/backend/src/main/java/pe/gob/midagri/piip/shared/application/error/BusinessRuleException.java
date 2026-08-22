package pe.gob.midagri.piip.shared.application.error;

/** Error funcional independiente del protocolo HTTP. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
