# Quickstart: validación de múltiples archivos independientes por tipo documental

**Feature**: `016-multiples-archivos-documentales` | **Fecha**: 2026-09-06

Guía de validación end-to-end. Los detalles de contrato están en [contracts/documents-api.md](./contracts/documents-api.md) y las reglas de modelo en [data-model.md](./data-model.md); aquí no se duplican.

## Prerrequisitos

- Backend operativo con Oracle (o esquema de pruebas H2) y Keycloak; DDL derivado regenerado aplicado ([data-model.md](./data-model.md), sección DDL).
- Expediente de prueba (iniciativa o proyecto) con tipos documentales presentados.
- Usuarios de prueba: un `ADMINISTRADOR_PIIP` de la unidad ejecutora del expediente, un administrador de otra unidad, un interno con lectura del ámbito y un `CONSULTA_EXTERNA` del ámbito.

## Comandos de validación (referencia)

Ejecución manual; requiere autorización explícita del usuario (regla del monorepo):

- Backend: `gradlew.bat test`, `gradlew.bat check`, `gradlew.bat integrationTest` (en `apps/backend`; Windows).
- Frontend: `npm test -- --watch=false`, `npm run build` (en `apps/frontend`).

## Escenarios de validación

### 1. Dos archivos independientes del mismo tipo (FR-001..FR-006)

1. Como administrador del ámbito, "Agregar archivo" dos veces sobre el mismo tipo documental (un PDF y un DOCX, p. ej.).
2. **Esperado**: dos elementos separados, cada uno con versión 1 y sus metadatos (nombre, fecha, autor); el tipo deja de presentarse pendiente. Rutas: `POST .../documents/{documentTypeId}/files` (contracts).

### 2. Aislamiento del historial (FR-007, FR-008)

1. Con dos archivos A y B del tipo, crear una "Nueva versión" de A.
2. **Esperado**: A pasa a versión 2 con historial [1, 2]; B permanece en versión 1 con metadatos, contenido y publicación intactos.

### 3. Eliminación individual (FR-012..FR-016)

1. Eliminar A.
2. **Esperado**: A desaparece de todas las consultas para todos los roles; B sigue visible con su versión vigente e historial; las descargas de versiones de A fallan (404); la eliminación queda registrada en auditoría (`DOCUMENTO_ARCHIVO_ELIMINADO` con archivo, tipo, expediente, versión vigente, actor y momento); los eventos previos persisten.

### 4. Autorización (SC-004, FR-014, FR-018)

1. Con el administrador de otra unidad y el usuario sin asignación: intentar agregar, versionar, eliminar y descargar fuera de regla.
2. **Esperado**: todos los intentos rechazados (403/404 conforme a contracts); la consulta externa solo descarga versiones publicadas de archivos activos.

### 5. Migración sin pérdida (FR-020..FR-023)

1. En un entorno con datos previos (posiciones cargadas, con versiones, "No aplica" y pendientes), registrar conteos por expediente y tipo; ejecutar la migración mediante su arranque dedicado (activación explícita del modo migración, ver plan.md); repetir los conteos.
2. **Esperado**: cada posición con versiones produce el archivo original del tipo con todas sus versiones (nombres, metadatos, contenidos, fechas, autores y publicación intactos); posiciones pendientes sin archivo; declaraciones "No aplica" conservadas por tipo; conteos de versiones idénticos antes y después; sin cruces entre expedientes de iniciativas y proyectos.

### 6. Interfaz (SC-006, FR-003, FR-009)

1. Abrir la página documental del expediente.
2. **Esperado**: archivos listados individualmente bajo su tipo; "Agregar archivo", "Nueva versión" y "Eliminar archivo" distinguibles sin ambigüedad; historial y versión vigente visibles por archivo; el selector de archivo toma un único archivo por operación.

### 7. Compatibilidad con clientes existentes (FR-024, FR-025)

1. Cargar dos veces el mismo tipo con la operación actual por tipo (`POST .../documents/{documentTypeId}/versions`).
2. **Esperado**: la primera carga crea (o usa) el archivo original con versión 1 y la segunda crea la versión 2 de ese mismo archivo, igual que hoy; la consulta actual sigue exponiendo por tipo el archivo original con su historial (`versions[]` compat) y añade `files[]`.

### 8. Declaración "No aplica" (FR-019, FR-022)

1. Marcar "No aplica" un tipo con archivos; luego cargar un archivo de ese tipo.
2. **Esperado**: la declaración convive con los archivos; la carga reanuda el tipo (deja de aplicar y el tipo vuelve a presentarse con archivos); el motivo histórico queda trazado en auditoría.

### 9. Expedientes separados (SC-008)

1. Operar sobre una iniciativa y su proyecto derivado.
2. **Esperado**: archivos, versiones y declaraciones de cada expediente permanecen separados; el proyecto derivado no hereda archivos.

## Criterio de éxito global

Los nueve escenarios pasan con las reglas vigentes intactas (MIME, tamaño, publicación, descarga, auditoría) y sin restauración de eliminados. La suite automatizada asociada (`gradlew.bat test`, `npm test -- --watch=false`) cubre estos escenarios de forma repetible; su ejecución se solicita expresamente.
