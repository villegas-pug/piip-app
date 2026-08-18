---
description: "Tareas trazables para ampliar el ciclo de vida del portafolio PIIP"
---

# Tareas: Ciclo de vida del portafolio PIIP

**Entrada**: documentos de `/specs/009-ciclo-vida-portafolio/`

**Prerrequisitos**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/portfolio-status-transitions.openapi.yaml` y `quickstart.md` vigentes; sin checklists ni `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: esta lista no autoriza implementación, pruebas, builds, generación OpenAPI, integración Oracle ni acciones Git. `/speckit-implement` autoriza únicamente las tareas de implementación vigentes; T018, T019, T038 y T039 requieren además autorización explícita para ejecutar sus comandos.

## Formato obligatorio

Cada tarea usa `- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta/archivo exacto`.

## Evidencia de baseline — no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| US1 / FR-001 / FR-005 | Registro en `Presentado`, aprobación por `/approval` y creación derivada separada ya existen. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java` | Conservar y cubrir como regresión; no reimplementar. |
| US1 / FR-002 / FR-003 | La derivación es la relación única `originRecord`; la iniciativa no cambia al crear el proyecto. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` | Reutilizar la relación, nunca crear un estado “Proyecto derivado”. |
| US2 / FR-004 | El frontend ya puede consultar el proyecto derivado desde el detalle de iniciativa. | `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.ts`, `apps/frontend/src/app/core/piip-http.repository.ts` | Usar esa evidencia para ocultar acciones y explicar el bloqueo. |
| US3 / FR-007 / FR-021 | `PortfolioStatus` ya contiene estados de proyecto y la entidad ya contiene `closingDate`. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/domain/PortfolioStatus.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` | Agregar reglas, no catálogo ni columna. |
| US5 / FR-016 / FR-018 | `@Version VERSION`, el mapa frontend de versiones y `AuditService.event(...)` ya existen. | `PortfolioRecordEntity.java`, `apps/frontend/src/app/core/piip-http.repository.ts`, `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditService.java` | Reutilizar control optimista y auditoría; no crear mecanismos paralelos. |
| US4 / FR-010 / FR-011 | Los listados ya consultan/filtran y la aprobación se confirma en detalle. | `apps/frontend/src/app/pages/initiatives/**`, `apps/frontend/src/app/pages/projects/**`, `apps/frontend/src/app/pages/initiative-detail/**` | Mantener listados sin mutaciones y ampliar solo detalles contextuales. |

## Phase 1: Preparación compartida

**Propósito**: introducir dependencias técnicas pequeñas y listas contextuales que no dependen del contrato generado.

- [X] T001 [P] [FR-021] Declarar el bean `Clock` con `ZoneId.of("America/Lima")` en `apps/backend/src/main/java/pe/gob/midagri/piip/config/TimeConfig.java`
- [X] T002 [P] [FR-008] [FR-009] [FR-029] Definir conjuntos tipados separados de estados de iniciativa, estados de proyecto y destinos permitidos sin `No Aplicable` en `apps/frontend/src/app/core/piip.catalogs.ts`

---

## Phase 2: Fundamentos bloqueantes de dominio y contrato

**Propósito**: establecer reglas, concurrencia y requests canónicos antes de los casos de uso y consumidores.

- [X] T003 [FR-006] [FR-007] [FR-021] [FR-022] [FR-025] Agregar comportamientos de dominio separados para transición de iniciativa y proyecto, sin setter genérico y preservando `closingDate` fuera de `FINISHED`, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java`
- [X] T004 [P] [FR-006] [FR-007] [FR-008] [FR-009] [FR-021] [FR-022] Cubrir matrices permitidas, destinos cruzados, `NOT_APPLICABLE`, terminales y fecha de cierre con reloj fijo en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioTransitionTest.java` (depende de T003)
- [X] T005 [P] [FR-004] [FR-026] Agregar lectura JPA de la iniciativa por código con `@Lock(PESSIMISTIC_WRITE)` y grafo de Unidad Ejecutora/origen, sin SQL nativo, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordRepository.java`
- [X] T006 [P] [FR-015] [FR-016] [FR-027] Definir `InitiativeStatusTransitionRequest` y `ProjectStatusTransitionRequest` separados, con versión/destino obligatorios y observación máxima de 1000, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioDtos.java`

**Checkpoint**: las matrices viven en el dominio, la iniciativa admite bloqueo localizado y el contrato de entrada está tipado por contexto; no existe cambio de esquema.

---

## Phase 3: User Story 1 — Conservar el recorrido vigente (P1)

**Objetivo**: demostrar que la ampliación no altera registro, aprobación ni derivación existentes.

**Prueba independiente**: registrar una iniciativa, aprobarla por `/approval` y crear su proyecto derivado conserva `Presentado`, `Iniciativa aprobada`, `Proyecto en ejecución` y la relación única, sin creación automática.

- [X] T007 [P] [US1] [FR-001] [FR-002] [FR-003] [FR-005] Ampliar la regresión backend del flujo registro-aprobación-derivación y de la unicidad/origen sin reimplementar el flujo en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioTransitionTest.java` (depende de T004)
- [X] T008 [P] [US1] [FR-001] [FR-003] [FR-005] Conservar en pruebas de presentación el botón de aprobación, la separación de creación derivada y la etiqueta real de estado en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.spec.ts`

**Checkpoint**: la evidencia automatizable del recorrido actual permanece explícita antes de ampliar acciones.

---

## Phase 4: User Story 2 — Proteger la iniciativa después de la derivación (P1)

**Objetivo**: bloquear autoritativamente y en UI cualquier cambio de estado de una iniciativa vinculada.

**Prueba independiente**: una iniciativa aprobada con proyecto vinculado no muestra controles, rechaza llamadas directas y nunca compite con la derivación hasta producir una combinación inválida.

- [X] T009 [US2] [FR-004] [FR-012] [FR-013] [FR-014] [FR-015] [FR-016] [FR-019] [FR-020] [FR-026] Implementar la transición transaccional de iniciativa, la validación de vínculo/versión/ámbito y el evento `ESTADO_INICIATIVA_CAMBIADO`, y usar la lectura bloqueante también en `createDerived(...)`, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java` (depende de T001, T003, T005 y T006)
- [X] T010 [US2] [FR-004] [FR-027] [FR-028] Exponer `POST /api/v1/initiatives/{code}/status-transitions` delegando al servicio y conservando `/approval` en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java` (depende de T009)
- [X] T011 [P] [US2] [FR-004] [FR-012] [FR-013] [FR-020] [FR-023] [FR-026] Cubrir rechazo por vínculo, ámbito, tipo incorrecto y carrera derivar-versus-archivar sin evento exitoso, y comprobar que los documentos pendientes no bloquean una transición válida de iniciativa, en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioInitiativeStatusServiceTest.java` (depende de T009 y T010)
- [X] T012 [P] [US2] [FR-004] Ocultar todas las acciones de estado y mostrar la explicación del proyecto vinculado en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.ts` y `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.html`
- [X] T013 [US2] [FR-004] Verificar ausencia de controles y mensaje de bloqueo con proyecto vinculado en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.spec.ts` (depende de T012)

**Checkpoint**: frontend y backend impiden modificar una iniciativa vinculada; la derivación y el archivado quedan serializados sobre la misma iniciativa.

---

## Phase 5: User Story 3 — Cambiar el estado del proyecto desde su detalle (P1)

**Objetivo**: ofrecer y confirmar únicamente transiciones válidas de proyecto desde un detalle general.

**Prueba independiente**: un administrador autorizado cambia un proyecto a un destino permitido, conserva la observación, recibe la nueva versión y obtiene `closingDate` de Lima solo al finalizar.

- [X] T014 [US3] [FR-007] [FR-008] [FR-009] [FR-012] [FR-013] [FR-014] [FR-015] [FR-016] [FR-018] [FR-019] [FR-021] [FR-022] Implementar la transición transaccional de proyecto con `Clock`, autorización, matriz, versión, fecha de cierre y evento `ESTADO_PROYECTO_CAMBIADO` en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java` (depende de T001, T003 y T006; después de T009 por compartir archivo)
- [X] T015 [US3] [FR-007] [FR-008] [FR-009] [FR-027] [FR-028] Exponer `POST /api/v1/projects/{code}/status-transitions` con el request contextual y `PortfolioRecordResponse` en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java` (depende de T014; después de T010 por compartir archivo)
- [X] T016 [P] [US3] [FR-007] [FR-008] [FR-009] [FR-012] [FR-021] [FR-022] [FR-023] Cubrir matriz, autorización, estados terminales, fecha Lima, preservación de `closingDate` y una transición válida con documentos pendientes en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioProjectStatusServiceTest.java` (depende de T014 y T015)
- [X] T017 [US3] [FR-005] [FR-015] [FR-027] [FR-028] Incorporar ambos endpoints, schemas separados, response existente y errores `400/403/404/409/422` a las aserciones OpenAPI en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java`, y cubrir esos códigos como respuestas HTTP reales mediante pruebas MVC en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioControllerStatusTransitionTest.java` (depende de T010 y T015)
- [X] T018 [US3] [FR-027] Generar y revisar `apps/backend/target/piip-openapi.json` desde `apps/backend` con `./gradlew.bat test --tests "pe.gob.midagri.piip.contract.OpenApiGenerationTest"` — autorización explícita requerida; depende de T017
- [X] T019 [US3] [FR-027] Regenerar el consumidor en `apps/frontend/src/app/api/generated/` desde `apps/frontend` con `npm run api:generate` — autorización explícita requerida; depende de T018
- [X] T020 [US3] [FR-007] [FR-008] [FR-016] [FR-027] Agregar modelo de detalle de proyecto, inputs separados de transición y métodos `getProjectDetail`, `transitionInitiativeStatus` y `transitionProjectStatus` en `apps/frontend/src/app/core/piip.models.ts` y `apps/frontend/src/app/core/piip.repository.ts` (depende de T019)
- [X] T021 [US3] [FR-007] [FR-008] [FR-016] [FR-017] [FR-027] Consumir las dos rutas generadas, reutilizar `recordVersions`, actualizar la versión devuelta y conservar el tratamiento `409` en `apps/frontend/src/app/core/piip-http.repository.ts` (depende de T020)
- [X] T022 [P] [US3] [FR-007] [FR-008] [FR-009] [FR-016] [FR-017] Probar requests contextuales, versión enviada/actualizada, rechazo de mezcla y mensaje de recarga en `apps/frontend/src/app/core/piip-http.repository.spec.ts` (depende de T021)
- [X] T023 [P] [US3] [FR-011] Declarar `proyectos/:code/documentos` antes de la nueva ruta `proyectos/:code` en `apps/frontend/src/app/app.routes.ts`
- [X] T024 [US3] [FR-007] [FR-008] [FR-009] [FR-011] [FR-021] [FR-022] Crear el detalle general con estado actual, origen, `closingDate`, acceso documental/auditoría, selector contextual, observación y confirmación en `apps/frontend/src/app/pages/project-detail/project-detail.component.ts`, `apps/frontend/src/app/pages/project-detail/project-detail.component.html` y `apps/frontend/src/app/pages/project-detail/project-detail.component.scss` (depende de T002, T021 y T023)
- [X] T025 [US3] [FR-007] [FR-008] [FR-009] [FR-011] [FR-021] [FR-022] Probar destinos por estado, terminales, ausencia de estados de iniciativa/`No Aplicable`, observación y fecha de cierre en `apps/frontend/src/app/pages/project-detail/project-detail.component.spec.ts` (depende de T024)
- [X] T026 [US3] [FR-010] [FR-011] Cambiar “Abrir” para navegar al detalle general y conservar el acceso al expediente sin añadir transiciones al listado en `apps/frontend/src/app/pages/projects/projects.component.ts`, `apps/frontend/src/app/pages/projects/projects.component.html` y `apps/frontend/src/app/pages/projects/projects.component.spec.ts` (depende de T023 y T024)

**Checkpoint**: los dos endpoints están en el contrato consumido y el proyecto se gestiona únicamente desde su detalle contextual.

---

## Phase 6: User Story 5 — Resolver concurrencia y auditar atómicamente (P1)

**Objetivo**: demostrar que versión, estado, cierre y auditoría se confirman juntos o no se confirman.

**Prueba independiente**: dos sesiones usan la misma versión; solo una persiste. Una falla de auditoría revierte la transición y el evento exitoso expone toda la evidencia requerida.

- [X] T027 [P] [US5] [FR-016] [FR-017] [FR-018] [FR-020] [FR-024] [FR-026] Implementar una prueba de integración JPA transaccional sin mocks de repositorio que cubra la carrera entre derivar y archivar, dos transiciones con la misma versión, el incremento del `@Version` existente y la ausencia de segundo versionado en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusConcurrencyTest.java` (depende de T005, T009 y T014)
- [X] T028 [P] [US5] [FR-018] [FR-019] [FR-020] Cubrir rollback ante falla de auditoría y contenido exacto de `DETALLE_JSON` para ambas transiciones en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusAuditTest.java` (depende de T009 y T014)
- [X] T029 [P] [US5] [FR-017] Confirmar que el repositorio conserva el registro visible y solicita recarga después de HTTP `409` en `apps/frontend/src/app/core/piip-http.repository.spec.ts` (depende de T021; coordinar con T022 por compartir archivo)
- [X] T030 [P] [US5] [FR-019] Mostrar nombres de evento y detalle de estados, rol, Unidad Ejecutora, observación y resultado en `apps/frontend/src/app/pages/audit/audit-event.presenter.ts` y `apps/frontend/src/app/pages/audit/audit-event.presenter.spec.ts`

**Checkpoint**: concurrencia y auditoría tienen evidencia focalizada; los rechazos no aparentan transiciones exitosas.

---

## Phase 7: User Story 4 — Gestionar decisiones de iniciativa desde el detalle (P2)

**Objetivo**: ampliar las acciones de iniciativa sin proyecto, conservar aprobación y mantener listados solo para consulta.

**Prueba independiente**: cada estado de iniciativa muestra únicamente acciones válidas en detalle; terminales y vinculadas no muestran acciones; los listados conservan filtros contextuales sin mutación.

- [X] T031 [US4] [FR-005] [FR-006] [FR-008] [FR-009] [FR-011] [FR-014] [FR-015] Integrar archivado/no admisible con observación de hasta 1000, conservar el botón de aprobación y ocultar acciones terminales en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.ts` y `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.html` (depende de T002 y T021; después de T012 por compartir archivos)
- [X] T032 [US4] [FR-005] [FR-006] [FR-008] [FR-009] [FR-011] Probar acciones por estado, aprobación intacta, terminales y exclusión de estados de proyecto/`No Aplicable` en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.spec.ts` (depende de T031; después de T013 por compartir archivo)
- [X] T033 [P] [US4] [FR-010] [FR-029] Limitar el filtro de iniciativas a estados de iniciativa y mantenerlo como consulta en `apps/frontend/src/app/pages/initiatives/initiatives.component.ts` y `apps/frontend/src/app/pages/initiatives/initiatives.component.html` (depende de T002)
- [X] T034 [P] [US4] [FR-010] [FR-029] Limitar el filtro de proyectos a estados de proyecto y mantenerlo como consulta en `apps/frontend/src/app/pages/projects/projects.component.ts` y `apps/frontend/src/app/pages/projects/projects.component.html` (depende de T002; después de T026 por compartir archivos)
- [X] T035 [US4] [FR-010] [FR-029] Verificar filtros contextuales y ausencia de controles de transición en `apps/frontend/src/app/pages/initiatives/initiatives.component.spec.ts` y `apps/frontend/src/app/pages/projects/projects.component.spec.ts` (depende de T033 y T034)

**Checkpoint**: la iniciativa ofrece solo decisiones válidas desde detalle y ambos listados permanecen exclusivamente consultivos.

---

## Phase 8: Documentación y cierre transversal

- [X] T036 [FR-001] [FR-004] [FR-007] [FR-010] [FR-016] [FR-019] [FR-021] [FR-023] Actualizar el recorrido cronológico, matrices contextuales, bloqueo por derivación, concurrencia, auditoría, cierre y carácter no bloqueante de documentos en `docs/funcional/guia-funcional-piip.md`
- [X] T037 [P] [FR-015] [FR-027] [FR-028] Reconciliar el contrato de diseño con los DTO/endpoints definitivos y documentar cualquier diferencia sustentada en `specs/009-ciclo-vida-portafolio/contracts/portfolio-status-transitions.openapi.yaml`
- [X] T038 [FR-001] [FR-004] [FR-007] [FR-016] [FR-018] [FR-021] Ejecutar las pruebas backend focalizadas desde `apps/backend` con `./gradlew.bat test --tests "pe.gob.midagri.piip.portfolio.*"` y registrar resultado en `specs/009-ciclo-vida-portafolio/quickstart.md` — autorización explícita requerida; `OpenApiGenerationTest` se ejecuta exclusivamente en T018
- [ ] T039 [FR-004] [FR-007] [FR-010] [FR-017] [FR-019] [FR-029] Ejecutar las pruebas frontend desde `apps/frontend` con `npm test -- --watch=false` y registrar resultado en `specs/009-ciclo-vida-portafolio/quickstart.md` — autorización explícita requerida
- [X] T040 [FR-018] [FR-023] [FR-024] [FR-025] Registrar las validaciones realmente ejecutadas, identificar T038 y T039 como ejecutadas o pendientes de autorización, y confirmar la ausencia de cambio lógico de esquema o versionado en `specs/009-ciclo-vida-portafolio/quickstart.md` (depende de T036 y T037; no depende de la ejecución de T038 ni T039)

## Dependencias y orden de ejecución

- **Propietario canónico**: T001 y T003–T017 en backend definen tiempo, dominio, concurrencia, servicio y contrato.
- **Consumidores**: T019–T035 en frontend dependen del OpenAPI publicado por T018/T019; T036–T040 documentan y verifican el resultado.
- **Orden obligatorio**: T003 antes de T009/T014; T005 antes de T009; T009 antes de T010; T014 antes de T015; T010 y T015 antes de T017; T017 → T018 → T019 → T020 → T021; T021 antes de los detalles; T012 antes de T031 por archivos compartidos; T026 antes de T034 por archivos compartidos.
- **Historias**: US1 puede verificarse tras fundamentos; US2 y US3 implementan endpoints P1; US5 depende de ambos para validar integridad; US4 consume la transición de iniciativa y completa el alcance P2.
- **Oportunidades paralelas**: T001 con T002; T004, T005 y T006 tras T003 cuando no compartan archivo; T007 con T008; T011 con T012; T016 con T023; T022 con T025 una vez disponibles sus dependencias; T027, T028 y T030; T033 con T037. T029 no se paraleliza con T022 y T034 no se paraleliza con T026 porque comparten archivos.

## Ejemplos de ejecución paralela por historia

- **US1**: T007 (backend) y T008 (frontend) pueden prepararse en paralelo.
- **US2**: después de T009/T010, T011 puede avanzar en paralelo con T012; T013 espera T012.
- **US3**: T016 y T023 pueden avanzar en paralelo; después del contrato/cliente, T022 puede avanzar mientras se construye T024, pero T025 espera el componente.
- **US5**: T027, T028 y T030 usan archivos distintos y pueden avanzar en paralelo; T029 se coordina con T022.
- **US4**: T033 puede avanzar en paralelo con tareas que no toquen listados; T034 espera T026 y T035 espera ambos filtros.

## Estrategia de implementación incremental

1. **Fundamento**: completar T001–T006 sin cambios de esquema.
2. **Regresión**: completar US1 (T007–T008) para fijar el flujo existente.
3. **MVP funcional P1**: completar US2, US3 y US5 (T009–T030), incluidos contrato y cliente sincronizados con autorizaciones explícitas.
4. **Incremento P2**: completar US4 (T031–T035).
5. **Cierre**: documentación y validaciones autorizadas (T036–T040).

**MVP recomendado**: fundamentos + US1 + US2 + US3 + US5. US1 sola solo protege la regresión y no entrega el nuevo ciclo de vida.

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog; no generan tareas.
- Dependencias históricas aprobadas: flujo vigente de aprobación y derivación, registrado únicamente como baseline.
- `NEEDS CLARIFICATION`: regla futura sobre documentos que podrían bloquear transiciones. No bloquea v1 porque FR-023 exige que los documentos pendientes nunca impidan una transición.
- No crear tareas de tabla de matriz, historial paralelo, segundo versionado, transición a `No Aplicable`, edición manual de `closingDate`, bloqueo documental, creación automática de proyectos ni mutación desde listados.

## Notas

- Cada checkbox representa trabajo nuevo o evidencia automatizable requerida por esta feature; la tabla de baseline no se marca como trabajo completado.
- `[P]` solo identifica archivos y dependencias que permiten ejecución simultánea; prevalecen las notas explícitas cuando dos tareas comparten archivo.
- No editar manualmente `apps/frontend/src/app/api/generated/`; T019 debe regenerarlo desde el OpenAPI autorizado.
- No marcar T018, T019, T038 ni T039 como completadas sin autorización y evidencia de sus comandos.
- El cierre debe distinguir implementación realizada, validaciones ejecutadas y validaciones pendientes.
