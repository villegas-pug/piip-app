package pe.gob.midagri.piip.shared.api;

/** @deprecated usar el error de application; se conserva como puente temporal de compatibilidad. */
@Deprecated(forRemoval = false)
public class BusinessRuleException extends pe.gob.midagri.piip.shared.application.error.BusinessRuleException {
    public BusinessRuleException(String message) { super(message); }
}
