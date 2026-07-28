# Matriz funcional PIIP

## 23 campos canónicos

| Grupo | Campos |
|---|---|
| Identificación | Tipo de registro; Código; Código de origen; Nombre; Tipo de solución; Fuente u origen |
| Fechas y responsables | Fecha de inicio; Responsable; Objetivo PEI; Actividad POI; Unidades de organización responsables |
| Contenido | Descripción; Resultados clave; Nota |
| Estado y producto | Estado; Tipo de producto final aprobado; Componente Digital; Fecha de cierre |
| Documentos | Informe de opinión técnica; Decisión de aprobación; Aprobación de producto final; Gestión del proyecto; Informe final de cierre |

## Seis catálogos

Los valores se mantienen exactamente en los enums backend y en `PIIP_CATALOGS`: tipo de registro, tipo de solución, fuente u origen, estado, producto final aprobado y componente digital.

`Unidad Ejecutora` es contexto técnico. `Unidades de organización responsables` sigue siendo uno de los 23 campos y se normaliza sin convertirlo en catálogo Excel.
