# Feature Specification: Administración integral de usuarios

**Feature Branch**: `008-administrar-usuarios`

**Created**: 2026-08-10

**Status**: Draft

**Input**: User description: "Implementar el CRUD completo de la Administración de Usuarios en el frontend y backend"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar usuarios y asignaciones autorizadas (Priority: P1)

Un administrador PIIP consulta un listado claro de los usuarios que puede administrar y de sus asignaciones vigentes, para conocer quién tiene acceso y en qué ámbito institucional.

**Why this priority**: La administración segura de accesos requiere primero una vista confiable de las asignaciones vigentes dentro del ámbito del administrador.

**Independent Test**: Un administrador con ámbitos asignados puede abrir la administración y comprobar que ve usuarios, roles, institución, Unidad Ejecutora y estado exclusivamente para los ámbitos que administra.

**Acceptance Scenarios**:

1. **Given** un administrador PIIP con ámbitos autorizados, **When** consulta la administración de usuarios, **Then** ve los usuarios y asignaciones vigentes que pertenecen a esos ámbitos.
2. **Given** un administrador PIIP sin cobertura institucional para un ámbito, **When** consulta la administración, **Then** no puede ver ni administrar asignaciones de ese ámbito.
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

### Edge Cases

- Una persona no tiene aún un registro local disponible para ser administrada porque nunca se autenticó en PIIP.
- Se intenta crear una asignación que ya se encuentra vigente para la misma persona, rol e igual ámbito.
- Se intenta retirar la última asignación de Administrador PIIP que cubre un ámbito.
- La institución y la Unidad Ejecutora seleccionadas no guardan relación entre sí.
- Dos administradores modifican o retiran la misma asignación de manera simultánea.
- El administrador pierde su autorización o cambia de ámbito durante una operación de administración.
- La cuenta de una persona con asignaciones vigentes es deshabilitada en Keycloak; PIIP no altera sus asignaciones ni ofrece una acción local para habilitarla.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir únicamente a los Administradores PIIP autorizados acceder a la administración de usuarios.
- **FR-002**: El sistema DEBE mostrar usuarios y asignaciones únicamente dentro de los ámbitos institucionales y de Unidad Ejecutora que el administrador puede gestionar.
- **FR-003**: El sistema DEBE mostrar, para cada asignación visible, la persona, rol, institución, Unidad Ejecutora, estado de vigencia y la acción disponible.
- **FR-004**: El sistema DEBE permitir crear una asignación de rol para una persona elegible, una institución y, cuando corresponda, una Unidad Ejecutora dentro del ámbito autorizado del administrador.
- **FR-005**: El sistema DEBE impedir una asignación vigente duplicada para la misma persona, rol e igual ámbito.
- **FR-006**: El sistema DEBE permitir actualizar el rol, la institución y la Unidad Ejecutora de una asignación vigente dentro del ámbito autorizado, conservando la misma asignación y registrando los valores anteriores y nuevos en la evidencia de auditoría.
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

### Key Entities *(include if feature involves data)*

- **Usuario local**: Registro de la identidad que ya interactuó con PIIP; contiene el identificador de autenticación, nombre visible, correo y sus asignaciones. El ciclo de vida de su cuenta pertenece a Keycloak.
- **Asignación de rol y ámbito**: Autorización de una persona para ejercer un rol dentro de una institución y, de manera opcional, una Unidad Ejecutora; tiene estado de vigencia y versión para controlar cambios concurrentes.
- **Rol PIIP**: Tipo de acceso funcional que se concede a un usuario dentro de un ámbito.
- **Institución y Unidad Ejecutora**: Límites organizacionales que determinan el alcance de una asignación y de la administración permitida.
- **Evidencia de auditoría**: Registro de la acción administrativa, su objeto y el actor responsable, sin información sensible.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las operaciones de administración realizadas por un administrador con ámbito autorizado afectan únicamente usuarios y asignaciones dentro de ese ámbito.
- **SC-002**: En pruebas de aceptación, el 100 % de los intentos de crear una asignación vigente duplicada, retirar al último administrador del ámbito o actualizar una versión desactualizada son rechazados sin alterar el estado válido existente.
- **SC-003**: Al menos el 95 % de los administradores de prueba completa una creación, modificación o retiro de asignación sin asistencia en menos de 2 minutos por operación.
- **SC-004**: El 100 % de las creaciones, modificaciones y retiros exitosos de asignaciones produce evidencia auditable sin exponer información sensible.
- **SC-005**: El administrador recibe confirmación o un mensaje accionable de error para el 100 % de las operaciones iniciadas desde la pantalla.

## Assumptions

- Keycloak es la única autoridad para habilitar, inhabilitar y administrar cuentas; PIIP no integra su API administrativa ni muestra ese estado en esta iteración.
- Los roles funcionales disponibles y los datos organizacionales ya definidos por PIIP continúan siendo la fuente para las opciones de asignación.
- Una persona debe tener un registro local disponible antes de que se le asigne un rol; no se asume aprovisionamiento administrativo de nuevas identidades.
- Los usuarios locales sin asignaciones son candidatos de primera asignación para cualquier Administrador PIIP; la cobertura se valida sobre el ámbito elegido al crear la asignación.
- Las operaciones sensibles mantienen controles de autorización por ámbito, control de concurrencia y evidencia de auditoría.
- La edición conserva la asignación vigente y permite cambiar su rol, institución y Unidad Ejecutora; la auditoría registra los valores anteriores y nuevos.
- El retiro de una asignación es una suspensión reversible, no una eliminación física ni lógica definitiva.
- Las garantías de último Administrador PIIP se aplican a las asignaciones locales activas; la disponibilidad de las cuentas asociadas se gestiona operativamente en Keycloak.
