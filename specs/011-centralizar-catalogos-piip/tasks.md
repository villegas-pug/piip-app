---
description: "Tareas ejecutables para centralizar y normalizar los catálogos PIIP"
---

# Tareas: Centralizar catálogos PIIP

**Entrada**: documentos de `/specs/011-centralizar-catalogos-piip/`

**Prerrequisitos**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/catalogs.openapi.yaml` y `quickstart.md` vigentes; checklist sin marcadores bloqueantes para el ambiente de pruebas.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI/DDL/cliente, integración Oracle, ejecución del reset ni acciones Git. `/speckit-implement` autoriza únicamente las tareas de modificación vigentes; las acciones señaladas como “autorización adicional requerida” conservan su gate independiente.

## Formato obligatorio

Cada tarea usa `- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta/archivo exacto`.

- **[P]**: puede ejecutarse en paralelo porque no comparte archivos ni una dependencia pendiente.
- **[US#] / [FR-###]**: vincula la tarea con la especificación.
- `[X]` se usa solo cuando existe evidencia de que el cambio o validación fue realizado.

## Evidencia de baseline — no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| US1 / FR-018 | Portafolio guarda solución/fuente como enum y PEI/POI como texto. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` | Reemplazar campos, no reconstruir el flujo de creación existente. |
| US1 / FR-022 | La responsabilidad ya usa `REGISTRO_UNIDAD_RESPONSABLE` y admite una referencia organizacional. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/ResponsibleUnitEntity.java` | Reutilizar la asociación y hacer obligatoria la identidad en las operaciones adaptadas. |
| US2 / FR-014 | `GET /catalogs` existe, pero devuelve `Map<String,List<String>>`. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/CatalogController.java` | Evolucionar el contrato; no crear endpoints de administración. |
| US2 / FR-015 | El cliente generado contiene `CatalogControllerService`, pero los consumidores no lo usan. | `apps/frontend/src/app/api/generated/services/catalog-controller.service.ts` | Regenerar el cliente y centralizar su consumo detrás del repositorio/estado Angular. |
| US2 / FR-013 | Inicio mantiene `Iniciativa` y `Proyecto` como opciones hardcodeadas. | `apps/frontend/src/app/pages/dashboard/dashboard.component.html` | Consumir `recordTypes` del bundle y mantener `Todos` como opción local. |
| US3 / FR-023 | La consulta de Unidades Orgánicas activas por Unidad Ejecutora y la autorización de ámbito ya existen. | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationController.java` | Mantener la frontera y completar identidad/orden/estado sin catálogo paralelo. |
| US4 / FR-021 | El expediente ya conserva posiciones, versiones, contenido, publicación, `No aplica`, motivo y auditoría. | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/**` | Cambiar solo la identidad del tipo y proteger el ciclo vigente con regresiones. |
| US4 / FR-015 | El selector documental mantiene seis opciones locales. | `apps/frontend/src/app/pages/documents/documents.component.ts` | Sustituir la lista por el catálogo backend; conservar códigos solo para presentación. |
| US6 / FR-037 | El perfil normal usa `ddl-auto=validate`; `dev` crea globalmente. | `apps/backend/src/main/resources/application.yml`, `apps/backend/src/main/resources/application-dev.yml` | Eliminar destrucción implícita y aislar el reset selectivo en `test-reset`. |
| FR-040 | OpenAPI y el cliente Angular ya tienen flujos de generación. | `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java`, `apps/frontend/ng-openapi-gen.json` | Publicar backend antes de regenerar el consumidor; no editar generado manualmente. |
| FR-043 | Estados/transiciones de feature 009 ya están implementados. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/domain/PortfolioStatus.java` | Conservarlos como baseline y cubrir regresión, no ampliarlos. |

## Phase 1: Preparación necesaria

**Propósito**: establecer nombres técnicos y configuración compartida antes de modificar entidades o consumidores.

- [X] T001 [FR-002] Crear `CatalogCode` con únicamente `SOLUTION_TYPE`, `SOURCE_ORIGIN`, `PEI_OBJECTIVE` y `POI_ACTIVITY` en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/domain/CatalogCode.java`
- [X] T002 [P] [FR-034] Incorporar propiedades tipadas, deshabilitadas por defecto y sin valores sensibles para `piip.test-reset` en `apps/backend/src/main/java/pe/gob/midagri/piip/config/PiipProperties.java`

---

## Phase 2: Fundamento JPA y validación compartida

**Propósito**: definir el modelo canónico y la validación común que bloquean todas las historias.

- [X] T003 [P] [FR-003] Crear pruebas de identidad, unicidad, orden, activo y pertenencia de `CATALOGO`/`CATALOGO_ITEM` en `apps/backend/src/test/java/pe/gob/midagri/piip/catalogs/persistence/CatalogPersistenceTest.java`
- [X] T004 [P] [FR-010] [FR-019] Ampliar las aserciones JPA y de DDL de 16 a 19 tablas para `CATALOGO`, `CATALOGO_ITEM`, `TIPO_DOCUMENTO`, cinco FK nuevas, unicidades y ausencia de columnas/checks legacy en `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/JpaModelTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/OracleSchemaGenerationTest.java`
- [X] T005 [FR-002] Implementar `CatalogEntity` con código único, nombre, orden y activo en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/persistence/CatalogEntity.java`
- [X] T006 [FR-003] Implementar `CatalogItemEntity` con FK de cabecera, unicidad `(ID_CATALOGO,CODIGO)` e índice de activos ordenados en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/persistence/CatalogItemEntity.java`
- [X] T007 [P] [FR-004] Crear consultas por código estable, catálogo, ID y activos ordenados en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/persistence/CatalogRepository.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/persistence/CatalogItemRepository.java`
- [X] T008 [P] [FR-010] Reemplazar el enum funcional por `DocumentTypeEntity` y su repositorio, con código único, nombre, orden y activo, en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentTypeEntity.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentTypeRepository.java`
- [X] T009 [FR-018] Sustituir solución, fuente, PEI y POI por cuatro asociaciones JPA a `CatalogItemEntity`, conservando nulabilidad PEI/POI y eliminando columnas legacy, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java`
- [X] T010 [FR-020] Sustituir el enum documental por la FK `ID_TIPO_DOCUMENTO` y conservar la unicidad por registro/tipo en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentEntity.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentRepository.java`
- [X] T011 [P] [FR-022] Hacer obligatoria la asociación a Unidad Orgánica y conservar `DENOMINACION_ORIGINAL` como snapshot en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/ResponsibleUnitEntity.java`
- [X] T012 [P] [FR-017] Crear pruebas de referencia inexistente, catálogo incorrecto, inactivo y resolución por código técnico en `apps/backend/src/test/java/pe/gob/midagri/piip/catalogs/application/CatalogReferenceServiceTest.java`
- [X] T013 [FR-017] Implementar resolución transaccional por ID/catálogo/activo y resolución interna por código estable en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/application/CatalogReferenceService.java`
- [X] T014 [P] [FR-014] Definir DTO reutilizables para opción persistente, opción técnica y bundle agregado en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/api/CatalogDtos.java`
- [X] T015 [FR-017] Mapear referencia inválida a `application/problem+json` 422 con motivo seguro en `apps/backend/src/main/java/pe/gob/midagri/piip/shared/api/ApiExceptionHandler.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/shared/api/BusinessRuleException.java`

**Checkpoint**: JPA expresa las nuevas identidades sin exponer entidades y existe una única validación de referencia reutilizable.

---

## Phase 3: Backend — User Story 1 Registrar con catálogos centralizados (P1)

**Objetivo**: crear iniciativa, proyecto preexistente y proyecto derivado usando IDs activos y sin listas/textos como identidad.

**Prueba independiente**: persistir los tres tipos de registro con IDs recibidos del catálogo, combinaciones PEI/POI independientes y rechazo transaccional de una referencia inválida o inactiva.

- [X] T016 [P] [US1] [FR-016] Ampliar las pruebas de los tres flujos para IDs, opcionalidad PEI/POI, `NOT_APPLICABLE` resuelto en backend y rollback ante referencia inválida en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioFlowPersistenceTest.java`
- [X] T017 [US1] [FR-016] Reemplazar enums/textos por `solutionTypeId`, `sourceId`, `peiObjectiveId`, `poiActivityId` y `responsibleUnits[].organizationalUnitId`, y devolver referencias estructuradas, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioDtos.java`
- [X] T018 [US1] [FR-017] Resolver y validar las referencias dentro de cada transacción de creación, incluido `NOT_APPLICABLE` por código para preexistentes, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java`
- [X] T019 [US1] [FR-012] Crear las posiciones del expediente desde los tipos documentales activos ordenados, sin `DocumentType.values()`, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java`
- [X] T020 [US1] [FR-018] Incorporar `EntityGraph`/consultas de carga necesarias para mapear las cuatro asociaciones sin N+1 en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordRepository.java`

**Checkpoint**: las tres creaciones constituyen un incremento backend comprobable y no alteran estados, permisos ni transiciones.

---

## Phase 4: Backend — User Story 2 Consultar y filtrar con identidades consistentes (P1)

**Objetivo**: exponer opciones activas y lecturas/filtros basados en identidad, manteniendo referencias históricas completas.

**Prueba independiente**: consultar bundle, listados, detalles y bandeja; comprobar orden, IDs, tipo técnico sin ID y que cada filtro vigente usa únicamente la identidad prevista por su contrato.

- [X] T021 [P] [US2] [FR-014] Reemplazar la prueba del mapa de etiquetas por escenarios del bundle tipado, ordenado, activo y sin `Todos` en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/CatalogContractTest.java`
- [X] T022 [P] [US2] [FR-041] Crear pruebas de respuestas con referencias completas y confirmar que iniciativas/proyectos conservan exclusivamente `q`, `status`, `executingUnitId`, `page` y `size` en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioCatalogQueryTest.java`
- [X] T023 [US2] [FR-014] Implementar consulta agregada de activos y catálogo técnico de tipo de registro en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/application/CatalogQueryService.java`
- [X] T024 [US2] [FR-029] Reubicar y adaptar `GET /catalogs` para delegar al servicio sin endpoints de escritura en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/api/CatalogController.java` y eliminar `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/CatalogController.java`
- [X] T025 [US2] [FR-016] Mapear referencias completas en listados y detalles sin agregar parámetros a `q`, `status`, `executingUnitId`, `page` y `size` en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java`
- [X] T026 [US2] [FR-041] [FR-045] Extraer la lógica de bandeja a un servicio y devolver las identidades requeridas por búsqueda, Tipo de registro, estado y Unidad Orgánica sin agregar parámetros HTTP ni filtros en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentInboxService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentInboxController.java`

**Checkpoint**: la consulta global es atómica, los filtros usan identidad y las lecturas no dependen del catálogo de activos para resolver historia.

---

## Phase 5: Backend — User Story 3 Elegir la Unidad Orgánica correcta (P1)

**Objetivo**: exigir una Unidad Orgánica activa, perteneciente a la Unidad Ejecutora y autorizada, sin designaciones como identidad.

**Prueba independiente**: intentar guardar una unidad válida, inexistente, inactiva y de otra Unidad Ejecutora y comprobar que solo la válida crea la asociación.

- [X] T027 [P] [US3] [FR-024] Crear pruebas de activo, pertenencia, ámbito y atomicidad de responsables en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitValidationTest.java`
- [X] T028 [US3] [FR-023] Completar la respuesta organizacional con `id`, `code`, `name`, `active`, `acronym`, `parentId` y `executingUnitId`, ordenada por nombre y sin `displayOrder`, en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationController.java`
- [X] T029 [US3] [FR-024] Restringir consultas a activas por Unidad Ejecutora y agregar resolución histórica por ID en `apps/backend/src/main/java/pe/gob/midagri/piip/organization/persistence/OrganizationalUnitRepository.java`
- [X] T030 [US3] [FR-023] Validar Unidad Orgánica y derivar `DENOMINACION_ORIGINAL` en backend antes de guardar responsables en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java`

**Checkpoint**: ninguna creación acepta ID nulo, inactivo o ajeno y el modelo organizacional vigente permanece como única fuente.

---

## Phase 6: Backend — User Story 4 Gestionar documentos sin alterar su proceso (P1)

**Objetivo**: usar Tipo documental persistente conservando posiciones, versiones, contenido, publicación, auditoría y `No aplica`.

**Prueba independiente**: operar un expediente de seis posiciones, cargar dos versiones, publicar una y marcar otra `No aplica`, incluyendo lectura/operación de una posición histórica cuyo tipo quedó inactivo.

- [X] T031 [P] [US4] [FR-021] Crear regresiones del ciclo documental completo y de posición histórica inactiva en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentCatalogFlowTest.java`
- [X] T032 [P] [US4] [FR-043] Adaptar pruebas de autorización documental para `documentTypeId` sin cambiar permisos ni ámbitos en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentAuthorizationTest.java`
- [X] T033 [US4] [FR-020] Sustituir el enum de respuesta y los paths `{type}` por referencias/`{documentTypeId}` en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentDtos.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentController.java`
- [X] T034 [US4] [FR-021] Resolver posiciones por FK, ordenar por tipo persistente y permitir operaciones vigentes sobre slots históricos existentes en `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentRepository.java`
- [X] T035 [US4] [FR-021] Registrar snapshots inmutables de código/nombre documental y conservar metadatos sin contenido sensible en `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java`

**Checkpoint**: solo cambia la identidad del tipo; las reglas y datos del expediente continúan cubiertos por regresión.

---

## Phase 7: Backend — User Story 5 Conservar historial inactivo (P2)

**Objetivo**: diferenciar lectura histórica de nueva selección en catálogos y Unidades Orgánicas.

**Prueba independiente**: desactivar un ítem utilizado, ocultarlo del bundle/filtros, resolverlo en detalle y rechazar su ID en una nueva creación.

- [X] T036 [P] [US5] [FR-026] Crear pruebas integradas de activos, inactivos, renombre y ausencia de eliminación física en `apps/backend/src/test/java/pe/gob/midagri/piip/catalogs/application/CatalogAvailabilityTest.java`
- [X] T037 [US5] [FR-026] Separar consultas de selección activa y resolución histórica sin filtrar asociaciones existentes en `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/application/CatalogQueryService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/application/CatalogReferenceService.java`
- [X] T038 [US5] [FR-026] Devolver Unidades Orgánicas inactivas solo dentro de lecturas históricas de responsables en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java`

**Checkpoint**: un inactivo desaparece de opciones pero conserva código, nombre y estado en registros previos.

---

## Phase 8: Publicación del contrato y consumidor generado

**Propósito**: congelar el propietario backend antes de modificar consumidores Angular. Depende de T016-T038.

- [X] T039 [FR-040] Actualizar expectativas de bundle, IDs, respuestas, rutas documentales y parámetros de consulta vigentes sin ampliarlos en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java`
- [X] T040 [FR-040] Generar y revisar `apps/backend/target/piip-openapi.json` mediante `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` desde `apps/backend` — autorización adicional para prueba y generación requerida
- [X] T041 [FR-040] Regenerar `apps/frontend/src/app/api/generated/**` mediante `npm run api:generate` desde `apps/frontend` y revisar que no queden firmas `{type}` ni requests textuales — autorización adicional para generación requerida; depende de T040

**Checkpoint**: el cliente generado refleja el contrato backend completo antes de adaptar repositorio o pantallas.

---

## Phase 9: Fundamento Angular compartido

**Propósito**: centralizar modelos, carga y errores antes de adaptar cada historia. Depende de T041.

- [X] T042 [P] [FR-028] Crear pruebas del estado `loading`, `loaded-empty`, `error`, reintento, conservación por ID y descarte de respuestas tardías en `apps/frontend/src/app/core/piip-catalogs.store.spec.ts`
- [X] T043 [FR-014] Incorporar opción persistente, opción técnica, referencia histórica y estado de carga en `apps/frontend/src/app/core/piip.models.ts`
- [X] T044 [FR-028] Implementar la fachada de bundle global y Unidades Orgánicas por UE con request-id en `apps/frontend/src/app/core/piip-catalogs.store.ts`
- [X] T045 [FR-015] Exponer el estado centralizado y operaciones de recarga en `apps/frontend/src/app/core/piip.repository.ts` y `apps/frontend/src/app/core/piip-repository.token.ts`
- [X] T046 [FR-016] Consumir servicios generados y retirar conversiones etiqueta→enum o unidad por sigla/nombre en `apps/frontend/src/app/core/piip-http.repository.ts`
- [X] T047 [P] [FR-041] Adaptar datos equivalentes del repositorio de prueba al contrato por identidades en `apps/frontend/src/app/core/piip-mock.repository.ts`

**Checkpoint**: todos los consumidores pueden distinguir carga/vacío/error y trabajar por identidad sin fallback.

---

## Phase 10: Frontend — User Story 1 Registrar con catálogos centralizados (P1)

**Objetivo**: completar los tres formularios con opciones backend e IDs de escritura.

**Prueba independiente**: registrar iniciativa, preexistente y derivado; observar requests con IDs, PEI/POI independientes y bloqueo ante valor heredado inactivo.

- [X] T048 [P] [US1] [FR-042] Ampliar pruebas de carga/vacío/error, IDs y selección preservada en `apps/frontend/src/app/pages/initiative-form/initiative-form.component.spec.ts`
- [X] T049 [P] [US1] [FR-042] Ampliar pruebas de catálogos e identidad del proyecto preexistente en `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.spec.ts`
- [X] T050 [P] [US1] [FR-044] Crear pruebas de precarga activa e inactiva y reemplazo obligatorio en `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.spec.ts`
- [X] T051 [US1] [FR-016] Cambiar controles/selectores y revisión para usar IDs de solución, fuente, PEI, POI y Unidad Orgánica en `apps/frontend/src/app/pages/initiative-form/initiative-form.component.ts`, `apps/frontend/src/app/pages/initiative-form/initiative-form.component.html` y `apps/frontend/src/app/pages/initiative-form/initiative-review-dialog.component.html`
- [X] T052 [US1] [FR-016] Adaptar el formulario preexistente y eliminar el literal/ID local de `No aplica` en `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.ts` y `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.html`
- [X] T053 [US1] [FR-044] Separar referencia heredada y control de nueva escritura, bloqueando inactivos hasta su reemplazo, en `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.ts` y `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.html`
- [X] T054 [US1] [FR-016] Mapear los tres inputs a requests generados por ID y propagar 422 comprensible sin sustitución automática en `apps/frontend/src/app/core/piip-http.repository.ts`

**Checkpoint**: US1 queda demostrable de extremo a extremo una vez autorizadas las validaciones correspondientes.

---

## Phase 11: Frontend — User Story 2 Consultar y filtrar con identidades consistentes (P1)

**Objetivo**: usar las mismas identidades/nombres en listados, filtros, bandeja y detalles.

**Prueba independiente**: navegar por ambos listados/detalles y la bandeja, usar las identidades previstas por sus filtros vigentes, conservar la selección tras recarga y mostrar estados vacíos/errores.

- [X] T055 [P] [US2] [FR-042] Adaptar pruebas de referencias resueltas y estados de catálogo manteniendo los filtros vigentes de iniciativas en `apps/frontend/src/app/pages/initiatives/initiatives.component.spec.ts`
- [X] T056 [P] [US2] [FR-042] Adaptar pruebas de referencias resueltas y estados de catálogo manteniendo los filtros vigentes de proyectos en `apps/frontend/src/app/pages/projects/projects.component.spec.ts`
- [X] T057 [P] [US2] [FR-042] [FR-045] Adaptar pruebas de búsqueda, Tipo de registro, estado, Unidad Orgánica y estados de carga sin filtros nuevos en `apps/frontend/src/app/pages/documents-inbox/documents-inbox.component.spec.ts`
- [X] T058 [P] [US2] [FR-041] Ampliar pruebas de referencias PEI/POI y estado activo en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.spec.ts` y `apps/frontend/src/app/pages/project-detail/project-detail.component.spec.ts`
- [X] T059 [US2] [FR-016] Adaptar iniciativas y proyectos a referencias resueltas conservando únicamente sus filtros vigentes y sin enviar IDs no previstos por el contrato en `apps/frontend/src/app/pages/initiatives/initiatives.component.ts`, `apps/frontend/src/app/pages/initiatives/initiatives.component.html`, `apps/frontend/src/app/pages/projects/projects.component.ts` y `apps/frontend/src/app/pages/projects/projects.component.html`
- [X] T060 [US2] [FR-013] Sustituir `Iniciativa` y `Proyecto` hardcodeados por `recordTypes`, mantener `Todos` local y cubrirlo en `apps/frontend/src/app/pages/dashboard/dashboard.component.ts`, `apps/frontend/src/app/pages/dashboard/dashboard.component.html` y `apps/frontend/src/app/pages/dashboard/dashboard.component.spec.ts`
- [X] T061 [US2] [FR-041] [FR-045] Adaptar la bandeja a referencias canónicas y estados explícitos conservando exclusivamente búsqueda, Tipo de registro, estado y Unidad Orgánica en `apps/frontend/src/app/pages/documents-inbox/documents-inbox.component.ts` y `apps/frontend/src/app/pages/documents-inbox/documents-inbox.component.html`
- [X] T062 [US2] [FR-041] Mostrar código, nombre y activo para solución, fuente, PEI, POI y responsables en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.ts`, `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.html`, `apps/frontend/src/app/pages/project-detail/project-detail.component.ts` y `apps/frontend/src/app/pages/project-detail/project-detail.component.html`

**Checkpoint**: listados, filtros y detalles ya no comparan identidades por etiquetas.

---

## Phase 12: Frontend — User Story 3 Elegir la Unidad Orgánica correcta (P1)

**Objetivo**: eliminar fallback y reconciliar las opciones por la Unidad Ejecutora vigente.

**Prueba independiente**: cambiar rápidamente de Unidad Ejecutora, descartar la respuesta anterior y comprobar que ninguna pantalla ofrece siglas locales ni conserva una unidad ajena.

- [X] T063 [P] [US3] [FR-023] Ampliar pruebas del adaptador para request-id, activo y pertenencia de Unidad Orgánica en `apps/frontend/src/app/core/piip-http.repository.spec.ts`
- [X] T064 [US3] [FR-023] Vaciar/reconciliar la selección al cambiar de Unidad Ejecutora y descartar respuestas tardías en `apps/frontend/src/app/core/piip-catalogs.store.ts` y `apps/frontend/src/app/core/piip-http.repository.ts`
- [X] T065 [US3] [FR-025] Retirar `RESPONSIBLE_UNITS` y todo fallback funcional de formularios, listados y bandeja en `apps/frontend/src/app/core/piip.catalogs.ts`, `apps/frontend/src/app/pages/initiative-form/initiative-form.component.ts`, `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.ts`, `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.ts`, `apps/frontend/src/app/pages/initiatives/initiatives.component.ts`, `apps/frontend/src/app/pages/projects/projects.component.ts` y `apps/frontend/src/app/pages/documents-inbox/documents-inbox.component.ts`

**Checkpoint**: toda responsabilidad nueva utiliza una opción activa de la UE actual o bloquea el formulario con vacío/error explícito.

---

## Phase 13: Frontend — User Story 4 Gestionar documentos sin alterar su proceso (P1)

**Objetivo**: seleccionar y operar posiciones por `documentTypeId` conservando toda la experiencia documental.

**Prueba independiente**: recorrer seis posiciones, versiones, publicación y `No aplica`; comprobar agrupación por código y lectura de un tipo histórico inactivo.

- [X] T066 [P] [US4] [FR-021] Adaptar regresiones de selector, versiones, publicación, `No aplica` e inactivo histórico en `apps/frontend/src/app/pages/documents/documents.component.spec.ts`
- [X] T067 [P] [US4] [FR-021] Adaptar pruebas de presentación de auditoría sin mapa local de etiquetas documentales en `apps/frontend/src/app/pages/audit/audit-event.presenter.spec.ts`
- [X] T068 [US4] [FR-020] Reemplazar la lista local por `documentTypes` del store y enviar IDs en `apps/frontend/src/app/pages/documents/documents.component.ts` y `apps/frontend/src/app/pages/documents/documents.component.html`
- [X] T069 [US4] [FR-021] Mapear carga, `No aplica`, versiones y respuestas documentales con el cliente generado por ID en `apps/frontend/src/app/core/piip-http.repository.ts` y `apps/frontend/src/app/core/piip.models.ts`
- [X] T070 [US4] [FR-021] Agrupar etapas y localizar posiciones por código estable/identidad, nunca por etiqueta, en `apps/frontend/src/app/pages/documents/documents.component.ts`, `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.ts` y `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.ts`
- [X] T071 [US4] [FR-021] Presentar el snapshot de auditoría recibido y eliminar `DOCUMENT_LABELS` en `apps/frontend/src/app/pages/audit/audit-event.presenter.ts` y `apps/frontend/src/app/pages/audit/audit-event-detail-dialog.component.ts`

**Checkpoint**: el cambio de catálogo no altera ninguna operación documental vigente.

---

## Phase 14: Frontend — User Story 5 Conservar historial inactivo (P2)

**Objetivo**: mostrar historia inactiva sin ofrecerla ni conservarla como selección nueva.

**Prueba independiente**: refrescar catálogos tras desactivar una opción, conservarla como contexto histórico y exigir una opción activa antes de guardar.

- [X] T072 [P] [US5] [FR-026] Completar escenarios de reconciliación activa/inactiva y renombre por ID en `apps/frontend/src/app/core/piip-catalogs.store.spec.ts`
- [X] T073 [US5] [FR-028] Invalidar controles cuya identidad desaparece o se inactiva y mostrar “La opción seleccionada ya no está disponible. Elige una opción vigente.” en `apps/frontend/src/app/core/piip-catalogs.store.ts`, `apps/frontend/src/app/pages/initiative-form/initiative-form.component.html`, `apps/frontend/src/app/pages/preexisting-project-form/preexisting-project-form.component.html` y `apps/frontend/src/app/pages/derived-project-form/derived-project-form.component.html`
- [X] T074 [US5] [FR-026] Renderizar referencias históricas inactivas con estado visible sin agregarlas a filtros en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.html`, `apps/frontend/src/app/pages/project-detail/project-detail.component.html` y `apps/frontend/src/app/pages/documents/documents.component.html`

**Checkpoint**: las opciones operativas contienen solo activos y la historia permanece comprensible.

---

## Phase 15: User Story 6 Reiniciar de forma controlada el ambiente de pruebas (P2)

**Objetivo**: reiniciar exclusivamente las tablas afectadas, descartar toda auditoría del ambiente, cargar datos idempotentes y preservar identidad/maestros.

**Prueba independiente**: ejecutar sobre Oracle de pruebas allowlisted, repetir tras éxito y tras fallo parcial, volver al perfil normal y comparar datos protegidos.

- [X] T075 [P] [US6] [FR-034] Crear pruebas fail-closed de perfiles, confirmación, huella JDBC y esquema allowlisted en `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetEnvironmentGuardTest.java`
- [X] T076 [P] [US6] [FR-035] Crear pruebas de allowlist exacta de trece tablas, cierre de FK, orden, inclusión obligatoria de auditoría y `NOTIFICACION`, y denylist exacta de seis tablas protegidas en `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetSchemaFilterTest.java`
- [X] T077 [P] [US6] [FR-036] Crear pruebas de etapas, detención al primer fallo y reejecución completa, tolerando `ORA-00942` únicamente cuando la causa raíz Oracle sea `942` durante el `DROP` de la tabla allowlisted actualmente procesada después del preflight, y tratándolo como fatal en preflight, create, seed, postvalidación, tablas protegidas o cualquier otra operación, en `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetCoordinatorTest.java`
- [X] T078 [P] [US6] [FR-032] Crear validación del seed sin DDL, IDs numéricos hardcodeados ni códigos duplicados en `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/CatalogSeedPolicyTest.java`
- [X] T079 [US6] [FR-034] Configurar `ddl-auto=none`, aplicación no web y activación manual en `apps/backend/src/main/resources/application-test-reset.yml`, y cambiar `apps/backend/src/main/resources/application-dev.yml` para no recrear globalmente el esquema
- [X] T080 [US6] [FR-034] Implementar la guardia de perfiles, confirmación y coincidencia de conexión antes de la etapa destructiva en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetEnvironmentGuard.java`
- [X] T081 [US6] [FR-035] Capturar `Metadata` Hibernate y filtrar exactamente la matriz de trece tablas en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/HibernateMetadataCapture.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetSchemaFilterProvider.java`
- [X] T082 [US6] [FR-038] Verificar que `EVENTO_AUDITORIA`, `AUDITORIA_ACCESO` y `NOTIFICACION` participan en drop/create y quedan vacías tras el reset en `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetCoordinatorTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/TestResetOracleIntegrationTest.java`
- [X] T083 [US6] [FR-036] Orquestar preflight, drop/create Hibernate de las trece tablas, seed y postvalidación fail-fast; continuar ante `ORA-00942` solo si la causa raíz Oracle es `942`, el preflight ya finalizó y el fallo corresponde al `DROP` de la tabla allowlisted actual, y detenerse ante ese código en cualquier otra etapa o ante cualquier otro error Oracle, en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetStage.java`
- [X] T084 [US6] [FR-030] Crear el DML Oracle idempotente con cuatro catálogos, sus valores exactos, seis tipos documentales y UO sintéticas condicionadas por código en `apps/backend/src/main/resources/db/test/catalog-data.sql`
- [X] T085 [US6] [FR-039] Excluir el bootstrap de identidad del perfil `test-reset` sin cambiar el arranque normal en `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/IdentityBootstrap.java`
- [X] T086 [US6] [FR-038] [FR-039] Crear integración Oracle de preservación de las seis tablas protegidas, descarte total de auditoría/notificaciones, idempotencia, recuperación y posterior `validate` en `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/TestResetOracleIntegrationTest.java`

**Checkpoint**: el código del reset queda implementado pero no se ejecuta hasta recibir autorización adicional y una conexión de pruebas allowlisted.

---

## Phase 16: Documentación, derivados y cierre

- [X] T087 [P] [FR-043] Actualizar el recorrido cronológico de registro, filtros, históricos y documentos sin inventar transiciones en `docs/funcional/guia-funcional-piip.md`
- [X] T088 [P] [FR-034] Documentar guardias, etapas, variables no sensibles, recuperación y prohibición productiva en `docs/development/test-catalog-reset.md`
- [X] T089 [FR-015] Eliminar únicamente las siete fuentes duplicadas y conservar estados/transiciones/componentes fuera de alcance en `apps/frontend/src/app/core/piip.catalogs.ts`
- [X] T090 [FR-043] Crear `apps/backend/src/test/java/pe/gob/midagri/piip/support/PortfolioRecordTestBuilder.java` y migrar los fixtures de portafolio, documentos, dashboard, work y autorización que construyen `PortfolioRecordEntity` en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioFlowPersistenceTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioTransitionTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioAuthorizationTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioInitiativeStatusServiceTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioProjectStatusServiceTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusAuditTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioStatusConcurrencyTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentAuthorizationTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/api/DashboardControllerTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/application/DashboardPortfolioServiceTest.java`, `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/persistence/DashboardPortfolioQueryRepositoryTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/work/api/WorkControllerTest.java`; adaptar además `apps/frontend/src/app/core/piip-mock.repository.ts` y `apps/frontend/src/app/core/piip-domain.spec.ts` a referencias estructuradas
- [X] T091 [FR-037] Ejecutar `gradlew.bat test --tests pe.gob.midagri.piip.persistence.OracleSchemaGenerationTest` desde `apps/backend` y actualizar `database/generated/piip-oracle.sql` desde `apps/backend/target/piip-oracle.sql` — autorización adicional para prueba/generación requerida
- [X] T092 [FR-041] Ejecutar `graphify update .` desde `F:/work-space/piip-monorepo` después de los cambios materiales y revisar que no se indexen artefactos temporales
- [X] T093 [FR-041] Registrar en `specs/011-centralizar-catalogos-piip/tasks.md` qué tareas se completaron y qué validaciones permanecieron sin autorización, sin marcar evidencia no ejecutada

## Validaciones propuestas — requieren autorización

- [X] T094 [FR-017] Ejecutar pruebas backend focalizadas con `gradlew.bat test --tests pe.gob.midagri.piip.catalogs.* --tests pe.gob.midagri.piip.portfolio.PortfolioFlowPersistenceTest --tests pe.gob.midagri.piip.portfolio.application.ResponsibleUnitValidationTest --tests pe.gob.midagri.piip.documents.application.DocumentCatalogFlowTest` desde `apps/backend` — autorización requerida
- [X] T095 [FR-040] Ejecutar la generación contractual con `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` desde `apps/backend` y verificar el diff de `apps/backend/target/piip-openapi.json` — autorización requerida
- [X] T096 [FR-042] Ejecutar `npm test -- --watch=false` desde `apps/frontend` y conservar evidencia de los escenarios de catálogos — autorización requerida
- [X] T097 [FR-042] Ejecutar `npx tsc -p tsconfig.app.json --noEmit` y `npx tsc -p tsconfig.spec.json --noEmit` desde `apps/frontend` como comprobación estática de tipos
- [X] T098 [FR-039] Ejecutar `gradlew.bat integrationTest --tests pe.gob.midagri.piip.persistence.TestResetOracleIntegrationTest` desde `apps/backend` contra Oracle de pruebas allowlisted — `BUILD SUCCESSFUL` tras excluir `SecurityConfig` en el perfil `test-reset`
- [X] T099 [FR-036] Ejecutar una vez el perfil manual con `gradlew.bat bootRun --args="--spring.profiles.active=test,test-reset --piip.test-reset.enabled=true --piip.test-reset.confirmation=$env:PIIP_TEST_RESET_CONFIRMATION --piip.test-reset.allowed-jdbc-fingerprint=$env:PIIP_TEST_RESET_JDBC_FINGERPRINT --piip.test-reset.allowed-schema=$env:PIIP_TEST_RESET_SCHEMA"` desde `apps/backend`, confirmar salida inequívoca y volver a arrancar con perfil normal — `test-reset completado correctamente sobre el esquema allowlisted`; perfil `dev` posterior saludable en el puerto 4001
- [ ] T100 [FR-021] Ejecutar el recorrido funcional de seis posiciones, dos versiones, publicación, descarga y `No aplica` con backend/frontend autorizados, documentando evidencia en `specs/011-centralizar-catalogos-piip/tasks.md` — autorización de servicios y prueba requerida

## Dependencias y orden de ejecución

- **Propietario canónico**: T001-T039 en `apps/backend/**` definen modelo, reglas y contrato.
- **Consumidor generado**: T040 debe preceder T041; T041 bloquea T042-T074.
- **Orden obligatorio**: Phase 1 → Phase 2 → backend US1-US5 (Phases 3-7) → publicación (Phase 8) → fundamento Angular (Phase 9) → frontend US1-US5 (Phases 10-14). US6 depende del modelo de Phase 2, pero no del cliente Angular.
- **Dependencias de historias**:
  - US1 depende de T001-T015; su frontend depende además de T039-T047.
  - US2 depende de T001-T015 y del mapeo de US1 T017-T020; su frontend depende de T039-T047.
  - US3 depende de T011/T013 y comparte `PortfolioService` con US1; ejecutar T027-T030 después de T018-T019.
  - US4 depende de T008/T010 y de la creación de slots T019; ejecutar T031-T035 antes de publicar contrato.
  - US5 depende de las consultas/validaciones de US1-US4; no es independiente del fundamento, aunque su aceptación sí es focalizable.
  - US6 depende de T002 y T005-T011; puede avanzar después de Phase 2 en archivos de `config/reset/**`, salvo T083/T084 que requieren el modelo definitivo.
- **MVP sugerido**: US1 completa, incluyendo Phases 1-3, publicación contractual necesaria, fundamento Angular y Phase 10. Debido al contrato compartido, la publicación final se realiza después de cerrar las firmas backend P1 de US2-US4; no se edita el cliente a mano para adelantar el MVP.

## Oportunidades de paralelización

- **Preparación/fundamento**: T002 puede avanzar junto con T003-T004; T007, T008 y T011 trabajan en archivos distintos después de sus entidades base.
- **US1**: T016 puede prepararse en paralelo con DTO/modelo ya definidos; T020 no comparte archivo con T017-T019.
- **US2**: T021 y T022 son paralelas; T026 puede avanzar después de definir los DTO compartidos sin tocar `PortfolioService`; T060 se separa de listados, bandeja y detalles.
- **US3**: T027 puede prepararse mientras T028-T029 cierran el contrato organizacional; T030 espera esas consultas.
- **US4**: T031 y T032 son paralelas; T035 puede avanzar después del contrato de snapshot sin tocar `DocumentController`.
- **US5**: T036 es independiente de las plantillas Angular; T037-T038 son secuenciales por política compartida.
- **Frontend US1**: T048-T050 son paralelas; T051-T053 se separan por carpeta después de T043-T047.
- **Frontend US2**: T055-T058 son paralelas; T059-T062 pueden dividirse por página cuando el estado central ya está estable.
- **Frontend US4**: T066 y T067 son paralelas; T071 no comparte componentes con T068-T070.
- **US6**: T075-T078 y T086 pueden escribirse en paralelo; T080-T082 trabajan en archivos distintos antes de integrarse en T083.
- **Cierre**: T087 y T088 son paralelas; T091 espera el modelo final; T092 espera todos los cambios de código.

## Ejemplos de ejecución paralela por historia

```text
US1 backend: T016 || T020  →  T017 → T018 → T019
US1 frontend: T048 || T049 || T050  →  T051 || T052 || T053  →  T054

US2 backend: T021 || T022 || T026  →  T023 → T024 → T025
US2 frontend: T055 || T056 || T057 || T058  →  T059 || T060 || T061 || T062

US3 backend: T027 || T028 || T029  →  T030
US3 frontend: T063  →  T064 → T065

US4 backend: T031 || T032  →  T033 → T034 → T035
US4 frontend: T066 || T067  →  T068 || T071  →  T069 → T070

US5 backend: T036  →  T037 → T038
US5 frontend: T072  →  T073 || T074

US6: T075 || T076 || T077 || T078  →  T079 || T080 || T081 || T082  →  T083 → T084 || T085 || T086
```

## Estrategia de implementación incremental

1. **Fundamento seguro**: completar T001-T015 y revisar el modelo JPA antes de tocar casos de uso.
2. **Backend P1**: completar US1-US4 y la política histórica US5; detenerse si el contrato diverge de `contracts/catalogs.openapi.yaml`.
3. **Contrato primero**: con autorización adicional, ejecutar T040-T041 una sola vez sobre el backend P1 estabilizado.
4. **MVP visible**: completar fundamento Angular y US1; demostrar los tres registros antes de continuar con consultas/documentos.
5. **Incrementos de consulta y organización**: completar US2 y US3 sin ampliar filtros/pantallas fuera de la spec.
6. **Incremento documental**: completar US4 y luego US5, preservando todos los comportamientos baseline.
7. **Reset de pruebas**: implementar US6 después del modelo definitivo; nunca ejecutar la ruta destructiva como parte automática de `/speckit-implement`.
8. **Cierre**: documentación, derivados, Graphify y solo las validaciones expresamente autorizadas.

## Cobertura de requisitos

| Requisitos | Tareas principales |
|------------|--------------------|
| FR-001, FR-002, FR-003, FR-004 | T001, T003, T005-T007, T014, T023-T024 |
| FR-005, FR-006, FR-007, FR-008, FR-009 | T084, T078 |
| FR-010, FR-011, FR-012 | T004, T008, T010, T019, T031-T034, T084 |
| FR-013, FR-014, FR-015, FR-016, FR-017 | T012-T015, T021-T025, T039-T046 |
| FR-018, FR-019, FR-020, FR-021 | T004, T009-T010, T016-T020, T031-T035, T066-T071 |
| FR-022, FR-023, FR-024, FR-025 | T011, T027-T030, T063-T065, T084 |
| FR-026, FR-027, FR-028, FR-029 | T036-T038, T072-T074, T089 |
| FR-030, FR-031, FR-032, FR-033 | T078, T083-T084 |
| FR-034, FR-035, FR-036, FR-037, FR-038, FR-039 | T002, T075-T086, T098-T099 |
| FR-040 | T039-T041, T095 |
| FR-041–FR-042 | T026, T042-T074, T090, T096-T100 |
| FR-043 | T032, T087, T090, T100 |
| FR-044 | T050, T053, T073 |
| FR-045 | T026, T057, T061 |

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog; ninguna tarea se derivó de ellas.
- Dependencia histórica aprobada: matrices de transición de feature 009 ratificadas desde la constitución 1.1.0, conservadas por la 1.2.0 y preservadas mediante T032, T087 y T090.
- Pendientes productivos diferidos: valores oficiales PEI/POI, autoridad institucional y migración no destructiva; no bloquean este backlog exclusivo de pruebas y no se convierten en tareas.
- Contradicciones bloqueantes: ninguna.

## Notas

### Estado de ejecución al 2026-08-20

- Implementación y pruebas escritas: T001-T090 completadas; ninguna suite se considera aprobada por el solo hecho de haber escrito sus casos.
- Contrato: T040 y T095 ejecutaron correctamente `OpenApiGenerationTest`; T041 regeneró 39 modelos y 11 servicios y eliminó el archivo generado obsoleto `catalogs.ts`.
- Comprobación estática: T097 pasó para `tsconfig.app.json` y `tsconfig.spec.json`; T092 actualizó Graphify sin indexar `target`, `node_modules`, `.gradle`, `dist` ni archivos temporales.
- DDL y backend: T091 generó 19 tablas y actualizó `database/generated/piip-oracle.sql` con contenido coincidente; T094 terminó con `BUILD SUCCESSFUL`.
- Frontend: T096 se ejecutó dos veces; tras corregir dos fixtures de la feature quedaron 177 de 178 pruebas aprobadas. El único fallo restante es baseline fuera de alcance en `user-administration.component.spec.ts` (`1 suspendida` frente a `Asignación suspendida`).
- T098 pasó con `BUILD SUCCESSFUL` contra Oracle allowlisted; la primera ejecución quedó bloqueada por `HttpSecurity` ausente en `web-application-type=none` y se corrigió excluyendo `SecurityConfig` solo bajo `test-reset`.
- T099 completó el reset manual con salida inequívoca `test-reset completado correctamente sobre el esquema allowlisted` y `BUILD SUCCESSFUL`; luego se arrancó el perfil `dev` y `/api/v1/actuator/health` respondió `UP` en el puerto 4001.
- Pendiente de autorización adicional: T100. No se ejecutó todavía el recorrido funcional con backend/frontend.

- Las tareas de prueba T003, T004, T012, T016, T021-T022, T027, T031-T032, T036, T042, T048-T050, T055-T058, T063, T066-T067, T072 y T075-T078/T086 crean o adaptan pruebas; no autorizan ejecutarlas.
- Los comandos T040-T041, T091 y T094-T100 conservan autorización adicional independiente.
- No introducir pantallas/endpoints de administración, nuevos estados, transiciones, permisos, `APLICA_A`, relación PEI-POI ni una tabla organizacional paralela.
- No marcar tareas de baseline como completadas ni reimplementar comportamiento vigente sin una adaptación explícita de esta feature.
