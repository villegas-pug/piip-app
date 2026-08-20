package pe.gob.midagri.piip.catalogs.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;

@DataJpaTest
@ActiveProfiles("test")
class CatalogPersistenceTest {
    @Autowired CatalogRepository catalogs;
    @Autowired CatalogItemRepository items;
    @Test
    void conservaIdentidadCodigoOrdenYEstado() {
        CatalogEntity catalog = catalogs.save(new CatalogEntity(CatalogCode.SOLUTION_TYPE, "Tipo de solución", 10, true));
        CatalogItemEntity item = items.saveAndFlush(new CatalogItemEntity(catalog, "NOT_APPLICABLE", "No aplica", 90, false));

        assertThat(catalog.getCode()).isEqualTo(CatalogCode.SOLUTION_TYPE);
        assertThat(item.getCatalog()).isSameAs(catalog);
        assertThat(item.getCode()).isEqualTo("NOT_APPLICABLE");
        assertThat(item.getDisplayOrder()).isEqualTo(90);
        assertThat(item.isActive()).isFalse();
    }

    @Test void exigeCodigoDeItemUnicoDentroDelCatalogo() {
        CatalogEntity catalog = catalogs.save(new CatalogEntity(CatalogCode.SOURCE_ORIGIN, "Fuente", 20, true));
        items.saveAndFlush(new CatalogItemEntity(catalog, "OTHER", "Otros", 10, true));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            items.saveAndFlush(new CatalogItemEntity(catalog, "OTHER", "Duplicado", 20, true)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
