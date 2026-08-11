# Feature Specification: Administración integral de usuarios

**Feature Branch**: `008-administrar-usuarios`

**Created**: 2026-08-10

**Status**: Draft

**Input**: User description: "Implementar el CRUD completo de la Administración de Usuarios en el frontend y backend"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar usuarios y asignaciones autorizadas (Priority: P1)

Un administrador PIIP consulta un listado claro de los usuarios que puede administrar y de sus asignaciones vigentes, para conocer quién tiene acceso y en qué ámbito institucional.

**Why this priority**: La administración segura de accesos requiere primero una vista confiable de las asignaciones vigentes dentro del ámbito del administrador.

**Independent Test**: Un administrador con ámbitos asignados puede abrir la administración desde una UE donde tenga ese rol y comprobar que ve usuarios, roles, institución, Unidad Ejecutora y estado exclusivamente para las instituciones que administra.

**Acceptance Scenarios**:

1. **Given** un administrador PIIP con un grant vigente dentro de una institución, **When** consulta la administración de usuarios, **Then** ve los usuarios y asignaciones de todas las UE de esa institución.
2. **Given** un administrador PIIP sin ningún grant Administrador dentro de otra institución, **When** consulta la administración, **Then** no puede ver ni administrar asignaciones de esa institución.
3. **Given** que no hay asignaciones visibles en el ámbito autorizado, **When** se muestra el listado, **Then** se informa claramente que no existen resultados sin presentar datos de otros ámbitos.

---

### User Story 2 - Gestionar una asignación de rol y ámbito (Priority: P1)

Un administrador PIIP crea, modifica y retira una asignación de rol dentro de los ámbitos que administra, para otorgar, corregir o retirar el acceso funcional de una persona.

**Why this priority**: La asignación de rol y ámbito determina el acceso funcional al sistema y es la operación central de la pantalla.

**Independent Test**: Un administrador autorizado crea una asignación, la modifica según las reglas aprobadas y la retira; cada operación se refleja en el listado y deja evidencia de auditoría.

**Acceptance Scenarios**:

1. **Given** una persona elegible y un ámbito que el administrador cubre, **When** crea una asignación válida, **Then** la persona recibe el rol solicitado dentro de ese ámbito y la asignación queda visible como vigente.
2. **Given** una asignación vigente dentro del ámbito del administrador, **When** actualiza su rol y/o ámbito, **Then** el sistema actualiza esa misma asignación, conserva su identidad y registra los valores anteriores y nuevos en la evidencia de auditoría.
3. **Given** una asignación vigente, **When** el administrador solicita retirarla, **Then** el sistema aplica el mecanismo de retiro aprobado y deja de considerarla para el acceso funcional.
4. **Given** una modificación simultánea de la misma asignación, **When** el administrador intenta guardar una versión desactualizada, **Then** el sistema rechaza la operación y solicita actualizar la información antes de reintentar.
5. **Given** un usuario local provisionado sin ninguna asignación previa, **When** un Administrador PIIP abre “Nueva asignación”, **Then** el usuario aparece como candidato para recibir su primer rol y ámbito, sin que PIIP administre el estado de su cuenta.

---

### User Story 3 - Separar estado de cuenta y autorización PIIP (Priority: P1)

Un administrador PIIP gestiona exclusivamente roles y ámbitos, mientras el ciclo de vida de la cuenta de cada persona se administra en Keycloak, para evitar decisiones contradictorias sobre su acceso.

**Why this priority**: Una misma cuenta no debe tener dos autoridades que puedan habilitarla o inhabilitarla con resultados distintos.

**Independent Test**: La administración permite operar asignaciones vigentes sin mostrar controles de estado de cuenta, y una asignación vigente conserva su semántica aunque el valor local heredado no sea activo.

**Acceptance Scenarios**:

1. **Given** una asignación visible, **When** el administrador consulta la pantalla, **Then** ve únicamente su estado de asignación y no puede habilitar ni inhabilitar la cuenta desde PIIP.
2. **Given** un usuario local con valor heredado inactivo y una asignación vigente, **When** PIIP resuelve su autorización después de una autenticación válida, **Then** considera la asignación vigente sin usar ese valor heredado.
3. **Given** una cuenta deshabilitada en Keycloak, **When** se administran sus asignaciones en PIIP, **Then** PIIP no modifica la cuenta ni reemplaza el procedimiento de Keycloak para habilitarla.

---

### User Story 4 - Aplicar cada rol exclusivamente dentro de su ámbito (Priority: P1)

Una persona con distintos roles en distintas Unidades Ejecutoras ejerce en cada ámbito únicamente las capacidades concedidas por la asignación correspondiente, para impedir que un rol privilegiado se combine con la cobertura de otra asignación.

**Why this priority**: La separación entre rol y ámbito puede ampliar privilegios y permitir escrituras administrativas fuera de la Unidad Ejecutora autorizada.

**Independent Test**: Una persona con Consulta externa en UE-001 y Administrador PIIP en UE-002 conserva esos roles operativos exactos, no puede ejecutar operaciones funcionales de Administrador sobre UE-001 y, al ingresar a Administración de usuarios desde UE-002, puede gestionar asignaciones de todas las Unidades Ejecutoras de la misma institución.

**Acceptance Scenarios**:

1. **Given** Consulta externa en UE-001 y Administrador PIIP en UE-002, **When** la persona intenta crear, aprobar o administrar un recurso funcional de UE-001, **Then** el sistema rechaza la operación sin combinar el rol de UE-002 con el ámbito de UE-001.
2. **Given** las mismas asignaciones, **When** consulta información legible de UE-001, **Then** conserva el acceso de lectura propio de Consulta externa.
3. **Given** que la persona cambia la Unidad Ejecutora activa, **When** la interfaz actualiza el contexto, **Then** muestra el rol efectivo de esa UE y ajusta las acciones disponibles sin conservar privilegios del contexto anterior.
4. **Given** Administrador PIIP y Consulta externa sobre la misma UE, **When** se determina el rol efectivo, **Then** prevalece Administrador PIIP.
5. **Given** que la persona administra al menos un ámbito, **When** intenta abrir Administración de usuarios desde una UE donde sólo tiene Consulta externa, **Then** el acceso permanece visible pero deshabilitado y una URL directa la devuelve al inicio con una indicación para seleccionar una UE administrable.
6. **Given** que la persona selecciona UE-002 cubierta por Administrador PIIP, **When** abre Administración de usuarios, **Then** accede a una bandeja que reúne las asignaciones de todas las Unidades Ejecutoras de la misma institución y distingue expresamente la UE activa usada para ingresar.
7. **Given** Consulta externa en UE-001 y Administrador PIIP en UE-002, **When** la persona consulta o modifica desde UE-002 una asignación perteneciente a UE-001, **Then** la operación administrativa está permitida sin cambiar su rol operativo en UE-001.
8. **Given** un Administrador PIIP de una Unidad Ejecutora de MIDAGRI, **When** crea o edita una asignación con alcance `Toda la institución`, **Then** el sistema advierte el alcance, solicita confirmación y permite la operación incluso cuando el destinatario es el propio actor.
9. **Given** un Administrador PIIP de una institución, **When** intenta administrar una asignación perteneciente a otra institución, **Then** el sistema rechaza la operación sin revelar ni modificar sus datos.

### Edge Cases

- Una persona no tiene aún un registro local disponible para ser administrada porque nunca se autenticó en PIIP.
- Se intenta crear una asignación que ya se encuentra vigente para la misma persona, rol e igual ámbito.
- Se intenta retirar la última asignación de Administrador PIIP que cubre un ámbito.
- La institución y la Unidad Ejecutora seleccionadas no guardan relación entre sí.
- Dos administradores modifican o retiran la misma asignación de manera simultánea.
- El administrador pierde su autorización o cambia de ámbito durante una operación de administración.
- La cuenta de una persona con asignaciones vigentes es deshabilitada en Keycloak; PIIP no altera sus asignaciones ni ofrece una acción local para habilitarla.
- Una persona posee roles operativos diferentes en Unidades Ejecutoras diferentes; ningún rol puede aprovechar la cobertura de otra asignación para operaciones funcionales, aunque Administración de usuarios tenga cobertura institucional propia.
- Una asignación institucional de un rol cubre únicamente las Unidades Ejecutoras de esa institución y conserva el rol con el que fue concedida.
- Una persona posee Administrador PIIP y Consulta externa sobre la misma Unidad Ejecutora; Administrador PIIP prevalece como rol efectivo de esa UE.
- Se abre mediante URL directa un recurso perteneciente a una UE distinta de la seleccionada; las acciones se calculan con la UE real del recurso.
- Se intenta abrir mediante URL directa Administración de usuarios desde una UE activa cubierta sólo por Consulta externa; la interfaz redirige al inicio y no conserva datos administrativos.
- Se cambia desde una UE administrable hacia una UE cubierta sólo por Consulta externa mientras Administración de usuarios está abierta; la interfaz abandona el módulo y limpia su contenido.
- Un Administrador PIIP de una Unidad Ejecutora crea o edita una asignación para toda la institución, incluida una autoasignación; la interfaz advierte expresamente el alcance antes de confirmar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir únicamente a los Administradores PIIP autorizados acceder a la administración de usuarios.
- **FR-002**: Administración de usuarios DEBE mostrar únicamente usuarios y asignaciones pertenecientes a instituciones donde el actor posea al menos una asignación activa y vigente de Administrador PIIP.
- **FR-003**: El sistema DEBE mostrar, para cada asignación visible, la persona, rol, institución, Unidad Ejecutora, estado de vigencia y la acción disponible.
- **FR-004**: El sistema DEBE permitir crear una asignación para cualquier Unidad Ejecutora activa de una institución donde el actor posea al menos una asignación activa y vigente de Administrador PIIP.
- **FR-005**: El sistema DEBE impedir una asignación vigente duplicada para la misma persona, rol e igual ámbito.
- **FR-006**: El sistema DEBE permitir actualizar el rol, la institución y la Unidad Ejecutora de una asignación vigente dentro de una institución administrable, conservando la misma asignación y registrando los valores anteriores y nuevos en la evidencia de auditoría.
- **FR-007**: El sistema DEBE retirar una asignación vigente mediante una suspensión reversible, sin eliminar la evidencia de auditoría.
- **FR-008**: El sistema DEBE impedir retirar la última asignación de Administrador PIIP que mantiene la cobertura requerida de un ámbito.
- **FR-009**: El sistema DEBE detectar que una asignación fue modificada de forma concurrente y evitar que una versión desactualizada sobrescriba el cambio más reciente.
- **FR-010**: El sistema DEBE registrar evidencia de auditoría para la creación, modificación y retiro de asignaciones, sin guardar credenciales, tokens, cuerpos de solicitud ni contenido documental.
- **FR-011**: El sistema DEBE informar al administrador, de forma clara y accionable, los resultados correctos, errores de validación, falta de autorización y conflictos de actualización.
- **FR-012**: El sistema DEBE presentar controles de acción que prevengan envíos duplicados mientras una operación de administración está en curso.
- **FR-013**: PIIP NO DEBE habilitar, inhabilitar ni administrar cuentas de Keycloak; esa decisión pertenece exclusivamente a Keycloak y a su operación autorizada.
- **FR-014**: El acceso funcional PIIP DEBE depender únicamente de una autenticación válida y de asignaciones locales activas y vigentes de rol y ámbito; el estado heredado del registro local no debe modificar esa decisión.
- **FR-015**: El sistema DEBE ofrecer a cualquier Administrador PIIP los usuarios locales sin ninguna asignación previa como candidatos para su primera asignación, sin incluirlos en el listado administrable principal ni consultar el directorio Keycloak.
- **FR-016**: Antes de enviar una alta, el frontend DEBE rechazar la combinación exacta ya visible de usuario, rol, institución y Unidad Ejecutora; el backend conserva la validación transaccional y autoritativa de duplicidad.
- **FR-017**: Toda decisión de autorización sensible DEBE conservar y evaluar conjuntamente el rol, la institución y la Unidad Ejecutora de una misma asignación activa y vigente.
- **FR-018**: Una operación funcional que requiere Administrador PIIP sobre una Unidad Ejecutora DEBE ser rechazada cuando el rol y la cobertura procedan de asignaciones diferentes; la cobertura institucional propia de Administración de usuarios constituye una capacidad separada y no amplía otras operaciones.
- **FR-019**: La lectura general de una Unidad Ejecutora DEBE permitirse cuando cualquier asignación activa y vigente cubra esa Unidad, sin ampliar las capacidades de escritura de su rol.
- **FR-020**: La identidad funcional entregada al frontend DEBE incluir las asignaciones exactas de rol, institución y Unidad Ejecutora necesarias para representar el contexto sin inferencias agregadas inseguras.
- **FR-021**: La interfaz DEBE mostrar el rol efectivo de la Unidad Ejecutora activa, dar precedencia a Administrador PIIP cuando ambos roles cubran la misma Unidad y no asumir un rol antes de cargar el contexto.
- **FR-022**: Administración de usuarios DEBE habilitarse únicamente cuando la Unidad Ejecutora activa esté cubierta por una asignación de Administrador PIIP; cuando exista administración en otra UE, la opción DEBE permanecer visible pero deshabilitada e indicar dónde está disponible.
- **FR-023**: Las acciones sobre un registro abierto directamente DEBEN calcularse usando la Unidad Ejecutora real del registro, aunque difiera de la Unidad activa seleccionada.
- **FR-024**: Una vez habilitado el acceso desde una UE con Administrador PIIP, la bandeja DEBE reunir las asignaciones de todas las Unidades Ejecutoras de las instituciones donde el actor tenga al menos un grant Administrador y comunicar que la UE activa gobierna el ingreso, no el alcance institucional del listado.
- **FR-025**: Si la Unidad Ejecutora activa deja de ser administrable mientras el módulo está abierto, la interfaz DEBE limpiar la vista administrativa, redirigir al inicio e informar el motivo.
- **FR-026**: La opción de asignar a toda una institución DEBE estar disponible para cualquier Administrador PIIP de esa institución y DEBE requerir confirmación explícita de su alcance.
- **FR-027**: La interfaz DEBE obtener las instituciones y Unidades Ejecutoras administrables desde una fuente específica de Administración de usuarios y NO DEBE inferirlas a partir de las Unidades Ejecutoras operativamente legibles.
- **FR-028**: El sistema DEBE permitir que un Administrador PIIP administre sus propias asignaciones, incluida la creación o ampliación a toda la institución, aplicando las mismas validaciones, concurrencia y auditoría que para terceros.
- **FR-029**: La cobertura institucional de Administración de usuarios NO DEBE modificar el rol visible ni habilitar capacidades funcionales sobre una Unidad Ejecutora donde el actor no posea el grant operativo correspondiente.
- **FR-030**: El backend DEBE rechazar la consulta o mutación de asignaciones pertenecientes a instituciones donde el actor no posea ningún grant Administrador activo y vigente.
- **FR-031**: Cambiar desde una UE administradora hacia una UE donde el actor sólo tenga Consulta externa DEBE cerrar Administración de usuarios, aunque la cobertura institucional del actor permita gestionar asignaciones de esa UE al ingresar desde una UE administradora.

### Key Entities *(include if feature involves data)*

- **Usuario local**: Registro de la identidad que ya interactuó con PIIP; contiene el identificador de autenticación, nombre visible, correo y sus asignaciones. El ciclo de vida de su cuenta pertenece a Keycloak.
- **Asignación de rol y ámbito**: Autorización de una persona para ejercer un rol dentro de una institución y, de manera opcional, una Unidad Ejecutora; tiene estado de vigencia y versión para controlar cambios concurrentes.
- **Rol PIIP**: Tipo de acceso funcional que se concede a un usuario dentro de un ámbito.
- **Institución y Unidad Ejecutora**: Límites organizacionales que determinan el alcance de una asignación y de la administración permitida.
- **Evidencia de auditoría**: Registro de la acción administrativa, su objeto y el actor responsable, sin información sensible.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las operaciones de Administración de usuarios afectan únicamente asignaciones de instituciones donde el actor posee al menos un grant Administrador activo y vigente.
- **SC-002**: En pruebas de aceptación, el 100 % de los intentos de crear una asignación vigente duplicada, retirar al último administrador del ámbito o actualizar una versión desactualizada son rechazados sin alterar el estado válido existente.
- **SC-003**: Al menos el 95 % de los administradores de prueba completa una creación, modificación o retiro de asignación sin asistencia en menos de 2 minutos por operación.
- **SC-004**: El 100 % de las creaciones, modificaciones y retiros exitosos de asignaciones produce evidencia auditable sin exponer información sensible.
- **SC-005**: El administrador recibe confirmación o un mensaje accionable de error para el 100 % de las operaciones iniciadas desde la pantalla.
- **SC-006**: El 100 % de los intentos de combinar un rol privilegiado de un ámbito con la cobertura de otro para operaciones funcionales son rechazados sin modificar datos.
- **SC-007**: En el 100 % de los cambios de Unidad Ejecutora evaluados, la interfaz muestra el rol efectivo correcto y no conserva acciones privilegiadas del contexto anterior.
- **SC-008**: En el 100 % de las pruebas con Consulta externa en la UE activa y Administrador PIIP en otra UE, Administración de usuarios no se abre hasta seleccionar una UE administrable y comunica correctamente la UE donde está disponible.
- **SC-009**: En el 100 % de las pruebas con Administrador PIIP en UE-002 y Consulta externa en UE-001, el módulo abierto desde UE-002 permite gestionar asignaciones de UE-001 sin cambiar el rol operativo mostrado en cada UE.

## Assumptions

- Keycloak es la única autoridad para habilitar, inhabilitar y administrar cuentas; PIIP no integra su API administrativa ni muestra ese estado en esta iteración.
- Los roles funcionales disponibles y los datos organizacionales ya definidos por PIIP continúan siendo la fuente para las opciones de asignación.
- Una persona debe tener un registro local disponible antes de que se le asigne un rol; no se asume aprovisionamiento administrativo de nuevas identidades.
- Los usuarios locales sin asignaciones son candidatos de primera asignación para cualquier Administrador PIIP; la cobertura se valida sobre el ámbito elegido al crear la asignación.
- Las operaciones sensibles mantienen controles de autorización por ámbito, control de concurrencia y evidencia de auditoría; Administración de usuarios usa como límite excepcional la institución de un grant Administrador vigente.
- La edición conserva la asignación vigente y permite cambiar su rol, institución y Unidad Ejecutora; la auditoría registra los valores anteriores y nuevos.
- El retiro de una asignación es una suspensión reversible, no una eliminación física ni lógica definitiva.
- Las garantías de último Administrador PIIP se aplican a las asignaciones locales activas; la disponibilidad de las cuentas asociadas se gestiona operativamente en Keycloak.
- Las asignaciones exactas de rol y ámbito son la fuente canónica de autorización; los conjuntos agregados existentes se conservan sólo por compatibilidad temporal.
- El rol visible se determina por la Unidad Ejecutora activa y Administrador PIIP prevalece cuando ambos roles cubren esa misma Unidad.
- La Unidad Ejecutora activa gobierna la entrada a Administración de usuarios: debe estar cubierta por Administrador PIIP. Una vez dentro, la bandeja abarca todas las Unidades Ejecutoras de las instituciones donde el actor tenga algún grant Administrador.
- La UE activa es contexto de navegación del frontend y no se almacena en el servidor. El backend autoriza Administración de usuarios por institución y conserva la autorización exacta `rol + institución + UE` para todas las demás capacidades funcionales.
- Un Administrador de una UE puede conceder asignaciones de alcance institucional y modificar sus propias asignaciones; si se autoasigna Administrador institucional, su rol operativo visible cambiará según el nuevo grant persistido.
- La semántica global actual de la bandeja de auditoría no cambia en este alcance porque no todos sus registros contienen una Unidad Ejecutora.
