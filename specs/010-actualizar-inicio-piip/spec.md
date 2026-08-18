# Feature Specification: Actualización de Inicio PIIP

**Feature Branch**: `main` (el hook de creación de rama está deshabilitado)

**Created**: 2026-08-18

**Status**: Draft

**Input**: User description: "Actualizar funcionalmente la página de Inicio tomando el mockup aprobado como referencia de composición, mostrando registros reales del portafolio de la Unidad Ejecutora activa y notificaciones personales, sin información ficticia ni reglas nuevas."

## Clarifications

### Session 2026-08-18

- Q: Cuando `Tipo` sea `Todos`, ¿cómo deben ordenarse iniciativas y proyectos? → A: Un único orden global por fecha de actualización descendente.
- Q: Al buscar o filtrar registros, ¿qué deben representar los indicadores y la distribución por estado? → A: Aplicar todos los filtros, incluida la búsqueda, y coincidir con el listado filtrado.
- Q: ¿Qué estados deben aparecer como indicadores y barras? → A: Solo estados presentes en el resultado filtrado, con conteo mayor que cero.
- Q: ¿Qué debe ocurrir al activar `Ver todas` en `Mis notificaciones`? → A: Expandir dentro de Inicio la lista completa de notificaciones.
- Q: ¿Cómo debe marcar el usuario una notificación como leída? → A: Mediante el botón `Marcar como leída` de cada notificación no leída.
- Q: ¿Cuántas notificaciones debe mostrar inicialmente el resumen compacto? → A: Las 3 notificaciones más recientes.
- Q: ¿Qué debe ocurrir al activar la campana de notificaciones? → A: Ir a Inicio y enfocar `Mis notificaciones`, sin marcar avisos.
- Q: ¿Qué debe ocurrir si el estado seleccionado deja de ser válido al cambiar el tipo? → A: Restablecer `Estado: Todos`.
- Q: ¿Cuántos registros debe mostrar cada página del listado de Inicio? → A: 5 registros por página.

## Clasificación de la definición

### Hechos confirmados por el repositorio

- La persona autenticada trabaja con una Unidad Ejecutora activa dentro de sus ámbitos autorizados.
- Los listados vigentes de iniciativas y proyectos admiten búsqueda, filtro por estado, filtro por Unidad Ejecutora y paginación; sus respuestas incluyen código, nombre, tipo, estado y Unidad Ejecutora.
- Existen las rutas de consulta y detalle para iniciativas y proyectos, así como los módulos completos `/iniciativas` y `/proyectos`.
- El catálogo vigente incluye, entre otros estados reales, `Presentado`, `Iniciativa aprobada`, `Iniciativa archivada` y `Proyecto en ejecución`. También contiene estados adicionales autorizados por la feature 009; el mockup no limita el catálogo a cuatro estados.
- Las notificaciones pertenecen a un destinatario individual y exponen tipo, mensaje, fecha de creación y estado leído/no leído.
- La lectura explícita de una notificación se conserva mediante la operación vigente `PUT /notifications/{id}/read`.
- El contrato de notificaciones no entrega actualmente el código, tipo de registro ni otra referencia contextual confiable para construir un enlace hacia una iniciativa o proyecto.
- El resumen vigente de Inicio agrega registros de todas las Unidades Ejecutoras cubiertas por el usuario; no recibe el identificador de la Unidad Ejecutora activa.
- La campana vigente abre el último aviso y marca automáticamente como leída una notificación no leída. Este comportamiento contradice el requisito de lectura explícita de esta feature.

### Supuestos funcionales adoptados

- La búsqueda visible de Inicio se limita conceptualmente a código o nombre, aunque la capacidad de consulta existente pueda admitir criterios adicionales en otros módulos.
- El filtro `Tipo: Todos` combina iniciativas y proyectos en una única lista, ordenada globalmente por fecha de actualización descendente, sin convertirlos en una tercera clase de registro.
- `Ver todas` expande la lista completa de notificaciones del usuario dentro del bloque de Inicio; no navega, abre un diálogo o panel lateral ni crea una ruta o módulo nuevo.
- El resumen compacto muestra inicialmente las tres notificaciones más recientes; esto no cambia el orden descendente por fecha, el total disponible ni la posibilidad de consultar todas.
- Los indicadores y la distribución por estado aplican la búsqueda, el tipo y el estado del listado; sus conteos corresponden al conjunto filtrado completo, no solo a la página visible.
- Los indicadores y la distribución omiten categorías con conteo cero y muestran toda categoría canónica que sí esté presente en el resultado filtrado.
- El listado de Inicio muestra cinco registros por página; los datos ilustrativos del mockup no modifican ese tamaño.
- Un resultado sin coincidencias por filtros se comunica como `sin resultados`; el estado vacío del portafolio se reserva para la ausencia real de registros en la Unidad Ejecutora activa.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Comprender el portafolio de la Unidad Ejecutora activa (Priority: P1)

Como persona autenticada, quiero ver en Inicio las iniciativas y proyectos reales de la Unidad Ejecutora activa para comprender inmediatamente qué registros forman mi portafolio consultable y en qué estado están.

**Why this priority**: Evita mezclar ámbitos autorizados distintos y elimina métricas o ejemplos que no correspondan al contexto de trabajo seleccionado.

**Independent Test**: Se selecciona una Unidad Ejecutora con registros conocidos y se verifica que el encabezado, listado, total, indicadores y distribución representen exclusivamente esos mismos registros.

**Acceptance Scenarios**:

1. **Given** una persona con acceso a más de una Unidad Ejecutora y una de ellas activa, **When** abre Inicio, **Then** identifica inequívocamente la Unidad Ejecutora activa y ve únicamente registros autorizados de esa unidad.
2. **Given** registros reales de iniciativa y proyecto en la unidad activa, **When** se presenta el listado, **Then** cada fila contiene código, nombre, tipo, estado actual, Unidad Ejecutora y la acción `Ver detalle`.
3. **Given** registros distribuidos en varios estados reales, **When** se muestran el total, los indicadores y la distribución, **Then** sus conteos provienen del mismo conjunto de registros y la suma agrupada por estado coincide con el total.
4. **Given** un registro visible, **When** la persona activa `Ver detalle`, **Then** accede al detalle correspondiente según sea iniciativa o proyecto.
5. **Given** que la Unidad Ejecutora activa no tiene registros, **When** termina la consulta, **Then** se muestra un único estado vacío y no se muestran filas ni indicadores contradictorios.

---

### User Story 2 - Encontrar registros sin perder la coherencia del resumen (Priority: P2)

Como persona autorizada, quiero buscar y filtrar los registros de Inicio para encontrar rápidamente una iniciativa o proyecto sin que el listado, los indicadores y el gráfico representen conjuntos diferentes.

**Why this priority**: La consulta solo es confiable si todos los componentes conservan el mismo alcance, filtros y total.

**Independent Test**: Se aplican combinaciones de búsqueda por código o nombre, tipo y estado sobre una Unidad Ejecutora con más de cinco registros coincidentes, y se reconcilian todas las vistas y páginas con el total filtrado.

**Acceptance Scenarios**:

1. **Given** registros con códigos y nombres distintos, **When** la persona busca por un código o fragmento de nombre existente, **Then** el listado, el total, los indicadores y la distribución muestran las coincidencias dentro de la Unidad Ejecutora activa.
2. **Given** iniciativas y proyectos en la unidad activa, **When** se selecciona `Todos`, `Iniciativa` o `Proyecto`, **Then** solo se consideran los tipos solicitados y se conservan categorías de estado válidas para esos tipos.
3. **Given** varios estados vigentes, **When** se aplica un filtro por estado, **Then** la opción corresponde al catálogo real y listado, indicadores, distribución y total reflejan la misma selección.
4. **Given** más de cinco registros coincidentes, **When** la persona cambia de página, **Then** cada página muestra hasta cinco registros y el total, el orden global por actualización descendente y el alcance de Unidad Ejecutora y filtros permanecen estables.
5. **Given** filtros sin coincidencias pero con registros existentes en la Unidad Ejecutora activa, **When** termina la consulta, **Then** se muestra un mensaje de `sin resultados` distinto del estado vacío del portafolio.
6. **Given** la página Inicio, **When** la persona activa `Ver iniciativas` o `Ver proyectos`, **Then** navega al módulo completo correspondiente sin sustituir sus listados.
7. **Given** un estado seleccionado que no es válido para el nuevo tipo, **When** la persona cambia el filtro de tipo, **Then** el filtro vuelve a `Estado: Todos`, se reinicia la página y todos los componentes se recalculan sin presentar una combinación inválida.

---

### User Story 3 - Consultar notificaciones personales y decidir cuándo leerlas (Priority: P2)

Como persona autenticada, quiero consultar mis notificaciones personales desde Inicio y marcar una como leída solo mediante una acción explícita, para conservar el control del estado de mis avisos.

**Why this priority**: Las notificaciones tienen destinatario individual y su lectura no debe inferirse de que el contenido fue renderizado o abierto.

**Independent Test**: Se cargan notificaciones leídas y no leídas del usuario autenticado, se alternan las pestañas y se comprueba que únicamente la acción explícita de lectura cambia el estado y el contador.

**Acceptance Scenarios**:

1. **Given** notificaciones dirigidas al usuario autenticado, **When** abre Inicio, **Then** ve el bloque `Mis notificaciones`, el texto `Avisos dirigidos a este usuario` y, por aviso, mensaje, tipo, fecha de creación y estado leído/no leído.
2. **Given** avisos leídos y no leídos, **When** alterna entre `Todas` y `No leídas`, **Then** cada pestaña presenta el subconjunto coherente y el contador de la campana coincide con la cantidad no leída.
3. **Given** una notificación no leída, **When** la página la renderiza, la persona abre la campana, cambia de pestaña o usa `Ver todas`, **Then** la notificación permanece no leída.
4. **Given** una notificación no leída, **When** la persona activa su botón `Marcar como leída`, **Then** se conserva la operación de lectura existente, se actualiza ese aviso y disminuye el contador exactamente en una unidad.
5. **Given** una notificación sin referencia contextual en el contrato, **When** se muestra en Inicio, **Then** no se ofrece un enlace inventado hacia iniciativa o proyecto.
6. **Given** que no existen notificaciones para el usuario, **When** abre Inicio, **Then** se muestra un estado informativo del bloque sin afectar el estado del portafolio.
7. **Given** más de tres notificaciones disponibles, **When** se muestra el resumen compacto, **Then** presenta las tres más recientes y, al activar `Ver todas`, el mismo bloque de Inicio se expande para mostrar la lista completa sin cambiar de página ni modificar estados de lectura.
8. **Given** que la persona se encuentra en cualquier pantalla autenticada, **When** activa la campana, **Then** llega a Inicio y el foco se coloca en `Mis notificaciones` sin cambiar el estado de lectura de ningún aviso.

### Edge Cases

- La persona cambia la Unidad Ejecutora activa mientras hay una consulta o página anterior: el portafolio visible se reinicia y se vuelve a calcular para la nueva unidad; las notificaciones personales no cambian de destinatario.
- Una página deja de existir después de aplicar filtros o cambiar de Unidad Ejecutora: se muestra la primera página válida sin alterar el total.
- Un estado real tiene conteo cero en el resultado filtrado: no aparece como indicador ni como barra y no se crea una etiqueta alternativa.
- El conjunto contiene estados reales adicionales a los cuatro ilustrados en el mockup: se muestran con su etiqueta canónica y se incluyen en la reconciliación del total.
- Una iniciativa y un proyecto comparten texto parcial en nombre o código: ambos aparecen cuando `Tipo` es `Todos` y cada uno conserva su tipo y ruta de detalle.
- La lista de notificaciones cambia entre la carga y una lectura explícita: el contador se reconcilia con la respuesta vigente y no se vuelve negativo ni marca otros avisos.
- La lectura explícita de una notificación ya leída no altera el estado de otras notificaciones.
- Un error al consultar portafolio o notificaciones no se representa como estado vacío; se comunica como error recuperable para evitar afirmar que no existen datos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Inicio DEBE identificar visual y textualmente la Unidad Ejecutora activa y declarar que el portafolio mostrado se limita a esa unidad y al alcance autorizado.
- **FR-002**: El conjunto base de portafolio DEBE contener exclusivamente iniciativas y proyectos registrados, autorizados y pertenecientes a la Unidad Ejecutora activa.
- **FR-003**: Cada registro visible DEBE mostrar únicamente código, nombre, tipo (`Iniciativa` o `Proyecto`), estado actual, Unidad Ejecutora y la acción `Ver detalle` como datos principales del listado.
- **FR-004**: `Ver detalle` DEBE conducir al detalle real del tipo de registro correspondiente.
- **FR-005**: Inicio DEBE ofrecer búsqueda por código o nombre utilizando capacidades respaldadas por los contratos vigentes.
- **FR-006**: Inicio DEBE ofrecer el filtro de tipo con exactamente `Todos`, `Iniciativa` y `Proyecto`.
- **FR-007**: Inicio DEBE ofrecer un filtro por estado formado únicamente por etiquetas canónicas válidas para los tipos incluidos en la consulta; si un cambio de tipo invalida el estado seleccionado, DEBE restablecer `Estado: Todos` y reiniciar la página.
- **FR-008**: La paginación DEBE mostrar hasta cinco registros por página, conservar el total, la Unidad Ejecutora activa y todos los filtros, activarse cuando el resultado exceda cinco registros y, para `Tipo: Todos`, operar sobre una única secuencia global ordenada por fecha de actualización descendente.
- **FR-009**: El listado, su total, los indicadores y la distribución por estado DEBEN derivarse del mismo conjunto base y aplicar conjuntamente la búsqueda, el tipo y el estado; los conteos DEBEN representar todo el resultado filtrado y no solo la página visible.
- **FR-010**: La suma de los conteos agrupados por estado DEBE coincidir con el total del conjunto filtrado.
- **FR-011**: Los indicadores y la distribución DEBEN utilizar las mismas categorías y etiquetas canónicas, incluir únicamente estados con conteo mayor que cero en el resultado filtrado y NO DEBEN renombrar un estado de modo que cambie su significado.
- **FR-012**: La representación DEBE reconocer como estados reales `Presentado`, `Iniciativa aprobada`, `Iniciativa archivada` y `Proyecto en ejecución`, sin limitarse a ellos cuando existan otros estados vigentes aplicables.
- **FR-013**: El estado vacío del portafolio DEBE aparecer únicamente cuando la Unidad Ejecutora activa no tenga registros y NO DEBE coexistir con filas, conteos positivos o gráficos con datos.
- **FR-014**: Cuando existan registros en la unidad pero ningún resultado coincida con los filtros, Inicio DEBE mostrar un mensaje de `sin resultados` distinto del estado vacío del portafolio.
- **FR-015**: Inicio DEBE conservar accesos `Ver iniciativas` y `Ver proyectos` hacia los módulos completos existentes.
- **FR-016**: Inicio NO DEBE mostrar métricas de tareas, prioridades, vencimientos, avance físico ni acciones operativas que no formen parte del portafolio definido para esta feature.
- **FR-017**: La campana superior DEBE conservarse, mostrar un contador numérico de notificaciones no leídas del usuario autenticado y, al activarse, llevar a Inicio y colocar el foco en `Mis notificaciones` sin cambiar estados de lectura.
- **FR-018**: Inicio DEBE mostrar un resumen separado titulado `Mis notificaciones` y el indicador `Avisos dirigidos a este usuario`.
- **FR-019**: Cada notificación visible DEBE mostrar mensaje, tipo, fecha de creación y estado leído/no leído, exactamente a partir de los datos disponibles para su destinatario.
- **FR-020**: El bloque DEBE ofrecer las pestañas `Todas` y `No leídas`, y sus resultados y conteos DEBEN ser coherentes entre sí y con la campana.
- **FR-021**: El resumen compacto DEBE mostrar las tres notificaciones más recientes y la acción `Ver todas` DEBE expandir dentro del mismo bloque de Inicio la lista completa disponible del usuario, sin navegación, diálogo o panel lateral y sin cambiar automáticamente el estado de lectura.
- **FR-022**: Renderizar, visualizar, abrir la campana, cambiar de pestaña o usar `Ver todas` NO DEBE marcar una notificación como leída.
- **FR-023**: Cada notificación no leída DEBE ofrecer su propio botón `Marcar como leída`; activarlo DEBE conservar la operación vigente `PUT /notifications/{id}/read`, sin confirmación adicional ni acción masiva.
- **FR-024**: Una lectura exitosa DEBE actualizar el aviso y el contador sin completar tareas, eliminar avisos ni modificar otros destinatarios.
- **FR-025**: Inicio NO DEBE crear enlaces a iniciativas o proyectos mientras el contrato de notificaciones no entregue una referencia contextual confiable.
- **FR-026**: El alcance del portafolio DEBE depender de autorización y Unidad Ejecutora activa; el alcance de notificaciones DEBE depender exclusivamente del destinatario individual autenticado.
- **FR-027**: Cambiar la Unidad Ejecutora activa DEBE actualizar listado, total, indicadores y distribución del portafolio, pero NO DEBE convertir las notificaciones en avisos de la unidad ni alterar su destinatario.
- **FR-028**: La feature NO DEBE modificar el modelo de negocio de tareas, prioridades, vencimientos, disparadores de notificación ni reglas actuales de destinatarios.
- **FR-029**: El mockup DEBE utilizarse solo como referencia de composición y experiencia; nombres, códigos, cantidades y registros ilustrativos NO DEBEN persistirse ni mostrarse como datos reales.
- **FR-030**: Los errores de carga DEBEN diferenciarse de la ausencia real de datos y ofrecer una forma de reintentar sin presentar información ficticia.

### Key Entities *(include if feature involves data)*

- **Contexto de Unidad Ejecutora activa**: Unidad seleccionada por la persona dentro de su ámbito autorizado; limita el conjunto de portafolio mostrado en Inicio.
- **Registro de portafolio**: Iniciativa o proyecto real con código, nombre, tipo, estado actual y Unidad Ejecutora, además de una ruta de detalle según su tipo.
- **Consulta de Inicio**: Selección compuesta por búsqueda, tipo, estado y página; determina un único conjunto reconciliable para listado, total, indicadores y distribución.
- **Agrupación por estado**: Conteo de los registros de la consulta por etiqueta canónica; su suma equivale al total filtrado.
- **Notificación personal**: Aviso persistente dirigido a una persona, con identificador, tipo, mensaje, fecha de creación y estado de lectura.
- **Estado de lectura**: Condición leída/no leída de una notificación que solo cambia por acción explícita del destinatario.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100 % de los casos de aceptación, todos los registros, indicadores y barras de distribución de Inicio pertenecen a la Unidad Ejecutora activa y al alcance autorizado.
- **SC-002**: Para toda combinación de búsqueda, tipo, estado y página, la suma de registros agrupados por estado coincide exactamente con el total filtrado mostrado.
- **SC-003**: El 100 % de las filas visibles contiene los seis elementos definidos: código, nombre, tipo, estado actual, Unidad Ejecutora y acción `Ver detalle`.
- **SC-004**: El 100 % de las etiquetas de estado mostradas coincide con el catálogo vigente, corresponde a un conteo positivo del resultado filtrado y ninguna etiqueta ilustrativa o inventada aparece en los resultados.
- **SC-005**: El estado vacío del portafolio aparece en el 100 % de las consultas base sin registros y en el 0 % de las consultas que sí contienen registros de la Unidad Ejecutora activa.
- **SC-006**: El 100 % de las navegaciones `Ver detalle`, `Ver iniciativas` y `Ver proyectos` llega al contexto real correspondiente sin crear rutas ficticias.
- **SC-007**: El contador de la campana coincide exactamente con el número de notificaciones no leídas del usuario autenticado después de cargar y después de cada lectura explícita exitosa.
- **SC-008**: El 100 % de las notificaciones conserva su estado al ser renderizada, visualizada, filtrada o incluida mediante `Ver todas`; solo el botón `Marcar como leída` del aviso puede cambiarlo.
- **SC-009**: El 100 % de las notificaciones mostradas pertenece al usuario autenticado y contiene mensaje, tipo, fecha y estado de lectura sin enlaces contextuales no respaldados.
- **SC-010**: Ningún escenario de aceptación introduce o modifica tareas, prioridades, vencimientos, avance físico, disparadores ni reglas de destinatarios.

## Dependencies and Constraints

- Depende de los contratos vigentes de consulta paginada de iniciativas y proyectos, del catálogo de estados, de las rutas de detalle y de la consulta y lectura de notificaciones.
- La solución posterior deberá resolver la discrepancia confirmada entre el alcance agregado actual del resumen y la Unidad Ejecutora activa, sin ampliar autorizaciones.
- La guía funcional de `docs/` tiene impacto porque cambia el propósito y recorrido de Inicio; deberá actualizarse con evidencia en la misma entrega de implementación.
- La especificación no autoriza generación de contratos, pruebas, compilación, servidores, integración Oracle ni implementación.

## Out of Scope

- Modificar el modelo de tareas o volver a presentar la bandeja de trabajo y alertas como parte de este Inicio.
- Agregar prioridades, vencimientos, avance físico o acciones operativas.
- Crear nuevos disparadores de notificaciones o cambiar quién recibe cada aviso.
- Agregar referencias de portafolio al contrato de notificaciones o inventar enlaces a registros.
- Cambiar reglas, estados o transiciones del ciclo de vida del portafolio.
- Sustituir o recortar los listados completos de los módulos Iniciativas y Proyectos.
- Copiar nombres, códigos, cantidades o registros ilustrativos del mockup.
- Implementar frontend, backend, contratos o documentación funcional antes de la revisión y aprobación de esta especificación y de los artefactos posteriores de diseño y tareas.
