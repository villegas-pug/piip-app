# Feature Specification: Refactorización integral de la arquitectura backend

**Feature Branch**: `refactor/backend`

**Created**: 2026-08-22

**Status**: Draft

**Input**: User description: "Generar únicamente la especificación de una refactorización arquitectónica integral del backend de PIIP-monorepo, basada en evidencia del checkout actual y sin modificar el comportamiento funcional observable ni comprometer al frontend."

## Clasificación del grounding

### Alcance y fuentes revisadas

- Se revisaron los 99 archivos Java productivos distribuidos en `audit`, `catalogs`, `config`, `dashboard`, `documents`, `identity`, `organization`, `portfolio`, `shared` y `work` bajo `apps/backend/src/main/java/pe/gob/midagri/piip/**`.
- Se revisaron las cuatro pruebas existentes de `apps/backend/src/test/java/pe/gob/midagri/piip/architecture/**`: `PersistencePolicyTest`, `JsonProducesMappingTest`, `HttpParameterBindingTest` y `ExecutingUnitFilterMappingTest`.
- Se revisaron `.specify/memory/constitution.md`, `docs/architecture/**`, `docs/development/spec-kit-adoption.md` y las specs vigentes relacionadas con autorización, portafolio, dashboard, trabajo, auditoría, documentos y catálogos (`004`, `008`, `009`, `010` y `011`).
- `graphify-out/graph.json` se usó solo como índice para localizar `LocalAuthorizationService`, controllers, repositorios, servicios y pruebas; las conclusiones se confirmaron en archivos canónicos.
- El inventario de `F:\work-space\piip-resources` no aportó una regla vigente necesaria para resolver los hallazgos. Ninguna propuesta externa de microservicios, Vertical Slice o dominio POJO puro se incorpora como requisito.
- No se ejecutaron builds, pruebas, servidores, migraciones, contenedores, generación OpenAPI ni integración Oracle.

### Reglas arquitectónicas vigentes

1. Los controllers delegan; las reglas y transacciones pertenecen a servicios de aplicación.
2. Los contratos HTTP no exponen entidades persistentes.
3. Hibernate JPA es la fuente canónica del esquema Oracle; continúan prohibidos SQL nativo, `JdbcTemplate`, procedimientos almacenados, Flyway y Liquibase para acceso funcional o definición estructural.
4. Keycloak autentica y Oracle autoriza con asignaciones exactas de rol, institución y Unidad Ejecutora.
5. La auditoría funcional es atómica y append-only durante la operación normal; la auditoría de acceso no guarda cuerpos, tokens ni contenido documental.
6. La refactorización no autoriza nuevas reglas, transiciones, roles, permisos, endpoints, DTO externos, tablas ni adaptaciones frontend.

### Clasificación por módulo

| Módulo | Evidencia actual | Clasificación | Consecuencia autorizada |
|---|---|---|---|
| `audit` | `AuditQueryService.accesses/events` devuelve `AccessAuditEntity` y `AuditEventEntity`; `AuditController` conoce y mapea esos tipos persistentes. | `DESVIACIÓN CONFIRMADA` | Introducir modelos de lectura no persistentes en application y retirar tipos JPA de API sin cambiar el JSON. |
| `catalogs` | `CatalogController` delega correctamente en `CatalogQueryService`; las transacciones y la resolución de referencias están en application. El retorno interno de `CatalogItemEntity` desde `CatalogReferenceService` puede revisarse, pero no demuestra por sí solo un incumplimiento de la frontera HTTP aprobada. | `CONFORME` con mejora opcional | Conservar el controller delgado y las validaciones `NOT_FOUND`, `WRONG_CATALOG` e `INACTIVE`; no crear trabajo por la mejora opcional. |
| `config` | Configuraciones y `config/reset` cumplen una responsabilidad de infraestructura; no contienen binding HTTP ni reglas funcionales de los casos revisados. | `CONFORME` | Conservar; no forzar capas artificiales ni modificar las guardias de `test-reset`. |
| `dashboard` | `DashboardController.portfolio` delega en `DashboardPortfolioService`; el resumen legado `summary` inyecta tres repositorios, abre transacción, carga entidades, filtra autorización y calcula agregados. | `DESVIACIÓN CONFIRMADA` parcial | Mantener el patrón conforme de `/portfolio` y trasladar el resumen legado a application. |
| `documents` | Los controllers delegan y `DocumentService` conserva un ciclo documental cohesivo; las desviaciones concretas son la dependencia de `DocumentInboxService` al DTO anidado de `OrganizationController` y la recepción de `MultipartFile` por `DocumentService.upload`. | `DESVIACIÓN CONFIRMADA` puntual | Eliminar la dependencia al controller ajeno y realizar el binding multipart en API, sin fragmentar el servicio por cantidad de dependencias. |
| `identity` | `IdentityController.me` inyecta `UserRepository`, carga `UserEntity`, registra autenticación y construye el read model; `LocalAuthorizationService` depende de `AccessDeniedException` y del contexto de seguridad. | `DESVIACIÓN CONFIRMADA` para `IdentityController.me`; mejora opcional para el acoplamiento de seguridad | Delegar el caso de identidad y conservar la integración actual de `LocalAuthorizationService` con Spring Security y HTTP 403. |
| `organization` | `OrganizationController` inyecta tres repositorios, filtra entidades y aplica autorización; no existe capa application en el módulo. | `DESVIACIÓN CONFIRMADA` | Incorporar consultas de aplicación cohesionadas y dejar el controller limitado a HTTP. |
| `portfolio` | `PortfolioController` es delgado; `PortfolioDtos` depende de un DTO anidado de `OrganizationController`; `PortfolioService` coordina portafolio, organización, catálogos, documentos, tareas, notificaciones y auditoría. | `DESVIACIÓN CONFIRMADA` parcial | Conservar el controller y las invariantes válidas; separar casos de uso e integraciones con propiedad modular explícita. |
| `shared` | Excepciones consumidas por application viven en `shared/api`; `ApiExceptionHandler` traduce mensajes libres de excepciones a `ProblemDetail`. | `DESVIACIÓN CONFIRMADA` parcial | Definir errores tipados independientes de HTTP y preservar exactamente la traducción observable vigente. |
| `work` | `WorkController` y `NotificationController` contienen `@Transactional`, inyectan repositorios, cargan o mutan entidades, validan versión, reconstruyen autorización, mapean respuestas y registran auditoría. | `DESVIACIÓN CONFIRMADA` | Crear límites de aplicación para tareas y notificaciones y dejar ambos controllers delgados. |
| Raíz | `PiipApplication` se limita al bootstrap de la aplicación. | `CONFORME` | Conservar sin responsabilidades funcionales nuevas. |

### Señales re-verificadas

| Señal | Evidencia | Clasificación | Decisión para esta feature |
|---|---|---|---|
| Transacciones en API | `WorkController.pending/complete/reassign`, `NotificationController.list/read` y `DashboardController.summary` declaran `@Transactional`. | `DESVIACIÓN CONFIRMADA` | Retirarlas de API y establecerlas en casos de uso application equivalentes. |
| Controllers con persistencia o negocio | `WorkController`, `NotificationController`, `DashboardController.summary`, `OrganizationController` e `IdentityController.me` inyectan repositorios o trabajan con entidades y reglas. | `DESVIACIÓN CONFIRMADA` | Delegación obligatoria en application. |
| Autorización distribuida | Los controllers de work, dashboard y organization combinan llamadas a `LocalAuthorizationService` con comprobaciones manuales sobre grants, instituciones y UE. | `DESVIACIÓN CONFIRMADA` | Centralizar políticas semánticas reutilizables sin fusionar grants de asignaciones distintas. |
| Entidades y DTOs cruzando capas | `AuditQueryService` devuelve JPA; `AuditController` mapea JPA; portfolio y documents dependen de `OrganizationController.OrganizationalUnitResponse`. | `DESVIACIÓN CONFIRMADA` | Modelos de aplicación y DTO HTTP con propietarios claros. |
| Servicios sobredimensionados | `PortfolioService` posee 14 dependencias y coordina responsabilidades funcionales distintas; `DocumentService` agrupa operaciones del mismo ciclo documental y no es incorrecto por cantidad de dependencias. | `DESVIACIÓN CONFIRMADA` solo para `PortfolioService` | Separar portfolio por casos o conjuntos cohesionados, sin una clase por método; conservar documents salvo sus fronteras confirmadas. |
| Excepciones entre capas | `ApiExceptionHandler` captura cualquier `IllegalStateException` como `422` y expone su mensaje; application importa excepciones desde `shared.api`. El acoplamiento de `LocalAuthorizationService` a `SecurityContextHolder` y `AccessDeniedException` es una mejora posible, no un incumplimiento independiente confirmado. | `DESVIACIÓN CONFIRMADA` para la mezcla de fallo técnico/regla esperada; `MEJORA PROPUESTA SIN INCUMPLIMIENTO` para el acoplamiento de seguridad | Tipar las reglas esperadas y preservar status, título, detalle y propiedades externas; no sustituir el contexto de seguridad solo por preferencia. |
| Read model documental | Los cinco campos documentales de `PortfolioRecordResponse` se construyen siempre como `null`; el cliente generado y `PiipHttpRepository` aún los consumen y normalizan. La intención original de mantenerlos no está documentada. | `NO VERIFICABLE` | No eliminarlos, renombrarlos ni cambiar su nulabilidad en esta feature. |
| Pruebas arquitectónicas | Las cuatro pruebas vigentes cubren SQL nativo, `produces`, nombres de bindings y filtros de UE; no impiden repositorios, entidades o transacciones en controllers. | `MEJORA PROPUESTA SIN INCUMPLIMIENTO` y validación aprobada | Proponer nuevas reglas automatizadas sin ejecutar pruebas durante `specify`. |

No quedan marcadores `NEEDS CLARIFICATION` bloqueantes. La compatibilidad estricta y las fronteras objetivo fueron definidas expresamente por la solicitud.

## Clarifications

### Session 2026-08-22

- Q: ¿La feature debe desacoplar también `LocalAuthorizationService` de Spring Security o limitarse a tipar reglas funcionales y evitar el manejo genérico de `IllegalStateException`? → A: Tipar reglas funcionales y corregir el manejo genérico de `IllegalStateException`; conservar la integración actual de `LocalAuthorizationService` con Spring Security.
- Q: ¿La equivalencia funcional debe exigir igualdad byte por byte o equivalencia semántica para valores legítimamente variables? → A: Exigir equivalencia semántica; los valores variables deben seguir las mismas reglas, aunque no sean idénticos entre ejecuciones.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Conservar el comportamiento durante la refactorización (Priority: P1)

Como usuario autenticado de PIIP, continúo realizando las mismas consultas y operaciones con las mismas autorizaciones y resultados, aunque las responsabilidades internas del backend se hayan reorganizado.

**Why this priority**: La refactorización carece de valor si altera contratos, reglas, datos o flujos ya consumidos por el frontend.

**Independent Test**: Se caracterizan solicitudes representativas antes del movimiento y se comparan después; los valores deterministas coinciden y los valores legítimamente variables conservan la misma regla de generación, cálculo y efecto.

**Acceptance Scenarios**:

1. **Given** una solicitud válida y un actor autorizado, **When** se ejecuta antes y después del incremento, **Then** conserva método, ruta, parámetros, estructura JSON, estado HTTP, efecto persistente, auditoría, tareas y notificaciones; los valores variables siguen la misma regla vigente.
2. **Given** una solicitud inválida, no autorizada, inexistente, concurrente o contraria a una regla, **When** se ejecuta antes y después, **Then** conserva estado, tipo de problema, título, detalle y propiedades observables.
3. **Given** el frontend vigente sin modificaciones, **When** consume el backend refactorizado, **Then** no requiere regeneración ni adaptación y completa los mismos flujos.

---

### User Story 2 - Ejecutar casos de uso desde application (Priority: P1)

Como mantenedor del backend, encuentro controllers limitados al contrato HTTP y casos de uso con autorización, transacción, versión, mutación y auditoría en application.

**Why this priority**: Las desviaciones confirmadas concentran reglas y persistencia en la entrada HTTP, dificultando pruebas, reutilización y atomicidad.

**Independent Test**: Una inspección arquitectónica verifica que ningún controller productivo contiene fronteras transaccionales, repositorios, entidades o reglas funcionales.

**Acceptance Scenarios**:

1. **Given** un controller de work, notifications, dashboard, organization o identity, **When** se inspeccionan sus dependencias y métodos, **Then** solo enlaza, valida y delega HTTP y construye la respuesta HTTP.
2. **Given** una operación con escritura, versión y auditoría, **When** ocurre un fallo en cualquier paso, **Then** el caso de uso conserva la atomicidad actual.
3. **Given** una consulta que requiere relaciones persistentes, **When** application construye su read model, **Then** la carga necesaria ocurre dentro de su frontera y API no depende de sesiones persistentes abiertas.

---

### User Story 3 - Aplicar autorización semántica consistente (Priority: P1)

Como responsable de seguridad, mantengo una sola interpretación de las asignaciones exactas de rol, institución y Unidad Ejecutora para cada capacidad funcional.

**Why this priority**: Combinar manualmente roles y coberturas de grants distintos puede ampliar privilegios de forma silenciosa.

**Independent Test**: Los escenarios de grants cruzados, alcance institucional, UE activa, recurso real y revocación concurrente producen exactamente las decisiones vigentes.

**Acceptance Scenarios**:

1. **Given** Consulta externa en una UE y Administrador PIIP en otra, **When** se intenta una operación funcional administrativa sobre la primera, **Then** no se combinan rol y cobertura de asignaciones diferentes.
2. **Given** cobertura institucional exclusiva de Administración de usuarios, **When** se evalúa otra capacidad funcional, **Then** esa cobertura no amplía el permiso operativo.
3. **Given** una asignación revocada durante una operación sensible, **When** se revalida la política vigente, **Then** la operación conserva el rechazo y la ausencia de efectos parciales.

---

### User Story 4 - Mantener fronteras de modelos y errores (Priority: P1)

Como mantenedor, puedo evolucionar un módulo sin que sus entidades, DTOs anidados o excepciones de transporte se filtren a otras capas y módulos.

**Why this priority**: Los cruces actuales acoplan API, application y persistence y convierten movimientos internos en riesgos contractuales.

**Independent Test**: Las reglas arquitectónicas detectan tipos persistentes en API, entidades devueltas por application y DTOs compartidos definidos dentro de controllers.

**Acceptance Scenarios**:

1. **Given** una consulta de auditoría, **When** application entrega el resultado, **Then** API recibe un modelo no persistente y devuelve el mismo JSON vigente.
2. **Given** una Unidad Orgánica usada por portfolio o documents, **When** se construye la respuesta, **Then** ningún consumidor depende de un tipo anidado en `OrganizationController`.
3. **Given** una regla funcional o denegación esperada, **When** cruza la frontera HTTP, **Then** una traducción central entrega el mismo `ProblemDetail` observable.

---

### User Story 5 - Refactorizar por incrementos reversibles (Priority: P2)

Como equipo de desarrollo, puedo mover responsabilidades por módulo o conjunto cohesivo, con caracterización previa y verificación posterior, sin una reescritura masiva.

**Why this priority**: El alcance integral eleva el riesgo si varios contratos y módulos cambian simultáneamente.

**Independent Test**: Cada incremento tiene baseline, propietario, dependencias, pruebas propuestas y un resultado funcionalmente equivalente antes de iniciar el siguiente.

**Acceptance Scenarios**:

1. **Given** el inventario completo, **When** se ordenan incrementos, **Then** primero se caracterizan contratos y políticas compartidas, luego se refactorizan consumidores dependientes.
2. **Given** un módulo clasificado `CONFORME`, **When** se ejecuta la feature, **Then** se conserva salvo el ajuste mínimo imprescindible para eliminar una dependencia confirmada.
3. **Given** un componente nuevo, **When** se revisa, **Then** tiene responsabilidad cohesionada, nombre funcional, dependencias justificadas, propietario modular y pruebas propuestas.

---

### User Story 6 - Prevenir la reintroducción de desviaciones (Priority: P2)

Como arquitecto del producto, dispongo de reglas automatizables que hacen visible una futura violación de las fronteras acordadas.

**Why this priority**: La reorganización solo es sostenible si sus límites pueden verificarse de forma repetible.

**Independent Test**: Las validaciones propuestas fallan ante un ejemplo controlado de repositorio o transacción en controller, entidad persistente expuesta o DTO anidado compartido.

**Acceptance Scenarios**:

1. **Given** el árbol productivo refactorizado, **When** se aplican las reglas arquitectónicas, **Then** se reportan cero violaciones de las fronteras obligatorias.
2. **Given** una futura desviación, **When** se incorpora al código, **Then** al menos una regla focalizada la identifica con archivo y motivo.

### Edge Cases

- Una respuesta depende hoy de una asociación lazy: el read model debe construirse dentro de application sin ampliar la transacción hasta API.
- Dos ejecuciones comparadas producen timestamps, correlaciones, versiones incrementadas o códigos distintos por el estado de partida: la caracterización debe normalizar exclusivamente esos valores y comprobar que conservan su regla de generación, orden y efecto.
- Un controller conserva temporalmente dos constructores para pruebas: el incremento debe mantener la capacidad de prueba sin dejar dependencias de repositorio en API.
- Una política de lectura admite cualquier grant y una de escritura exige Administrador PIIP del mismo grant: no deben unificarse en una comprobación más permisiva.
- Administración de usuarios usa cobertura institucional especializada: no debe reutilizarse como autorización funcional general.
- Una versión optimista queda obsoleta durante el refactor: debe continuar devolviendo `409` sin sobrescribir datos.
- La auditoría funcional falla después de una mutación: ambas deben conservar el rollback conjunto; la auditoría de acceso mantiene su transacción independiente.
- Una entidad rechaza una transición mediante `IllegalStateException`: la tipificación interna no puede cambiar el `422` ni el mensaje que hoy recibe el consumidor.
- Los cinco campos documentales de `PortfolioRecordResponse` continúan siempre nulos: se preservan por compatibilidad y no se presentan como funcionalidad implementada.
- Un módulo no necesita domain o application adicional: no se crean paquetes vacíos ni adaptadores sin responsabilidad verificable.
- `config/reset` usa infraestructura especial autorizada: no se reubica por uniformidad ni se altera su protección fail-closed.
- Una mejora de rendimiento o N+1 aparece durante la revisión: se registra fuera de alcance y no se mezcla con esta refactorización.
- Un cambio interno parece simplificar un DTO: si altera nombre, tipo, nulabilidad, orden semántico o estructura JSON, queda fuera de alcance.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La feature DEBE inventariar y clasificar el 100 % de los módulos productivos del backend antes de autorizar movimientos internos.
- **FR-002**: Solo una `DESVIACIÓN CONFIRMADA` o una decisión arquitectónica expresamente aprobada en esta solicitud PUEDE originar trabajo ejecutable; `MEJORA PROPUESTA SIN INCUMPLIMIENTO`, `NO VERIFICABLE` y antecedentes NO DEBEN ampliar el alcance por sí solos.
- **FR-003**: Toda operación DEBE cumplir el criterio de equivalencia semántica: ante el mismo estado inicial, actor y entrada, conserva respuesta, autorización y efecto funcional; los valores deterministas coinciden y los timestamps, correlaciones, versiones, códigos u otros valores legítimamente variables conservan exactamente su regla de generación, cálculo, orden y efecto.
- **FR-004**: La feature DEBE conservar endpoints, métodos HTTP, rutas, parámetros, requests, responses, nombres, tipos, nulabilidad, estructuras JSON, estados HTTP y errores observables.
- **FR-005**: La feature NO DEBE modificar frontend, cliente API generado, OpenAPI ni exigir su regeneración o adaptación.
- **FR-006**: La feature DEBE conservar entidades, asociaciones, constraints, modelo JPA, esquema Oracle, datos y relaciones existentes, sin migraciones ni cambios estructurales.
- **FR-007**: La feature DEBE conservar autenticación, autorización, roles, ámbitos, transiciones, concurrencia, versiones, auditoría, tareas y notificaciones vigentes.
- **FR-008**: Todo controller productivo DEBE limitarse a binding HTTP, validación de entrada, delegación a application y construcción de la respuesta HTTP.
- **FR-009**: Ningún controller productivo DEBE definir `@Transactional` ni otra frontera transaccional.
- **FR-010**: Ningún controller productivo DEBE inyectar repositorios, cargar o mutar entidades JPA ni depender de tipos persistence.
- **FR-011**: Ningún controller productivo DEBE validar versiones, registrar auditoría funcional, implementar reglas de negocio ni reconstruir autorización desde roles, grants, instituciones o UE.
- **FR-012**: Application DEBE poseer los casos de uso, fronteras transaccionales, autorización funcional, coordinación de repositorios, versión, mutaciones, auditoría y construcción de modelos de respuesta internos.
- **FR-013**: Persistence DEBE encapsular entidades y repositorios JPA; la refactorización NO DEBE introducir SQL nativo, `JdbcTemplate`, procedimientos almacenados, Flyway ni Liquibase.
- **FR-014**: Domain DEBE conservar las invariantes y operaciones propias ya justificadas, sin sustituir JPA por un dominio POJO puro ni crear un dominio artificial.
- **FR-015**: API NO DEBE recibir tipos persistence y application NO DEBE devolver entidades JPA hacia API. Una integración interna entre módulos solo se modificará si existe una desviación adicional confirmada.
- **FR-016**: Los modelos compartidos entre módulos NO DEBEN estar definidos dentro de controllers; cada modelo DEBE tener propietario modular y frontera explícita.
- **FR-017**: Las reglas funcionales esperadas DEBEN representarse mediante errores internos tipados e independientes de HTTP, y `ApiExceptionHandler` NO DEBE tratar cualquier `IllegalStateException` como regla funcional; DEBE conservar la traducción observable vigente a `ProblemDetail`. `LocalAuthorizationService` PUEDE conservar su integración actual con `SecurityContextHolder` y `AccessDeniedException`.
- **FR-018**: Las políticas de autorización reutilizables DEBEN evaluar conjuntamente los datos de una misma asignación exacta y conservar separadas la lectura general, la escritura funcional por UE y la cobertura institucional de Administración de usuarios.
- **FR-019**: `WorkController.pending/complete/reassign` DEBE delegar consultas y comandos de tareas a application, incluida la autorización, versión, reasignación, cálculo de alerta y auditoría.
- **FR-020**: `NotificationController.list/read` DEBE delegar a application la consulta personal y la mutación de lectura, conservando destinatario, orden, payload y `204`.
- **FR-021**: `DashboardController.summary` DEBE delegar agregación, filtros de visibilidad, tareas, alertas y notificaciones a application; `DashboardController.portfolio` y `DashboardPortfolioService` DEBEN conservarse como patrón conforme.
- **FR-022**: `OrganizationController` DEBE delegar las consultas de instituciones, UE y Unidades Orgánicas a application, conservando filtros, orden y autorización vigentes.
- **FR-023**: `IdentityController.me` DEBE delegar el registro de autenticación, carga de usuario y construcción del contexto visible a un caso de uso, conservando exactamente `roleScopes`, roles y ámbitos agregados actuales.
- **FR-024**: `AuditQueryService` DEBE devolver modelos de lectura no persistentes y `AuditController` NO DEBE conocer `AccessAuditEntity`, `AuditEventEntity` ni `UserEntity`.
- **FR-025**: Portfolio y documents DEBEN dejar de depender de `OrganizationController.OrganizationalUnitResponse` y reutilizar un modelo con propietario organizacional independiente de API.
- **FR-026**: `PortfolioService` DEBE separarse por casos de uso o conjuntos funcionales cohesionados cuando la responsabilidad esté confirmada, conservando en una misma transacción las coordinaciones atómicas de portafolio, tareas, notificaciones, documentos y auditoría.
- **FR-027**: API DEBE realizar el binding multipart y entregar a `DocumentService.upload` un input de aplicación equivalente; `DocumentService` PUEDE conservar unido el ciclo documental cohesivo y NO DEBE fragmentarse solo por cantidad de dependencias.
- **FR-028**: `CatalogController`, `CatalogQueryService` y las validaciones de `CatalogReferenceService` DEBEN conservarse como comportamiento conforme; la mejora opcional de su integración interna NO DEBE generar trabajo ejecutable en esta feature.
- **FR-029**: Los cinco campos documentales heredados de `PortfolioRecordResponse` DEBEN mantener nombres, tipos, nulabilidad y valor observable actuales; su eliminación o redefinición requiere una especificación contractual independiente.
- **FR-030**: `config`, `config/reset` y los controllers ya conformes DEBEN conservar su estructura válida salvo el cambio mínimo necesario para resolver una dependencia confirmada.
- **FR-031**: Cada componente nuevo DEBE tener responsabilidad única y cohesionada, nombre funcional, dependencias justificadas, caso de uso o contrato identificable, propietario modular y pruebas propuestas; NO DEBE crearse una clase por método ni una abstracción sin consumidor real.
- **FR-032**: DEBEN proponerse reglas arquitectónicas que impidan transacciones, repositorios, entidades y reconstrucción de autorización en controllers; tipos persistence hacia API; y DTOs compartidos anidados en controllers.
- **FR-033**: Antes de mover cada comportamiento DEBEN proponerse pruebas de caracterización de contratos, errores, autorización y efectos; después DEBEN proponerse pruebas unitarias del caso de uso, atomicidad, auditoría, concurrencia y regresión modular.
- **FR-034**: La implementación posterior DEBE seguir incrementos pequeños, trazables y reversibles en este orden: inventario y caracterización; fronteras compartidas; módulos de menor dependencia; módulos consumidores; verificación de compatibilidad después de cada incremento.
- **FR-035**: El orden concreto DEBE resolverse por dependencias y riesgo; no se DEBE refactorizar en paralelo componentes que compartan contrato, política de autorización, DTO o transacción.
- **FR-036**: La verificación posterior DEBE cubrir MVC y contratos, autorización, atomicidad, auditoría, concurrencia, arquitectura y compatibilidad con consumidores frontend, pero su ejecución requiere autorización explícita en el turno correspondiente.
- **FR-037**: La implementación posterior DEBE actualizar la documentación arquitectónica afectada; no DEBE cambiar la guía funcional si la equivalencia observable se conserva, y DEBE declarar esa ausencia de impacto con evidencia.
- **FR-038**: La feature NO DEBE agregar ni eliminar funcionalidad, optimizar N+1 o rendimiento, migrar a microservicios, introducir Vertical Slice como mandato, reescribir el backend ni alterar credenciales o defaults de pruebas.

### Matriz de trazabilidad de requisitos

| Requisito | Archivo y símbolo actual | Regla y módulo | Criterio de aceptación | Riesgo de regresión |
|---|---|---|---|---|
| FR-001, FR-002 | Inventario `apps/backend/src/main/java/pe/gob/midagri/piip/**`; tablas de clasificación anteriores | Grounding SDD; todos | 10/10 módulos clasificados y cero trabajo originado solo por una propuesta | Alcance incompleto o inventado |
| FR-003, FR-004 | Todos los `*Controller.java`, `*Dtos.java` y `ApiExceptionHandler` | Compatibilidad contractual; todos | Campos deterministas idénticos y valores variables sujetos a la misma regla, sin diferencias semánticas observables | Ruptura del cliente Angular o falsos positivos de regresión |
| FR-005 | `apps/frontend/src/app/api/generated/**`, `piip-http.repository.ts` | Protección del consumidor; cross-domain | Cero archivos frontend/OpenAPI modificados o regenerados | Desalineación de contrato |
| FR-006 | Entidades `**/persistence/*Entity.java`; `docs/architecture/data-model-final.md` | Constitución IV; persistence | Cero diferencias lógicas de modelo o esquema | Pérdida o incompatibilidad de datos |
| FR-007 | `LocalAccessContext`, `PortfolioRecordEntity`, `AuditService`, repositorios work | Constitución II, III y V; todos | Matrices y escenarios funcionales existentes conservados al 100 % | Privilegios, transiciones o efectos alterados |
| FR-008, FR-009, FR-010, FR-011 | `WorkController`, `NotificationController`, `DashboardController.summary`, `OrganizationController`, `IdentityController.me` | Controllers delegan; API | Cero transacciones, repositorios, entidades o reglas funcionales en controllers | Acoplamiento y atomicidad incorrecta |
| FR-012 | Métodos transaccionales actuales de `PortfolioService`, `DocumentService`, `UserAdministrationService` | Límite application | Cada caso conserva autorización, transacción, versión y auditoría | Efectos parciales |
| FR-013 | `PersistencePolicyTest`; repositorios JPA actuales | Constitución IV; persistence | La política sigue rechazando accesos prohibidos | Doble fuente de persistencia |
| FR-014 | `PortfolioRecordEntity.transition*`, `WorkTaskEntity` | Dominio sin artificio; domain | Invariantes vigentes permanecen en el modelo o frontera justificada | Dominio anémico o duplicado |
| FR-015 | `AuditQueryService.accesses/events`; `AuditController.toEventResponse` | Encapsulación persistence; audit | Cero tipos `*Entity` en contratos entre application y API | Lazy loading y acoplamiento JPA |
| FR-016 | `PortfolioDtos.ResponsibleUnitResponse`; `DocumentInboxService` | Propiedad modular; portfolio/documents/organization | Cero imports de DTOs anidados en controllers ajenos | Cambio transversal frágil |
| FR-017 | `PortfolioRecordEntity`, `WorkTaskEntity`, `shared/api/*Exception`, `ApiExceptionHandler`; integración vigente de `LocalAuthorizationService` | Frontera de errores; portfolio/work/shared/identity | Reglas funcionales tipadas con idéntico `ProblemDetail` externo; `LocalAuthorizationService` conserva `SecurityContextHolder` y `AccessDeniedException` | Cambio de 403/404/409/422, mensajes o contexto autorizado |
| FR-018 | `LocalAuthorizationService`, `LocalAccessContext`, comprobación manual en `WorkController.reassign` | Seguridad exacta; identity/work | Grants cruzados y cobertura administrativa no amplían permisos | Escalada horizontal |
| FR-019 | `WorkController.pending/complete/reassign` | API/application; work | Controller solo delega y resultados permanecen idénticos | Tareas, alertas o auditoría alteradas |
| FR-020 | `NotificationController.list/read` | API/application; work | Mismo destinatario, orden, JSON y `204` | Lectura de notificación ajena |
| FR-021 | `DashboardController.summary`; `DashboardPortfolioService.portfolio` | Patrón conforme parcial; dashboard | Resumen idéntico y `/portfolio` sin regresión | Conteos o alcance incorrectos |
| FR-022 | `OrganizationController.institutions/executingUnits/organizationalUnits` | API/application; organization | Mismas colecciones, filtros, orden y autorización | Exposición cross-UE |
| FR-023 | `IdentityController.me`; `LocalAuthorizationService.recordAuthentication` | API/application; identity | Mismo usuario, grants exactos y agregados visibles | Contexto de sesión inconsistente |
| FR-024 | `AuditQueryService` y `AuditController` | No exponer JPA; audit | JSON de las 100 entradas conserva orden y campos | Historial incompleto o filtrado distinto |
| FR-025 | `PortfolioDtos` L7/L15; `PortfolioService.toResponse`; `DocumentInboxService` L11/L37-L40 | Modelo organizacional compartido | Respuestas mantienen exactamente la estructura de Unidad Orgánica | Ruptura de portfolio o bandeja documental |
| FR-026 | `PortfolioService` L29-L42 y casos L65-L215 | Cohesión application; portfolio | Casos separados conservan transacciones multi-módulo atómicas | Reordenamiento de tareas/documentos/auditoría |
| FR-027 | `DocumentController.upload`; `DocumentService.upload` L50-L59 | Binding API/application; documents | API adapta multipart y application conserva bytes, nombre, MIME, límites, checksum, auditoría y respuesta | Pérdida de contenido o contrato multipart roto |
| FR-028 | `CatalogController.get`; `CatalogQueryService.bundle`; `CatalogReferenceService.resolveActive*` | Conservar conformidad; catalogs | Contrato y validaciones `NOT_FOUND`, `WRONG_CATALOG`, `INACTIVE` permanecen idénticos | Sobre-refactorización o referencias inválidas |
| FR-029 | `PortfolioDtos.PortfolioRecordResponse` L78-L79; `PortfolioService.toResponse` L277; `piip-http.repository.ts` L835-L839 | Compatibilidad heredada; portfolio/frontend | Cinco propiedades conservan presencia y nulabilidad actual | Cliente generado o mapper roto |
| FR-030 | `config/**`, `CatalogController`, `PortfolioController`, `DocumentController`, `UserAdministrationController`, `DocumentInboxController` | Conservar conformidad; varios | Cero movimientos sin dependencia confirmada | Sobre-refactorización |
| FR-031 | Componentes application que se propongan | SOLID/KISS/YAGNI; todos | Cada componente demuestra caso, propietario, dependencias y prueba; cero wrappers vacíos | Fragmentación artificial |
| FR-032 | `architecture/*.java` actuales | Trazabilidad y calidad; tests | Reglas propuestas cubren las cuatro prohibiciones nuevas | Reintroducción silenciosa |
| FR-033 | Tests MVC/application existentes por módulo | Caracterización y regresión; tests | Cada movimiento tiene prueba previa y posterior propuesta | Cambio no detectado |
| FR-034, FR-035 | Grafo de dependencias y matriz de módulos de esta spec | Incrementalidad; todos | Un incremento activo por contrato/política compartida y rollback identificable | Reescritura no reversible |
| FR-036 | Suites backend, contrato y consumidores frontend existentes | Validación autorizada; cross-domain | Plan de pruebas completo, ejecución solo con permiso explícito | Evidencia falsa o acciones no autorizadas |
| FR-037 | `docs/architecture/**`, `docs/funcional/guia-funcional-piip.md` | Impacto documental | Arquitectura actualizada; guía funcional sin cambios si equivalencia está probada | Documentación divergente |
| FR-038 | Exclusiones expresas de la solicitud | Alcance; todos | Cero cambios de funcionalidad, rendimiento, despliegue o estilo arquitectónico externo | Expansión incontrolada |

### Key Entities *(include if feature involves data)*

- **Caso de uso de aplicación**: Unidad cohesionada que coordina autorización, persistencia, versión, auditoría y resultado para una capacidad funcional existente.
- **Política de autorización**: Regla semántica reutilizable que evalúa una capacidad sobre grants exactos sin combinar rol y ámbito de asignaciones diferentes.
- **Modelo de lectura**: Representación no persistente construida por application y adaptada por API sin exponer entidades JPA.
- **DTO HTTP**: Contrato externo cuya forma, tipos y nulabilidad deben permanecer sin cambios.
- **Entidad persistente**: Modelo JPA encapsulado en persistence y conservado sin cambios de esquema.
- **Error funcional tipado**: Resultado interno esperado que una frontera central traduce al `ProblemDetail` vigente.
- **Incremento de refactorización**: Conjunto pequeño y reversible de movimientos con baseline, dependencias, riesgo y verificación definidos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los 10 módulos productivos queda clasificado con evidencia actual y sin conclusiones basadas exclusivamente en Graphify o antecedentes externos.
- **SC-002**: Existen cero controllers productivos con fronteras transaccionales, repositorios, carga o mutación de entidades y reconstrucción manual de autorización.
- **SC-003**: Existen cero respuestas de application hacia API que expongan entidades persistentes y cero DTOs compartidos definidos dentro de controllers.
- **SC-004**: El 100 % de los escenarios caracterizados mantiene equivalencia semántica de entrada, respuesta, autorización y efecto funcional; todo valor variable normalizado conserva la misma regla de generación, cálculo, orden y efecto.
- **SC-005**: El 100 % de los escenarios de error caracterizados conserva estado HTTP, tipo, título, detalle y propiedades observables.
- **SC-006**: El 100 % de los escenarios de grants cruzados, UE real, cobertura institucional y revocación concurrente conserva la decisión de autorización vigente.
- **SC-007**: El 100 % de las mutaciones caracterizadas conserva atomicidad entre datos funcionales, versión, tareas, notificaciones y auditoría aplicables.
- **SC-008**: Se modifican cero archivos de frontend, cliente generado, OpenAPI, entidades JPA, DDL, migraciones o configuración Oracle.
- **SC-009**: Las reglas arquitectónicas propuestas cubren el 100 % de las fronteras obligatorias de esta spec y reportan archivo y motivo ante una violación.
- **SC-010**: El 100 % de los componentes nuevos demuestra responsabilidad cohesionada, propietario modular, dependencias justificadas y al menos una prueba propuesta; existen cero capas, wrappers o clases sin caso de uso identificable.
- **SC-011**: El 100 % de los incrementos tiene baseline, riesgo, dependencias, criterio de reversión y verificación posterior antes de iniciar el siguiente.
- **SC-012**: El frontend vigente completa sus flujos representativos contra el backend refactorizado sin cambios ni regeneración de contrato.

## Assumptions

- El checkout actual en la rama `refactor/backend` es la única fuente de verdad para el baseline de esta especificación.
- La arquitectura objetivo es el monolito modular vigente; no se adopta microservicios, Vertical Slice ni dominio POJO puro.
- Los controllers y servicios actualmente conformes son patrones válidos y no un backlog de reescritura.
- Los mensajes y propiedades actuales de `ProblemDetail` se tratan como observables y se caracterizan antes de cambiar la tipificación interna.
- Los cinco campos documentales siempre nulos permanecen por compatibilidad hasta una especificación contractual independiente.
- La separación de servicios se decide por responsabilidades funcionales comprobadas, no por cantidad de líneas, métodos o dependencias.
- Las validaciones se definen en esta fase pero no se ejecutan sin autorización explícita posterior.

## Dependencies and Constraints

- La caracterización contractual y de autorización precede a cualquier movimiento de responsabilidades.
- Las fronteras compartidas de modelos, errores y políticas preceden a los consumidores que dependen de ellas.
- Backend es propietario canónico del comportamiento y contrato; frontend permanece como consumidor protegido y fuera del alcance de cambios.
- JPA y Oracle conservan el modelo actual; no existe trabajo de migración o regeneración de esquema.
- Las pruebas, builds, servicios, contenedores, OpenAPI e integración Oracle requieren autorización explícita en el turno de ejecución.

## Out of Scope

- Cambios de frontend, OpenAPI o cliente Angular generado.
- Nuevos endpoints, campos, DTOs externos, estados HTTP o funcionalidad.
- Nuevos roles, permisos, transiciones o reglas sobre usuarios, instituciones o UE inactivas.
- Cambios de entidades, esquema, DDL, datos, migraciones o tecnología de persistencia.
- Optimización N+1, rendimiento, caché o escalabilidad operativa.
- Microservicios, Vertical Slice obligatorio, dominio POJO puro o reescritura total.
- Credenciales, wallets y defaults de pruebas.
- Implementación, builds, pruebas, servidores, contenedores o migraciones durante `specify`.
