package pe.gob.midagri.piip.shared.application.error;

/** Error funcional independiente del protocolo HTTP. */
public class BusinessRuleException extends RuntimeException {
    private final ProblemCode problemCode;

    public BusinessRuleException(String message) {
        this(ProblemCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessRuleException(ProblemCode problemCode, String message) {
        super(message);
        this.problemCode = problemCode;
    }

    public ProblemCode getProblemCode() { return problemCode; }
}
