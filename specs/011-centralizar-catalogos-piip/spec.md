# Feature Specification: Centralización de Catálogos PIIP

**Feature Branch**: `main` (el hook de creación de rama está deshabilitado)

**Created**: 2026-08-20

**Status**: Draft

**Input**: User description: "Centralizar y normalizar los catálogos utilizados por PIIP en el registro, consulta y gestión de iniciativas, proyectos y documentos, convirtiendo al backend en fuente de verdad, usando referencias persistentes y conservando el proceso documental y el modelo organizacional vigentes."

## Clasificación de la definición

### Hechos confirmados por el repositorio

- `Tipo de solución` y `Fuente u origen` se representan actualmente mediante enums y se almacenan como texto en `REGISTRO_PORTAFOLIO`.
- `Objetivo PEI` y `Actividad POI` se almacenan actualmente como textos libres opcionales en `REGISTRO_PORTAFOLIO`.
- `Tipo documental` se representa mediante un enum de seis valores y se almacena como texto en `DOCUMENTO`.
- La consulta general de catálogos devuelve listas de etiquetas, sin un contrato uniforme de identificador, código, nombre, orden y estado activo.
- El frontend mantiene listas funcionales duplicadas para tipo de registro, tipo de solución, fuente u origen y tipo documental; también conserva el fallback `RESPONSIBLE_UNITS`.
- `UNIDAD_ORGANICA` ya pertenece a una Unidad Ejecutora, tiene código y estado activo, y dispone de consulta por Unidad Ejecutora.
- `REGISTRO_UNIDAD_RESPONSABLE` ya asocia un registro con `ID_UNIDAD_ORGANICA`; no existe evidencia que justifique introducir `PROYECTO_UNIDAD_ORGANICA`.
- La creación vigente valida que una Unidad Orgánica seleccionada pertenezca a la Unidad Ejecutora del registro, aunque todavía admite una designación sin referencia organizacional.
- El flujo documental vigente conserva una posición única por registro y tipo, versiones, contenido, publicación externa, estado `No aplica`, motivo y auditoría.

### Decisiones funcionales aportadas por la solicitud

- Las siete fuentes de selección centralizadas son: Tipo de solución, Fuente u origen, Objetivo PEI, Actividad POI, Tipo documental, Unidad Orgánica responsable y Tipo de registro.
- Tipo de solución, Fuente u origen, Objetivo PEI y Actividad POI serán catálogos persistentes independientes bajo `CATALOGO` y `CATALOGO_ITEM`.
- Tipo documental será un catálogo persistente independiente bajo `TIPO_DOCUMENTO`, sin `APLICA_A`.
- Tipo de registro seguirá siendo un catálogo técnico de solo lectura basado en sus dos valores vigentes y no tendrá tabla administrable.
- Objetivo PEI y Actividad POI serán independientes; ninguna selección filtrará, habilitará, obligará ni condicionará la otra.
- El frontend conservará y enviará identificadores recibidos del backend para toda referencia persistente; no utilizará códigos ni números hardcodeados como identificadores de escritura.
- Los datos históricos de prueba relacionados con iniciativas, proyectos y documentos se eliminarán mediante un reinicio exclusivo del ambiente de pruebas; no se intentará convertir sus textos anteriores.
- Las entidades persistentes serán la única definición estructural permanente; el archivo SQL externo contendrá exclusivamente datos iniciales.

### Supuestos funcionales adoptados

- Esta feature adapta las operaciones existentes y no crea una capacidad general de edición de iniciativas o proyectos; cualquier actualización incorporada por una feature futura deberá usar las identidades y validaciones aquí definidas.
- Para Tipo de registro, el contrato de lectura entrega código, nombre, orden y estado activo; al no ser persistente, no requiere un identificador de base de datos.
- Las opciones `Todos` y equivalentes pertenecen exclusivamente a cada filtro del frontend y nunca se exponen ni almacenan como elementos de catálogo.
- El orden visible será determinista y respetará el orden de presentación cuando el catálogo lo defina; los empates se resolverán sin alterar el significado ni la identidad de los elementos.
- Los valores sintéticos PEI, POI y organizacionales se restringen al ambiente de pruebas. Su condición se documenta únicamente en el seed y la documentación técnica; no se agrega una marca visible ni un campo adicional al contrato.

## Clarifications

### Session 2026-08-20

- Q: ¿Esta feature debe crear una capacidad nueva de edición general de iniciativas y proyectos o solo adaptar las operaciones existentes? → A: Adaptar únicamente las operaciones existentes; no crear pantallas ni endpoints generales de edición y exigir IDs a cualquier actualización futura.
- Q: ¿Los filtros de consulta deben incluir ítems inactivos para localizar registros históricos? → A: No; los filtros muestran únicamente ítems activos, mientras los valores inactivos continúan resolviéndose en los resultados y detalles históricos.
- Q: ¿Qué debe ocurrir si el perfil de reinicio falla después de iniciar el proceso? → A: Detenerse en el primer error, informar la etapa fallida y permitir una nueva ejecución completa e idempotente después de corregir la causa.
- Q: ¿Qué debe ocurrir si un proyecto derivado precarga desde su iniciativa un valor de catálogo que ya está inactivo? → A: Mostrarlo como referencia histórica y exigir su reemplazo por un ítem activo antes de crear el proyecto.
- Q: ¿Qué filtros debe tener la bandeja documental? → A: Mantener únicamente los filtros que ya existen y centralizar sus opciones; no crear filtros nuevos.
- Q: Cuando una persona vea un PEI o POI de prueba, ¿cómo debe saber que no es información oficial? → A: No mostrar ningún aviso en pantalla; registrar su condición sintética únicamente en el seed y la documentación técnica de pruebas.
- Q: Después del reinicio del ambiente de pruebas, ¿deben conservarse las notificaciones anteriores? → A: No; eliminar todas las notificaciones y recrear `NOTIFICACION` vacía, preservando usuarios y permisos.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar con catálogos centralizados (Priority: P1)

Como persona autorizada para registrar una iniciativa o proyecto, quiero cargar y seleccionar opciones vigentes provistas por PIIP para guardar referencias consistentes sin depender de listas duplicadas en la interfaz.

**Why this priority**: La creación es el punto donde una selección deja de ser informativa y pasa a formar parte del registro; si se guarda texto o una identidad incorrecta, todas las consultas posteriores quedan expuestas a inconsistencias.

**Independent Test**: Se crea una iniciativa, un proyecto preexistente y un proyecto derivado seleccionando opciones obtenidas del backend, y se comprueba que cada campo persistente queda asociado al identificador correcto y que no se envían códigos ni identificadores numéricos hardcodeados.

**Acceptance Scenarios**:

1. **Given** una persona que abre un formulario de registro, **When** finaliza la carga inicial, **Then** Tipo de solución, Fuente u origen, Objetivo PEI, Actividad POI y Unidad Orgánica muestran únicamente opciones activas y válidas para el contexto.
2. **Given** una selección válida para todos los campos aplicables, **When** la persona registra una iniciativa, **Then** se guardan referencias a Tipo de solución, Fuente u origen, Objetivo PEI y Actividad POI y la Unidad Orgánica queda asociada mediante el modelo vigente de responsabilidad.
3. **Given** un proyecto preexistente o derivado, **When** la persona completa sus campos aplicables, **Then** el proyecto utiliza las mismas referencias centralizadas sin convertir Componente digital, estados u otros conceptos fuera de alcance en nuevos catálogos.
4. **Given** opciones PEI y POI cargadas, **When** la persona elige una de cada catálogo, **Then** puede combinar cualquier Objetivo PEI activo con cualquier Actividad POI activa sin filtrado ni dependencia mutua.
5. **Given** que una opción seleccionada deja de estar activa antes de guardar, **When** se procesa el registro, **Then** la operación se rechaza de forma comprensible y no conserva una nueva referencia inválida.
6. **Given** un proyecto derivado que precarga desde su iniciativa un valor de catálogo inactivo, **When** la persona revisa el formulario, **Then** reconoce el valor heredado como referencia histórica y debe reemplazarlo por un ítem activo antes de confirmar la creación.

---

### User Story 2 - Consultar y filtrar con identidades consistentes (Priority: P1)

Como persona que consulta iniciativas, proyectos o la bandeja documental, quiero que filtros, listados y detalles resuelvan las mismas identidades y nombres de catálogo para reconocer la información sin divergencias entre pantallas.

**Why this priority**: Centralizar solo la creación no elimina la duplicación ni garantiza que un registro se interprete igual en todos sus consumidores.

**Independent Test**: Se recorre el listado y detalle de iniciativas y proyectos y la bandeja documental con filtros basados en opciones consultadas al backend, comprobando que todas las vistas muestran la misma denominación y usan la identidad correcta al consultar.

**Acceptance Scenarios**:

1. **Given** registros asociados a catálogos persistentes, **When** se muestran listados o detalles, **Then** cada valor se entrega con identificador, código, nombre, orden y estado activo según corresponda.
2. **Given** un consumidor que ya posee un filtro de Tipo de solución, Fuente u origen, Objetivo PEI, Actividad POI o Tipo documental, **When** la persona consulta sus opciones, **Then** solo se muestran ítems activos y la consulta usa la identidad entregada por PIIP, no una etiqueta ni un número hardcodeado; la feature no agrega ese filtro a consumidores que actualmente no lo tienen.
3. **Given** los filtros de Tipo de registro, **When** se selecciona `Iniciativa` o `Proyecto`, **Then** se usa el catálogo técnico vigente; `Todos` permanece como opción local del filtro y no se convierte en dato de catálogo.
4. **Given** que una consulta de catálogo falla o no devuelve opciones, **When** finaliza la carga, **Then** la pantalla diferencia carga, vacío y error y no sustituye el resultado por una lista funcional hardcodeada.
5. **Given** una selección válida y una recarga o actualización de opciones, **When** la identidad sigue disponible, **Then** la selección se conserva sin compararla únicamente por su etiqueta visible.

---

### User Story 3 - Elegir la Unidad Orgánica responsable correcta (Priority: P1)

Como persona que registra una iniciativa o proyecto, quiero elegir una Unidad Orgánica activa perteneciente a la Unidad Ejecutora del registro para asociar la responsabilidad con el modelo organizacional real.

**Why this priority**: Una unidad de otra Unidad Ejecutora o una sigla local sin identidad institucional produciría una responsabilidad ambigua y podría cruzar ámbitos organizacionales.

**Independent Test**: Se cambia la Unidad Ejecutora activa entre dos contextos con unidades distintas, se selecciona una Unidad Orgánica en cada uno y se comprueba que solo se aceptan identidades activas pertenecientes a la unidad correspondiente.

**Acceptance Scenarios**:

1. **Given** una Unidad Ejecutora seleccionada, **When** se consultan las unidades responsables, **Then** se muestran únicamente sus Unidades Orgánicas activas.
2. **Given** una Unidad Orgánica válida, **When** se guarda el registro, **Then** el frontend envía `ID_UNIDAD_ORGANICA` y el backend conserva la asociación en `REGISTRO_UNIDAD_RESPONSABLE`.
3. **Given** un identificador inexistente, inactivo o perteneciente a otra Unidad Ejecutora, **When** se intenta crear una responsabilidad, **Then** la operación se rechaza sin crear una asociación parcial.
4. **Given** que la consulta inicial no devuelve opciones válidas, **When** se presenta el formulario, **Then** no aparecen `DGIA`, `DIPNA`, `DGA`, `DCLIMA`, `DGESEP`, `SENASA` ni otras siglas como fallback funcional.

---

### User Story 4 - Gestionar documentos sin alterar su proceso (Priority: P1)

Como persona que gestiona documentos de una iniciativa o proyecto, quiero seleccionar y consultar tipos documentales centralizados conservando íntegramente las reglas actuales del expediente.

**Why this priority**: Cambiar la identidad del tipo documental afecta una restricción estructural central; cualquier regresión podría duplicar posiciones, perder versiones o alterar la publicación y la auditoría.

**Independent Test**: Se prepara un expediente con los seis tipos documentales, se carga más de una versión, se publica una versión y se marca otra posición como `No aplica`, verificando que la nueva referencia no cambia ninguno de esos comportamientos.

**Acceptance Scenarios**:

1. **Given** una iniciativa o proyecto, **When** se consulta su expediente, **Then** los seis tipos documentales activos continúan disponibles sin distinción `APLICA_A`.
2. **Given** un tipo documental seleccionado desde el catálogo, **When** se crea o actualiza una posición documental, **Then** `DOCUMENTO` referencia su identidad persistente y mantiene una única posición por registro y tipo.
3. **Given** una posición con versiones y contenido, **When** cambia la representación persistente del tipo, **Then** se conservan nombre, MIME, tamaño, checksum, contenido, versiones, publicación externa, auditoría, estado `No aplica` y motivo.
4. **Given** un tipo documental inactivo referenciado históricamente, **When** se consulta el expediente, **Then** su nombre continúa resolviéndose, pero el tipo no está disponible para una nueva selección.

---

### User Story 5 - Conservar historial con elementos inactivos (Priority: P2)

Como persona que consulta un registro existente, quiero reconocer los valores históricos aunque ya no estén disponibles para nuevas selecciones, para no perder contexto ni reactivar datos obsoletos accidentalmente.

**Why this priority**: La desactivación es el mecanismo operativo previsto; ocultar por completo el valor produciría detalles incompletos y permitir seleccionarlo nuevamente contradiría la política de disponibilidad.

**Independent Test**: Se desactiva un ítem ya utilizado, se consulta el registro histórico y se intenta utilizar nuevamente su identificador en una operación de creación, comprobando la diferencia entre visualización histórica y nueva selección.

**Acceptance Scenarios**:

1. **Given** un registro que referencia un ítem inactivo, **When** se consulta su detalle, **Then** se muestran código y nombre históricos junto con su estado inactivo.
2. **Given** un formulario de creación, **When** se presentan las opciones, **Then** solo se ofrecen ítems activos.
3. **Given** un identificador inactivo enviado manualmente en una operación de creación, **When** el backend lo valida, **Then** rechaza la nueva referencia sin alterar el historial existente.
4. **Given** un elemento ya utilizado por registros, **When** cambia su disponibilidad, **Then** se desactiva sin eliminación física durante la operación normal.
5. **Given** registros históricos asociados a un ítem inactivo, **When** se presentan los filtros, **Then** ese ítem no aparece como opción, aunque su código y nombre continúan visibles en los resultados y detalles que lo referencian.

---

### User Story 6 - Reiniciar de forma controlada el ambiente de pruebas (Priority: P2)

Como responsable técnico del ambiente de pruebas, quiero reiniciar selectivamente los datos transaccionales afectados y cargar catálogos reproducibles para validar el nuevo modelo sin comprometer usuarios, seguridad ni maestros no incluidos.

**Why this priority**: El cambio sustituye columnas textuales por referencias y la solicitud descarta convertir el historial de prueba; el reinicio debe ser explícito, repetible y limitado al ambiente autorizado.

**Independent Test**: En un ambiente de pruebas se activa manualmente el perfil exclusivo, se observa la limpieza, recreación y carga completa, se reinicia luego con el perfil normal y se verifica que los datos preservados permanecen intactos y que la limpieza no vuelve a ejecutarse.

**Acceptance Scenarios**:

1. **Given** el perfil exclusivo deshabilitado, **When** el sistema opera con su perfil normal, **Then** valida la estructura vigente y no limpia, elimina, recrea ni recarga tablas.
2. **Given** un ambiente de pruebas autorizado, **When** se activa manualmente el perfil exclusivo, **Then** identifica las dependencias, limpia los datos afectados, elimina y recrea las estructuras necesarias en orden seguro y carga automáticamente los datos iniciales.
3. **Given** un ambiente que no sea de pruebas autorizado, incluido producción, **When** se intenta activar el perfil, **Then** la operación se rechaza antes de cualquier eliminación o modificación.
4. **Given** un fallo en cualquier etapa, **When** se detecta el error, **Then** el proceso se detiene antes de ejecutar etapas posteriores, identifica inequívocamente la etapa fallida y no comunica una finalización correcta.
5. **Given** una ejecución correcta, **When** se revisan los datos preservados, **Then** usuarios, roles, permisos, ámbitos, instituciones, Unidades Ejecutoras, identidad, seguridad y maestros no afectados permanecen íntegros.
6. **Given** una segunda ejecución de la carga de datos, **When** se localizan elementos por sus códigos estables, **Then** no se duplican cabeceras, ítems ni relaciones.
7. **Given** una ejecución correcta del reinicio, **When** termina la recreación, **Then** `EVENTO_AUDITORIA`, `AUDITORIA_ACCESO` y `NOTIFICACION` existen nuevamente y no conservan filas anteriores al reinicio.

### Edge Cases

- Un identificador existe, pero pertenece a un catálogo distinto del campo recibido: se rechaza aunque el ítem esté activo.
- Dos ítems intentan usar el mismo código dentro del mismo catálogo: se preserva la unicidad y no se crea el duplicado.
- El mismo código aparece en catálogos genéricos diferentes: puede coexistir porque la unicidad del ítem está acotada a su catálogo.
- Un catálogo activo no tiene ítems activos: el consumidor recibe una respuesta vacía explícita y no crea opciones locales.
- Una consulta termina después de que la persona cambió de Unidad Ejecutora: el resultado anterior no reemplaza las opciones de la nueva unidad.
- La Unidad Orgánica histórica se desactiva: continúa resolviéndose en el registro existente, pero no aparece en selecciones nuevas.
- Un proyecto derivado hereda como referencia un valor que se desactivó después de registrar la iniciativa: el valor sigue visible como contexto, pero no satisface la nueva selección hasta ser reemplazado por un ítem activo.
- La etiqueta de un ítem cambia sin cambiar su código ni identidad: los consumidores muestran la denominación vigente sin romper las referencias existentes.
- Un registro previo al reinicio de pruebas depende de versiones, contenido, auditoría u otras tablas relacionadas: la lista y el orden de eliminación deben incluir todas las dependencias estrictamente necesarias, y el historial de auditoría de ese ambiente se descarta por completo.
- Una notificación no referencia un registro de portafolio: también se elimina, porque el reinicio del ambiente de pruebas descarta íntegramente el historial de `NOTIFICACION`.
- La carga encuentra una Unidad Ejecutora activa sin Unidades Orgánicas válidas: puede agregar varias unidades sintéticas de prueba vinculándolas por el código estable de la Unidad Ejecutora, sin reutilizar arbitrariamente siglas hardcodeadas.
- El archivo de datos contiene instrucciones estructurales: el perfil debe rechazar o no ejecutar esa versión como carga válida.
- El perfil se detiene después de una eliminación parcial: tras corregir la causa, una nueva ejecución completa debe reconciliar el estado existente sin duplicar datos ni omitir etapas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: PIIP DEBE ofrecer desde el backend las siete fuentes centralizadas: Tipo de solución, Fuente u origen, Objetivo PEI, Actividad POI, Tipo documental, Unidad Orgánica responsable y Tipo de registro.
- **FR-002**: `CATALOGO` y `CATALOGO_ITEM` DEBEN contener exclusivamente Tipo de solución, Fuente u origen, Objetivo PEI y Actividad POI dentro del alcance de esta feature.
- **FR-003**: Cada catálogo genérico e ítem DEBE tener identificador técnico, código estable, nombre, orden de presentación e indicador activo.
- **FR-004**: El código de un ítem DEBE ser único dentro de su catálogo y DEBE poder utilizarse para carga, diagnóstico e integraciones técnicas sin sustituir al identificador de escritura usado por el frontend.
- **FR-005**: Tipo de solución DEBE reutilizar exactamente `POTENTIAL_OR_ADAPTABLE` — `Solución potencial o adaptable`, `TO_BE_DEFINED` — `Solución por definir` y `NOT_APPLICABLE` — `No aplica`.
- **FR-006**: Fuente u origen DEBE reutilizar exactamente `INITIATIVE_SHEET` — `Ficha de iniciativa de innovación pública`, `INTERNAL_CONTEST` — `Concurso interno`, `OPEN_INNOVATION` — `Innovación abierta`, `MANAGEMENT_PROPOSAL` — `Propuesta de jefatura o directivos`, `OTHER` — `Otros` y `CALL` — `Convocatoria`.
- **FR-007**: Objetivo PEI y Actividad POI DEBEN ser catálogos persistentes independientes y conservar su opcionalidad vigente; ninguna opción o selección de uno DEBE filtrar, condicionar, obligar ni modificar el otro.
- **FR-008**: Objetivo PEI DEBE cargar para pruebas `PEI-001` a `PEI-004` con las cuatro denominaciones sintéticas proporcionadas por la solicitud; su condición de datos de prueba DEBE constar en el seed y la documentación técnica, sin marca visible ni campo adicional en el contrato.
- **FR-009**: Actividad POI DEBE cargar para pruebas `POI-001` a `POI-004` con las cuatro denominaciones sintéticas proporcionadas por la solicitud; su condición de datos de prueba DEBE constar en el seed y la documentación técnica, sin marca visible ni campo adicional en el contrato.
- **FR-010**: `TIPO_DOCUMENTO` DEBE tener identificador técnico, código estable y único, nombre, orden de presentación e indicador activo; NO DEBE contener `APLICA_A`.
- **FR-011**: Tipo documental DEBE reutilizar exactamente los seis códigos vigentes: `PUBLIC_INNOVATION_INITIATIVE_SHEET`, `INITIATIVE_TECHNICAL_OPINION`, `FORMAL_APPROVAL_DECISION`, `FINAL_PRODUCT_APPROVAL`, `PROJECT_MANAGEMENT_DOCUMENTATION` y `FINAL_CLOSURE_REPORT`, conservando sus denominaciones vigentes.
- **FR-012**: Los seis tipos documentales activos DEBEN permanecer disponibles para iniciativas y proyectos.
- **FR-013**: Tipo de registro DEBE permanecer como catálogo técnico de solo lectura con `Iniciativa` y `Proyecto`; NO DEBE tener tabla administrable y `Todos` NO DEBE ser elemento del catálogo.
- **FR-014**: Las consultas de catálogo DEBEN entregar, según corresponda, identificador, código, nombre, orden y estado activo mediante un contrato suficiente y consistente para sus consumidores.
- **FR-015**: El frontend DEBE obtener del backend las opciones funcionales centralizadas y NO DEBE conservar listas duplicadas ni fallbacks funcionales para ellas.
- **FR-016**: Para crear una referencia persistente, el frontend DEBE enviar el identificador activo recibido. Un filtro local vigente DEBE comparar por identidad; un filtro remoto DEBE enviar únicamente la identidad prevista por su contrato actual. El frontend NO DEBE hardcodear identificadores numéricos ni usar el código o la etiqueta como identidad de escritura. Cualquier actualización general incorporada por una feature futura DEBE respetar la misma regla, pero su creación está fuera del alcance actual.
- **FR-017**: El backend DEBE validar que todo identificador persistente exista, pertenezca al catálogo esperado y esté activo cuando represente una nueva selección.
- **FR-018**: `REGISTRO_PORTAFOLIO` DEBE reemplazar el almacenamiento anterior de Tipo de solución, Fuente u origen, Objetivo PEI y Actividad POI por `ID_TIPO_SOLUCION`, `ID_FUENTE_ORIGEN`, `ID_OBJETIVO_PEI` e `ID_ACTIVIDAD_POI`, con referencias explícitas a sus ítems.
- **FR-019**: Las columnas textuales o enum anteriores y sus restricciones `CHECK` asociadas DEBEN eliminarse; el ambiente de pruebas NO DEBE mantener simultáneamente columnas antiguas y nuevas.
- **FR-020**: `DOCUMENTO` DEBE reemplazar el tipo textual por `ID_TIPO_DOCUMENTO` con referencia a `TIPO_DOCUMENTO`; la unicidad vigente por registro y tipo DEBE conservarse sobre la nueva identidad.
- **FR-021**: La centralización de Tipo documental NO DEBE alterar posiciones, versiones, nombre, MIME, tamaño, checksum, publicación externa, estado `No aplica`, motivo, auditoría, contenido ni restricciones de unicidad del proceso documental.
- **FR-022**: La Unidad Orgánica responsable DEBE reutilizar Institución, Unidad Ejecutora, Unidad Orgánica y Registro Unidad Responsable; NO DEBE crear una tabla nueva de unidad responsable ni introducir `PROYECTO_UNIDAD_ORGANICA`.
- **FR-023**: El selector de unidades responsables DEBE mostrar únicamente Unidades Orgánicas activas de la Unidad Ejecutora del registro y DEBE enviar `ID_UNIDAD_ORGANICA` para conservar la asociación vigente.
- **FR-024**: El backend DEBE rechazar una Unidad Orgánica inexistente, inactiva o ajena a la Unidad Ejecutora del registro.
- **FR-025**: El fallback `RESPONSIBLE_UNITS` DEBE eliminarse una vez que la carga inicial garantice opciones válidas para las Unidades Ejecutoras del ambiente de pruebas; sus siglas NO DEBEN asignarse arbitrariamente sin evidencia institucional.
- **FR-026**: Un ítem o Unidad Orgánica inactiva referenciada históricamente DEBE continuar resolviéndose en resultados y detalles de lectura, pero NO DEBE aparecer en filtros ni aceptarse como nueva selección.
- **FR-027**: Los elementos utilizados por registros funcionales NO DEBEN eliminarse físicamente durante la operación normal; la disponibilidad DEBE cambiar mediante el indicador activo.
- **FR-028**: La interfaz DEBE distinguir estado de carga, respuesta vacía y error de consulta para cada consumidor y DEBE conservar una selección válida cuando la identidad continúe disponible.
- **FR-029**: La feature NO DEBE crear pantalla de administración ni endpoints para crear, editar, reordenar o desactivar catálogos; el backend expondrá únicamente consultas funcionales.
- **FR-030**: Como excepción constitucional acotada al perfil destructivo de desarrollo o pruebas, los datos iniciales y sus ajustes DEBEN mantenerse en un archivo SQL externo, versionado, ejecutable automáticamente y utilizable como respaldo manual; el archivo NO DEBE ejecutarse durante la operación normal ni en producción.
- **FR-031**: La carga de datos DEBE ser idempotente y localizar cabeceras, ítems y entidades relacionadas mediante códigos estables, sin depender de identificadores numéricos hardcodeados.
- **FR-032**: El archivo SQL de datos iniciales DEBE contener únicamente datos y NO DEBE contener `CREATE TABLE`, `ALTER TABLE` ni otra definición estructural duplicada.
- **FR-033**: Cuando una Unidad Ejecutora activa de pruebas carezca de opciones válidas, la carga PODRÁ agregar varias Unidades Orgánicas sintéticas y realistas vinculadas mediante el código estable de esa Unidad Ejecutora y claramente identificadas como datos de prueba.
- **FR-034**: DEBE existir un perfil exclusivo de reinicio y carga, deshabilitado por defecto, activable solo manualmente en pruebas y rechazado en producción antes de cualquier acción destructiva.
- **FR-035**: Antes de concretar el reinicio, se DEBE identificar la lista completa de tablas transaccionales afectadas, sus dependencias estrictamente necesarias y el orden seguro de limpieza y eliminación por claves foráneas; no se asumirá que basta con la tabla principal.
- **FR-036**: El perfil exclusivo DEBE ejecutar en orden: limpieza selectiva, eliminación controlada de estructuras afectadas, recreación desde las entidades persistentes actualizadas, carga automática del SQL externo desde el backend y confirmación inequívoca de éxito o error. Ante el primer fallo, DEBE detenerse, identificar la etapa fallida, omitir las etapas posteriores y permitir una nueva ejecución completa después de corregir la causa.
- **FR-037**: Las entidades persistentes DEBEN ser la única definición estructural permanente; el perfil normal DEBE validar la estructura y NO DEBE repetir limpieza, eliminación, recreación ni carga.
- **FR-038**: El reinicio de pruebas DEBE eliminar el historial afectado de iniciativas, proyectos, documentos y catálogos, así como todas las filas y estructuras de `EVENTO_AUDITORIA`, `AUDITORIA_ACCESO` y `NOTIFICACION`, y recrear esas tablas vacías en lugar de preservar o convertir datos anteriores.
- **FR-039**: El reinicio DEBE conservar íntegramente usuarios, roles, permisos, asignaciones, ámbitos administrativos, instituciones, Unidades Ejecutoras, identidad, seguridad y todo maestro no afectado por esta feature.
- **FR-040**: El contrato OpenAPI y su consumidor generado DEBEN reflejar el contrato centralizado aprobado antes de adaptar los consumidores funcionales.
- **FR-041**: Como mínimo, la cobertura funcional DEBE incluir: Crear iniciativa; Crear proyecto preexistente; Crear proyecto derivado; listados y filtros de iniciativas; listados y filtros de proyectos; bandeja documental; gestión documental de iniciativas; gestión documental de proyectos; y detalles que muestran Objetivo PEI y Actividad POI.
- **FR-042**: Cada consumidor mínimo DEBE contemplar carga desde backend, estado de carga, respuesta vacía, error, conservación de selección válida, visualización histórica de inactivos y ausencia de fallbacks funcionales hardcodeados.
- **FR-043**: La feature NO DEBE cambiar estados, transiciones, permisos, alcances de autorización ni reglas vigentes del portafolio o del proceso documental.
- **FR-044**: Cuando un proyecto derivado precargue desde su iniciativa un valor de catálogo inactivo, DEBE mostrarlo como referencia histórica, marcarlo como no válido para la nueva escritura y exigir su reemplazo por un ítem activo antes de crear el proyecto.
- **FR-045**: La bandeja documental DEBE conservar únicamente sus filtros vigentes y centralizar las opciones de aquellos que correspondan; esta feature NO DEBE crear filtros nuevos en esa bandeja.

### Datos iniciales de prueba

Los valores PEI y POI siguientes son sintéticos y exclusivos del ambiente de pruebas. La interfaz los presenta por su nombre sin una etiqueta adicional de oficialidad.

#### Objetivo PEI

| Código | Nombre |
|---|---|
| `PEI-001` | Fortalecer la gestión institucional orientada a resultados. |
| `PEI-002` | Mejorar la calidad de los servicios brindados a la ciudadanía. |
| `PEI-003` | Impulsar la transformación digital institucional. |
| `PEI-004` | Fortalecer las capacidades institucionales para la innovación. |

#### Actividad POI

| Código | Nombre |
|---|---|
| `POI-001` | Ejecutar acciones de mejora de procesos institucionales. |
| `POI-002` | Implementar servicios digitales para la atención de usuarios. |
| `POI-003` | Realizar el seguimiento de indicadores de desempeño institucional. |
| `POI-004` | Fortalecer las capacidades del personal en gestión e innovación. |

### Cobertura mínima por consumidor

| Consumidor | Fuentes centralizadas mínimas | Resultado esperado |
|---|---|---|
| Crear iniciativa | Tipo de solución, Fuente u origen, Objetivo PEI, Actividad POI, Unidad Orgánica | Guarda referencias válidas y activas. |
| Crear proyecto preexistente | Fuente u origen, Objetivo PEI, Actividad POI, Unidad Orgánica y campos aplicables | Guarda referencias válidas sin introducir reglas nuevas. |
| Crear proyecto derivado | Tipo de solución, Fuente u origen, Objetivo PEI, Actividad POI, Unidad Orgánica y campos aplicables | Conserva el origen, muestra valores heredados inactivos como contexto y exige referencias activas para el nuevo proyecto. |
| Listados y filtros de iniciativas | Tipo de registro y catálogos persistentes aplicables | Filtra por identidades canónicas y muestra nombres resueltos. |
| Listados y filtros de proyectos | Tipo de registro y catálogos persistentes aplicables | Filtra por identidades canónicas y muestra nombres resueltos. |
| Bandeja documental | Únicamente las fuentes correspondientes a sus filtros vigentes | Centraliza sus opciones sin crear filtros nuevos, no usa listas duplicadas y resuelve históricos inactivos. |
| Documentos de iniciativas | Tipo documental | Conserva las seis posiciones y todo el ciclo documental. |
| Documentos de proyectos | Tipo documental | Conserva las seis posiciones y todo el ciclo documental. |
| Detalles de iniciativa y proyecto | Objetivo PEI y Actividad POI, además de catálogos visibles aplicables | Muestra código, nombre y estado aun cuando el ítem histórico esté inactivo. |

### Key Entities *(include if feature involves data)*

- **Catálogo**: Cabecera de una fuente genérica, identificada por código estable, nombre, orden y estado activo; agrupa sus ítems sin mezclar dominios.
- **Ítem de catálogo**: Opción persistente perteneciente a un catálogo genérico; tiene identidad técnica, código único dentro de su catálogo, nombre, orden y estado activo.
- **Tipo documental**: Opción persistente independiente que representa una posición documental mediante uno de los seis códigos vigentes, sin limitarse por tipo de registro.
- **Tipo de registro**: Catálogo técnico de solo lectura que distingue iniciativa y proyecto sin persistir una tabla administrable.
- **Unidad Orgánica**: Unidad activa perteneciente a una Unidad Ejecutora y seleccionable como responsabilidad del registro.
- **Registro Unidad Responsable**: Asociación vigente entre una iniciativa o proyecto y una Unidad Orgánica, con su orden de presentación y denominación histórica.
- **Registro de portafolio**: Iniciativa o proyecto que referencia Tipo de solución, Fuente u origen, Objetivo PEI y Actividad POI mediante identidades persistentes según sus campos aplicables.
- **Documento**: Posición única de un registro que referencia un Tipo documental y conserva estado, motivo de no aplicación, versiones, contenido, publicación y auditoría.
- **Perfil de reinicio de pruebas**: Capacidad técnica explícita y restringida que elimina el historial de prueba afectado, recrea las estructuras derivadas y carga datos iniciales sin tocar seguridad ni maestros preservados.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las siete fuentes de selección definidas obtiene sus opciones desde el backend o, para opciones como `Todos`, las mantiene explícitamente fuera del catálogo funcional.
- **SC-002**: El 100 % de las nuevas iniciativas y proyectos guarda identificadores válidos para todos los catálogos persistentes aplicables; ninguna escritura usa códigos, etiquetas ni números hardcodeados como identidad.
- **SC-003**: El 100 % de las combinaciones entre los cuatro Objetivos PEI y las cuatro Actividades POI activas puede seleccionarse sin filtrado ni dependencia mutua.
- **SC-004**: El 100 % de las nuevas asociaciones de responsabilidad referencia una Unidad Orgánica activa perteneciente a la Unidad Ejecutora del registro.
- **SC-005**: Los seis tipos documentales vigentes están disponibles tanto para iniciativas como para proyectos y cada expediente conserva exactamente una posición por registro y tipo.
- **SC-006**: En los escenarios documentales de aceptación se conserva el 100 % de versiones, contenido, metadatos, publicación, auditoría, estado `No aplica`, motivo y restricciones de unicidad.
- **SC-007**: El 100 % de los registros históricos que referencian un ítem inactivo continúa mostrando una denominación resoluble, mientras que el 0 % de los filtros y nuevas selecciones ofrece o acepta ese ítem.
- **SC-008**: Ninguno de los consumidores mínimos contiene fallbacks o listas funcionales duplicadas para los siete conceptos centralizados.
- **SC-009**: El 100 % de las consultas de opciones distingue correctamente carga, vacío y error y conserva la selección cuando la misma identidad sigue disponible.
- **SC-010**: Dos ejecuciones consecutivas de la carga con los mismos códigos producen una sola cabecera, un solo ítem por código dentro de su catálogo y una sola relación inicial esperada.
- **SC-011**: El intento de activar el reinicio fuera de pruebas es rechazado antes de modificar datos en el 100 % de los casos de aceptación.
- **SC-012**: Después de un reinicio correcto, el 100 % de usuarios, roles, permisos, ámbitos, instituciones, Unidades Ejecutoras y configuración de identidad y seguridad permanece sin eliminación ni modificación atribuible al reinicio.
- **SC-013**: Un arranque posterior con el perfil normal valida la estructura sin repetir ninguna etapa destructiva o de carga.
- **SC-014**: La inspección del archivo SQL de datos iniciales encuentra cero instrucciones de creación o alteración estructural.
- **SC-015**: En el 100 % de los fallos inducidos durante el reinicio, el proceso identifica la etapa fallida, no ejecuta etapas posteriores ni informa éxito, y puede volver a ejecutarse completamente después de corregir la causa.
- **SC-016**: El 100 % de los proyectos derivados que precargan un valor inactivo muestra ese valor como referencia histórica y bloquea la confirmación hasta que sea reemplazado por un ítem activo.
- **SC-017**: Después de un reinicio correcto, `EVENTO_AUDITORIA`, `AUDITORIA_ACCESO` y `NOTIFICACION` contienen cero filas anteriores al reinicio y los usuarios destinatarios permanecen íntegros.

## Dependencies and Constraints

- Depende del modelo vigente de Institución, Unidad Ejecutora, Unidad Orgánica y Registro Unidad Responsable.
- Depende de preservar los códigos y denominaciones actuales confirmados para Tipo de solución, Fuente u origen y Tipo documental.
- La lista completa de tablas dependientes y su orden seguro es un artefacto obligatorio de diseño previo a cualquier reinicio; esta especificación no inventa esa lista.
- La adaptación posterior del contrato deberá preceder a la regeneración de su consumidor y a los cambios de pantallas que lo utilizan.
- La guía funcional de `docs/` tiene impacto porque cambian datos visibles, comportamiento de selecciones y representación histórica; deberá actualizarse con evidencia en la misma entrega de implementación.
- La especificación no autoriza implementación, generación de contratos, migraciones, pruebas, compilación, servidores, contenedores, integración Oracle ni acciones Git.

## Production Readiness Clarifications

Estos pendientes están marcados para una futura salida a producción y no bloquean el diseño ni la implementación exclusiva del ambiente de pruebas descrito aquí:

- **NEEDS CLARIFICATION — Valores oficiales PEI y POI**: confirmar los códigos y denominaciones oficiales antes de sustituir los datos sintéticos de prueba.
- **NEEDS CLARIFICATION — Autoridad institucional**: identificar quién aprueba y actualiza oficialmente los catálogos PEI y POI.
- **NEEDS CLARIFICATION — Migración productiva**: definir una estrategia no destructiva para convertir datos productivos previos sin aplicar el reinicio de pruebas.

## Out of Scope

- Pantalla administrativa o endpoints de escritura para catálogos.
- Pantallas, endpoints o casos de uso nuevos para la edición general de iniciativas o proyectos.
- Cambios en estados, transiciones, roles, permisos o alcances del portafolio.
- Conversión de Componente digital u otros enums estables no solicitados en tablas administrables.
- Rediseño del modelo organizacional, nueva tabla de unidades responsables o uso de `PROYECTO_UNIDAD_ORGANICA`.
- Relación jerárquica, filtrado o dependencia entre Objetivo PEI y Actividad POI.
- Restricción de tipos documentales por iniciativa o proyecto mediante `APLICA_A`.
- Eliminación física ordinaria de elementos ya usados por registros funcionales.
- Migración destructiva equivalente en producción.
- Publicación de los valores sintéticos PEI, POI u organizacionales como información oficial de MIDAGRI.
- Implementación de código, ejecución de migraciones, cambios en base de datos, inicio de servicios, pruebas, compilación o generación de contrato durante esta fase de especificación.
