# Data model: Múltiples archivos independientes por tipo documental

**Feature**: `016-multiples-archivos-documentales` | **Fecha**: 2026-09-06

Modelo canónico: entidades JPA (`apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/`); el DDL Oracle deriva de Hibernate (`database/generated/piip-oracle.sql`).

## Diagrama de relaciones

```text
REGISTRO_PORTAFOLIO ||--o| DOCUMENTO            (una posición por expediente y tipo; UK registro+tipo)
DOCUMENTO           ||--o{ ARCHIVO_DOCUMENTO    (N archivos independientes por tipo)
ARCHIVO_DOCUMENTO   ||--o{ DOCUMENTO_VERSION    (historial propio por archivo; UK archivo+número)
DOCUMENTO_VERSION   ||--|| DOCUMENTO_CONTENIDO  (BLOB diferido, sin cambios)
TIPO_DOCUMENTO      ||--o| DOCUMENTO            (catálogo, sin cambios)
```

## Entidad: Posición documental (`DOCUMENTO`, `DocumentEntity`) — conservada

La tabla existente consolida su rol de presentación del tipo documental en el expediente.

| Campo | Tipo | Regla |
|-------|------|-------|
| ID | PK | Sin cambios |
| ID_REGISTRO | FK → `REGISTRO_PORTAFOLIO` | Sin cambios |
| ID_TIPO_DOCUMENTO | FK → `TIPO_DOCUMENTO` | Sin cambios; se rechazan tipos inexistentes o inactivos como referencia nueva |
| ESTADO | `PENDING`/`LOADED`/`NOT_APPLICABLE` | Sin cambios de dominio; reglas de transición abajo |
| MOTIVO_NO_APLICA | varchar(500) | Sin cambios (declaración por tipo, única, unidireccional) |
| VERSION | @Version optimista | Sin cambios |
| ~~ULTIMA_VERSION~~ | — | **Se retira**: la versión vigente pasa a ser un concepto del archivo; la respuesta de compatibilidad expone la del archivo original activo |

UK `UK_DOCUMENTO_REGISTRO_TIPO (ID_REGISTRO, ID_TIPO_DOCUMENTO)`: se conserva (una presentación por tipo).

## Entidad: Archivo independiente (`ARCHIVO_DOCUMENTO`, `DocumentFileEntity`) — nueva

Unidad individual de un tipo documental dentro del expediente; agrupa su propio historial.

| Campo | Tipo | Regla |
|-------|------|-------|
| ID | PK | Nuevo |
| ID_DOCUMENTO | FK → `DOCUMENTO`, NOT NULL | El archivo pertenece a una posición (y solo a una) |
| ES_ORIGINAL | boolean, NOT NULL | Marca el archivo receptor de la carga por tipo (compatibilidad FR-024/FR-025); a lo más un original activo por posición |
| ULTIMA_VERSION | int, NOT NULL | Número de versión vigente del archivo (mayor número; hereda el rol que tenía la posición) |
| ELIMINADO | boolean, NOT NULL (false) | Inhabilitación lógica (FR-013): oculto para todos los roles, sin restauración |
| FECHA_ELIMINACION | timestamp, nullable | Solo presente en archivos eliminados |
| ELIMINADO_POR | varchar, nullable | Subject del actor que eliminó (consistente con el estilo de `CARGADO_POR`) |
| VERSION | @Version optimista | Concurrencia de eliminación/publicación sobre el mismo archivo |

Sin UK propia (N archivos por posición). El original surge de la migración o de una carga por la operación de tipo sin original activo; "agregar archivo" nunca crea originales (research D3).

## Entidad: Versión (`DOCUMENTO_VERSION`, `DocumentVersionEntity`) — adaptada

| Campo | Tipo | Regla |
|-------|------|-------|
| ID | PK | Sin cambios |
| ID_DOCUMENTO | FK → `DOCUMENTO`, NOT NULL | **Conservada** (evita reasignación masiva; consistencia con la posición del archivo) |
| ID_ARCHIVO | FK → `ARCHIVO_DOCUMENTO`, nullable en mapeo | **Nueva**; invariant de servicio: toda versión escrita por el código tiene archivo asignado; nula solo en el estado previo a migración |
| NUMERO_VERSION | int | Numeración 1..N por archivo (FR-007) |
| NOMBRE_ARCHIVO, TIPO_MIME, TAMANIO_BYTES, CHECKSUM_SHA256 | — | Sin cambios (validaciones vigentes: MIME pdf/docx/xlsx, tamaño máximo configurado, no vacío, checksum) |
| CARGADO_POR / FECHA_CARGA | — | Sin cambios |
| PUBLICADO_EXTERNO / PUBLICADO_POR / FECHA_PUBLICACION | — | Sin cambios (publicación por versión) |
| VERSION_OPTIMISTA | @Version | Sin cambios |

UK: `UK_DOC_VERSION` pasa de `(ID_DOCUMENTO, NUMERO_VERSION)` a **`(ID_ARCHIVO, NUMERO_VERSION)`**. Con `ID_ARCHIVO` nula (datos previos a migrar) la UK no exige unicidad, lo que admite el estado previo; tras la migración todas las versiones tienen archivo y la UK opera plenamente.

## Entidad: Contenido (`DOCUMENTO_CONTENIDO`, `DocumentContentEntity`) — sin cambios

BLOB 1:1 por versión, carga diferida. La migración no toca los contenidos.

## Invariantes de servicio

- Toda versión persistida por el código nuevo tiene `ID_ARCHIVO` asignada y su FK de posición coincide con la posición de su archivo.
- Todo archivo tiene al menos una versión (se crea junto con su versión 1); no existen archivos vacíos (A3).
- A lo más un original activo por posición; si el original se elimina, la siguiente carga por tipo crea un archivo nuevo y lo marca original (edge case de la spec).
- Archivo eliminado: invisible y no descargable para todos los roles en todas las consultas del expediente; sus contenidos y eventos de auditoría no se destruyen (FR-013, FR-015).
- Dos archivos del mismo tipo pueden compartir nombre de archivo: la identidad es la del archivo, no el nombre (edge case).

## Transiciones de estado de la posición (`DocumentState`)

Semántica vigente conservada (última acción gana):

- `PENDING → LOADED`: primera carga del tipo por cualquier vía (agregar archivo; carga por tipo que crea el primer archivo).
- `NOT_APPLICABLE → LOADED`: carga sobre un tipo con declaración vigente (reanudación; el tratamiento del motivo es el vigente; el motivo histórico queda trazado en auditoría).
- `PENDING | LOADED → NOT_APPLICABLE`: marcar "No aplica" (convive con los archivos existentes del tipo).
- `LOADED → PENDING`: se elimina el único archivo activo del tipo y no hay declaración vigente (A6).
- `LOADED → NOT_APPLICABLE` (permanece): se elimina el único archivo activo y hay declaración vigente.
- No existe operación de des-marca de "No aplica" (unidireccional vigente).

## Ciclo de vida del archivo

- `ACTIVO → ELIMINADO` (unidireccional, sin restauración en esta versión): fija `ELIMINADO`, `FECHA_ELIMINACION`, `ELIMINADO_POR`; audita `DOCUMENTO_ARCHIVO_ELIMINADO`; recalcula el estado de la posición conforme a las transiciones anteriores.

## Migración de datos (FR-020..FR-023)

Operación de aplicación transaccional JPA, idempotente, ejecutada como paso de despliegue tras aplicar el DDL derivado regenerado (research D4):

1. Por cada posición con al menos una versión: crear `ARCHIVO_DOCUMENTO` con `ES_ORIGINAL = verdadero`, `ELIMINADO = falso`, `ULTIMA_VERSION = max(NUMERO_VERSION)` de sus versiones, y asignar `ID_ARCHIVO` a todas sus versiones. Nombres, metadatos, contenidos, fechas, autores y estados de publicación de las versiones no se modifican.
2. Posiciones `PENDING` sin versiones: no generan archivo; el tipo queda pendiente (FR-021).
3. Estados y motivos "No aplica" permanecen en la posición (FR-022).
4. Idempotencia: solo procesa versiones con `ID_ARCHIVO` nula; reejecutar no cambia el resultado.
5. Verificación: conteos por expediente y tipo (posiciones → archivos; versiones antes = después; contenidos intactos) emitidos por la propia operación; sin mezclar expedientes de iniciativas y proyectos; el proyecto derivado no hereda archivos (FR-023).

## Cambios en el DDL derivado (`database/generated/piip-oracle.sql`)

- Nueva tabla `ARCHIVO_DOCUMENTO` con FK a `DOCUMENTO`.
- `DOCUMENTO_VERSION`: nueva columna `ID_ARCHIVO` (nullable) con FK a `ARCHIVO_DOCUMENTO`; nueva UK `(ID_ARCHIVO, NUMERO_VERSION)`; retiro de la UK `(ID_DOCUMENTO, NUMERO_VERSION)`.
- `DOCUMENTO`: retiro de la columna `ULTIMA_VERSION`.
- El archivo se regenera desde Hibernate (nunca edición manual) y su aplicación sobre la base desplegada sigue el mecanismo de la feature 015 (`validate` en dev/prod; ajuste de esquema con el DDL derivado).

## Trazabilidad requisitos → modelo

| Requisitos | Elemento del modelo |
|------------|---------------------|
| FR-001..FR-006 | `ARCHIVO_DOCUMENTO` por posición; "agregar archivo" crea archivo con versión 1 |
| FR-007..FR-008 | UK `(ID_ARCHIVO, NUMERO_VERSION)`; historial aislado por archivo |
| FR-009..FR-011 | Consulta y descarga operan sobre archivos activos y sus versiones (reglas de ámbito vigentes) |
| FR-012..FR-016 | `ELIMINADO`/`FECHA_ELIMINACION`/`ELIMINADO_POR` + evento `DOCUMENTO_ARCHIVO_ELIMINADO` |
| FR-017..FR-019 | Validaciones de versión sin cambios; posición conserva estado y declaración por tipo |
| FR-020..FR-023 | Reglas de migración y verificación por conteos |
| FR-024..FR-026 | `ES_ORIGINAL` + contrato de compatibilidad + conteos por estado de posición |
