package pe.gob.midagri.piip.shared.application.error;

/** Discriminadores estables del contrato de error; no dependen del texto localizado. */
public enum ProblemCode {
    INVALID_REQUEST,
    FORBIDDEN_SCOPE,
    RESOURCE_NOT_FOUND,
    STALE_VERSION,
    ACTIVE_ASSIGNMENT_DUPLICATE,
    SELF_ADMIN_SUSPENSION,
    LAST_ACTIVE_ADMIN,
    INCOMPATIBLE_ASSIGNMENT_STATE,
    INVALID_ACTIVE_REFERENCE,
    BUSINESS_RULE_VIOLATION
}
