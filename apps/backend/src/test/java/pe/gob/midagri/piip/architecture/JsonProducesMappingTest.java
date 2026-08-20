package pe.gob.midagri.piip.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import pe.gob.midagri.piip.documents.api.DocumentController;
import pe.gob.midagri.piip.portfolio.api.PortfolioController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonProducesMappingTest {
    @Test
    void portfolioJsonEndpointsDeclareTheirResponseMediaType() {
        assertJsonProduces(PortfolioController.class, List.of(
            "initiatives", "createInitiative", "initiative", "approve", "transitionInitiative",
            "projects", "eligible", "derived", "preexisting", "project", "transitionProject"));
    }

    @Test
    void documentJsonEndpointsDeclareTheirResponseMediaType() {
        assertJsonProduces(DocumentController.class, List.of("list", "upload", "publication"));
    }

    private static void assertJsonProduces(Class<?> controller, List<String> methodNames) {
        for (String methodName : methodNames) {
            Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
            String[] produces = mappingProduces(method);
            assertThat(produces).as("%s.%s", controller.getSimpleName(), methodName)
                .containsExactly(MediaType.APPLICATION_JSON_VALUE);
        }
    }

    private static String[] mappingProduces(Method method) {
        GetMapping get = method.getAnnotation(GetMapping.class);
        if (get != null) {
            return get.produces();
        }
        PostMapping post = method.getAnnotation(PostMapping.class);
        if (post != null) {
            return post.produces();
        }
        PutMapping put = method.getAnnotation(PutMapping.class);
        if (put != null) {
            return put.produces();
        }
        throw new AssertionError("Método sin mapping HTTP: " + method);
    }
}
