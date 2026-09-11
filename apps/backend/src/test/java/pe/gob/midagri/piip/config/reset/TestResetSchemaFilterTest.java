package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.hibernate.mapping.Table;

class TestResetSchemaFilterTest {
    @Test
    void fijaAllowlistOrdenYTablasProtegidas() {
        assertThat(TestResetSchemaFilterProvider.DROP_ORDER).hasSize(21)
            .startsWith("DOCUMENTO_CONTENIDO", "DOCUMENTO_VERSION", "ARCHIVO_DOCUMENTO", "DOCUMENTO")
            .endsWith("INSTITUCION", "CATALOGO");
        assertThat(TestResetSchemaFilterProvider.CREATE_ORDER).hasSize(21)
            .startsWith("CATALOGO", "INSTITUCION", "ROL", "USUARIO")
            .endsWith("EVENTO_AUDITORIA", "AUDITORIA_ACCESO");
        assertThat(TestResetSchemaFilterProvider.ALLOWLIST).hasSize(21)
            .contains("INSTITUCION", "ROL", "UNIDAD_EJECUTORA", "UNIDAD_ORGANICA", "USUARIO", "USUARIO_ROL_AMBITO",
                "ESTADO_PORTAFOLIO");
        // Integridad por FK: en drop el hijo (REGISTRO_PORTAFOLIO) se elimina antes que el padre (ESTADO_PORTAFOLIO).
        assertThat(TestResetSchemaFilterProvider.DROP_ORDER.indexOf("ESTADO_PORTAFOLIO"))
            .isGreaterThan(TestResetSchemaFilterProvider.DROP_ORDER.indexOf("REGISTRO_PORTAFOLIO"));
        // En create el padre (ESTADO_PORTAFOLIO) se crea antes que el hijo (REGISTRO_PORTAFOLIO).
        assertThat(TestResetSchemaFilterProvider.CREATE_ORDER.indexOf("ESTADO_PORTAFOLIO"))
            .isLessThan(TestResetSchemaFilterProvider.CREATE_ORDER.indexOf("REGISTRO_PORTAFOLIO"));
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
