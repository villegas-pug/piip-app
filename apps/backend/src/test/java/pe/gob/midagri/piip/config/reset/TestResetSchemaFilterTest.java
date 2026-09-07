package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.hibernate.mapping.Table;

class TestResetSchemaFilterTest {
    @Test
    void fijaAllowlistOrdenYTablasProtegidas() {
        assertThat(TestResetSchemaFilterProvider.DROP_ORDER).hasSize(20)
            .startsWith("DOCUMENTO_CONTENIDO", "DOCUMENTO_VERSION", "ARCHIVO_DOCUMENTO", "DOCUMENTO")
            .endsWith("INSTITUCION", "CATALOGO");
        assertThat(TestResetSchemaFilterProvider.CREATE_ORDER).hasSize(20)
            .startsWith("CATALOGO", "INSTITUCION", "ROL", "USUARIO")
            .endsWith("EVENTO_AUDITORIA", "AUDITORIA_ACCESO");
        assertThat(TestResetSchemaFilterProvider.ALLOWLIST).hasSize(20)
            .contains("INSTITUCION", "ROL", "UNIDAD_EJECUTORA", "UNIDAD_ORGANICA", "USUARIO", "USUARIO_ROL_AMBITO");
    }

    @Test void filtroIncluyeUnicamenteLaTablaSeleccionada() {
        TestResetSchemaFilterProvider provider = new TestResetSchemaFilterProvider();
        provider.select("DOCUMENTO");
        assertThat(provider.getDropFilter().includeTable(new Table("test", "DOCUMENTO"))).isTrue();
        assertThat(provider.getDropFilter().includeTable(new Table("test", "USUARIO"))).isFalse();
        provider.select("USUARIO");
        assertThat(provider.currentTable()).isEqualTo("USUARIO");
        assertThatThrownBy(() -> provider.select("SECUENCIA_NO_AUTORIZADA")).isInstanceOf(IllegalArgumentException.class);
    }
}
