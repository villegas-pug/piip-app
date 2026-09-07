# Research: Múltiples archivos independientes por tipo documental

**Feature**: `016-multiples-archivos-documentales` | **Fecha**: 2026-09-06

Decisiones de diseño derivadas del grounding contra el código vigente. El contexto técnico del plan no contiene NEEDS CLARIFICATION: todas las incógnitas funcionales se resolvieron en las clarificaciones de la spec y las técnicas con evidencia del repositorio.

## D1: Rol de la tabla `DOCUMENTO` ante la multi-cardinalidad

- **Decision**: conservar `DOCUMENTO` como posición/presencia del tipo documental (una fila por expediente y tipo, UK `UK_DOCUMENTO_REGISTRO_TIPO` intacta, estado y motivo "No aplica" intactos) e introducir una entidad nueva de archivo entre la posición y sus versiones.
- **Rationale**: la posición ya materializa la presentación del tipo y concentra el estado para los indicadores del resumen (FR-026) y la declaración "No aplica" (FR-019); conservarla deja el comportamiento de estados, reanudación por carga y conteos idéntico al vigente, y el DDL de la tabla existente cambia solo en el retiro de `ULTIMA_VERSION`.
- **Alternativas consideradas**: reinterpretar `DOCUMENTO` como archivo (eliminar su UK y migrar estado/motivo a una tabla nueva de presencia) — descartada porque muta la semántica de la tabla existente, elimina la UK que garantiza una presentación por tipo y obliga a mover estado y motivo de todas las filas; modelar la multi-cardinalidad relajando la UK sin entidad nueva — descartada porque duplicaría la declaración "No aplica" por archivo y violaría FR-019.

## D2: Nivel de la eliminación lógica

- **Decision**: la inhabilitación lógica vive en el archivo (`ELIMINADO`, `FECHA_ELIMINACION`, `ELIMINADO_POR` en `ARCHIVO_DOCUMENTO`); el archivo eliminado y todas sus versiones dejan de exponerse en todas las consultas y descargas para todos los roles, sin restauración en esta versión.
- **Rationale**: FR-012 exige operar sobre la identidad de un archivo; las versiones y contenidos no se tocan (FR-013, contenidos no destruidos); el rastro canónico de la operación queda en el evento de auditoría `DOCUMENTO_ARCHIVO_ELIMINADO`.
- **Alternativas consideradas**: borrado físico — excluido por la spec (A5, ausencia de política de borrado); marca de eliminación en la posición — descartada porque ocultaría todos los archivos del tipo y violaría FR-012; marca por versión — descartada porque exige recorrer el historial y no representa la identidad del archivo.

## D3: Identificación del "archivo original" para compatibilidad

- **Decision**: columna explícita `ES_ORIGINAL` en el archivo. El original surge de la migración (posición con versiones) o de una carga por la operación actual de tipo cuando no existe original activo; "agregar archivo" nunca crea originales.
- **Rationale**: FR-024/FR-025 anclan la compatibilidad al archivo original del tipo; una marca explícita es estable ante eliminaciones (si el original se elimina, la siguiente carga por tipo crea un archivo nuevo y lo marca original, conforme al edge case de la spec) y mantiene el Independent Test de US6 (dos cargas por tipo → versión 1 y 2 del mismo archivo).
- **Alternativas consideradas**: derivar el original como "el archivo más antiguo del tipo" — descartada porque se corrompe al eliminar el más antiguo y mezclaría la carga por tipo con archivos creados como independientes.

## D4: Estrategia de migración de datos

- **Decision**: operación de aplicación transaccional JPA, idempotente y verificable por conteos, ejecutada como paso de despliegue tras aplicar el DDL derivado regenerado. Por cada posición con versiones: crear el archivo (original, no eliminado, `ULTIMA_VERSION` = máximo número de versión) y asignarle sus versiones; las posiciones `PENDING` sin versiones no generan archivo; los estados y motivos "No aplica" se conservan en la posición. Canal de invocación: arranque dedicado de migración — el operador de despliegue lanza el monolito con una activación explícita (propiedad de arranque deshabilitada por defecto) bajo la cual el proceso ejecuta únicamente la operación idempotente, registra los conteos por expediente y tipo como evidencia y termina; el arranque normal del servicio nunca ejecuta la migración (la alternativa descartada era la migración automática acoplada al ciclo de vida normal, no una invocación explícita de despliegue).
- **Rationale**: la constitución IV prohíbe SQL nativo y herramientas de migración para acceso funcional; el volumen es de v1 (adopción inicial) y el código JPA es auditable e idempotente. La verificación por conteos por expediente y tipo la exige FR-023.
- **Alternativas consideradas**: DML externo versionado — prohibido en producción por la constitución (la excepción del perfil destructivo es exclusiva de desarrollo/pruebas y ya está reservada al seed/reset de la feature 015); migración automática al arranque — descartada por acoplar la transformación de datos al ciclo de vida de la aplicación.

## D5: Referencia de la versión al archivo y unicidad

- **Decision**: `DOCUMENTO_VERSION` conserva `ID_DOCUMENTO` (posición, como hoy) y añade `ID_ARCHIVO` (nullable en el mapeo para admitir el estado previo a migración; invariant de servicio: toda versión escrita por el código nuevo tiene archivo asignado). La unicidad de versiones pasa de `UK_DOC_VERSION (ID_DOCUMENTO, NUMERO_VERSION)` a `(ID_ARCHIVO, NUMERO_VERSION)`.
- **Rationale**: mantener `ID_DOCUMENTO` evita reasignar la FK de todas las versiones existentes y preserva las consultas por posición; la nueva columna permite una migración aditiva (insertar archivos + rellenar `ID_ARCHIVO`) y materializable en pruebas con el esquema nuevo; la unicidad reubicada es la que exige FR-007 (historial propio numerado desde 1 por archivo) y permite que dos archivos del mismo tipo tengan cada uno su versión 1 (FR-002).
- **Alternativas consideradas**: reasignar la FK de versión de posición a archivo — descartada porque exige un update masivo de la FK, impide representar el estado previo en el esquema nuevo y complica la prueba de migración; exigir `ID_ARCHIVO` NOT NULL desde el inicio — descartada porque haría imposible materializar datos previos a migrar en las pruebas automatizadas.

## D6: Contrato HTTP aditivo

- **Decision**: conservar intactas las rutas y formas actuales (listado por posición, carga por tipo `/documents/{documentTypeId}/versions`, "No aplica", publicación, descarga) y añadir `/documents/{documentTypeId}/files` (agregar archivo), `/documents/files/{fileId}/versions` (nueva versión de un archivo) y `DELETE /documents/files/{fileId}` (eliminación individual). La respuesta del listado añade `files[]` por posición y mantiene `versions[]` como el historial del archivo original activo (vacío si no hay original activo).
- **Rationale**: FR-024/FR-025 exigen que los clientes existentes conserven efecto e información; el añadido `files[]` da a la UI nueva la estructura por archivo (FR-003, FR-009). El cliente generado (`apps/frontend/src/app/api/generated/`) se sincroniza tras publicar `piip-openapi.json` (flujo vigente de contrato: publicar primero backend).
- **Alternativas consideradas**: reemplazar el listado por una forma solo por archivo — descartada porque rompe a los clientes actuales que leen `versions[0]`; versionar el contrato — descartada por innecesaria al ser un cambio aditivo.

## D7: Estado de la posición ante archivos múltiples

- **Decision**: el estado de la posición mantiene la semántica vigente (última acción gana): la carga fuerza `LOADED` (incluida la reanudación de una declaración "No aplica" vigente, con el tratamiento actual del motivo); marcar "No aplica" fuerza `NOT_APPLICABLE` con su motivo; al eliminar el único archivo activo de un tipo, la posición vuelve a `PENDING` si no hay declaración vigente o permanece `NOT_APPLICABLE` si la hay (A6).
- **Rationale**: conserva los indicadores del resumen por tipo sin tocar `DocumentInboxService` (FR-026) y replica el comportamiento observado en `DocumentEntity.registerUpload()` y `markNotApplicable()`.
- **Alternativas consideradas**: eliminar el estado de la posición y derivarlo siempre de los archivos — descartada porque cambia la semántica de los conteos vigentes y obliga a reescribir el resumen del expediente.

## D8: Eventos de auditoría

- **Decision**: reutilizar `DOCUMENTO_CARGADO` para toda carga (nuevo archivo o nueva versión) extendiendo su detalle con el identificador del archivo; añadir el evento `DOCUMENTO_ARCHIVO_ELIMINADO` para la eliminación individual, con expediente, tipo, archivo, versión vigente, actor y momento.
- **Rationale**: la auditoría es append-only (constitución V); la eliminación lógica no destruye eventos (edge case de la spec); la nomenclatura sigue el patrón vigente (`DOCUMENTO_NO_APLICA`, `DOCUMENTO_RETIRADO`).
- **Alternativas consideradas**: evento nuevo también para la creación de archivo — descartada por redundante con `DOCUMENTO_CARGADO` de su versión 1.
