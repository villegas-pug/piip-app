package pe.gob.midagri.piip.config.reset;

import java.util.*;
import org.hibernate.boot.model.relational.*;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.*;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test-reset")
public class TestResetSchemaFilterProvider implements SchemaFilterProvider, HibernatePropertiesCustomizer {
    public static final List<String> DROP_ORDER = List.of("DOCUMENTO_CONTENIDO", "DOCUMENTO_VERSION", "DOCUMENTO",
        "REGISTRO_UNIDAD_RESPONSABLE", "TAREA_TRABAJO", "NOTIFICACION", "EVENTO_AUDITORIA", "AUDITORIA_ACCESO",
        "REGISTRO_PORTAFOLIO", "CONTADOR_CODIGO", "CATALOGO_ITEM", "TIPO_DOCUMENTO", "USUARIO_ROL_AMBITO",
        "UNIDAD_ORGANICA", "USUARIO", "ROL", "UNIDAD_EJECUTORA", "INSTITUCION", "CATALOGO");
    public static final List<String> CREATE_ORDER = List.of("CATALOGO", "INSTITUCION", "ROL", "USUARIO",
        "UNIDAD_EJECUTORA", "UNIDAD_ORGANICA", "USUARIO_ROL_AMBITO", "CATALOGO_ITEM", "TIPO_DOCUMENTO", "CONTADOR_CODIGO",
        "REGISTRO_PORTAFOLIO", "REGISTRO_UNIDAD_RESPONSABLE", "DOCUMENTO", "DOCUMENTO_VERSION", "DOCUMENTO_CONTENIDO",
        "TAREA_TRABAJO", "NOTIFICACION", "EVENTO_AUDITORIA", "AUDITORIA_ACCESO");
    public static final Set<String> ALLOWLIST = Set.copyOf(DROP_ORDER);

    private volatile String currentTable;
    private final SchemaFilter selected = new SchemaFilter() {
        @Override public boolean includeNamespace(Namespace namespace) { return true; }
        @Override public boolean includeTable(Table table) { return currentTable != null && currentTable.equalsIgnoreCase(table.getName()); }
        @Override public boolean includeSequence(Sequence sequence) { return false; }
    };

    @Override public void customize(Map<String, Object> properties) { properties.put("hibernate.hbm2ddl.schema_filter_provider", this); }
    public void select(String table) {
        String normalized = table == null ? null : table.toUpperCase(Locale.ROOT);
        if (!ALLOWLIST.contains(normalized)) throw new IllegalArgumentException("Tabla fuera de la allowlist test-reset");
        currentTable = normalized;
    }
    public String currentTable() { return currentTable; }
    @Override public SchemaFilter getCreateFilter() { return selected; }
    @Override public SchemaFilter getDropFilter() { return selected; }
    @Override public SchemaFilter getTruncatorFilter() { return SchemaFilter.ALL; }
    @Override public SchemaFilter getMigrateFilter() { return SchemaFilter.ALL; }
    @Override public SchemaFilter getValidateFilter() { return SchemaFilter.ALL; }
}
