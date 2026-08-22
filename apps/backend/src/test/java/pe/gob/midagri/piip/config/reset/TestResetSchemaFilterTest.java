package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.hibernate.mapping.Table;

class TestResetSchemaFilterTest {
    @Test
    void fijaAllowlistOrdenYTablasProtegidas() {
        assertThat(TestResetSchemaFilterProvider.DROP_ORDER).hasSize(13)
            .startsWith("DOCUMENTO_CONTENIDO", "DOCUMENTO_VERSION", "DOCUMENTO")
            .endsWith("TIPO_DOCUMENTO", "CATALOGO");
        assertThat(TestResetSchemaFilterProvider.CREATE_ORDER).hasSize(13)
            .startsWith("CATALOGO", "CATALOGO_ITEM", "TIPO_DOCUMENTO")
            .endsWith("EVENTO_AUDITORIA", "AUDITORIA_ACCESO");
        assertThat(TestResetSchemaFilterProvider.PROTECTED_TABLES)
            .containsExactlyInAnyOrder("INSTITUCION", "ROL", "UNIDAD_EJECUTORA", "UNIDAD_ORGANICA", "USUARIO", "USUARIO_ROL_AMBITO");
        assertThat(TestResetSchemaFilterProvider.ALLOWLIST).doesNotContainAnyElementsOf(TestResetSchemaFilterProvider.PROTECTED_TABLES);
    }

    @Test void filtroIncluyeUnicamenteLaTablaSeleccionada() {
        TestResetSchemaFilterProvider provider = new TestResetSchemaFilterProvider();
        provider.select("DOCUMENTO");
        assertThat(provider.getDropFilter().includeTable(new Table("test", "DOCUMENTO"))).isTrue();
        assertThat(provider.getDropFilter().includeTable(new Table("test", "USUARIO"))).isFalse();
        assertThatThrownBy(() -> provider.select("USUARIO")).isInstanceOf(IllegalArgumentException.class);
    }
}
