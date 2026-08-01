# Tasks: Migrar backend a Gradle

**Input**: Design documents from `specs/006-migrate-gradle-backend/`

**Execution boundary**: simulación de `/speckit-implement`. La implementación ya existe; las tareas se marcan completadas como trazabilidad documental y no se reescribe el producto.

## Phase 1: Setup documental

**Purpose**: completar la trazabilidad de Spec Kit sin modificar el producto.

- [x] T001 Registrar en `specs/006-migrate-gradle-backend/plan.md` el baseline Gradle existente y sus límites.
- [x] T002 [P] Registrar las decisiones y evidencias consultadas en `specs/006-migrate-gradle-backend/research.md`.
- [x] T003 [P] Documentar las comprobaciones futuras y artefactos esperados en `specs/006-migrate-gradle-backend/quickstart.md`.

## Phase 2: User Story 1 - Flujo reproducible (Priority: P1)

**Goal**: dejar trazable que el backend ya tiene un único flujo Gradle versionado.

**Independent Test**: revisión documental de `build.gradle.kts`, `settings.gradle.kts`, wrapper y rutas `target/`; no ejecutar build en esta simulación.

- [x] T004 [US1] Documentar FR-001, FR-002 y FR-003 mediante la evidencia existente en `apps/backend/build.gradle.kts` y `apps/backend/settings.gradle.kts`.
- [x] T005 [P] [US1] Documentar FR-001 mediante los wrappers existentes `apps/backend/gradlew` y `apps/backend/gradlew.bat`.
- [x] T006 [P] [US1] Documentar FR-004 y SC-002 mediante la conservación de `apps/backend/target/piip-openapi.json` y `apps/backend/target/piip-oracle.sql`.

## Phase 3: User Story 2 - Entrega automatizada (Priority: P1)

**Goal**: dejar trazable la migración operativa ya aplicada en CI y documentación.

**Independent Test**: revisión de referencias Gradle en los archivos operativos; no ejecutar CI ni infraestructura.

- [x] T007 [US2] Documentar FR-005, SC-001 y SC-003 mediante la evidencia de Gradle en `.github/workflows/ci.yml`.
- [x] T008 [P] [US2] Documentar FR-006 y SC-004 mediante las instrucciones vigentes en `README.md` y `docs/deployment/institutional-development.md`.
- [x] T009 [P] [US2] Documentar FR-006 y FR-007 mediante el guard de ámbito en `.codex/hooks/piip_scope_guard.py`.

## Phase 4: Revisión Spec Kit

- [x] T010 Verificar la consistencia cruzada de `spec.md`, `plan.md` y `tasks.md` mediante `/speckit-analyze`.
- [x] T011 [P] Confirmar FR-007 revisando `specs/006-migrate-gradle-backend/tasks.md` y que no existan tareas de infraestructura ni modificación de código fuera de los artefactos Spec Kit.

## Dependencies & Execution Order

- T001 precede la generación de tareas; T002 y T003 se completaron en paralelo documental.
- T004–T009 registran evidencia del baseline y no habilitan reimplementación.
- T010 y T011 completan la revisión final de los artefactos Spec Kit.

## Out of Scope

- No se ejecutó `/speckit-implement` real sobre el producto.
- No se ejecutaron Docker, Testcontainers, `integrationTest`, pruebas, builds ni conexión Oracle.
- No se modificaron Java, Gradle, CI, frontend, wallets, secretos, entidades ni documentación operativa.
