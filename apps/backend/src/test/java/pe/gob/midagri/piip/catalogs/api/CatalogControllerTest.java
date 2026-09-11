package pe.gob.midagri.piip.catalogs.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.CatalogBundleResponse;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.PortfolioStatusCatalogResponse;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogControllerTest {
    @Test
    void publicaElBundleDeCatalogosComoJson() throws NoSuchMethodException {
        GetMapping mapping = CatalogController.class.getDeclaredMethod("get").getAnnotation(GetMapping.class);

        assertThat(mapping.produces()).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void publicaPortfolioStatusesEnElContratoDelBundle() {
        List<String> componentes = Arrays.stream(CatalogBundleResponse.class.getRecordComponents())
            .map(RecordComponent::getName).toList();

        assertThat(componentes).containsExactly("recordTypes", "solutionTypes", "sources",
            "peiObjectives", "poiActivities", "documentTypes", "portfolioStatuses");
        assertThat(Arrays.stream(PortfolioStatusCatalogResponse.class.getRecordComponents())
            .map(RecordComponent::getName).toList())
            .containsExactly("code", "name", "displayOrder", "active", "applicability");
    }
}
