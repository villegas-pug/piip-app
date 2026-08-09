---
description: "Lista trazable de tareas para nombrar los parámetros HTTP del backend PIIP"
---

# Tareas: Nombrar parámetros HTTP del backend

**Entrada**: documentos de `specs/007-name-http-parameters/`

**Prerrequisitos**: `spec.md`, `plan.md`, `tasks.md`, `research.md` y el inventario de compatibilidad vigentes; sin checklists ni `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI ni integración Oracle. La invocación explícita de `/speckit-implement` aprueba estos artefactos vigentes y autoriza T001 a T005; T006 y T007, así como las demás acciones externas, requieren autorización separada.

## Formato obligatorio

Cada tarea usa `- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta/archivo exacto`.

## Evidencia de baseline — no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| US1 / FR-001 | Existen 36 anotaciones de enlace HTTP sin nombre explícito. | `apps/backend/src/main/java/**/api/*Controller.java` | Nombrarlas sin cambiar su identificador público. |
| US2 / FR-002 | Los consumidores usan los nombres actuales de rutas, consulta y multipart. | `specs/007-name-http-parameters/contracts/http-parameter-compatibility.md` | Mantener el contrato sin adaptar frontend u OpenAPI. |
| FR-003 | `-parameters` ya está configurado para Gradle. | `apps/backend/build.gradle.kts` | Mantener la configuración; no depender exclusivamente de ella. |

## Phase 1: Fundamento de regresión

**Propósito**: fijar el inventario verificable antes de modificar los controladores.

- [X] T001 [FR-001] [FR-002] [FR-003] Crear `HttpParameterBindingTest` que, mediante reflexión, compruebe el nombre explícito de las 36 entradas en `apps/backend/src/test/java/pe/gob/midagri/piip/architecture/HttpParameterBindingTest.java`

**Checkpoint**: la prueba describe el inventario de 16 variables de ruta, 19 parámetros de consulta y una parte multipart, sin requerir contexto Spring u Oracle.

---

## Phase 2: User Story 1 - Procesar solicitudes con parámetros identificables (Priority: P1)

**Goal**: cubrir en un incremento independiente los tres tipos de entrada HTTP: ruta, consulta y multipart.

**Independent Test**: el inventario de T001 verifica los nombres explícitos de las 13 entradas de organización y documentos.

- [X] T002 [P] [US1] [FR-001] [FR-003] [FR-005] Nombrar explícitamente `executingUnitId` en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationController.java` y las 12 entradas de ruta, consulta y multipart en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentController.java`

**Checkpoint**: las operaciones representativas de ruta, consulta y multipart no dependen del nombre del parámetro Java.

---

## Phase 3: User Story 2 - Mantener compatibilidad de los consumidores actuales (Priority: P2)

**Goal**: completar el inventario restante sin alterar los nombres públicos usados por consumidores existentes.

**Independent Test**: el inventario de T001 verifica que las 23 entradas restantes conservan nombre, opcionalidad y valores por defecto publicados.

- [X] T003 [P] [US2] [FR-001] [FR-002] [FR-003] Nombrar explícitamente las 17 entradas de listados y detalle en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java`
- [X] T004 [P] [US2] [FR-001] [FR-002] [FR-003] Nombrar explícitamente las seis entradas restantes en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/UserAdministrationController.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/work/api/WorkController.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/work/api/NotificationController.java`

**Checkpoint**: las 36 entradas del inventario conservan rutas, métodos, nombres, tipos, obligatoriedad y valores por defecto.

---

## Phase 4: Verificación de alcance y compatibilidad

- [X] T005 [FR-001] [FR-002] [FR-004] [FR-005] Revisar el diff de los seis controladores contra `specs/007-name-http-parameters/contracts/http-parameter-compatibility.md` y confirmar que solo cambian nombres explícitos de enlace HTTP
- [ ] T006 [FR-001] [FR-002] [FR-003] Ejecutar la prueba focalizada desde `apps/backend` con `gradlew.bat test --tests "pe.gob.midagri.piip.architecture.HttpParameterBindingTest"` — requiere autorización explícita
- [ ] T007 [US1] [US2] [FR-002] [FR-004] Ejecutar los escenarios manuales de ruta, consulta, multipart y entrada inválida descritos en `specs/007-name-http-parameters/quickstart.md` — requiere autorización explícita para levantar y usar el backend

## Dependencias y orden de ejecución

- **Propietario canónico**: T002, T003 y T004 cambian exclusivamente el backend, propietario del contrato HTTP.
- **Consumidores**: no hay tareas frontend ni OpenAPI; T005 confirma que no requieren adaptación.
- **Orden obligatorio**: T001 antes de T002-T004; T002-T004 antes de T005; T005 antes de las validaciones T006-T007.
- **Oportunidades paralelas**: T002, T003 y T004 pueden ejecutarse en paralelo después de T001 porque no comparten archivos. T006 y T007 solo se consideran después de T005 y de sus autorizaciones respectivas.

## Estrategia de implementación

1. **MVP**: T001 y T002 demuestran resolución explícita para ruta, consulta y multipart.
2. **Entrega completa**: T003 y T004 completan las 36 entradas y preservan el contrato para todos los consumidores.
3. **Cierre**: T005 revisa el alcance; T006 y T007 se ejecutan únicamente con autorización independiente.

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- No crear tareas a partir de pendientes históricos por arrastre.
- Dependencia histórica aprobada: Ninguna.
- Contradicciones sin resolver: Ninguna.

## Notas

- Cada tarea representa trabajo nuevo y apunta a una ruta real.
- No se crearán tareas para frontend, OpenAPI, persistencia, autorización ni auditoría, porque están fuera de alcance.
- Los checkboxes solo cambian a `[X]` con evidencia del cambio o validación correspondiente.
