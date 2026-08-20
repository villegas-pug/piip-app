package pe.gob.midagri.piip.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.catalogs.application.CatalogQueryService;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;

class CatalogContractTest {
    @Test
    void devuelveBundleTipadoCompletoSinOpcionesLocales() {
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        DocumentTypeRepository documentTypes = mock(DocumentTypeRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        for (CatalogCode code : CatalogCode.values()) {
            CatalogEntity catalog = new CatalogEntity(code, code.name(), code.ordinal(), true);
            when(items.findByCatalogCodeAndCatalogActiveTrueAndActiveTrueOrderByDisplayOrderAscCodeAsc(code))
                .thenReturn(List.of(new CatalogItemEntity(catalog, code.name() + "-01", code.name(), 10, true)));
        }
        when(documentTypes.findByActiveTrueOrderByDisplayOrderAscCodeAsc())
            .thenReturn(List.of(new DocumentTypeEntity("TECHNICAL_OPINION", "Informe técnico", 10, true)));

        var bundle = new CatalogQueryService(items, documentTypes, authorization).bundle();

        assertThat(bundle.recordTypes()).extracting("code").containsExactly("INITIATIVE", "PROJECT");
        assertThat(bundle.solutionTypes()).hasSize(1);
        assertThat(bundle.sources()).hasSize(1);
        assertThat(bundle.peiObjectives()).hasSize(1);
        assertThat(bundle.poiActivities()).hasSize(1);
        assertThat(bundle.documentTypes()).hasSize(1);
        assertThat(bundle.toString()).doesNotContain("Todos", "Todas", "official", "synthetic", "testData");
    }
}
