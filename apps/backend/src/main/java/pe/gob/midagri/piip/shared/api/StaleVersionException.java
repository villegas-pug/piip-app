package pe.gob.midagri.piip.shared.api;

/** @deprecated usar el error de application; se conserva como puente temporal de compatibilidad. */
@Deprecated(forRemoval = false)
public class StaleVersionException extends pe.gob.midagri.piip.shared.application.error.StaleVersionException {
    public StaleVersionException() { super(); }
}
