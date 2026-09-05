# Feature Specification: Inicialización Oracle con Hibernate y seed externo

**Feature Branch**: `015-inicializacion-oracle`
**Created**: 2026-09-05
**Status**: Draft
**Input**: User description: "Inicializar un esquema Oracle descartable desde cero mediante la definición estructural de Hibernate y un seed SQL externo ejecutado por Spring, manteniendo los perfiles actuales y sin cambiar la lógica funcional del producto."

## Clarifications

### Session 2026-09-05

- Q: ¿Cómo debe resolverse la exigencia constitucional de una confirmación explícita para el proceso destructivo? → A: La Constitución fue enmendada a la versión 1.3.0 para establecer que la activación exacta de `test,test-reset` sustituye la confirmación separada; se mantienen las guardas de conexión, esquema, metadata y `ddl-auto=none`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Inicializar un esquema Oracle descartable (Priority: P1)

Como responsable técnico de un entorno de desarrollo o pruebas, necesito reconstruir un esquema Oracle descartable desde cero para disponer de una base funcional reproducible sin preparar manualmente las tablas ni los datos iniciales.

**Why this priority**: Es el objetivo principal de la feature y permite reproducir de forma controlada la estructura y los datos mínimos del entorno.

**Independent Test**: Con una instancia Oracle descartable y el usuario `SISPIIP` preparados por el DBA, activar exclusivamente `test,test-reset` y comprobar que el proceso termina con las 19 tablas y todos los datos iniciales esperados.

**Acceptance Scenarios**:

1. **Given** una instancia Oracle accesible con el esquema `SISPIIP`, **When** se ejecuta el proceso con exactamente los perfiles `test,test-reset`, **Then** se eliminan y recrean las 19 tablas definidas por el modelo persistente.
2. **Given** las 19 tablas recreadas, **When** finaliza la carga inicial, **Then** existen los roles, la organización, el usuario administrador, sus ámbitos y los catálogos iniciales acordados.
3. **Given** una segunda ejecución sobre el mismo esquema, **When** se repite el proceso, **Then** el resultado estructural y funcional es equivalente al de la primera ejecución y no aparecen duplicados por claves naturales.

---

### User Story 2 - Proteger el arranque ordinario (Priority: P1)

Como responsable de operación, necesito que los perfiles ordinarios no destruyan ni modifiquen el esquema para que el arranque normal conserve los datos existentes.

**Why this priority**: La inicialización es destructiva y debe permanecer completamente separada del funcionamiento normal de la aplicación.

**Independent Test**: Arrancar con el perfil `dev` por defecto y con `prod` activado externamente, y verificar que ambos validan el esquema sin crear, alterar, eliminar ni cargar datos.

**Acceptance Scenarios**:

1. **Given** un arranque sin perfil activo explícito, **When** inicia la aplicación, **Then** se utiliza `dev` como perfil por defecto y se valida el esquema sin ejecutar el reset.
2. **Given** el perfil `prod`, **When** inicia la aplicación, **Then** se mantiene la validación del esquema y no se ejecuta ningún reset ni seed.
3. **Given** una activación de `test-reset` combinada con perfiles adicionales, `prod` o una acción de esquema distinta de `none`, **When** comienza el proceso, **Then** la operación se rechaza antes de cualquier cambio en Oracle.

---

### User Story 3 - Reproducir identidad y datos maestros mínimos (Priority: P2)

Como responsable de pruebas, necesito que el entorno inicial contenga una identidad local vinculada a Keycloak y los datos maestros mínimos para poder probar autorización y operaciones del producto inmediatamente después del reset.

**Why this priority**: Sin identidad, ámbitos y catálogos, una base estructuralmente correcta no sería funcional para las pruebas.

**Independent Test**: Tras la inicialización, consultar por claves naturales los roles, la institución, las unidades ejecutoras, las unidades orgánicas, el usuario y sus ámbitos, y comprobar las cantidades de catálogos, ítems y tipos documentales.

**Acceptance Scenarios**:

1. **Given** un usuario existente en Keycloak cuyo `sub` coincide con el valor literal aprobado en el seed, **When** se completa la carga, **Then** existe un usuario local activo asociado a ese subject.
2. **Given** las unidades ejecutoras `UE-001` y `UE-002`, **When** se completa la carga, **Then** el usuario administrador dispone de una asignación activa `ADMINISTRADOR_PIIP` para cada unidad ejecutora.
3. **Given** los datos sintéticos acordados, **When** se ejecuta la postvalidación, **Then** existen 2 roles, 1 institución, 2 unidades ejecutoras, 4 unidades orgánicas, 1 usuario, 2 ámbitos, 4 catálogos, 17 ítems y 6 tipos documentales.

### Edge Cases

- Si la URL JDBC efectiva no coincide con el objetivo autorizado o el esquema conectado no es `SISPIIP`, la operación debe abortar antes del primer borrado.
- Si la metadata persistente contiene una tabla adicional, falta una tabla esperada o presenta una relación no autorizada, la operación debe abortar.
- Si la acción efectiva de esquema es `create`, `create-drop` o `update`, la operación debe abortar antes de inicializar el contexto persistente.
- Si el subject del usuario no existe en Keycloak, la fila local puede quedar técnicamente cargada, pero la verificación funcional de autenticación debe identificarlo como prerrequisito incumplido y no crear una identidad Keycloak.
- Si falta una tabla allowlisted durante el borrado, solo debe tolerarse el error correspondiente a esa tabla; cualquier otro error debe detener el proceso.
- Si el seed se ejecuta nuevamente, debe actualizar o reutilizar las claves naturales sin duplicar roles, organización, usuario, ámbitos ni datos maestros.
- Si el arranque ordinario no encuentra un ámbito administrador activo en `prod`, debe fallar sin crear ni modificar registros.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir reconstruir un esquema Oracle descartable desde cero mediante la definición estructural canónica del modelo persistente.
- **FR-002**: La reconstrucción MUST abarcar exactamente las 19 tablas del modelo vigente, incluyendo tablas de identidad, organización, operación y auditoría.
- **FR-003**: La operación destructiva MUST ejecutarse únicamente con exactamente los perfiles `test,test-reset`, en ese orden, y MUST rechazar `prod` o cualquier perfil adicional.
- **FR-004**: La operación destructiva MUST comprobar antes de modificar Oracle que la acción efectiva de esquema sea `none` y MUST rechazar `create`, `create-drop` y `update`.
- **FR-005**: La operación MUST validar previamente la URL JDBC autorizada, el fingerprint configurado y el esquema `SISPIIP`.
- **FR-006**: La operación MUST eliminar las tablas en un orden compatible con sus dependencias y recrearlas en un orden compatible con la creación de sus relaciones.
- **FR-007**: La carga inicial MUST ejecutarse mediante un archivo SQL externo versionado, idempotente y limitado a DML; el archivo MUST contener comentarios y bloques legibles ordenados por dependencias.
- **FR-008**: El seed MUST resolver las referencias mediante claves naturales y MUST evitar IDs identity hardcodeados y cualquier instrucción DDL.
- **FR-009**: El seed MUST cargar los roles `ADMINISTRADOR_PIIP` y `CONSULTA_EXTERNA`, sin crear un usuario de consulta externa no definido.
- **FR-010**: El seed MUST cargar la institución `MIDAGRI`, las unidades ejecutoras `UE-001` y `UE-002` y las cuatro unidades orgánicas sintéticas acordadas, conservando el código como nombre donde aplique.
- **FR-011**: El seed MUST cargar el usuario local administrador activo con los valores de identidad aprobados directamente en el seed, sin almacenar contraseñas ni crear identidades en Keycloak.
- **FR-012**: El seed MUST cargar dos ámbitos activos `ADMINISTRADOR_PIIP`, uno para `UE-001` y otro para `UE-002`, vinculados a la institución `MIDAGRI` y al usuario inicial.
- **FR-013**: El seed MUST cargar 4 catálogos, 17 ítems y 6 tipos documentales.
- **FR-014**: La postvalidación MUST comprobar las cantidades y relaciones de los datos iniciales, así como la ausencia de datos operativos, notificaciones y auditoría producidos por el reset.
- **FR-015**: Los perfiles `dev` y `prod` MUST conservar `ddl-auto=validate` y MUST no ejecutar el reset ni el seed durante el arranque ordinario.
- **FR-016**: La configuración MUST usar `dev` como perfil por defecto cuando no se indique un perfil activo y MUST permitir que una activación externa de `prod` lo reemplace.
- **FR-017**: La conexión SID directa MUST funcionar sin depender de `oracle.net.tns_admin` ni de wallet o alias TNS.
- **FR-018**: Los arranques ordinarios MUST no crear automáticamente usuarios, instituciones, unidades ejecutoras ni ámbitos; `prod` MUST validar que exista un ámbito administrador activo y MUST fallar si no existe.
- **FR-019**: La Constitución 1.3.0 establece que la activación exacta de `test,test-reset` sustituye la confirmación separada como autorización operativa del proceso destructivo. El mecanismo DEBE exigir exactamente esos perfiles y no DEBE requerir variables adicionales de habilitación o confirmación.
- **FR-020**: La documentación operativa MUST distinguir la inicialización destructiva de la clonación fiel de una base existente y MUST declarar que el proceso no copia datos institucionales reales.
- **FR-021**: El seed MAY contener los datos personales aprobados del único usuario inicial de pruebas, pero la documentación y los artefactos versionados MUST no contener contraseñas, wallets, tokens ni otras credenciales o secretos.

### Key Entities

- **Esquema Oracle descartable**: conjunto de 19 tablas que puede reconstruirse para desarrollo y pruebas sin afectar producción.
- **Seed inicial**: conjunto versionado de datos mínimos de identidad, organización y catálogos necesarios para operar el entorno de pruebas.
- **Usuario administrador local**: referencia Oracle a una identidad autenticada externamente mediante su `keycloak_subject`.
- **Ámbito de autorización**: relación activa entre usuario, rol, institución y unidad ejecutora que habilita capacidades funcionales.
- **Fingerprint de conexión**: huella de la URL JDBC que limita el proceso destructivo al destino Oracle autorizado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una ejecución autorizada sobre un esquema Oracle descartable deja exactamente 19 tablas y todas las restricciones del modelo vigente, sin intervención manual para crear tablas.
- **SC-002**: Una ejecución autorizada deja exactamente 2 roles, 1 institución, 2 unidades ejecutoras, 4 unidades orgánicas, 1 usuario, 2 ámbitos, 4 catálogos, 17 ítems y 6 tipos documentales.
- **SC-003**: El 100% de los intentos con `prod`, perfiles adicionales, fingerprint incorrecto, esquema incorrecto o acción de esquema distinta de `none` son rechazados antes de cualquier operación destructiva.
- **SC-004**: Dos ejecuciones consecutivas sobre el mismo destino producen el mismo conjunto de claves naturales y no incrementan duplicados en los datos iniciales.
- **SC-005**: Los arranques ordinarios con `dev` y `prod` conservan los datos existentes, no ejecutan operaciones destructivas ni cargas iniciales automáticas y no crean identidades implícitamente.
- **SC-006**: El usuario cuyo `sub` coincide con el seed puede autenticarse y recibe autorización funcional mediante sus dos ámbitos activos, siempre que la identidad exista en Keycloak.
- **SC-007**: El DDL derivado y el seed versionado son revisables por Git; el seed solo contiene los datos personales aprobados del usuario inicial y no contiene contraseñas, tokens, wallets, credenciales ni datos documentales institucionales.

## Assumptions

- La instancia, el servicio SID, el esquema `SISPIIP`, la conectividad, los tablespaces, las cuotas y los privilegios Oracle son preparados previamente por el DBA.
- El esquema utilizado por `test-reset` es descartable y no contiene datos institucionales que deban conservarse.
- La identidad cuyos datos se cargan en el seed existe previamente en el realm de Keycloak acordado y su `sub` coincide con el valor literal aprobado.
- La contraseña de Oracle se proporciona fuera de los cambios versionados y no se incorporará a la especificación, al seed ni a ejemplos reales.
- Los datos personales del usuario inicial del seed fueron aprobados expresamente para este entorno descartable y no representan una carga de datos institucionales.
- El seed representa datos sintéticos de desarrollo y pruebas; no es una migración ni una clonación de la base institucional.
- La enmienda constitucional 1.3.0 que sustituye la confirmación separada por la activación exacta de `test,test-reset` ya está vigente y armoniza el principio IV con FR-019.
- La copia versionada del DDL es un artefacto derivado del modelo persistente y debe regenerarse mediante el mecanismo autorizado antes de actualizarse.
- La feature no cambia las reglas de negocio del portafolio, los 23 campos funcionales, los seis catálogos funcionales ni las transiciones autorizadas.
- La feature no crea usuarios, contraseñas, roles ni asignaciones dentro de Keycloak; solo carga referencias locales de Oracle.
- La integración Oracle, la generación del DDL, las pruebas y los builds se ejecutarán únicamente con autorización operativa explícita.

## Out of Scope

- Clonación fiel o migración de datos reales desde otra instancia Oracle.
- Creación de la instancia, PDB, listener, servicio, tablespace, cuotas, grants o configuración de red Oracle.
- Creación o modificación de cuentas, credenciales, roles o asignaciones en Keycloak.
- Carga automática de catálogos institucionales reales o documentos reales.
- Ejecución del reset durante el arranque ordinario de `dev` o `prod`.
- Incorporación de la inicialización Oracle al alcance funcional de la feature 014.
