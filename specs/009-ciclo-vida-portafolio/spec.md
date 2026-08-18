# Feature Specification: Ciclo de vida del portafolio PIIP

**Feature Branch**: `009-ciclo-vida-portafolio`

**Created**: 2026-08-18

**Status**: Draft

**Input**: User description: "Ampliar la gestión de estados de iniciativas y proyectos sin romper el flujo actual, con cambios contextuales desde el detalle, control de ámbito, concurrencia, auditoría y cierre de proyectos."

## Clarifications

### Session 2026-08-18

- Q: ¿Qué matriz debe regir los estados de proyecto en la primera versión? → A: Matriz controlada: `Proyecto en ejecución` puede pasar a `Producto aprobado`, `Producto no aprobado`, `Suspendido` o `Cancelado`; `Suspendido` puede pasar a `Proyecto en ejecución` o `Cancelado`; `Producto no aprobado` puede pasar a `Proyecto en ejecución` o `Cancelado`; `Producto aprobado` puede pasar a `Finalizado`; `Cancelado` y `Finalizado` son terminales.
- Q: ¿Qué matriz debe regir los estados adicionales de iniciativa mientras no exista proyecto vinculado? → A: `Presentado` puede pasar a `Iniciativa aprobada`, `No Admisible` o `Iniciativa archivada`; `Iniciativa aprobada` sin proyecto puede pasar a `Iniciativa archivada`; `No Admisible` e `Iniciativa archivada` son terminales.
- Q: ¿Cómo se determina `closingDate` al pasar un proyecto a `Finalizado`? → A: Se asigna automáticamente la fecha local de `America/Lima` cuando la transición se confirma exitosamente; el usuario no introduce ni edita esa fecha.
- Q: ¿Debe conservarse `SC-010` como métrica de usabilidad? → A: No. Se elimina porque no proviene del requerimiento y carece de un protocolo de medición; `SC-003` y `SC-009` conservan la validación contextual solicitada.

## Clasificación de la definición

### Hechos confirmados por el repositorio

- Una iniciativa se registra en `Presentado`.
- La operación existente `POST /initiatives/{code}/approval` cambia únicamente una iniciativa `Presentado` a `Iniciativa aprobada`, recibe `version` y `observation`, exige `Administrador PIIP` sobre la Unidad Ejecutora real del registro y responde con `PortfolioRecordResponse`.
- Aprobar una iniciativa no crea automáticamente un proyecto. Un proyecto derivado se crea mediante una operación separada y nace en `Proyecto en ejecución`.
- La relación de derivación está representada por `REGISTRO_PORTAFOLIO.ID_REGISTRO_ORIGEN` y es única. “Proyecto derivado” no es un estado.
- La iniciativa conserva `Iniciativa aprobada` después de crear el proyecto derivado. La existencia del proyecto puede consultarse por la relación de origen.
- `PortfolioStatus` y `PiipStatus` contienen los estados adicionales `Iniciativa archivada`, `Producto aprobado`, `Producto no aprobado`, `Suspendido`, `Cancelado`, `Finalizado`, `No Aplicable` y `No Admisible`, pero su presencia en el catálogo no confirma transiciones operativas.
- `REGISTRO_PORTAFOLIO` ya contiene `ESTADO`, `FECHA_CIERRE` y una única columna `VERSION` gestionada con control optimista. Un conflicto de versión se representa como HTTP `409`.
- `PortfolioService` concentra las reglas y transacciones del portafolio; `PortfolioRecordEntity` protege la transición de aprobación; `AuditService.event(...)` participa en la transacción funcional existente.
- `EVENTO_AUDITORIA` conserva el tipo de evento, la entidad afectada, un detalle JSON, el actor y la fecha. La auditoría de acceso conserva además método, ruta, código de respuesta, roles y correlación.
- El frontend tiene detalle propio para iniciativas, pero los proyectos se abren actualmente en su expediente documental; no existe una ruta ni un componente de detalle general de proyecto.
- Los listados de iniciativas y proyectos ya permiten consultar y filtrar por estado.

### Decisiones funcionales propuestas para esta versión

- Mantener sin cambios funcionales la aprobación actual y su ruta.
- Incorporar una matriz controlada de iniciativa mientras no exista proyecto vinculado: `Presentado` puede aprobarse, declararse no admisible o archivarse; `Iniciativa aprobada` puede archivarse; `No Admisible` e `Iniciativa archivada` son terminales.
- Incorporar un detalle general de proyecto y ubicar allí el selector contextual de estado.
- Aplicar una matriz controlada de proyecto: desde `Proyecto en ejecución` se permite aprobar o no aprobar el producto, suspender o cancelar; desde `Suspendido` se permite reanudar o cancelar; desde `Producto no aprobado` se permite volver a ejecución o cancelar; desde `Producto aprobado` solo se permite finalizar; `Cancelado` y `Finalizado` son terminales.
- Separar rutas, requests, opciones visuales y validaciones de iniciativa y proyecto para impedir que sus estados se mezclen.
- Excluir `No Aplicable` de toda opción de transición de esta versión.
- Registrar la observación del usuario en la evidencia de auditoría, sin reemplazar ni modificar el campo operativo `Nota`.
- Registrar solo una transición exitosa como evento funcional atómico. Los intentos fallidos conservan el resultado HTTP en la auditoría de acceso existente, sin crear un evento funcional que pueda confundirse con un cambio aplicado.
- Al entrar exitosamente en `Finalizado`, establecer `closingDate` con la fecha local de `America/Lima` correspondiente a esa transición. El usuario no introduce la fecha y cualquier transición a otro estado deja `closingDate` sin cambios.

### Inferencias de diseño derivadas de las convenciones actuales

- Las nuevas mutaciones se modelan como subrecursos de transición con `POST`, siguiendo la operación de aprobación existente, en lugar de convertir los listados o el catálogo general en puntos de escritura.
- Las nuevas operaciones reutilizan `PortfolioRecordResponse`, `ProblemDetail`, la columna `VERSION`, el mapa de versiones vigente del repositorio frontend y el manejo actual del `409`.
- Para que la comprobación “la iniciativa no tiene proyecto vinculado” no compita con una creación simultánea del proyecto, ambas operaciones deben serializar el acceso a la iniciativa origen mediante el mecanismo de bloqueo JPA compatible con la arquitectura vigente.
- No se requiere una nueva tabla de transiciones, de versiones ni de auditoría: las reglas aprobadas pertenecen al dominio y el evento se registra en la infraestructura existente.

### Reglas pendientes

- **[NEEDS CLARIFICATION: definir en una versión posterior qué documentos, si alguno, deben bloquear cada transición. En esta primera versión los documentos pendientes solo se informan y nunca bloquean.]**

### Fuentes canónicas consultadas

- `docs/architecture/piip-fields.md`: campos operativos y catálogos controlados.
- `docs/funcional/guia-funcional-piip.md`: recorrido confirmado, relación iniciativa-proyecto y límite de las transiciones actualmente demostradas.
- `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java` y `PortfolioDtos.java`: rutas, métodos y contratos HTTP existentes.
- `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java`: autorización, transacciones, tareas, notificaciones y auditoría del flujo actual.
- `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` y `PortfolioRecordRepository.java`: estado, relación de origen, `closingDate`, versión y unicidad.
- `apps/backend/src/main/java/pe/gob/midagri/piip/audit/**` y `shared/api/ApiExceptionHandler.java`: evidencia de auditoría y convenciones de error.
- `apps/frontend/src/app/app.routes.ts`, `core/piip.repository.ts`, `core/piip-http.repository.ts`, `core/piip.models.ts` y `pages/**`: navegación, repositorio de presentación, versiones, conflictos y componentes actuales.
- `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/**`: evidencia focalizada de aprobación y autorización por ámbito.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Conservar el recorrido vigente (Priority: P1)

Un Administrador PIIP registra y aprueba una iniciativa y después crea explícitamente su proyecto derivado, sin que la ampliación del ciclo de vida cambie los estados ni las operaciones existentes.

**Why this priority**: La nueva capacidad solo es válida si no rompe el recorrido funcional que ya está confirmado y utilizado.

**Independent Test**: Se registra una iniciativa, se aprueba con la operación actual y se crea su proyecto derivado; se comprueban los tres estados esperados y la relación única entre ambos registros.

**Acceptance Scenarios**:

1. **Given** una nueva iniciativa válida, **When** se registra, **Then** queda en `Presentado`.
2. **Given** una iniciativa `Presentado` sin proyecto vinculado, **When** un Administrador PIIP autorizado confirma la aprobación con la versión vigente, **Then** la iniciativa queda en `Iniciativa aprobada` y no se crea un proyecto automáticamente.
3. **Given** una iniciativa `Iniciativa aprobada` sin proyecto vinculado, **When** un Administrador PIIP autorizado crea el proyecto derivado, **Then** el proyecto nace en `Proyecto en ejecución` y queda vinculado de manera única a la iniciativa.
4. **Given** un proyecto derivado ya creado, **When** se consulta la iniciativa origen, **Then** la iniciativa conserva `Iniciativa aprobada` y muestra la relación con el proyecto, sin presentar “Proyecto derivado” como estado.

---

### User Story 2 - Proteger la iniciativa después de la derivación (Priority: P1)

Un usuario consulta el detalle de una iniciativa que ya originó un proyecto y reconoce que su estado quedó bloqueado para preservar la trazabilidad entre ambos registros.

**Why this priority**: Cambiar la iniciativa después de crear el proyecto podría contradecir el origen que justifica la existencia del proyecto.

**Independent Test**: Con una iniciativa aprobada y un proyecto vinculado, se comprueba la ausencia de controles de cambio y el rechazo autoritativo de una llamada directa o simultánea.

**Acceptance Scenarios**:

1. **Given** una iniciativa con proyecto vinculado, **When** se abre su detalle, **Then** no se muestra ninguna acción ni selector para cambiar su estado y se informa que el bloqueo se debe al proyecto relacionado.
2. **Given** una iniciativa con proyecto vinculado, **When** se intenta invocar directamente cualquier operación de cambio de estado, **Then** la operación es rechazada sin modificar la iniciativa ni crear un evento funcional de transición exitosa.
3. **Given** que la creación del proyecto y un cambio de estado de la iniciativa se solicitan de forma concurrente, **When** ambas operaciones compiten por la misma iniciativa, **Then** solo puede consolidarse un resultado que conserve simultáneamente la iniciativa en `Iniciativa aprobada` y el proyecto vinculado, o el cambio de iniciativa sin proyecto creado; nunca un proyecto vinculado a una iniciativa con otro estado.

---

### User Story 3 - Cambiar el estado del proyecto desde su detalle (Priority: P1)

Un Administrador PIIP autorizado abre el detalle de un proyecto y selecciona una transición propia del contexto de proyecto, dejando una observación y recibiendo una confirmación clara.

**Why this priority**: El seguimiento del proyecto es el valor principal de la ampliación y debe separar con claridad sus decisiones de las decisiones de iniciativa.

**Independent Test**: Desde un proyecto en un estado de origen permitido se selecciona un destino permitido, se confirma con observación y se comprueba el nuevo estado, la versión incrementada y la auditoría.

**Acceptance Scenarios**:

1. **Given** un proyecto visible y administrable, **When** se abre su detalle, **Then** el selector solo contiene los destinos de la matriz controlada para su estado actual y nunca contiene estados de iniciativa ni `No Aplicable`.
2. **Given** un destino permitido y la versión vigente, **When** el administrador confirma el cambio con su observación, **Then** el proyecto adopta el nuevo estado, conserva la observación en auditoría y devuelve la nueva versión del mismo registro.
3. **Given** un proyecto que pasa a `Finalizado`, **When** la transición se confirma, **Then** `closingDate` se establece automáticamente con la fecha local de `America/Lima` de esa operación.
4. **Given** un proyecto que pasa a cualquier estado distinto de `Finalizado`, **When** la transición se confirma, **Then** `closingDate` no es creado, borrado ni reemplazado por esa transición.

---

### User Story 4 - Gestionar decisiones de iniciativa desde el detalle (Priority: P2)

Un Administrador PIIP autorizado abre una iniciativa sin proyecto vinculado y ve únicamente las acciones de iniciativa válidas para su estado actual.

**Why this priority**: La experiencia debe mantener la aprobación vigente y permitir ampliar decisiones sin exponer un catálogo indiscriminado.

**Independent Test**: Se consultan iniciativas con distintos estados y se comprueba que cada detalle presenta solo acciones de iniciativa aplicables, sin estados de proyecto ni ejecución desde el listado.

**Acceptance Scenarios**:

1. **Given** una iniciativa `Presentado` sin proyecto vinculado, **When** se abre el detalle, **Then** conserva el botón actual de aprobación.
2. **Given** una iniciativa `Presentado` sin proyecto vinculado, **When** un Administrador PIIP abre su detalle, **Then** puede aprobarla, declararla `No Admisible` o pasarla a `Iniciativa archivada`.
3. **Given** una iniciativa `Iniciativa aprobada` sin proyecto vinculado, **When** un Administrador PIIP abre su detalle, **Then** puede crear el proyecto derivado o pasar la iniciativa a `Iniciativa archivada`.
4. **Given** una iniciativa `No Admisible` o `Iniciativa archivada`, **When** se abre su detalle, **Then** no se muestran acciones de cambio de estado porque ambos estados son terminales.
5. **Given** cualquier iniciativa, **When** se muestran sus acciones, **Then** nunca aparecen estados propios de proyecto ni `No Aplicable`.
6. **Given** un listado de iniciativas o proyectos, **When** el usuario consulta o filtra, **Then** puede seguir usando los filtros existentes, pero no puede ejecutar allí una transición; cualquier acceso contextual navega primero al detalle.

---

### User Story 5 - Resolver concurrencia y auditar atómicamente (Priority: P1)

Un Administrador PIIP realiza un cambio sobre la versión que abrió y recibe un resultado íntegro: estado y auditoría confirmados juntos, o ningún cambio cuando la información quedó obsoleta.

**Why this priority**: El ciclo de vida pierde confiabilidad si una actualización concurrente sobrescribe otra o si estado y evidencia quedan desalineados.

**Independent Test**: Dos sesiones abren la misma versión; una confirma una transición y la segunda intenta confirmar otra. Solo la primera persiste y la segunda recibe conflicto sin evento funcional exitoso.

**Acceptance Scenarios**:

1. **Given** dos usuarios que abrieron la misma versión, **When** el primero confirma una transición válida, **Then** cambia el estado, incrementa la versión y registra la auditoría en una sola transacción.
2. **Given** que la versión ya cambió, **When** el segundo usuario envía la versión obsoleta, **Then** recibe HTTP `409`, se le solicita recargar y no se modifica el registro ni se registra una transición funcional exitosa.
3. **Given** una falla al guardar la auditoría funcional, **When** la transición intentaba confirmarse, **Then** también se revierte el cambio de estado y `closingDate`.
4. **Given** una transición exitosa, **When** un Administrador PIIP consulta su auditoría, **Then** encuentra el registro afectado, estado anterior, estado nuevo, actor, rol, Unidad Ejecutora, fecha, observación y resultado.

### Edge Cases

- El código corresponde a un registro de tipo distinto al de la ruta solicitada.
- El destino pertenece al catálogo general, pero no al contexto ni a la matriz aprobada para el tipo de registro.
- El destino coincide con el estado actual y no constituye una transición.
- El usuario tiene `Administrador PIIP` en otra Unidad Ejecutora, pero no en la Unidad Ejecutora real del registro.
- El usuario pierde su asignación o alcance después de abrir el detalle y antes de confirmar.
- El proyecto derivado se crea mientras otra sesión intenta cambiar el estado de la iniciativa.
- Dos solicitudes de transición usan la misma versión del registro.
- El registro fue eliminado o dejó de estar visible dentro del ámbito autorizado antes de confirmar.
- La observación está vacía, contiene solo espacios o supera la longitud admitida.
- Hay documentos pendientes al confirmar: se muestran como advertencia, pero no bloquean esta versión.
- Se intenta enviar `No Aplicable`, un estado de iniciativa a una ruta de proyecto o un estado de proyecto a una ruta de iniciativa.
- Se intenta cambiar un proyecto `Cancelado` o `Finalizado`; ambos estados son terminales y la operación se rechaza.
- Se intenta cambiar una iniciativa `No Admisible` o `Iniciativa archivada`; ambos estados son terminales y la operación se rechaza.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE conservar el flujo `Registrar iniciativa → Presentado → aprobar mediante la operación existente → Iniciativa aprobada → crear proyecto explícitamente → Proyecto en ejecución`.
- **FR-002**: “Proyecto derivado” DEBE tratarse exclusivamente como la relación única entre una iniciativa aprobada y el proyecto creado desde ella, nunca como un estado.
- **FR-003**: La iniciativa origen DEBE conservar `Iniciativa aprobada` después de crear el proyecto derivado.
- **FR-004**: El sistema DEBE bloquear toda acción de cambio de estado de una iniciativa desde que exista un proyecto vinculado, tanto en la interfaz como en la validación autoritativa del servidor.
- **FR-005**: La aprobación vigente DEBE conservar su ruta, semántica, autorización, request, response, tareas, notificaciones y auditoría actuales.
- **FR-006**: El detalle de iniciativa DEBE aplicar esta matriz únicamente mientras no exista proyecto vinculado: `Presentado` → `Iniciativa aprobada`, `No Admisible` o `Iniciativa archivada`; `Iniciativa aprobada` → `Iniciativa archivada`; `No Admisible` e `Iniciativa archivada` no admiten salidas.
- **FR-007**: El detalle de proyecto DEBE aplicar esta matriz: `Proyecto en ejecución` → `Producto aprobado`, `Producto no aprobado`, `Suspendido` o `Cancelado`; `Suspendido` → `Proyecto en ejecución` o `Cancelado`; `Producto no aprobado` → `Proyecto en ejecución` o `Cancelado`; `Producto aprobado` → `Finalizado`; `Cancelado` y `Finalizado` no admiten salidas.
- **FR-008**: Las rutas y controles de iniciativa DEBEN rechazar estados propios de proyecto, y las rutas y controles de proyecto DEBEN rechazar `Presentado`, `Iniciativa aprobada`, `Iniciativa archivada` y `No Admisible`.
- **FR-009**: `No Aplicable` NO DEBE aparecer ni ser aceptado como destino de transición en esta primera versión.
- **FR-010**: Los listados DEBEN conservar la consulta y los filtros por estado; no DEBEN contener un selector ni ejecutar directamente una transición.
- **FR-011**: Todo cambio de estado DEBE iniciarse y confirmarse desde el detalle contextual del registro.
- **FR-012**: Las acciones de escritura DEBEN estar disponibles únicamente para `Administrador PIIP` cuando una asignación vigente de ese mismo rol cubra la Unidad Ejecutora real del registro.
- **FR-013**: El servidor DEBE volver a validar rol, ámbito, tipo de registro, estado actual, destino, vínculo de derivación y versión al confirmar, sin confiar en las opciones mostradas por el frontend.
- **FR-014**: La solicitud DEBE conservar la observación introducida por el usuario como parte de la evidencia de auditoría y NO DEBE copiarla al campo `Nota`.
- **FR-015**: La observación DEBE admitir hasta 1000 caracteres, siguiendo el contrato de aprobación vigente; una observación ausente se registra como texto vacío en la auditoría.
- **FR-016**: Cada mutación DEBE enviar la versión del registro que el usuario abrió y DEBE reutilizar el control optimista existente de `REGISTRO_PORTAFOLIO`.
- **FR-017**: Una versión obsoleta DEBE producir HTTP `409`, no debe sobrescribir cambios y debe indicar al usuario que recargue el detalle.
- **FR-018**: El estado, `closingDate`, el incremento de versión y el evento funcional de auditoría DEBEN confirmarse o revertirse dentro de una única transacción.
- **FR-019**: La auditoría funcional de una transición exitosa DEBE identificar el registro afectado, estado anterior, estado nuevo, actor, rol efectivo, Unidad Ejecutora, fecha, observación y resultado `EXITOSO`.
- **FR-020**: Los intentos rechazados NO DEBEN generar un evento que aparente una transición exitosa; su código de resultado DEBE permanecer disponible en la auditoría de acceso existente.
- **FR-021**: Al pasar un proyecto a `Finalizado`, el sistema DEBE establecer `closingDate` automáticamente con la fecha local de `America/Lima` correspondiente a la confirmación exitosa, dentro de la misma transacción y sin aceptar una fecha enviada por el usuario.
- **FR-022**: Una transición a cualquier estado distinto de `Finalizado` NO DEBE crear, borrar ni modificar `closingDate`.
- **FR-023**: Los documentos pendientes NO DEBEN impedir ninguna transición de esta primera versión; la interfaz puede mostrarlos exclusivamente como advertencia informativa.
- **FR-024**: El sistema NO DEBE crear una segunda tabla, columna o mecanismo de versionado para estas transiciones.
- **FR-025**: El sistema NO DEBE persistir una tabla de matriz de transiciones en esta versión; la matriz funcional que se apruebe se expresa como reglas explícitas del dominio.
- **FR-026**: La validación del vínculo y la creación de un proyecto derivado DEBEN coordinarse sobre la misma iniciativa de modo que no pueda quedar un proyecto vinculado a una iniciativa cuyo estado haya cambiado fuera de `Iniciativa aprobada`.
- **FR-027**: La respuesta exitosa DEBE devolver el registro actualizado con su estado, `closingDate`, `updatedAt` y nueva `version` mediante el contrato de respuesta existente.
- **FR-028**: Los errores DEBEN usar las convenciones vigentes: `400` para request inválido, `403` para falta de autorización o ámbito, `404` para registro inexistente, `409` para conflicto de versión y `422` para regla de negocio o transición no permitida.
- **FR-029**: Los filtros de estado de cada listado DEBEN conservarse, pero solo DEBEN mostrar estados pertenecientes a su tipo de registro; siguen siendo filtros de consulta y no controles de transición.

### Matriz contextual de estados

| Contexto | Estado confirmado de origen | Destinos de esta versión | Tratamiento |
|---|---|---|---|
| Iniciativa | `Presentado` | `Iniciativa aprobada` | Transición existente que se conserva mediante `/approval`. |
| Iniciativa sin proyecto | `Presentado` | `Iniciativa aprobada`, `No Admisible`, `Iniciativa archivada` | Aprobación existente, inadmisibilidad o archivado. |
| Iniciativa sin proyecto | `Iniciativa aprobada` | `Iniciativa archivada` | Archivado previo a la creación del proyecto. |
| Iniciativa sin proyecto | `No Admisible`, `Iniciativa archivada` | Ninguno | Estados terminales sin reapertura en esta versión. |
| Iniciativa con proyecto | `Iniciativa aprobada` | Ninguno | Bloqueo total de acciones de estado. |
| Proyecto | `Proyecto en ejecución` | `Producto aprobado`, `Producto no aprobado`, `Suspendido`, `Cancelado` | Decisión inicial, evaluación, suspensión o cancelación. |
| Proyecto | `Suspendido` | `Proyecto en ejecución`, `Cancelado` | Reanudación o cancelación. |
| Proyecto | `Producto no aprobado` | `Proyecto en ejecución`, `Cancelado` | Retorno a ejecución para retrabajo o cancelación. |
| Proyecto | `Producto aprobado` | `Finalizado` | Cierre posterior a la aprobación del producto. |
| Proyecto | `Cancelado`, `Finalizado` | Ninguno | Estados terminales sin reapertura en esta versión. |
| Cualquier contexto | Cualquiera | `No Aplicable` | Excluido de la primera versión. |

### Contrato HTTP y responsabilidades técnicas solicitadas

Todas las rutas de esta sección son relativas a la base API confirmada `/api/v1`; por ejemplo, la aprobación completa es `POST /api/v1/initiatives/{code}/approval`.

#### Contratos confirmados que se conservan

| Método y ruta | Request | Response | Uso |
|---|---|---|---|
| `GET /initiatives/{code}` | Sin body | `PortfolioRecordResponse` | Cargar el detalle y la versión vigente de la iniciativa. |
| `POST /initiatives/{code}/approval` | `ApprovalRequest { version, observation }` | `PortfolioRecordResponse` | Conservar la aprobación `Presentado → Iniciativa aprobada`. |
| `GET /projects/{code}` | Sin body | `PortfolioRecordResponse` | Cargar el registro y la versión vigente del proyecto. |
| `POST /projects/derived` | `DerivedProjectRequest` | `PortfolioRecordResponse` con `201` | Conservar la creación explícita del proyecto derivado. |
| `GET /initiatives` y `GET /projects` | Filtros existentes, incluido `status` | Página de `PortfolioRecordResponse` | Mantener listados de consulta y filtros. |

#### Contratos propuestos

| Método y ruta | Request propuesto | Response | Restricción contextual |
|---|---|---|---|
| `POST /initiatives/{code}/status-transitions` | `InitiativeStatusTransitionRequest { version: long, targetStatus: PortfolioStatus, observation: string }` | `PortfolioRecordResponse` | Solo `INITIATIVE_ARCHIVED` y `NOT_ADMISSIBLE`, conforme a la matriz de iniciativa y sin proyecto vinculado. No reemplaza `/approval`. |
| `POST /projects/{code}/status-transitions` | `ProjectStatusTransitionRequest { version: long, targetStatus: PortfolioStatus, observation: string }` | `PortfolioRecordResponse` | Solo estados propios de proyecto aprobados por la matriz. |

Las rutas separadas son una propuesta derivada de las rutas existentes `/initiatives/**` y `/projects/**`. Los requests se mantienen separados aunque compartan campos para que el contrato y la documentación no sugieran que ambos contextos aceptan el mismo catálogo. El servidor valida el tipo real del registro y no acepta un código del contexto contrario.

`targetStatus` usa los códigos técnicos del enum que ya consume el backend. La ruta de iniciativa admite `INITIATIVE_ARCHIVED` desde `PRESENTED` o `INITIATIVE_APPROVED`, y `NOT_ADMISSIBLE` únicamente desde `PRESENTED`; la aprobación continúa usando `/approval`. La ruta de proyecto se limita a `PRODUCT_APPROVED`, `PRODUCT_NOT_APPROVED`, `SUSPENDED`, `CANCELLED`, `PROJECT_IN_PROGRESS` y `FINISHED` según la matriz controlada; `PROJECT_IN_PROGRESS` solo se acepta al reanudar un proyecto suspendido o devolver a ejecución un producto no aprobado. `NOT_APPLICABLE` se rechaza en ambas rutas. La respuesta continúa presentando las etiquetas funcionales de `PortfolioRecordResponse`.

#### Backend

- `PortfolioController` conserva `approve(...)` y agrega operaciones separadas para las dos rutas propuestas.
- `PortfolioDtos` conserva `ApprovalRequest` y `PortfolioRecordResponse`; agrega `InitiativeStatusTransitionRequest` y `ProjectStatusTransitionRequest`, ambos con `version` obligatorio, `targetStatus` obligatorio y `observation` de hasta 1000 caracteres.
- `PortfolioService` continúa como límite transaccional y agrega responsabilidades separadas para transición de iniciativa y transición de proyecto. En cada caso carga el registro, autoriza la Unidad Ejecutora real, verifica versión, tipo, estado, destino y vínculo, aplica el cambio y registra auditoría antes de responder.
- La transición a `Finalizado` obtiene la fecha mediante una fuente temporal inyectable configurada para `America/Lima`, de modo que la regla pueda verificarse de forma determinista sin depender de la zona horaria del servidor.
- `PortfolioRecordEntity` continúa como autoridad del estado y expone comportamientos de dominio separados por contexto; no se permite un setter genérico público de estado.
- `PortfolioRecordRepository` reutiliza `existsByOriginRecordId(...)` y agrega una lectura bloqueante de la iniciativa para coordinar el cambio de estado con `createDerived(...)`, sin SQL nativo.
- `AuditService` y `AuditEventEntity` se reutilizan. Los eventos propuestos son `ESTADO_INICIATIVA_CAMBIADO` y `ESTADO_PROYECTO_CAMBIADO`; la aprobación vigente conserva `INICIATIVA_APROBADA`. El detalle JSON contiene `estadoAnterior`, `estadoNuevo`, `rol`, `unidadEjecutoraId`, `unidadEjecutora`, `observacion` y `resultado`; actor y fecha permanecen en las columnas existentes.
- `ApiExceptionHandler` conserva `ProblemDetail` y los códigos existentes. No se propone una nueva familia de errores.

#### Frontend

- `app.routes.ts` agrega `proyectos/:code` como detalle general de proyecto y conserva `proyectos/:code/documentos` como expediente documental. La ruta documental debe permanecer antes o diferenciarse de la ruta dinámica para no perder su resolución específica.
- `ProjectDetailComponent` es el nuevo contexto de consulta y transición del proyecto. Muestra datos generales, vínculo de origen, `closingDate`, auditoría, acceso al expediente documental y el selector filtrado por la matriz aprobada.
- `InitiativeDetailComponent` conserva la aprobación y agrega las acciones contextuales `Iniciativa archivada` y `No Admisible` conforme a la matriz controlada. Cuando existe `derivedProject`, oculta todas las acciones de estado y explica el bloqueo.
- `InitiativesComponent` y `ProjectsComponent` conservan filtros y consulta, pero filtran sus opciones de estado por tipo de registro en lugar de reutilizar indiscriminadamente `PIIP_CATALOGS.statuses`. La acción principal de un proyecto navega al nuevo detalle; los accesos a documentos y auditoría continúan disponibles, pero el listado no alberga ni confirma transiciones.
- `PiipRepository` incorpora operaciones distintas para cambiar el estado de iniciativa y proyecto, además de una lectura de detalle de proyecto.
- `PiipHttpRepository` implementa ambas rutas propuestas, toma la versión del mismo `recordVersions` existente, conserva el mensaje actual de conflicto `409`, actualiza el registro devuelto y refresca el contexto visible.
- `piip.models.ts` agrega inputs separados de transición y un modelo de detalle de proyecto. `PiipStatus` se conserva como catálogo de visualización, pero las opciones de cada selector proceden de listas contextuales explícitas y no de `PIIP_CATALOGS.statuses` completo.
- Los clientes generados desde OpenAPI son consumidores del contrato resultante; su regeneración forma parte de la implementación posterior de esta feature y solo se ejecuta con autorización explícita. No se realiza durante `specify`, `clarify`, `plan`, `tasks` ni `analyze`.

#### Persistencia y atomicidad

- `REGISTRO_PORTAFOLIO.ESTADO` almacena el nuevo estado, `FECHA_CIERRE` almacena la fecha efectiva de finalización y `VERSION` mantiene el único control de concurrencia.
- `EVENTO_AUDITORIA` conserva la evidencia append-only mediante sus columnas actuales y `DETALLE_JSON`; no se agrega una tabla de historial de estados.
- La transición y `AuditService.event(...)` participan en la misma transacción de `PortfolioService`. Una falla de auditoría revierte estado, fecha de cierre y versión.
- La auditoría de acceso mantiene su transacción independiente y registra el resultado HTTP incluso cuando la transición funcional se rechaza.
- La iniciativa origen se obtiene con bloqueo de escritura tanto para validar una transición de iniciativa como para crear el proyecto derivado. Así, la verificación de estado y de relación no depende solo de una lectura susceptible a carrera.

### Key Entities *(include if feature involves data)*

- **Registro de portafolio**: Iniciativa o proyecto identificado por código; conserva tipo, estado, Unidad Ejecutora, fecha de cierre, relación de origen, actualización y versión optimista.
- **Iniciativa**: Registro que nace en `Presentado`, puede aprobarse y puede originar como máximo un proyecto. Una vez vinculada, permanece aprobada y bloqueada para cambios de estado.
- **Proyecto**: Registro independiente que nace en `Proyecto en ejecución`, puede recorrer únicamente estados propios del proyecto y conserva una relación opcional con su iniciativa origen.
- **Transición de estado**: Decisión contextual compuesta por registro, tipo, estado anterior, estado destino, versión esperada, observación, actor, rol y Unidad Ejecutora.
- **Evento de auditoría**: Evidencia append-only de una transición exitosa, registrada atómicamente con el cambio del portafolio.
- **Auditoría de acceso**: Evidencia técnica del intento HTTP y su resultado, incluso cuando la operación funcional es rechazada.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las pruebas del flujo vigente conserva `Presentado`, `Iniciativa aprobada` y `Proyecto en ejecución` en los puntos definidos, sin creación automática de proyectos.
- **SC-002**: El 100 % de las iniciativas con proyecto vinculado permanece en `Iniciativa aprobada` y rechaza cualquier intento de cambio de estado.
- **SC-003**: El 100 % de los selectores de detalle y filtros de listado contiene únicamente estados del tipo de registro correspondiente; ningún selector de transición incluye `No Aplicable`.
- **SC-004**: El 100 % de las transiciones exitosas incrementa la versión existente y genera exactamente un evento funcional con registro, estados, actor, rol, Unidad Ejecutora, fecha, observación y resultado.
- **SC-005**: El 100 % de los intentos con versión obsoleta recibe conflicto y conserva sin cambios el estado, `closingDate` y el historial de transiciones exitosas.
- **SC-006**: El 100 % de las transiciones exitosas a `Finalizado` establece `closingDate` con la fecha efectiva de la transición; ninguna transición a otro estado modifica ese campo.
- **SC-007**: El 100 % de los intentos fuera del ámbito de `Administrador PIIP` de la Unidad Ejecutora real es rechazado sin cambio funcional.
- **SC-008**: En el 100 % de los casos donde falla la escritura de auditoría, no persiste el cambio de estado ni la fecha de cierre.
- **SC-009**: Los listados conservan el 100 % de sus capacidades actuales de consulta y filtros, y el 100 % de las transiciones se confirma desde un detalle contextual.

## Assumptions

- La fecha efectiva de finalización es la fecha local de `America/Lima` al confirmar exitosamente la transición; el usuario no introduce ni edita `closingDate` en esta versión.
- La observación es opcional, mantiene el máximo de 1000 caracteres del contrato de aprobación y se conserva en auditoría como texto vacío cuando no se proporciona.
- Los documentos pendientes se consultan con los mecanismos actuales y solo producen una advertencia; no se introduce una dependencia transaccional con el módulo documental.
- La relación única de origen existente sigue siendo la autoridad para determinar si la iniciativa ya tiene proyecto derivado.
- Los estados adicionales de iniciativa y proyecto se rigen exclusivamente por las matrices controladas registradas en la sesión de aclaración del 2026-08-18; su presencia previa en el catálogo no autorizaba estas transiciones.
- La guía funcional `docs/funcional/guia-funcional-piip.md` tendrá impacto cuando se implemente esta feature, porque cambiarán acciones, estados operativos y recorridos visibles. Su actualización deberá incluirse en el futuro `tasks.md`; no se modifica durante `specify`.

## Out of Scope

- Ejecutar la implementación de frontend, backend, OpenAPI, esquema generado o cliente API durante la elaboración y el refinamiento de los artefactos Spec Kit; la implementación posterior de esta feature permanece sujeta al gate de `/speckit-implement`.
- Ejecutar pruebas, builds, servidores, integración Oracle o generación de contratos.
- Crear un segundo sistema de versiones, una tabla de historial de estados o una tabla de transiciones permitidas.
- Habilitar `No Aplicable` como transición.
- Bloquear transiciones por documentos pendientes.
- Cambiar estados desde listados, dashboard, tareas o notificaciones.
- Modificar estados automáticamente por carga, publicación o ausencia de documentos.
- Reabrir proyectos `Cancelado` o `Finalizado`, o admitir transiciones fuera de la matriz controlada.
- Reabrir iniciativas `No Admisible` o `Iniciativa archivada`, o admitir transiciones fuera de la matriz controlada.
