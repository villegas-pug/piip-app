# Modelo de diseño: Fronteras internas del backend

## Alcance del modelo

Este artefacto describe modelos internos, propiedad y transiciones que la refactorización debe conservar. No introduce entidades JPA, columnas, relaciones, constraints, tablas, DDL ni migraciones. Las entidades persistentes actuales siguen siendo la fuente canónica del esquema Oracle.

## Propiedad y relaciones

```text
API DTO existente
    <-> mapper de API
        <-> command/read model de application
            <-> caso de uso transaccional
                <-> repositorios y entidades JPA existentes

OrganizationReadModels.OrganizationalUnitView
    <- OrganizationQueryService
    <- DocumentInboxService
    <- PortfolioReadModelAssembler

LocalAuthorizationService + LocalAccessContext + RoleScopeGrant
    <- casos de uso de identity, organization, documents, work, dashboard y portfolio

errores shared/application/error
    -> ApiExceptionHandler
    -> ProblemDetail existente
```

## Modelos internos

### `OrganizationReadModels`

| Modelo | Campos | Productor | Consumidores |
|--------|--------|----------|--------------|
| `InstitutionView` | `id`, `code`, `name` | `OrganizationQueryService` | `OrganizationController` |
| `ExecutingUnitView` | `id`, `code`, `name`, `institutionId` | `OrganizationQueryService` | `OrganizationController` |
| `OrganizationalUnitView` | `id`, `code`, `name`, `active`, `acronym`, `parentId`, `executingUnitId` | `OrganizationQueryService` o ensambladores dentro de transacción | `OrganizationController`, `DocumentInboxService`, `PortfolioReadModelAssembler` |

Reglas: la consulta activa conserva `findByExecutingUnitIdAndActiveTrueOrderByName`; la lectura histórica de responsables conserva identidad, nombre original y orden sin revalidar como una selección nueva.

### `AuditReadModels`

| Modelo | Campos observables |
|--------|--------------------|
| `AccessAuditView` | `subject`, `roles`, `method`, `path`, `status`, `recordCode`, `correlationId`, `durationMs`, `occurredAt` |
| `AuditEventView` | `event`, `entityCode`, `detail`, `actor`, `actorName`, `actorEmail`, `occurredAt` |

Reglas: máximo 100 entradas, orden descendente vigente, filtro opcional por códigos de la UE y actor visible nullable. Ningún modelo retiene `AccessAuditEntity`, `AuditEventEntity` o `UserEntity`.

### `CurrentIdentityReadModel`

Campos: `subject`, `fullName`, `email`, `roleScopes`, `roles`, `institutionIds`, `executingUnitIds`, `institutionWide`.

`RoleScopeView` contiene `role`, `institutionId`, `executingUnitId`. El orden es institución, UE con `nullsFirst` y rol. Los conjuntos agregados continúan derivados de grants exactos; no sustituyen esos grants para autorizar.

### `DocumentUploadInput`

Contrato: `originalFilename`, `mimeType`, `sizeBytes`, `empty` y `readContent()`.

API implementa el contrato mediante un adaptador de `MultipartFile`; application valida en el orden actual: vacío, tamaño máximo, MIME permitido y recién después lectura. Application conserva saneamiento del nombre, SHA-256, copia defensiva del contenido y traducción del fallo de lectura.

### `WorkTaskReadModels`

`WorkTaskView` contiene `id`, `recordCode`, `type`, `description`, `assignedTo`, `priority`, `status`, `dueDate`, `alert`, `version`. `ReassignCommand` contiene `userSubject` y `version`.

`alert` conserva cuatro resultados: `SIN_PLAZO`, `VENCIDA`, `PROXIMA`, `EN_PLAZO`, calculados respecto de la fecha vigente en la zona por defecto y con el mismo límite de tres días.

### `NotificationReadModels`

`NotificationView` contiene `id`, `type`, `message`, `read`, `createdAt`.

Reglas: solo el destinatario actual puede listar o marcar como leída; la lista conserva orden descendente por creación y la mutación conserva respuesta 204.

### `DashboardSummaryReadModel`

Campos: `initiatives`, `projects`, `alerts`, `pendingTasks`, `notifications`, `portfolioByStatus`.

Reglas: portafolio visible según grants exactos; tareas solo para Administrador PIIP y dentro del ámbito del mismo grant; notificaciones no leídas del usuario; `portfolioByStatus` conserva `LinkedHashMap` y el orden de encuentro actual.

### `PortfolioReadModel`

Conserva todos los campos de `PortfolioDtos.PortfolioRecordResponse`, incluidos los cinco campos documentales heredados con valor `null`, y anida responsables mediante `OrganizationalUnitView` más `originalDesignation` y `displayOrder`.

El read model no retiene `PortfolioRecordEntity`, `CatalogItemEntity`, `ResponsibleUnitEntity` ni `OrganizationalUnitEntity`. `PortfolioReadModelAssembler` lo construye dentro de la transacción del caso de uso.

`PortfolioPageView` contiene `content`, `page`, `size`, `totalElements` y `totalPages`. Sustituye el uso interno de `shared/api/PageResponse` sin cambiar el JSON paginado externo.

### Commands de portfolio

| Command | Origen HTTP | Regla |
|---------|-------------|-------|
| `PortfolioQuery` | parámetros de listas de iniciativas/proyectos | Conserva tipo, búsqueda, status, UE, página, tamaño y orden vigentes. |
| `InitiativeCreateCommand` | `InitiativeCreateRequest` | Mismos campos, Bean Validation previa en API y referencias por ID. |
| `ApprovalCommand` | `ApprovalRequest` | `version` y `observation` sin transformación funcional. |
| `InitiativeTransitionCommand` | `InitiativeStatusTransitionRequest` | `targetStatus`, `version`, `observation` sin ampliar la matriz. |
| `DerivedProjectCommand` | `DerivedProjectRequest` | Mismos campos y código de iniciativa origen. |
| `PreexistingProjectCommand` | `PreexistingProjectRequest` | Mismos campos y resolución backend de `NOT_APPLICABLE`. |
| `ProjectTransitionCommand` | `ProjectStatusTransitionRequest` | `targetStatus`, `version`, `observation` sin ampliar la matriz. |

Los mappers de API copian valores; no aplican defaults, reglas, autorización ni normalización adicional.

### Contratos de integración de portfolio

| Contrato | Input | Efecto |
|----------|-------|--------|
| `PortfolioDocumentService.initializeSlots` | `recordId` | Crea un slot por `DocumentTypeEntity` activo con el orden vigente y se une a la transacción llamante. |
| `PortfolioWorkService` | IDs/códigos del registro, actor y evento funcional | Crea o completa tareas, crea notificaciones y registra eventos de tarea en la misma transacción. |
| `ResponsibleUnitService` | `recordId`, `executingUnitId`, lista ordenada de IDs UO | Valida existencia/activo/pertenencia y persiste nombre original y `displayOrder`. |

Estos contratos no devuelven entidades. Los repositorios siguen siendo internos a application/persistence del módulo propietario.

## Errores internos tipados

| Error | Semántica interna | Traducción HTTP preservada |
|-------|-------------------|----------------------------|
| `NotFoundException` | Recurso esperado inexistente | 404, título `Recurso no encontrado`, detalle vigente |
| `BusinessRuleException` | Regla funcional esperada no satisfecha | 422, título `Regla de negocio`, detalle vigente |
| `InvalidReferenceException` | Referencia inexistente, inactiva, de catálogo incorrecto o fuera de UE | 422, título `Referencia inválida`, más `referenceField`, `referenceId`, `reason` |
| `StaleVersionException` | Versión optimista obsoleta | 409, título `Conflicto de versión`, detalle vigente |
| `AccessDeniedException` | Actor sin asignación/capacidad exacta | 403, título `Acceso denegado`, detalle vigente |

`IllegalStateException` no forma parte del modelo funcional transversal. Cuando una invariante de una entidad use ese tipo, el caso de uso la convierte explícitamente al error funcional correspondiente; otros usos técnicos conservan su naturaleza técnica.

## Transiciones persistentes conservadas

### Portafolio

Se conservan exclusivamente las matrices ratificadas por la constitución 1.2.0. La refactorización no agrega, elimina ni renombra estados o transiciones. Los locks, versiones, timestamps, fechas de cierre y auditoría permanecen en el mismo caso de uso atómico.

### Tarea de trabajo

```text
PENDING --complete--> COMPLETED
PENDING --reassign--> PENDING con nuevo assignedUser
```

Reasignar una tarea no pendiente sigue produciendo `Solo se reasignan tareas pendientes`. Completar una tarea ya no pendiente conserva el comportamiento idempotente actual de `WorkTaskEntity.complete()`.

### Notificación

```text
read=false --markRead--> read=true + readAt
read=true  --markRead--> read=true con la regla actual de actualización
```

### Documento

Se conservan `PENDING`, `LOADED` y `NOT_APPLICABLE`, el contador de versiones, la publicación externa por versión y todas las reglas actuales de visibilidad. No se modifica la persistencia documental.

## Invariantes de autorización

1. Un usuario autenticado sin asignación Oracle activa no obtiene permisos funcionales.
2. Rol, institución y UE deben provenir de la misma asignación para una operación que exige Administrador PIIP.
3. Lectura general y escritura funcional mantienen políticas distintas.
4. La cobertura institucional de Administración de usuarios no amplía otras capacidades.
5. El usuario destino de una reasignación debe poseer una asignación Administrador PIIP válida para la institución y UE de la tarea.
6. La autorización se revalida dentro del caso de uso; no se confía en un agregado preparado por API.

## Ausencia de cambio persistente

- Entidades JPA nuevas: ninguna.
- Entidades JPA modificadas: ninguna.
- Relaciones/constraints modificados: ninguno.
- Tablas/columnas/índices modificados: ninguno.
- DDL, DML o migraciones: ninguno.
- Estado de datos requerido: el mismo baseline del checkout al iniciar cada caracterización.
