package pe.gob.midagri.piip.catalogs.application;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;

@DataJpaTest
@ActiveProfiles("test")
class CatalogAvailabilityTest {
    @Autowired CatalogRepository catalogs;
    @Autowired CatalogItemRepository items;

    @Test void inactivoDesapareceDeSeleccionPeroConservaIdentidadHistoriaYRenombre() {
        CatalogEntity catalog = catalogs.save(new CatalogEntity(CatalogCode.PEI_OBJECTIVE, "Objetivo PEI", 30, true));
        CatalogItemEntity item = items.saveAndFlush(new CatalogItemEntity(catalog, "PEI-001", "Nombre inicial", 10, true));
        Long id = item.getId();
        assertThat(items.findByCatalogCodeAndCatalogActiveTrueAndActiveTrueOrderByDisplayOrderAscCodeAsc(CatalogCode.PEI_OBJECTIVE)).hasSize(1);

        item.rename("Nombre corregido");
        item.deactivate();
        items.saveAndFlush(item);

        assertThat(items.findByCatalogCodeAndCatalogActiveTrueAndActiveTrueOrderByDisplayOrderAscCodeAsc(CatalogCode.PEI_OBJECTIVE)).isEmpty();
        assertThat(items.findById(id)).get().satisfies(historical -> {
            assertThat(historical.getCode()).isEqualTo("PEI-001");
            assertThat(historical.getName()).isEqualTo("Nombre corregido");
            assertThat(historical.isActive()).isFalse();
        });
        assertThat(items.count()).isEqualTo(1L);
    }
}
