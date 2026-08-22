package pe.gob.midagri.piip.shared.api;

/** @deprecated usar el error de application; se conserva como puente temporal de compatibilidad. */
@Deprecated(forRemoval = false)
public class InvalidReferenceException extends pe.gob.midagri.piip.shared.application.error.InvalidReferenceException {
    public InvalidReferenceException(String message, String referenceField, Long referenceId, String reason) {
        super(message, referenceField, referenceId, reason);
    }
}
