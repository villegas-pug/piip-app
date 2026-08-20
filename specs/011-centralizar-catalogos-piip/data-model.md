# Modelo de datos: Catálogos centralizados PIIP

## Principios

- Las entidades JPA son la definición estructural canónica para Oracle.
- Los identificadores persistentes se usan en escrituras; los códigos estables se usan para carga, diagnóstico e integración.
- Los ítems inactivos no se eliminan durante la operación normal y siguen resolviendo registros históricos.
- `Objetivo PEI` y `Actividad POI` son catálogos independientes, sin relación ni validación cruzada.
- El reinicio destructivo pertenece exclusivamente al ambiente de pruebas; no es una migración productiva.

## Relaciones

```text
CATALOGO 1 ─── * CATALOGO_ITEM
                       ├── * REGISTRO_PORTAFOLIO.ID_TIPO_SOLUCION
                       ├── * REGISTRO_PORTAFOLIO.ID_FUENTE_ORIGEN
                       ├── * REGISTRO_PORTAFOLIO.ID_OBJETIVO_PEI
                       └── * REGISTRO_PORTAFOLIO.ID_ACTIVIDAD_POI

TIPO_DOCUMENTO 1 ─── * DOCUMENTO * ─── 1 REGISTRO_PORTAFOLIO
                                             │
                                             ├── * REGISTRO_UNIDAD_RESPONSABLE * ─── 1 UNIDAD_ORGANICA
                                             └── 0..1 REGISTRO_PORTAFOLIO (origen derivado)
```

## Entidades nuevas

### `CatalogEntity` → `CATALOGO`

| Atributo | Columna | Tipo | Restricción |
|----------|---------|------|-------------|
| `id` | `ID_CATALOGO` | `Long` | PK, identidad Oracle. |
| `code` | `CODIGO` | `String(40)` | No nulo, único, estable. |
| `name` | `NOMBRE` | `String(180)` | No nulo. |
| `displayOrder` | `ORDEN_PRESENTACION` | `int` | No nulo, mayor o igual a 0. |
| `active` | `ACTIVO` | `boolean` | No nulo. |

Cabeceras permitidas en esta feature:

1. `SOLUTION_TYPE` — Tipo de solución.
2. `SOURCE_ORIGIN` — Fuente u origen.
3. `PEI_OBJECTIVE` — Objetivo PEI.
4. `POI_ACTIVITY` — Actividad POI.

No se crea administración HTTP; cualquier cambio de datos iniciales se versiona en el seed autorizado.

### `CatalogItemEntity` → `CATALOGO_ITEM`

| Atributo | Columna | Tipo | Restricción |
|----------|---------|------|-------------|
| `id` | `ID_CATALOGO_ITEM` | `Long` | PK, identidad Oracle. |
| `catalog` | `ID_CATALOGO` | `CatalogEntity` | FK no nula. |
| `code` | `CODIGO` | `String(60)` | No nulo; único dentro de la cabecera. |
| `name` | `NOMBRE` | `String(500)` | No nulo. |
| `displayOrder` | `ORDEN_PRESENTACION` | `int` | No nulo, mayor o igual a 0. |
| `active` | `ACTIVO` | `boolean` | No nulo. |

Restricciones e índices:

- `UK_CATALOGO_ITEM_CATALOGO_CODIGO (ID_CATALOGO, CODIGO)`.
- Índice de lectura `IDX_CATALOGO_ITEM_ACTIVO_ORDEN (ID_CATALOGO, ACTIVO, ORDEN_PRESENTACION)`.
- Orden de respuesta: `ORDEN_PRESENTACION`, luego `CODIGO` como desempate determinista.

Validaciones de uso:

- La referencia debe existir.
- Debe pertenecer a la cabecera esperada por el campo.
- Debe estar activa para una nueva escritura.
- Una referencia inactiva sigue siendo válida para lectura histórica.

### `DocumentTypeEntity` → `TIPO_DOCUMENTO`

| Atributo | Columna | Tipo | Restricción |
|----------|---------|------|-------------|
| `id` | `ID_TIPO_DOCUMENTO` | `Long` | PK, identidad Oracle. |
| `code` | `CODIGO` | `String(60)` | No nulo, único y estable. |
| `name` | `NOMBRE` | `String(180)` | No nulo. |
| `displayOrder` | `ORDEN_PRESENTACION` | `int` | No nulo, mayor o igual a 0. |
| `active` | `ACTIVO` | `boolean` | No nulo. |

Contiene exactamente los seis códigos vigentes definidos en `spec.md`. No incorpora `APLICA_A`; todos aplican a iniciativa y proyecto.

## Entidades adaptadas

### `PortfolioRecordEntity` → `REGISTRO_PORTAFOLIO`

Reemplazos estructurales en el ambiente de pruebas:

| Anterior | Nuevo | Nulabilidad | Regla |
|----------|-------|-------------|-------|
| `TIPO_SOLUCION` texto/enum | `ID_TIPO_SOLUCION` FK a `CATALOGO_ITEM` | No nulo | El ítem debe pertenecer a `SOLUTION_TYPE`. Un proyecto preexistente resuelve por código estable `NOT_APPLICABLE`. |
| `FUENTE_ORIGEN` texto/enum | `ID_FUENTE_ORIGEN` FK a `CATALOGO_ITEM` | No nulo | Debe pertenecer a `SOURCE_ORIGIN`. |
| `OBJETIVO_PEI` texto | `ID_OBJETIVO_PEI` FK a `CATALOGO_ITEM` | Nulo permitido | Debe pertenecer a `PEI_OBJECTIVE`; no condiciona POI. |
| `ACTIVIDAD_POI` texto | `ID_ACTIVIDAD_POI` FK a `CATALOGO_ITEM` | Nulo permitido | Debe pertenecer a `POI_ACTIVITY`; no condiciona PEI. |

Índices nuevos sugeridos para filtros:

- `IDX_REGISTRO_TIPO_SOLUCION (ID_TIPO_SOLUCION)`.
- `IDX_REGISTRO_FUENTE_ORIGEN (ID_FUENTE_ORIGEN)`.
- `IDX_REGISTRO_OBJETIVO_PEI (ID_OBJETIVO_PEI)`.
- `IDX_REGISTRO_ACTIVIDAD_POI (ID_ACTIVIDAD_POI)`.

Se conservan tipo de registro, origen derivado, Unidad Ejecutora, estados, componente digital, producto final, fechas, textos narrativos, auditoría y versión optimista. No se añaden mutaciones generales.

### `DocumentEntity` → `DOCUMENTO`

- Reemplazar `TIPO_DOCUMENTO` textual por `ID_TIPO_DOCUMENTO`, FK no nula a `TIPO_DOCUMENTO`.
- Reemplazar `UK_DOCUMENTO_REGISTRO_TIPO (ID_REGISTRO, TIPO_DOCUMENTO)` por una unicidad equivalente sobre `(ID_REGISTRO, ID_TIPO_DOCUMENTO)`.
- Conservar sin cambios `ESTADO`, `MOTIVO_NO_APLICA`, `ULTIMA_VERSION`, `VERSION` y las relaciones con versiones/contenido.
- Crear las seis posiciones mediante los seis registros activos del catálogo, no mediante `DocumentType.values()`.
- Una posición ya existente puede continuar su ciclo aunque el tipo quede inactivo: cargar versión, descargar, publicar y marcar `No aplica` no crean una referencia nueva. El tipo inactivo no se usa para crear una posición adicional.

### `ResponsibleUnitEntity` → `REGISTRO_UNIDAD_RESPONSABLE`

- `ID_UNIDAD_ORGANICA` pasa a ser obligatorio para las operaciones de creación adaptadas y la asociación JPA se declara no nula tras el reset de pruebas.
- `DENOMINACION_ORIGINAL` se conserva como snapshot del nombre presentado al crear la responsabilidad; el cliente no lo usa como identidad.
- La validación comprueba que la Unidad Orgánica existe, está activa y pertenece a la Unidad Ejecutora del registro.
- No se crea `PROYECTO_UNIDAD_ORGANICA` ni otra tabla paralela.

### `OrganizationalUnitEntity` → `UNIDAD_ORGANICA`

- Se reutilizan `ID_UNIDAD_EJECUTORA`, `CODIGO`, `NOMBRE`, `SIGLA`, `ACTIVO` y jerarquía padre.
- No se agrega `ORDEN_PRESENTACION`; la consulta conserva su orden determinista por nombre y el contrato no exige ese campo para Unidad Orgánica.
- La consulta de selección devuelve únicamente unidades activas de la Unidad Ejecutora autorizada y conserva `id`, `code`, `name`, `acronym`, `parentId`, `executingUnitId` y `active`.
- Las inactivas siguen resolviéndose a través de las lecturas del registro, no del endpoint de opciones activas.

## Catálogo técnico no persistente

### Tipo de registro

`RecordType` conserva `INITIATIVE` y `PROJECT`. El backend los proyecta a:

| Campo | Regla |
|-------|-------|
| `code` | Nombre técnico estable del enum. |
| `name` | Etiqueta funcional vigente. |
| `displayOrder` | Orden explícito, no inferido del ordinal como identidad. |
| `active` | `true` para los dos valores vigentes. |

No tiene `id` de base de datos. `Todos` permanece como opción local de filtros.

## Objetos de contrato

### `PersistentCatalogItemResponse`

`id`, `code`, `name`, `displayOrder`, `active`.

### `TechnicalCatalogItemResponse`

`code`, `name`, `displayOrder`, `active`.

### `OrganizationalUnitOptionResponse`

`id`, `code`, `name`, `acronym`, `parentId`, `executingUnitId`, `active`. No contiene `displayOrder`.

### `CatalogBundleResponse`

Agrupa `recordTypes`, `solutionTypes`, `sources`, `peiObjectives`, `poiActivities` y `documentTypes`. El endpoint de opciones entrega únicamente activos; las referencias embebidas en resultados pueden tener `active=false`.

### Solicitudes de portafolio

- `InitiativeCreateRequest`: `solutionTypeId`, `sourceId`, `peiObjectiveId?`, `poiActivityId?`, `responsibleUnits[].organizationalUnitId`.
- `DerivedProjectRequest`: mismos IDs aplicables; una referencia heredada inactiva no satisface la nueva solicitud.
- `PreexistingProjectRequest`: `sourceId`, PEI/POI opcionales y Unidades Orgánicas. El servicio resuelve `NOT_APPLICABLE` por código bajo `SOLUTION_TYPE`.

### Respuesta de portafolio

Sustituye cadenas de solución, fuente, PEI y POI por `PersistentCatalogItemResponse`; conserva el resto del contrato funcional y expone responsables con identidad y denominación resoluble cuando sea necesario.

## Datos iniciales

El archivo `apps/backend/src/main/resources/db/test/catalog-data.sql`:

- contiene solo DML Oracle;
- usa `MERGE` y subconsultas por códigos estables;
- no codifica IDs numéricos;
- carga las cuatro cabeceras, sus ítems, los seis tipos documentales y, si corresponde, unidades sintéticas de prueba;
- no elimina ni renombra datos protegidos;
- puede ejecutarse dos veces sin duplicados.

Los valores PEI/POI son sintéticos de prueba hasta resolver los pendientes productivos de `spec.md`.
Su condición se documenta en el seed y la guía técnica; no existe campo de oficialidad ni marca visible en el contrato.

## Transiciones y disponibilidad

Los catálogos no agregan un ciclo funcional administrable. Solo existe esta regla de disponibilidad:

```text
ACTIVO ──desactivación de datos autorizada fuera de esta feature──> INACTIVO
```

- `ACTIVO`: visible en opciones/filtros y aceptable para nuevas escrituras.
- `INACTIVO`: oculto en opciones/filtros, rechazado en nuevas escrituras y visible en registros históricos.
- No existe eliminación física ordinaria ni endpoint para cambiar este estado en la feature.

## Integridad preservada durante el reset

- Reiniciadas: las trece tablas listadas en la matriz de `plan.md`, incluidas `EVENTO_AUDITORIA`, `AUDITORIA_ACCESO` y `NOTIFICACION`; las tres se recrean vacías y no conservan filas del ambiente anterior.
- Protegidas: `USUARIO`, `ROL`, `USUARIO_ROL_AMBITO`, `INSTITUCION`, `UNIDAD_EJECUTORA`, `UNIDAD_ORGANICA` y maestros no incluidos en la matriz.
- La recreación se deriva del mismo `Metadata` JPA y usa una allowlist de tablas; ninguna sentencia DDL vive en el seed.
