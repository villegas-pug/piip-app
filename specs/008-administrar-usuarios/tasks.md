# Tasks: Administración de usuarios por institución

**Input**: `specs/008-administrar-usuarios/{spec.md,plan.md,research.md,data-model.md,contracts/user-administration-http.md,quickstart.md}`

**Baseline**: T001-T046 registran el CRUD, candidatos, retiro del estado local, autorización funcional exacta, cliente actual y acceso por UE activa. No se reimplementan. Las nuevas tareas amplían exclusivamente la cobertura de Administración de usuarios dentro de la institución.

**Autorización**: T044 y T045 conservan su autorización independiente para pruebas y E2E del incremento anterior. T047-T052, T054-T057 y T060 requerirán una nueva invocación explícita de `/speckit-implement`; T053, T058 y T059 requieren además autorización expresa para OpenAPI, generación, pruebas y E2E.

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
- [X] T031 [US4] Implementar el baseline de nombre, rol efectivo y acceso transversal en `apps/frontend/src/app/layout/{app-shell.component.ts,app-shell.component.html,app-shell.component.scss}`; la regla de entrada transversal queda reemplazada por T039-T043.
- [X] T032 [P] [US4] Sustituir comprobaciones globales por capacidades de la UE seleccionada o de la UE real del registro en `apps/frontend/src/app/pages/{initiatives/initiatives.component.html,initiative-detail/initiative-detail.component.html,initiative-form/initiative-form.component.ts,projects/projects.component.{ts,html},preexisting-project-form/preexisting-project-form.component.html,derived-project-form/derived-project-form.component.html,documents/documents.component.html}`.
- [X] T033 [US4] Filtrar instituciones, UE y `Toda la institución` exclusivamente con grants Administrador en `apps/frontend/src/app/pages/user-administration/user-administration.component.{ts,html}`.
- [X] T034 [US4] Ampliar pruebas de repositorio, guards, shell y pantallas en `apps/frontend/src/app/{core/piip-http.repository.spec.ts,core/administrator.guard.spec.ts,core/active-scope-administrator.guard.spec.ts,layout/app-shell.component.spec.ts,pages/initiative-detail/initiative-detail.component.spec.ts,pages/projects/projects.component.spec.ts,pages/documents/documents.component.spec.ts,pages/user-administration/user-administration.component.spec.ts}`.

## Phase 6: Publicación y validación autorizada

- [X] T035 Con autorización explícita, ejecutar pruebas backend focalizadas y `npm test -- --watch=false`, distinguiendo fallos propios y ajenos.
- [X] T036 Con autorización explícita, ejecutar el recorrido E2E reversible de `specs/008-administrar-usuarios/quickstart.md` con Consulta externa en UE-001 y Administrador PIIP en UE-002.
- [X] T037 Ejecutar `git diff --check` y revisar que no se modificaron entidades JPA, esquema, Keycloak ni semántica global de auditoría.
- [X] T038 Ejecutar `graphify update .` después de los cambios materiales de código y antes del checkpoint de sesión.

## Phase 7: Administración de usuarios gobernada por la UE activa

**Baseline histórico completado, ampliado por Phase 8.**

**Goal**: eliminar la contradicción visual entre el rol de la UE activa y el acceso al módulo.

**Independent Test**: con Consulta externa en UE-001 y Administrador PIIP en UE-002, la opción administrativa queda visible pero deshabilitada en UE-001, la URL directa se rechaza y UE-002 habilita la entrada. La cobertura de la bandeja queda redefinida por T047-T060.

- [X] T039 [US4] Añadir pruebas de regresión para acceso directo, opción deshabilitada, ayuda de UE disponible y redirección al cambiar de ámbito en `apps/frontend/src/app/{core/active-scope-administrator.guard.spec.ts,layout/app-shell.component.spec.ts}`.
- [X] T040 [US4] Proteger `/administracion/usuarios` con el guard de la UE activa y emitir el mensaje específico de ámbito requerido en `apps/frontend/src/app/{app.routes.ts,core/active-scope-administrator.guard.ts}`.
- [X] T041 [US4] Mantener visible pero deshabilitada la opción administrativa cuando el actor administre otra UE, indicar las UE disponibles y redirigir al abandonar un ámbito administrable en `apps/frontend/src/app/layout/{app-shell.component.ts,app-shell.component.html,app-shell.component.scss}`.
- [X] T042 [US4] Añadir pruebas del bloque contextual, bandeja transversal y confirmación institucional en `apps/frontend/src/app/pages/user-administration/user-administration.component.spec.ts`.
- [X] T043 [US4] Mostrar la UE administrable usada para ingresar, explicar el alcance transversal y confirmar altas o ediciones para `Toda la institución` en `apps/frontend/src/app/pages/user-administration/{user-administration.component.ts,user-administration.component.html,user-administration.component.scss}`.
- [ ] T044 Con autorización explícita, ejecutar `npm test -- --watch=false` y distinguir cualquier fallo ajeno a esta iteración.
- [ ] T045 Con autorización explícita, ejecutar el recorrido E2E actualizado de `specs/008-administrar-usuarios/quickstart.md` sin persistir cambios institucionales.
- [X] T046 Ejecutar `git diff --check`, confirmar que no se modificaron backend, Oracle, Keycloak, OpenAPI ni generated, y actualizar Graphify antes del checkpoint.

## Phase 8: Cobertura institucional exclusiva para Administración de usuarios

**Goal**: permitir que un Administrador PIIP ingrese únicamente desde una UE donde tenga ese rol y, una vez dentro, gestione asignaciones de todas las UE de la misma institución sin adquirir privilegios funcionales en ellas.

**Independent Test**: con `CONSULTA_EXTERNA · UE-001` y `ADMINISTRADOR_PIIP · UE-002`, UE-001 conserva su rol y bloquea la entrada; desde UE-002 se listan y gestionan asignaciones de UE-001, pero una escritura funcional directa sobre UE-001 continúa devolviendo 403.

- [X] T047 [US4] Añadir pruebas de regresión para cobertura administrativa institucional y separación de capacidades funcionales en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/application/UserAdministrationServiceTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioAuthorizationTest.java`.
- [X] T048 [US4] Derivar instituciones administrables desde grants Administrador persistidos y aplicar esa cobertura únicamente a listado, alta, edición, suspensión y reactivación en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java`.
- [X] T049 [US4] Añadir DTO y `GET /api/v1/admin/users/administrable-scopes` con instituciones y UE activas en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/{AdminDtos.java,UserAdministrationController.java}`.
- [X] T050 [US4] Permitir destinos individuales o institucionales y autoasignación dentro de una institución administrable, conservando duplicados, versión, último administrador, bloqueos y auditoría en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java`.
- [X] T051 [P] [US4] Cubrir endpoint, otra institución, catálogos activos, alcance institucional, autoasignación, concurrencia y auditoría en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/{api/UserAdministrationControllerTest.java,application/UserAdministrationServiceTest.java}`.
- [X] T052 [US4] Actualizar la aserción contractual del nuevo endpoint y DTO en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java`.
- [X] T053 [US4] Con autorización explícita, publicar OpenAPI y regenerar el cliente Angular mediante los comandos canónicos desde `apps/backend/` y `apps/frontend/`, revisando `apps/frontend/src/app/api/generated/` sin editar generated manualmente.
- [X] T054 [US4] Exponer el catálogo administrativo generado mediante los repositorios frontend y modelos de presentación en `apps/frontend/src/app/core/{piip.models.ts,piip.repository.ts,piip-http.repository.ts,piip-mock.repository.ts}`.
- [X] T055 [P] [US4] Mantener el acceso por grant exacto de la UE activa y añadir la regresión UE-001 bloqueada/UE-002 habilitada en `apps/frontend/src/app/{core/active-scope-administrator.guard.spec.ts,layout/app-shell.component.spec.ts}`.
- [X] T056 [US4] Cargar el catálogo administrativo, mostrar todas las UE de la institución, habilitar `Toda la institución` y permitir autoasignación en `apps/frontend/src/app/pages/user-administration/user-administration.component.{ts,html,scss}`.
- [X] T057 [US4] Cubrir bandeja institucional, opciones de UE, confirmación, autoasignación, cambio de UE y errores 403/409/422 en `apps/frontend/src/app/pages/user-administration/user-administration.component.spec.ts` y `apps/frontend/src/app/core/piip-http.repository.spec.ts`.
- [ ] T058 Con autorización explícita, ejecutar pruebas focalizadas desde `apps/backend/` y `apps/frontend/`, distinguiendo fallos propios y ajenos.
- [ ] T059 Con autorización explícita, ejecutar el recorrido E2E actualizado de `specs/008-administrar-usuarios/quickstart.md` y restaurar toda operación reversible.
- [X] T060 Ejecutar `git diff --check`, confirmar ausencia de cambios de esquema/Keycloak y ejecutar `graphify update .` desde la raíz del repositorio antes del checkpoint de sesión.

## Dependencias

- T016 precede a T017-T018; T017-T018 bloquean T020-T025.
- T020-T024 pueden avanzar en paralelo después de T018, salvo archivos de pruebas compartidos.
- T025-T026 preceden a T027; T027 precede a toda adaptación Angular T028-T034.
- T028-T034 dependen del cliente generado por T027.
- T035-T038 siguen a toda la implementación y conservan sus autorizaciones independientes.
- T039 precede a T040-T041; T042 precede a T043.
- T040-T043 completan el incremento y preceden a T044-T046.
- T047 precede a T048-T050; T048-T050 preceden a T051-T052.
- T049-T052 preceden a T053; T053 bloquea T054-T057 por el contrato generado.
- T054 y T055 pueden avanzar en paralelo después de T053; T056 depende de T054 y T057 depende de T055-T056.
- T058-T060 siguen a toda la implementación; T053, T058 y T059 conservan sus autorizaciones explícitas independientes.

## Prueba independiente

La autorización funcional exacta permanece implementada cuando la misma cuenta puede leer UE-001 como Consulta externa, no puede ejecutar escrituras funcionales de Administrador allí, conserva Administrador PIIP en UE-002 y la interfaz refleja ambos contextos sin arrastrar privilegios.

La nueva iteración se considera implementada cuando UE-001 impide abrir Administración de usuarios, UE-002 habilita el módulo y la bandeja permite gestionar asignaciones de todas las UE de MIDAGRI sin alterar el rol operativo mostrado ni autorizar recursos funcionales de UE-001.
