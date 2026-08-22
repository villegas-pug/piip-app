package pe.gob.midagri.piip.catalogs.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;

class CatalogReferenceServiceTest {
    private final CatalogItemRepository items = mock(CatalogItemRepository.class);
    private final CatalogReferenceService service = new CatalogReferenceService(items);

    @Test
    void rechazaUnaReferenciaDeOtroCatalogo() {
        CatalogEntity catalog = new CatalogEntity(CatalogCode.SOURCE_ORIGIN, "Fuente", 20, true);
        CatalogItemEntity item = new CatalogItemEntity(catalog, "DEMAND", "Demanda", 10, true);
        when(items.findById(7L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.resolveActive(7L, CatalogCode.SOLUTION_TYPE, "solutionTypeId"))
            .isInstanceOf(InvalidReferenceException.class)
            .hasMessageContaining("catálogo esperado");
    }

    @Test
    void rechazaUnaReferenciaInactiva() {
        CatalogEntity catalog = new CatalogEntity(CatalogCode.SOURCE_ORIGIN, "Fuente", 20, true);
        CatalogItemEntity item = new CatalogItemEntity(catalog, "DEMAND", "Demanda", 10, false);
        when(items.findById(8L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.resolveActive(8L, CatalogCode.SOURCE_ORIGIN, "sourceId"))
            .isInstanceOf(InvalidReferenceException.class)
            .hasMessageContaining("inactiva");
    }

    @Test void resuelveCodigoTecnicoSoloSiCatalogoEItemEstanActivos() {
        CatalogEntity catalog = new CatalogEntity(CatalogCode.SOLUTION_TYPE, "Tipo", 10, true);
        CatalogItemEntity item = new CatalogItemEntity(catalog, "NOT_APPLICABLE", "No aplica", 30, true);
        when(items.findByCatalogCodeAndCatalogActiveTrueAndCodeIgnoreCaseAndActiveTrue(CatalogCode.SOLUTION_TYPE, "NOT_APPLICABLE"))
            .thenReturn(Optional.of(item));

        assertThat(service.resolveActiveByCode(CatalogCode.SOLUTION_TYPE, "NOT_APPLICABLE", "solutionTypeId")).isSameAs(item);
    }
}
