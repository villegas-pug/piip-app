# Especificación de Feature: Administración de Unidades Organizacionales

**Feature Branch**: `019-administrar-unidades-organizacionales`

**Created**: 2026-09-11

**Status**: Draft

**Input**: Administración separada de Unidades Ejecutoras y Unidades Orgánicas, con autorización institucional de `ADMINISTRADOR_PIIP`, trazabilidad, concurrencia y preservación de las consultas organizacionales vigentes.

## Clarifications

### Session 2026-09-11

- Q: ¿Qué debe ocurrir al desactivar una Unidad Ejecutora que todavía tiene Unidades Orgánicas activas? → A: Permitir la desactivación y mantener activas las UO.
- Q: Si ya existe el código `UE-001` en una institución, ¿debe permitirse crear otro como `ue-001` en esa misma institución? → A: No permitirlo; ambos se consideran el mismo código. El backend genera los códigos dentro de la transacción de alta y no los define el usuario.
- Q: ¿La numeración automática debe reiniciarse en cada institución para las UE y en cada UE para las UO? → A: Sí; usar prefijo por tipo y número consecutivo reiniciado por ámbito.
- Q: ¿Qué prefijo debe llevar cada código generado automáticamente? → A: `UE-` para Unidades Ejecutoras y `UO-` para Unidades Orgánicas, seguidos del consecutivo.
- Q: ¿Cómo se habilita la feature cuando ya existen UE en Oracle? → A: Solo se habilita inicialmente bajo `test,test-reset`; la semilla incorpora los valores de orden y fechas. No se habilita en bases con UE preexistentes ni se inventa una migración productiva.
- Q: ¿Debe el administrador indicar un orden de presentación no negativo al crear toda Unidad Ejecutora? → A: No es obligatorio. Si no se informa, el backend asigna el mayor orden de la institución más uno; usa 0 si aún no existe una UE.

## Clasificación del grounding

### Hechos confirmados del baseline

- La relación persistida vigente es `INSTITUCION` -> `UNIDAD_EJECUTORA` -> `UNIDAD_ORGANICA`. `ExecutingUnitEntity.institution` y `OrganizationalUnitEntity.executingUnit` son obligatorios; los códigos son únicos, respectivamente, dentro de la institución y de la Unidad Ejecutora.
- `ExecutingUnitEntity` contiene `institution`, `code`, `name`, `active` y `version`; no contiene actualmente orden de presentación, fecha de registro ni fecha de activación. La descripción visible puede derivarse de `name` sin crear una columna de descripción.
- `OrganizationalUnitEntity` contiene `executingUnit`, `parent`, `code`, `name`, `acronym`, `active` y `version`. Su campo `parent` materializa hoy `UNIDAD_ORGANICA.ID_UNIDAD_PADRE` como FK autorreferente.
- `OrganizationController` solo publica consultas: `GET /institutions`, `GET /executing-units` y `GET /organizational-units?executingUnitId=...`. El último endpoint conserva el catálogo de Unidades Orgánicas activas de la UE solicitada con `acronym` no nulo ni vacío.
- `OrganizationQueryService` ya limita las instituciones y UEs consultables al ámbito autorizado; para Unidades Orgánicas exige lectura de la UE. El frontend carga estas consultas en `PiipHttpRepository` y modela `ExecutingUnit` y `OrganizationalUnit` en `piip.models.ts`.
- `UNIDAD_EJECUTORA` y `UNIDAD_ORGANICA` ya tienen `VERSION`; por tanto, el baseline dispone de control de concurrencia optimista para ambos maestros.
- El DDL derivado actualmente conserva `FK_UO_PADRE` desde `UNIDAD_ORGANICA.ID_UNIDAD_PADRE` hacia `UNIDAD_ORGANICA`. Esta relación no acredita una regla funcional de jerarquía interna.
- Los datos sintéticos de organización residen en `apps/backend/src/main/resources/db/test/catalog-data.sql`; se cargan únicamente con los perfiles exactos `test,test-reset`, son idempotentes y tienen validación posterior fail-closed. Los entornos ordinarios usan validación de esquema y no cargan ese seed.

### Decisiones de esta especificación

- La administración se organiza en dos experiencias independientes: una para `ExecutingUnitEntity` y otra para `OrganizationalUnitEntity`. Cada una tiene su propia ruta, listado, formulario y acciones; no se incrusta el formulario de UO dentro del de UE.
- Para estas dos administraciones, `ADMINISTRADOR_PIIP` es el único rol que concede capacidad de escritura. No se crea ningún rol ni se amplía la capacidad de otros roles existentes fuera de este alcance.
- Una UE nueva recibe su institución del contexto institucional previamente seleccionado por el administrador. La institución se muestra como referencia heredada y no editable; el cliente no puede escoger ni sustituir libremente `institution`.
- Una UO nueva recibe su UE del contexto seleccionado. La UE se muestra como referencia heredada y no editable; el cliente no puede elegir ni sustituir libremente `executingUnit`.
- El backend genera los códigos de UE y UO dentro de la transacción de alta; son únicos en su ámbito de pertenencia e inmutables después de la creación. La edición conserva tanto su ámbito heredado como su código.
- La desactivación y la reactivación son operaciones reversibles; no existe eliminación física de UE ni UO.
- La desactivación de una UE está permitida aunque contenga UO activas y no altera automáticamente el estado de esas UO.
- `ID_UNIDAD_PADRE` queda explícitamente pendiente: la relación esperada apunta a una tabla externa aún no definida. No se tratará como jerarquía entre UO, ni se usará, expondrá o modificará en las nuevas rutas, formularios, contratos o casos de uso.

### Compatibilidad y contradicciones acotadas

- Se conservan los endpoints de consulta vigentes `GET /institutions`, `GET /executing-units` y `GET /organizational-units?executingUnitId=...` para los consumidores actuales. La administración añadirá sus capacidades sin sustituir el catálogo de UO usado por iniciativas y proyectos.
- La respuesta vigente de `GET /organizational-units` incluye `parentId` por compatibilidad. Esta feature no lo consumirá en las nuevas interfaces ni lo agregará a contratos nuevos; su retiro o rediseño exige la definición aprobada de la tabla externa destino de `ID_UNIDAD_PADRE`.
- Aunque el baseline también contiene el rol `CONSULTA_EXTERNA`, esta feature no lo elimina ni lo modifica. La regla consolidada se aplica a la escritura administrativa: solo `ADMINISTRADOR_PIIP` habilita el CRUD de UE y UO.

### Fuentes canónicas consultadas

- `apps/backend/src/main/java/pe/gob/midagri/piip/organization/persistence/ExecutingUnitEntity.java`
- `apps/backend/src/main/java/pe/gob/midagri/piip/organization/persistence/OrganizationalUnitEntity.java`
- `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationController.java`
- `apps/backend/src/main/java/pe/gob/midagri/piip/organization/application/OrganizationQueryService.java`
- `apps/frontend/src/app/core/piip-http.repository.ts`
- `apps/frontend/src/app/core/piip.models.ts`
- `database/generated/piip-oracle.sql`
- `docs/architecture/data-model-final.md`
- `docs/funcional/guia-funcional-piip.md`
- `apps/backend/src/main/resources/db/test/catalog-data.sql`

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar Unidades Ejecutoras en una institución (Priority: P1)

Como `ADMINISTRADOR_PIIP` con ámbito institucional explícito, quiero consultar y administrar las Unidades Ejecutoras de una institución autorizada, para mantener el primer nivel operativo de su estructura organizacional.

**Why this priority**: La UE es el contexto obligatorio de las UO y del portafolio; sin su administración institucional no se puede mantener la estructura principal.

**Independent Test**: Se puede seleccionar una institución autorizada, crear una UE y verificar su listado, edición, desactivación y reactivación sin administrar UO.

**Acceptance Scenarios**:

1. **Given** un administrador con ámbito institucional sobre una sola institución, **When** abre la administración de UE, **Then** ve y usa esa institución como contexto heredado no editable.
2. **Given** un administrador con ámbito institucional sobre varias instituciones, **When** abre la administración de UE, **Then** debe seleccionar una institución autorizada antes de crear o administrar sus UE.
3. **Given** una institución autorizada seleccionada, **When** crea una UE activa con nombre válido e informa o no un orden de presentación, **Then** la UE queda asociada a esa institución, recibe un código `UE-` consecutivo, conserva el orden informado o recibe el mayor orden de la institución más uno (0 si no existe una UE) y tiene fecha de registro y fecha de activación iguales al instante de creación.
4. **Given** una UE existente, **When** edita su nombre u orden de presentación con una versión vigente, **Then** conserva su código e institución y obtiene la representación actualizada.
5. **Given** una UE activa, **When** la desactiva con una versión vigente, **Then** deja de estar activa, no se elimina y conserva su fecha de activación anterior.
6. **Given** una UE inactiva, **When** la reactiva con una versión vigente, **Then** vuelve a estar activa y su fecha de activación pasa a ser el instante de la reactivación.

---

### User Story 2 - Administrar Unidades Orgánicas de una UE (Priority: P1)

Como `ADMINISTRADOR_PIIP` autorizado sobre la institución de una UE, quiero consultar y administrar en una ruta separada las Unidades Orgánicas de esa UE, para mantener las opciones organizacionales que pueden participar en iniciativas y proyectos.

**Why this priority**: Las UO son datos maestros requeridos por los registros de portafolio y requieren un ciclo de vida propio, separado de la administración de UE.

**Independent Test**: Se puede abrir una UE autorizada, crear una UO y verificar sus consultas, edición, desactivación y reactivación desde la interfaz exclusiva de UO.

**Acceptance Scenarios**:

1. **Given** una UE autorizada seleccionada, **When** se abre la administración de UO, **Then** la UE se muestra como contexto heredado no editable y se listan sus UO, incluidas las inactivas para fines administrativos.
2. **Given** una UE autorizada seleccionada, **When** se crea una UO con nombre, sigla no vacía y un valor booleano explícito para su estado inicial, **Then** queda asociada obligatoriamente a esa UE, recibe un código `UO-` consecutivo y no se registra ningún valor para `ID_UNIDAD_PADRE`.
3. **Given** una UO activa existente, **When** se edita su nombre o sigla no vacía con una versión vigente, **Then** conserva su código y UE de pertenencia.
4. **Given** una UO activa o inactiva, **When** ejecuta la acción explícita de desactivación o reactivación con una versión vigente, **Then** cambia solo su estado conforme a las reglas de ciclo de vida.
5. **Given** una UO activa, **When** se desactiva, **Then** deja de estar disponible como nueva opción para iniciativas y proyectos sin eliminar su registro administrativo ni la evidencia histórica.
6. **Given** una UO inactiva, **When** se reactiva, **Then** vuelve a estar disponible como opción de catálogo solo si su sigla no está vacía.
7. **Given** que durante la generación el candidato `UO-<consecutivo>` ya existe en la misma UE, **When** el backend detecta la colisión, **Then** rechaza la operación sin crear un registro duplicado ni confirmar auditoría de éxito.

---

### User Story 3 - Navegar entre administraciones sin mezclar formularios (Priority: P2)

Como administrador, quiero ir desde una UE a las UO de esa UE sin abandonar el contexto, para administrar los dos niveles organizacionales de forma fluida y sin confundir sus datos.

**Why this priority**: Reduce errores de ámbito y preserva la separación conceptual entre el primer y segundo nivel organizacional.

**Independent Test**: Se puede iniciar en el listado o detalle administrativo de una UE, navegar a UO y verificar que la UE llega precargada y que se usa un formulario distinto.

**Acceptance Scenarios**:

1. **Given** una UE visible para el administrador, **When** elige administrar sus UO, **Then** navega a la ruta independiente de UO con esa UE precargada.
2. **Given** la ruta de UO abierta desde una UE, **When** se muestra el formulario de alta o edición, **Then** no contiene el formulario de UE ni permite cambiar la UE heredada.
3. **Given** una ruta administrativa abierta directamente con una institución o UE no autorizada, **When** se consulta o intenta una operación, **Then** no se muestran ni modifican datos ajenos al ámbito y se informa el rechazo de autorización.

---

### User Story 4 - Conservar catálogo, auditoría y consistencia (Priority: P2)

Como responsable funcional, quiero que los cambios organizacionales sean auditables, detecten conflictos y no alteren los catálogos actuales de portafolio, para conservar la trazabilidad y evitar referencias organizacionales inválidas.

**Why this priority**: La administración de maestros tiene efectos transversales sobre usuarios, portafolio y datos institucionales, por lo que necesita protección verificable.

**Independent Test**: Se puede realizar cada mutación, consultar su evidencia y comprobar que las consultas actuales de catálogo solo devuelven UO activas de la UE solicitada con sigla no vacía.

**Acceptance Scenarios**:

1. **Given** una creación, edición, desactivación o reactivación confirmada, **When** se consulta la auditoría correspondiente, **Then** se identifica actor, fecha, acción, entidad, ámbito y valores funcionales relevantes, sin cuerpos HTTP ni secretos.
2. **Given** dos administradores con la misma versión inicial de una UE o UO, **When** uno confirma un cambio y el otro intenta confirmar después, **Then** el segundo recibe un conflicto y no sobrescribe el cambio confirmado.
3. **Given** una UO inactiva o sin sigla, **When** iniciativas o proyectos consultan el catálogo de su UE, **Then** esa UO no aparece como opción nueva.
4. **Given** un consumidor actual de los endpoints organizacionales de consulta, **When** se entrega la administración, **Then** sus rutas y semántica de lectura vigentes permanecen disponibles.

---

### Edge Cases

- Un administrador intenta crear una UE sin haber seleccionado una institución cuando posee más de una institución autorizada: se rechaza la operación y se solicita seleccionar una institución válida.
- El cliente envía una institución o UE distinta al contexto heredado: la autoridad de autorización valida el ámbito real y rechaza la manipulación.
- Se intenta crear o editar con nombre obligatorio vacío, o la generación produce un código ya existente en su ámbito: se informa el error correspondiente y no se persiste ningún cambio.
- Se intenta editar el código, institución de una UE o UE de una UO: se rechaza por inmutabilidad y herencia de contexto.
- Se solicita una UE, UO, institución o contexto que no existe: se responde como inexistente sin exponer información de otros ámbitos.
- Se intenta desactivar o reactivar una entidad que ya está en ese mismo estado: se rechaza como operación incompatible, sin crear una auditoría de éxito.
- Una UO se desactiva entre la carga del catálogo y el registro de una iniciativa o proyecto: la operación de portafolio conserva su validación y rechaza la nueva referencia no vigente.
- Una respuesta heredada incluye `parentId`: las nuevas interfaces administrativas no lo muestran, no lo envían ni lo convierten en una relación entre UO.

## Requirements *(mandatory)*

### Functional Requirements

**Actores, autorización y contextos**

- **FR-001**: El sistema DEBE conceder las operaciones de alta, consulta administrativa, edición, desactivación y reactivación de UE y UO únicamente a un usuario con rol `ADMINISTRADOR_PIIP` y ámbito institucional activo, vigente y explícito sobre la institución correspondiente.
- **FR-002**: El backend DEBE ser la autoridad final de autorización para toda operación administrativa, incluso si el cliente oculta controles o llega desde una ruta precargada.
- **FR-003**: La administración de UE y UO NO DEBE crear un rol nuevo ni ampliar la capacidad de escritura a otro rol.
- **FR-004**: Cuando el administrador tenga más de una institución autorizada, el sistema DEBE exigir la selección previa de una de esas instituciones antes de listar administrativamente, crear o modificar UE en su contexto.
- **FR-005**: Toda operación sobre una UE DEBE comprobar que su `institution` pertenece al ámbito institucional autorizado del actor; toda operación sobre una UO DEBE comprobar que su `executingUnit` pertenece a una institución autorizada por el actor.

**Unidad Ejecutora**

- **FR-006**: El sistema DEBE proporcionar una interfaz administrativa exclusiva de UE con operaciones de alta, consulta, edición, desactivación y reactivación.
- **FR-007**: Al crear una `ExecutingUnitEntity`, el sistema DEBE asignar `institution` desde el contexto institucional autorizado seleccionado, mostrar la institución como dato heredado no editable y rechazar cualquier intento de escoger o cambiar libremente la institución desde el cliente.
- **FR-008**: La UE DEBE exigir `name`. El backend DEBE generar `code` con el prefijo `UE-` y una numeración consecutiva reiniciada por institución, sin solicitarlo ni aceptarlo desde el formulario. Debe garantizar que sea único dentro de la institución sin distinguir mayúsculas y minúsculas. El código DEBE permanecer inmutable después de crear la UE.
- **FR-009**: La descripción visible de la UE DEBE derivarse de `name` y NO DEBE crear ni requerir una columna de descripción independiente.
- **FR-010**: La UE DEBE conservar un orden de presentación que sirva únicamente para ordenar listados; ese orden NO DEBE interpretarse ni utilizarse como nivel jerárquico. Si el alta lo recibe, el orden DEBE ser un entero no negativo; si no lo recibe, el backend DEBE asignar el mayor orden de la institución más uno, o `0` cuando no exista una UE. La lista administrativa DEBE ordenarlo ascendentemente y, ante valores iguales, ordenar por `name` y luego por identificador técnico.
- **FR-011**: El backend DEBE asignar la fecha de registro de UE al crearla; dicha fecha NO DEBE ser editable.
- **FR-012**: La fecha de activación de UE DEBE indicar el inicio de su vigencia activa actual. Al crear una UE activa, debe ser igual a la fecha de registro; al desactivarla debe conservarse; al reactivarla debe actualizarse al instante de reactivación.
- **FR-013**: La UE DEBE permitir desactivación y reactivación reversibles, y NO DEBE admitir eliminación física. La desactivación DEBE permitirse aunque existan UO activas y NO DEBE cambiar automáticamente el estado de esas UO.

**Unidad Orgánica**

- **FR-014**: El sistema DEBE proporcionar una interfaz administrativa exclusiva de UO con operaciones de alta, consulta, edición, desactivación y reactivación, separada de la interfaz y formularios de UE.
- **FR-015**: Toda `OrganizationalUnitEntity` DEBE pertenecer obligatoriamente a una `ExecutingUnitEntity`. Al crearla, `executingUnit` DEBE heredarse del contexto seleccionado, mostrarse como dato no editable y no poder sustituirse desde el cliente.
- **FR-016**: La UO DEBE conservar los campos vigentes `code`, `name`, `acronym` y `active`; `name` y `acronym` son obligatorios y no pueden estar vacíos en el alta. El alta DEBE recibir un valor booleano explícito para `active`, sin asignar un estado predeterminado. El backend DEBE generar `code` con el prefijo `UO-` y una numeración consecutiva reiniciada por UE, sin solicitarlo ni aceptarlo desde el formulario. Debe garantizar que sea único dentro de la UE sin distinguir mayúsculas y minúsculas. El código DEBE permanecer inmutable después de crear la UO.
- **FR-017**: La desactivación y reactivación de UO DEBEN ser reversibles y NO DEBEN eliminar físicamente su registro ni sus referencias históricas. Una UO activa DEBE conservar `acronym` no nulo ni vacío; una edición no puede vaciar la sigla mientras la UO esté activa y su reactivación se rechaza mientras la sigla permanezca nula o vacía. Una UO activa sin sigla no se ofrece en el catálogo.
- **FR-018**: El catálogo consumido por iniciativas y proyectos DEBE conservar la regla vigente: solo ofrecer UO activas de la UE correspondiente cuyo `acronym` no sea nulo ni esté vacío.

**Navegación y separación de experiencias**

- **FR-019**: Las interfaces de UE y UO DEBEN tener rutas, listados, formularios y acciones de confirmación independientes.
- **FR-020**: Desde la administración de UE, el usuario DEBE poder navegar a la administración de UO con la UE precargada. Esta navegación NO DEBE incrustar el formulario de UO dentro del formulario de UE ni permitir modificar el contexto heredado.

**Concurrencia, auditoría y errores**

- **FR-021**: La edición, desactivación y reactivación de una UE o UO existente DEBEN exigir la versión vigente y rechazar una versión obsoleta sin sobrescribir datos confirmados. El alta no exige versión porque todavía no existe una representación previa.
- **FR-022**: Cada alta, edición, desactivación y reactivación confirmada DEBE generar evidencia de auditoría append-only con actor, fecha, tipo e identificador de entidad, acción, ámbito institucional y valores funcionales relevantes; no DEBE guardar tokens, cuerpos HTTP ni contenido ajeno a la operación. La evidencia DEBE poder consultarse por ámbito mediante la extensión compatible de `GET /audit/events?executingUnitId=...`. Cuando no se informe `executingUnitId`, la consulta DEBE limitarse a las UE cubiertas por los ámbitos institucionales autorizados del actor y NUNCA DEBE devolver eventos sin filtro de ámbito.
- **FR-023**: Las operaciones administrativas DEBEN diferenciar y comunicar, como mínimo, autorización denegada, validación inválida, recurso inexistente y conflicto de concurrencia. Las operaciones rechazadas NO DEBEN registrarse como mutaciones funcionales exitosas.
- **FR-024**: La consulta administrativa DEBE permitir distinguir entidades activas e inactivas sin exponer registros fuera del ámbito autorizado.

**Relación pendiente `ID_UNIDAD_PADRE`**

- **FR-025**: Esta feature NO DEBE usar, exponer ni modificar `ID_UNIDAD_PADRE`, `OrganizationalUnitEntity.parent` ni `parentId` en formularios, contratos o casos de uso nuevos.
- **FR-026**: El sistema NO DEBE presentar `ID_UNIDAD_PADRE` como una jerarquía interna entre dos UO. La tabla externa destino, entidad destino, cardinalidad y reglas de esa relación permanecen pendientes de aprobación y fuera de alcance.

**Compatibilidad e impacto de entrega**

- **FR-027**: Los endpoints de consulta actuales `GET /institutions`, `GET /executing-units` y `GET /organizational-units?executingUnitId=...` DEBEN continuar disponibles y conservar su semántica actual para los consumidores existentes.
- **FR-028**: La entrega DEBE actualizar los contratos de administración y los modelos de presentación necesarios, sin exponer entidades de persistencia como contratos HTTP y sin incorporar `ID_UNIDAD_PADRE` a los contratos nuevos.
- **FR-029**: La entrega DEBE alinear `ExecutingUnitEntity`, `OrganizationalUnitEntity`, `OrganizationController`, `OrganizationQueryService`, `PiipHttpRepository`, los modelos `ExecutingUnit` y `OrganizationalUnit`, las rutas administrativas y la documentación funcional con estas reglas, conservando la compatibilidad definida en FR-027.

**Persistencia, datos y despliegue**

- **FR-030**: Cualquier cambio estructural de UE, UO o auditoría DEBE definirse primero en las entidades JPA y sus anotaciones; el DDL generado por Hibernate DEBE reflejar ese modelo y revisarse como salida derivada. No se debe crear ni editar DDL manual como fuente estructural alternativa.
- **FR-031**: La entrega NO DEBE usar Flyway, Liquibase, `JdbcTemplate`, SQL nativo ni procedimientos almacenados para definir la estructura funcional o ejecutar sus operaciones.
- **FR-032**: Si se requieren columnas nuevas en `UNIDAD_EJECUTORA` para orden de presentación, fecha de registro o fecha de activación, la especificación de despliegue DEBE identificar la coordinación previa de Oracle requerida, porque los ambientes ordinarios validan el esquema y no lo modifican automáticamente.
- **FR-033**: El seed de datos sintéticos DEBE permanecer exclusivamente en `apps/backend/src/main/resources/db/test/catalog-data.sql` y ejecutarse solo bajo los perfiles exactos `test,test-reset`; debe preservar idempotencia, guardias fail-closed y postvalidaciones.
- **FR-034**: Si la entrega cambia instituciones, UE o UO sintéticas, DEBE actualizar el seed y sus postvalidaciones correspondientes. No DEBE modificar datos institucionales reales, ejecutar el seed en producción ni usar el seed como mecanismo de migración de producción.
- **FR-035**: La entrega DEBE distinguir en su evidencia de implementación entre: cambios JPA que definen el modelo estructural, DML sintético exclusivo de `test-reset` y datos reales de ambientes institucionales. Una migración externa aprobada, si fuese necesaria, DEBE tratarse como dependencia de despliegue sin inventar un mecanismo nuevo.

**Documentación y pruebas**

- **FR-036**: La guía funcional y el modelo de datos de arquitectura DEBEN actualizarse en español en la misma entrega de implementación para describir la administración separada, la cadena Institución -> UE -> UO, el rol administrativo, la herencia de contexto y la exclusión de `ID_UNIDAD_PADRE`.
- **FR-037**: La entrega DEBE incluir pruebas focalizadas de autorización institucional, herencia de contexto, unicidad, inmutabilidad, estados, fechas de UE, auditoría, concurrencia, separación de rutas, compatibilidad de consultas y reglas del catálogo de UO.

### Key Entities *(include if feature involves data)*

- **Institución (`INSTITUCION`)**: límite institucional del ámbito administrativo. Es la referencia heredada al crear una UE.
- **Unidad Ejecutora (`ExecutingUnitEntity` / `UNIDAD_EJECUTORA`)**: primer nivel organizacional de una institución; posee `code`, `name`, estado, versión, orden de presentación y fechas funcionales de registro y activación.
- **Unidad Orgánica (`OrganizationalUnitEntity` / `UNIDAD_ORGANICA`)**: segundo nivel organizacional de una UE; posee `code`, `name`, `acronym`, estado y versión. Es la fuente de las opciones organizacionales de iniciativas y proyectos.
- **Ámbito administrativo (`USUARIO_ROL_AMBITO`)**: asignación activa y vigente que une al actor con `ADMINISTRADOR_PIIP` y una institución autorizada.
- **Evento de auditoría (`EVENTO_AUDITORIA`)**: evidencia append-only de las mutaciones administrativas confirmadas, sin secretos ni cuerpos HTTP.
- **Relación pendiente (`ID_UNIDAD_PADRE`)**: FK actualmente autorreferente en `UNIDAD_ORGANICA`; su tabla externa destino, entidad y cardinalidad están pendientes y no forman parte de esta feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las altas de UE se completa con una institución heredada autorizada; ningún intento de crear una UE sin contexto institucional válido o con una institución manipulada se confirma.
- **SC-002**: El 100% de las altas de UO se completa con una UE heredada autorizada; ningún intento de crear o mover una UO hacia otra UE desde el formulario se confirma.
- **SC-003**: El 100% de los códigos generados es único y no vacío dentro de su ámbito, incluso frente a diferencias de mayúsculas o minúsculas; el 100% de los intentos de modificar un código después del alta se rechaza sin cambiar el registro existente.
- **SC-004**: En el 100% de las UE creadas activas, fecha de registro y fecha de activación coinciden; tras una desactivación se conserva la fecha de activación y tras cada reactivación se actualiza.
- **SC-005**: El 100% de las acciones de alta, edición, desactivación y reactivación confirmadas deja evidencia auditable; el 0% de las mutaciones con versión obsoleta sobrescribe un cambio previo.
- **SC-006**: Un administrador autorizado puede completar el alta de una UE o UO en menos de 3 minutos con los datos obligatorios disponibles; al menos el 90% de usuarios de prueba completa ambas tareas en su primer intento.
- **SC-007**: El 100% de las navegaciones desde una UE a sus UO conserva la UE precargada y muestra un formulario de UO independiente, sin campos ni formulario de UE incrustados.
- **SC-008**: El catálogo de iniciativas y proyectos mantiene el 100% de sus opciones dentro de la UE solicitada, activas y con sigla no vacía; las tres consultas organizacionales existentes continúan disponibles para sus consumidores.

## Assumptions

- La institución seleccionable se limita a los ámbitos institucionales activos y vigentes de `ADMINISTRADOR_PIIP`; no se infiere autorización a partir de datos enviados por el cliente.
- La consulta administrativa de UE y UO incluirá elementos activos e inactivos dentro del ámbito autorizado, mientras que el catálogo de portafolio conservará solo UO activas con `acronym` no vacío.
- La inmutabilidad del código se aplica tanto a UE como a UO; la edición permite actualizar únicamente los demás campos admitidos por esta especificación y conserva su contexto heredado.
- El código es un dato obligatorio generado por el backend al crear una UE o UO; no se ingresa ni se puede sustituir desde el cliente. Usa `UE-` para UE y `UO-` para UO, seguidos de un consecutivo que se reinicia por institución para UE y por UE para UO.
- Los valores de fecha se establecen por la autoridad del servidor y se comunican como instantes auditables; no se define en esta feature una zona horaria nueva.
- La nueva estructura funcional de UE requerirá ampliar `UNIDAD_EJECUTORA` con los atributos persistidos necesarios para orden y fechas. La modificación efectiva del esquema de Oracle dependerá de una coordinación de despliegue aprobada.
- La habilitación inicial de esta feature queda limitada al perfil exacto `test,test-reset`. La semilla sintética definirá valores deterministas de orden, fecha de registro y fecha de activación para las UE que crea.
- La UO no recibe campos funcionales nuevos en esta versión, salvo los necesarios para exponer su administración separada y su control de concurrencia ya existente.

## Out of Scope

- Eliminar, renombrar o modificar roles existentes fuera de la autorización de estas interfaces; en particular, no se crea ningún rol nuevo.
- Eliminación física, traslado de institución de una UE o traslado de UE de una UO.
- Crear una columna de descripción independiente para `UNIDAD_EJECUTORA`.
- Interpretar el orden de UE como jerarquía organizacional.
- Inventar la tabla externa destino, entidad destino, cardinalidad o reglas de `ID_UNIDAD_PADRE`.
- Exponer, editar o utilizar `ID_UNIDAD_PADRE`, `parent` o `parentId` en los nuevos contratos, formularios o casos de uso.
- Alterar las reglas de iniciativas, proyectos, sus Unidades Orgánicas Involucradas, estados, documentos, tareas o notificaciones, salvo preservar su catálogo de UO vigente.
- Cargar o modificar datos institucionales reales, ejecutar semillas en producción o crear un mecanismo nuevo de migración de producción.
- Habilitar esta feature en una base Oracle que ya contenga UE, hasta que exista una decisión posterior y aprobada de datos históricos y despliegue.
- Sustituir o retirar los endpoints de consulta vigentes dentro de esta feature.

## Dependencies

- Asignaciones activas y vigentes de `ADMINISTRADOR_PIIP` con ámbito institucional explícito en `USUARIO_ROL_AMBITO`.
- Coordinación de despliegue con Oracle para cualquier diferencia entre el modelo JPA aprobado y el esquema validado por ambientes ordinarios.
- Contrato administrativo aprobado y posterior sincronización de sus consumidores, sin romper `GET /institutions`, `GET /executing-units` ni `GET /organizational-units?executingUnitId=...`.
- Definición funcional posterior y aprobada de la relación externa objetivo de `ID_UNIDAD_PADRE` antes de incluirla en cualquier caso de uso.
