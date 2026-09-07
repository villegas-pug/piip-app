# Contrato HTTP: expediente documental con archivos independientes

**Feature**: `016-multiples-archivos-documentales` | **Fecha**: 2026-09-06

Contrato del módulo `documents` expuesto por `DocumentController` (`apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/`). Base: `/portfolio-records/{recordCode}/documents`. Este documento es la fuente de diseño para la publicación de `piip-openapi.json` (publicar primero backend; el cliente Angular se sincroniza después).

## Principios del contrato

- **Aditivo**: las rutas y formas actuales se conservan con su efecto vigente (FR-024, FR-025); las operaciones nuevas de archivos se añaden junto a ellas.
- **Carga de a un archivo por operación** (FR-006): multipart con un único archivo, igual que hoy.
- **Autorización vigente** (FR-018): escrituras documentales con `ADMINISTRADOR_PIIP` sobre la unidad ejecutora del expediente; consulta y descarga con lectura del ámbito; consulta externa solo versiones publicadas externamente de archivos no eliminados.

## Operaciones existentes (compatibilidad)

### GET `/portfolio-records/{recordCode}/documents`

Lista el expediente documental por posición (tipo documental). **Respuesta extendida**: cada `DocumentResponse` añade `files[]`; los campos existentes se conservan.

| Campo | Tipo | Regla |
|-------|------|-------|
| id, documentType, state, notApplicableReason | — | Sin cambios (estado y declaración por tipo, FR-019/FR-026) |
| latestVersion | int | Versión vigente del archivo original activo; 0 si no hay original activo |
| versions[] | VersionResponse[] | **Compatibilidad**: historial completo del archivo original activo (descendente); vacío si no hay original activo (p. ej., eliminado) |
| files[] | FileResponse[] | **Nuevo**: archivos activos del tipo, orden ascendente por fecha de carga de su primera versión (empates por id) |

Consulta externa: ve solo archivos activos y, de cada archivo, únicamente sus versiones publicadas externamente (mismo criterio de filtrado vigente); si un archivo activo no tiene versiones publicadas, lo ve con `versions[]` vacío y `current` nulo.

### POST `/portfolio-records/{recordCode}/documents/{documentTypeId}/versions`

Carga por tipo (operación actual). **Sin cambios de ruta ni de efecto**: crea una nueva versión del archivo original activo del tipo; si no existe original activo (nunca lo hubo o fue eliminado), crea un archivo nuevo, lo marca original y su versión es la 1 (edge case de la spec; Independent Test de US6). Respuesta `201` con `VersionResponse`. Auditoría `DOCUMENTO_CARGADO` (detalle extendido con el identificador del archivo).

### PUT `/portfolio-records/{recordCode}/documents/{documentTypeId}/not-applicable`

Marca "No aplica" por tipo documental con motivo (límite vigente de 500). Sin cambios: declaración única por tipo, unidireccional, convive con los archivos del tipo (FR-019).

### PUT `/portfolio-records/{recordCode}/documents/versions/{versionId}/publication?published={bool}&version={expected}`

Publica o retira la publicación externa de una versión, con bloqueo optimista. Sin cambios. Sobre la versión de un archivo eliminado responde `422` con `reason=NOT_FOUND` (mecanismo canónico de referencia inválida; el archivo eliminado oculta sus versiones para todos los roles).

### GET `/portfolio-records/{recordCode}/documents/versions/{versionId}/content`

Descarga el contenido de una versión. Sin cambios en reglas: internos del ámbito descargan cualquier versión; consulta externa solo versiones publicadas. Sobre la versión de un archivo eliminado responde `422` con `reason=NOT_FOUND` para todos los roles (FR-013, FR-016).

## Operaciones nuevas

### POST `/portfolio-records/{recordCode}/documents/{documentTypeId}/files`

**Agregar archivo independiente** (FR-001..FR-006). Multipart con un único archivo (mismas validaciones: MIME pdf/docx/xlsx, tamaño máximo configurado, archivo no vacío, checksum). Crea un archivo nuevo del tipo cuya primera versión es la 1; nunca crea originales (research D3). Respuesta `201` con `FileResponse`. Auditoría `DOCUMENTO_CARGADO` (detalle con identificador del archivo). Rechaza tipos inexistentes o inactivos como referencia nueva (regla vigente).

### POST `/portfolio-records/{recordCode}/documents/files/{fileId}/versions`

**Nueva versión de un archivo específico** (FR-004, FR-007, FR-008). Multipart con un único archivo y las validaciones vigentes. Incrementa el historial del archivo identificado sin tocar ningún otro archivo. Respuesta `201` con `VersionResponse`. Auditoría `DOCUMENTO_CARGADO` (detalle con identificador del archivo). `422` (`reason=NOT_FOUND`) si el archivo no existe, está eliminado o pertenece a otro expediente (mecanismo canónico de referencia inválida).

### DELETE `/portfolio-records/{recordCode}/documents/files/{fileId}`

**Eliminación individual lógica** (FR-012..FR-016). Inhabilita el archivo identificado: este y todas sus versiones dejan de ser visibles y descargables para todos los roles; los contenidos no se destruyen; no afecta a otros archivos del mismo tipo ni a sus historiales; recalcula el estado de la posición (data-model). Respuesta `204` sin cuerpo. Auditoría `DOCUMENTO_ARCHIVO_ELIMINADO` con expediente, tipo (código y nombre), identificador del archivo, versión vigente al momento de eliminar, nombre del archivo vigente, actor y momento. `422` (`reason=NOT_FOUND`) si el archivo no existe, ya está eliminado o pertenece a otro expediente; `403` para usuarios sin administración del ámbito, incluidos administradores de otras unidades.

## Esquemas

### FileResponse (nuevo)

| Campo | Tipo | Contenido |
|-------|------|-----------|
| id | int | Identificador del archivo (para "nueva versión" y "eliminar") |
| original | bool | Marca del archivo receptor de la carga por tipo |
| latestVersion | int | Versión vigente del archivo |
| current | VersionResponse | Metadatos de la versión vigente (para consulta externa: la publicada más reciente, o nulo si no tiene publicadas) |
| versions | VersionResponse[] | Historial completo del archivo (descendente) |

### VersionResponse (sin cambios)

`id`, `version`, `filename`, `mimeType`, `sizeBytes`, `checksumSha256`, `uploadedAt`, `externallyPublished`, `optimisticVersion`.

### DocumentResponse (extendido)

 Campos existentes sin cambios + `files[]` (ver operación GET).

## Códigos de error comunes

| Código | Causa |
|--------|-------|
| 400 | Regla de negocio: archivo vacío, MIME no permitido, tamaño sobre el máximo configurado, motivo fuera de límite |
| 401 | No autenticado |
| 403 | Sin permiso funcional sobre el ámbito (incluido administrador de otra unidad) |
| 409 | Bloqueo optimista (`expectedVersion` desactualizado) en publicación |
| 422 | Referencia inválida (`reason=NOT_FOUND`/`INACTIVE`, mecanismo canónico de `InvalidReferenceException`): expediente, tipo documental inexistente o inactivo como referencia nueva, archivo inexistente, eliminado o de otro expediente, o versión inexistente o de archivo eliminado |

## Autorización por operación

| Operación | Autorización |
|-----------|--------------|
| GET listado, GET contenido | Lectura del ámbito del expediente (`requireReadableUnit`); externos restringidos a versiones publicadas |
| POST versions (por tipo), POST files, POST files/{fileId}/versions, PUT not-applicable, PUT publication | `ADMINISTRADOR_PIIP` sobre la unidad ejecutora del expediente (`requireUnit`) |
| DELETE files/{fileId} | `ADMINISTRADOR_PIIP` sobre la unidad ejecutora del expediente (`requireUnit`) — misma autorización que la carga (FR-014) |

## Publicación del contrato

La publicación regenera `piip-openapi.json` desde controladores y DTO (flujo vigente del monorepo: contrato backend primero, sincronización del cliente Angular después). Este documento no sustituye al artefacto publicado: fija el diseño del contrato para la implementación.
