package pe.gob.midagri.piip.catalogs.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogControllerTest {
    @Test
    void publicaElBundleDeCatalogosComoJson() throws NoSuchMethodException {
        GetMapping mapping = CatalogController.class.getDeclaredMethod("get").getAnnotation(GetMapping.class);

        assertThat(mapping.produces()).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    }
}
