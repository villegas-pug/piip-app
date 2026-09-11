package pe.gob.midagri.piip.portfolio.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatusApplicability;

@DataJpaTest
@ActiveProfiles("test")
class PortfolioStatusCatalogPersistenceTest {
    @Autowired PortfolioStatusRepository statuses;
    @Autowired EntityManager entityManager;

    /** Inventario inicial oficial: código, denominación, orden y aplicabilidad (todos activos). */
    private record EstadoEsperado(PortfolioStatus code, String name, int displayOrder, PortfolioStatusApplicability applicability) {}

    private static final List<EstadoEsperado> INVENTARIO = List.of(
        new EstadoEsperado(PortfolioStatus.PRESENTED, "Presentado", 1, PortfolioStatusApplicability.INITIATIVE),
        new EstadoEsperado(PortfolioStatus.INITIATIVE_APPROVED, "Iniciativa aprobada", 2, PortfolioStatusApplicability.INITIATIVE),
        new EstadoEsperado(PortfolioStatus.INITIATIVE_ARCHIVED, "Iniciativa archivada", 3, PortfolioStatusApplicability.INITIATIVE),
        new EstadoEsperado(PortfolioStatus.PROJECT_IN_PROGRESS, "Proyecto en ejecución", 4, PortfolioStatusApplicability.PROJECT),
        new EstadoEsperado(PortfolioStatus.PRODUCT_APPROVED, "Producto aprobado", 5, PortfolioStatusApplicability.PROJECT),
        new EstadoEsperado(PortfolioStatus.PRODUCT_NOT_APPROVED, "Producto no aprobado", 6, PortfolioStatusApplicability.PROJECT),
        new EstadoEsperado(PortfolioStatus.SUSPENDED, "Suspendido", 7, PortfolioStatusApplicability.PROJECT),
        new EstadoEsperado(PortfolioStatus.CANCELLED, "Cancelado", 8, PortfolioStatusApplicability.PROJECT),
        new EstadoEsperado(PortfolioStatus.FINISHED, "Finalizado", 9, PortfolioStatusApplicability.PROJECT),
        new EstadoEsperado(PortfolioStatus.NOT_APPLICABLE, "No Aplicable", 10, PortfolioStatusApplicability.NONE),
        new EstadoEsperado(PortfolioStatus.NOT_ADMISSIBLE, "No Admisible", 11, PortfolioStatusApplicability.INITIATIVE));

    @Test
    void persisteLosOnceCodigosConSusCincoAtributosEnOrden() {
        INVENTARIO.forEach(esperado -> statuses.save(new PortfolioStatusCatalogEntity(
            esperado.code(), esperado.name(), esperado.displayOrder(), true, esperado.applicability())));
        statuses.flush();
        entityManager.clear();

        List<PortfolioStatusCatalogEntity> persistidos = statuses.findAllByOrderByDisplayOrderAscCodeAsc();

        assertThat(persistidos).hasSize(11);
        for (int posicion = 0; posicion < INVENTARIO.size(); posicion++) {
            EstadoEsperado esperado = INVENTARIO.get(posicion);
            PortfolioStatusCatalogEntity entidad = persistidos.get(posicion);
            assertThat(entidad.getCode()).isEqualTo(esperado.code());
            assertThat(entidad.getName()).isEqualTo(esperado.name());
            assertThat(entidad.getDisplayOrder()).isEqualTo(esperado.displayOrder());
            assertThat(entidad.isActive()).isTrue();
            assertThat(entidad.getApplicability()).isEqualTo(esperado.applicability());
        }
    }

    @Test
    void resuelveElEstadoPorSuCodigoNatural() {
        statuses.saveAndFlush(new PortfolioStatusCatalogEntity(
            PortfolioStatus.SUSPENDED, "Suspendido", 7, true, PortfolioStatusApplicability.PROJECT));
        entityManager.clear();

        assertThat(statuses.findByCode(PortfolioStatus.SUSPENDED)).get().satisfies(entidad -> {
            assertThat(entidad.getCode()).isEqualTo(PortfolioStatus.SUSPENDED);
            assertThat(entidad.getName()).isEqualTo("Suspendido");
            assertThat(entidad.getDisplayOrder()).isEqualTo(7);
            assertThat(entidad.isActive()).isTrue();
            assertThat(entidad.getApplicability()).isEqualTo(PortfolioStatusApplicability.PROJECT);
        });
        assertThat(statuses.findByCode(PortfolioStatus.CANCELLED)).isEmpty();
    }

    @Test
    void rechazaUnaSegundaIdentidadParaElMismoCodigo() {
        statuses.saveAndFlush(new PortfolioStatusCatalogEntity(
            PortfolioStatus.PRESENTED, "Presentado", 1, true, PortfolioStatusApplicability.INITIATIVE));
        entityManager.clear();

        assertThatThrownBy(() -> {
            entityManager.persist(new PortfolioStatusCatalogEntity(
                PortfolioStatus.PRESENTED, "Duplicado", 2, true, PortfolioStatusApplicability.INITIATIVE));
            entityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }
}
