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

## Expediente documental

Cada tipo documental de una iniciativa o proyecto admite varios archivos simultáneos e independientes: cada archivo conserva su propio historial de versiones y se identifica, consulta, versiona y elimina individualmente, sin afectar a los demás archivos del mismo tipo. La carga por tipo existente crea el archivo original del tipo en su primera versión y versiona ese mismo original en las cargas siguientes; agregar archivo, nueva versión y eliminar archivo son acciones distintas y explícitas. La eliminación de un archivo es individual, autorizada y auditable, y no destruye las versiones ni los contenidos de ningún archivo. La declaración «No aplica» sigue siendo única por tipo documental del expediente, convive con los archivos ya cargados y una nueva carga la reanuda. Los indicadores del expediente (documentos cargados, pendientes y «No aplica») cuentan por tipo documental, no por archivo: un tipo cuenta como cargado si tiene al menos un archivo no eliminado.
