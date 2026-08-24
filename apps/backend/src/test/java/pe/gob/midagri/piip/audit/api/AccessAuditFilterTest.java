package pe.gob.midagri.piip.audit.api;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.shared.api.ApiExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccessAuditFilterTest {
    @Test
    void persistsTheProblemCodeAttributeAsTheSafeReason() throws Exception {
        AuditService audit = mock(AuditService.class);
        AccessAuditFilter filter = new AccessAuditFilter(audit);
        MockHttpServletRequest request = request("/admin/role-assignments/20");
        request.setAttribute(ApiExceptionHandler.SAFE_REASON_ATTRIBUTE, "SELF_ADMIN_SUSPENSION");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(422);

        filter.doFilter(request, response, mock(FilterChain.class));

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(audit).access(any(), any(), eq("GET"), eq("/admin/role-assignments/{id}"), eq(422),
            isNull(), anyString(), anyString(), anyLong(), reason.capture());
        assertThat(reason.getValue()).isEqualTo("SELF_ADMIN_SUSPENSION");
    }

    @Test
    void derivesAStatusOnlyFallbackWhenNoProblemCodeWasAttached() throws Exception {
        AuditService audit = mock(AuditService.class);
        AccessAuditFilter filter = new AccessAuditFilter(audit);
        MockHttpServletRequest request = request("/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(403);

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(audit).access(any(), any(), eq("GET"), eq("/admin/users"), eq(403), isNull(), anyString(), anyString(), anyLong(), eq("FORBIDDEN_SCOPE"));
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
