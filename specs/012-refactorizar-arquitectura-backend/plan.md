# Plan de implementación: Refactorizar la arquitectura backend

**Rama**: `refactor/backend` | **Fecha**: 2026-08-22 | **Spec**: [spec.md](./spec.md)

**Entrada**: especificación en `/specs/012-refactorizar-arquitectura-backend/spec.md`

**Nota**: este artefacto define diseño y secuencia. No autoriza implementación, pruebas, compilación, servidores, contenedores, generación OpenAPI, integración Oracle ni acciones Git.

## Resumen

Refactorizar incrementalmente el backend PIIP para que los controllers se limiten a HTTP y los casos de uso de application posean autorización, transacciones, versión, persistencia coordinada, auditoría y modelos de lectura. La solución conserva el monolito modular, el modelo JPA/Oracle y todos los contratos observables; no modifica frontend, OpenAPI, cliente generado, endpoints, DTO externos, estados, roles ni reglas funcionales.

El diseño corrige únicamente desviaciones confirmadas: entidades JPA devueltas por auditoría, DTO organizacional propiedad de un controller, `MultipartFile` en application, lógica de identidad/organización/dashboard/work en API, errores internos ubicados en `shared/api`, manejo genérico de `IllegalStateException` y responsabilidades distintas concentradas en `PortfolioService`. `LocalAuthorizationService` conserva `SecurityContextHolder` y `AccessDeniedException`; se amplía solo con operaciones semánticas que evalúen grants exactos sin combinar rol y cobertura de asignaciones diferentes.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| Los controllers deben delegar y las transacciones pertenecen a application. | `AGENTS.md`; `.specify/memory/constitution.md`; `spec.md` FR-008 a FR-012 | Mover las desviaciones confirmadas sin reescribir controllers ya conformes. |
| `AuditQueryService` devuelve `AccessAuditEntity` y `AuditEventEntity`; `AuditController` conoce JPA. | `apps/backend/src/main/java/pe/gob/midagri/piip/audit/**` | Introducir read models de auditoría en application y mapearlos a las respuestas HTTP vigentes. |
| `OrganizationController` inyecta tres repositorios y `LocalAuthorizationService`; su DTO de Unidad Orgánica es importado por portfolio y documents. | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationController.java`; `portfolio/application/PortfolioService.java`; `documents/application/DocumentInboxService.java` | Crear consultas y vistas propiedad de `organization/application`; conservar records HTTP como adaptadores de API. |
| `IdentityController.me` carga `UserEntity`, registra autenticación y agrega grants. | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/IdentityController.java` | Crear un caso de uso de identidad actual; conservar integración y mensajes de seguridad existentes. |
| `WorkController`, `NotificationController` y `DashboardController.summary` contienen transacciones, repositorios, entidades, autorización y mapeo. | `apps/backend/src/main/java/pe/gob/midagri/piip/{work,dashboard}/api/**` | Trasladar cada operación a servicios cohesionados de application; mantener rutas, DTO y estados HTTP. |
| `DashboardController.portfolio` ya delega en `DashboardPortfolioService`. | `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/application/DashboardPortfolioService.java` | Conservarlo como patrón; no unificarlo artificialmente con el resumen legado. |
| `DocumentService` implementa un ciclo documental cohesivo, pero recibe `MultipartFile`; `DocumentInboxService` importa un DTO de controller. | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/**` | Crear un input binario independiente de Spring MVC y reutilizar la vista organizacional; no fragmentar el ciclo documental. |
| `PortfolioService` posee consultas, registros, aprobaciones, transiciones y coordinación con cuatro módulos. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java` | Separar consultas, comandos de iniciativa y comandos de proyecto; asignar a work/documents sus efectos y compartir un ensamblador de read model. |
| Los errores funcionales viven en `shared/api`; el handler captura cualquier `IllegalStateException` como 422. | `apps/backend/src/main/java/pe/gob/midagri/piip/shared/api/**` | Mover errores internos a una frontera independiente de HTTP y traducir solo errores funcionales tipados. |
| Las pruebas actuales cubren contratos y reglas funcionales, pero las cuatro pruebas arquitectónicas no impiden estas desviaciones. | `apps/backend/src/test/java/pe/gob/midagri/piip/**`; `architecture/**` | Reutilizar pruebas focalizadas como caracterización y agregar reglas ArchUnit/estáticas propuestas. |
| `catalogs`, `config`, `config/reset`, `PiipApplication` y varios controllers ya delegados son conformes. | Clasificación de `spec.md` | Mantenerlos fuera del trabajo ejecutable, salvo imports mínimos exigidos por una frontera confirmada. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | No | N/A; protección de `apps/frontend/src/app/api/generated/**` y consumidores | Consumidor protegido; no se adapta ni regenera. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/{audit,dashboard,documents,identity,organization,portfolio,shared,work}/**` y pruebas equivalentes | Propietario canónico del comportamiento y de las fronteras internas. |
| Database | No | N/A; protección de entidades JPA y `database/generated/piip-oracle.sql` | JPA/Oracle permanecen sin cambios. |
| Contrato HTTP | No | Endpoints y DTO actuales de los ocho módulos afectados | Backend preserva forma, status y `ProblemDetail`; OpenAPI y cliente no cambian. |
| Documentación | Sí | `specs/012-refactorizar-arquitectura-backend/**`; durante implementación, `docs/architecture/backend-modular-architecture.md` | La arquitectura se documenta; la guía funcional no cambia si se demuestra equivalencia. |

## Contexto técnico

**Lenguajes/versiones**: Java 21 y Spring Boot 4.1.0; Angular 22 permanece como consumidor fuera de alcance.

**Dependencias principales**: Spring MVC, Spring Security OAuth2 Resource Server, Spring Data JPA, Hibernate ORM, Bean Validation, Springdoc existente, Oracle JDBC y ArchUnit 1.4.1. No se incorpora ninguna dependencia nueva.

**Persistencia**: Hibernate JPA sobre Oracle. Se conservan todas las entidades, repositorios, asociaciones, constraints, tablas, datos y configuración; no se introduce SQL, DDL ni migración.

**Validación propuesta**: caracterización MVC/JSON/`ProblemDetail`; pruebas unitarias de casos de uso; autorización por grant exacto; atomicidad, auditoría y concurrencia; reglas ArchUnit; regresión modular; y compatibilidad estática con el contrato consumido por Angular. Su ejecución requiere autorización explícita posterior.

**Plataforma objetivo**: monolito modular PIIP autenticado por Keycloak y autorizado con asignaciones Oracle de rol, institución y Unidad Ejecutora.

**Restricciones**: equivalencia semántica sin regresión; coincidencia exacta para valores deterministas y conservación de la regla para timestamps, correlaciones, versiones y códigos legítimamente variables; `LocalAuthorizationService` mantiene Spring Security; no se crean capas vacías, una clase por método, dominio POJO paralelo ni abstracciones sin consumidor.

**Escala/alcance**: 10 módulos productivos clasificados; cambios ejecutables en 8 módulos (`audit`, `dashboard`, `documents`, `identity`, `organization`, `portfolio`, `shared`, `work`); 3 módulos/áreas conformes protegidos (`catalogs`, `config`, raíz); todos los endpoints externos afectados congelados en la matriz contractual; cero cambios de esquema o frontend.

## Verificación de la constitución

*GATE: debe aprobarse antes del diseño y volver a revisarse al finalizarlo.*

### Gate inicial

- **I. Fuente funcional**: aprobado. La refactorización no redefine los 23 campos, catálogos, obligatoriedades ni semántica de `NA`/`No aplica`.
- **II. Estados y transiciones**: aprobado. Se conservan las matrices ratificadas; los movimientos internos no autorizan nuevas transiciones.
- **III. Organización y seguridad**: aprobado. Keycloak sigue autenticando y Oracle autorizando. Las políticas usan grants exactos y no fusionan rol con ámbitos de otra asignación.
- **IV. Persistencia**: aprobado. No se alteran JPA, Oracle, DDL ni tecnología de acceso; no se usa la excepción de DML/reset.
- **V. Trazabilidad y calidad**: aprobado. La auditoría funcional permanece en la misma transacción de la mutación y la auditoría de acceso conserva su transacción independiente; se proponen pruebas antes de implementar.
- **Grounding SDD**: aprobado. Graphify se usó como índice y las decisiones se confirmaron en la spec, código, pruebas, constitución y protocolo Spec Kit.

### Gate posterior al diseño

Aprobado. Los contratos de [contracts/http-compatibility.md](./contracts/http-compatibility.md) congelan la superficie externa; [data-model.md](./data-model.md) declara que no hay cambios persistentes; [research.md](./research.md) resuelve todas las decisiones técnicas sin `NEEDS CLARIFICATION`; y la secuencia ubica primero errores/modelos/políticas compartidos, después consumidores y al final portfolio. No se usa ninguna excepción constitucional ni se agrega complejidad no justificada.

## Dependencias y secuencia

- **Propietario canónico**: backend para comportamiento, autorización, errores internos y adaptación HTTP; `organization/application` para la vista organizacional compartida; `work/application` y `documents/application` para efectos de sus módulos iniciados por portfolio.
- **Consumidores**: controllers backend y servicios de `documents`/`portfolio`; frontend solo consume el contrato externo congelado y no se modifica.
- **Orden obligatorio**: caracterización → errores tipados y reglas arquitectónicas base → vista/consultas de organization → audit e identity → documents → work/notifications → dashboard summary → portfolio → documentación y verificación final.
- **Paralelización permitida**: solo después de congelar la frontera compartida y en archivos sin contrato, política, DTO, transacción o fixture común. Audit e identity pueden prepararse en paralelo tras shared; no se paralelizan organization con documents/portfolio, identity con work/dashboard/portfolio, ni work/documents con portfolio.

### Incrementos reversibles

| Incremento | Resultado y rutas principales | Dependencias | Caracterización y criterio de reversión |
|------------|-------------------------------|--------------|-----------------------------------------|
| 0. Baseline | Congelar matrices HTTP, errores, autorización y efectos en tests existentes y nuevos casos de caracterización. | Ninguna | No mover código hasta que cada endpoint afectado tenga expectativas observables; revertir el incremento si aparece una salida sin baseline. |
| 1. Shared | Crear errores en `shared/application/error/**`; adaptar `ApiExceptionHandler`; retirar el catch genérico de `IllegalStateException`; agregar reglas arquitectónicas. | Incremento 0 | Los errores funcionales conservan 403/404/409/422, títulos, detalles y propiedades; un fallo técnico no se clasifica como regla funcional. |
| 2. Organization | Crear `OrganizationQueryService` y `OrganizationReadModels`; adaptar `OrganizationController`; establecer `OrganizationalUnitView` como modelo propietario. | Incremento 1 | Mismas colecciones, orden, filtros y autorización; si cambia el JSON o alcance, restaurar el controller y no avanzar a consumidores. |
| 3. Audit e identity | Crear `AuditReadModels` y mapear JPA dentro de `AuditQueryService`; crear `CurrentIdentityService`/read model y adelgazar `IdentityController.me`; mantener `LocalAuthorizationService`. | Incrementos 1-2; entre sí no comparten contrato externo | Auditoría conserva máximo, orden y actor nullable; identidad conserva registro de autenticación, `roleScopes`, agregados y orden. |
| 4. Documents | Reemplazar el DTO de controller por `OrganizationalUnitView`; introducir `DocumentUploadInput` y un adaptador multipart en API; mantener cohesivo `DocumentService`; crear `PortfolioDocumentService.initializeSlots(recordId)` para la integración de portfolio. | Incrementos 1-2 | Orden de validación, lectura, bytes, nombre saneado, MIME, límite, checksum, slot, auditoría y respuesta se mantienen; rollback aislado al adaptador y modelo interno. |
| 5. Work y notifications | Crear `WorkTaskService`, `WorkTaskReadModels`, `NotificationService` y `NotificationReadModels`; centralizar en `LocalAuthorizationService` la elegibilidad exacta de reasignación; crear `PortfolioWorkService` para efectos de portfolio. | Incrementos 1 y 3 | Misma pertenencia, orden, alerta, 204, versión y auditoría; cada controller queda sin repositorios/JPA/`@Transactional`. |
| 6. Dashboard summary | Crear `DashboardSummaryService` y `DashboardSummaryReadModel`; mover visibilidad, conteos, tareas, alertas y notificaciones; conservar `DashboardPortfolioService`. | Incremento 5 y política de identity | `GET /dashboard` conserva conteos y orden de `portfolioByStatus`; `/dashboard/portfolio` no se altera. |
| 7. Portfolio | Separar `PortfolioQueryService`, `InitiativeApplicationService` y `ProjectApplicationService`; centralizar mapeo en `PortfolioReadModelAssembler`; consumir `PortfolioDocumentService` y `PortfolioWorkService`; retirar fachada transitoria sin responsabilidad. | Incrementos 1-6 | Cada caso conserva una única transacción, locks/versiones, orden de tareas/notificaciones/documentos/auditoría y DTO externo; revertir caso por caso, nunca todo el módulo a la vez. |
| 8. Cierre arquitectónico | Completar reglas ArchUnit, actualizar documentación arquitectónica y comprobar ausencia de cambios protegidos. | Todos | Cero controllers desviados, cero JPA hacia API, cero DTO compartido en controllers y cero cambios en frontend/OpenAPI/JPA/DDL. |

## Estructura del proyecto

### Documentación de la feature

```text
specs/012-refactorizar-arquitectura-backend/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── http-compatibility.md
├── checklists/
│   └── requirements.md
└── tasks.md                    # Se generará mediante /speckit-tasks
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/
├── audit/{api,application}/
├── dashboard/{api,application}/
├── documents/{api,application}/
├── identity/{api,application}/
├── organization/{api,application}/
├── portfolio/{api,application}/
├── shared/{api,application/error}/
└── work/{api,application}/

apps/backend/src/test/java/pe/gob/midagri/piip/
├── architecture/
├── audit/
├── dashboard/
├── documents/
├── identity/
├── organization/
├── portfolio/
├── shared/
└── work/

docs/architecture/backend-modular-architecture.md
```

**Decisión de estructura**: cada módulo conserva `api`, `application`, `domain` y `persistence` solo cuando ya existen o una responsabilidad confirmada los justifica. API posee binding y DTO HTTP; application posee casos de uso, transacciones, autorización y read models; domain conserva invariantes existentes; persistence conserva JPA. Los modelos compartidos tienen un único propietario modular y no se crea un paquete vacío para uniformar módulos.

## Diseño por responsabilidad

### API y modelos de aplicación

- Los controllers mantienen rutas, parámetros, validaciones Bean Validation, anotaciones de status y records externos.
- Los controllers cuya desviación incluye lógica o JPA adaptan request/response HTTP a commands/read models de application. Los componentes ya conformes no se modifican solo por uniformidad. La adaptación no contiene reglas funcionales, repositorios, entidades ni transacciones.
- `OrganizationReadModels.OrganizationalUnitView` es el único modelo organizacional interno reutilizable por organization, documents y portfolio. Cada API conserva sus records externos para no convertir un modelo interno en contrato accidental.
- `AuditReadModels`, `CurrentIdentityReadModel`, `WorkTaskReadModels`, `NotificationReadModels`, `DashboardSummaryReadModel` y `PortfolioReadModel` se construyen dentro de transacciones de lectura y no conservan entidades ni asociaciones lazy.
- `DocumentUploadInput` es un contrato de application con metadatos y lectura diferida de bytes. Un adaptador en `documents/api` envuelve `MultipartFile`, permitiendo que `DocumentService` conserve el orden actual vacío → tamaño → MIME → lectura y el mismo error observable.

### Errores y traducción HTTP

- `BusinessRuleException`, `InvalidReferenceException`, `NotFoundException` y `StaleVersionException` pasan a `shared/application/error` manteniendo nombres, mensajes y propiedades.
- `ApiExceptionHandler` depende de esos errores y conserva la traducción vigente a `ProblemDetail`.
- `IllegalStateException` deja de ser una regla funcional genérica. Las invariantes de portfolio/work se capturan en el caso de uso y se convierten explícitamente a `BusinessRuleException` con el mismo detalle. Un `IllegalStateException` técnico no se traduce a 422.
- `AccessDeniedException` continúa siendo producido por `LocalAuthorizationService` y traducido a 403; no se introduce una excepción de autorización paralela.

### Autorización

- `LocalAuthorizationService` conserva lectura de `SecurityContextHolder`, resolución de `LocalAccessContext`, persistencia de última autenticación y mensajes actuales.
- Las comprobaciones reutilizables operan sobre una asignación exacta: lectura por UE, Administrador PIIP por UE y elegibilidad del usuario destino para reasignación. La cobertura institucional de Administración de usuarios permanece separada.
- La revalidación ocurre dentro del caso de uso y de la transacción aplicable; no se pasan decisiones de autorización precomputadas desde API.
- `CurrentIdentityService` no agrega una transacción exterior que cambie el commit independiente de `recordAuthentication`; conserva la secuencia actual de contexto, registro de autenticación y lectura del usuario.

### Transacciones, auditoría y concurrencia

- Las consultas que construyen read models JPA son `@Transactional(readOnly = true)` en application.
- Cada mutación mantiene una sola frontera `@Transactional` que incluye versión, cambio de entidad, tareas/notificaciones y `AuditService.event` cuando corresponda.
- `AuditService.access` conserva `REQUIRES_NEW`; `AuditService.event`, `PortfolioWorkService` y `PortfolioDocumentService` se unen a la transacción funcional.
- `CodeGeneratorService.next` conserva su propagación, bloqueo y formato vigentes.
- Los locks `findByCodeIgnoreCaseForUpdate`, `@Version`, `flush()` y mensajes 409 se conservan en los mismos casos funcionales.
- La fecha de alerta usa `LocalDate.now()` o un `Clock.systemDefaultZone()` inyectable equivalente; no se cambia implícitamente a UTC.

### Integraciones iniciadas por portfolio

- `PortfolioDocumentService.initializeSlots(recordId)` pertenece a `documents/application`, carga el registro dentro de la transacción llamante y crea un slot por tipo documental activo en el orden vigente.
- `PortfolioWorkService` pertenece a `work/application` y concentra creación/completado de tareas, notificaciones y eventos de tarea iniciados por altas/aprobaciones de portfolio.
- Ambos contratos usan IDs, códigos y datos de actor; no devuelven entidades. Su propagación debe ser `REQUIRED` o equivalente para conservar rollback conjunto.

### Portfolio

- `PortfolioQueryService` posee lista, detalle y elegibilidad, incluida la especificación de visibilidad y paginación.
- `InitiativeApplicationService` posee registro, aprobación y transiciones de iniciativa y recibe commands de application equivalentes a los request DTO actuales.
- `ProjectApplicationService` posee registro derivado, registro preexistente y transiciones de proyecto y recibe commands de application equivalentes a los request DTO actuales.
- `PortfolioReadModelAssembler` concentra catálogos, Unidades Orgánicas responsables y los cinco campos documentales heredados en `null`.
- `PortfolioQueryService` devuelve `PortfolioPageView`/`PortfolioReadModel`; `PortfolioController` construye `PageResponse` y `PortfolioDtos` sin pasar tipos API a application.
- La persistencia de Unidades Orgánicas responsables puede extraerse a `ResponsibleUnitService`; se crea solo porque la consumen los dos servicios de comandos y mantiene validación, nombre original y orden.
- `PortfolioService` puede actuar como fachada temporal durante un incremento, pero se elimina al final si solo delega y no representa una responsabilidad cohesionada.

## Estrategia de verificación propuesta

1. **Contrato HTTP**: pruebas MVC de método, ruta, parámetros, validación, status, estructura JSON, orden, nulabilidad y headers de descarga.
2. **Errores**: tabla de 400/403/404/409/422 con `type`, `title`, `detail` y propiedades de referencia; caso técnico que confirme que `IllegalStateException` no se clasifica como negocio.
3. **Autorización**: grants cruzados, institución, UE real, usuario destino, cobertura administrativa y revocación concurrente sin privilegios combinados.
4. **Audit**: máximo 100, orden descendente, filtro por UE, actor nullable y ausencia de entidades JPA fuera de application.
5. **Organization e identity**: orden/filtros actuales, modelo compartido independiente de API, registro de autenticación y agregación exacta de scopes.
6. **Documents**: bytes, copia defensiva, MIME, tamaño, checksum, nombre saneado, publicación, descarga, `No aplica`, auditoría, notificación e inicialización de slots dentro de la transacción llamante.
7. **Work/notifications/dashboard**: pertenencia al usuario, alerta temporal, versión, reasignación, 204, conteos, mapa de estados y efectos iniciados por portfolio.
8. **Portfolio**: consultas, tres formas de creación, aprobación, matrices de transición, locks, rollback de efectos coordinados y cinco campos heredados nulos.
9. **Arquitectura**: reglas que prohíban `@Transactional`, repositorios, entidades y reconstrucción de autorización en controllers; JPA hacia API; DTOs compartidos anidados en controllers; y dependencias de application hacia `shared.api` para errores.
10. **Protección cross-domain**: diff estático con cero archivos en `apps/frontend/**`, OpenAPI generado, entidades JPA, `database/**`, configuración Oracle y guía funcional.

Ninguna verificación automatizada se ejecuta durante `plan`; su ejecución posterior requiere autorización explícita.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: frontend, OpenAPI, cliente Angular, endpoints/DTO externos, JPA/Oracle/DDL/datos, catálogos conformes, `config/reset`, nuevas reglas o transiciones, rendimiento/N+1, microservicios, Vertical Slice, dominio POJO puro y reescritura masiva.
- **Specs `001`-`005` consultadas**: `004` solo como antecedente de identidad/autorización; no genera backlog ni amplía roles o ámbitos.
- **Dependencias históricas aprobadas**: matrices de transición de feature 009 ratificadas por la constitución 1.2.0; contratos vigentes de features 008, 009, 010 y 011 se conservan como baseline.
- **NEEDS CLARIFICATION**: Ninguna.

## Seguimiento de complejidad

No existen contradicciones constitucionales ni excepciones que justificar. La separación propuesta usa servicios por responsabilidad funcional confirmada; no introduce puertos, adaptadores, eventos, buses, repositorios alternativos ni capas sin un consumidor real.
