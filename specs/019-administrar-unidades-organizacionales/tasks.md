---
description: "Lista trazable de tareas para la administración de Unidades Ejecutoras y Unidades Orgánicas"
---

# Tareas: Administración de Unidades Organizacionales

**Entrada**: documentos de `/specs/019-administrar-unidades-organizacionales/`

**Prerrequisitos**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/organization-administration.md` y `quickstart.md` vigentes; sin checklists ni `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI ni integración Oracle. La invocación explícita de `/speckit.implement` autoriza las tareas de implementación de la feature activa y aprueba sus artefactos vigentes; las demás acciones requieren autorización separada.

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
| FR-027 / US4 | Consultas organizacionales de catálogo ya publicadas | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationController.java` | Mantener las tres rutas y su semántica. |
| FR-018 / US4 | El catálogo filtra UO activas y con sigla | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/application/OrganizationQueryService.java` | No alterar este filtro al añadir administración. |
| FR-021 / US4 | UE y UO ya poseen control optimista | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/persistence/{ExecutingUnitEntity,OrganizationalUnitEntity}.java` | Reutilizar `@Version`; no reimplementar concurrencia base. |
| FR-033 | Seed de prueba guardado y postvalidado | `apps/backend/src/main/resources/db/test/catalog-data.sql` | Mantener DML idempotente exclusivo de `test,test-reset`. |

## Phase 1: Fundamento compartido backend

**Propósito**: establecer el modelo JPA, autorización, repositorios y auditoría necesarios antes de exponer mutaciones administrativas.

- [X] T001 [FR-010] [FR-030] Ampliar `ExecutingUnitEntity` con `@Column(name = "ORDEN_PRESENTACION")`, `@Column(name = "FECHA_REGISTRO")` y `@Column(name = "FECHA_ACTIVACION")`, orden de presentación entero no negativo, operaciones de ciclo de vida e inmutabilidad de institución/código en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/persistence/ExecutingUnitEntity.java`
- [X] T002 [P] [FR-001] Añadir revalidación de `ADMINISTRADOR_PIIP` con ámbito institucional activo y vigente en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/LocalAuthorizationService.java`
- [X] T003 [P] [FR-022] Extender `AuditEventEntity` con `entityId`, `institutionId`, `executingUnitId` y `organizationalUnitId`, y adaptar la escritura, read models, consultas y respuesta para filtrar siempre por ámbitos autorizados en `apps/backend/src/main/java/pe/gob/midagri/piip/audit/persistence/AuditEventEntity.java`, `AuditEventRepository.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditService.java`, `AuditReadModels.java`, `AuditQueryService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/audit/api/AuditController.java`
- [X] T004 [FR-008] [FR-010] Añadir bloqueos pesimistas, consultas administrativas con activos e inactivos, búsquedas de códigos por ámbito y máximo orden de UE por institución en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/persistence/InstitutionRepository.java`, `ExecutingUnitRepository.java` y `OrganizationalUnitRepository.java` (depende de T001)
- [X] T005 [FR-023] Incorporar códigos de problema administrativos y mapeos `400`, `403`, `404`, `409` y `422` en `apps/backend/src/main/java/pe/gob/midagri/piip/shared/application/error/ProblemCode.java` y `ApiExceptionHandler.java`
- [X] T006 [FR-028] Crear DTO, mapper HTTP, commands y read models administrativos sin entidades JPA, código manipulable ni datos de padre en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationAdministrationDtos.java`, `OrganizationAdministrationHttpMapper.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/organization/application/OrganizationAdministrationCommands.java` y `OrganizationAdministrationReadModels.java` (depende de T001)
- [X] T007 [FR-033] Actualizar los datos sintéticos deterministas de UE/UO y las postvalidaciones fail-closed para `test,test-reset` en `apps/backend/src/main/resources/db/test/catalog-data.sql`, `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetPostValidationTest.java` (depende de T001 y T004)

**Checkpoint**: el modelo JPA, los repositorios, la autorización y los datos sintéticos admiten el contrato administrativo sin habilitar una base Oracle con UE preexistentes.

---

## Phase 2: User Story 1 - Administrar Unidades Ejecutoras (Priority: P1)

**Objetivo**: permitir al administrador institucional listar, crear, editar, desactivar y reactivar UE sin alterar su código ni institución.

**Prueba independiente**: seleccionar una institución administrable, ejecutar el ciclo completo de una UE y comprobar herencia, código generado, fechas, versión y estado sin administrar UO.

- [X] T008 [US1] [FR-006] [FR-010] [FR-021] Definir pruebas de servicio para autorización institucional, alta sin versión, código `UE-<consecutivo>`, fechas, orden no negativo, orden automático máximo más uno (0 inicial), desempate, inmutabilidad y versión obligatoria en edición y estados de UE en `apps/backend/src/test/java/pe/gob/midagri/piip/organization/application/OrganizationAdministrationServiceTest.java` (depende de T001 a T006)
- [X] T009 [US1] [FR-007] [FR-010] [FR-021] [FR-022] Implementar operaciones transaccionales de consulta administrativa, alta sin versión con orden opcional y valor automático máximo más uno, y edición, desactivación y reactivación con versión de UE, lock de institución y los eventos `UE_CREADA`, `UE_ACTUALIZADA`, `UE_DESACTIVADA` y `UE_REACTIVADA` en la misma transacción en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/application/OrganizationAdministrationService.java` (depende de T001 a T006)
- [X] T010 [US1] [FR-028] Exponer instituciones administrables y operaciones de UE bajo `/admin/organization` con payload estricto en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationAdministrationController.java` (depende de T006 y T009)
- [X] T011 [US1] [FR-023] Añadir pruebas HTTP de payload prohibido, herencia de institución, `ProblemDetail` y versión obsoleta para UE en `apps/backend/src/test/java/pe/gob/midagri/piip/organization/api/OrganizationAdministrationControllerTest.java` (depende de T005, T006 y T010)
- [X] T012 [US1] [FR-030] Cubrir columnas, `@Version` y locks de UE en `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/JpaModelTest.java` (depende de T001 y T004)

**Checkpoint**: el contrato de UE devuelve contexto de institución, código generado, estado, orden, fechas y versión, sin aceptar cambios de contexto o código.

---

## Phase 3: User Story 2 - Administrar Unidades Orgánicas (Priority: P1)

**Objetivo**: administrar UO de una UE autorizada en un flujo propio, conservando el catálogo de portafolio y excluyendo toda relación de padre.

**Prueba independiente**: abrir una UE autorizada, crear, editar, desactivar y reactivar una UO desde su administración exclusiva y comprobar que nunca se persiste ni expone `ID_UNIDAD_PADRE`.

- [X] T013 [US2] [FR-014] [FR-016] [FR-017] [FR-021] Ampliar pruebas de servicio con altas UO sin versión, códigos `UO-<consecutivo>` por UE, sigla obligatoria no vacía, estado inicial `active` booleano obligatorio, versión para cambios de UO existente, contexto heredado y exclusión de padre en `apps/backend/src/test/java/pe/gob/midagri/piip/organization/application/OrganizationAdministrationServiceTest.java` (depende de T008 y T009)
- [X] T014 [US2] [FR-015] [FR-016] [FR-021] [FR-022] Implementar en el servicio administrativo la consulta, alta de UO sin versión y con estado inicial explícito, y edición, desactivación y reactivación con versión, lock de UE y los eventos `UO_CREADA`, `UO_ACTUALIZADA`, `UO_DESACTIVADA` y `UO_REACTIVADA` en la misma transacción en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/application/OrganizationAdministrationService.java` (depende de T003, T004, T006 y T009)
- [X] T015 [US2] [FR-025] Exponer las operaciones administrativas de UO sin `parent`, `parentId` ni `ID_UNIDAD_PADRE` en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationAdministrationController.java` y `OrganizationAdministrationDtos.java` (depende de T010 y T014)
- [X] T016 [US2] [FR-016] [FR-017] [FR-018] Añadir pruebas HTTP de UO, rechazo de sigla vacía en alta o edición activa, reactivación sin sigla, ausencia de `parentId` y preservación del catálogo legado en `apps/backend/src/test/java/pe/gob/midagri/piip/organization/api/OrganizationAdministrationControllerTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/organization/application/OrganizationQueryServiceTest.java` (depende de T015)

**Checkpoint**: la administración de UO incluye inactivas dentro del ámbito, no permite cambiar de UE ni manipular padre y conserva la lectura de catálogo vigente.

---

## Phase 4: Contrato canónico y sincronización autorizada

**Propósito**: publicar el contrato backend completo antes de modificar sus consumidores Angular.

- [X] T017 [FR-028] Ampliar aserciones de paths, schemas, campos prohibidos y respuestas de error administrativas en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java` (depende de T010 y T015)
- [X] T018 [FR-028] Publicar el contrato OpenAPI administrativo desde el backend en `apps/backend/target/piip-openapi.json` solo con autorización explícita de generación OpenAPI (depende de T017)
- [X] T019 [FR-028] Regenerar el cliente Angular desde `apps/backend/target/piip-openapi.json` mediante `apps/frontend/ng-openapi-gen.json` solo con autorización explícita, sin editar `apps/frontend/src/app/api/generated/**` manualmente (depende de T018)
- [X] T020 [FR-028] Incorporar modelos administrativos separados y operaciones del cliente sincronizado en `apps/frontend/src/app/core/piip.models.ts`, `piip.repository.ts`, `piip-http.repository.ts` y `piip-mock.repository.ts` (depende de T019)

**Checkpoint**: Angular dispone del cliente generado del contrato administrativo y los modelos legados de catálogo conservan `parentId` aislado de la administración.

---

## Phase 5: User Story 1 - Experiencia Angular de UE (Priority: P1)

**Objetivo**: ofrecer una ruta exclusiva de UE con selección institucional, listado administrativo, formulario y acciones de estado.

**Prueba independiente**: con una institución administrable, completar desde Angular el ciclo de una UE mostrando la institución heredada y los campos no editables.

- [X] T021 [US1] [FR-004] Crear guard defensivo de administración institucional y añadir la entrada de navegación condicionada por `ADMINISTRADOR_PIIP` en `apps/frontend/src/app/core/organization-administration.guard.ts` y `apps/frontend/src/app/layout/app-shell.component.ts` (depende de T020)
- [X] T022 [US1] [FR-006] [FR-010] Implementar selector de institución, listado de UE activas/inactivas ordenado y acciones de administración en `apps/frontend/src/app/pages/executing-unit-administration/executing-unit-administration.component.ts`, `.html` y `.scss` (depende de T020 y T021)
- [X] T023 [US1] [FR-007] [FR-010] Implementar formulario exclusivo de alta y edición de UE con contexto heredado, código/fechas solo lectura, orden entero no negativo opcional solo en alta y control de cambios pendientes en `apps/frontend/src/app/pages/executing-unit-administration/executing-unit-form.component.ts`, `.html` y `.scss` (depende de T022)
- [X] T024 [US1] [FR-013] Implementar confirmación accesible de desactivación/reactivación y manejo de versión en `apps/frontend/src/app/pages/executing-unit-administration/executing-unit-state-dialog.component.ts`, `.html` y `.scss` (depende de T022)
- [X] T025 [US1] [FR-037] Añadir pruebas de guard, selección única/múltiple, contexto heredado, estados y conflicto de UE en `apps/frontend/src/app/pages/executing-unit-administration/executing-unit-administration.component.spec.ts` y `executing-unit-form.component.spec.ts` (depende de T023 y T024)

**Checkpoint**: UE se administra sin selector editable de institución ni entrada editable de código, y el usuario distingue errores de autorización, inexistencia, validación y concurrencia.

---

## Phase 6: User Story 2 - Experiencia Angular de UO (Priority: P1)

**Objetivo**: ofrecer una ruta y formularios propios para UO de una UE heredada, sin reutilizar el estado del catálogo de portafolio.

**Prueba independiente**: abrir las UO de una UE autorizada, completar su ciclo de vida y comprobar que la lista administrativa incluye inactivas sin mostrar selector de UE ni padre.

- [X] T026 [US2] [FR-014] Implementar listado administrativo de UO con contexto de UE heredado y estado separado del catálogo en `apps/frontend/src/app/pages/organizational-unit-administration/organizational-unit-administration.component.ts`, `.html` y `.scss` (depende de T020 y T022)
- [X] T027 [US2] [FR-015] [FR-016] [FR-017] Implementar formulario exclusivo de UO sin selector de UE, código editable ni `parentId`, con sigla obligatoria no vacía y estado inicial `active` booleano obligatorio en alta, y sigla obligatoria en edición de UO activa, en `apps/frontend/src/app/pages/organizational-unit-administration/organizational-unit-form.component.ts`, `.html` y `.scss` (depende de T026)
- [X] T028 [US2] [FR-017] Implementar confirmación accesible de desactivación/reactivación de UO, incluyendo la respuesta por sigla vacía, en `apps/frontend/src/app/pages/organizational-unit-administration/organizational-unit-state-dialog.component.ts`, `.html` y `.scss` (depende de T026)
- [X] T029 [US2] [FR-037] Añadir pruebas de contexto heredado, activas/inactivas, ausencia de padre y errores de UO en `apps/frontend/src/app/pages/organizational-unit-administration/organizational-unit-administration.component.spec.ts` y `organizational-unit-form.component.spec.ts` (depende de T027 y T028)

**Checkpoint**: el CRUD de UO es una experiencia distinta de UE y el catálogo de portafolio mantiene sus propios datos filtrados.

---

## Phase 7: User Story 3 - Navegación separada UE a UO (Priority: P2)

**Objetivo**: navegar desde una UE a la administración de sus UO manteniendo un contexto verificable en la URL y sin mezclar formularios.

**Prueba independiente**: usar la acción de una fila UE para abrir UO, verificar `executingUnitId` en ruta, contexto precargado y retorno seguro al nivel anterior.

- [X] T030 [US3] [FR-019] Declarar todas las rutas independientes de UE y UO con `executingUnitId` y asociar el guard de T021 en `apps/frontend/src/app/app.routes.ts` (depende de T020, T021, T023 y T027)
- [X] T031 [US3] [FR-020] Añadir acción UE→UO, enlaces de retorno y validación defensiva de contexto en `apps/frontend/src/app/pages/executing-unit-administration/executing-unit-administration.component.ts` y `apps/frontend/src/app/pages/organizational-unit-administration/organizational-unit-administration.component.ts` (depende de T030)
- [X] T032 [US3] [FR-023] Implementar limpieza segura y anuncios accesibles ante `403`, `404`, `409` y `422` en `apps/frontend/src/app/core/piip-http.repository.ts` y `apps/frontend/src/app/pages/organizational-unit-administration/organizational-unit-administration.component.ts` (depende de T020 y T031)
- [X] T033 [US3] [FR-037] Añadir pruebas de rutas directas manipuladas, navegación UE→UO, retorno y preservación de borrador ante conflicto en `apps/frontend/src/app/app.routes.spec.ts` y `apps/frontend/src/app/pages/organizational-unit-administration/organizational-unit-administration.component.spec.ts` (depende de T032)

**Checkpoint**: toda ruta UO identifica su UE, conserva el contexto sin exponer datos no autorizados y nunca incorpora un formulario UO dentro de una pantalla UE.

---

## Phase 8: User Story 4 - Auditoría, concurrencia y compatibilidad (Priority: P2)

**Objetivo**: comprobar evidencia append-only, conflictos y la no regresión de las consultas y catálogos existentes.

**Prueba independiente**: confirmar una mutación de cada tipo, comprobar su evento por ámbito y verificar que una versión obsoleta no sobrescribe ni audita éxito, mientras las lecturas legadas siguen filtrando UO correctamente.

- [X] T034 [US4] [FR-022] Añadir pruebas de lectura de auditoría por institución, UE y UO, incluyendo tipo/ID de entidad, actor, fecha y detalle seguro, en `apps/backend/src/test/java/pe/gob/midagri/piip/audit/api/AuditControllerTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/audit/application/AuditQueryServiceTest.java` (depende de T003, T009 y T014)
- [X] T035 [US4] [FR-021] Añadir pruebas de creaciones concurrentes por ámbito y conflicto de versión sin sobrescritura en `apps/backend/src/test/java/pe/gob/midagri/piip/organization/application/OrganizationAdministrationConcurrencyTest.java` (depende de T004, T009 y T014)
- [X] T036 [US4] [FR-022] Añadir pruebas de atomicidad entre mutación y auditoría, y de ausencia de auditoría de éxito ante rechazo, en `apps/backend/src/test/java/pe/gob/midagri/piip/organization/application/OrganizationAdministrationTransactionalAuditTest.java` (depende de T003, T009 y T014)
- [X] T037 [US4] [FR-018] Añadir regresiones del catálogo legado de UO activas con sigla no vacía en `apps/backend/src/test/java/pe/gob/midagri/piip/organization/application/OrganizationQueryServiceTest.java` y `apps/frontend/src/app/core/piip-http.repository.spec.ts` (depende de T016 y T020)
- [X] T038 [US4] [FR-027] Añadir aserciones de compatibilidad de las tres consultas organizacionales existentes en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java` y `apps/frontend/src/app/core/piip-http.repository.spec.ts` (depende de T017 y T037)

**Checkpoint**: las mutaciones confirmadas son auditables por ámbito, los conflictos no producen escrituras extra y los consumidores de catálogo conservan su comportamiento.

---

## Phase 9: Documentación, validaciones y cierre

- [X] T039 [FR-036] Actualizar el modelo de datos con UE administrativas, fechas, orden y exclusión de padre en `docs/architecture/data-model-final.md` (depende de T001 y T014)
- [X] T040 [FR-036] Actualizar la guía funcional con roles, ámbitos, experiencias separadas, herencia de contexto y catálogo de UO en `docs/funcional/guia-funcional-piip.md` (depende de T022, T029 y T031)
- [X] T041 [FR-033] Documentar evidencia de DML sintético, guardias fail-closed y exclusión de producción en `specs/019-administrar-unidades-organizacionales/quickstart.md` (depende de T007)
- [ ] T042 [FR-036] Actualizar semánticamente el grafo local de los documentos modificados mediante la skill Graphify en `graphify-out/graph.json` solo después de contar con autorización explícita del proveedor requerido; sin esa autorización, no configurarlo ni usarlo y registrar el pendiente en T047 (depende de T039 a T041)
- [X] T043 [FR-037] Ejecutar las pruebas focalizadas backend desde `apps/backend` con `gradlew.bat test` solo tras autorización explícita y usando el perfil exacto `test,test-reset` cuando la prueba requiera el reset sintético (depende de T038)
- [X] T044 [FR-037] Ejecutar las pruebas frontend desde `apps/frontend` con `npm test -- --watch=false` solo tras autorización explícita (depende de T033 y T038)
- [X] T045 [FR-037] Verificar que el contrato OpenAPI y el cliente Angular ya generados por T018 y T019 contienen las operaciones, esquemas y errores administrativos previstos, sin editar archivos generados, en `apps/backend/target/piip-openapi.json` y `apps/frontend/src/app/api/generated/` (depende de T018, T019 y T038)
- [X] T046 [FR-030] [FR-031] [FR-032] Regenerar y revisar únicamente como salida derivada de las entidades JPA `database/generated/piip-oracle.sql` para confirmar las columnas UE y de auditoría requeridas, sin editarlo manualmente ni crear una migración productiva (depende de T001 y T003; requiere la autorización separada aplicable a la generación o verificación de DDL)
- [X] T047 [FR-031] [FR-032] [FR-037] Registrar en `specs/019-administrar-unidades-organizacionales/tasks.md` evidencia de que el modelo se definió mediante JPA, no se usó SQL nativo, Flyway, Liquibase, `JdbcTemplate` ni procedimientos, y toda diferencia de esquema Oracle quedó como coordinación externa sin DDL manual ni migración productiva; incluir el resultado de T046, validaciones ejecutadas, la limitación exclusiva a `test,test-reset` y el estado ejecutado o pendiente de T042 (depende de T039 a T046; T042 solo si hubo autorización del proveedor)

### Evidencia de cierre

- T043: `..\\gradlew.bat test` desde `apps/backend` terminó con `BUILD SUCCESSFUL`; 407 pruebas exitosas. Las pruebas focalizadas de las dos regresiones también terminaron correctamente.
- T044: `npm test -- --watch=false` desde `apps/frontend` terminó correctamente con 47 archivos y 297 pruebas exitosas.
- T046: `OracleSchemaGenerationTest` generó el DDL desde metadata JPA/`OracleDialect`; verificó 21 tablas, claves foráneas descriptivas y ausencia de `INSERT`. `database/generated/piip-oracle.sql` se sincronizó desde `apps/backend/target/piip-oracle.sql`; ambos archivos tienen SHA-256 `F545E1717626F5745100D04DF3CA6472E8B43905F7F789872CF563DBA8E2DA01`. Las diferencias revisadas corresponden a las columnas UE/auditoría e índice de ámbito esperados.
- T047: el modelo estructural permanece definido por JPA; no se usaron SQL nativo, Flyway, Liquibase, `JdbcTemplate` ni procedimientos, y no se creó migración productiva. El DML sintético y sus guardias permanecen limitados a `test,test-reset`.
- T042: `graphify . --update` no pudo completar extracción semántica porque no hay backend LLM configurado; `ollama` está instalado, pero el servidor local `127.0.0.1:11434` no está disponible. No se modificó manualmente `graphify-out/graph.json`; queda pendiente ambiental.

## Dependencias y orden de ejecución

- **Propietario canónico**: T001 a T017 pertenecen al backend; fijan modelo, autorización, auditoría y contrato antes de cambiar Angular.
- **Consumidores**: T018 a T033 requieren contrato backend publicado y cliente generado. T039 a T047 dependen del comportamiento ya integrado.
- **Orden obligatorio**: Fundamento (T001-T007) -> UE backend (T008-T012) -> UO backend (T013-T016) -> contrato (T017-T020) -> UI UE (T021-T025) -> UI UO (T026-T029) -> navegación (T030-T033) -> consistencia (T034-T038) -> cierre (T039-T047).
- **Oportunidades paralelas**: T002 y T003 pueden iniciar en paralelo con T001; T005 puede iniciar en paralelo con T002/T003; T012 puede ejecutarse en paralelo con T011 tras T010; T035 puede iniciar en paralelo con T034 tras T009/T014; T039 y T040 pueden ejecutarse en paralelo tras sus dependencias. No paralelizar tareas que modifiquen el mismo contrato, modelo, catálogo, documentación o cliente generado.

## Estrategia de implementación

1. **MVP**: completar T001-T025 para entregar el CRUD de UE con autorización, concurrencia, auditoría, contrato y experiencia Angular.
2. **Incremento UO**: completar T013-T20 y T026-T29 para ofrecer el CRUD separado de UO sin relación de padre.
3. **Integración**: completar T030-T38 para navegación, auditoría por ámbito, concurrencia y regresión de catálogo.
4. **Cierre gobernado**: completar T039-T47, respetando autorizaciones separadas para Graphify semántico, pruebas, OpenAPI y revisión de DDL derivado.

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- No crear tareas a partir de pendientes históricos por arrastre.
- Si la feature depende directamente de un requisito histórico, registrar aquí el requisito, la dependencia y su aprobación explícita: feature 017 solo aporta evidencia del catálogo vigente de UO; no se reimplementa.
- Registrar contradicciones sin resolver como `NEEDS CLARIFICATION`: Ninguna. La habilitación queda limitada a `test,test-reset`; las bases con UE preexistentes están fuera de alcance.

## Notas

- Cada tarea representa trabajo nuevo aprobado y apunta a una ruta real o a un archivo nuevo con ruta concreta.
- No reimplementar capacidades incluidas en la evidencia de baseline.
- No paralelizar cambios que compartan contrato, catálogo, regla funcional, documentación o configuración.
- El cierre distingue cambios realizados, tareas pendientes y validaciones no ejecutadas.
