package pe.gob.midagri.piip.audit.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.shared.api.ApiExceptionHandler;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AccessAuditFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessAuditFilter.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private final AuditService audit;
    public AccessAuditFilter(AuditService audit) { this.audit = audit; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/actuator/") || uri.contains("/swagger-ui") || uri.contains("/v3/api-docs");
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        long start = System.nanoTime();
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        response.setHeader(CORRELATION_HEADER, correlationId);
        try { chain.doFilter(request, response); }
        finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String subject = authentication == null || !authentication.isAuthenticated() ? null : authentication.getName();
            String roles = authentication == null ? "" : authentication.getAuthorities().toString();
            try {
                String safeReason = (String) request.getAttribute(ApiExceptionHandler.SAFE_REASON_ATTRIBUTE);
                if (safeReason == null && response.getStatus() >= 400) safeReason = fallbackReason(response.getStatus());
                audit.access(subject, roles, request.getMethod(), normalize(request.getRequestURI()), response.getStatus(), extractRecordCode(request.getRequestURI()), clientIp(request), correlationId, (System.nanoTime() - start) / 1_000_000, safeReason);
            } catch (RuntimeException exception) {
                LOGGER.error("No se pudo persistir AUDITORIA_ACCESO para {}", correlationId, exception);
            }
        }
    }

    private String normalize(String uri) { return uri.replaceAll("/(I|P)-\\d{3}-\\d{4}", "/{code}").replaceAll("/\\d+", "/{id}"); }
    private String extractRecordCode(String uri) { var match = java.util.regex.Pattern.compile("(?:I|P)-\\d{3}-\\d{4}", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(uri); return match.find() ? match.group().toUpperCase() : null; }
    private String clientIp(HttpServletRequest request) { String forwarded = request.getHeader("X-Forwarded-For"); return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim(); }
    private String fallbackReason(int status) {
        return switch (status) { case 400 -> "INVALID_REQUEST"; case 403 -> "FORBIDDEN_SCOPE"; case 404 -> "RESOURCE_NOT_FOUND"; case 409 -> "STALE_VERSION"; default -> "BUSINESS_RULE_VIOLATION"; };
    }
}
