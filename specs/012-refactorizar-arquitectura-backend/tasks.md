# Tasks: Refactorizar la arquitectura interna del backend PIIP

**Input**: artefactos de diseño en `specs/012-refactorizar-arquitectura-backend/`  
**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/http-compatibility.md`, `quickstart.md`

**Autorización**: este archivo descompone la implementación futura. Su generación no autoriza modificar producto. La ejecución requiere una invocación posterior y explícita de `/speckit-implement`; pruebas, builds, servidores, OpenAPI, Oracle y Git requieren además autorización explícita en el turno correspondiente.

**Tests**: la especificación exige caracterización, regresión, arquitectura, autorización, atomicidad, auditoría y concurrencia. Por ello se incluyen tareas para escribir o adaptar pruebas antes del movimiento correspondiente. Las tareas de la fase final que ejecutan comandos permanecen bloqueadas hasta recibir autorización.

**Organization**: las fases se agrupan por historia de usuario, pero las historias P1 se ordenan por las dependencias arquitectónicas confirmadas: US1 → fronteras compartidas → US4 → US3 → US2. Dentro de US2 se respeta `documents` → `work/notifications` → `dashboard` → `portfolio`.

## Baseline confirmado (no ejecutable)

| Evidencia existente | Estado | Uso en esta feature |
|---|---|---|
| Clasificación de los 10 módulos productivos en `spec.md` | Baseline existente | Limita el trabajo a desviaciones confirmadas y decisiones aprobadas. |
| `PersistencePolicyTest`, `JsonProducesMappingTest`, `HttpParameterBindingTest`, `ExecutingUnitFilterMappingTest` | Baseline existente | Se preservan y se complementan; no se marcan como trabajo nuevo. |
| Pruebas MVC y de aplicación actuales de audit, dashboard, documents, identity, portfolio y work | Baseline existente | Se amplían antes de mover cada comportamiento; no se asumen verdes sin ejecución autorizada. |
| `CatalogController`, `CatalogQueryService`, `CatalogReferenceService`, `DashboardPortfolioService`, `config/**` | Estructura conforme/protegida | No originan refactorización salvo imports mínimos derivados de `shared`. |
| Frontend, cliente generado, OpenAPI, JPA, DDL, Oracle y guía funcional | Alcance protegido | Deben permanecer sin cambios; la guía funcional no cambia si se conserva equivalencia observable. |

---

## Phase 1: Setup y trazabilidad reversible

**Purpose**: preparar un registro verificable por incremento sin mover código productivo.

- [X] T001 [FR-001] [FR-002] [FR-034] Crear `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md` con la clasificación canónica de módulos, baseline por endpoint, riesgo, dependencias, propietario, criterio de reversión, archivos protegidos y campos de verificación/no verificación para los incrementos 0-8.
- [X] T002 [FR-005] [FR-006] [FR-030] [FR-038] Registrar en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md` el commit y diff base, junto con la lista exacta de árboles protegidos (`apps/frontend/**`, OpenAPI/cliente generado, entidades JPA, `database/**`, configuración Oracle y guía funcional) que deberán conservar cero cambios.

**Checkpoint**: el formato de evidencia permite detener y revertir un incremento sin interpretar un checklist completado como prueba ejecutada.

---

## Phase 2: User Story 1 — Conservar el comportamiento durante la refactorización (P1) 🎯 MVP

**Goal**: congelar contratos HTTP, errores, autorización y efectos observables antes de mover responsabilidades.

**Independent Test**: las solicitudes representativas conservan exactamente método, ruta, binding, JSON, estados, errores, autorización y efectos; los valores variables se comparan por su regla vigente.

### Pruebas de caracterización para User Story 1

- [X] T003 [P] [US1] [FR-003] [FR-004] [FR-033] Crear la caracterización de colecciones, filtros, orden y autorización de organization en `apps/backend/src/test/java/pe/gob/midagri/piip/organization/api/OrganizationControllerTest.java`.
- [X] T004 [P] [US1] [FR-003] [FR-004] [FR-024] [FR-033] Ampliar `apps/backend/src/test/java/pe/gob/midagri/piip/audit/api/AuditControllerTest.java` con máximo 100, orden descendente, filtro por UE, actor nullable y forma JSON.
- [X] T005 [P] [US1] [FR-003] [FR-004] [FR-023] [FR-033] Ampliar `apps/backend/src/test/java/pe/gob/midagri/piip/identity/api/IdentityControllerTest.java` con `roleScopes`, roles y ámbitos agregados, orden y efecto de registro de autenticación.
- [X] T006 [P] [US1] [FR-003] [FR-004] [FR-027] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/documents/api/DocumentControllerContractTest.java` para multipart, orden de validación, descarga, headers, bytes, nombre, MIME, límite y errores observables.
- [X] T007 [P] [US1] [FR-003] [FR-004] [FR-019] [FR-020] [FR-033] Ampliar `apps/backend/src/test/java/pe/gob/midagri/piip/work/api/WorkControllerTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/work/api/NotificationControllerTest.java` con pertenencia, orden, alerta temporal, versión, reasignación, payload y `204`.
- [X] T008 [P] [US1] [FR-003] [FR-004] [FR-021] [FR-033] Ampliar `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/api/DashboardControllerTest.java` con conteos, visibilidad, orden de `portfolioByStatus`, tareas, alertas y notificaciones, manteniendo separado `/dashboard/portfolio`.
- [X] T009 [P] [US1] [FR-003] [FR-004] [FR-026] [FR-029] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioControllerContractTest.java` para lista, detalle, tres altas, transiciones, paginación, errores y los cinco campos documentales heredados presentes y nulos.
- [X] T010 [P] [US1] [FR-007] [FR-033] Ampliar `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusAuditTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusConcurrencyTest.java` con atomicidad entre versión, tareas, notificaciones, documentos y auditoría, sin ejecutar las pruebas.

### Cierre de baseline para User Story 1

- [X] T011 [US1] [FR-003] [FR-004] [FR-033] [FR-034] Vincular cada escenario caracterizado con su endpoint, expectativa determinista o regla de normalización y criterio de rollback del incremento 0 en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

**Checkpoint**: US1 queda demostrable de forma aislada por las pruebas de caracterización; no se mueve código si un comportamiento afectado carece de baseline.

---

## Phase 3: Foundational — errores internos y traducción HTTP

**Purpose**: establecer la frontera compartida requerida por todas las historias restantes.

**⚠️ CRITICAL**: ninguna tarea de módulos consumidores comienza hasta completar esta fase.

- [X] T012 [FR-015] [FR-017] Mover `BusinessRuleException`, `InvalidReferenceException`, `NotFoundException` y `StaleVersionException` de `apps/backend/src/main/java/pe/gob/midagri/piip/shared/api/` a `apps/backend/src/main/java/pe/gob/midagri/piip/shared/application/error/`, conservando nombres, mensajes y propiedades, y actualizar sus imports en `apps/backend/src/main/java/pe/gob/midagri/piip/` y `apps/backend/src/test/java/pe/gob/midagri/piip/`.
- [X] T013 [FR-017] Adaptar `apps/backend/src/main/java/pe/gob/midagri/piip/shared/api/ApiExceptionHandler.java` a los errores internos tipados, retirar el manejo genérico de `IllegalStateException` y conservar los `ProblemDetail` observables de 403/404/409/422.
- [X] T014 [FR-014] [FR-017] Capturar explícitamente las invariantes funcionales vigentes de portfolio y work como `BusinessRuleException`, sin cambiar las excepciones técnicas de `config/reset`, auditoría o checksum, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java`, `ProjectApplicationService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/work/api/WorkController.java`.
- [X] T015 [FR-017] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/shared/api/ApiExceptionHandlerTest.java` con la matriz observable de errores y un caso técnico que compruebe que `IllegalStateException` no se traduce como regla de negocio.
- [X] T016 [FR-028] Confirmar únicamente los imports mínimos de los errores movidos en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/application/CatalogReferenceService.java` y sus pruebas, sin alterar lógica ni integración de catálogos.
- [X] T017 [FR-034] Registrar resultado, archivos, riesgos residuales, reversión y verificación no ejecutada del incremento 1 en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

**Checkpoint**: `shared.api` solo adapta HTTP; los errores funcionales pertenecen a application y un fallo técnico deja de parecer 422.

---

## Phase 4: User Story 4 — Mantener fronteras de modelos y errores (P1)

**Goal**: eliminar entidades y DTOs de controller de las fronteras internas de menor dependencia.

**Independent Test**: las reglas de frontera y las pruebas unitarias detectan entidades hacia API y modelos compartidos dentro de controllers; organization y audit mantienen el contrato caracterizado.

### Pruebas para User Story 4

- [X] T018 [P] [US4] [FR-015] [FR-016] [FR-022] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/organization/application/OrganizationQueryServiceTest.java` para filtros, orden, autorización y ausencia de entidades en los resultados.
- [X] T019 [P] [US4] [FR-015] [FR-024] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/audit/application/AuditQueryServiceTest.java` para mapeo completo dentro de la transacción de lectura, actor nullable, límite y orden.

### Implementación para User Story 4

- [X] T020 [US4] [FR-016] [FR-022] [FR-025] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/organization/application/OrganizationReadModels.java` con modelos no persistentes y `OrganizationalUnitView` como propietario modular reutilizable.
- [X] T021 [US4] [FR-012] [FR-022] Implementar consultas y fronteras `readOnly` en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/application/OrganizationQueryService.java`, construyendo los read models sin exponer asociaciones lazy.
- [X] T022 [US4] [FR-008] [FR-009] [FR-010] [FR-022] Reducir `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationController.java` a binding, delegación y adaptación a sus records HTTP, preservando rutas, filtros, orden y JSON.
- [X] T023 [P] [US4] [FR-015] [FR-024] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditReadModels.java` con vistas de acceso y evento que no retengan `AccessAuditEntity`, `AuditEventEntity` ni `UserEntity`.
- [X] T024 [US4] [FR-012] [FR-024] Adaptar `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditQueryService.java` para mapear entidades a `AuditReadModels` dentro de la transacción de lectura y preservar máximo, filtros y orden.
- [X] T025 [US4] [FR-008] [FR-010] [FR-024] Reducir `apps/backend/src/main/java/pe/gob/midagri/piip/audit/api/AuditController.java` a delegación y adaptación HTTP, eliminando conocimiento de tipos persistence.
- [X] T026 [US4] [FR-034] Registrar los resultados y criterios de reversión de organization y audit como incrementos 2 y parte independiente del 3 en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

**Checkpoint**: organization es dueño del modelo organizacional interno; audit devuelve vistas no persistentes y ambos contratos HTTP siguen congelados.

---

## Phase 5: User Story 3 — Aplicar autorización semántica consistente (P1)

**Goal**: centralizar decisiones sobre una asignación exacta y adelgazar `IdentityController.me` sin cambiar la integración de seguridad.

**Independent Test**: grants cruzados, UE real, cobertura institucional y revocación concurrente producen las decisiones actuales sin combinar privilegios de filas distintas.

### Pruebas para User Story 3

- [X] T027 [P] [US3] [FR-018] [FR-033] Ampliar `apps/backend/src/test/java/pe/gob/midagri/piip/identity/application/LocalAuthorizationServiceTest.java` con lectura por UE, Administrador PIIP por UE, elegibilidad exacta del destinatario y grants cruzados no combinables.
- [X] T028 [P] [US3] [FR-007] [FR-018] [FR-033] Ampliar `apps/backend/src/test/java/pe/gob/midagri/piip/identity/LocalAuthorizationConcurrencyTest.java` con revocación concurrente y reevaluación dentro del caso de uso.
- [X] T029 [P] [US3] [FR-023] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/identity/application/CurrentIdentityServiceTest.java` para secuencia de contexto, commit independiente de `recordAuthentication`, usuario visible y agregados exactos.

### Implementación para User Story 3

- [X] T030 [US3] [FR-017] [FR-018] Extender `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/LocalAuthorizationService.java` con políticas semánticas basadas en una asignación exacta, conservando `SecurityContextHolder`, `AccessDeniedException`, mensajes y cobertura administrativa separada.
- [X] T031 [P] [US3] [FR-015] [FR-023] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/CurrentIdentityReadModel.java` sin entidades JPA y con `roleScopes`, roles y ámbitos equivalentes.
- [X] T032 [US3] [FR-012] [FR-023] Implementar `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/CurrentIdentityService.java` sin una transacción exterior que altere el commit independiente de `recordAuthentication`.
- [X] T033 [US3] [FR-008] [FR-010] [FR-023] Reducir `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/IdentityController.java` a binding, delegación y construcción del response HTTP desde `CurrentIdentityReadModel`.
- [X] T034 [US3] [FR-034] Registrar el resultado, secuencia transaccional, riesgo y reversión de identity en el incremento 3 de `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

**Checkpoint**: la política exacta es reutilizable, pero Administración de usuarios conserva su cobertura institucional independiente; `me` conserva todos sus agregados y efectos.

---

## Phase 6: User Story 2 — Ejecutar casos de uso desde application (P1)

**Goal**: dejar los controllers afectados como adaptadores HTTP y trasladar casos de uso, transacciones, repositorios, versión, auditoría y efectos coordinados a application.

**Independent Test**: una inspección arquitectónica y las pruebas por servicio comprueban que los controllers productivos no contienen transacciones, repositorios, entidades ni reglas funcionales y que cada caso conserva sus efectos.

### Incremento 4 — Documents

- [X] T035 [P] [US2] [FR-027] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentUploadInputTest.java` para lectura diferida, copia defensiva y conservación del orden vacío → tamaño → MIME → lectura.
- [X] T036 [P] [US2] [FR-025] [FR-027] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentUploadInput.java` con metadatos y proveedor diferido de bytes, sin dependencia de Spring Web.
- [X] T037 [US2] [FR-008] [FR-027] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/MultipartDocumentUploadAdapter.java` y adaptar `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentController.java` para convertir `MultipartFile` al input de application sin ejecutar reglas funcionales.
- [X] T038 [US2] [FR-012] [FR-025] [FR-027] Adaptar `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` y `DocumentInboxService.java` para consumir `DocumentUploadInput` y `OrganizationReadModels.OrganizationalUnitView`, preservando el ciclo documental cohesivo.
- [X] T039 [US2] [FR-026] [FR-031] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/PortfolioDocumentService.java` con `initializeSlots(recordId)` unido a la transacción llamante y sin devolver entidades.
- [X] T040 [US2] [FR-007] [FR-026] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/PortfolioDocumentServiceTest.java` para orden de tipos activos, un slot por tipo y rollback junto al alta de portfolio.
- [X] T041 [US2] [FR-034] Registrar resultado, orden de validación, propagación y reversión del incremento 4 en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

### Incremento 5 — Work y notifications

- [X] T042 [P] [US2] [FR-015] [FR-019] [FR-020] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/work/application/WorkTaskReadModels.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/work/application/NotificationReadModels.java` con modelos no persistentes y propietarios del módulo work.
- [X] T043 [US2] [FR-012] [FR-018] [FR-019] Implementar `apps/backend/src/main/java/pe/gob/midagri/piip/work/application/WorkTaskService.java` con consulta pending, complete y reassign, reautorización exacta, versión, alerta en zona por defecto y auditoría dentro de una sola transacción.
- [X] T044 [US2] [FR-008] [FR-009] [FR-010] [FR-011] [FR-019] Reducir `apps/backend/src/main/java/pe/gob/midagri/piip/work/api/WorkController.java` a binding, delegación y adaptación HTTP, moviendo sus records compartidos a la frontera apropiada.
- [X] T045 [US2] [FR-019] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/work/application/WorkTaskServiceTest.java` para pertenencia, grants exactos, alerta, versión, reasignación, auditoría y regla funcional tipada.
- [X] T046 [P] [US2] [FR-012] [FR-020] Implementar `apps/backend/src/main/java/pe/gob/midagri/piip/work/application/NotificationService.java` con consulta personal ordenada y mutación de lectura transaccional.
- [X] T047 [US2] [FR-008] [FR-009] [FR-010] [FR-020] Reducir `apps/backend/src/main/java/pe/gob/midagri/piip/work/api/NotificationController.java` a binding, delegación y preservación de payload y `204`.
- [X] T048 [US2] [FR-020] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/work/application/NotificationServiceTest.java` para destinatario, orden, notificación inexistente y lectura idempotente según el comportamiento vigente.
- [X] T049 [US2] [FR-026] [FR-031] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/work/application/PortfolioWorkService.java` para efectos de tareas, notificaciones y eventos iniciados por portfolio, usando IDs/códigos y uniéndose a la transacción llamante.
- [X] T050 [US2] [FR-007] [FR-026] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/work/application/PortfolioWorkServiceTest.java` para orden de efectos y rollback conjunto ante fallo de tarea, notificación o auditoría.
- [X] T051 [US2] [FR-034] Registrar resultado, política compartida, propagación y reversión del incremento 5 en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

### Incremento 6 — Dashboard summary

- [X] T052 [P] [US2] [FR-015] [FR-021] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/application/DashboardSummaryReadModel.java` con conteos, mapa ordenado, tareas, alertas y notificaciones no persistentes.
- [X] T053 [US2] [FR-012] [FR-021] Implementar `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/application/DashboardSummaryService.java` con visibilidad, agregación y fronteras `readOnly`, reutilizando servicios de work sin alterar `DashboardPortfolioService`.
- [X] T054 [US2] [FR-008] [FR-009] [FR-010] [FR-021] Reducir `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/api/DashboardController.java` a adaptación de `summary`, conservando intacta la delegación conforme de `portfolio`.
- [X] T055 [US2] [FR-021] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/application/DashboardSummaryServiceTest.java` para visibilidad, conteos, orden, tareas, alertas y notificaciones.
- [X] T056 [US2] [FR-034] Registrar resultado y reversión del incremento 6, declarando explícitamente que `DashboardPortfolioService` no cambió, en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

### Incremento 7 — Portfolio

- [X] T057 [P] [US2] [FR-015] [FR-016] [FR-029] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioReadModels.java` y `PortfolioReadModelAssembler.java`, preservando los cinco campos documentales heredados presentes y nulos.
- [X] T058 [P] [US2] [FR-012] [FR-025] [FR-031] Crear `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitService.java` para validación, snapshot de nombre y orden reutilizados por ambos comandos, consumiendo `OrganizationalUnitView`.
- [X] T059 [US2] [FR-012] [FR-026] Implementar `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioQueryService.java` con lista, detalle, elegibilidad, visibilidad, paginación y `PortfolioPageView` sin tipos API ni entidades expuestas.
- [X] T060 [US2] [FR-012] [FR-026] Implementar `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` con alta, aprobación y transiciones, preservando locks, versión, código, auditoría y una sola transacción por caso.
- [X] T061 [US2] [FR-012] [FR-026] Implementar `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ProjectApplicationService.java` con alta derivada, alta preexistente y transiciones, preservando locks, versión, código, auditoría y una sola transacción por caso.
- [X] T062 [US2] [FR-007] [FR-026] Integrar `PortfolioDocumentService` y `PortfolioWorkService` desde los servicios de comandos en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` y `ProjectApplicationService.java`, conservando orden y rollback atómico.
- [X] T063 [US2] [FR-008] [FR-009] [FR-010] [FR-011] [FR-026] Adaptar `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java` para construir `PageResponse` y `PortfolioDtos` desde commands/read models sin reglas, transacciones, repositorios ni entidades.
- [X] T064 [P] [US2] [FR-026] [FR-033] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioQueryServiceTest.java` para visibilidad, paginación, elegibilidad, responsables y mapeo de campos heredados.
- [X] T065 [US2] [FR-007] [FR-026] [FR-033] Adaptar `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioAuthorizationTest.java`, `PortfolioInitiativeStatusServiceTest.java`, `PortfolioProjectStatusServiceTest.java`, `PortfolioStatusAuditTest.java` y `PortfolioStatusConcurrencyTest.java` a los nuevos servicios, preservando cobertura de autorización, transiciones, locks, auditoría y rollback.
- [X] T066 [US2] [FR-026] [FR-031] Retirar `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java` solo cuando sus consumidores y pruebas ya usen los servicios cohesionados y la clase haya quedado como fachada sin responsabilidad propia.
- [X] T067 [US2] [FR-034] Registrar resultado, secuencia atómica, riesgo y reversión caso por caso del incremento 7 en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

**Checkpoint**: todos los controllers desviados delegan; las transacciones y efectos coordinados pertenecen a application y cada módulo conserva su contrato externo.

---

## Phase 7: User Story 5 — Refactorizar por incrementos reversibles (P2)

**Goal**: demostrar que cada incremento puede revisarse, detenerse o revertirse de forma aislada.

**Independent Test**: cada incremento contiene baseline, riesgo, dependencias, propietario, archivos, criterio de rollback y verificación posterior antes de habilitar el siguiente.

- [X] T068 [US5] [FR-034] [FR-035] Auditar y completar los registros 0-7 en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`, dejando explícito cualquier resultado no ejecutado y prohibiendo paralelismo entre contratos, políticas, DTOs, transacciones o fixtures compartidos.
- [X] T069 [US5] [FR-003] [FR-034] Añadir a `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md` la comparación semántica por incremento y el punto exacto de restauración por módulo, sin declarar equivalencia donde no exista evidencia ejecutada.

**Checkpoint**: la trazabilidad no depende del estado de los checkboxes y distingue evidencia estática, prueba escrita, prueba ejecutada y validación pendiente.

---

## Phase 8: User Story 6 — Prevenir la reintroducción de desviaciones (P2)

**Goal**: automatizar las fronteras obligatorias y documentar la arquitectura resultante.

**Independent Test**: ejemplos controlados hacen fallar las reglas con archivo y motivo ante transacciones/repositorios/JPA/autorización en controllers, persistence hacia API o DTOs compartidos dentro de controllers.

### Pruebas arquitectónicas para User Story 6

- [X] T070 [P] [US6] [FR-008] [FR-009] [FR-010] [FR-011] [FR-032] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/architecture/ControllerLayeringTest.java` para prohibir `@Transactional`, repositorios, entidades y dependencias persistence en controllers, reportando archivo y regla violada.
- [X] T071 [P] [US6] [FR-015] [FR-017] [FR-032] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/architecture/ApplicationBoundaryTest.java` para prohibir entidades hacia API, dependencias application→`shared.api` para errores y tipos API usados como contratos internos.
- [X] T072 [P] [US6] [FR-016] [FR-032] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/architecture/SharedModelOwnershipTest.java` para detectar DTOs compartidos anidados en controllers y exigir propietario modular explícito.
- [X] T073 [US6] [FR-032] [FR-033] Añadir casos negativos controlados a `apps/backend/src/test/java/pe/gob/midagri/piip/architecture/ArchitectureRuleSelfTest.java` que demuestren el mensaje de fallo de cada regla sin introducir clases productivas deliberadamente inválidas.

### Documentación y cierre para User Story 6

- [X] T074 [US6] [FR-031] [FR-037] Crear `docs/architecture/backend-modular-architecture.md` con responsabilidades de API/application/domain/persistence, propietarios de read models, propagación transaccional, autorización semántica e integraciones portfolio→documents/work.
- [X] T075 [US6] [FR-005] [FR-006] [FR-013] [FR-030] [FR-037] [FR-038] Revisar el diff contra el manifiesto protegido y registrar en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md` cero cambios en frontend, OpenAPI/cliente, JPA/DDL/Oracle/config, SQL nativo o herramientas de migración, y la ausencia justificada de impacto funcional en `docs/`.
- [X] T076 [US6] [FR-031] [FR-032] [FR-034] Completar el incremento 8 y la matriz componente→responsabilidad→propietario→consumidor→prueba en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`, eliminando wrappers o abstracciones sin consumidor real.

**Checkpoint**: las desviaciones no pueden reintroducirse silenciosamente y la documentación refleja solo la arquitectura implementada.

---

## Phase 9: Validaciones propuestas — requieren autorización explícita

**Purpose**: ejecutar evidencia focalizada y regresión solo si el usuario lo autoriza en el turno de implementación. Completar estos checkboxes sin ejecutar los comandos está prohibido.

- [X] T077 [FR-033] [FR-036] Con autorización explícita, ejecutar desde `apps/backend` las pruebas focalizadas de `shared`, `organization`, `audit` e `identity` mediante `gradlew.bat test --tests "pe.gob.midagri.piip.shared.*" --tests "pe.gob.midagri.piip.organization.*" --tests "pe.gob.midagri.piip.audit.*" --tests "pe.gob.midagri.piip.identity.*"` y registrar salida y fecha en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.
- [X] T078 [FR-033] [FR-036] Con autorización explícita, ejecutar desde `apps/backend` las pruebas focalizadas de `documents`, `work`, `dashboard` y `portfolio` mediante `gradlew.bat test --tests "pe.gob.midagri.piip.documents.*" --tests "pe.gob.midagri.piip.work.*" --tests "pe.gob.midagri.piip.dashboard.*" --tests "pe.gob.midagri.piip.portfolio.*"` y registrar salida y fecha en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.
- [X] T079 [FR-032] [FR-036] Con autorización explícita, ejecutar desde `apps/backend` `gradlew.bat test --tests "pe.gob.midagri.piip.architecture.*"` y registrar cada frontera cubierta en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.
- [X] T080 [FR-003] [FR-036] Con autorización explícita, ejecutar desde `apps/backend` `gradlew.bat test` y después `gradlew.bat check`, sin omitir ni reinterpretar fallos baseline, y registrar resultados completos en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.
- [X] T081 [FR-005] [FR-036] Con autorización explícita, ejecutar desde `apps/frontend` `npm test -- --watch=false` contra el contrato backend conservado, sin modificar frontend ni regenerar OpenAPI/cliente, y registrar los flujos representativos o fallos baseline en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.
- [X] T082 [FR-003] [FR-004] [FR-036] Tras las validaciones autorizadas, completar la matriz final de equivalencia semántica y marcar como `NEEDS CLARIFICATION` cualquier diferencia no explicada en `specs/012-refactorizar-arquitectura-backend/implementation-evidence.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: sin dependencias.
- **Phase 2 (US1 / baseline)**: depende de Phase 1 y bloquea todo movimiento productivo.
- **Phase 3 (Foundational/shared)**: depende del baseline US1 y bloquea todos los módulos consumidores.
- **Phase 4 (US4 / organization y audit)**: depende de shared; organization debe finalizar antes de documents y portfolio. Audit puede avanzar en paralelo con la preparación de identity solo después de congelar shared.
- **Phase 5 (US3 / identity y autorización)**: depende de shared; sus políticas deben finalizar antes de work, dashboard y portfolio.
- **Phase 6 (US2)**: depende de US4 y US3. Su orden interno obligatorio es documents → work/notifications → dashboard → portfolio.
- **Phase 7 (US5)**: depende de los registros generados por todos los incrementos productivos.
- **Phase 8 (US6)**: depende de la estructura final de todos los módulos.
- **Phase 9 (Validaciones)**: depende de todas las tareas de implementación y de autorización explícita adicional; no es parte del permiso implícito de `/speckit-implement`.

### User Story Dependencies

```text
US1 (baseline observable)
  └─ Shared foundational
      ├─ US4 (modelos/errores: organization + audit)
      └─ US3 (autorización + identity)
           └─ US2 (documents → work → dashboard → portfolio)
                ├─ US5 (evidencia reversible completa)
                └─ US6 (guardas arquitectónicas finales)
```

### Parallel Opportunities

- En US1, T003-T010 son paralelizables porque escriben fixtures de módulos distintos; T011 espera a todas.
- Tras shared, T023 puede prepararse en paralelo con T020-T022; T024-T025 esperan a T023.
- En US3, T027-T029 son paralelizables; T031 puede prepararse en paralelo con T030, pero T032-T033 esperan a ambas.
- En documents, T035 y T036 pueden prepararse en paralelo; T037-T040 son secuenciales por contrato y transacción.
- En work, T042 puede comenzar antes de T043 y T046; `WorkTaskService` y `NotificationService` pueden implementarse en paralelo solo si no comparten cambios en autorización o fixtures.
- No paralelizar organization con documents/portfolio, identity con work/dashboard/portfolio, ni documents/work con portfolio.

---

## Parallel Example: User Story 1

```text
T003 OrganizationControllerTest.java
T004 AuditControllerTest.java
T005 IdentityControllerTest.java
T006 DocumentControllerContractTest.java
T007 WorkControllerTest.java + NotificationControllerTest.java
T008 DashboardControllerTest.java
T009 PortfolioControllerContractTest.java
T010 PortfolioStatusAuditTest.java + PortfolioStatusConcurrencyTest.java
```

## Parallel Example: User Story 4

```text
Tras T012-T017:
  Rama A: T018 → T020 → T021 → T022
  Rama B: T019 → T023 → T024 → T025
Convergencia: T026
```

## Parallel Example: User Story 2

```text
Documents: T035 || T036 → T037 → T038 → T039 → T040 → T041
Work:      T042 → (T043 → T044 → T045) || (T046 → T047 → T048) → T049 → T050 → T051
Dashboard: T052 → T053 → T054 → T055 → T056
Portfolio: T057 || T058 → T059 → T060 → T061 → T062 → T063 → T064 → T065 → T066 → T067
```

---

## Implementation Strategy

### MVP First — equivalencia demostrable

1. Completar Phase 1.
2. Escribir la caracterización de US1 en Phase 2.
3. Con autorización específica, ejecutar solo la evidencia de baseline requerida antes del primer movimiento.
4. Detenerse si algún endpoint afectado no tiene expectativas observables o si aparece una contradicción.

### Incremental Delivery

1. Baseline US1 → comportamiento congelado.
2. Shared → errores internos y traducción HTTP estable.
3. US4 → organization y audit con modelos propietarios.
4. US3 → identity y autorización semántica exacta.
5. US2 → documents, work/notifications, dashboard y portfolio en ese orden.
6. US5 → evidencia de reversibilidad completa.
7. US6 → guardas y documentación final.
8. Validaciones autorizadas → equivalencia confirmada o diferencias declaradas.

### Control histórico y de alcance

- Los antecedentes y Graphify solo orientan; `spec.md`, `plan.md`, código y pruebas actuales gobiernan las tareas.
- `MEJORA PROPUESTA SIN INCUMPLIMIENTO`, `NO VERIFICABLE` y antecedentes no generan tareas.
- No se incorporan optimización N+1, microservicios, Vertical Slice obligatorio, cambios funcionales, migraciones, JPA, Oracle, OpenAPI, cliente Angular o frontend.
- Cualquier contradicción descubierta durante implementación se marca `NEEDS CLARIFICATION` y detiene solo el incremento afectado.

---

## Notes

- `[P]` indica archivos y decisiones realmente independientes, no mera conveniencia.
- Cada tarea incluye al menos una historia o requisito trazable y una ruta exacta.
- Escribir pruebas no equivale a ejecutarlas; los resultados se registran como no verificados hasta contar con evidencia de comando autorizada.
- No ejecutar Git, OpenAPI, Oracle, builds, servidores ni pruebas por efecto de este documento.
