# Especificación de Feature: Unidades Orgánicas Involucradas

**Feature Branch**: `017-unidades-organicas-involucradas`

**Created**: 2026-09-07

**Status**: Draft

**Input**: Descripción del usuario: reemplazar la cardinalidad exactamente-una de "Unidad Orgánica responsable" (feature 013) por una lista dinámica y ordenada de "Unidades Orgánicas Involucradas" en registro, edición, revisión, detalle y auditoría de iniciativas y proyectos; validez (activas, misma Unidad Ejecutora, sigla obligatoria, sin duplicados), compatibilidad histórica sin migraciones, presentación accesible, y mantenimiento del seed sintético de Unidades Orgánicas bajo los perfiles `test,test-reset`.

## Clasificación del grounding

### Hechos confirmados del baseline (verificados contra código)

- El esquema de persistencia ya soporta N asociaciones ordenadas por registro: `REGISTRO_UNIDAD_RESPONSABLE` conserva identidad de la unidad, denominación original, orden de presentación con unicidad por (registro, orden). La cardinalidad exactamente-una la imponen únicamente los contratos de entrada/salida y el servicio de aplicación.
- Los tres tipos de alta (iniciativa, proyecto derivado, proyecto preexistente) reciben hoy una lista `responsibleUnits` restringida a un solo elemento; la edición permite sustituir el conjunto de forma atómica y conservar el valor si el campo no se envía.
- El maestro de Unidades Orgánicas pertenece a una Unidad Ejecutora, tiene código único dentro de ella, nombre obligatorio, sigla opcional y estado activo/inactivo. El catálogo de escritura ofrece solo unidades activas de la Unidad Ejecutora del registro.
- Los eventos de auditoría de actualización registran el valor anterior y nuevo de la lista; los eventos de alta no incluyen la lista de unidades hoy.
- El seed sintético versionado carga hoy cuatro Unidades Orgánicas (dos por cada una de las dos Unidades Ejecutoras sintéticas) con código, nombre y sigla no vacíos, de forma idempotente, exclusivamente bajo los perfiles `test,test-reset`, seguido de una postvalidación de conteos y de vaciado de tablas operativas. La postvalidación actual no verifica siglas ni asociación Unidad Orgánica–Unidad Ejecutora.
- La revisión previa al confirmar un alta no muestra hoy las unidades; el detalle y la auditoría visible usan la denominación "Unidades responsables".
- La documentación funcional (guía y matriz de campos) describe el campo como una única referencia activa con posición técnica 1.

### Reglas vigentes que permanecen

- Flujo base y transiciones de estado autorizadas; condiciones de edición vigentes (iniciativa en `Presentado` y sin proyecto derivado; proyectos en `Proyecto en ejecución`).
- Campos inmutables del registro (código, tipo, origen, Unidad Ejecutora, estado, fecha de cierre).
- Control de concurrencia por versión: una edición desactualizada no sobrescribe cambios posteriores.
- Autorización por rol y ámbito (ADMINISTRADOR_PIIP sobre la Unidad Ejecutora del registro); la presentación de controles no reemplaza la validación efectiva de permisos.
- Semántica de campo ausente en edición (conserva el valor persistido), sustitución atómica del conjunto y rechazo de operaciones sin cambios efectivos.
- Auditoría append-only con actor, fecha, Unidad Ejecutora y contexto, sin cuerpos de solicitudes ni información sensible; una operación rechazada no se registra como modificación exitosa.
- Inicialización Oracle: DML externo versionado idempotente, guardias fail-closed, perfiles exactos `test,test-reset`, prohibido en producción.

### Decisiones de esta especificación

- La denominación visible del campo es "Unidades Orgánicas Involucradas" en registro, edición, revisión, detalle y auditoría visible. Los nombres técnicos internos del contrato y la persistencia vigentes (p. ej. `responsibleUnits`) se conservan; renombrarlos queda fuera de alcance. Correspondencia canónica: la denominación visible "Unidades Orgánicas Involucradas" corresponde al campo técnico `responsibleUnits` del contrato y de la persistencia vigentes; los documentos de contrato reproducen esta correspondencia.
- La lista es dinámica y ordenada: mínimo una unidad, sin máximo funcional, sin duplicados, incorporación al final, retiro de cualquier fila salvo la última restante, sin reordenamiento manual. El orden funcional es el orden de incorporación y la numeración visible es la secuencia continua 1..N recalculada al retirar filas.
- Toda modificación de la lista se confirma de forma atómica: si una fila es inválida no se modifica ninguna asociación anterior.
- Descripción y Abreviatura son datos de solo lectura provenientes del maestro institucional; no se permiten valores personalizados por registro ni completar siglas ausentes.
- Las reglas de vigencia (activa), pertenencia (misma Unidad Ejecutora del registro) y sigla aplican a las nuevas incorporaciones; la unicidad aplica al conjunto completo confirmado. Las asociaciones históricas retenidas (incluidas inactivas) permanecen como contexto y no se eliminan ni regularizan automáticamente.
- Las unidades del maestro sin sigla no se ofrecen para nuevas asociaciones; su ausencia es un problema de datos maestros que impide usar la unidad hasta su regularización.
- Las opciones ya seleccionadas en una fila no se ofrecen nuevamente en otras filas.
- Los eventos de auditoría de alta deben incluir la lista ordenada confirmada (hoy no la incluyen); los de modificación conservan valor anterior y nuevo con los mismos atributos por elemento.
- El seed sintético reutiliza los valores versionados actuales (dos unidades activas por Unidad Ejecutora sintética, con código, nombre y sigla); la adecuación se centra en extender la postvalidación (sigla no vacía, asociación correcta, mínimo dos activas por Unidad Ejecutora sintética) sin crear perfiles adicionales ni habilitar cargas automáticas.
- La guía funcional y la definición de los campos PIIP deben quedar alineadas con la nueva regla en la misma entrega de implementación.

### Contradicciones detectadas

- La feature 013 (clarificación 2026-08-22, FR-014, FR-022A) fija la cardinalidad exactamente-una con posición técnica 1: superseded por esta feature únicamente en la cardinalidad del campo, por decisión expresa del solicitante. El resto de reglas de la 013 permanece vigente.
- La guía funcional describe la edición como "se reemplaza por una única referencia activa… con un solo elemento": superseded por esta feature; el impacto documental queda exigido como requisito (FR-032).
- La matriz de campos documenta "Unidades de organización responsables" como uno de los 23 campos: el campo permanece entre los 23; cambia su regla de cardinalidad y su denominación visible, con alineación documental exigida (FR-032).
- Los eventos de auditoría de alta actuales no incluyen la lista de unidades: esta feature los extiende por requisito aprobado del solicitante; no contradice fuente vigente, se registra como evolución de contrato.
- Divergencia menor conocida entre el conteo de tablas del coordinador de reset (20) y lo declarado en la feature 015/constitución (19 + auditoría): preexistente, no bloqueante, sin acción en esta feature.

Ninguna contradicción adicional respaldada por fuentes vigentes surgió durante el grounding. Las decisiones del solicitante están aprobadas y no se reabren como NEEDS CLARIFICATION.

### Fuentes canónicas consultadas

- `specs/013-actualizar-registros-portafolio/spec.md`
- `specs/015-inicializacion-oracle/spec.md` y `apps/backend/src/main/resources/db/test/catalog-data.sql`
- `.specify/memory/constitution.md` (v1.3.0)
- Backend: `organization/persistence/OrganizationalUnitEntity.java`, `organization/persistence/OrganizationalUnitRepository.java`, `organization/api/OrganizationController.java`, `portfolio/persistence/ResponsibleUnitEntity.java`, `portfolio/persistence/ResponsibleUnitRepository.java`, `portfolio/application/ResponsibleUnitService.java`, `portfolio/application/InitiativeApplicationService.java`, `portfolio/application/ProjectApplicationService.java`, `portfolio/application/PortfolioUpdateAuditDetail.java`, `portfolio/api/PortfolioDtos.java`, `config/reset/TestResetCoordinator.java` y pruebas asociadas.
- Frontend: `core/piip.repository.ts`, `core/piip.http.repository.ts`, `core/piip.models.ts`, `core/piip-mock.repository.ts`, `pages/initiative-form/`, `pages/derived-project-form/`, `pages/preexisting-project-form/`, diálogos de revisión, `pages/portfolio-record-edit/`, `pages/initiative-detail/`, `pages/audit/`, cliente generado `api/generated/models/`.
- Documentación: `docs/architecture/piip-fields.md`, `docs/funcional/guia-funcional-piip.md`.

## Clarifications

### Session 2026-09-07

- Q: ¿Al abrir el formulario de registro de un proyecto derivado, cómo debe iniciarse la lista de Unidades Orgánicas Involucradas? → A: Precargar la lista completa de la iniciativa de origen como valor inicial editable.
- Q: ¿Cómo debe presentar el catálogo de selección a una Unidad Orgánica activa de la Unidad Ejecutora que aún no tiene sigla registrada en el maestro? → A: Ocultarla del catálogo de selección (no se ofrece como opción).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar con una o varias Unidades Orgánicas Involucradas (Priority: P1)

Como administrador PIIP, quiero registrar una iniciativa o proyecto (derivado o preexistente) relacionándolo con una lista dinámica y ordenada de Unidades Orgánicas Involucradas, para reflejar todas las unidades que participan en el registro.

**Why this priority**: Es el núcleo del requisito; sin la lista múltiple en los tres tipos de alta, la feature no entrega valor. El resto de historias depende de esta capacidad.

**Independent Test**: Se puede probar registrando cada tipo de alta con una y con varias unidades válidas y verificando que el registro confirmado conserva la lista ordenada.

**Acceptance Scenarios**:

1. **Given** el catálogo de unidades cargado y con opciones válidas, **When** confirmo un alta con una unidad válida, **Then** el registro queda asociado a esa única unidad en la posición 1.
2. **Given** dos o más unidades activas de la misma Unidad Ejecutora con sigla registrada, **When** confirmo un alta con esa lista en el orden seleccionado, **Then** el registro conserva todas las asociaciones en ese orden.
3. **Given** una unidad inactiva, de otra Unidad Ejecutora, sin sigla o ya seleccionada en otra fila, **When** intento confirmar el alta, **Then** la operación se rechaza identificando la fila y la causa, sin registrar asociaciones.
4. **Given** el catálogo cargando, vacío o con error, **When** se muestra el formulario, **Then** la interfaz informa el estado, bloquea una confirmación inválida y permite reintentar la consulta.
5. **Given** el alta confirmada, **When** reviso el registro antes de la confirmación final, **Then** la revisión previa presenta todas las unidades en el orden confirmado con Nro, Descripción y Abreviatura.
6. **Given** una iniciativa confirmada con varias unidades, **When** abro el formulario de registro de su proyecto derivado, **Then** la lista se inicia precargada con las unidades de la iniciativa en el mismo orden y permanece editable antes de confirmar.

---

### User Story 2 - Editar la lista de Unidades Orgánicas Involucradas (Priority: P2)

Como administrador PIIP, quiero editar la lista de un registro existente agregando unidades al final o retirando filas, para mantener actualizada la participación de las unidades sin perder el mínimo de una.

**Why this priority**: La edición es el segundo flujo de mayor uso; reemplaza la sustitución de un único valor y debe respetar las reglas de estados, concurrencia y atomicidad vigentes.

**Independent Test**: Se puede probar editando un registro en estado editable: agregar filas, retirar filas y verificar renumeración, mínimo de una, atomicidad y conservación del histórico cuando no se modifica la lista.

**Acceptance Scenarios**:

1. **Given** un registro editable con una unidad asociada, **When** agrego una segunda unidad válida al final y confirmo, **Then** la lista queda con dos asociaciones en el orden de incorporación.
2. **Given** una lista con varias filas, **When** retiro una fila intermedia, **Then** la lista se renumera automáticamente en la secuencia continua 1..N.
3. **Given** una lista con una sola fila, **When** se muestran los controles, **Then** el retiro de la última fila no está disponible.
4. **Given** una fila inválida en la lista editada, **When** confirmo, **Then** ninguna asociación anterior se modifica (atomicidad).
5. **Given** un registro editable, **When** edito otros campos sin modificar la lista, **Then** las asociaciones existentes se conservan íntegramente.
6. **Given** una versión desactualizada del registro, **When** confirmo la edición, **Then** el sistema rechaza la sobrescritura por control de concurrencia vigente.

---

### User Story 3 - Consultar revisión y detalle con el orden confirmado (Priority: P3)

Como usuario con permisos de consulta, quiero que la revisión previa y el detalle de iniciativas y proyectos presenten todas las Unidades Orgánicas Involucradas en el orden confirmado, para conocer la participación completa del registro.

**Why this priority**: La lectura fiel del orden confirmado es la contraparte de visibilidad del núcleo de la feature; hoy la revisión previa no muestra las unidades y el detalle solo muestra una etiqueta.

**Independent Test**: Se puede probar consultando la revisión y el detalle de un registro con varias unidades y comparando el orden presentado con el confirmado.

**Acceptance Scenarios**:

1. **Given** un registro con tres unidades confirmadas en un orden, **When** consulto su detalle, **Then** veo las tres unidades con Nro, Descripción y Abreviatura en ese mismo orden.
2. **Given** la revisión previa a la confirmación de un alta, **When** se muestra el resumen, **Then** la lista de unidades aparece completa y ordenada.
3. **Given** una pantalla pequeña, **When** consulto la lista, **Then** la misma información se presenta de forma adaptable y legible.

---

### User Story 4 - Auditar altas y modificaciones de la lista (Priority: P4)

Como auditor, quiero que las altas registren la lista ordenada confirmada y las modificaciones conserven el valor anterior y nuevo, para reconstruir la historia funcional del registro.

**Why this priority**: La trazabilidad del nuevo campo es obligatoria y debe integrarse al mecanismo de auditoría vigente sin alterar sus garantías.

**Independent Test**: Se puede probar confirmando altas y modificaciones y verificando el contenido de los eventos de auditoría.

**Acceptance Scenarios**:

1. **Given** un alta confirmada con una lista ordenada, **When** consulto su evento de auditoría, **Then** la lista aparece completa y ordenada, con unidad, nombre, sigla y Nro por elemento.
2. **Given** una modificación confirmada de la lista, **When** consulto su evento de auditoría, **Then** se conservan los valores anterior y nuevo de la lista completa.
3. **Given** una operación rechazada, **When** consulto la auditoría, **Then** no aparece registrada como modificación funcional exitosa.
4. **Given** cualquier evento auditado, **When** reviso su contenido, **Then** conserva actor, fecha, Unidad Ejecutora y contexto vigente, sin cuerpos completos de solicitudes ni información sensible.

---

### User Story 5 - Conservar los registros históricos (Priority: P5)

Como administrador PIIP, quiero que los registros históricos con varias Unidades Orgánicas sigan siendo legibles y no se alteren al editar campos ajenos a la lista, para no perder información confirmada.

**Why this priority**: La compatibilidad histórica protege datos ya confirmados; no habilita nuevas asociaciones pero exige legibilidad y no regresión.

**Independent Test**: Se puede probar con un registro histórico con varias unidades (incluida una inactiva): editar otros campos y verificar que la lista se conserva sin migraciones ni renumeraciones.

**Acceptance Scenarios**:

1. **Given** un registro histórico con varias unidades, **When** consulto su detalle, **Then** todas las asociaciones siguen legibles en su orden original.
2. **Given** una asociación histórica inactiva, **When** consulto el registro, **Then** permanece visible como contexto y no se reofrece para nuevas asociaciones.
3. **Given** un registro histórico, **When** edito otros campos sin tocar la lista, **Then** las asociaciones históricas se conservan íntegramente, sin migraciones, eliminaciones ni renumeraciones automáticas.
4. **Given** un registro histórico cuya lista se decide modificar, **When** confirmo la edición, **Then** las nuevas asociaciones cumplen vigencia, pertenencia, sigla y unicidad, y las históricas retenidas permanecen como contexto.

---

### User Story 6 - Mantener los datos sintéticos de Unidades Orgánicas (Priority: P6)

Como equipo del PIIP, quiero que la inicialización de pruebas deje disponibles Unidades Orgánicas sintéticas completas y válidas, para registrar y editar con listas de varias unidades sin depender de datos institucionales reales.

**Why this priority**: Habilita las pruebas de todas las historias anteriores; es infraestructura de datos de prueba, no funcionalidad de producto.

**Independent Test**: Se puede probar ejecutando la inicialización autorizada y verificando el conjunto resultante, su repetibilidad y el vaciado de tablas operativas.

**Acceptance Scenarios**:

1. **Given** la activación exacta y ordenada de los perfiles `test,test-reset`, **When** concluye la inicialización, **Then** cada Unidad Ejecutora sintética dispone de al menos dos Unidades Orgánicas activas con código, Descripción y Abreviatura completos.
2. **Given** una segunda ejecución autorizada, **When** concluye, **Then** produce el mismo conjunto de datos sin duplicados.
3. **Given** la inicialización concluida, **When** se verifican las tablas operativas, **Then** permanecen vacías (sin iniciativas, proyectos ni asociaciones operativas).
4. **Given** una unidad sintética sin sigla o con asociación organizacional inválida, **When** corre la postvalidación, **Then** la inicialización se declara incompleta y falla de forma segura.
5. **Given** los perfiles ordinarios de desarrollo o producción, **When** se inicia el sistema, **Then** estos datos sintéticos no se insertan automáticamente.

---

### Edge Cases

- ¿Qué ocurre si el catálogo está cargando, vacío o con error durante el registro o la edición? La interfaz informa el estado de forma diferenciada, impide una confirmación inválida y permite reintentar la consulta.
- ¿Qué ocurre si una unidad seleccionada queda inactiva entre la selección y la confirmación? La confirmación se rechaza identificando la fila y la causa, sin cambios parciales.
- ¿Qué ocurre si el maestro contiene una unidad activa sin sigla? No se ofrece para nuevas asociaciones ni se inventa o deriva una abreviatura; la unidad queda indisponible hasta la regularización del dato maestro.
- ¿Qué ocurre si la lista tiene una sola fila? El control de retiro no está disponible; el mínimo de una unidad es obligatorio para confirmar.
- ¿Qué ocurre si se confirma una lista con duplicados? Rechazo atómico con identificación de la fila duplicada; el conjunto anterior permanece intacto.
- ¿Qué ocurre si un registro histórico contiene una unidad inactiva? Permanece legible como contexto; no puede incorporarse como nueva selección.
- ¿Qué ocurre si la edición usa una versión desactualizada? El control de concurrencia vigente rechaza la sobrescritura; el usuario puede recargar y reintentar.
- ¿Qué ocurre si la edición no produce cambios efectivos? Se mantiene el rechazo vigente de operaciones sin cambios.
- ¿Qué ocurre si el seed sintético se ejecuta dos veces? La carga es repetible e idempotente: el mismo conjunto, sin duplicados.
- ¿Qué ocurre si la postvalidación detecta faltantes, duplicados, asociaciones incorrectas o siglas vacías? La inicialización se considera incompleta y falla de forma segura.

## Requirements *(mandatory)*

### Functional Requirements

**Registro y edición de la lista**

- **FR-001**: El sistema DEBE permitir confirmar, en el registro de iniciativas, de proyectos derivados y de proyectos preexistentes, una lista ordenada de una o más Unidades Orgánicas Involucradas, bajo la denominación visible "Unidades Orgánicas Involucradas".
- **FR-002**: La lista DEBE contener como mínimo una Unidad Orgánica y no DEBE tener máximo funcional de elementos.
- **FR-003**: El sistema DEBE rechazar la confirmación de una lista que contenga una misma Unidad Orgánica más de una vez.
- **FR-004**: El usuario DEBE poder agregar unidades al final de la lista y retirar cualquier fila, excepto cuando solo reste una fila.
- **FR-005**: El orden funcional DEBE corresponder al orden de incorporación; al retirar una fila, la lista DEBE renumerarse automáticamente quedando siempre en la secuencia continua 1..N.
- **FR-006**: Cada fila DEBE mostrar exclusivamente: Nro (número autonumérico derivado de la posición 1..N), Descripción (nombre de la Unidad Orgánica del maestro institucional) y Abreviatura (sigla de la Unidad Orgánica del maestro institucional), todos de solo lectura.
- **FR-007**: Toda modificación de la lista DEBE confirmarse de manera atómica: si una fila es inválida, no se modifica ninguna asociación anterior.
- **FR-008**: La edición de iniciativas y proyectos DEBE permitir agregar y retirar unidades respetando el mínimo de una, bajo los estados editables y el control de concurrencia vigentes, sin alterar estados, transiciones, documentos, tareas ni notificaciones.
- **FR-009**: Editar otros campos sin modificar la lista DEBE conservar íntegramente las asociaciones existentes del registro.
- **FR-033**: El registro de un proyecto derivado DEBE iniciar la lista con las Unidades Orgánicas Involucradas confirmadas en la iniciativa de origen, como valor inicial editable: el usuario puede retirar o agregar unidades antes de confirmar, sujetas a las reglas de validez vigentes.

**Reglas de validez**

- **FR-010**: Solo DEBEN poder incorporarse como nuevas asociaciones Unidades Orgánicas activas que pertenezcan a la misma Unidad Ejecutora del registro.
- **FR-011**: Toda Unidad Orgánica disponible para nuevas asociaciones DEBE tener una sigla registrada en el maestro; el sistema NO DEBE inventar, derivar ni completar automáticamente una abreviatura. La ausencia de sigla DEBE tratarse como problema de datos maestros que impide usar esa unidad hasta su regularización.
- **FR-012**: El catálogo de opciones para nuevas asociaciones DEBE ofrecer únicamente unidades activas de la Unidad Ejecutora del registro con sigla no vacía, y NO DEBE reofrecer las opciones ya seleccionadas en otras filas. Esta exclusión es una primera barrera y no sustituye la validación del servicio, que DEBE rechazar toda nueva incorporación sin sigla conforme a FR-011 (defensa en profundidad).
- **FR-013**: Si el catálogo está cargando, vacío o presenta error, la interfaz DEBE informarlo claramente, impedir una confirmación inválida y permitir reintentar la consulta.

**Revisión, detalle y presentación**

- **FR-014**: La revisión previa a la confirmación y el detalle de iniciativas y proyectos DEBEN presentar todas las Unidades Orgánicas Involucradas en el orden confirmado durante el registro o edición.
- **FR-015**: En escritorio, la presentación DEBE usar columnas Nro, Descripción y Abreviatura; en pantallas pequeñas DEBE conservar la misma información mediante una presentación adaptable y legible.
- **FR-016**: Los controles para agregar, seleccionar y eliminar DEBEN ser operables por completo mediante teclado; cada fila y control DEBE exponer etiqueta, rol y estado a las tecnologías de asistencia, y el foco DEBE gestionarse de forma predecible al agregar o retirar filas.
- **FR-017**: Los errores DEBEN identificar la fila afectada y explicar la causa, sin perder las selecciones válidas ingresadas por el usuario.

**Auditoría**

- **FR-018**: Las altas DEBEN registrar en auditoría la lista ordenada de Unidades Orgánicas confirmadas.
- **FR-019**: Las modificaciones DEBEN conservar en auditoría los valores anterior y nuevo de la lista.
- **FR-020**: Cada elemento auditado DEBE identificar la unidad, su nombre, su sigla y su Nro de presentación.
- **FR-021**: La auditoría DEBE conservar actor, fecha, Unidad Ejecutora y contexto vigente, sin almacenar cuerpos completos de solicitudes ni información sensible. Una operación rechazada NO DEBE registrarse como modificación funcional exitosa.

**Compatibilidad histórica**

- **FR-022**: Los registros históricos con varias Unidades Orgánicas DEBEN continuar siendo legibles, y una asociación histórica inactiva DEBE permanecer visible como contexto.
- **FR-023**: El sistema NO DEBE realizar migraciones, eliminaciones, renumeraciones ni saneamientos automáticos de información histórica.
- **FR-024**: Si el usuario modifica la lista de un registro histórico, las nuevas asociaciones confirmadas DEBEN cumplir las reglas de vigencia, pertenencia, sigla y unicidad; las asociaciones históricas retenidas permanecen como contexto.

**Autorización y consistencia**

- **FR-025**: El sistema DEBE conservar los roles y ámbitos de autorización vigentes; la presentación visual de controles NO DEBE reemplazar la validación efectiva de permisos.
- **FR-026**: El sistema DEBE conservar el control de concurrencia vigente para evitar que una edición desactualizada sobrescriba cambios posteriores.

**Datos sintéticos de Unidades Orgánicas**

- **FR-027**: El sistema DEBE mantener y adecuar el conjunto de datos sintéticos de Unidades Orgánicas del procedimiento controlado existente, reutilizando los valores sintéticos actualmente versionados: cuatro Unidades Orgánicas sintéticas, al menos dos activas por cada una de las dos Unidades Ejecutoras sintéticas, cada una con código, nombre (usado como Descripción) y sigla (usada como Abreviatura) no vacíos, y código único dentro de su Unidad Ejecutora.
- **FR-028**: Los datos sintéticos DEBEN cargarse exclusivamente mediante la activación exacta y ordenada de los perfiles `test,test-reset`, sin crear perfiles adicionales ni habilitar carga automática en los entornos ordinarios de desarrollo o producción.
- **FR-029**: La carga DEBE ser repetible: una segunda ejecución autorizada DEBE producir el mismo conjunto esperado, sin duplicar Unidades Orgánicas.
- **FR-030**: La validación posterior a la carga DEBE detectar datos faltantes, duplicados, asociaciones organizacionales incorrectas o siglas vacías y considerar incompleta la inicialización (fallo seguro), sin insertar iniciativas, proyectos ni asociaciones operativas: las tablas operativas DEBEN permanecer vacías tras la inicialización.
- **FR-031**: Los datos sintéticos son exclusivamente sintéticos y NO DEBEN confundirse con información institucional oficial, ni usarse como mecanismo de carga o migración de producción.

**Documentación funcional**

- **FR-032**: La guía funcional y la definición de los campos PIIP DEBEN quedar alineadas con la nueva regla de cardinalidad y la denominación "Unidades Orgánicas Involucradas" en la misma entrega de implementación.

### Key Entities *(include if feature involves data)*

- **Unidad Orgánica (maestro institucional)**: identidad, código, nombre (mostrado como Descripción), sigla (mostrada como Abreviatura), Unidad Ejecutora de pertenencia y estado activo/inactivo. Fuente única de Descripción y Abreviatura.
- **Unidades Orgánicas Involucradas**: asociación ordenada 1..N entre un registro del portafolio y Unidades Orgánicas; cada elemento conserva la posición de presentación (Nro), la unidad, su denominación y su abreviatura confirmadas. Mínimo un elemento; sin duplicados; orden igual al de incorporación.
- **Registro del portafolio**: iniciativa, proyecto derivado o proyecto preexistente; pertenece a una Unidad Ejecutora y mantiene versión para control de concurrencia.
- **Unidad Ejecutora**: ámbito organizacional del registro; toda unidad incorporada debe pertenecer a la misma.
- **Evento de auditoría de registro/actualización**: evidencia con actor, fecha, Unidad Ejecutora y contexto; incluye la lista ordenada de unidades (valor confirmado en altas; anterior y nuevo en modificaciones).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los tres tipos de alta permiten confirmar una o varias Unidades Orgánicas válidas; ninguna confirmación prospera con lista vacía, duplicada, inactiva, de otra Unidad Ejecutora o con unidad sin sigla (100% de los intentos inválidos rechazados).
- **SC-002**: Después de cada eliminación de una fila, los números visibles forman exactamente la secuencia continua 1..N en el 100% de los casos.
- **SC-003**: Una fila inválida no produce cambios parciales: 0 asociaciones modificadas cuando la confirmación se rechaza.
- **SC-004**: Revisión, detalle y auditoría presentan el 100% de los elementos de la lista en el orden confirmado.
- **SC-005**: Los registros históricos permanecen legibles y sin modificación al editar campos ajenos a la lista (100% de las ediciones sin cambio de lista conservan las asociaciones previas).
- **SC-006**: Tras una ejecución autorizada de `test,test-reset`, cada Unidad Ejecutora sintética dispone de al menos dos Unidades Orgánicas activas con código, Descripción y Abreviatura completos; una segunda ejecución produce el mismo conjunto sin duplicados; las tablas operativas permanecen con 0 filas.
- **SC-007**: Con los datos sintéticos cargados, un usuario puede completar el registro y la edición de una iniciativa o proyecto con dos Unidades Orgánicas distintas de la misma Unidad Ejecutora en una sola sesión, sin errores de datos maestros.
- **SC-008**: La guía funcional y la definición de los campos PIIP quedan alineadas: el 100% de las menciones vigentes de la regla de cardinalidad exactamente-una del campo se actualizan a la nueva regla y denominación.

## Assumptions

- La denominación visible "Unidades Orgánicas Involucradas" reemplaza a "Unidad Orgánica responsable" en las interfaces de usuario (registro, edición, revisión, detalle y auditoría visible); los nombres técnicos del contrato y la persistencia vigentes se conservan sin renombrar.
- El catálogo de opciones para nuevas asociaciones se obtiene del maestro institucional existente (unidades activas de la Unidad Ejecutora del registro), filtrando además las unidades sin sigla.
- La validación de vigencia, pertenencia y sigla aplica a las nuevas incorporaciones; la unicidad aplica al conjunto completo confirmado; las asociaciones históricas retenidas (incluidas inactivas o sin sigla) se conservan como contexto.
- El orden confirmado se persiste con la lista y toda presentación posterior (revisión, detalle, auditoría) deriva de él; no existe orden derivable alternativo.
- El seed sintético vigente ya cumple código, nombre y sigla no vacíos, y se reutiliza sin reemplazo de valores salvo decisión funcional posterior explícita.
- La sincronización del cliente del frontend sigue el flujo vigente del monorepo: el contrato del backend se publica primero y el cliente se sincroniza después.
- Los usuarios operan con conectividad estable y navegadores soportados por la aplicación vigente; no se introducen requisitos nuevos de entorno.

## Out of Scope

- Crear o modificar el CRUD del maestro de Unidades Orgánicas.
- Permitir Descripción o Abreviatura personalizadas por registro.
- Reordenamiento manual de filas.
- Establecer un máximo funcional de elementos.
- Inventar, derivar o completar siglas para datos maestros incompletos.
- Cambiar roles, ámbitos, estados o transiciones.
- Alterar funcionalidades documentales, tareas o notificaciones no necesarias para este requisito.
- Realizar migraciones, saneamientos o renumeraciones automáticas de información histórica.
- Renombrar tablas, entidades o propiedades técnicas internas del contrato y la persistencia vigentes.
- Cargar datos institucionales reales o usar el seed sintético como mecanismo de carga o migración de producción.
- Crear iniciativas o proyectos mock durante la inicialización.
- Activar automáticamente el perfil destructivo o crear perfiles adicionales de carga.
