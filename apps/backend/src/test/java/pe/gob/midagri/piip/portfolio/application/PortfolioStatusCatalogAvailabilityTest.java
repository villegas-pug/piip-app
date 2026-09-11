package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.PortfolioStatusCatalogResponse;
import pe.gob.midagri.piip.catalogs.application.CatalogQueryService;
import pe.gob.midagri.piip.catalogs.persistence.CatalogItemRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusRepository;

@DataJpaTest
@ActiveProfiles("test")
class PortfolioStatusCatalogAvailabilityTest {
    @Autowired PortfolioStatusRepository statuses;
    @Autowired CatalogItemRepository items;
    @Autowired DocumentTypeRepository documentTypes;

    private CatalogQueryService queryService() {
        return new CatalogQueryService(items, documentTypes, mock(LocalAuthorizationService.class), statuses);
    }

    private PortfolioStatusCatalogEntity seed(PortfolioStatus code, String name, int displayOrder,
            boolean active, PortfolioStatusApplicability applicability) {
        return statuses.save(new PortfolioStatusCatalogEntity(code, name, displayOrder, active, applicability));
    }

    private void seedInventarioInicial() {
        seed(PortfolioStatus.PRESENTED, "Presentado", 1, true, PortfolioStatusApplicability.INITIATIVE);
        seed(PortfolioStatus.INITIATIVE_APPROVED, "Iniciativa aprobada", 2, true, PortfolioStatusApplicability.INITIATIVE);
        seed(PortfolioStatus.INITIATIVE_ARCHIVED, "Iniciativa archivada", 3, true, PortfolioStatusApplicability.INITIATIVE);
        seed(PortfolioStatus.PROJECT_IN_PROGRESS, "Proyecto en ejecución", 4, true, PortfolioStatusApplicability.PROJECT);
        seed(PortfolioStatus.PRODUCT_APPROVED, "Producto aprobado", 5, true, PortfolioStatusApplicability.PROJECT);
        seed(PortfolioStatus.PRODUCT_NOT_APPROVED, "Producto no aprobado", 6, true, PortfolioStatusApplicability.PROJECT);
        seed(PortfolioStatus.SUSPENDED, "Suspendido", 7, true, PortfolioStatusApplicability.PROJECT);
        seed(PortfolioStatus.CANCELLED, "Cancelado", 8, true, PortfolioStatusApplicability.PROJECT);
        seed(PortfolioStatus.FINISHED, "Finalizado", 9, true, PortfolioStatusApplicability.PROJECT);
        seed(PortfolioStatus.NOT_APPLICABLE, "No Aplicable", 10, true, PortfolioStatusApplicability.NONE);
        seed(PortfolioStatus.NOT_ADMISSIBLE, "No Admisible", 11, true, PortfolioStatusApplicability.INITIATIVE);
        statuses.flush();
    }

    @Test
    void inactivoSaleDelBundleYElRenombradoConservaIdentidadPorCodigo() {
        seedInventarioInicial();

        List<PortfolioStatusCatalogResponse> bundleInicial = queryService().bundle().portfolioStatuses();
        assertThat(bundleInicial).extracting(PortfolioStatusCatalogResponse::code).containsExactly(
            "PRESENTED", "INITIATIVE_APPROVED", "INITIATIVE_ARCHIVED", "PROJECT_IN_PROGRESS",
            "PRODUCT_APPROVED", "PRODUCT_NOT_APPROVED", "SUSPENDED", "CANCELLED", "FINISHED",
            "NOT_APPLICABLE", "NOT_ADMISSIBLE");
        assertThat(bundleInicial).allSatisfy(entrada -> assertThat(entrada.active()).isTrue());
        assertThat(bundleInicial.getFirst()).satisfies(entrada -> {
            assertThat(entrada.name()).isEqualTo("Presentado");
            assertThat(entrada.displayOrder()).isEqualTo(1);
            assertThat(entrada.applicability()).isEqualTo("INITIATIVE");
        });

        PortfolioStatusCatalogEntity suspendido = statuses.findByCode(PortfolioStatus.SUSPENDED).orElseThrow();
        suspendido.rename("Suspendido (histórico)");
        suspendido.deactivate();
        statuses.saveAndFlush(suspendido);

        List<PortfolioStatusCatalogResponse> bundleTrasInactivar = queryService().bundle().portfolioStatuses();
        assertThat(bundleTrasInactivar).hasSize(10);
        assertThat(bundleTrasInactivar).extracting(PortfolioStatusCatalogResponse::code).doesNotContain("SUSPENDED");

        assertThat(statuses.findByCode(PortfolioStatus.SUSPENDED)).get().satisfies(historico -> {
            assertThat(historico.getCode()).isEqualTo(PortfolioStatus.SUSPENDED);
            assertThat(historico.getName()).isEqualTo("Suspendido (histórico)");
            assertThat(historico.isActive()).isFalse();
        });
        assertThat(statuses.findAllByOrderByDisplayOrderAscCodeAsc()).hasSize(11);
        assertThat(statuses.count()).isEqualTo(11L);
    }

    @Test
    void respetaElOrdenPersistenteYDesempataPorCodigo() {
        seed(PortfolioStatus.FINISHED, "Finalizado", 3, true, PortfolioStatusApplicability.PROJECT);
        seed(PortfolioStatus.PRESENTED, "Presentado", 1, true, PortfolioStatusApplicability.INITIATIVE);
        seed(PortfolioStatus.SUSPENDED, "Suspendido", 2, true, PortfolioStatusApplicability.PROJECT);
        seed(PortfolioStatus.CANCELLED, "Cancelado", 2, true, PortfolioStatusApplicability.PROJECT);
        statuses.flush();

        assertThat(queryService().bundle().portfolioStatuses())
            .extracting(PortfolioStatusCatalogResponse::code)
            .containsExactly("PRESENTED", "CANCELLED", "SUSPENDED", "FINISHED");
    }
}
