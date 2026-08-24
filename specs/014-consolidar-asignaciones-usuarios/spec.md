# Feature Specification: Consolidación de asignaciones de usuarios

**Feature Branch**: `refactor/backend`

**Created**: 2026-08-23

**Status**: Draft

**Input**: User description: "Consolidar la edición, suspensión y reactivación de asignaciones de usuarios, cubriendo backend y frontend, con autorización, auditoría, concurrencia y contrato OpenAPI consistentes."

## Clarifications

### Session 2026-08-23

- Q: ¿Qué entidad representa el tercer campo editable mencionado como “unidad orgánica”? → A: La Unidad Ejecutora actual, representada por `executingUnitId`.
- Q: Si existen varias asignaciones suspendidas exactamente iguales, ¿cuál debe reactivarse? → A: La suspendida más recientemente.
- Q: ¿Qué estado HTTP debe devolver `POST /role-assignments` cuando reactiva una coincidencia suspendida? → A: `201` al crear y `200` al reactivar.
- Q: ¿Cómo debe auditarse una edición, suspensión o reactivación rechazada? → A: Solo mediante auditoría de acceso HTTP con estado y motivo seguro.
- Q: Si una mutación propia se confirma, pero falla el refresco de autorización, ¿qué debe hacer la interfaz? → A: Limpiar Administración, redirigir a `/inicio` y mostrar una acción para reintentar.
- Q: ¿SC-010 debe medir tiempos o una muestra estadística de usuarios? → A: No; la aceptación debe comprobar funcionalmente que un administrador autorizado puede completar todas las acciones de Administración de usuarios y ver el resultado confirmado.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Editar una asignación activa (Priority: P1)

Un `ADMINISTRADOR_PIIP` activo corrige el rol, la institución o la Unidad Ejecutora de una asignación activa propia o de otra persona, dentro de su ámbito administrable, para mantener el acceso funcional correcto sin reemplazar la identidad de la asignación.

**Why this priority**: La asignación vigente determina las capacidades funcionales de la persona y una edición inconsistente puede ampliar o retirar acceso indebidamente.

**Independent Test**: Con una asignación activa y una versión vigente, un administrador cambia uno o más de los tres campos permitidos, guarda y comprueba que se conserva el identificador, se muestra el nuevo acceso y existe evidencia de los valores anteriores y nuevos.

**Acceptance Scenarios**:

1. **Given** una asignación activa dentro de una institución administrable, **When** el administrador cambia rol, institución o Unidad Ejecutora por opciones disponibles, **Then** el sistema actualiza esa misma asignación y no altera otros datos del usuario.
2. **Given** una asignación activa del propio actor, **When** el actor la edita con una combinación válida, **Then** se aplican las mismas reglas de ámbito, unicidad, versión, último administrador y auditoría que para una asignación ajena.
3. **Given** una edición que coincide exactamente con otra asignación activa de la misma persona, **When** se intenta guardar, **Then** la operación se rechaza con un mensaje específico y ninguna asignación cambia.
4. **Given** una edición que retiraría la última cobertura activa de `ADMINISTRADOR_PIIP` de una Unidad Ejecutora, **When** se intenta guardar, **Then** la operación se rechaza y se conserva la cobertura previa.

---

### User Story 2 - Suspender y reactivar de forma segura (Priority: P1)

Un `ADMINISTRADOR_PIIP` retira temporalmente o restablece una asignación sin eliminar al usuario, cambiar su estado general ni administrar su cuenta de Keycloak.

**Why this priority**: La suspensión debe retirar acceso de forma reversible sin perder trazabilidad ni comprometer la continuidad administrativa de una Unidad Ejecutora.

**Independent Test**: Un administrador suspende una asignación ajena activa y luego la reactiva; el registro conserva su identidad, cambia de estado de forma reversible, respeta la versión y deja auditoría en ambas operaciones.

**Acceptance Scenarios**:

1. **Given** una asignación ajena activa dentro del ámbito administrable, **When** el administrador confirma la suspensión, **Then** la asignación queda suspendida sin borrar al usuario ni modificar su cuenta de Keycloak.
2. **Given** una asignación propia activa con rol `CONSULTA_EXTERNA` y el actor conserva otra asignación activa `ADMINISTRADOR_PIIP`, **When** solicita suspenderla, **Then** la suspensión es permitida.
3. **Given** una asignación propia activa con rol `ADMINISTRADOR_PIIP`, **When** el actor intenta suspenderla, **Then** el sistema rechaza siempre la operación, aunque existan otros administradores activos.
4. **Given** una asignación suspendida sin duplicado activo exacto, **When** un administrador autorizado la reactiva con la versión vigente, **Then** recupera el estado activo conservando su identidad.
5. **Given** una reactivación que produciría un duplicado activo exacto, **When** se intenta confirmar, **Then** la operación se rechaza sin modificar datos.

---

### User Story 3 - Actualizar el acceso del usuario autenticado (Priority: P1)

Un administrador que modifica una asignación propia ve reflejados de inmediato su rol efectivo, sus Unidades Ejecutoras disponibles, su navegación y sus acciones permitidas.

**Why this priority**: Mantener en el navegador un contexto anterior a la mutación permitiría mostrar acciones ya no autorizadas o esconder capacidades recién concedidas.

**Independent Test**: El actor edita una asignación propia, el sistema vuelve a obtener su identidad funcional y sus ámbitos, y la navegación se reconcilia antes de permitir otra acción administrativa.

**Acceptance Scenarios**:

1. **Given** una autoedición que conserva acceso administrativo a la Unidad Ejecutora activa, **When** la operación termina, **Then** el contexto de autorización y la bandeja se actualizan sin recarga manual.
2. **Given** una autoedición que elimina el acceso administrativo a la Unidad Ejecutora activa, **When** la operación termina, **Then** la vista administrativa se limpia, las acciones dejan de estar disponibles y el usuario es llevado a una ruta permitida.
3. **Given** una autoedición cuyo cambio persistió pero cuyo refresco de contexto falla, **When** el frontend detecta el fallo, **Then** limpia Administración de usuarios, redirige a `/inicio`, informa que el acceso puede estar desactualizado y muestra una acción para reintentar el refresco.

---

### User Story 4 - Obtener errores y auditoría consistentes (Priority: P2)

Un administrador recibe un resultado específico y accionable ante validaciones, falta de autorización, referencias ausentes, conflictos de versión o reglas de negocio; a la vez, cada cambio exitoso queda trazado de forma atómica.

**Why this priority**: La administración de acceso requiere distinguir por qué una operación fue rechazada y demostrar quién realizó cada cambio.

**Independent Test**: Para cada categoría de error se provoca una condición conocida y se verifica el estado HTTP, el `ProblemDetail`, el mensaje visible y la ausencia de cambios; para cada éxito se verifica un único evento funcional asociado a la asignación.

**Acceptance Scenarios**:

1. **Given** una versión desactualizada, **When** se intenta editar, suspender o reactivar, **Then** el sistema responde `409`, no sobrescribe el cambio vigente y solicita recargar la información.
2. **Given** un actor que perdió autorización o intenta operar fuera de su institución administrable, **When** envía la mutación, **Then** el sistema responde `403` y no modifica ni audita un éxito funcional.
3. **Given** una regla de negocio incumplida, **When** la operación se rechaza con `422`, **Then** la interfaz distingue duplicado, autosuspensión administrativa, último administrador o estado incompatible.
4. **Given** una operación exitosa, **When** se confirma la transacción, **Then** el cambio y su evento funcional quedan confirmados juntos con actor, acción, asignación, antes, después, resultado y fecha.

---

### User Story 5 - Reutilizar una coincidencia suspendida (Priority: P2)

Un administrador que intenta conceder una combinación ya existente en estado suspendido recupera esa asignación en lugar de crear otra fila histórica equivalente.

**Why this priority**: Reutilizar la asignación suspendida evita identidades duplicadas para la misma combinación y conserva la trazabilidad reversible.

**Independent Test**: Con una coincidencia exacta suspendida y ninguna activa, se solicita la asignación; el sistema reactiva una coincidencia existente, devuelve su identificador y no incrementa la cantidad de asignaciones históricas exactas.

**Acceptance Scenarios**:

1. **Given** una coincidencia exacta suspendida, **When** se solicita conceder usuario, rol, institución y Unidad Ejecutora iguales, **Then** se reactiva una asignación existente y no se crea otra.
2. **Given** una coincidencia exacta activa, **When** se solicita la misma combinación, **Then** se rechaza como duplicado activo.
3. **Given** varias coincidencias históricas suspendidas por datos preexistentes, **When** se solicita la misma combinación, **Then** se reactiva únicamente la suspendida más recientemente, no se crea otra y la identidad elegida queda auditada.

### Edge Cases

- El actor pierde su última asignación administrativa entre la carga de la pantalla y el envío de la mutación.
- La asignación cambia de versión después de abrir el diálogo, incluso si el formulario conserva valores válidos.
- La asignación ya está suspendida al solicitar suspensión o ya está activa al solicitar reactivación.
- La institución o Unidad Ejecutora dejó de estar activa, no existe o la Unidad Ejecutora ya no pertenece a la institución seleccionada.
- Una autoedición cambia `ADMINISTRADOR_PIIP` a `CONSULTA_EXTERNA` y el actor deja de poder entrar al módulo desde la Unidad Ejecutora activa.
- Una asignación institucional de `ADMINISTRADOR_PIIP` cubre varias Unidades Ejecutoras; antes de editarla o suspenderla se debe validar la cobertura resultante en cada Unidad Ejecutora afectada.
- Una Unidad Ejecutora tiene varios administradores; suspender a uno ajeno es válido si permanece al menos otra asignación administradora activa que la cubra.
- Existe una asignación suspendida exacta y, concurrentemente, otra operación crea o reactiva la misma combinación.
- El cambio funcional se confirma, pero el refresco posterior de identidad del navegador falla.
- Una respuesta `422` llega con una regla desconocida para la versión del frontend; se debe mostrar el detalle seguro del contrato sin presentarla como éxito.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir mutaciones de asignaciones únicamente a un actor con al menos una asignación local activa y vigente de `ADMINISTRADOR_PIIP`.
- **FR-002**: Cada mutación DEBE revalidar los permisos persistidos del actor en el momento de la operación y no confiar únicamente en el contexto obtenido al iniciar la sesión o cargar la pantalla.
- **FR-003**: El sistema DEBE limitar la administración a instituciones donde el actor conserve una asignación activa y vigente de `ADMINISTRADOR_PIIP`.
- **FR-004**: La edición DEBE conservar el identificador de la asignación y permitir modificar exclusivamente rol, institución y Unidad Ejecutora.
- **FR-005**: La edición DEBE estar disponible para asignaciones activas propias y ajenas dentro del ámbito administrable.
- **FR-006**: Los roles seleccionables DEBEN limitarse a los roles PIIP existentes y activos; esta feature NO DEBE crear roles nuevos.
- **FR-007**: Las instituciones y Unidades Ejecutoras seleccionables DEBEN proceder de los ámbitos administrables vigentes que el sistema ya expone para Administración de usuarios.
- **FR-008**: Una Unidad Ejecutora DEBE poder conservar varias asignaciones activas de `ADMINISTRADOR_PIIP`.
- **FR-009**: Después de cualquier edición o suspensión, cada Unidad Ejecutora afectada DEBE conservar al menos una asignación activa de `ADMINISTRADOR_PIIP` que la cubra, incluida una asignación institucional cuando corresponda.
- **FR-010**: La unicidad activa DEBE evaluarse por usuario, rol, institución y Unidad Ejecutora; el alcance institucional se representa como Unidad Ejecutora ausente y constituye un valor distinto de cualquier Unidad Ejecutora concreta.
- **FR-011**: El sistema DEBE rechazar una creación, edición o reactivación que produzca una combinación activa exacta duplicada.
- **FR-012**: Si una solicitud de asignación coincide exactamente con una o más asignaciones suspendidas y no existe una coincidencia activa, el sistema DEBE reactivar exclusivamente la suspendida más recientemente en lugar de crear otra.
- **FR-013**: La suspensión DEBE ser reversible, conservar la asignación y retirar su vigencia funcional sin eliminarla físicamente.
- **FR-014**: La suspensión NO DEBE eliminar al usuario, modificar el estado general del usuario ni crear, desactivar o eliminar su cuenta de Keycloak.
- **FR-015**: Un `ADMINISTRADOR_PIIP` DEBE poder suspender asignaciones ajenas `ADMINISTRADOR_PIIP` y `CONSULTA_EXTERNA` cuando se cumplen ámbito, versión, estado y cobertura mínima.
- **FR-016**: Un `ADMINISTRADOR_PIIP` DEBE poder suspender una asignación propia `CONSULTA_EXTERNA` cuando conserva autorización administrativa por otra asignación vigente.
- **FR-017**: El sistema DEBE rechazar siempre la suspensión de la asignación propia `ADMINISTRADOR_PIIP`, independientemente de cuántos administradores adicionales existan.
- **FR-018**: Una asignación suspendida DEBE poder reactivarse únicamente por un actor que todavía conserve autorización administrativa sobre su institución.
- **FR-019**: Editar, suspender y reactivar DEBEN exigir el estado compatible de la asignación: activa para editar o suspender, suspendida para reactivar.
- **FR-020**: Editar, suspender y reactivar DEBEN exigir una versión esperada y rechazar con conflicto cualquier versión desactualizada o actualización concurrente.
- **FR-021**: Después de una mutación exitosa sobre una asignación propia, el frontend DEBE volver a obtener la identidad funcional, los ámbitos disponibles y las Unidades Ejecutoras antes de habilitar nuevas acciones dependientes de permisos.
- **FR-022**: Después del refresco de una mutación propia, el frontend DEBE recalcular el rol efectivo, las acciones, la navegación y la validez de la Unidad Ejecutora activa.
- **FR-023**: Si el actor deja de administrar la Unidad Ejecutora activa, el frontend DEBE limpiar la vista administrativa, impedir el uso del contexto anterior y navegar a una ruta permitida.
- **FR-024**: Después de cualquier operación exitosa, el frontend DEBE actualizar la bandeja y el detalle de asignaciones con la representación confirmada por el backend.
- **FR-025**: El frontend DEBE mostrar `Editar` y `Suspender` únicamente para asignaciones activas, y `Reactivar` únicamente para asignaciones suspendidas.
- **FR-026**: El frontend DEBE impedir la acción de autosuspensión de una asignación `ADMINISTRADOR_PIIP`; el backend conserva la validación autoritativa aunque se invoque el endpoint directamente.
- **FR-027**: El frontend DEBE permitir la autosuspensión de `CONSULTA_EXTERNA` solo cuando el actor conserve una asignación administrativa separada que autorice la operación.
- **FR-028**: Las operaciones fallidas NO DEBEN modificar datos ni registrar un resultado funcional exitoso.
- **FR-029**: Cada edición, suspensión y reactivación exitosa DEBE producir un único evento funcional dentro de la misma transacción que el cambio.
- **FR-030**: El evento funcional DEBE identificar actor, acción, identificador de asignación, usuario afectado, valores anteriores, valores nuevos, resultado y fecha, sin tokens, cuerpos HTTP, credenciales ni contenido documental.
- **FR-031**: Los intentos rechazados DEBEN quedar observables únicamente mediante la auditoría de acceso HTTP con su estado y un motivo seguro; NO DEBEN crear eventos funcionales de éxito ni de fallo.
- **FR-032**: Los controllers DEBEN limitarse a validar y adaptar la solicitud HTTP, delegando autorización, reglas, concurrencia, transacción y auditoría a las capas de aplicación y dominio.
- **FR-033**: Los contratos HTTP NO DEBEN exponer entidades persistentes.
- **FR-034**: Todas las respuestas de error DEBEN usar `ProblemDetail` y conservar un discriminador estable que permita al frontend distinguir reglas con el mismo estado HTTP.
- **FR-035**: El contrato OpenAPI DEBE reflejar las operaciones, solicitudes, respuestas correctas, versiones, estados de error y discriminadores de reglas realmente publicados por el backend.
- **FR-036**: Frontend y backend DEBEN aplicar la misma matriz de permisos y estados, manteniendo al backend como validación autoritativa.
- **FR-037**: `POST /role-assignments` DEBE responder `201` cuando crea una asignación nueva y `200` cuando reactiva una coincidencia suspendida existente.
- **FR-038**: Esta feature NO DEBE cambiar autenticación, roles disponibles, catálogos organizacionales ni el estado general de `USUARIO`.
- **FR-039**: Si una mutación propia se confirma pero falla el refresco de autorización, el frontend DEBE limpiar Administración de usuarios, redirigir a `/inicio`, advertir que el acceso puede estar desactualizado y ofrecer una acción para reintentar; NO DEBE conservar acciones administrativas basadas en el contexto anterior.

### Key Entities *(include if feature involves data)*

- **Usuario local**: Identidad previamente provisionada en PIIP; su estado general y su cuenta institucional no forman parte de esta feature.
- **Asignación de rol y ámbito**: Registro versionado que une usuario, rol, institución y Unidad Ejecutora opcional; conserva identidad, estado activo/suspendido y vigencia.
- **Rol PIIP**: Rol funcional existente (`ADMINISTRADOR_PIIP` o `CONSULTA_EXTERNA`) que se evalúa junto con su ámbito.
- **Institución y Unidad Ejecutora**: Límites organizacionales actuales de la asignación. Una Unidad Ejecutora ausente representa cobertura institucional.
- **Contexto de autorización**: Conjunto vigente de asignaciones exactas del usuario autenticado utilizado para calcular rol efectivo, Unidades Ejecutoras y funcionalidades.
- **Evento funcional de auditoría**: Evidencia atómica de una mutación exitosa con actor, asignación, antes, después, resultado y fecha.
- **Auditoría de acceso**: Evidencia separada de la solicitud HTTP y su estado, incluida una operación rechazada, sin sustituir el evento funcional exitoso.

### Matriz de permisos y operaciones

| Actor al momento de mutar | Asignación objetivo | Editar activa | Suspender activa | Reactivar suspendida | Condiciones adicionales |
|---------------------------|---------------------|---------------|------------------|----------------------|------------------------|
| `ADMINISTRADOR_PIIP` activo | Propia `ADMINISTRADOR_PIIP` | Permitido | Prohibido siempre | Permitido si otra asignación activa conserva la autorización del actor | Ámbito administrable, versión, unicidad y cobertura mínima |
| `ADMINISTRADOR_PIIP` activo | Propia `CONSULTA_EXTERNA` | Permitido | Permitido | Permitido | El actor conserva una asignación administrativa separada y vigente |
| `ADMINISTRADOR_PIIP` activo | Ajena `ADMINISTRADOR_PIIP` | Permitido | Permitido | Permitido | No dejar ninguna Unidad Ejecutora afectada sin administrador activo |
| `ADMINISTRADOR_PIIP` activo | Ajena `CONSULTA_EXTERNA` | Permitido | Permitido | Permitido | Ámbito administrable, versión, estado y unicidad |
| Actor sin `ADMINISTRADOR_PIIP` activo | Cualquiera | Prohibido | Prohibido | Prohibido | Respuesta `403`, sin cambio funcional |

### Estados y transiciones

| Estado actual | Operación | Estado resultante | Regla |
|---------------|-----------|-------------------|-------|
| Activa | Editar | Activa | Conserva identificador; valida versión, ámbito, duplicado y cobertura administrativa |
| Activa | Suspender | Suspendida | Conserva registro; fija fin de vigencia; prohíbe autosuspensión administrativa |
| Suspendida | Reactivar | Activa | Renueva vigencia; valida versión, referencias, ámbito y duplicado activo |
| Suspendida exacta | Solicitar asignación equivalente | Activa | Reactiva una coincidencia existente; no crea una nueva |
| Activa | Reactivar | Sin cambio | Rechazo `422` por estado incompatible |
| Suspendida | Editar o suspender | Sin cambio | Rechazo `422` por estado incompatible |

No existe transición a eliminación física. Estas transiciones no modifican `USUARIO.ACTIVO` ni el estado de la cuenta Keycloak.

### Contratos API

El prefijo funcional vigente es `/api/v1`; las rutas de Administración de usuarios se publican bajo `/admin`, el refresco funcional conserva `/identity/me` y la consulta de accesos conserva `/audit/accesses`. El backend es propietario del contrato y el cliente frontend se sincroniza después de publicar OpenAPI.

| Método y ruta | Entrada | Éxito esperado | Semántica consolidada |
|---------------|---------|----------------|-----------------------|
| `GET /api/v1/admin/users` | Sin cuerpo | `200 UserResponse[]` | Devuelve asignaciones activas y suspendidas visibles dentro de las instituciones administrables |
| `GET /api/v1/admin/users/administrable-scopes` | Sin cuerpo | `200 AdministrableScopeResponse[]` | Provee instituciones, opción institucional y Unidades Ejecutoras activas para los combos |
| `PUT /api/v1/admin/role-assignments/{scopeId}?version={expectedVersion}` | `RoleAssignmentUpdateRequest { role, institutionId, executingUnitId? }` | `200 ScopeResponse` | Edita la misma asignación activa y devuelve su representación vigente |
| `DELETE /api/v1/admin/role-assignments/{scopeId}?version={expectedVersion}` | Sin cuerpo | `204` | Suspende una asignación activa sin eliminarla |
| `PUT /api/v1/admin/role-assignments/{scopeId}/reactivation?version={expectedVersion}` | Sin cuerpo | `200 ScopeResponse` | Reactiva una asignación suspendida |
| `POST /api/v1/admin/role-assignments` | `RoleAssignmentRequest { userSubject, role, institutionId, executingUnitId? }` | `201 ScopeResponse` si crea; `200 ScopeResponse` si reactiva coincidencia suspendida | Evita duplicado activo y reutiliza una coincidencia suspendida |
| `GET /api/v1/identity/me` | Sin cuerpo | `200 CurrentUserResponse` | Fuente para refrescar el contexto funcional después de una mutación propia |
| `GET /api/v1/audit/accesses?executingUnitId={executingUnitId?}` | Sin cuerpo; Unidad Ejecutora opcional | `200 AccessAuditResponse[]` | Devuelve accesos HTTP visibles dentro del ámbito autorizado, incluido `safeReason` nullable para rechazos |

`ScopeResponse` debe conservar como mínimo `id`, `role`, `institutionId`, `institution`, `executingUnitId`, `executingUnit`, `active`, vigencias y `version`. Los identificadores del path y la versión son obligatorios para las mutaciones sobre una asignación existente.

`AccessAuditResponse` debe publicar `subject`, `roles`, `method`, `path`, `status`, `recordCode`, `safeReason`, `correlationId`, `durationMs` y `occurredAt`. `safeReason` es nullable y solo puede contener un código seguro; nunca `detail`, cuerpos HTTP, tokens, credenciales ni contenido documental.

### Modelo de errores

| Estado | Categoría | Casos mínimos | Mensaje o discriminación requerida |
|--------|-----------|---------------|------------------------------------|
| `400` | `INVALID_REQUEST` | JSON mal formado, campo obligatorio ausente, tipo o valor no admitido | Indicar el campo o que el cuerpo no cumple el contrato |
| `403` | `FORBIDDEN_SCOPE` | Actor sin administrador vigente, institución fuera del ámbito, permiso perdido antes de mutar | Indicar que el actor no tiene autorización sobre el ámbito |
| `404` | `RESOURCE_NOT_FOUND` | Usuario, asignación, rol, institución o Unidad Ejecutora inexistente | Identificar de forma segura la referencia ausente |
| `409` | `STALE_VERSION` | Versión desactualizada o conflicto optimista | Indicar que la información cambió y debe recargarse |
| `422` | `ACTIVE_ASSIGNMENT_DUPLICATE` | Creación, edición o reactivación produciría duplicado activo exacto | Mensaje específico de asignación activa duplicada |
| `422` | `SELF_ADMIN_SUSPENSION` | Actor intenta suspender su propia asignación `ADMINISTRADOR_PIIP` | Mensaje específico de autosuspensión administrativa prohibida |
| `422` | `LAST_ACTIVE_ADMIN` | Edición o suspensión dejaría una Unidad Ejecutora sin administrador activo | Mensaje específico de último administrador/cobertura mínima |
| `422` | `INCOMPATIBLE_ASSIGNMENT_STATE` | Editar o suspender una suspendida; reactivar una activa | Mensaje específico del estado incompatible |
| `422` | `INVALID_ACTIVE_REFERENCE` | Rol, institución o Unidad Ejecutora existe pero no está activa o no pertenece a la institución | Mensaje específico de referencia no utilizable |
| `422` | `BUSINESS_RULE_VIOLATION` | Regla funcional controlada que todavía no dispone de un código más específico | Presentar un mensaje seguro sin inferir la regla desde `detail` |

`ProblemDetail.problemCode` es obligatorio. Los valores publicados son `INVALID_REQUEST`, `FORBIDDEN_SCOPE`, `RESOURCE_NOT_FOUND`, `STALE_VERSION`, `ACTIVE_ASSIGNMENT_DUPLICATE`, `SELF_ADMIN_SUSPENSION`, `LAST_ACTIVE_ADMIN`, `INCOMPATIBLE_ASSIGNMENT_STATE`, `INVALID_ACTIVE_REFERENCE` y `BUSINESS_RULE_VIOLATION`. Este último es únicamente el fallback de compatibilidad para una `BusinessRuleException` sin código específico.

### Auditoría y concurrencia

- La autorización persistida del actor, el bloqueo/lectura de la asignación, la comparación de versión, las reglas, la mutación y el evento funcional forman una única frontera transaccional.
- Las consultas que evitan duplicados y protegen cobertura administrativa deben serializar operaciones concurrentes sobre el mismo usuario y los ámbitos administrativos afectados.
- Una comparación previa en el frontend solo mejora la experiencia; no sustituye la validación autoritativa dentro de la transacción.
- El evento funcional se confirma o revierte con la mutación. Una operación rechazada se registra únicamente en la auditoría HTTP, confirmada por separado con estado y motivo seguro, sin evento funcional adicional.
- Los valores `antes` y `después` deben representar los cuatro componentes de la asignación y su estado; no basta identificar únicamente al usuario afectado.

### Hallazgos del comportamiento actual

La evidencia siguiente es estática; no se ejecutaron pruebas, builds, servidores, Oracle ni Keycloak durante `specify`.

| ID | Evidencia actual | Evaluación frente a esta feature |
|----|------------------|----------------------------------|
| `H-001` | `UserAdministrationController` ya publica edición, suspensión y reactivación; delega en `UserAdministrationService`. | Coincide con controllers delgados y las rutas base; el contrato debe evolucionar sin exponer entidades. |
| `H-002` | `currentAdministrator()` vuelve a resolver las asignaciones persistidas del actor en cada operación (`UserAdministrationService.java:146-150`). | Coincide con la revalidación backend exigida. |
| `H-003` | `assign()` rechaza duplicados activos, pero crea una fila nueva sin buscar una coincidencia suspendida (`UserAdministrationService.java:75-88`). | Gap: falta reactivación automática de la coincidencia suspendida. |
| `H-004` | `suspend()` protege al último administrador, pero no compara el usuario objetivo con el actor (`UserAdministrationService.java:115-127`). | Gap: hoy una autosuspensión `ADMINISTRADOR_PIIP` puede superar la regla si existe otro administrador. |
| `H-005` | `update()` conserva identificador, limita el request a rol/institución/UE, valida estado, versión, ámbito, duplicado y último administrador (`UserAdministrationService.java:91-113`). | Coincide en lo esencial; falta garantizar el refresco frontend cuando la asignación pertenece al actor. |
| `H-006` | La UI refresca autorización después de suspender o reactivar una asignación propia (`user-administration.component.ts:304-356`), pero `saveEdit()` solo recarga la bandeja (`:256-280`). | Gap: la autoedición puede dejar identidad, navegación y acciones desactualizadas. |
| `H-007` | La UI muestra acciones por estado y permite editar asignaciones propias o ajenas, pero no oculta ni bloquea la autosuspensión administrativa (`user-administration.component.html:61-74`). | Gap de prevención frontend; el backend también debe ser autoritativo. |
| `H-008` | Los combos actuales usan `administrable-scopes`, roles existentes y `executingUnitId`; no usan el catálogo de Unidad Orgánica (`edit-user-assignment-dialog.component.html:10-13`). | La aclaración confirma que el campo editable es la Unidad Ejecutora actual; no existe gap de modelo en este punto. |
| `H-009` | `ApiExceptionHandler` ya traduce validación a `400`, autorización a `403`, ausencia a `404`, versión a `409` y reglas a `422`; la UI presenta `422` de forma genérica (`user-administration.component.ts:479-488`). | Gap: faltan discriminadores y mensajes específicos por regla en el frontend. |
| `H-010` | Los eventos `ROL_ACTUALIZADO`, `ROL_SUSPENDIDO` y `ROL_REACTIVADO` participan en la transacción, pero identifican la entidad por subject del usuario y registran antes/después de forma parcial. | Gap: falta identificar la asignación, explicitar resultado y completar antes/después según el prompt. |
| `H-011` | Existe cliente generado para las rutas actuales, pero no hay un artefacto OpenAPI generado versionado que demuestre en esta fase el contrato consolidado. | La publicación/regeneración queda para una fase autorizada posterior; no se ejecuta durante `specify`. |

### Riesgos

- La coexistencia histórica de varias coincidencias suspendidas exige ordenar por el momento de suspensión y aplicar pruebas de concurrencia para reactivar únicamente la más reciente.
- Cambiar una asignación propia puede retirar el permiso necesario para completar cargas posteriores; el refresco debe ocurrir inmediatamente después de confirmar la mutación.
- Basar mensajes específicos en texto libre de `detail` acoplaría frontend y backend; se requiere un discriminador contractual estable.
- La protección actual de “último administrador del ámbito” debe validarse contra la regla consolidada por cada Unidad Ejecutora, especialmente con asignaciones institucionales.
- Cambiar códigos de éxito o error sin sincronizar OpenAPI y cliente generado puede romper el consumo frontend.

### Exclusiones

- Crear, eliminar, habilitar o deshabilitar cuentas de Keycloak.
- Crear o eliminar el agregado `USUARIO` o modificar `USUARIO.ACTIVO`.
- Eliminar físicamente asignaciones.
- Cambiar la autenticación, issuer, audience, PKCE o manejo de tokens.
- Añadir roles o modificar catálogos institucionales, de Unidades Ejecutoras u organizacionales.
- Incorporar una relación nueva con Unidad Orgánica distinta de la Unidad Ejecutora actual.
- Cambiar capacidades funcionales ajenas a Administración de usuarios.
- Implementar código, generar OpenAPI, regenerar el cliente, acceder a Oracle o ejecutar pruebas/builds durante esta fase.

### Plan de validación

1. **Contrato estático**: revisar controller, DTO, `ProblemDetail`, OpenAPI publicado y cliente generado para cada ruta, estado y discriminador.
2. **Backend focalizado**: validar matriz de actor/objetivo, revalidación persistida, autoedición, autosuspensión, último administrador por UE, duplicados, reactivación automática, referencias, versión y rollback de auditoría.
3. **Persistencia y concurrencia**: validar carreras de edición/suspensión/reactivación, creación contra coincidencia suspendida y cobertura administrativa concurrente.
4. **Frontend focalizado**: validar acciones por estado y propiedad, combos, mensajes por error, bloqueo de autosuspensión administrativa, refresco de identidad y navegación tras mutaciones propias, y recarga de bandeja/detalle.
5. **Integración contractual**: comparar OpenAPI generado con el cliente consumido sin editar código generado manualmente.
6. **E2E autorizado**: recorrer los criterios mínimos con usuarios y asignaciones controlados, incluido cambio de permisos del actor y pérdida de acceso a la UE activa.
7. **Regresión**: verificar que autenticación, cuentas Keycloak, estado general de usuario, roles, catálogos y demás módulos no cambian.

La ejecución de pruebas, builds, servidores, OpenAPI, Oracle o E2E requiere una autorización explícita posterior y no forma parte de `specify`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las ediciones válidas modifica únicamente rol, institución o Unidad Ejecutora y conserva el identificador de la asignación.
- **SC-002**: El 100 % de los intentos de autosuspender una asignación propia `ADMINISTRADOR_PIIP` es rechazado sin cambios, incluso cuando existen otros administradores.
- **SC-003**: El 100 % de las mutaciones que dejarían una Unidad Ejecutora sin administrador activo es rechazado sin reducir la cobertura válida.
- **SC-004**: El 100 % de las creaciones, ediciones y reactivaciones que producirían un duplicado activo exacto es rechazado sin crear ni activar otra coincidencia.
- **SC-005**: El 100 % de las solicitudes con una coincidencia suspendida exacta y ninguna activa reactiva una asignación existente y crea cero filas nuevas.
- **SC-006**: Cuando el refresco de autorización está disponible, después del 100 % de las mutaciones propias exitosas el rol efectivo, las Unidades Ejecutoras y las acciones visibles se recalculan antes de la siguiente acción del usuario, sin requerir recarga manual.
- **SC-007**: El 100 % de las versiones desactualizadas se rechaza con `409` sin sobrescribir el estado confirmado más reciente.
- **SC-008**: El 100 % de las mutaciones exitosas genera exactamente un evento funcional con actor, asignación, antes, después, resultado y fecha; ninguna operación rechazada genera un evento funcional de éxito.
- **SC-009**: En pruebas de aceptación, el 100 % de los errores `400`, `403`, `404`, `409` y `422` definidos muestra un mensaje específico y accionable coherente con el backend.
- **SC-010**: En pruebas de aceptación, un `ADMINISTRADOR_PIIP` autorizado puede asignar, auto-reactivar, editar, suspender y reactivar asignaciones permitidas, y la bandeja y el detalle muestran el resultado confirmado por el backend.
- **SC-011**: El contrato publicado y el cliente consumidor representan el 100 % de las operaciones, campos, versiones, éxitos y errores definidos en esta especificación.
- **SC-012**: El 100 % de los escenarios de exclusión verificados conserva sin cambios las cuentas Keycloak, `USUARIO.ACTIVO`, los roles existentes y los catálogos organizacionales.
- **SC-013**: En el 100 % de los fallos simulados de refresco posteriores a una mutación propia confirmada, Administración de usuarios queda limpia, la navegación termina en `/inicio`, se muestra una advertencia y existe una acción de reintento.

## Assumptions

- La fuente de verdad es el backend actual; la evidencia de frontend, cliente generado, specs previas y documentación se usa para contraste y no para inventar reglas.
- El tercer campo editable es la Unidad Ejecutora opcional de la asignación actual, representada por `executingUnitId`; no corresponde a la entidad Unidad Orgánica responsable.
- Una asignación institucional de `ADMINISTRADOR_PIIP` cubre las Unidades Ejecutoras activas de su institución al evaluar la conservación de al menos un administrador.
- El actor que suspende o reactiva una asignación propia `CONSULTA_EXTERNA` conserva una asignación `ADMINISTRADOR_PIIP` separada; de lo contrario no superaría la revalidación de autorización.
- Si existen varias coincidencias suspendidas históricas, se reactiva exclusivamente la suspendida más recientemente, no se crea otra y la identidad elegida queda auditada.
- La bandeja y el detalle mencionados corresponden a la lista agrupada y al detalle expandido de asignaciones existentes; no se crea una pantalla adicional por este requisito.
