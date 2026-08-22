# Feature Specification: Actualización controlada de registros de portafolio

**Feature Branch**: `refactor/backend` (el hook de creación de rama está deshabilitado)

**Created**: 2026-08-22

**Status**: Draft — lista para planificación

**Input**: User description: "Definir y especificar la actualización controlada de una Iniciativa o Proyecto existente, abarcando backend y frontend, con autorización por rol y Unidad Ejecutora, validación de estado y referencias, concurrencia optimista, auditoría append-only y conservación de las capacidades vigentes. Generar únicamente la especificación y los artefactos Spec Kit asociados."

## Clasificación del grounding

### Hechos confirmados por el repositorio

- PIIP expone alta y consulta para iniciativas, proyectos derivados y proyectos preexistentes; también expone aprobación de iniciativas y transiciones separadas para iniciativas y proyectos.
- No existe un endpoint general `PUT`, `PATCH` o `DELETE` para iniciativas o proyectos, ni una operación de actualización en `PiipRepository` o en el cliente Angular generado.
- `REGISTRO_PORTAFOLIO` representa iniciativas y proyectos, conserva el código único, la relación opcional y única con una iniciativa origen, la Unidad Ejecutora, los datos operativos, el estado, las fechas de creación/modificación y una columna `VERSION` gestionada como versión optimista.
- `REGISTRO_UNIDAD_RESPONSABLE` normaliza las Unidades Orgánicas responsables y conserva identidad, denominación original y orden de presentación.
- Los contratos de alta usan identidades persistentes para Tipo de solución, Fuente u origen, Objetivo PEI, Actividad POI y Unidad Orgánica responsable. PEI y POI son opcionales e independientes.
- La validación vigente de una Unidad Orgánica responsable comprueba existencia, estado activo y pertenencia a la misma Unidad Ejecutora del registro.
- Las mutaciones vigentes de portafolio exigen una asignación activa de `ADMINISTRADOR_PIIP` que cubra la Unidad Ejecutora real del registro. La comprobación no depende de `CREADO_POR`.
- La aprobación y las transiciones comparan la versión enviada con la versión vigente, responden con conflicto cuando no coincide y dejan que la versión del registro avance al confirmar.
- La auditoría funcional vigente es append-only: `EVENTO_AUDITORIA` conserva tipo de evento, tipo y código de entidad, detalle, actor y fecha. La auditoría de acceso conserva por separado método, ruta, estado HTTP y correlación, sin cuerpos HTTP, tokens ni contenido documental.
- El frontend conserva la versión de cada registro consultado, ya presenta un mensaje específico ante conflicto `409`, filtra las acciones por autorización de la Unidad Ejecutora y refresca sus colecciones después de mutaciones exitosas.
- Los formularios de alta de iniciativa, proyecto derivado y proyecto preexistente ya contienen controles y validaciones reutilizables, pero no existen rutas ni formularios de edición.
- Los listados permiten abrir el detalle y otras acciones contextuales; el detalle concentra las decisiones de ciclo de vida. La solicitud admite edición desde detalle y/o listado, sin obligar a ejecutar la mutación directamente desde el listado.

### Reglas vigentes que esta feature debe conservar

- Los 23 campos canónicos y la diferencia entre `NA` y `No aplica` mantienen su significado actual.
- Una iniciativa se registra en `Presentado`; un proyecto derivado o preexistente se registra en `Proyecto en ejecución`.
- Un proyecto derivado conserva vínculo único, código de origen y Unidad Ejecutora heredada de su iniciativa. Crear el proyecto no cambia el estado `Iniciativa aprobada` de la iniciativa origen.
- Las matrices de transición ratificadas por la feature 009 permanecen separadas de la edición de datos. Editar no crea una transición ni habilita un destino adicional.
- La fecha de cierre se establece únicamente por la transición vigente a `Finalizado`; no es un dato de edición general.
- Los catálogos de escritura ofrecen referencias activas. Una referencia histórica inactiva puede mostrarse, pero debe reemplazarse por una opción activa si el campo se incluye en una actualización.
- Los Objetivos PEI y las Actividades POI siguen siendo opcionales e independientes; ninguno habilita, obliga ni filtra al otro.
- La Unidad Ejecutora activa de la interfaz no sustituye la Unidad Ejecutora real del registro para autorizar la operación.

### Decisiones de contrato propuestas

- La actualización se modela mediante `PATCH /initiatives/{code}` y `PATCH /projects/{code}` porque solo puede modificar un subconjunto autorizado de campos y no reemplaza el recurso completo. Los contratos permanecen separados para impedir mezclar reglas de iniciativa, proyecto derivado y proyecto preexistente.
- Cada request incluye obligatoriamente la `version` que el usuario abrió y únicamente los campos de negocio autorizados para ese tipo/origen. Un campo ausente significa "conservar el valor vigente"; no significa vaciarlo.
- El response exitoso reutiliza la representación completa del registro, incluida la versión nueva, para que detalle y listado puedan reconciliarse sin una interpretación paralela.
- Los errores observables son: `400` para request mal formado; `403` para rol o ámbito insuficiente; `404` para código inexistente en la ruta correspondiente; `409` para versión desactualizada; y `422` para estado no editable, referencia inválida o regla funcional incumplida.
- La acción puede ofrecerse en el menú del listado como navegación al formulario de edición, pero la edición se confirma en una pantalla contextual cargada con el detalle y la versión vigentes. El listado no contiene edición inline.
- El frontend oculta la acción si su contexto local no acredita `ADMINISTRADOR_PIIP` sobre la UE del registro; el servidor vuelve a validar rol, ámbito, tipo, origen, estado, referencias y versión al confirmar.

### Fuentes canónicas consultadas

- `docs/architecture/piip-fields.md` y `docs/funcional/guia-funcional-piip.md` para campos, flujo vigente, catálogos, alcance organizacional, auditoría y concurrencia.
- `specs/009-ciclo-vida-portafolio/**` para matrices de estado, terminalidad, relación de derivación y control de versión ratificados.
- `specs/011-centralizar-catalogos-piip/**` para identidades persistentes, PEI/POI opcionales e independientes y validación de Unidad Orgánica por UE.
- `specs/012-refactorizar-arquitectura-backend/plan.md` para las responsabilidades vigentes de los servicios de aplicación del portafolio después de la refactorización.
- `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/**`, `identity/application/LocalAuthorizationService.java`, `audit/**` y `shared/api/ApiExceptionHandler.java` para contratos, autorización, persistencia, auditoría y errores actuales.
- `database/generated/piip-oracle.sql` como esquema Oracle derivado vigente, contrastado con las entidades JPA canónicas.
- `apps/frontend/src/app/app.routes.ts`, `core/piip.repository.ts`, `core/piip-http.repository.ts`, `core/piip.models.ts`, `pages/**` y `api/generated/**` para navegación, formularios, acciones, cliente y manejo de versión/error.
- `graphify-out/graph.json` se usó solo como índice acotado; todos los hechos anteriores se contrastaron con archivos canónicos.
- La revisión fue estática. No se ejecutaron pruebas, builds, generación OpenAPI, servidores, contenedores, migraciones ni operaciones de base de datos.

## Clarifications

### Session 2026-08-22

- Q: ¿Qué campos pueden editarse en cada tipo de registro? → A: Se permiten los campos de negocio capturados en el alta aplicable a cada tipo; código, tipo, origen, Unidad Ejecutora, estado y datos técnicos permanecen inmutables. PEI y POI pueden asignarse, cambiarse o retirarse independientemente. Las Unidades Orgánicas responsables pueden sustituirse de forma atómica por un conjunto no vacío, ordenado, activo y perteneciente a la misma UE.
- Q: ¿En qué estados se permite editar y qué ocurre después de la derivación? → A: Una iniciativa solo puede editarse en `Presentado` y sin proyecto derivado; un proyecto derivado o preexistente solo puede editarse en `Proyecto en ejecución`. Una iniciativa deja de ser editable al aprobarse y, por consecuencia, tampoco admite edición después de tener un proyecto derivado.
- Q: ¿Qué debe conservar la auditoría funcional? → A: Cada actualización exitosa registra los campos efectivamente modificados con sus valores anterior y nuevo, además del actor, UE, versiones anterior y nueva y resultado; no guarda el request completo, tokens, archivos ni contenido documental.
- Q: ¿Cuántas Unidades Orgánicas responsables puede seleccionar el usuario y cómo se determina su orden? → A: Puede seleccionar varias Unidades Orgánicas responsables y definir explícitamente su orden de presentación.
- Q: ¿La edición debe solicitar un motivo u observación adicional? → A: No. El contrato no solicita motivo; la auditoría conserva únicamente los valores efectivamente modificados y el contexto aprobado.
- Q: ¿A qué superficie navega el usuario después de guardar una edición? → A: Vuelve al detalle actualizado del registro y recibe una confirmación visible de éxito.
- Q: ¿Qué ocurre si el usuario intenta salir con cambios sin guardar? → A: La interfaz solicita confirmación antes de descartarlos y no crea un borrador local de edición.

### Matriz de campos editables aprobada

| Registro | Campos editables | Campos inmutables relevantes |
|----------|------------------|------------------------------|
| Iniciativa | Nombre, Tipo de solución, Fuente u origen, Fecha de inicio, Responsable, Objetivo PEI, Actividad POI, Unidades Orgánicas responsables, Descripción, Nota y Componente Digital. | Código, tipo de registro, código/modo/relación de origen, Unidad Ejecutora, Resultados clave, Estado, Tipo de producto final, Fecha de cierre, creador y fechas técnicas no son editables por el usuario; la fecha de modificación cambia automáticamente al confirmar. |
| Proyecto derivado | Nombre, Tipo de solución, Fuente u origen, Fecha de inicio, Responsable, Objetivo PEI, Actividad POI, Unidades Orgánicas responsables, Descripción, Resultados clave, Nota y Componente Digital. | Código, tipo de registro, código/modo/relación de origen, Unidad Ejecutora heredada, Estado, Tipo de producto final, Fecha de cierre, creador y fechas técnicas no son editables por el usuario; la fecha de modificación cambia automáticamente al confirmar. |
| Proyecto preexistente | Nombre, Fuente u origen, Fecha de inicio, Responsable, Objetivo PEI, Actividad POI, Unidades Orgánicas responsables, Descripción, Resultados clave, Nota y Componente Digital. | Código, tipo de registro, código/modo/relación de origen, Unidad Ejecutora, Tipo de solución fijo `No aplica`, Estado, Tipo de producto final, Fecha de cierre, creador y fechas técnicas no son editables por el usuario; la fecha de modificación cambia automáticamente al confirmar. |

### Formato funcional de auditoría aprobado

- Los eventos se distinguen como actualización de iniciativa o actualización de proyecto y se asocian al código del registro.
- El detalle conserva `tipoRegistro`, `unidadEjecutoraId`, `unidadEjecutora`, `versionAnterior`, `versionNueva`, `cambios` y `resultado`.
- `cambios` contiene únicamente los campos cuyo valor efectivo cambió. Cada entrada conserva `anterior` y `nuevo`; un retiro permitido de PEI o POI se representa explícitamente con valor nuevo nulo.
- Las referencias de catálogo se representan con identidad, código y nombre; las Unidades Orgánicas responsables se representan como listas ordenadas con identidad, código, nombre y posición.
- Actor y fecha permanecen en los atributos propios del evento vigente. No se duplica el request completo ni se registra información excluida por seguridad.
- La actualización no solicita ni registra un motivo u observación adicional; los valores anterior y nuevo constituyen la evidencia funcional del cambio.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Editar un registro autorizado (Priority: P1)

Un `Administrador PIIP` abre una iniciativa o proyecto de una Unidad Ejecutora cubierta por su asignación activa, modifica únicamente datos permitidos y confirma la actualización sin depender de quién creó el registro.

**Why this priority**: La capacidad principal es corregir o mantener información operativa existente sin romper la separación de ámbitos ni el ciclo de vida.

**Independent Test**: Con un registro en estado editable y un administrador distinto del creador pero autorizado para su UE, se abre el formulario, se cambia un campo permitido y se comprueba la representación actualizada y su nueva versión.

**Acceptance Scenarios**:

1. **Given** un registro visible, una versión vigente y un `Administrador PIIP` autorizado para su UE, **When** modifica campos permitidos y confirma, **Then** se actualiza el mismo registro, conserva su identidad y devuelve sus datos completos con una versión nueva.
2. **Given** que el administrador no es el creador original, **When** su asignación activa cubre la UE real del registro, **Then** puede editar en las mismas condiciones que cualquier otro administrador autorizado de ese ámbito.
3. **Given** un campo que no pertenece a la matriz editable del tipo de registro, **When** se intenta incluir en la actualización, **Then** la operación se rechaza sin cambios parciales.
4. **Given** un registro exitosamente actualizado, **When** finaliza la confirmación, **Then** el usuario vuelve al detalle actualizado, recibe una confirmación visible de éxito y una consulta posterior del listado muestra el valor y la versión confirmados.
5. **Given** un formulario con cambios sin guardar, **When** el usuario intenta cancelar, navegar o cerrar la edición, **Then** la interfaz solicita confirmación y le permite permanecer sin perder los cambios.
6. **Given** que el usuario confirma el descarte, **When** abandona la edición, **Then** los cambios locales se eliminan y no queda un borrador que pueda reutilizar una versión obsoleta.

---

### User Story 2 - Impedir ediciones fuera de rol, ámbito o estado (Priority: P1)

PIIP protege la edición tanto en la interfaz como en el servidor cuando el usuario no posee el rol y ámbito requeridos o cuando el estado del registro no admite edición.

**Why this priority**: Ocultar un control no constituye autorización; la protección autoritativa evita alteraciones fuera del ámbito institucional o del momento funcional permitido.

**Independent Test**: Se repite la misma solicitud con un usuario sin rol, con un administrador de otra UE y con un registro no editable; todos los intentos se rechazan y el registro permanece igual.

**Acceptance Scenarios**:

1. **Given** un usuario sin `ADMINISTRADOR_PIIP` sobre la UE real del registro, **When** consulta el listado o detalle, **Then** no ve la acción de edición.
2. **Given** ese mismo usuario, **When** invoca directamente el contrato de actualización, **Then** recibe `403` y no se modifica ni se registra una actualización funcional exitosa.
3. **Given** que el usuario pierde el rol o ámbito después de abrir el formulario, **When** intenta confirmar, **Then** el servidor revalida la asignación, responde `403` y no aplica cambios.
4. **Given** un registro cuyo estado no permite edición, **When** se intenta actualizar por interfaz o llamada directa, **Then** la interfaz no ofrece la acción y el servidor responde `422` sin modificar datos.

---

### User Story 3 - Resolver una edición concurrente (Priority: P1)

Dos administradores pueden abrir el mismo registro, pero una versión antigua nunca sobrescribe una actualización confirmada posteriormente.

**Why this priority**: Sin concurrencia optimista, una corrección válida puede perderse silenciosamente y la auditoría dejar de representar el orden real de cambios.

**Independent Test**: Dos sesiones cargan la misma versión; la primera confirma y la segunda intenta guardar después. Solo la primera cambia el registro y la segunda recibe conflicto.

**Acceptance Scenarios**:

1. **Given** dos formularios abiertos con la misma versión, **When** el primero confirma una actualización válida, **Then** el registro avanza una versión y conserva esos cambios.
2. **Given** que la versión ya avanzó, **When** el segundo formulario confirma su copia antigua, **Then** recibe `409`, no sobrescribe datos y se le ofrece recargar la versión vigente.
3. **Given** un conflicto `409`, **When** el usuario recarga, **Then** ve los valores vigentes y puede decidir una nueva edición sin reenvío automático de la copia obsoleta.

---

### User Story 4 - Validar referencias y pertenencia organizacional (Priority: P1)

Al editar referencias, PIIP acepta únicamente selecciones válidas para escritura y conserva la coherencia de la Unidad Orgánica responsable con la UE inmutable del registro.

**Why this priority**: Una referencia inexistente, inactiva o de otra UE degrada la integridad del portafolio y puede exponer datos organizacionales fuera de ámbito.

**Independent Test**: Se intenta cambiar catálogos y Unidades Orgánicas con referencias activas válidas, inactivas, inexistentes y de otra UE; solo el conjunto válido se confirma.

**Acceptance Scenarios**:

1. **Given** un campo de catálogo incluido en la actualización, **When** la referencia existe, pertenece al catálogo correcto y está activa, **Then** se acepta su identidad y se devuelve la referencia resuelta.
2. **Given** una referencia inexistente, inactiva o de catálogo incorrecto, **When** se confirma la edición, **Then** se responde `422` indicando el campo y la causa y no se aplican otros cambios.
3. **Given** varias Unidades Orgánicas responsables activas y pertenecientes a la UE del registro, **When** el usuario las selecciona y define su orden, **Then** el conjunto confirmado conserva todas sus identidades y el orden explícito de presentación.
4. **Given** una Unidad Orgánica inexistente, inactiva o perteneciente a otra UE, **When** se confirma, **Then** se responde `422` y se conserva completo el conjunto anterior.
5. **Given** PEI y POI dentro de la matriz editable, **When** se modifica uno sin el otro, **Then** la operación no exige ni deriva una relación entre ambos.
6. **Given** el mismo conjunto de Unidades Orgánicas en un orden diferente, **When** el usuario confirma el nuevo orden, **Then** se considera un cambio efectivo y la respuesta y auditoría conservan la secuencia confirmada.

---

### User Story 5 - Conservar trazabilidad de la actualización (Priority: P1)

Cada actualización confirmada produce evidencia funcional append-only que identifica el registro, el actor, el ámbito y los cambios aprobados, sin reescribir eventos anteriores.

**Why this priority**: La edición de datos existentes requiere explicar quién cambió qué y sobre qué versión sin perder el historial previo.

**Independent Test**: Se actualiza un registro y se consulta la auditoría antes y después; aparece un evento nuevo correlacionado con el registro y los eventos anteriores permanecen intactos.

**Acceptance Scenarios**:

1. **Given** una actualización válida, **When** se confirma, **Then** el cambio y su evento funcional se consolidan atómicamente o ambos se revierten.
2. **Given** eventos previos del registro, **When** se agrega la actualización, **Then** ninguno se modifica ni elimina.
3. **Given** una edición rechazada por autorización, inexistencia, versión o regla, **When** falla, **Then** no se crea un evento funcional que aparente una actualización exitosa; la auditoría de acceso conserva el resultado HTTP vigente.
4. **Given** un evento de actualización, **When** se consulta, **Then** no contiene tokens, cuerpo HTTP completo, archivos ni contenido documental.
5. **Given** una actualización válida, **When** el administrador la confirma, **Then** no se le exige motivo u observación y el evento conserva los cambios efectivos con su contexto de actor, UE y versiones.

---

### User Story 6 - Conservar altas, consultas y ciclo de vida (Priority: P2)

La nueva capacidad convive con las operaciones vigentes sin modificar creación, aprobación, derivación, consulta, documentos ni transiciones.

**Why this priority**: La edición agrega una mutación acotada; no redefine el comportamiento ya ratificado del portafolio.

**Independent Test**: Se ejecutan los recorridos existentes de alta, consulta, aprobación, creación derivada/preexistente y transiciones, y sus contratos y reglas se mantienen sin depender de la nueva edición.

**Acceptance Scenarios**:

1. **Given** una nueva iniciativa, **When** se registra, aprueba y deriva mediante las operaciones vigentes, **Then** conserva estados, tareas, notificaciones, documentos, relación y auditoría actuales.
2. **Given** un proyecto preexistente, **When** se registra mediante su operación vigente, **Then** conserva código de origen `NA`, modo preexistente y estado inicial.
3. **Given** una actualización de datos, **When** se confirma, **Then** no altera por sí misma el estado, la fecha de cierre, documentos, tareas, notificaciones ni relación de origen.
4. **Given** cualquier registro, **When** se consulta después de incorporar la feature, **Then** el contrato completo continúa representando los campos vigentes y la versión actual.

### Edge Cases

- El código existe, pero corresponde a un tipo distinto al de la ruta invocada.
- El registro deja de existir o de ser visible en el ámbito entre la carga y la confirmación.
- La solicitud no incluye `version`, incluye una versión negativa o incluye únicamente campos ausentes.
- Un campo se envía explícitamente vacío o nulo cuando su regla vigente no lo permite.
- El mismo valor ya vigente se envía como cambio y no produce una diferencia funcional.
- La referencia seleccionada estaba activa al abrir el formulario y queda inactiva antes de confirmar.
- La Unidad Orgánica responsable cambia de UE o queda inactiva entre carga y confirmación.
- La lista de Unidades Orgánicas contiene duplicados, está vacía o altera el orden.
- La iniciativa adquiere un proyecto derivado mientras otro usuario mantiene abierto su formulario de edición.
- Una transición de estado y una edición compiten sobre la misma versión.
- El frontend conserva datos locales de un registro después de recibir `403`, `404`, `409` o `422`.
- El usuario intenta abandonar un formulario modificado mediante cancelar, navegación interna o cierre de la vista.
- La persistencia del evento de auditoría falla después de validar la solicitud.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir actualizar una iniciativa, un proyecto derivado o un proyecto preexistente existente mediante contratos separados por tipo de registro.
- **FR-002**: La actualización DEBE modificar el registro existente y NO DEBE crear un registro, código, relación de origen, expediente documental o historial paralelo.
- **FR-003**: La autorización DEBE depender de una asignación activa de `ADMINISTRADOR_PIIP` que, en una misma asignación válida, cubra la Unidad Ejecutora real del registro; NO DEBE depender del creador original.
- **FR-004**: El servidor DEBE volver a validar rol y ámbito dentro del caso de uso al confirmar, aunque la interfaz haya mostrado previamente la acción.
- **FR-005**: La interfaz DEBE ocultar la acción de edición cuando el contexto actual no acredita el rol y ámbito requeridos o el estado no es editable.
- **FR-006**: El listado PUEDE ofrecer una acción contextual que navegue a edición; la mutación NO DEBE ejecutarse inline desde la fila.
- **FR-007**: El formulario DEBE cargar el detalle y la versión vigentes, diferenciar iniciativa, proyecto derivado y proyecto preexistente y mostrar como solo lectura los datos no editables.
- **FR-007A**: Si el formulario tiene cambios sin guardar, la interfaz DEBE solicitar confirmación antes de cancelar, navegar o cerrar la edición y DEBE permitir permanecer en ella. Si el usuario confirma el descarte, DEBE eliminar los cambios locales.
- **FR-007B**: La edición NO DEBE guardar ni restaurar borradores locales; cada apertura DEBE partir del detalle y la versión vigentes para evitar reutilizar datos obsoletos.
- **FR-008**: Una iniciativa DEBE permitir editar exclusivamente Nombre, Tipo de solución, Fuente u origen, Fecha de inicio, Responsable, Objetivo PEI, Actividad POI, Unidades Orgánicas responsables, Descripción, Nota y Componente Digital.
- **FR-009**: Un proyecto derivado DEBE permitir editar exclusivamente Nombre, Tipo de solución, Fuente u origen, Fecha de inicio, Responsable, Objetivo PEI, Actividad POI, Unidades Orgánicas responsables, Descripción, Resultados clave, Nota y Componente Digital.
- **FR-010**: Un proyecto preexistente DEBE permitir editar exclusivamente Nombre, Fuente u origen, Fecha de inicio, Responsable, Objetivo PEI, Actividad POI, Unidades Orgánicas responsables, Descripción, Resultados clave, Nota y Componente Digital; su Tipo de solución fijo `No aplica` NO DEBE ser editable.
- **FR-010A**: El código, tipo de registro, modo de origen, relación/código de origen, Unidad Ejecutora, estado, tipo de producto final, fecha de cierre, creador y fechas técnicas DEBEN permanecer inmutables. Resultados clave también DEBE permanecer inmutable para iniciativas.
- **FR-011**: La Unidad Ejecutora heredada de un proyecto derivado NO DEBE separarse de la iniciativa origen ni cambiar la relación vigente entre ambos registros.
- **FR-012**: La actualización DEBE validar cualquier referencia incluida mediante su identidad, catálogo correcto y disponibilidad activa para escritura.
- **FR-013**: PEI y POI DEBEN permanecer opcionales e independientes; modificar, asignar o retirar uno NO DEBE condicionar el otro.
- **FR-013A**: Un campo ausente DEBE conservar su valor; un valor nulo explícito DEBE retirar únicamente Objetivo PEI, Actividad POI, Nota o Resultados clave cuando ese campo opcional pertenezca al tipo de registro. Un campo obligatorio nulo DEBE rechazarse como request inválido.
- **FR-014**: La interfaz DEBE permitir seleccionar varias Unidades Orgánicas responsables y definir explícitamente su orden de presentación. La sustitución DEBE exigir un conjunto no vacío y validar que cada unidad exista, esté activa, pertenezca a la UE del registro y no esté duplicada; la persistencia, respuesta y auditoría DEBEN conservar el orden enviado.
- **FR-015**: La sustitución de Unidades Orgánicas responsables DEBE ser atómica: un elemento inválido conserva el conjunto anterior completo.
- **FR-015A**: Una iniciativa DEBE aceptar edición únicamente en `Presentado` y mientras no tenga proyecto derivado; cualquier otro estado o vínculo DEBE producir `422`.
- **FR-015B**: Un proyecto derivado o preexistente DEBE aceptar edición únicamente en `Proyecto en ejecución`; cualquier otro estado DEBE producir `422`.
- **FR-016**: Cada request de actualización DEBE incluir la versión que el usuario abrió; el servidor DEBE compararla con la versión vigente y mantener el control optimista persistente actual.
- **FR-017**: Una versión desactualizada DEBE producir `409`, no aplicar cambios ni crear un evento funcional exitoso y exigir una recarga explícita antes de reintentar.
- **FR-018**: Una actualización exitosa DEBE devolver la representación completa vigente con su nueva versión y permitir que el frontend refresque detalle y listado.
- **FR-018A**: Una actualización exitosa DEBE actualizar automáticamente la fecha de modificación; el usuario NO DEBE poder enviarla ni elegirla.
- **FR-018B**: Después de una actualización exitosa, la interfaz DEBE navegar al detalle del mismo registro, mostrar una confirmación visible y presentar los datos y la versión devueltos; el listado DEBE quedar reconciliado para su siguiente consulta.
- **FR-019**: El sistema DEBE responder `403` ante rol o ámbito insuficiente, `404` ante registro inexistente en la ruta correspondiente y `422` ante estado no editable, referencia inválida o regla funcional incumplida.
- **FR-020**: Los errores DEBEN ser comprensibles y preservar las propiedades vigentes que identifican campo, referencia y causa cuando corresponda.
- **FR-021**: Cada actualización con al menos un cambio efectivo DEBE agregar exactamente un evento funcional append-only en la misma unidad atómica que el cambio del registro.
- **FR-022**: El evento DEBE identificar el tipo y código de registro y conservar actor, fecha, UE, versión anterior, versión nueva, resultado exitoso y únicamente los campos efectivamente modificados con sus valores anterior y nuevo.
- **FR-022A**: En auditoría, una referencia de catálogo DEBE conservar identidad, código y nombre; el conjunto de Unidades Orgánicas responsables DEBE conservar listas ordenadas de identidad, código, nombre y posición; un PEI o POI retirado DEBE representar explícitamente el nuevo valor nulo.
- **FR-022B**: El request de actualización NO DEBE incluir un motivo u observación de edición y el evento funcional NO DEBE inventar ese dato; la trazabilidad se compone de los valores anterior/nuevo y del contexto aprobado de registro, actor, UE, fecha y versiones.
- **FR-023**: La auditoría NO DEBE guardar tokens, cuerpos HTTP completos, archivos ni contenido documental, y NO DEBE modificar o eliminar eventos anteriores.
- **FR-024**: Un intento rechazado NO DEBE crear un evento funcional de actualización exitosa; la auditoría de acceso vigente conserva método, ruta, estado HTTP y correlación.
- **FR-025**: La edición NO DEBE alterar por sí misma el estado, ejecutar una transición, aprobar una iniciativa, crear un proyecto, recalcular `closingDate`, modificar documentos, completar tareas ni generar notificaciones de ciclo de vida.
- **FR-026**: Las operaciones vigentes de alta, consulta, aprobación, derivación, incorporación preexistente y transición DEBEN conservar métodos, rutas, requests, responses, reglas, efectos y autorización actuales.
- **FR-027**: La feature NO DEBE incorporar eliminación física o lógica, archivado/suspensión como operación nueva, CRUD de catálogos, usuarios u organizaciones, ni CRUD independiente de documentos, tareas o notificaciones.
- **FR-028**: La feature NO DEBE introducir migraciones destructivas ni cambiar las reglas actuales de alta o la relación de proyectos derivados con su iniciativa origen.
- **FR-029**: La guía funcional DEBE actualizarse en la futura implementación porque cambiarán acciones visibles, datos modificables y comportamiento de autorización; esta especificación no modifica todavía esa guía.
- **FR-030**: Una solicitud sin ningún campo editable o sin un cambio efectivo DEBE responder `422`, conservar la versión y no crear un evento funcional, evitando auditoría engañosa y versiones sin modificación.

### Key Entities *(include if feature involves data)*

- **Registro de portafolio**: Iniciativa o proyecto identificado por código; conserva tipo, origen, UE, campos operativos, estado, fechas técnicas y versión optimista.
- **Iniciativa**: Registro sin origen que nace `Presentado` y puede originar como máximo un proyecto derivado conforme al ciclo vigente.
- **Proyecto derivado**: Proyecto vinculado de manera única a una iniciativa y con Unidad Ejecutora heredada; su origen y vínculo no son datos de edición general.
- **Proyecto preexistente**: Proyecto sin iniciativa predecesora, con modo preexistente y código de origen `NA`.
- **Unidad Ejecutora**: Ámbito real del registro usado para autorización y coherencia organizacional.
- **Unidad Orgánica responsable**: Referencia ordenada y normalizada asociada al registro, válida solo si pertenece a la misma UE y está activa al escribir.
- **Referencia de catálogo**: Identidad persistente de Tipo de solución, Fuente u origen, Objetivo PEI o Actividad POI, validada dentro de su catálogo y estado.
- **Versión del registro**: Valor que identifica la copia abierta por el usuario y evita que una actualización antigua sobrescriba una nueva.
- **Evento de auditoría**: Evidencia append-only de una actualización confirmada, asociada al registro y actor sin contener secretos ni contenido documental.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100 % de los escenarios de aceptación, solo un `Administrador PIIP` con cobertura vigente sobre la UE real puede confirmar una edición, independientemente del creador original.
- **SC-002**: En el 100 % de las solicitudes con versión desactualizada, se conserva la actualización más reciente, se responde conflicto y no aparece un evento funcional de éxito para el intento rechazado.
- **SC-003**: El 100 % de las referencias inválidas, inactivas, de catálogo incorrecto o de otra UE se rechaza sin cambios parciales.
- **SC-004**: El 100 % de las actualizaciones exitosas queda visible en detalle y listado tras el refresco y devuelve una versión superior a la enviada.
- **SC-005**: En el 100 % de las ejecuciones cronometradas de aceptación, un usuario autorizado completa una edición válida en menos de 3 minutos, medidos desde que el detalle vigente termina de cargar hasta que aparece la confirmación de éxito en el detalle actualizado, sin volver a capturar campos no modificados. Cada ejecución DEBE usar un conjunto de cambios definido antes de iniciar y conservar identificador de ejecución, instante inicial, instante final, duración y resultado.
- **SC-006**: En una prueba de aceptación con una muestra no vacía cuyo tamaño y perfiles hayan sido aprobados y registrados antes de iniciar, al menos el 90 % de los participantes clasifica sin errores la matriz completa de campos mostrada para su variante como editable o solo lectura y explica correctamente, ante un escenario suministrado, por qué la acción de edición no está disponible. El resultado DEBE conservar cantidad total, cantidad aprobada y porcentaje obtenido.
- **SC-007**: El 100 % de las actualizaciones exitosas agrega exactamente un evento funcional de actualización y conserva todos los eventos anteriores.
- **SC-008**: Cero recorridos vigentes de alta, consulta, aprobación, derivación, incorporación preexistente o transición cambian sus reglas observables por la incorporación de edición.
- **SC-009**: Cero operaciones de esta feature eliminan, archivan, suspenden, transicionan o alteran documentos, tareas, notificaciones, usuarios, catálogos u organizaciones como efecto implícito.

## Assumptions

- La autenticación Keycloak, la autorización local Oracle y la selección de Unidad Ejecutora vigente se reutilizan; esta feature no crea roles ni ámbitos nuevos.
- `PATCH` expresa una modificación parcial controlada y resulta preferible a `PUT`, porque el cliente no reemplaza los 23 campos ni los subrecursos documentales y omitir un campo conserva su valor.
- La versión seguirá siendo un número incluido en el contrato y en la representación completa; no se introduce un segundo mecanismo de versión ni se infiere una política de `ETag` sin evidencia.
- Los formularios de alta pueden reutilizar presentación, validadores y carga de catálogos, pero edición constituye rutas y casos de uso explícitos; no se sobrecarga silenciosamente el alta.
- Los borradores locales vigentes para operaciones de alta no se reutilizan en edición.
- Los campos documentales, las tareas y las notificaciones pertenecen a operaciones independientes y no forman parte del request de actualización general.
- Los listados pueden añadir un acceso contextual a edición, mientras que el detalle ofrece el acceso principal y la confirmación ocurre en una pantalla dedicada.
- Las tres clarificaciones funcionales quedaron resueltas el 2026-08-22; no quedan decisiones bloqueantes para iniciar `/speckit-plan`.

## Out of Scope

- Eliminación física o lógica de iniciativas, proyectos o evidencia.
- Nuevas operaciones de archivado, suspensión, cancelación, aprobación o transición.
- CRUD de catálogos, usuarios, roles, instituciones, Unidades Ejecutoras o Unidades Orgánicas.
- CRUD independiente o cambios de reglas para documentos, tareas o notificaciones.
- Migraciones destructivas, limpieza de datos o alteración de reglas de alta.
- Cambio de la relación única entre proyecto derivado e iniciativa origen.
- Edición inline en los listados.
- Implementación de código, endpoints, cliente generado, cambios JPA/DDL, migraciones o ejecución de verificaciones durante `/speckit-specify`.
