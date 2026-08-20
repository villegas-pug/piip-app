package pe.gob.midagri.piip.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.gob.midagri.piip.audit.api.AuditController;
import pe.gob.midagri.piip.documents.api.DocumentInboxController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutingUnitFilterMappingTest {
    @Test
    void documentsExposeTheOptionalExecutingUnitFilterAsJson() throws NoSuchMethodException {
        assertFilter(DocumentInboxController.class, "list");
    }

    @Test
    void auditEndpointsExposeTheOptionalExecutingUnitFilterAsJson() throws NoSuchMethodException {
        assertFilter(AuditController.class, "accesses");
        assertFilter(AuditController.class, "events");
    }

    private static void assertFilter(Class<?> controller, String methodName) throws NoSuchMethodException {
        Method method = Arrays.stream(controller.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst().orElseThrow();
        RequestParam parameter = method.getParameters()[0].getAnnotation(RequestParam.class);
        assertThat(parameter.value()).isEqualTo("executingUnitId");
        assertThat(parameter.required()).isFalse();
        assertThat(method.getAnnotation(GetMapping.class).produces())
            .containsExactly(MediaType.APPLICATION_JSON_VALUE);
    }
}
