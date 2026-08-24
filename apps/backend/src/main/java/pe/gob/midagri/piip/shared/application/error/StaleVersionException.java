package pe.gob.midagri.piip.shared.application.error;

public class StaleVersionException extends RuntimeException {
    public StaleVersionException() {
        super("El recurso fue modificado por otro usuario");
    }

    public ProblemCode getProblemCode() { return ProblemCode.STALE_VERSION; }
}
