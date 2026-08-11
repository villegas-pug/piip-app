# Tasks: Keycloak como autoridad de estado de cuenta

**Input**: `specs/008-administrar-usuarios/{spec.md,plan.md,research.md,data-model.md,contracts/user-administration-http.md,quickstart.md}`

**Baseline**: el CRUD de asignaciones, la publicación de candidatos y el cliente generado existente son trabajo previo. Las tareas siguientes cubren exclusivamente el retiro del estado local de usuario y su sustitución por la autoridad de Keycloak; no reimplementan ese baseline.

**Autorización**: generar tareas no autoriza implementación. `/speckit-implement` autoriza sólo estas tareas de código. Publicar OpenAPI, regenerar el cliente, ejecutar pruebas, E2E, Oracle y Git requieren autorización explícita separada en el turno de ejecución.

## Phase 1: User Story 3 — Separar estado de cuenta y autorización PIIP (P1)


**Goal**: eliminar el estado local de cuenta de la autorización y de la administración, preservando las operaciones sobre asignaciones.

**Independent Test**: una asignación vigente autoriza sin depender del valor local heredado, y la pantalla no expone una acción de estado de cuenta.

- [X] T001 [US3] Retirar el endpoint de cambio de estado y los campos de cuenta de los DTOs en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/{UserAdministrationController,AdminDtos}.java`.
- [X] T002 [US3] Retirar `changeStatus`, sus eventos de auditoría y el mapeo de estado de usuario en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java`.
- [X] T003 [US3] Eliminar el uso de `user.active` de las consultas de autorización, cobertura, último administrador y destinatarios en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/persistence/UserRoleScopeRepository.java`.
- [X] T004 [P] [US3] Cambiar la consulta de candidatos para ignorar el estado heredado local en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/persistence/UserRepository.java`.

## Phase 2: Contrato publicado y consumidor de User Story 3

- [X] T005 [P] [US3] Actualizar las pruebas de repositorio, servicio, controlador y autorización local para cubrir la ausencia de estado local funcional en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/`.
- [X] T006 [US3] Actualizar la prueba de contrato OpenAPI para que no describa el cambio de estado en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java`.
- [X] T007 [US3] Con autorización explícita, publicar el contrato mediante `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java` y comprobar que retiró la ruta y campos obsoletos.
- [X] T008 [US3] Con autorización explícita posterior a T007, regenerar `apps/frontend/src/app/api/generated/` mediante `apps/frontend/package.json` y revisar los archivos retirados por `removeStaleFiles`.

## Phase 3: Interfaz de User Story 3

- [X] T009 [US3] Retirar estado, señales y llamada de cambio de estado de usuario en `apps/frontend/src/app/pages/user-administration/user-administration.component.ts`.
- [X] T010 [P] [US3] Retirar etiquetas, confirmaciones y botones de estado de usuario, conservando estado de asignación y accesibilidad, en `apps/frontend/src/app/pages/user-administration/{user-administration.component.html,user-administration.component.scss}`.
- [X] T011 [US3] Actualizar pruebas del componente para ausencia de controles de cuenta y preservación de roles/ámbitos, errores y duplicados en `apps/frontend/src/app/pages/user-administration/user-administration.component.spec.ts`.

## Phase 4: Validación y E2E

- [X] T012 Con autorización explícita, ejecutar las pruebas focalizadas backend desde `apps/backend` y registrar el resultado sin ejecutar integración Oracle adicional.
- [X] T013 Con autorización explícita, ejecutar `npm test -- --watch=false` desde `apps/frontend` y distinguir cualquier fallo ajeno a Administración de usuarios.
- [X] T014 Con autorización explícita, ejecutar el recorrido E2E de `specs/008-administrar-usuarios/quickstart.md`: confirmar la ausencia de gestión de cuentas en PIIP y los flujos de asignación; documentar por separado cualquier validación operativa realizada en Keycloak.
- [X] T015 Ejecutar `git diff --check` desde `F:/work-space/midagri/piip-monorepo` y registrar el resultado sin sustituir pruebas funcionales.

## Dependencias y orden

- T001-T004 preceden a T005-T007.
- T007 precede a T008; T008 precede a T009-T011.
- T012-T014 siguen la implementación y requieren autorización independiente.

## Oportunidades de paralelización

- T003 y T004 pueden avanzar en paralelo al limitarse a repositorios distintos.
- T005 puede avanzar en paralelo con T003-T004 una vez acordadas las firmas finales de T001-T002.
- T010 puede avanzar en paralelo con T009 después de T008.

## Estrategia incremental

1. Retirar primero la semántica local en backend y comprobar sus pruebas focalizadas.
2. Publicar contrato y regenerar el cliente únicamente después de la publicación autorizada.
3. Simplificar la UI y validar que la gestión de asignaciones permanece intacta.
