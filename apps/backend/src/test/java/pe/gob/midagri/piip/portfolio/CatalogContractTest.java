package pe.gob.midagri.piip.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.PortfolioStatusCatalogResponse;
import pe.gob.midagri.piip.catalogs.application.CatalogQueryService;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;

class CatalogContractTest {
    @Test
    void devuelveBundleTipadoCompletoSinOpcionesLocales() {
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        DocumentTypeRepository documentTypes = mock(DocumentTypeRepository.class);
        PortfolioStatusRepository portfolioStatuses = mock(PortfolioStatusRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        for (CatalogCode code : CatalogCode.values()) {
            CatalogEntity catalog = new CatalogEntity(code, code.name(), code.ordinal(), true);
            when(items.findByCatalogCodeAndCatalogActiveTrueAndActiveTrueOrderByDisplayOrderAscCodeAsc(code))
                .thenReturn(List.of(new CatalogItemEntity(catalog, code.name() + "-01", code.name(), 10, true)));
        }
        when(documentTypes.findByActiveTrueOrderByDisplayOrderAscCodeAsc())
            .thenReturn(List.of(new DocumentTypeEntity("TECHNICAL_OPINION", "Informe técnico", 10, true)));
        when(portfolioStatuses.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc())
            .thenReturn(List.of(
                new PortfolioStatusCatalogEntity(PortfolioStatus.PRESENTED, "Presentado", 1, true, PortfolioStatusApplicability.INITIATIVE),
                new PortfolioStatusCatalogEntity(PortfolioStatus.SUSPENDED, "Suspendido", 7, true, PortfolioStatusApplicability.PROJECT)));

        var bundle = new CatalogQueryService(items, documentTypes, authorization, portfolioStatuses).bundle();

        assertThat(bundle.recordTypes()).extracting("code").containsExactly("INITIATIVE", "PROJECT");
        assertThat(bundle.solutionTypes()).hasSize(1);
        assertThat(bundle.sources()).hasSize(1);
        assertThat(bundle.peiObjectives()).hasSize(1);
        assertThat(bundle.poiActivities()).hasSize(1);
        assertThat(bundle.documentTypes()).hasSize(1);
        assertThat(bundle.portfolioStatuses()).hasSize(2);
        assertThat(bundle.portfolioStatuses()).extracting(PortfolioStatusCatalogResponse::code)
            .containsExactly("PRESENTED", "SUSPENDED");
        assertThat(bundle.portfolioStatuses().getFirst()).satisfies(entrada -> {
            assertThat(entrada.name()).isEqualTo("Presentado");
            assertThat(entrada.displayOrder()).isEqualTo(1);
            assertThat(entrada.active()).isTrue();
            assertThat(entrada.applicability()).isEqualTo("INITIATIVE");
        });
        assertThat(bundle.toString()).doesNotContain("Todos", "Todas", "official", "synthetic", "testData");
    }
}
