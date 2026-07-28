package pe.gob.midagri.piip.shared.api;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException exception) { return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", exception.getMessage()); }

    @ExceptionHandler({BusinessRuleException.class, IllegalStateException.class})
    ProblemDetail business(RuntimeException exception) { return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio", exception.getMessage()); }

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

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title); problem.setType(URI.create("https://piip.midagri.gob.pe/problems/" + status.value()));
        return problem;
    }
}
