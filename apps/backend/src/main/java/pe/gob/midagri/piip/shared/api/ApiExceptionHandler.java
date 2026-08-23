package pe.gob.midagri.piip.shared.api;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException exception) { return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", exception.getMessage()); }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail business(RuntimeException exception) { return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio", exception.getMessage()); }

    @ExceptionHandler(InvalidReferenceException.class)
    ProblemDetail invalidReference(InvalidReferenceException exception) {
        ProblemDetail detail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Referencia inválida", exception.getMessage());
        detail.setProperty("referenceField", exception.getReferenceField());
        detail.setProperty("referenceId", exception.getReferenceId());
        detail.setProperty("reason", exception.getReason());
        return detail;
    }

    @ExceptionHandler({OptimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class, StaleVersionException.class})
    ProblemDetail conflict(RuntimeException exception) { return problem(HttpStatus.CONFLICT, "Conflicto de versión", "El recurso fue modificado por otro usuario"); }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException exception) { return problem(HttpStatus.FORBIDDEN, "Acceso denegado", exception.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage()).findFirst().orElse("Solicitud inválida");
        return problem(HttpStatus.BAD_REQUEST, "Validación", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail malformed(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", "El cuerpo JSON no cumple el contrato solicitado");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title); problem.setType(URI.create("https://piip.midagri.gob.pe/problems/" + status.value()));
        return problem;
    }
}
