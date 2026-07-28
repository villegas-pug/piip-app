package pe.gob.midagri.piip.identity.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.gob.midagri.piip.identity.application.*;
import java.io.IOException;

@Component
public class LocalAuthorityFilter extends OncePerRequestFilter {
    private final LocalAuthorizationService authorization;
    public LocalAuthorityFilter(LocalAuthorizationService authorization) { this.authorization = authorization; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            var jwt = token.getToken();
            LocalAccessContext local = authorization.resolve(jwt.getSubject());
            var authorities = local.roles().stream().map(role -> new SimpleGrantedAuthority(role.authority())).toList();
            JwtAuthenticationToken effective = new JwtAuthenticationToken(jwt, authorities, token.getName());
            effective.setDetails(local);
            SecurityContextHolder.getContext().setAuthentication(effective);
        }
        chain.doFilter(request, response);
    }
}
