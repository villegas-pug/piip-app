package pe.gob.midagri.piip.shared.application.error;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public ProblemCode getProblemCode() { return ProblemCode.RESOURCE_NOT_FOUND; }
}
