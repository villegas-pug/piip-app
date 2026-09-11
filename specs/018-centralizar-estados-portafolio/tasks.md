---
description: "Lista trazable de tareas para una feature nueva del monorepo PIIP"
---

# Tareas: Centralización de estados del portafolio

**Entrada**: documentos de `/specs/018-centralizar-estados-portafolio/`

**Prerrequisitos**: `spec.md`, `plan.md` y `tasks.md` vigentes; documentos de diseño adicionales cuando existan; sin checklists ni `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI ni integración Oracle. La invocación explícita de `/speckit-implement` autoriza las tareas de implementación de la feature activa y aprueba sus artefactos vigentes; las demás acciones requieren autorización separada.

## Formato obligatorio

Cada tarea usa:

`- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta/archivo exacto`

- **[P]**: puede ejecutarse en paralelo porque no comparte archivos ni una dependencia pendiente.
- **[US#] / [FR-###]**: toda tarea se vincula con al menos una historia o requisito de la spec.
- La descripción incluye una ruta real del monorepo; no se aceptan ubicaciones genéricas.
- `[X]` se utiliza solo después de obtener evidencia de que el cambio fue realizado.
- Toda excepción constitucional para DML inicial o reset destructivo de auditoría incluye tareas explícitas de guardias fail-closed, prohibición productiva, allowlist y verificación del alcance eliminado.

## Evidencia de baseline - no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| FR-014 / FR-021 | Matrices exactas de iniciativa y proyecto implementadas en el dominio | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` (`approve`, `transitionInitiativeTo`, `transitionProjectTo`) | Mantener sin cambios; nunca derivarlas del catálogo |
| FR-016 | Estados iniciales asignados en factories (`PRESENTED`, `PROJECT_IN_PROGRESS`) | `PortfolioRecordEntity.java` | Mantener la asignación; añadir validación persistente (T015) |
| US2 | `eligibleInitiatives()` selecciona por código enum `INITIATIVE_APPROVED` | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioQueryService.java` | Mantener (decisión D10: inactividad del estado no bloquea la derivación) |
| FR-023 | Guardias fail-closed de activación exacta y ordenada `test,test-reset` antes de JPA | `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetEnvironmentGuard.java`, `TestResetStartupGuard.java` | Mantener y verificar cobertura de la nueva tabla (T037) |
| FR-028 | `dev`/`prod` con `ddl-auto=validate` y sin seed | `apps/backend/src/main/resources/application-dev.yml`, `application-prod.yml`, `apps/backend/src/test/java/pe/gob/midagri/piip/config/ProfileConfigurationTest.java` | Mantener (T038) |
| FR-011 | Store de catálogos con fases `idle/loading/ready/error` y valor previo en carga/error | `apps/frontend/src/app/core/piip-catalogs.store.ts` | Mantener; los consumidores gatean acciones en `ready` (T041+) |
| FR-018 | Patrón vigente de históricos inactivos con denominación vigente y marca "Inactivo" | `apps/frontend/src/app/pages/initiatives/initiative-detail.component.html`, `apps/frontend/src/app/pages/project-detail/project-detail.component.html` | Reutilizar el patrón con la metadata del registro (T052/T053) |
| FR-021 | Matrices frontend como reglas independientes | `apps/frontend/src/app/core/piip.catalogs.ts` | Reindexar por código (T042), no eliminar |
| FR-010 | Requests de transición y filtro del dashboard ya envían códigos técnicos | `apps/frontend/src/app/api/generated/fn/`, `apps/frontend/src/app/core/piip-http.repository.ts` | Mantener; eliminar el mapa inverso etiqueta→código (T043) |

## Phase 1: Preparación necesaria

**Propósito**: fundamento JPA compartido por todas las historias (tabla, entidad, repositorio y asociación).

- [X] T001 [FR-001] [FR-002] Crear el enum de aplicabilidad `INITIATIVE`/`PROJECT`/`NONE` en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/domain/PortfolioStatusApplicability.java`
- [X] T002 [FR-001] [FR-003] Crear la entidad `PortfolioStatusCatalogEntity` (tabla `ESTADO_PORTAFOLIO`, PK natural `CODIGO` tipada `PortfolioStatus`, `NOMBRE`, `ORDEN_PRESENTACION >= 0`, `ACTIVO`, `APLICABILIDAD`, índice `(ACTIVO, APLICABILIDAD, ORDEN_PRESENTACION, CODIGO)`, sin setter de código) en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioStatusCatalogEntity.java` (depende de T001)
- [X] T003 [FR-008] Crear `PortfolioStatusRepository` con `findByCode`, `findAllByActiveTrueOrderByDisplayOrderAscCodeAsc` y `findAllByOrderByDisplayOrderAscCodeAsc` en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioStatusRepository.java` (depende de T002)
- [X] T004 [FR-003] [FR-019] Añadir la asociación read-only `statusCatalog` (`insertable=false, updatable=false`, join `ESTADO` → `ESTADO_PORTAFOLIO.CODIGO`) en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` e incluir `statusCatalog` en los EntityGraph de `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordRepository.java` (depende de T002)

---

## Phase 2: Contrato o fundamento bloqueante

**Propósito**: definir el contrato y la validación canónica del backend antes de tocar consumidores.

- [X] T005 [P] [US1] [FR-002] [FR-004] [FR-008] [FR-009] Añadir `PortfolioStatusCatalogResponse(code, name, displayOrder, active, applicability)` y el campo `portfolioStatuses` al bundle en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/api/CatalogDtos.java`
- [X] T006 [P] [US2] [FR-010] [FR-015] Añadir `PortfolioStatusReferenceResponse(code, name, active)`, cambiar `status` de las respuestas de registro a referencia y `targetStatus` de los requests de transición a `String` con valores permitidos, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioDtos.java`
- [X] T007 [P] [US2] [FR-015] Añadir los cuatro `ProblemCode` 422 (`PORTFOLIO_STATUS_NOT_FOUND`, `PORTFOLIO_STATUS_INACTIVE`, `PORTFOLIO_STATUS_NOT_APPLICABLE`, `PORTFOLIO_STATUS_TRANSITION_NOT_ALLOWED`) en `apps/backend/src/main/java/pe/gob/midagri/piip/shared/application/error/ProblemCode.java`
- [X] T008 [US2] [FR-013] Crear el servicio de aplicación que resuelve un estado por código y valida el destino en orden existencia → actividad → aplicabilidad con causas distinguibles, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusValidationService.java` (depende de T003 y T007)

**Checkpoint**: el contrato (T005-T007) y la validación canónica (T008) quedan definidos antes de modificar consumidores backend o frontend.

---

## Phase 3: Backend - US1 (catálogo único consultable)

**Objetivo**: la consulta central expone los once estados activos con sus cinco atributos, ordenados y sin identificadores internos.

- [X] T009 [US1] [FR-008] Incluir `portfolioStatuses` (solo activos, orden `displayOrder` asc y `code` asc) en el bundle de `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/application/CatalogQueryService.java` (depende de T003 y T005)
- [X] T010 [US1] [FR-001] [FR-005] Crear la prueba de persistencia del catálogo (PK natural única por código, once códigos con atributos, orden) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioStatusCatalogPersistenceTest.java` (depende de T002 y T004)
- [X] T011 [US1] [FR-006] [FR-007] Crear la prueba de disponibilidad (inactivo fuera del bundle, renombrado conserva identidad, orden persistente) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusCatalogAvailabilityTest.java` (depende de T009)
- [X] T012 [US1] [FR-008] [FR-009] Actualizar el contrato publicado del bundle con `portfolioStatuses` en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/CatalogContractTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/catalogs/api/CatalogControllerTest.java` (depende de T009)
- [X] T013 [US1] [FR-027] Adaptar el modelo JPA persistido (nueva tabla y FK por código natural) en `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/JpaModelTest.java` (depende de T004)

**Checkpoint**: `GET /catalogs` expone `portfolioStatuses` activo/ordenado con identidad por código, con pruebas de persistencia y contrato actualizadas (sin ejecutarlas aún).

---

## Phase 4: Backend - US2 (estados coherentes y validaciones)

**Objetivo**: lecturas, asignaciones, transiciones, dashboard, documentos y auditoría backend resuelven todo por código con causas distinguibles.

- [X] T014 [US2] [FR-011] Restringir `parseStatus` a aceptar exclusivamente el código (sin etiquetas ni `values()` como inventario) en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioApplicationSupport.java`
- [X] T015 [US2] [FR-016] Validar con `PortfolioStatusValidationService` los estados iniciales en creación (iniciativa `PRESENTED`, proyecto derivado/preexistente `PROJECT_IN_PROGRESS`) y la aprobación (`INITIATIVE_APPROVED`) en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ProjectApplicationService.java` (depende de T008)
- [X] T016 [US2] [FR-013] [FR-014] [FR-015] [FR-017] Validar el destino de las transiciones en orden existencia → actividad → aplicabilidad → matriz con los cuatro `ProblemCode`, rechazando `NOT_APPLICABLE` por aplicabilidad y sin rechazar el origen inactivo, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioApplicationSupport.java` (depende de T008 y T014)
- [X] T017 [US2] [FR-010] Cambiar `status` a referencia estructurada (código, denominación vigente, actividad) en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioReadModelAssembler.java` (depende de T006)
- [X] T018 [US2] [FR-010] Cambiar `status` de los ítems a referencia, `PortfolioStatusCountResponse` a `{status, count}` y `portfolioByStatus` a lista `portfolioStatusCounts` en `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/api/DashboardDtos.java` (depende de T006)
- [X] T019 [US2] [FR-010] Sustituir el map etiqueta→conteo por la lista estructurada por código en `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/application/DashboardSummaryService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/application/DashboardSummaryReadModel.java` (depende de T018)
- [X] T020 [US2] [FR-010] Resolver inventario y orden desde la metadata del catálogo (sin `values()`/`label()`) y cargar la asociación sin N+1 en `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/application/DashboardPortfolioService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/persistence/DashboardPortfolioQueryRepository.java` (depende de T018)
- [X] T021 [US2] [FR-010] Cambiar `DossierSummary.status` a referencia estructurada en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentInboxService.java` (depende de T006)
- [X] T022 [US2] [US3] [FR-010] [FR-019] Persistir códigos estables (`statusCode`, `previousStatusCode`, `newStatusCode`) en los eventos nuevos de alta y transición, conservando el detalle técnico, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioApplicationSupport.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` (depende de T016)
- [X] T023 [US2] [FR-010] Enriquecer la lectura de auditoría con referencias opcionales `status`/`previousStatus`/`newStatus` (metadata vigente; legados sin código como texto histórico) en `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditQueryService.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditReadModels.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/audit/api/AuditController.java` (depende de T022)
- [X] T024 [US2] [FR-013] [FR-014] [FR-015] [FR-017] Ampliar la prueba parametrizada exhaustiva de matrices (pares permitidos/prohibidos), causas distinguibles y rechazo de `NOT_APPLICABLE` por aplicabilidad en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioTransitionTest.java` (depende de T016)
- [X] T025 [US2] [FR-016] Adaptar las pruebas de estados iniciales y aprobación en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioInitiativeStatusServiceTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioProjectStatusServiceTest.java` (depende de T015)
- [X] T026 [US2] [FR-010] [FR-019] Adaptar las pruebas de concurrencia y auditoría (códigos estables, rollback conjunto estado/auditoría) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusConcurrencyTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusAuditTest.java` (depende de T016 y T022)
- [X] T027 [US2] [FR-010] Adaptar las pruebas de dashboard (conteos por código, orden por catálogo, inactivos con conteo) en `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/application/DashboardPortfolioServiceTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/application/DashboardSummaryServiceTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/persistence/DashboardPortfolioQueryRepositoryTest.java` (depende de T019 y T020)
- [X] T028 [US2] [FR-010] [FR-011] Adaptar las pruebas de bandeja documental y del filtro por código (incluye inactivos consultables por llamada directa) en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentInboxServiceTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioQueryServiceTest.java` (depende de T014 y T021)
- [X] T029 [US2] [FR-015] Adaptar las pruebas de contrato HTTP de transiciones y respuestas (`targetStatus` String, causas 422, `status` referencia) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioControllerStatusTransitionTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioDtosValidationTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioControllerContractTest.java` (depende de T006 y T016)

**Checkpoint**: consumidores backend resueltos por código con referencias estructuradas y causas distinguibles; matrices intactas; auditoría nueva solo agrega códigos.

---

## Phase 5: Backend - US3 (históricos y reglas)

**Objetivo**: los históricos inactivos permanecen legibles con metadata vigente y ninguna metadata del catálogo altera reglas.

- [X] T030 [US3] [FR-018] Ampliar la prueba de metadata histórica (estado inactivo legible con `{code, name, active}` en la respuesta del registro y ausente del bundle) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioCatalogQueryTest.java` (depende de T017)
- [X] T031 [US3] [FR-019] [FR-020] [FR-021] Crear la prueba de integridad de metadata (cambio de actividad/orden/aplicabilidad no reasigna registros ni habilita transiciones; eliminación de estado referenciado rechazada por la FK) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusMetadataIntegrityTest.java` (depende de T010 y T024)
- [X] T032 [US3] [FR-020] Ampliar la prueba de origen inactivo (puede salir hacia destino válido por matriz; rechazos por causa cuando el destino es inválido) en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioTransitionTest.java` (depende de T024)

**Checkpoint**: históricos preservados por código, sin reescritura ni reasignación, y matrices separadas de la metadata.

---

## Phase 6: Datos - US4 (inicialización descartable)

**Objetivo**: la carga de prueba incorpora exactamente los once estados en ambos artefactos, con guardias fail-closed y postvalidación, bajo `test,test-reset`.

- [X] T033 [US4] [FR-022] [FR-024] [FR-027] Añadir `ESTADO_PORTAFOLIO` a la allowlist y a los órdenes de creación/eliminación (drop después de `REGISTRO_PORTAFOLIO`, create antes) en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetSchemaFilterProvider.java` (depende de T002)
- [X] T034 [US4] [FR-025] [FR-026] Ampliar el coordinador a 21 tablas y postvalidar exactamente los once códigos con nombre/orden/actividad/aplicabilidad y ausencia de extras, con fallo cerrado ante discrepancias, en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` (depende de T033)
- [X] T035 [US4] [FR-022] [FR-023] [FR-024] Añadir los once MERGE por código natural con solo `WHEN NOT MATCHED INSERT` (sin UPDATE corrector) y actualizar cabecera/conteos en `apps/backend/src/main/resources/db/test/catalog-data.sql` (depende de T033)
- [X] T036 [US4] [FR-022] [FR-024] Reflejar el mismo inventario en el espejo externo coordinado `database/dml/seed/catalog-data.sql` (depende de T035)
- [X] T037 [US4] [FR-023] [FR-028] Confirmar y, si aplica, extender las guardias fail-closed de la activación exacta y ordenada `test,test-reset` y la prohibición productiva para el DML de estados en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetEnvironmentGuard.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetStartupGuard.java` (depende de T033)
- [X] T038 [US4] [FR-028] Verificar que `dev`/`prod` mantienen `ddl-auto=validate` y no cargan seed en `apps/backend/src/test/java/pe/gob/midagri/piip/config/ProfileConfigurationTest.java` (depende de T037)
- [X] T039 [US4] [FR-025] [FR-026] Adaptar las pruebas de política de seed, esquema y postvalidación (exactitud, idempotencia, discrepancia falla cerrada, perfiles) en `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/CatalogSeedPolicyTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetPostValidationTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetSchemaFilterTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetCoordinatorTest.java` (depende de T034 y T035)
- [X] T040 [US4] [FR-027] Regenerar el DDL revisable desde el artefacto del build en `database/generated/piip-oracle.sql` (fuente: `apps/backend/target/piip-oracle.sql`; requiere la generación autorizada vía `OracleSchemaGenerationTest`) (depende de T004)

**Checkpoint**: ambos seeds coordinados con once MERGE insert-only, reset en 21 tablas con postvalidación fail-closed y guardias/perfiles intactos; JPA sigue canónico.

---

## Phase 7: Frontend - US1 (fundamento del contrato)

**Objetivo**: el frontend consume `portfolioStatuses` y referencias del cliente regenerado, sin inventarios locales.

**Habilitación previa**: requiere la publicación del contrato y la regeneración autorizada del cliente (T069).

- [X] T041 [US1] [FR-003] [FR-004] [FR-010] Redefinir `PiipStatus` como unión de los once códigos y añadir `PortfolioStatusOption` (cinco atributos, sin `id`), `PortfolioStatusReference` y `portfolioStatuses` en el bundle, con registros/dashboard/documentos usando referencias, en `apps/frontend/src/app/core/piip.models.ts` (depende de T069)
- [X] T042 [US1] [US2] [FR-011] [FR-021] Eliminar `PIIP_CATALOGS.statuses`, `INITIATIVE_STATUSES` y `PROJECT_STATUSES`, y reindexar `INITIATIVE_STATUS_TRANSITIONS`/`PROJECT_STATUS_TRANSITIONS` por código, en `apps/frontend/src/app/core/piip.catalogs.ts` (depende de T041)
- [X] T043 [US1] [FR-010] [FR-011] Eliminar el mapa `portfolioStatusCode` y los casts de respuesta; mapear opciones/referencias del bundle y enviar códigos en filtros y transiciones, en `apps/frontend/src/app/core/piip-http.repository.ts` (depende de T041)
- [X] T044 [US1] [FR-011] Alinear el mock al contrato (bundle con estados, referencias estructuradas, matrices por código) sin usarlo como fallback productivo en `apps/frontend/src/app/core/piip-mock.repository.ts` (depende de T041 y T042)

**Checkpoint**: el dominio frontend usa solo códigos, opciones del bundle y referencias; matrices por código como reglas independientes.

---

## Phase 8: Frontend - US2/US3 (consumidores)

**Objetivo**: formularios, filtros, listas, selectores, detalles, indicadores, dashboard, documentos, auditoría y visuales resuelven por código; históricos inactivos legibles con metadata del registro.

- [X] T045 [US2] [FR-010] [FR-016] Resolver la denominación del estado inicial `PRESENTED` por código desde el catálogo y bloquear revisión/registro si el catálogo no está `ready` o el código falta/inactivo/no aplicable, en `apps/frontend/src/app/pages/initiative-form/initiative-form.component.ts`, `initiative-form.component.html` y `apps/frontend/src/app/pages/initiative-form/initiative-review-dialog.component.html` (depende de T041 y T043)
- [X] T046 [P] [US2] [FR-010] [FR-016] Hacer lo propio con `PROJECT_IN_PROGRESS` y la etiqueta de iniciativa elegible en `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.ts`, `derived-project-form.component.html`, `apps/frontend/src/app/pages/derived-project-form/derived-project-review-dialog.component.html` y `apps/frontend/src/app/pages/projects/project-registration-dialog.component.html` (depende de T041 y T043)
- [X] T047 [P] [US2] [FR-010] [FR-016] Hacer lo propio con `PROJECT_IN_PROGRESS` en el formulario preexistente en `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.ts` y `preexisting-project-form.component.html` (depende de T041 y T043)
- [X] T048 [P] [US2] [FR-010] [FR-011] Ofrecer opciones por aplicabilidad desde el bundle, comparar por código y presentar denominaciones vigentes en `apps/frontend/src/app/pages/initiatives/initiatives.component.ts` y `initiatives.component.html` (depende de T042 y T043)
- [X] T049 [P] [US2] [FR-010] [FR-011] Hacer lo propio en el listado de proyectos con sus cinco indicadores en `apps/frontend/src/app/pages/projects/projects.component.ts` y `projects.component.html` (depende de T042 y T043)
- [X] T050 [P] [US2] [FR-010] [FR-011] Hacer lo propio en la bandeja documental (filtro global por código) en `apps/frontend/src/app/pages/documents-inbox/documents-inbox.component.ts` y `documents-inbox.component.html` (depende de T042 y T043)
- [X] T051 [US2] [FR-010] Ofrecer opciones por aplicabilidad, filtrar por código y presentar filas/distribución desde referencias y conteos estructurados en `apps/frontend/src/app/pages/dashboard/dashboard.component.ts` y `dashboard.component.html` (depende de T043)
- [X] T052 [P] [US2] [US3] [FR-010] [FR-018] [FR-020] Calcular destinos como matriz por código ∩ activos ∩ aplicables, evaluar el origen inactivo por el código del registro y mostrar el histórico inactivo con la metadata del registro y marca "Inactivo", en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.ts`, `initiative-detail.component.html` y `apps/frontend/src/app/pages/initiative-detail/initiative-status-transition-dialog.component.ts` (depende de T042 y T043)
- [X] T053 [P] [US2] [US3] [FR-010] [FR-018] [FR-020] Hacer lo propio en el detalle de proyecto y su diálogo de transición en `apps/frontend/src/app/pages/project-detail/project-detail.component.ts`, `project-detail.component.html` y `apps/frontend/src/app/pages/project-detail/project-status-transition-dialog.component.ts` (depende de T042 y T043)
- [X] T054 [US2] [FR-010] Clasificar visualmente por código y mostrar la denominación desde catálogo o referencia del registro en `apps/frontend/src/app/pages/documents/documents.component.ts` (depende de T043)
- [X] T055 [US2] [US3] [FR-010] Presentar la auditoría con referencias estructuradas (códigos y denominaciones vigentes) y eventos legados como texto histórico en `apps/frontend/src/app/pages/audit/audit-event.presenter.ts`, `apps/frontend/src/app/pages/audit/audit.component.html` y `apps/frontend/src/app/pages/audit/audit-event-detail-dialog.component.ts` (depende de T043)
- [X] T056 [P] [US2] [FR-010] Decidir permisos de edición comparando códigos en `apps/frontend/src/app/core/portfolio-edit-permissions.ts` (depende de T041)
- [X] T057 [P] [US1] [US2] [FR-012] Reindexar los mapas visuales por código con presentación neutral para códigos desconocidos en `apps/frontend/src/app/pages/initiatives/initiative-status-visual.ts` y `apps/frontend/src/app/pages/projects/project-status-visual.ts` (depende de T041)
- [X] T058 [P] [US1] [US2] Actualizar las specs de núcleo (bundle con estados, sin inventarios ni fallbacks, matrices por código, permisos por código) en `apps/frontend/src/app/core/piip-http.repository.spec.ts`, `apps/frontend/src/app/core/piip-catalogs.store.spec.ts`, `apps/frontend/src/app/core/piip-domain.spec.ts` y `apps/frontend/src/app/core/portfolio-edit-permissions.spec.ts` (depende de T043)
- [X] T059 [P] [US2] Actualizar las specs de páginas (formularios con gate `ready`, filtros/listas por código, dashboard y bandeja estructurados, `NOT_APPLICABLE` nunca ofrecido, visual neutral) en `apps/frontend/src/app/pages/initiative-form/initiative-form.component.spec.ts`, `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.spec.ts`, `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.spec.ts`, `apps/frontend/src/app/pages/initiatives/initiatives.component.spec.ts`, `apps/frontend/src/app/pages/projects/projects.component.spec.ts`, `apps/frontend/src/app/pages/dashboard/dashboard.component.spec.ts` y `apps/frontend/src/app/pages/documents-inbox/documents-inbox.component.spec.ts` (depende de T045-T051)
- [X] T060 [P] [US2] [US3] Actualizar las specs de detalles, diálogos, edición y auditoría (destinos matriz ∩ activos ∩ aplicables, histórico inactivo legible, origen inactivo, auditoría estructurada, renombrado propagado) en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.spec.ts`, `apps/frontend/src/app/pages/project-detail/project-detail.component.spec.ts`, `apps/frontend/src/app/pages/initiative-detail/initiative-status-transition-dialog.component.spec.ts`, `apps/frontend/src/app/pages/project-detail/project-status-transition-dialog.component.spec.ts`, `apps/frontend/src/app/pages/audit/audit-event.presenter.spec.ts` y `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.spec.ts` (depende de T052-T055)

**Checkpoint**: cero inventarios funcionales locales; asociaciones visuales por código con neutral; históricos inactivos presentados con metadata del registro.

---

## Phase 9: Documentación y cierre

- [X] T061 [P] [FR-030] Documentar la fuente única de los estados del portafolio (catálogo persistente, identidad por código, actividad/aplicabilidad) en `docs/architecture/piip-fields.md`
- [X] T062 [P] [FR-030] Documentar `ESTADO_PORTAFOLIO`, la FK por código natural y el conteo 21 tablas en `docs/architecture/data-model-final.md` (y `docs/architecture/data-model-comparison.md` si aplica)
- [X] T063 [P] [FR-028] [FR-029] [FR-030] Actualizar la guía funcional (dashboard, formularios, transiciones, documentos y auditoría con fuente única; preparación productiva incompleta) en `docs/funcional/guia-funcional-piip.md`
- [X] T064 [P] [FR-030] Actualizar el conteo de tablas (21), el catálogo de estados (once) y la postvalidación en `docs/development/test-catalog-reset.md`
- [X] T065 [P] [FR-028] [FR-030] Dejar constancia de `validate` sin seed en `dev`/`prod` y de la provisión institucional pendiente (feature posterior) en `docs/deployment/institutional-development.md`
- [X] T066 [FR-030] Registrar tareas completadas, pendientes y validaciones realmente ejecutadas en `specs/018-centralizar-estados-portafolio/tasks.md`

## Validaciones propuestas - requieren autorización

- [X] T067 [US1] [US4] [FR-027] Pruebas backend completas desde `apps/backend` con `gradlew.bat test` (incluye `OpenApiGenerationTest`, que produce `apps/backend/target/piip-openapi.json`) - autorización requerida
- [X] T068 [US1] [US4] Verificación backend completa desde `apps/backend` con `gradlew.bat check` - autorización requerida
- [X] T069 [US1] [FR-008] [FR-009] Publicar el contrato y regenerar el cliente: confirmar `apps/backend/target/piip-openapi.json` y ejecutar `npm run api:generate` desde `apps/frontend` (habilita las fases 7 y 8) - autorización requerida
- [X] T070 [US2] Pruebas frontend desde `apps/frontend` con `npm test -- --watch=false` - autorización requerida
- [X] T071 [US2] Build frontend desde `apps/frontend` con `npm run build` - autorización requerida
- [X] T072 [US4] Integración Oracle desde `apps/backend` con `gradlew.bat integrationTest` (requiere Docker o variables Oracle) - autorización requerida
- [X] T073 [US4] [FR-022] [FR-026] Inicialización descartable: arranque con la activación exacta y ordenada `test,test-reset` tras una reconstrucción descartable, verificando once códigos y reejecución sin duplicados (perfil destructivo exclusivo de pruebas) - autorización requerida

## Dependencias y orden de ejecución

- **Propietario canónico**: backend (T001-T040) define esquema, reglas y contrato; `database/dml/seed/catalog-data.sql` (T036) es espejo coordinado del seed backend (T035).
- **Consumidores**: cliente Angular generado y consumidores frontend (T041-T060), bloqueados hasta la publicación autorizada del contrato (T069); documentación (T061-T065) tras el contrato cerrado.
- **Orden obligatorio**: T001 → T002 → T003/T004 → T008; T009-T013 (US1) → T014-T029 (US2) → T030-T032 (US3); T033 → T034/T035 → T036; T016 → T022 → T023; T069 (autorizada) → T041 → T042/T043 → T044-T057 → T058-T060; T061-T066 al cierre. `implement` no autoriza por sí mismo OpenAPI, builds, Oracle ni Git.
- **Oportunidades paralelas**: T005/T006/T007 (DTO y errores, archivos distintos); T010/T013 tras T004; T018/T021 tras T006 y T019/T020 tras T018; formularios y listados frontend T046/T047/T048/T049/T050/T052/T053/T056/T057 (archivos distintos); specs T058/T059/T060; documentación T061-T065.
- **No paralelizar**: seeds T035→T036 (coordinación del mismo inventario); T018→T019/T020 (comparten DTO); T024→T032 (mismo archivo de prueba); T041→T042/T043 (modelos compartidos).

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- No crear tareas a partir de pendientes históricos por arrastre.
- Si la feature depende directamente de un requisito histórico, registrar aquí el requisito, la dependencia y su aprobación explícita: 009 (matrices ratificadas por el principio II de la Constitución), 011 (patrón de consulta central y catálogos persistentes), 015 (activación exacta `test,test-reset` y DML-only), 016 (baseline de 20 tablas) - grounding sin reapertura.
- Registrar contradicciones sin resolver como `NEEDS CLARIFICATION`: Ninguna.

## Notas

- Cada tarea representa trabajo nuevo aprobado y apunta a una ruta real.
- No reimplementar capacidades incluidas en la evidencia de baseline.
- No paralelizar cambios que compartan contrato, catálogo, regla funcional, documentación o configuración.
- El cierre distingue cambios realizados, tareas pendientes y validaciones no ejecutadas.
- Las fases 7 y 8 quedan bloqueadas hasta la publicación autorizada del contrato (T069); T040 y T073 dependen de generaciones o perfiles que exigen autorización expresa en el turno vigente.

## Cierre de implementación

- T001-T069 y T071 se completaron; se implementaron el catálogo persistente, el contrato, los consumidores, los seeds coordinados, el DDL revisable y la documentación funcional y técnica.
- T067 (`gradlew.bat test`) y T068 (`gradlew.bat check`) finalizaron correctamente. T069 publicó el contrato y regeneró el cliente. T040 regeneró el DDL mediante `OracleSchemaGenerationTest`; el artefacto generado y el revisable tienen el mismo SHA-256.
- T070 se completó después de configurar el runner Vitest para no ejecutar archivos en paralelo (`apps/frontend/angular.json` y `apps/frontend/vitest-base.config.ts`): 41/41 archivos y 283/283 tests pasaron.
- T071 (`npm run build`) finalizó correctamente con advertencias no bloqueantes de presupuesto para el bundle inicial y dos hojas SCSS.
- T072 se completó después de ejecutar el backend con los perfiles exactos `test,test-reset` y reconstruir el esquema descartable: `gradlew.bat integrationTest` finalizó con `BUILD SUCCESSFUL`.
- T073 se completó dentro de T072 y se confirmó de forma aislada con `integrationTest --tests pe.gob.midagri.piip.persistence.TestResetOracleIntegrationTest`: BUILD SUCCESSFUL, 2 pruebas; el reset `test,test-reset` se ejecutó dos veces, validó el dataset y la idempotencia sin duplicados.
