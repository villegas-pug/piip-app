# Tareas: Consolidación de asignaciones de usuarios

**Entrada**: documentos de `/specs/014-consolidar-asignaciones-usuarios/`

**Prerrequisitos**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/user-assignment-administration.openapi.yaml` y `quickstart.md` vigentes; checklist completo; sin `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI/cliente/DDL, servidores, Oracle ni Git. La invocación explícita de `/speckit-implement` autoriza únicamente las tareas de implementación de la feature activa; las acciones marcadas con autorización requerida necesitan una instrucción adicional del usuario en ese turno.

## Formato obligatorio

Cada tarea usa `- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta exacta`. `[P]` identifica trabajo ejecutable en paralelo solo cuando no comparte archivo, contrato o dependencia pendiente.

## Evidencia de baseline — no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| US1 / FR-004–FR-011 | La edición conserva el ID, recibe rol/institución/UE, valida versión, estado, cobertura y duplicado activo. | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/{api/UserAdministrationController.java,application/UserAdministrationService.java}` | Reforzar cobertura por cada UE, locks, discriminadores y auditoría; no reimplementar la ruta. |
| US2 / FR-013–FR-020 | La suspensión/reactivación reversible y `@Version` ya existen; no se elimina la fila. | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/{application/UserAdministrationService.java,persistence/UserRoleScopeEntity.java}` | Añadir autosuspensión administrativa incondicional, estados codificados y concurrencia consolidada. |
| US3 / FR-021–FR-024 | La UI refresca autorización después de suspensión/reactivación propia, pero no después de editar y conserva datos ante fallo. | `apps/frontend/src/app/pages/user-administration/user-administration.component.ts` | Generalizar el refresco y aplicar recuperación fail-closed. |
| US4 / FR-028–FR-035 | `ApiExceptionHandler` usa `ProblemDetail`; acceso es `REQUIRES_NEW` y eventos participan en la transacción. | `apps/backend/src/main/java/pe/gob/midagri/piip/{shared/api/ApiExceptionHandler.java,audit/application/AuditService.java}` | Añadir `problemCode`, motivo seguro y snapshots completos sin crear eventos de rechazo. |
| US5 / FR-012 y FR-037 | `assign()` bloquea el usuario y rechaza duplicado activo, pero siempre crea y el controller fija `201`. | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/{application/UserAdministrationService.java,api/UserAdministrationController.java}` | Buscar la coincidencia suspendida más reciente y devolver `200` al reactivarla. |
| FR-024–FR-027 y FR-036 | Angular ya muestra Editar/Suspender/Reactivar por estado, pero el componente llama directamente al cliente generado. | `apps/frontend/src/app/pages/user-administration/`, `apps/frontend/src/app/api/generated/services/user-administration-controller.service.ts` | Encapsular mutaciones en `PiipRepository` y conservar backend autoritativo. |

## Phase 1: Preparación compartida

**Propósito**: fijar fronteras de capas y tipos internos antes de modificar reglas o consumidores.

- [X] T001 [FR-032] Crear commands, read models, `AssignmentMutationResult` y snapshots de asignación en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationCommands.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationReadModels.java`
- [X] T002 [P] [FR-032] Ampliar primero la regla que rechaza dependencias de `application` hacia `api` y exposición de entidades en `apps/backend/src/test/java/pe/gob/midagri/piip/architecture/ApplicationBoundaryTest.java`
- [X] T003 [FR-032] Crear el adaptador DTO ↔ application en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/UserAdministrationHttpMapper.java` y adaptar las firmas compartidas de `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/UserAdministrationController.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java` hasta satisfacer T002 sin conservar imports `application -> api`
- [X] T004 [P] [FR-024] Definir inputs/resultados de mutación, las cuatro firmas de escritura, lecturas de usuarios/candidatos y stubs fail-fast en `apps/frontend/src/app/core/piip.models.ts`, `apps/frontend/src/app/core/piip.repository.ts` y `apps/frontend/src/app/core/piip-mock.repository.ts`; las implementaciones mock se completan con cada historia

**Checkpoint**: application ya no depende de DTO HTTP y Angular dispone de una frontera propia sin editar archivos generados.

---

## Phase 2: Fundamento bloqueante de autorización, concurrencia y error seguro

**Propósito**: establecer los códigos transversales, el modelo JPA de auditoría y el orden de locks usado por todas las historias.

- [X] T005 [FR-034] Agregar primero pruebas de valores/compatibilidad en `apps/backend/src/test/java/pe/gob/midagri/piip/shared/application/error/ProblemCodeTest.java` y luego definir los códigos `INVALID_REQUEST`, `FORBIDDEN_SCOPE`, `RESOURCE_NOT_FOUND`, `STALE_VERSION`, `ACTIVE_ASSIGNMENT_DUPLICATE`, `SELF_ADMIN_SUSPENSION`, `LAST_ACTIVE_ADMIN`, `INCOMPATIBLE_ASSIGNMENT_STATE`, `INVALID_ACTIVE_REFERENCE` y `BUSINESS_RULE_VIOLATION` en `apps/backend/src/main/java/pe/gob/midagri/piip/shared/application/error/ProblemCode.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/shared/application/error/BusinessRuleException.java` y excepciones relacionadas, conservando constructores compatibles
- [X] T006 [P] [FR-031] Agregar primero la prueba de persistencia/lectura de `safeReason` en `apps/backend/src/test/java/pe/gob/midagri/piip/audit/persistence/AccessAuditRepositoryTest.java` y luego mapearlo nullable a `MOTIVO_SEGURO` sin índice nuevo en `apps/backend/src/main/java/pe/gob/midagri/piip/audit/persistence/AccessAuditEntity.java`
- [X] T007 [P] [FR-002] Crear pruebas JPA para grants vigentes, administradores por UE y orden de locks en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/persistence/UserRoleScopeRepositoryTest.java`
- [X] T008 [P] [FR-002] Crear caracterización de revocación concurrente del actor y orden actor/destinatario en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/UserAdministrationConcurrencyTest.java`
- [X] T009 [FR-002] Añadir lock pesimista conjunto de usuarios por colección de IDs ascendentes en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/persistence/UserRepository.java`, dejando la localización previa de IDs como lectura sin lock
- [X] T010 [FR-002] Consolidar consultas bloqueantes de grants vigentes del actor y administradores ordenados por UE/scope en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/persistence/UserRoleScopeRepository.java`
- [X] T011 [FR-001] Aplicar en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java` el orden localizar IDs → bloquear actor/destinatario juntos por ID ascendente → releer grants del actor bajo lock → autorizar → bloquear scope/cobertura (depende de T009 y T010)

**Checkpoint**: toda mutación parte de autorización persistida serializada y dispone de códigos estables; `USUARIO_ROL_AMBITO` conserva su esquema.

---

## Phase 3: Backend — User Story 1, editar una asignación activa (P1)

**Objetivo**: conservar el ID y modificar solo rol, institución o UE, protegiendo versión, duplicado y cobertura posterior de cada UE.

**Independent Test**: editar una asignación propia o ajena con versión vigente conserva ID y deja snapshot antes/después; duplicado, versión obsoleta o pérdida de la última cobertura rechazan sin cambios.

- [X] T012 [P] [US1] Agregar pruebas de servicio para edición propia/ajena, campos permitidos, ID estable, duplicado, estado incompatible, referencias y versión en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/application/UserAdministrationServiceTest.java`
- [X] T013 [P] [US1] Agregar pruebas MockMvc/WebMvc de request, `version`, status, `Content-Type` y cuerpo `200 ScopeResponse` en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/api/UserAdministrationControllerTest.java`
- [X] T014 [US1] Cubrir edición de grant institucional, varias UEs y dos mutaciones que compiten por la última cobertura en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/UserAdministrationConcurrencyTest.java`
- [X] T015 [US1] Implementar en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/identity/persistence/UserRoleScopeRepository.java` la cobertura posterior: UEs activas por ID, scopes administradores por ID y cálculo `actuales - scope mutado + scope destino si aún cubre la UE`, incluida asignación institucional
- [X] T016 [US1] Consolidar `update` con command/read model, validaciones codificadas y evento `USUARIO_ROL_AMBITO` con ID, usuario, `before`, `after` y `SUCCESS` en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java`

**Checkpoint**: US1 queda demostrable desde servicio y controller sin modificar otra información del usuario.

---

## Phase 4: Backend — User Story 2, suspender y reactivar con seguridad (P1)

**Objetivo**: aplicar transiciones reversibles, autosuspensión administrativa prohibida, versión, cobertura y referencias activas.

**Independent Test**: suspender y reactivar una asignación ajena conserva ID; suspender `CONSULTA_EXTERNA` propia es válido con otro grant administrador y autosuspender `ADMINISTRADOR_PIIP` propio siempre devuelve la regla específica.

- [X] T017 [P] [US2] Agregar pruebas de servicio para estados, versión, asignación propia/ajena, ambos roles, duplicado y referencias de reactivación en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/application/UserAdministrationServiceTest.java`
- [X] T018 [P] [US2] Agregar pruebas MockMvc/WebMvc de status, `Content-Type`, cuerpo/ausencia de cuerpo y `problemCode` para `DELETE 204`, reactivación `200` y errores `403/404/409/422` en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/api/UserAdministrationControllerTest.java`
- [X] T019 [US2] Cubrir dos suspensiones concurrentes sobre la misma UE y un grant institucional que protege varias UEs en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/UserAdministrationConcurrencyTest.java`
- [X] T020 [US2] Implementar `suspend` con versión, estado, cobertura posterior y rechazo incondicional `SELF_ADMIN_SUSPENSION` para el scope administrador propio en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java`
- [X] T021 [US2] Implementar `reactivate` con versión, estado compatible, referencias activas, ámbito, duplicado y snapshot funcional completo en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java`

**Checkpoint**: US2 conserva identidad/historia y no toca cuenta Keycloak ni `USUARIO.ACTIVO`.

---

## Phase 5: Backend — User Story 4, errores y auditoría consistentes (P2)

**Objetivo**: publicar errores discriminados, auditar rechazos solo por acceso HTTP y confirmar exactamente un evento funcional por éxito.

**Independent Test**: cada categoría `400/403/404/409/422` entrega `problemCode`; un rechazo conserva `MOTIVO_SEGURO` y cero eventos funcionales, mientras un éxito confirma mutación/evento juntos o revierte ambos.

- [X] T022 [P] [US4] Agregar pruebas de `ProblemDetail.problemCode`, propiedades de referencia y atributo seguro de request para todos los estados controlados en `apps/backend/src/test/java/pe/gob/midagri/piip/shared/api/ApiExceptionHandlerTest.java`
- [X] T023 [P] [US4] Crear pruebas del filtro para atributo `problemCode` y fallback seguro derivado solo de status, y ampliar proyección/controller de accesos con `safeReason`, en `apps/backend/src/test/java/pe/gob/midagri/piip/audit/api/AccessAuditFilterTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/audit/application/AuditQueryServiceTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/audit/api/AuditControllerTest.java`
- [X] T024 [P] [US4] Agregar pruebas de exactamente un evento por éxito para assign/update/suspend/reactivate, cero eventos funcionales por rechazo y rollback conjunto ante fallo de auditoría; afirmar actor, acción, `entityType=USUARIO_ROL_AMBITO`, `codigoEntidad=scope.id`, usuario afectado, snapshot anterior, snapshot posterior, resultado y fecha en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/application/UserAdministrationServiceTest.java` y `UserAdministrationTransactionalAuditTest.java`
- [X] T025 [US4] Publicar `problemCode` y colocar el mismo código seguro como atributo de request sin persistir `detail` en `apps/backend/src/main/java/pe/gob/midagri/piip/shared/api/ApiExceptionHandler.java`
- [X] T026 [US4] Propagar `safeReason` mediante `apps/backend/src/main/java/pe/gob/midagri/piip/audit/api/AccessAuditFilter.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditService.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditReadModels.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditQueryService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/audit/api/AuditController.java`; para respuesta `>=400` sin atributo usar código genérico por status y nunca `detail`, body o excepción
- [X] T027 [US4] Unificar eventos de assign/update/suspend/reactivate como `USUARIO_ROL_AMBITO`, con `codigoEntidad=scope.id`, actor, acción, usuario afectado, snapshots completos de rol/institución/Unidad Ejecutora/estado/vigencia, `result=SUCCESS` y fecha generada por la auditoría en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java`
- [X] T028 [US4] Añadir assertions OpenAPI estructurales en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java`: `problemCode` string requerido con valores publicados, `safeReason` nullable, errores `application/problem+json` con `$ref` a `ProblemDetail`, versión y cuerpos de mutación
- [X] T029 [US4] Documentar respuestas `application/problem+json`, schema requerido `ProblemDetail.problemCode` y `AccessResponse.safeReason` en `apps/backend/src/main/java/pe/gob/midagri/piip/config/OpenApiConfig.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/UserAdministrationController.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/AdminDtos.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/audit/api/AuditController.java`

**Checkpoint**: US4 backend distingue errores sin parsing de texto y conserva fronteras transaccionales de auditoría.

---

## Phase 6: Backend — User Story 5, reutilizar coincidencia suspendida (P2)

**Objetivo**: reactivar la coincidencia exacta suspendida más reciente y diferenciar `200` de una creación `201`.

**Independent Test**: con historia suspendida se conserva el ID elegido por `VIGENTE_HASTA DESC, ID DESC`, no crece el historial y POST devuelve `200`; sin historia crea y devuelve `201`; concurrencia deja una sola activa.

- [X] T030 [P] [US5] Agregar pruebas JPA de combinación exacta con UE null-safe, `active=false`, `validUntil is not null`, lock pesimista y orden `validUntil DESC, id DESC` en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/persistence/UserRoleScopeRepositoryTest.java`
- [X] T031 [P] [US5] Agregar pruebas de servicio y concurrencia para auto-reactivación, duplicado activo, cero filas nuevas y dos POST simultáneos en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/application/UserAdministrationServiceTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/identity/UserAdministrationConcurrencyTest.java`
- [X] T032 [P] [US5] Agregar pruebas MockMvc/WebMvc de status, `Content-Type` y cuerpo `ScopeResponse` para `201 CREATED` y `200 REACTIVATED` en `apps/backend/src/test/java/pe/gob/midagri/piip/identity/api/UserAdministrationControllerTest.java`
- [X] T033 [US5] Implementar en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/persistence/UserRoleScopeRepository.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java` la consulta JPQL `PESSIMISTIC_WRITE` exacta/null-safe, `active=false`, `validUntil is not null`, `ORDER BY validUntil DESC, id DESC`, serializada por lock del usuario; emitir `ROL_REACTIVADO` con ID/before/after/`SUCCESS` al reutilizar y `ROL_ASIGNADO` al crear
- [X] T034 [US5] Retirar el `@ResponseStatus(CREATED)` fijo y mapear `CREATED` a `201` y `REACTIVATED` a `200`, ambos con cuerpo, en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/UserAdministrationController.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/UserAdministrationHttpMapper.java`

**Checkpoint**: US5 backend no duplica historia exacta y el resultado HTTP representa la operación real.

---

## Phase 7: Gate contractual backend → Angular

**Propósito**: publicar el contrato backend estabilizado y regenerar el consumidor antes de usar los nuevos tipos. Estas tareas requieren autorización explícita adicional.

- [X] T035 [US1] [US2] [US4] [US5] Ejecutar `gradlew.bat test --tests "pe.gob.midagri.piip.contract.OpenApiGenerationTest"` desde `apps/backend` para publicar `apps/backend/target/piip-openapi.json` — autorización explícita requerida
- [X] T036 [US1] [US2] [US4] [US5] Revisar estructuralmente `apps/backend/target/piip-openapi.json` generado en el mismo checkout/ejecución de T035 contra `specs/014-consolidar-asignaciones-usuarios/contracts/user-assignment-administration.openapi.yaml`, confirmando freshness, `problemCode`, `safeReason`, `200/201/204` y schemas sin entidades JPA
- [X] T037 [US1] [US2] [US4] [US5] Ejecutar `npm run api:generate` desde `apps/frontend` con `apps/frontend/ng-openapi-gen.json` y el OpenAPI revisado — autorización explícita requerida; no editar manualmente `apps/frontend/src/app/api/generated/`
- [X] T038 [US1] [US2] [US4] [US5] Revisar el diff generado en `apps/frontend/src/app/api/generated/models/problem-detail.ts`, modelos/funciones/exports de auditoría y todos los artefactos de `apps/frontend/src/app/api/generated/fn/user-administration-controller/`, incluido `apps/frontend/src/app/api/generated/services/user-administration-controller.service.ts`; confirmar `assign$Response()`, cuerpos tipados para update/reactivate y `200/201`, semántica `204` de suspend y ausencia de edición manual o workarounds ajenos al generador

**Checkpoint**: el cliente generado refleja el backend antes de adaptar mensajes y resultado dual de asignación.

---

## Phase 8: Frontend — User Story 1, editar una asignación activa (P1)

**Objetivo**: ejecutar lecturas y edición mediante `PiipRepository`, retirar de esos flujos el consumo directo del cliente generado, conservar temporalmente `assign` en el componente hasta T052 y reconciliar bandeja/detalle con la respuesta confirmada.

**Independent Test**: el diálogo cambia solo rol/institución/UE, envía ID/versión vigentes y, solo después de validar `ScopeResponse`, actualiza la misma fila en bandeja y detalle para edición propia o ajena.

- [X] T039 [P] [US1] Agregar pruebas de lecturas y edición HTTP, request exacto y respuesta `ScopeResponse` en `apps/frontend/src/app/core/piip-http.repository.spec.ts`
- [X] T040 [P] [US1] Agregar pruebas de edición propia/ajena, versión, ausencia de actualización previa a la respuesta y reconciliación de bandeja/candidatos/detalle en `apps/frontend/src/app/pages/user-administration/user-administration.component.spec.ts`
- [X] T041 [US1] Implementar lecturas `users`/`assignmentCandidates` y `updateUserAssignment` en `apps/frontend/src/app/core/piip-http.repository.ts`, completar el mock de update en `apps/frontend/src/app/core/piip-mock.repository.ts`, retirar del componente el consumo directo de `UserAdministrationControllerService` para `load()`/`saveEdit()` y conservarlo temporalmente solo para `assign` hasta T052; migrar esas lecturas y la edición a `PIIP_REPOSITORY`, aplicando o recargando el `ScopeResponse` confirmado en `apps/frontend/src/app/pages/user-administration/user-administration.component.ts`

**Checkpoint**: US1 es usable; lecturas y edición ya no consumen directamente el cliente generado, mientras `assign` permanece temporalmente en el componente hasta T052.

---

## Phase 9: Frontend — User Story 2, suspender y reactivar con seguridad (P1)

**Objetivo**: reflejar la matriz autoritativa, prevenir accesiblemente la autosuspensión administrativa y conservar la autosuspensión permitida de Consulta externa.

**Independent Test**: en fila única y detalle expandido, el scope administrador propio conserva una acción visible con explicación accesible y cero DELETE; Consulta externa propia sí puede suspenderse, y reactivar exige respuesta `200` con ID/versión confirmados.

- [X] T042 [P] [US2] Agregar pruebas HTTP de `suspendUserAssignment` como `204` sin cuerpo y `reactivateUserAssignment` como `200 ScopeResponse`, incluido ID/versión y errores `application/problem+json`, en `apps/frontend/src/app/core/piip-http.repository.spec.ts`
- [X] T043 [P] [US2] Agregar pruebas de acciones por estado/propiedad en fila única y detalle expandido; verificar explicación accesible y cero petición para autosuspensión administrativa, suspensión válida de `CONSULTA_EXTERNA` propia y reconciliación autoritativa de bandeja, candidatos y detalle después de suspender o reactivar asignaciones propias y ajenas en `apps/frontend/src/app/pages/user-administration/user-administration.component.spec.ts`
- [X] T044 [US2] Implementar suspensión/reactivación en `apps/frontend/src/app/core/piip-http.repository.ts` y `apps/frontend/src/app/core/piip-mock.repository.ts`, migrar esas llamadas desde `apps/frontend/src/app/pages/user-administration/user-administration.component.ts` y completar en esta tarea la reconciliación después de cada éxito sin depender de tareas posteriores: para objetivos ajenos, recargar bandeja, candidatos y detalle; para objetivos propios, preservar el refresco de autorización vigente, revalidar la Unidad Ejecutora original y recargar únicamente si continúa autorizada; no actualizar signals antes del `204` confirmado o del `200 ScopeResponse` validado
- [X] T045 [US2] Mantener visible pero no ejecutable la autosuspensión `ADMINISTRADOR_PIIP` propia con explicación accesible asociada, sin ocultar otras acciones válidas, en `apps/frontend/src/app/pages/user-administration/user-administration.component.html` y `apps/frontend/src/app/pages/user-administration/user-administration.component.scss`

**Checkpoint**: US2 queda coherente en UI y backend, manteniendo al servidor como autoridad definitiva.

---

## Phase 10: Frontend — User Story 3, actualizar el acceso del actor (P1)

**Objetivo**: refrescar identidad/UE/navegación tras toda mutación propia, mantener bloqueada la UI durante la cadena completa y fallar de forma cerrada cuando no se puede confirmar el nuevo contexto.

**Independent Test**: creación, edición, suspensión y reactivación propias mantienen `operationPending` hasta terminar refresco/revalidación/recarga; pérdida de la UE original o fallo limpia Administración, navega a `/inicio` y deja un aviso persistente con retry sin doble clic ni repoblación prematura.

- [X] T046 [P] [US3] Agregar pruebas de las cuatro mutaciones propias, captura de UE/ruta original, bloqueo hasta completar toda la cadena, pérdida de acceso aunque se seleccione otra UE y cero cargas protegidas tras fallo en `apps/frontend/src/app/pages/user-administration/user-administration.component.spec.ts`
- [X] T047 [P] [US3] Agregar pruebas de limpieza del contexto/repositorio, aviso persistente después de navegar y retry exclusivo hasta rehidratación exitosa en `apps/frontend/src/app/core/authorization-recovery.service.spec.ts` y `apps/frontend/src/app/core/piip-http.repository.spec.ts`
- [X] T048 [US3] Generalizar el flujo propio ya preservado por T044 en una cadena única y agnóstica al transporte para las cuatro mutaciones de UI; capturar ruta y UE antes del request, mantener `operationPending`, ejecutar `refreshAuthorizationContext()`, revalidar la UE original y recargar solo si continúa autorizada en `apps/frontend/src/app/pages/user-administration/user-administration.component.ts`; permitir que `assign` conserve temporalmente el cliente generado hasta T052 sin duplicar la coordinación
- [X] T049 [US3] Crear recuperación persistente con aviso accesible y acción `Reintentar` en `apps/frontend/src/app/core/authorization-recovery.service.ts`; ante fallo limpiar usuarios/candidatos/ámbitos, navegar a `/inicio`, bloquear doble retry y no repoblar Administración hasta rehidratación exitosa desde `apps/frontend/src/app/pages/user-administration/user-administration.component.ts`

**Checkpoint**: no quedan acciones administrativas basadas en el contexto anterior después de una mutación propia.

---

## Phase 11: Frontend — User Story 5, resultado de creación o auto-reactivación (P2)

**Objetivo**: migrar el alta al repositorio antes del mapper transversal de errores, consumir la respuesta HTTP completa y reconciliar la asignación elegida por backend.

**Independent Test**: POST `201` informa creación, POST `200` informa reactivación del ID devuelto; ambos casos propios refrescan identidad antes de recargar, y cualquier 2xx no contemplado o cuerpo ausente falla sin modificar la UI.

- [X] T050 [P] [US5] Agregar pruebas de `assign$Response()` para `201`, `200`, cuerpo ausente, 2xx inesperado y cero reconciliación previa a validar la respuesta en `apps/frontend/src/app/core/piip-http.repository.spec.ts`
- [X] T051 [P] [US5] Agregar pruebas de creación propia `201` y auto-reactivación propia `200`: refresco previo, ID/versión confirmados, mensajes, bandeja/candidatos/detalle reconciliados en `apps/frontend/src/app/pages/user-administration/user-administration.component.spec.ts`
- [X] T052 [US5] Implementar `assignUserRole` mediante `assign$Response()` en `apps/frontend/src/app/core/piip-http.repository.ts` y `apps/frontend/src/app/core/piip-mock.repository.ts`, mapear `201 → CREATED` y `200 → REACTIVATED`, exigir cuerpo y rechazar otro 2xx antes de modificar signals; migrar alta/mensajes/recarga en `apps/frontend/src/app/pages/user-administration/user-administration.component.ts`, reutilizar sin cambios la coordinación propia de T048 y retirar la última dependencia directa de `UserAdministrationControllerService`

**Checkpoint**: US5 completa backend y frontend sin inferir el resultado por el estado previo de la bandeja.

---

## Phase 12: Frontend — User Story 4, errores y auditoría consistentes (P2)

**Objetivo**: presentar errores accionables por código estable y hacer observable el motivo seguro de accesos rechazados.

**Independent Test**: todos los códigos publicados muestran mensajes estables para errores JSON, texto o Blob; un código desconocido usa `detail` seguro más status como fallback, y Auditoría presenta `safeReason` sin inferirlo del cuerpo.

- [X] T053 [P] [US4] Agregar pruebas de decoder para `HttpErrorResponse.error` como JSON, texto y Blob, y de la matriz `INVALID_REQUEST`, `FORBIDDEN_SCOPE`, `RESOURCE_NOT_FOUND`, `STALE_VERSION`, `ACTIVE_ASSIGNMENT_DUPLICATE`, `SELF_ADMIN_SUSPENSION`, `LAST_ACTIVE_ADMIN`, `INCOMPATIBLE_ASSIGNMENT_STATE`, `INVALID_ACTIVE_REFERENCE` y `BUSINESS_RULE_VIOLATION`; un código conocido nunca depende de `detail` y uno desconocido usa fallback seguro en `apps/frontend/src/app/core/piip-http.repository.spec.ts` y `apps/frontend/src/app/pages/user-administration/user-administration.component.spec.ts`
- [X] T054 [P] [US4] Agregar pruebas de presentación nullable del motivo seguro de accesos en `apps/frontend/src/app/pages/audit/audit.component.spec.ts`
- [X] T055 [US4] Incorporar `problemCode` y `safeReason` a modelos de presentación, deserializar estructuralmente JSON/texto/Blob sin discriminar por `detail` y adaptar accesos en `apps/frontend/src/app/core/piip.models.ts` y `apps/frontend/src/app/core/piip-http.repository.ts`
- [X] T056 [US4] Mostrar mensajes por `problemCode`, recarga accionable y fallback seguro para código desconocido en `apps/frontend/src/app/pages/user-administration/user-administration.component.ts`, y presentar `safeReason` en `apps/frontend/src/app/pages/audit/audit.component.ts` y `apps/frontend/src/app/pages/audit/audit.component.html`

**Checkpoint**: US4 es coherente entre `ProblemDetail`, auditoría persistida y mensajes Angular.

---

## Phase 13: Documentación y cierre estático

- [X] T057 [FR-036] Actualizar auto-reactivación, autosuspensión, cobertura por UE, errores, auditoría y refresco propio en `docs/funcional/guia-funcional-piip.md`
- [X] T058 [P] [FR-032] Revisar estáticamente que no exista `identity.application -> identity.api`, exposición de entidades, SQL nativo, cambios Keycloak/`USUARIO.ACTIVO` ni edición manual del generado en `apps/backend/src/main/java/`, `apps/frontend/src/app/api/generated/` y `database/generated/piip-oracle.sql`
- [X] T059 [FR-035] Actualizar `specs/014-consolidar-asignaciones-usuarios/quickstart.md` únicamente con tareas realmente completadas, validaciones ejecutadas y límites pendientes, sin declarar runtime no observado
- [X] T060 [FR-036] Ejecutar `graphify update .` desde la raíz `F:\work-space\piip-monorepo` después de los cambios materiales de código y antes del checkpoint de sesión
- [X] T061 [FR-036] Ejecutar `git diff --check` y revisar `git status --short` desde `F:\work-space\piip-monorepo`, preservando cambios ajenos y registrando el estado sin commit/push

---

## Validaciones propuestas — requieren autorización

- [X] T062 [US1] [US2] [US4] [US5] Ejecutar `gradlew.bat test --tests "pe.gob.midagri.piip.identity.*" --tests "pe.gob.midagri.piip.audit.*" --tests "pe.gob.midagri.piip.shared.api.*" --tests "pe.gob.midagri.piip.contract.OpenApiGenerationTest"` desde `apps/backend` — autorización explícita requerida
- [X] T063 [FR-032] Ejecutar `gradlew.bat check` desde `apps/backend` para incluir reglas de arquitectura y regresión backend — autorización explícita requerida
- [X] T064 [FR-031] Ejecutar `gradlew.bat test --tests "pe.gob.midagri.piip.persistence.OracleSchemaGenerationTest"` desde `apps/backend` para generar `apps/backend/target/piip-oracle.sql` — autorización explícita requerida
- [X] T065 [FR-031] Comparar en solo lectura `apps/backend/target/piip-oracle.sql` contra JPA y `database/generated/piip-oracle.sql`, confirmando que el único cambio estructural sea `AUDITORIA_ACCESO.MOTIVO_SEGURO` y que `USUARIO_ROL_AMBITO`, índices y constraints permanezcan intactos
- [X] T066 [FR-031] Sincronizar el DDL derivado confirmado por T065 en `database/generated/piip-oracle.sql` desde el agente principal/propietario documental, sin SQL funcional ni edición estructural independiente de JPA
- [X] T067 [US1] [US2] [US3] [US4] [US5] Ejecutar `npm test -- --watch=false` desde `apps/frontend` después de T039–T056 y registrar pruebas totales/fallos sin ocultar regresiones ajenas — autorización explícita requerida
- [X] T068 [FR-036] Ejecutar `npm run build` desde `apps/frontend` después de T039–T056 y registrar errores o warnings — autorización explícita requerida
- [X] T069 [FR-031] [US1] [US2] [US4] [US5] Ejecutar `gradlew.bat integrationTest --tests "pe.gob.midagri.piip.identity.UserAdministrationOracleIntegrationTest"` desde `apps/backend` con el wallet Oracle de pruebas para verificar `MOTIVO_SEGURO` y locks, sin Docker ni `test-reset` — autorización explícita requerida
- [X] T070 [US1] [US2] [US3] [US4] [US5] Ejecutar el recorrido E2E reversible de asignación, auto-reactivación, edición, suspensión y reactivación definido en `specs/014-consolidar-asignaciones-usuarios/quickstart.md`; confirmar bandeja, detalle, permisos, errores y auditoría, restaurar mediante mutaciones auditables y no borrar eventos — autorización explícita requerida

## Dependencias y orden de ejecución

### Grafo de historias y gates

```text
Phase 1 Preparación
        ↓
Phase 2 Fundamento bloqueante
        ↓
US1 backend → US2 backend
        ↓
US4 backend → US5 backend
        ↓
Gate OpenAPI/cliente autorizado
        ↓
US1 frontend → US2 frontend
        ↓
US3 frontend → US5 frontend
        ↓
US4 frontend
        ↓
Documentación → validaciones autorizadas
```

- **Propietarios canónicos**: T001–T003, T005–T036 son backend; T004 y T037–T056 son frontend; T038 es revisión del consumidor generado coordinada por el agente principal.
- **Consumidores**: T039–T056 dependen del cliente regenerado y revisado en T037–T038; T053–T056 dependen además de que assign ya haya migrado en T052.
- **Gates transversales**: T035–T038, T062 y T069 declaran todas las historias cuya implementación validan; las etiquetas adicionales expresan cobertura y no alteran su fase ni su propietario canónico.
- **Orden obligatorio**: T009–T011 antes de mutaciones; T015 antes de T020; T025–T029 y T033–T034 antes de T035; T035–T038 antes de T039–T056; T041 antes de T044, T044 antes de T048, T048 antes de T052 y T052 antes de T053–T056.
- **Oportunidades paralelas**: T002 con T004; T006–T008; pruebas en archivos distintos T012/T013, T017/T018, T022–T024, T030–T032, T039/T040, T042/T043, T046/T047, T050/T051 y T053/T054.
- **No paralelizar**: backend y frontend que comparten contrato/regla; tareas sobre `UserAdministrationService.java`, `user-administration.component.ts`, OpenAPI o cliente generado.

## Ejemplos de ejecución paralela

### US1

```text
T012 UserAdministrationServiceTest ─┐
T013 UserAdministrationControllerTest ─┼─> T015 → T016
T014 UserAdministrationConcurrencyTest ┘

T039 piip-http.repository.spec.ts ─┐
T040 user-administration.component.spec.ts ─┴─> T041
```

### US2

```text
T017 UserAdministrationServiceTest ─┐
T018 UserAdministrationControllerTest ─┼─> T020 → T021
T019 UserAdministrationConcurrencyTest ┘

T042 piip-http.repository.spec.ts ─┐
T043 user-administration.component.spec.ts ─┴─> T044 → T045
```

### US4 y US5

```text
T022 ApiExceptionHandlerTest ─┐
T023 pruebas de auditoría ─────┼─> T025 → T029
T024 rollback/eventos ─────────┘

T030 pruebas JPA ──────┐
T031 servicio/concurrencia ─┼─> T033 → T034
T032 controller ───────┘
```

## Estrategia de implementación

### MVP recomendado

Completar T001–T041. Debido al gate contractual único definido por el plan, este MVP estabiliza primero backend/OpenAPI/cliente y expone como primer incremento usable solo US1 frontend: edición segura de la misma asignación con cobertura por UE y repositorio Angular.

### Entrega incremental

1. Backend US1, US2, US4 y US5 sobre el fundamento común.
2. Gate contractual OpenAPI/cliente con autorización explícita.
3. MVP usable US1 frontend.
4. US2 frontend para suspensión/reactivación segura.
5. US3 frontend para reconciliación del actor y fail-closed.
6. US5 frontend y luego US4 frontend para que todas las escrituras atraviesen el mapper común.
7. Documentación y validaciones autorizadas.

Cada checkpoint debe conservar historias anteriores operativas y distinguir evidencia estática, pruebas ejecutadas y validaciones pendientes.

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- La feature 008 se usa solo como baseline aprobado de administración integral y grants exactos; no se reabren sus tareas pendientes.
- La feature 013 fue leída por el marcador previo de `AGENTS.md`, pero no aporta tareas ni reglas funcionales a esta feature.
- Dependencias históricas aprobadas: feature 008 y constitución 1.2.0.
- `NEEDS CLARIFICATION`: Ninguna.

## Notas

- Todos los checkboxes permanecen pendientes hasta contar con evidencia del cambio o validación correspondiente.
- No ejecutar automáticamente T035, T037, T062–T064 ni T067–T070: requieren autorización explícita adicional.
- El backend permanece autoritativo; las restricciones Angular son prevención y presentación.
- El cierre no autoriza `git commit`, `git push`, tags ni pull requests.
