# Tasks: Autorización exacta por rol y ámbito

**Input**: `specs/008-administrar-usuarios/{spec.md,plan.md,research.md,data-model.md,contracts/user-administration-http.md,quickstart.md}`

**Baseline**: T001-T015 de la iteración anterior están completadas. El CRUD de asignaciones, candidatos, retiro del estado local y cliente actual no se reimplementan. Las tareas pendientes cubren exclusivamente la corrección aprobada de autorización exacta.

**Autorización**: `/speckit-implement` autoriza únicamente T016-T026, T028-T034 y T037-T038. T027, T035 y T036 requieren autorización explícita adicional para OpenAPI, generación, pruebas y E2E según se indica en cada tarea.

## Phase 1: Baseline completado

T001-T015 permanecen completadas en el historial de esta feature: Keycloak como autoridad de cuenta, CRUD de asignaciones, candidatos, contrato, cliente, UI y validaciones anteriores.

## Phase 2: Fundamento de autorización exacta

**Goal**: conservar la tupla rol y ámbito como unidad canónica antes de adaptar consumidores.

- [X] T016 [US4] Añadir pruebas de regresión que demuestren que Administrador en UE-002 no concede escritura en UE-001 cubierta sólo por Consulta externa en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/{LocalAccessContextTest.java,application/LocalAuthorizationServiceTest.java}`.
- [X] T017 [US4] Crear el valor inmutable `RoleScopeGrant` y convertir `LocalAccessContext` para conservar grants exactos y proyecciones derivadas en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/{RoleScopeGrant.java,LocalAccessContext.java}`.
- [X] T018 [US4] Mapear asignaciones activas a grants y corregir `requireUnit`/`requireReadableUnit` en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/LocalAuthorizationService.java`.
- [X] T019 [P] [US4] Ajustar mocks y constructores de contexto afectados sin alterar reglas funcionales en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/{LocalAccessContextTest.java,LocalAuthorizationConcurrencyTest.java,application/UserAdministrationServiceTest.java}`.

## Phase 3: Consumidores backend por ámbito

**Goal**: impedir el producto cartesiano entre roles y coberturas en todos los casos sensibles.

- [X] T020 [P] [US4] Aplicar rol Administrador exacto en escrituras e iniciativas elegibles en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java` y crear `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioAuthorizationTest.java`.
- [X] T021 [P] [US4] Aplicar rol exacto para escritura, visibilidad interna y descarga de documentos en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` y crear `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentAuthorizationTest.java`.
- [X] T022 [P] [US4] Filtrar y autorizar tareas administrativas por la UE del registro en `apps/backend/src/main/java/pe/gob/midagri/piip/{work/api/WorkController.java,dashboard/api/DashboardController.java}` y crear `apps/backend/src/test/java/pe/gob/midagri/piip/{work/api/WorkControllerTest.java,dashboard/api/DashboardControllerTest.java}`.
- [X] T023 [US4] Limitar listado, origen, destino y cobertura institucional exclusivamente a grants Administrador en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java` y ampliar `UserAdministrationServiceTest.java`.
- [X] T024 [US4] Revisar estáticamente todos los usos de `require(RoleCode.ADMINISTRADOR_PIIP)`, `hasRole` y cobertura para confirmar que no queda una combinación sensible rol-ámbito fuera del alcance documentado en `apps/backend/src/main/java/`.

## Phase 4: Contrato de identidad exacta

**Goal**: entregar al frontend grants suficientes para representar permisos sin inferencias agregadas.

- [X] T025 [US4] Añadir `RoleScopeResponse` y `roleScopes` compatibles a `/identity/me` en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/IdentityController.java` y crear `apps/backend/src/test/java/pe/gob/midagri/piip/identity/api/IdentityControllerTest.java` para cubrir únicamente asignaciones activas y vigentes.
- [X] T026 [US4] Actualizar la aserción contractual para `roleScopes` en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java` sin publicar todavía el artefacto.
- [X] T027 Con autorización explícita, ejecutar `OpenApiGenerationTest`, verificar `roleScopes` y regenerar `apps/frontend/src/app/api/generated/` mediante `npm run api:generate`, revisando `removeStaleFiles` sin editar generated manualmente.

## Phase 5: Contexto y acciones Angular

**Goal**: mostrar y usar el rol de la UE correcta en navegación, rutas y acciones.

- [X] T028 [US4] Adaptar el modelo de identidad generado a los tipos de presentación en `apps/frontend/src/app/core/piip.models.ts`, manteniendo los campos agregados sólo por compatibilidad.
- [X] T029 [US4] Incorporar capacidades por grant, rol efectivo por UE y limpieza de cargas privilegiadas en `apps/frontend/src/app/core/{piip.repository.ts,piip-http.repository.ts,piip-mock.repository.ts}`.
- [X] T030 [P] [US4] Separar autorización de Administrador transversal y Administrador de UE activa en `apps/frontend/src/app/core/{administrator.guard.ts,active-scope-administrator.guard.ts}` y `apps/frontend/src/app/app.routes.ts`.
- [X] T031 [US4] Mostrar nombre y rol efectivo de la UE activa, conservar acceso transversal a Administración de usuarios y recalcular al cambiar de UE en `apps/frontend/src/app/layout/{app-shell.component.ts,app-shell.component.html,app-shell.component.scss}`.
- [X] T032 [P] [US4] Sustituir comprobaciones globales por capacidades de la UE seleccionada o de la UE real del registro en `apps/frontend/src/app/pages/{initiatives/initiatives.component.html,initiative-detail/initiative-detail.component.html,initiative-form/initiative-form.component.ts,projects/projects.component.{ts,html},preexisting-project-form/preexisting-project-form.component.html,derived-project-form/derived-project-form.component.html,documents/documents.component.html}`.
- [X] T033 [US4] Filtrar instituciones, UE y `Toda la institución` exclusivamente con grants Administrador en `apps/frontend/src/app/pages/user-administration/user-administration.component.{ts,html}`.
- [X] T034 [US4] Ampliar pruebas de repositorio, guards, shell y pantallas en `apps/frontend/src/app/{core/piip-http.repository.spec.ts,core/administrator.guard.spec.ts,core/active-scope-administrator.guard.spec.ts,layout/app-shell.component.spec.ts,pages/initiative-detail/initiative-detail.component.spec.ts,pages/projects/projects.component.spec.ts,pages/documents/documents.component.spec.ts,pages/user-administration/user-administration.component.spec.ts}`.

## Phase 6: Publicación y validación autorizada

- [X] T035 Con autorización explícita, ejecutar pruebas backend focalizadas y `npm test -- --watch=false`, distinguiendo fallos propios y ajenos.
- [X] T036 Con autorización explícita, ejecutar el recorrido E2E reversible de `specs/008-administrar-usuarios/quickstart.md` con Consulta externa en UE-001 y Administrador PIIP en UE-002.
- [X] T037 Ejecutar `git diff --check` y revisar que no se modificaron entidades JPA, esquema, Keycloak ni semántica global de auditoría.
- [X] T038 Ejecutar `graphify update .` después de los cambios materiales de código y antes del checkpoint de sesión.

## Dependencias

- T016 precede a T017-T018; T017-T018 bloquean T020-T025.
- T020-T024 pueden avanzar en paralelo después de T018, salvo archivos de pruebas compartidos.
- T025-T026 preceden a T027; T027 precede a toda adaptación Angular T028-T034.
- T028-T034 dependen del cliente generado por T027.
- T035-T038 siguen a toda la implementación y conservan sus autorizaciones independientes.

## Prueba independiente

La historia US4 se considera implementada cuando la misma cuenta puede leer UE-001 como Consulta externa, no puede escribir ni administrar allí, conserva Administrador PIIP en UE-002 y la interfaz refleja ambos contextos sin arrastrar privilegios.
