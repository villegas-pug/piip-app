package pe.gob.midagri.piip.shared.api;

public class StaleVersionException extends RuntimeException {
    public StaleVersionException() {
        super("El recurso fue modificado por otro usuario");
    }
}
