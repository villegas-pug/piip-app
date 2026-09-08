---
description: "Lista trazable de tareas para una feature nueva del monorepo PIIP"
---

# Tareas: Unidades Orgánicas Involucradas

**Entrada**: documentos de `/specs/017-unidades-organicas-involucradas/`

**Prerrequisitos**: `spec.md`, `plan.md` y este `tasks.md` vigentes; `research.md`, `data-model.md`, `contracts/http-contract.md`, `contracts/ui-contract.md` y `quickstart.md` disponibles; sin checklists ni `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI ni integración Oracle. La invocación explícita de `/speckit.implement` autoriza las tareas de implementación de la feature activa y aprueba sus artefactos vigentes; las demás acciones requieren autorización separada.

## Formato obligatorio

Cada tarea usa:

`- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta/archivo exacto`

- **[P]**: puede ejecutarse en paralelo con otras tareas que no compartan sus archivos, una vez satisfecha la dependencia que declare explícitamente (p. ej. las tareas de pruebas `T013/T016/T019/T022/T024/T027/T036`).
- **[US#] / [FR-###]**: toda tarea se vincula con al menos una historia o requisito de la spec.
- La descripción incluye una ruta real del monorepo; no se aceptan ubicaciones genéricas.
- `[X]` se utiliza solo después de obtener evidencia de que el cambio fue realizado.
- Esta feature no agrega DML ni excepciones constitucionales nuevas: reutiliza el seed vigente dentro de la excepción ya autorizada (`test,test-reset`, DML-only, idempotente, fail-closed, prohibido en producción) y solo extiende su postvalidación en Java, sin crear perfiles adicionales ni habilitar cargas automáticas (FR-028).

## Evidencia de baseline - no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| [FR-005] [FR-007] | `REGISTRO_UNIDAD_RESPONSABLE` soporta N filas ordenadas con UK (registro, orden); `replace` ya resuelve/valida toda la lista antes de persistir y reinserta con orden continuo | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/ResponsibleUnitEntity.java`, `portfolio/application/ResponsibleUnitService.java` | Reutilizar esquema y atomicidad; solo ampliar la cardinalidad |
| [FR-010] | El servicio ya valida por unidad: existencia (incluida histórica), actividad y pertenencia a la Unidad Ejecutora del registro | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitService.java` | Extender a lista completa y añadir sigla para nuevas incorporaciones |
| [FR-022] | La lectura histórica resuelve unidades inactivas y la edición sin `responsibleUnits` conserva el conjunto persistido | `organization/persistence/OrganizationalUnitRepository.java` (`findHistoricalById`), `portfolio/application/InitiativeApplicationService.java` | Mantener; proteger con pruebas extendidas |
| [FR-012] | El catálogo de escritura ofrece solo unidades activas de la Unidad Ejecutora ordenadas por nombre | `organization/api/OrganizationQueryService.java`, `OrganizationController.java` | Añadir el filtro de sigla no vacía (Q2=A) |
| [FR-019] | La auditoría de actualización registra anterior/nuevo de `responsibleUnits` con `{id, code, name, displayOrder}` | `portfolio/application/PortfolioUpdateAuditDetail.java` | Extender con sigla y Nro por elemento |
| [FR-026] | Control de concurrencia por versión (409) en ediciones | `portfolio/application/InitiativeApplicationService.java`, `ProjectApplicationService.java` | Mantener intacto |
| [FR-027] [FR-029] | Seed idempotente (MERGE) con 4 UOs sintéticas (2 por UE) con código, nombre y sigla no vacíos | `apps/backend/src/main/resources/db/test/catalog-data.sql` (sección 3) | Reutilizar sin cambios de valores; no hay tarea de SQL |
| [FR-028] [FR-030] | Postvalidación del reset con conteos y vaciado de tablas operativas; guardias fail-closed de perfiles exactos | `config/reset/TestResetCoordinator.java` | Extender la postvalidación sin tocar guardias ni perfiles |

## Phase 2: Contrato o fundamento bloqueante

**Propósito**: el backend es el propietario canónico del contrato y de las reglas de lista; todo consumidor frontend depende de esta fase.

- [X] T001 [US1 US2 FR-001 FR-002 FR-003] Relajar `responsibleUnits` a mínimo uno sin máximo en los tres DTO de alta (`@NotEmpty @Size(min = 1)` sin `max`) y en los dos DTO de edición (getter `@Size(min = 1)`, `hasValidPresentValues` con tamaño ≥ 1, presencia JSON preservada) en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioDtos.java`
- [X] T002 [US1 US2 US5 FR-002 FR-003 FR-007 FR-010 FR-011 FR-024] Sustituir `requireExactlyOne` por la validación de la lista completa (mínimo una unidad; sin duplicados en el conjunto; nuevas incorporaciones: existentes, activas, de la misma Unidad Ejecutora del registro y con sigla no vacía; asociaciones históricas retenidas como contexto) manteniendo la resolución y validación previa a cualquier cambio de persistencia en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitService.java`, fijando aquí los códigos de rechazo (duplicado `DUPLICATED_UNIT`, nueva incorporación sin sigla `MISSING_ACRONYM`, ambos 422 identificando fila; lista vacía 400 por DTO) antes de publicar el contrato en T005
- [X] T003 [US1 US2 FR-005 FR-006] Persistir el orden continuo 1..N y la denominación original del maestro en `save` y en el reemplazo atómico de `replace` (renumeración completa al confirmar) en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitService.java` (depende de T002)
- [X] T004 [P] [US1 US2 FR-011 FR-012] Excluir las unidades con sigla vacía del catálogo `GET /organizational-units` (añadiendo el método de repositorio que el filtro requiera) en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationQueryService.java`, `organization/api/OrganizationController.java` y `organization/persistence/OrganizationalUnitRepository.java`
- [X] T036 [P] [US1 US2 US5 FR-002 FR-003 FR-007 FR-010 FR-011 FR-024] Extender las pruebas de validación del servicio (mínimo una unidad, duplicados, unidad inactiva o de otra Unidad Ejecutora, nueva incorporación sin sigla, históricas retenidas como contexto, atomicidad del reemplazo) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitValidationTest.java` (depende de T002)
- [x] T005 [US1 US2 US5 FR-001 FR-002 FR-003 FR-011 FR-012] Publicar el contrato OpenAPI regenerando `apps/backend/target/piip-openapi.json` con `OpenApiGenerationTest` en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java` (depende de T001-T004; ejecución sujeta a autorización explícita)
- [x] T006 [US1 US2 FR-001] Sincronizar el cliente Angular generado desde el contrato publicado (nunca editado a mano) en `apps/frontend/src/app/api/generated/` (depende de T005; ejecución con `npm run api:generate` sujeta a autorización explícita)

**Checkpoint**: DTO y reglas de lista definidos, contrato publicado y cliente sincronizado antes de modificar consumidores; sin cambios estructurales de persistencia (el esquema vigente ya soporta N).

## Phase 3: Frontend - US1 Registro con una o varias unidades

**Objetivo**: los tres tipos de alta confirman una lista dinámica y ordenada, independiente y testeable de extremo a extremo.

- [X] T007 [US1 FR-004 FR-005 FR-006 FR-012 FR-013 FR-015 FR-016 FR-017] Crear el componente standalone de lista dinámica reutilizable (filas Nro/Descripción/Abreviatura de solo lectura del maestro, agregar al final, retirar salvo última fila, renumeración automática 1..N, opciones sin duplicados ya seleccionados, estados cargando/vacío/error con reintento, accesible: operación completa por teclado, etiqueta/rol/estado por fila y control para tecnologías de asistencia, foco gestionado al agregar/retirar; adaptable a pantallas pequeñas) en `apps/frontend/src/app/shared/organizational-unit-list/`
- [X] T008 [US1 FR-001 FR-002] Adaptar los contratos internos de entrada y el envío de la lista ordenada en los tres registros de alta (`responsibleUnits` como arreglo de `{ organizationalUnitId }`) en `apps/frontend/src/app/core/piip.models.ts`, `apps/frontend/src/app/core/piip.repository.ts` y `apps/frontend/src/app/core/piip-http.repository.ts` (depende de T006)
- [X] T009 [US1 FR-001 FR-012 FR-013 FR-017] Sustituir el select único por la lista dinámica con etiqueta "Unidades Orgánicas Involucradas", dependencias del catálogo y errores por fila en `apps/frontend/src/app/pages/initiative-form/initiative-form.component.ts`, `.html` y `.scss` (depende de T007 y T008)
- [X] T010 [US1 FR-033] Introducir la lista precargada con las unidades confirmadas de la iniciativa de origen como valor inicial editable (Q1=A) en `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.ts`, `.html` y `.scss` (depende de T007 y T008)
- [X] T011 [US1 FR-001] Introducir la lista dinámica en el registro de proyectos preexistentes en `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.ts`, `.html` y `.scss` (depende de T007 y T008)
- [X] T012 [P] [US1 FR-001] Adecuar el repositorio mock a la lista ordenada de unidades en `apps/frontend/src/app/core/piip-mock.repository.ts`
- [X] T013 [P] [US1 FR-001 FR-003 FR-004 FR-005 FR-013 FR-033] Crear/actualizar las pruebas del componente de lista y de los tres formularios de alta (una y varias unidades, bloqueos de catálogo, renumeración, precarga del derivado) en `apps/frontend/src/app/shared/organizational-unit-list/organizational-unit-list.component.spec.ts`, `apps/frontend/src/app/pages/initiative-form/initiative-form.component.spec.ts`, `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.spec.ts` y `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.spec.ts` (depende de T007-T011)

**Checkpoint**: cada alta confirma una lista 1..N válida y el rechazo identifica fila y causa sin cambios parciales; ninguna validación ejecutada sin autorización.

## Phase 4: Frontend - US2 Edición de la lista

**Objetivo**: la edición permite agregar y retirar unidades con mínimo una, conservando atomicidad, concurrencia y el histórico intacto.

- [X] T014 [US2 FR-004 FR-005 FR-008 FR-024] Habilitar la edición completa del conjunto (retirar la reducción forzada `[ids[0]]`, agregar al final y retirar filas con mínimo una, renumeración, referencias históricas retenidas como contexto no reofrecibles) con el componente de lista en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.ts`, `.html` y `.scss` (depende de T007)
- [X] T015 [US2 FR-009] Enviar la lista en `updateInitiative` y `updateProject` solo cuando difiera del valor base (envío disperso vigente) en `apps/frontend/src/app/core/piip-http.repository.ts` (depende de T008)
- [X] T016 [P] [US2 FR-004 FR-005 FR-008 FR-009 FR-026] Actualizar las pruebas de edición (agregar/retirar/renumerar, mínimo una, conservación al editar campos ajenos, 409 conserva cambios locales) en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.spec.ts` (depende de T014 y T015)

**Checkpoint**: la edición del conjunto respeta mínimo una y atomicidad; sin cambios en estados editables ni control de versión.

## Phase 5: Frontend - US3 Revisión previa y detalle

**Objetivo**: revisión y detalle presentan todas las unidades en el orden confirmado.

- [X] T017 [P] [US3 FR-014 FR-015] Mostrar la lista ordenada (Nro/Descripción/Abreviatura) en las revisiones previas de alta en `apps/frontend/src/app/pages/initiative-form/initiative-review-dialog.component.ts` y `.html`, `apps/frontend/src/app/pages/derived-project-form/derived-project-review-dialog.component.ts` y `.html`, y en la sección REVISIÓN FINAL de `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.html` (depende de T008)
- [X] T018 [P] [US3 FR-014 FR-015] Mostrar la lista ordenada con denominación "Unidades Orgánicas Involucradas" y presentación adaptable en los detalles en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.ts` y `.html`, y `apps/frontend/src/app/pages/project-detail/project-detail.component.ts` y `.html` (depende de T008)
- [X] T019 [P] [US3 FR-014 FR-015] Actualizar las pruebas de revisión y detalle (orden confirmado, contenido completo, y disponibilidad de Nro/Descripción/Abreviatura con independencia de la presentación adaptable) en `apps/frontend/src/app/pages/derived-project-form/derived-project-review-dialog.component.spec.ts`, `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.spec.ts` y `apps/frontend/src/app/pages/project-detail/project-detail.component.spec.ts` (depende de T017 y T018)

**Checkpoint**: revisión y detalle reflejan el orden confirmado sin derivaciones alternativas.

## Phase 6: Backend y Frontend - US4 Auditoría de la lista

**Objetivo**: altas y modificaciones registran la lista con unidad, nombre, sigla y Nro.

- [x] T020 [US4 FR-018] Extender el detalle de los eventos de alta (`INICIATIVA_REGISTRADA`, `PROYECTO_DERIVADO_REGISTRADO`, `PROYECTO_PREEXISTENTE_REGISTRADO`) con la lista ordenada confirmada en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` y `portfolio/application/ProjectApplicationService.java` (depende de T002 y T003)
- [x] T021 [US4 FR-019 FR-020] Añadir sigla y Nro de presentación a cada elemento auditado del snapshot anterior/nuevo en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateAuditDetail.java` (depende de T003)
- [x] T022 [P] [US4 FR-018 FR-019 FR-020 FR-021] Actualizar las pruebas de auditoría (altas con lista ordenada, modificaciones con anterior/nuevo, operación rechazada sin registro de éxito, sin cuerpos ni datos sensibles) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/` (`PortfolioUpdateAuditTest` y pruebas de aplicación) (depende de T020 y T021)
- [X] T023 [US4 FR-014 FR-020] Presentar la lista ordenada con la denominación "Unidades Orgánicas Involucradas" en la auditoría visible (etiquetas y listas anterior/nuevo) en `apps/frontend/src/app/pages/audit/audit-event.presenter.ts` y `apps/frontend/src/app/pages/audit/audit.component.html` (depende de T008)

**Checkpoint**: la evidencia de auditoría conserva actor, fecha, Unidad Ejecutora y contexto, sin cuerpos de solicitudes.

## Phase 7: Backend y Frontend - US5 Compatibilidad histórica

**Objetivo**: los registros históricos con varias unidades siguen legibles y no se alteran al editar campos ajenos.

- [x] T024 [P] [US5 FR-022 FR-023 FR-024] Extender las pruebas de compatibilidad histórica (edición sin lista conserva el conjunto N; asociación inactiva retenida visible como contexto; nuevas incorporaciones validadas; sin migraciones ni renumeraciones automáticas) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/` (`PortfolioUpdateApplicationTest` y asociadas) (depende de T002 y T003)
- [X] T025 [US5 FR-022 FR-024] Mantener las asociaciones históricas (incluidas inactivas) visibles como contexto en detalle y edición sin reofrecerlas como nuevas selecciones en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.html`, `apps/frontend/src/app/pages/project-detail/project-detail.component.html` y `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.ts` (depende de T014 y T018)

**Checkpoint**: el histórico permanece intacto; las reglas de vigencia aplican solo a nuevas incorporaciones.

## Phase 8: Backend - US6 Datos sintéticos y postvalidación

**Objetivo**: la inicialización autorizada deja 4 UOs sintéticas válidas y falla seguro ante datos incompletos.

- [x] T026 [P] [US6 FR-027 FR-028 FR-030 FR-031] Extender la postvalidación del reset (por UO sintética: código, nombre y sigla no vacíos y asociación Unidad Orgánica-Unidad Ejecutora correcta; mínimo dos activas por UE sintética; conteos vigentes y tablas operativas vacías; fallo que declara la inicialización incompleta) sin tocar guardias fail-closed ni perfiles, en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` (depende de T002 solo por convención de fase; el seed `apps/backend/src/main/resources/db/test/catalog-data.sql` se reutiliza sin cambios)
- [x] T027 [P] [US6 FR-029 FR-030] Crear/actualizar las pruebas de la postvalidación extendida (conjunto válido pasa; sigla vacía, asociación incorrecta o faltantes fallan de forma segura; repetibilidad del conjunto sin duplicados) en `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/` (depende de T026)

**Checkpoint**: la validación posterior detecta faltantes, duplicados, asociaciones incorrectas o siglas vacías; los perfiles ordinarios no insertan estos datos.

## Phase 9: Documentación y cierre

- [x] T028 [FR-032 SC-008] Alinear la guía funcional (denominación "Unidades Orgánicas Involucradas", regla de lista 1..N en registro y edición, consulta del catálogo sin sigla vacía, derivado precargado, compatibilidad histórica) en `docs/funcional/guia-funcional-piip.md`
- [x] T029 [P] [FR-032 SC-008] Alinear la definición del campo en la matriz de campos (regla de lista ordenada y denominación visible, campo entre los 23) en `docs/architecture/piip-fields.md`
- [x] T030 [FR-001 FR-003 FR-011] Consolidar en `specs/017-unidades-organicas-involucradas/contracts/http-contract.md` los códigos de error fijados en T002 para lista vacía (400), duplicada (`DUPLICATED_UNIT`) y unidad sin sigla (`MISSING_ACRONYM`) (depende de T002)
- [x] T031 [US1-US6] Registrar tareas completadas, pendientes y validaciones realmente ejecutadas en `specs/017-unidades-organicas-involucradas/tasks.md`
- [x] T032 [US1-US6] Refrescar el índice estructural con `graphify update .` desde la raíz del monorepo tras los cambios de código (regla de AGENTS.md; no es validación de producto)

## Validaciones propuestas - requieren autorización

- [x] T033 [US1-US6 FR-001 FR-025 FR-031] Pruebas backend completas (incluye la autorización vigente sobre los endpoints modificados) desde `apps/backend` con `gradlew.bat test` - autorización requerida
- [x] T034 [US1 US2 US3 FR-001 FR-004 FR-005 FR-014] Pruebas frontend completas desde `apps/frontend` con `npm test -- --watch=false` - autorización requerida
- [x] T035 [US6 US1 SC-007 FR-027 FR-028 FR-029 FR-030] Integración Oracle del reset con los perfiles `test,test-reset` desde `apps/backend` con `gradlew.bat integrationTest`, incluyendo confirmar el registro de una iniciativa o proyecto con dos Unidades Orgánicas distintas de la misma Unidad Ejecutora sobre los datos sintéticos (SC-007) (requiere Docker o variables Oracle) - autorización requerida

## Dependencias y orden de ejecución

- **Propietario canónico**: backend define contrato y reglas (T001-T005, T020-T022, T026-T027); frontend y documentación son consumidores.
- **Consumidores**: T006-T019 y T023-T025 (frontend) dependen del contrato publicado (T005-T006); T028-T030 dependen del comportamiento consolidado.
- **Orden obligatorio**: T001→T002→T003→T005→T006 (contrato); T002→T036 (pruebas de validación); T007+T008→T009/T010/T011 (formularios con el componente de lista), T008→T015/T017/T018/T023 (consumidores del cliente); T003→T021, T002+T003→T020/T022/T024; fases US3/US4/US5 tras US1/US2 cuando comparten archivos (`piip-http.repository.ts`, `portfolio-record-edit.component.ts`); US6 y documentación pueden avanzar en paralelo tras Phase 2 sin compartir contrato.
- **Oportunidades paralelas**: T004 (árbol `organization/**`), T012, T013/T016/T019/T022/T024/T027/T036 (pruebas sobre fuentes terminadas), T017/T018 (archivos distintos), T026/T027 (`config/reset/**`), T028/T029 (documentos distintos).

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- No crear tareas a partir de pendientes históricos por arrastre.
- Si la feature depende directamente de un requisito histórico, registrar aquí el requisito, la dependencia y su aprobación explícita: `specs/013-actualizar-registros-portafolio` — esta feature supersede exclusivamente su cardinalidad exactamente-una del campo (clarificación 2026-08-22, FR-014, FR-022A) por decisión aprobada del solicitante; sus demás reglas permanecen vigentes. `specs/015-inicializacion-oracle` — se reutilizan su seed sintético y su coordinador de reset sin alterar guardias ni perfiles exactos `test,test-reset`; la divergencia menor 19/20 tablas permanece registrada sin acción.
- Registrar contradicciones sin resolver como `NEEDS CLARIFICATION`: Ninguna.

## Notas

- Cada tarea representa trabajo nuevo aprobado y apunta a una ruta real; el componente de lista (`apps/frontend/src/app/shared/organizational-unit-list/`) es la única ruta nueva del frontend y la postvalidación se extiende dentro de la clase existente del reset.
- No reimplementar capacidades incluidas en la evidencia de baseline (esquema N ordenado, atomicidad de `replace`, lectura histórica, guardias del reset, control de versión).
- No paralelizar cambios que compartan contrato, catálogo, regla funcional, documentación o configuración.
- El cierre distingue cambios realizados, tareas pendientes y validaciones no ejecutadas; MVP sugerido: Phase 2 + Phase 3 (US1) para entregar el registro con lista múltiple de extremo a extremo.
- La ejecución de `OpenApiGenerationTest` (T005) y `npm run api:generate` (T006) son pasos del flujo contractual del monorepo y requieren autorización explícita en el turno en que se ejecuten.

### Cierre de implementación 2026-09-07

- Implementadas: T001-T032 y T036. T005 publicó el OpenAPI y T006 sincronizó el cliente generado con autorización explícita; T032 actualizó el índice estructural y la semántica documental conforme a `AGENTS.md`.
- Validaciones ejecutadas con autorización: `OpenApiGenerationTest` (publicación contractual), `npx tsc --noEmit -p tsconfig.spec.json`, `git diff --check -- apps/frontend`, T033 (`gradlew.bat test`), T034 (`npm test -- --watch=false`, 41 archivos y 262 pruebas aprobadas tras corregir regresiones) y T035 (`gradlew.bat integrationTest` contra Oracle local).
- Graphify: índice actualizado con extracción semántica documental; el diagnóstico no encontró endpoints faltantes ni aristas colapsadas, aunque reportó cuatro self-loops preexistentes.
