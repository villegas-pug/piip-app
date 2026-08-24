package pe.gob.midagri.piip.shared.api;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import pe.gob.midagri.piip.shared.application.error.ProblemCode;
import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {
    public static final String SAFE_REASON_ATTRIBUTE = ApiExceptionHandler.class.getName() + ".safeReason";
    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException exception, HttpServletRequest request) { return problem(request, HttpStatus.NOT_FOUND, "Recurso no encontrado", exception.getMessage(), ProblemCode.RESOURCE_NOT_FOUND); }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail business(BusinessRuleException exception, HttpServletRequest request) { return problem(request, HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio", exception.getMessage(), exception.getProblemCode()); }
    ProblemDetail business(BusinessRuleException exception) { return problem(null, HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio", exception.getMessage(), exception.getProblemCode()); }

    @ExceptionHandler(InvalidReferenceException.class)
    ProblemDetail invalidReference(InvalidReferenceException exception, HttpServletRequest request) {
        ProblemDetail detail = problem(request, HttpStatus.UNPROCESSABLE_ENTITY, "Referencia inválida", exception.getMessage(), exception.getProblemCode());
        detail.setProperty("referenceField", exception.getReferenceField());
        detail.setProperty("referenceId", exception.getReferenceId());
        detail.setProperty("reason", exception.getReason());
        return detail;
    }
    ProblemDetail invalidReference(InvalidReferenceException exception) {
        return invalidReference(exception, null);
    }

    @ExceptionHandler({OptimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class, StaleVersionException.class})
    ProblemDetail conflict(RuntimeException exception, HttpServletRequest request) { return problem(request, HttpStatus.CONFLICT, "Conflicto de versión", "El recurso fue modificado por otro usuario", ProblemCode.STALE_VERSION); }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException exception, HttpServletRequest request) { return problem(request, HttpStatus.FORBIDDEN, "Acceso denegado", exception.getMessage(), ProblemCode.FORBIDDEN_SCOPE); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage()).findFirst().orElse("Solicitud inválida");
        return problem(request, HttpStatus.BAD_REQUEST, "Validación", detail, ProblemCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail malformed(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "Solicitud inválida", "El cuerpo JSON no cumple el contrato solicitado", ProblemCode.INVALID_REQUEST);
    }

    private ProblemDetail problem(HttpServletRequest request, HttpStatus status, String title, String detail, ProblemCode code) {
        if (request != null) request.setAttribute(SAFE_REASON_ATTRIBUTE, code.name());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title); problem.setType(URI.create("https://piip.midagri.gob.pe/problems/" + status.value()));
        problem.setProperty("problemCode", code.name());
        return problem;
    }
}
