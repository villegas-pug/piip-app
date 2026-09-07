---
description: "Lista trazable de tareas para una feature nueva del monorepo PIIP"
---

# Tareas: Múltiples archivos independientes por tipo documental

**Entrada**: documentos de `/specs/016-multiples-archivos-documentales/`

**Prerrequisitos**: `spec.md` y `plan.md` vigentes; documentos de diseño disponibles (`research.md`, `data-model.md`, `contracts/documents-api.md`, `quickstart.md`); sin checklists pendientes ni `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI ni integración Oracle. La invocación explícita de `/speckit.implement` autoriza las tareas de implementación de la feature activa y aprueba sus artefactos vigentes; las demás acciones requieren autorización separada.

## Formato obligatorio

Cada tarea usa:

`- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta/archivo exacto`

- **[P]**: puede ejecutarse en paralelo con las otras tareas [P] con las que no comparte archivos (ver «Oportunidades paralelas»); sus dependencias con tareas previas se declaran explícitamente en la propia tarea y se satisfacen antes de su ejecución.
- **[US#] / [FR-###]**: toda tarea se vincula con al menos una historia o requisito de la spec.
- La descripción incluye una ruta real del monorepo; no se aceptan ubicaciones genéricas.
- `[X]` se utiliza solo después de obtener evidencia de que el cambio fue realizado.
- Toda excepción constitucional para DML inicial o reset destructivo de auditoría incluye tareas explícitas de guardias fail-closed, prohibición productiva, allowlist y verificación del alcance eliminado. Esta feature no introduce excepciones DML: la migración es una operación de aplicación transaccional JPA.

## Evidencia de baseline - no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| FR-006 | Carga de a un archivo por operación: `upload` procesa un único archivo; el selector toma solo `files[0]` | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java`, `apps/frontend/src/app/pages/documents/documents.component.ts` | Mantener sin cambios |
| FR-017 | Validaciones vigentes: MIME pdf/docx/xlsx, tamaño máximo configurado, archivo no vacío, checksum SHA-256 | `DocumentService.java` (validación de carga) | Reutilizar tal cual en las tres vías de carga |
| FR-011 / FR-018 | Descarga con lectura del ámbito; externos solo versiones publicadas; escrituras con `ADMINISTRADOR_PIIP` sobre la unidad ejecutora | `DocumentService.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/LocalAuthorizationService.java` | Mantener reglas; extender su cobertura a archivos |
| FR-019 | "No aplica" por (registro, tipo) con motivo de hasta 500, unidireccional; la carga fuerza `LOADED` | `DocumentService.java` (markNotApplicable), `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentEntity.java` | Conservar semántica por tipo documental |
| FR-015 (patrón) | Auditoría append-only con detalle por evento | `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditService.java` | Reutilizar patrón para `DOCUMENTO_ARCHIVO_ELIMINADO` |
| FR-024 | La carga por tipo versiona la posición existente | `DocumentService.java` (upload) | Re-base sobre archivos conservando el efecto |
| FR-026 | El resumen del expediente cuenta posiciones por estado | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentInboxService.java` | Mantener escala por tipo documental |
| A8 | Notificación a consulta externa por publicación de versión | `DocumentService.java` (publish) | Mantener por versión |
| — | Publicación por versión con bloqueo optimista | `DocumentService.java` (publish) | Mantener; `404` sobre versiones de archivos eliminados |

## Phase 1: Fundamento del modelo documental (bloqueante)

**Propósito**: establecer el fundamento canónico (modelo JPA y DDL derivado) del que dependen todas las historias.

- [x] T001 [FR-001] [FR-013] Crear la entidad de archivo independiente `DocumentFileEntity` mapeada a `ARCHIVO_DOCUMENTO` (ID, ID_DOCUMENTO NOT NULL, ES_ORIGINAL, ULTIMA_VERSION, ELIMINADO = false, FECHA_ELIMINACION, ELIMINADO_POR, @Version; ciclo ACTIVO→ELIMINADO unidireccional) conforme a `specs/016-multiples-archivos-documentales/data-model.md` en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentFileEntity.java`
- [x] T002 [FR-007] Añadir `ID_ARCHIVO` (nullable en mapeo, asignada por invariant de servicio) a la versión, conservar `ID_DOCUMENTO` y reubicar `UK_DOC_VERSION` a `(ID_ARCHIVO, NUMERO_VERSION)` en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentVersionEntity.java` (depende de T001)
- [x] T003 [FR-001] Retirar `ULTIMA_VERSION` de la posición y reducir `registerUpload()` a forzar el estado `LOADED` (la versión vigente pasa a ser concepto del archivo) en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentEntity.java`
- [x] T004 [FR-009] [FR-012] Crear `DocumentFileRepository` con las consultas por posición (archivos activos, original activo) en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentFileRepository.java` (depende de T001)
- [x] T005 [FR-017] Extraer en `DocumentService` el núcleo compartido de validación y persistencia de versiones (MIME/tamaño/no vacío/checksum/contenido, autorización y auditoría `DOCUMENTO_CARGADO` con identificador del archivo) reutilizable por carga por tipo, "agregar archivo" y "nueva versión por archivo" en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` (depende de T001-T004)
- [x] T006 [FR-004] [FR-024] Re-base de la carga por tipo (`DocumentService.upload`) sobre el núcleo de archivos: sin original activo crea un archivo nuevo marcado original con versión 1; con original activo crea la versión siguiente de ese archivo; efecto, validaciones y estado de la posición se conservan en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` (depende de T005)
- [x] T007 [FR-024] Adaptar las pruebas del núcleo documental vigentes al modelo con archivos conservando sus aserciones de efecto (dos cargas por tipo → versiones 1 y 2 del mismo archivo original; inicialización de posiciones; "No aplica"; autorizaciones) en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentCatalogFlowTest.java`, `DocumentAuthorizationTest.java` y `PortfolioDocumentServiceTest.java` (depende de T006)
- [x] T008 [FR-020] Regenerar mediante Hibernate y revisar `database/generated/piip-oracle.sql` (tabla `ARCHIVO_DOCUMENTO`; `DOCUMENTO_VERSION.ID_ARCHIVO` con FK y UK `(ID_ARCHIVO, NUMERO_VERSION)`; retiro de `DOCUMENTO.ULTIMA_VERSION` y de la UK anterior) sin edición manual, verificando tablas, índices, FK y constraints — autorización explícita requerida (depende de T001-T003)

**Checkpoint**: el modelo JPA compila con `ARCHIVO_DOCUMENTO`, la UK de versión por archivo y la posición sin `ULTIMA_VERSION`; la carga por tipo conserva su efecto (versiones 1 y 2 del archivo original) y el DDL derivado refleja el nuevo modelo.

---

## Phase 2: Backend - US1 Agregar archivo independiente

**Objetivo**: el administrador del ámbito agrega varios archivos independientes del mismo tipo, cada uno con versión 1 (FR-001..FR-006).

- [x] T009 [US1] [FR-002] [FR-005] Implementar `DocumentService.addFile(recordCode, documentTypeId, DocumentUploadInput)`: validaciones vigentes, archivo nuevo con versión 1 (sin marca de original, research D3), posición a `LOADED` (con reanudación de "No aplica" conforme a FR-019), rechazo de tipos inexistentes o inactivos, auditoría `DOCUMENTO_CARGADO` con identificador del archivo en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` (depende de T005)
- [x] T010 [US1] [FR-001] [FR-003] Añadir `FileResponse` (id, original, latestVersion, current, versions[]) al DTO documental conforme a `specs/016-multiples-archivos-documentales/contracts/documents-api.md` en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentDtos.java`
- [x] T011 [US1] [FR-002] Exponer `POST /portfolio-records/{recordCode}/documents/{documentTypeId}/files` (multipart de un archivo, 201 con `FileResponse`) en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentController.java` (depende de T009 y T010)
- [x] T012 [US1] [FR-002] [FR-017] [FR-019] Documentar con pruebas backend US1: dos "agregar archivo" del mismo tipo → dos archivos independientes con versión 1; agregar un archivo a un tipo con declaración "No aplica" vigente reanuda el tipo (la declaración deja de aplicar y la posición pasa a `LOADED`); rechazos de archivo vacío, MIME no permitido, tamaño sobre el máximo, tipo inactivo y usuario sin administración (incluido administrador de otra unidad) en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/` y `apps/backend/src/test/java/pe/gob/midagri/piip/documents/api/DocumentControllerContractTest.java` (depende de T011)

**Checkpoint**: dos POST sucesivos de archivos del mismo tipo producen dos `FileResponse` independientes con versión 1, sin crear versiones adicionales.

---

## Phase 3: Backend - US2 Nueva versión de un archivo específico

**Objetivo**: crear una nueva versión de un archivo identificado, sin tocar ningún otro archivo (FR-004, FR-007, FR-008).

- [x] T013 [US2] [FR-007] Implementar `DocumentService.addVersionToFile(recordCode, fileId, DocumentUploadInput)`: validaciones vigentes, incrementa exclusivamente el historial del archivo identificado (`404` si no existe, está eliminado o pertenece a otro expediente), auditoría `DOCUMENTO_CARGADO` con identificador del archivo en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` (depende de T005)
- [x] T014 [US2] [FR-004] Exponer `POST /portfolio-records/{recordCode}/documents/files/{fileId}/versions` (multipart de un archivo, 201 con `VersionResponse`) en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentController.java` (depende de T013)
- [x] T015 [US2] [FR-008] Documentar con pruebas backend US2: nueva versión de A → A versión 2 con historial [1, 2] y B intacta (metadatos, contenido, publicación); `404` por archivo eliminado o de otro expediente; rechazo sin administración del ámbito en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/` (depende de T014)

**Checkpoint**: la nueva versión de un archivo no altera número de versión, metadatos, contenidos, publicaciones ni historial de ningún otro archivo.

---

## Phase 4: Backend y contrato - US3 Consulta de archivos, vigente e historial

**Objetivo**: consultar todos los archivos del tipo con su vigente e historial, y publicar el contrato (FR-009..FR-011, FR-025).

- [x] T016 [US3] [FR-009] [FR-010] Extender el listado del expediente (`DocumentService.list` y `DocumentResponse`): `files[]` con los archivos activos del tipo (ascendente por fecha de la primera versión, empates por id) con vigente e historial por archivo; `versions[]` de compatibilidad como historial del original activo (vacío sin original activo); `latestVersion` del original activo o 0; consulta externa restringida a versiones publicadas de archivos activos en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentDtos.java` (depende de T004)
- [x] T017 [US3] [FR-011] Documentar con pruebas backend US3: dos archivos (uno con dos versiones y otro con una) visibles como elementos separados con vigente e historial; interno descarga cualquier versión; externa solo publicadas; `versions[]` de compatibilidad expone el historial del original (FR-025) en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/` (depende de T016)
- [x] T018 [US3] [FR-025] Generar y revisar `apps/backend/target/piip-openapi.json` con `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` desde `apps/backend`, verificando las operaciones nuevas y la respuesta extendida contra `specs/016-multiples-archivos-documentales/contracts/documents-api.md` — autorización explícita requerida (depende de T011, T014 y T016)

**Checkpoint**: el listado expone por tipo los archivos activos con su vigente e historial, y el contrato publicado refleja las rutas nuevas sin romper las existentes.

---

## Phase 5: Frontend - US1/US2/US3 (después del contrato publicado)

**Objetivo**: la interfaz presenta archivos individuales con historial y acciones distinguibles de agregar y versionar (SC-006, sin eliminación aún).

- [x] T019 [US3] [FR-009] Sincronizar el cliente Angular generado con `npm run api:generate` desde `apps/frontend` (entrada `apps/backend/target/piip-openapi.json`, salida `apps/frontend/src/app/api/generated/`, sin edición manual) — autorización explícita requerida (depende de T018)
- [x] T020 [US3] [FR-003] Adaptar el repositorio HTTP y los modelos de presentación para representar archivos individuales (identidad por archivo, versión vigente e historial por archivo, conservando los indicadores del resumen) en `apps/frontend/src/app/core/piip-http.repository.ts` y `apps/frontend/src/app/core/piip.models.ts` (depende de T019)
- [x] T021 [US1] [US2] [US3] [FR-004] Rediseñar la página del expediente documental: archivos como elementos separados con vigente e historial, acciones distinguibles "Agregar archivo" y "Nueva versión" (por archivo), descargas por versión y accesibilidad vigente (etiquetas, foco, estados de carga y error) en `apps/frontend/src/app/pages/documents/documents.component.ts`, `documents.component.html` y `documents.component.scss` (depende de T020)
- [x] T022 [P] [US3] [FR-003] Actualizar el repositorio mock a la nueva forma de presentación por archivos en `apps/frontend/src/app/core/piip-mock.repository.ts` (depende de T020)
- [x] T023 [P] [US1] [US2] [FR-004] Documentar con pruebas de componente la presentación individual de archivos, el historial visible y la distinción de acciones en `apps/frontend/src/app/pages/documents/documents.component.spec.ts` (depende de T021)

**Checkpoint**: la interfaz lista cada archivo como elemento separado con su historial y distingue "Agregar archivo" de "Nueva versión".

---

## Phase 6: Backend - US4 Migración sin pérdida (paralelizable)

**Objetivo**: cada posición existente migra a archivo original sin pérdida ni mezcla de expedientes (FR-020..FR-023).

- [x] T024 [P] [US4] [FR-020] Implementar la operación de migración transaccional e idempotente como servicio de aplicación: posiciones con al menos una versión → archivo original (`ES_ORIGINAL`, `ULTIMA_VERSION = max`) con todas sus versiones asignadas por `ID_ARCHIVO` nula, sin modificar nombres, metadatos, contenidos, fechas, autores ni publicaciones; posiciones pendientes sin archivo; declaraciones "No aplica" intactas; conteos por expediente y tipo emitidos por la operación, en un nuevo servicio en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/` con su invocación por arranque dedicado de migración (activación explícita deshabilitada por defecto; ejecuta, registra los conteos y termina; sin controlador HTTP) conforme a research D4 (depende de T001-T004)
- [x] T025 [P] [US4] [FR-023] Documentar con pruebas backend US4: posición con tres versiones → archivo original con esas tres versiones intactas; pendiente sin archivo; "No aplica" conservada; idempotencia (re-ejecución sin cambios); sin mezcla entre expedientes de iniciativas y proyectos; el proyecto derivado no hereda archivos, en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/` (depende de T024)

**Checkpoint**: la migración es verificable por conteos por expediente y tipo con cero pérdida (SC-005) y cero mezclas (SC-008).

---

## Phase 7: Backend - US5 Eliminación individual lógica

**Objetivo**: eliminar un archivo identificado sin afectar a los demás, con autorización y auditoría (FR-012..FR-016).

- [x] T026 [US5] [FR-012] [FR-013] Implementar `DocumentService.deleteFile(recordCode, fileId)`: inhabilitación lógica (ELIMINADO, FECHA_ELIMINACION, ELIMINADO_POR) sin tocar otros archivos ni destruir contenidos, recálculo del estado de la posición (PENDING sin declaración vigente; NOT_APPLICABLE con declaración), autorización de carga (`requireUnit(ADMINISTRADOR_PIIP, unidad ejecutora)`), auditoría `DOCUMENTO_ARCHIVO_ELIMINADO` con expediente, tipo, archivo, versión vigente, nombre vigente, actor y momento en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` (depende de T005)
- [x] T027 [US5] [FR-013] [FR-016] Ocultar archivos eliminados y sus versiones para todos los roles: descarga y publicación de versiones de archivos eliminados responden `404`; el listado solo expone archivos activos, en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` (depende de T016 y T026)
- [x] T028 [US5] [FR-012] Exponer `DELETE /portfolio-records/{recordCode}/documents/files/{fileId}` (204; `404` inexistente/eliminado/otro expediente; `403` sin administración del ámbito) en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentController.java` (depende de T026)
- [x] T029 [US5] [FR-015] Documentar con pruebas backend US5: eliminar A → A y sus versiones invisibles e indescargables para todos los roles, B intacta con vigente e historial, la versión publicada de A deja de estar disponible para consulta externa, la eliminación queda en auditoría y los rechazos de autorización (incluido administrador de otra unidad) operan, en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/` (depende de T027 y T028)
- [x] T030 [US5] [FR-013] Regenerar y revisar `apps/backend/target/piip-openapi.json` (misma prueba y autorización que T018) con la operación de eliminación y sincronizar el cliente Angular con `npm run api:generate` desde `apps/frontend` — autorización explícita requerida (depende de T028)

**Checkpoint**: la eliminación de un archivo no modifica otros archivos ni sus historiales (SC-003) y queda registrada en auditoría (SC-007).

---

## Phase 8: Frontend - US5 Eliminación individual

**Objetivo**: la acción "Eliminar archivo" opera por archivo en la interfaz (SC-006 completo).

- [x] T031 [US5] [FR-012] Añadir la acción "Eliminar archivo" por archivo en la página del expediente (confirmación explícita, mensaje de resultado, accesibilidad vigente) en `apps/frontend/src/app/pages/documents/documents.component.ts` y `documents.component.html` (depende de T030)
- [x] T032 [P] [US5] [FR-015] Registrar la etiqueta y el detalle del evento `DOCUMENTO_ARCHIVO_ELIMINADO` en el presentador de auditoría en `apps/frontend/src/app/pages/audit/audit-event.presenter.ts` y `apps/frontend/src/app/pages/audit/audit-event.presenter.spec.ts`
- [x] T033 [P] [US5] [FR-012] Documentar con pruebas de componente la acción "Eliminar archivo" (solo el archivo objetivo, confirmación y estados) en `apps/frontend/src/app/pages/documents/documents.component.spec.ts` (depende de T031)

**Checkpoint**: la interfaz distingue sin ambigüedad "Agregar archivo", "Nueva versión" y "Eliminar archivo" (SC-006 completo).

---

## Phase 9: Compatibilidad explícita - US6

**Objetivo**: los clientes existentes conservan efecto e información; los indicadores cuentan por tipo (FR-024..FR-026).

- [x] T034 [US6] [FR-024] [FR-025] Documentar con pruebas de compatibilidad los clientes existentes: dos cargas por la operación actual por tipo → versiones 1 y 2 del mismo archivo original (misma aserción que T007, aquí verificada por la vía del contrato HTTP); carga por tipo sin original activo → archivo nuevo original con versión 1; la consulta actual expone por tipo el original con su historial completo (`versions[]`, `latestVersion`) en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/api/DocumentControllerContractTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentCatalogFlowTest.java` (depende de T007 y T016)
- [x] T035 [US6] [FR-026] Verificar y, solo si el efecto se alterara, ajustar los conteos del resumen del expediente por tipo documental (cargado con al menos un archivo no eliminado; totales en la escala del catálogo) con su prueba en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentInboxService.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/` (depende de T027)

**Checkpoint**: los clientes existentes conservan efecto e información y los indicadores siguen contando por tipo.

---

## Phase 10: Documentación y cierre

- [x] T036 [FR-001] [FR-019] Actualizar la guía funcional documental (archivos independientes por tipo, declaración "No aplica" por tipo y su reanudación por carga) en `docs/architecture/piip-fields.md`
- [x] T037 [FR-001] [FR-020] Actualizar el modelo de datos documental (ER con `ARCHIVO_DOCUMENTO`, UK de versión por archivo, migración) en `docs/architecture/data-model-final.md` y `docs/architecture/data-model-comparison.md`
- [x] T038 [FR-001..FR-026] Registrar en esta lista las tareas completadas con evidencia, las pendientes y las validaciones realmente ejecutadas en `specs/016-multiples-archivos-documentales/tasks.md`

## Validaciones propuestas - requieren autorización

- [x] T039 [FR-001] Suite de pruebas backend desde `apps/backend` con `gradlew.bat test` - autorización requerida
- [x] T040 [FR-020] Generación DDL desde `apps/backend` con `gradlew.bat test --tests pe.gob.midagri.piip.persistence.OracleSchemaGenerationTest` y comparación con `database/generated/piip-oracle.sql` - autorización requerida
- [x] T041 [FR-025] Generación del contrato desde `apps/backend` con `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` (artefacto `apps/backend/target/piip-openapi.json`) - autorización requerida
- [x] T042 [US3] Suite frontend desde `apps/frontend` con `npm test -- --watch=false` - autorización requerida
- [x] T043 [FR-023] Integración Oracle desde `apps/backend` con `gradlew.bat integrationTest` (migración y expedientes sobre Oracle real) - autorización requerida
- [x] T044 [US3] Compilación frontend desde `apps/frontend` con `npm run build` - autorización requerida

## Dependencias y orden de ejecución

- **Propietario canónico**: backend `documents` (T001-T018, T024-T030); artefactos derivados del backend: `database/generated/piip-oracle.sql` (T008), `apps/backend/target/piip-openapi.json` (T018, T030) y cliente generado `apps/frontend/src/app/api/generated/` (T019, T030).
- **Consumidores**: frontend de presentación y acciones (T019-T023 tras T018; T031-T033 tras T030); documentación de arquitectura (T036-T037) tras el modelo y contrato definitivos.
- **Orden obligatorio**: T001-T003 → T004-T006 → T007-T008 (fundamento) → T009-T012 (US1) → T013-T015 (US2) → T016-T018 (US3 backend y contrato) → T019-T023 (frontend) → T026-T030 (US5 backend) → T031-T033 (frontend US5); T034-T035 (US6) tras T016/T027; T036-T038 al cierre.
- **Oportunidades paralelas**: T024-T025 (US4, área de migración) tras el fundamento y en paralelo con las fases US1-US3; T022, T023, T032 y T033 entre sí por no compartir archivos; T036-T037 en paralelo tras el contrato definitivo.

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- No crear tareas a partir de pendientes históricos por arrastre (el antimalware de `specs/003-documents` queda fuera, conforme al alcance excluido de la spec vigente).
- Si la feature depende directamente de un requisito histórico, registrar aquí el requisito, la dependencia y su aprobación explícita: `specs/003-documents` fija la cardinalidad de una posición por tipo que esta feature supersede expresamente; aprobación registrada en la sección "Contradicciones y supersesión de antecedentes" de `specs/016-multiples-archivos-documentales/spec.md`.
- Registrar contradicciones sin resolver como `NEEDS CLARIFICATION`: Ninguna.

## Notas

- Cada tarea representa trabajo nuevo aprobado y apunta a una ruta real.
- No reimplementar capacidades incluidas en la evidencia de baseline.
- No paralelizar cambios que compartan contrato, catálogo, regla funcional, documentación o configuración: el backend publica primero y el frontend consume después.

## Evidencia de ejecución - 2026-09-07

- T001-T037 y T038 completadas. La implementación backend, contrato, cliente generado, presentación Angular y guía funcional están actualizados; la lista de tareas refleja evidencia en sus rutas asociadas.
- T008/T040: `gradlew.bat test --tests pe.gob.midagri.piip.persistence.OracleSchemaGenerationTest` terminó con `BUILD SUCCESSFUL`. El DDL generado contiene `ARCHIVO_DOCUMENTO`, `DOCUMENTO` sin `ULTIMA_VERSION`, `DOCUMENTO_VERSION.ID_ARCHIVO`, `UK_DOC_VERSION (ID_ARCHIVO, NUMERO_VERSION)`, `FK_ARCHDOC_DOC` y `FK_DOCVER_ARCHIVO`; `apps/backend/target/piip-oracle.sql` coincide byte a byte con `database/generated/piip-oracle.sql`.
- T018/T030/T041: `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` terminó con `BUILD SUCCESSFUL`. El contrato generado contiene las rutas para agregar, versionar y eliminar archivos, además de `FileResponse` y `DocumentResponse.files`; `npm run api:generate` sincronizó el cliente Angular (T019).
- T039, T042, T043 y T044 fueron autorizadas y ejecutadas con resultado exitoso.
- T042 requirió corregir tres errores de compilación frontend: el retorno de `mapDocuments`, el tipado inicial de `documentDossiers` y el cierre sintáctico del proveedor `ActivatedRoute`; después pasaron 39 archivos y 242 pruebas.
- T044 generó `dist/piip-web2` correctamente; Angular reportó advertencias no bloqueantes de presupuesto para el bundle inicial (566.42 kB frente a 500 kB) y `dashboard.component.scss` (12.96 kB frente a 12 kB).
- T043: `.\gradlew.bat integrationTest` desde `apps/backend` terminó con `BUILD FAILED`; 7 tests fueron detectados, 6 fallaron y 1 fue omitido. Los fallos ocurrieron durante la creación del contexto/conexión Oracle con `java.net.UnknownHostException` al resolver el host del datasource configurado en `application.yml`; no se alcanzaron las verificaciones funcionales de migración. `OracleContainerTest` fue el test omitido por la configuración `disabledWithoutDocker = true`.
- T043 se reintentó con el servidor Oracle disponible. El primer reintento confirmó conectividad, pero falló por `Schema validation: missing table [archivo_documento]`; se actualizó el soporte `test-reset` para la matriz vigente de 20 tablas y se repitió la tarea. El siguiente intento presentó fallos por validación concurrente contra el esquema anterior en contextos `dev`; el reset recreó correctamente el esquema actualizado. La repetición final de `.\gradlew.bat integrationTest` terminó con `BUILD SUCCESSFUL` en 30 s, con los tests Oracle reales y el reset de pruebas completados; `OracleContainerTest` permaneció omitido por `disabledWithoutDocker = true`.
- Para sincronizar `test-reset` con el modelo documental se actualizaron `TestResetSchemaFilterProvider`, `TestResetCoordinator`, `TestResetSchemaFilterTest` y `TestResetCoordinatorTest`: `ARCHIVO_DOCUMENTO` quedó incluido en la allowlist y en los órdenes de eliminación/creación, y la validación pasó de 19 a 20 tablas.
- El cierre distingue cambios realizados, tareas pendientes y validaciones no ejecutadas.
