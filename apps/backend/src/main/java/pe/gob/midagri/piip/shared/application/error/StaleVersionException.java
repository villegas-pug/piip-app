package pe.gob.midagri.piip.shared.application.error;

public class StaleVersionException extends RuntimeException {
    public StaleVersionException() {
        super("El recurso fue modificado por otro usuario");
    }
}
