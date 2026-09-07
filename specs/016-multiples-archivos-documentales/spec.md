# Feature Specification: Múltiples archivos independientes por tipo documental

**Feature Branch**: `016-multiples-archivos-documentales`

**Created**: 2026-09-06

**Status**: Draft

**Input**: User description: "Permitir múltiples archivos independientes por tipo documental en iniciativas y proyectos PIIP, conservando la trazabilidad de versiones por archivo, con eliminación individual autorizada y auditable, migración sin pérdida de información y compatibilidad con clientes existentes."

Hoy cada iniciativa o proyecto (expediente) tiene una única posición documental por cada tipo de documento: una nueva carga del mismo tipo se registra como una nueva versión dentro de esa posición. Esta feature permite varios archivos simultáneos e independientes del mismo tipo documental, cada uno con su propio historial de versiones, identificable y operable individualmente (agregar, versionar, consultar, eliminar), conservando íntegras las reglas actuales de autorización, publicación externa, descarga, auditoría, tamaño máximo y tipos MIME. Incluye la migración sin pérdida de los documentos existentes y la compatibilidad de las cargas actuales.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Agregar varios archivos del mismo tipo documental (Priority: P1)

Como administrador PIIP del ámbito de un expediente, necesito agregar varios archivos independientes del mismo tipo documental a una iniciativa o proyecto, para representar documentos distintos que deben coexistir (por ejemplo, dos informes de gestión de distinto origen), donde hoy la segunda carga se convierte automáticamente en una nueva versión de la primera.

**Why this priority**: Es la capacidad central de la feature: sin coexistencia de archivos independientes por tipo, ninguno de los demás requisitos tiene sentido. Hoy el sistema materializa una única posición por tipo y toda carga repetida engrosa el historial de esa posición.

**Independent Test**: Con un expediente y un tipo documental, cargar dos archivos por separado mediante "Agregar archivo" y verificar que existen dos elementos independientes, cada uno con versión 1, ambos consultables y descargables según las reglas vigentes.

**Acceptance Scenarios**:

1. **Given** un expediente con un tipo documental presentado sin archivos, **When** el administrador del ámbito agrega un archivo de ese tipo, **Then** se crea el primer archivo independiente del tipo, con ese archivo como su versión 1, y el tipo deja de presentarse como pendiente.
2. **Given** un tipo documental con un archivo existente (versión vigente 1), **When** el administrador del ámbito agrega otro archivo del mismo tipo, **Then** existen dos archivos independientes, ambos con versión 1, listados como elementos separados y distinguibles entre sí.
3. **Given** un usuario sin autorización de administración sobre el ámbito del expediente, **When** intenta agregar un archivo, **Then** la operación es rechazada.
4. **Given** un archivo vacío, o con tipo MIME no permitido, o que excede el tamaño máximo vigente, **When** se intenta agregar, **Then** la operación es rechazada sin crear archivo ni versión alguna.

---

### User Story 2 - Crear una nueva versión de un archivo específico (Priority: P1)

Como administrador PIIP del ámbito, necesito crear una nueva versión de un archivo específico, como acción distinta y explícita de agregar otro archivo, para que cada documento conserve su propio historial sin contaminar el de los demás.

**Why this priority**: La trazabilidad por archivo es un requisito duro: si las dos acciones se confundieran, toda carga nueva engrosaría el historial de un archivo equivocado. La distinción explícita es lo que preserva el sentido de "archivo independiente".

**Independent Test**: Con dos archivos A y B del mismo tipo, crear una nueva versión de A y verificar que A pasa a versión 2 con historial [1, 2] mientras B permanece en versión 1 con sus metadatos, contenido y publicación intactos.

**Acceptance Scenarios**:

1. **Given** dos archivos A y B del mismo tipo documental (ambos en versión 1), **When** el administrador del ámbito crea una nueva versión de A, **Then** A pasa a tener versión 2 e historial [1, 2], y B permanece en versión 1 con sus metadatos, contenido y estado de publicación intactos.
2. **Given** un usuario sin autorización de administración sobre el ámbito, **When** intenta crear una nueva versión de un archivo, **Then** la operación es rechazada.
3. **Given** un archivo con historial de versiones, **When** se consulta el expediente, **Then** el historial completo del archivo está disponible junto a su versión vigente.

---

### User Story 3 - Consultar todos los archivos, su versión vigente y su historial (Priority: P1)

Como usuario con permiso de consulta del ámbito (interno o de consulta externa), necesito ver todos los archivos de cada tipo documental del expediente, y de cada archivo su versión vigente y su historial, para conocer el estado completo del expediente y descargar lo que me corresponde.

**Why this priority**: Es el valor de uso inmediato de la feature: la consulta actual solo presenta, por tipo, la versión más reciente de la única posición, y no expone el historial. Sin esta historia, los archivos múltiples no serían revisables ni descargables de forma completa.

**Independent Test**: Con un tipo que tiene dos archivos (uno con dos versiones y otro con una), consultar el expediente y verificar que se ven ambos archivos como elementos separados, con la versión vigente y el historial completo de cada uno, y que las descargas responden a las reglas vigentes de publicación externa.

**Acceptance Scenarios**:

1. **Given** un tipo documental con dos archivos (uno con dos versiones y otro con una), **When** un usuario con permiso de lectura del ámbito consulta el expediente, **Then** ve ambos archivos como elementos separados, y de cada uno su versión vigente y su historial completo.
2. **Given** una versión publicada para consulta externa en un archivo, **When** un usuario de consulta externa del ámbito la descarga, **Then** la descarga se completa; y si intenta descargar una versión no publicada del mismo archivo, **Then** la descarga es rechazada.
3. **Given** un usuario interno del ámbito, **When** solicita cualquier versión de cualquier archivo del expediente, **Then** la descarga se completa.

---

### User Story 4 - Migrar los documentos existentes sin pérdida (Priority: P1)

Como administrador PIIP, necesito que cada posición documental existente migre al nuevo modelo como un archivo independiente sin pérdida de información, para que los expedientes en uso conserven toda su trazabilidad histórica.

**Why this priority**: Condiciona el despliegue: sin migración sin pérdida, los expedientes existentes (posiciones, versiones, contenidos, publicaciones y declaraciones "No aplica") quedarían sin representación válida en el nuevo modelo y la feature no podría entrar en operación.

**Independent Test**: En un entorno con posiciones cargadas, ejecutar la migración y verificar, por expediente y tipo, que los conteos de archivos y versiones coinciden con los previos y que nombres, metadatos, contenidos, fechas, autores y estados de publicación se conservan intactos.

**Acceptance Scenarios**:

1. **Given** una posición documental existente con tres versiones, **When** se ejecuta la migración, **Then** existe el archivo original del tipo con exactamente esas tres versiones, conservando nombres, metadatos, contenidos, fechas, autores y estados de publicación externa.
2. **Given** una posición marcada "No aplica" con su motivo, **When** se ejecuta la migración, **Then** la declaración y su motivo se conservan a nivel del tipo documental del expediente, conforme a FR-019.
3. **Given** una posición pendiente sin versiones cargadas, **When** se ejecuta la migración, **Then** no se crea archivo alguno y el tipo se presenta como pendiente hasta la primera carga.
4. **Given** una iniciativa y su proyecto derivado con expedientes separados, **When** se ejecuta la migración, **Then** ningún archivo, versión ni declaración cruza entre expedientes.

---

### User Story 5 - Eliminar individualmente un archivo (Priority: P2)

Como administrador PIIP del ámbito, necesito eliminar individualmente un archivo (inhabilitación lógica) sin afectar a los demás archivos del mismo tipo, dejando registro auditable, para retirar cargas equivocadas del expediente.

**Why this priority**: Agrega control de calidad sobre cargas erróneas; su ausencia no invalida el núcleo (los archivos múltiples siguen siendo operables sin eliminación). Hoy no existe ninguna operación de eliminación documental.

**Independent Test**: Con dos archivos A y B del mismo tipo, eliminar A y verificar que desaparece para todos los roles, que B sigue visible con su versión vigente y su historial, y que la eliminación de A quedó registrada en auditoría.

**Acceptance Scenarios**:

1. **Given** dos archivos A y B del mismo tipo documental, **When** el administrador del ámbito elimina A, **Then** A y sus versiones dejan de aparecer para todos los roles en las consultas del expediente, B sigue visible con su versión vigente y su historial, y la eliminación queda registrada en auditoría conforme a FR-015.
2. **Given** un usuario sin autorización (incluido un administrador de otra unidad), **When** intenta eliminar un archivo, **Then** la operación es rechazada.
3. **Given** un archivo con una versión publicada externamente, **When** es eliminado, **Then** esa versión deja de estar disponible para la consulta externa.

---

### User Story 6 - Mantener la compatibilidad con los clientes existentes (Priority: P2)

Como consumidor de la operación de carga actual por tipo documental, necesito que mi integración siga funcionando con el mismo efecto y sin perder historial, para no romper los flujos que ya operan contra el expediente.

**Why this priority**: Protege a los consumidores vigentes durante la transición; puede garantizarse junto con el núcleo sin bloquearlo, porque la operación actual por tipo conserva su efecto (nueva versión del archivo original migrado).

**Independent Test**: Usar la operación de carga actual por tipo dos veces sobre el mismo expediente y tipo, y verificar que la primera carga crea el archivo original del tipo con versión 1 y la segunda crea la versión 2 de ese mismo archivo, igual que hoy.

**Acceptance Scenarios**:

1. **Given** un cliente que usa la operación de carga actual por tipo, **When** carga dos veces el mismo tipo documental, **Then** la primera carga crea el archivo original del tipo con versión 1 y la segunda crea la versión 2 de ese mismo archivo, igual que hoy.
2. **Given** un cliente que consulta el expediente con el contrato actual, **When** consulta los documentos, **Then** sigue obteniendo, por cada tipo, al menos el archivo original migrado con su historial completo de versiones.

---

### Edge Cases

- Dos archivos del mismo tipo con el mismo nombre de archivo: siguen siendo archivos independientes (la identidad no depende del nombre).
- Carga por la operación actual de tipo cuando el tipo no tiene archivo original (nunca lo tuvo o fue eliminado): crea el primer archivo independiente del tipo.
- Declarar "No aplica" un tipo que tiene varios archivos: la declaración es única, a nivel del tipo, con su motivo; los archivos del tipo siguen siendo consultables, conforme al comportamiento vigente en que una posición marcada conserva sus versiones.
- Cargar un archivo de un tipo con declaración "No aplica" vigente: la carga reanuda el tipo (la declaración deja de aplicar y el tipo vuelve a presentarse con archivos); el motivo histórico queda trazado en auditoría.
- Tipos documentales inactivos: sus archivos históricos se conservan y siguen siendo consultables conforme a las reglas actuales; las nuevas cargas de tipos inactivos siguen rechazándose.
- Eliminar el único archivo de un tipo: el tipo vuelve a presentarse como pendiente (sin archivos).
- Publicar o retirar la publicación de una versión de un archivo mientras otro archivo del mismo tipo se elimina: operaciones independientes, sin efecto cruzado.
- La eliminación nunca elimina eventos de auditoría previos (la auditoría es de solo adición en operación normal).

## Requirements *(mandatory)*

### Functional Requirements

#### Archivos independientes por tipo

- **FR-001**: El sistema MUST permitir que un expediente (iniciativa o proyecto) tenga varios archivos simultáneos del mismo tipo documental, donde cada archivo es una unidad individual identificable dentro del expediente.
- **FR-002**: La acción de agregar un archivo sobre un tipo que ya tiene archivos MUST crear un archivo independiente nuevo cuya primera versión es la número 1.
- **FR-003**: Cada archivo MUST presentarse como un elemento separado en la consulta del expediente, distinguible de los demás archivos del mismo tipo por los metadatos de su versión vigente (nombre de archivo, fecha y hora de carga, autor).
- **FR-004**: "Agregar archivo" y "Nueva versión" MUST ser acciones distintas y explícitas; crear una nueva versión MUST requerir identificar unívocamente el archivo al que pertenece.
- **FR-005**: La primera carga de un tipo documental sin archivos en el expediente MUST crear el primer archivo independiente del tipo (versión 1).
- **FR-006**: Las cargas MUST realizarse de a un archivo por operación; no se requiere selección múltiple en una sola operación.

#### Historial de versiones por archivo

- **FR-007**: Cada archivo MUST conservar su propio historial de versiones, numeradas desde 1 en incrementos de uno.
- **FR-008**: Crear una nueva versión de un archivo MUST no modificar el número de versión, los metadatos, los contenidos, los estados de publicación ni el historial de ningún otro archivo del mismo tipo o de otros tipos.

#### Consulta y descarga

- **FR-009**: Un usuario con permiso de lectura sobre el ámbito del expediente MUST poder consultar todos los archivos no eliminados de cada tipo documental y, para cada archivo, su versión vigente y su historial completo de versiones.
- **FR-010**: La versión vigente de un archivo MUST ser la de mayor número de versión de ese archivo.
- **FR-011**: La descarga MUST regirse por las reglas actuales: usuarios internos del ámbito pueden descargar cualquier versión de cualquier archivo; la consulta externa solo puede descargar versiones publicadas externamente, y cualquier intento fuera de regla es rechazado.

#### Eliminación individual

- **FR-012**: Debe existir una operación de eliminación que actúa sobre la identidad de un archivo específico y MUST no eliminar ni modificar otros archivos del mismo tipo, de otros tipos, ni sus versiones.
- **FR-013**: La eliminación MUST ser una inhabilitación lógica: el archivo eliminado y todas sus versiones dejan de ser visibles y descargables para todos los roles en todas las consultas del expediente, sin operación de restauración en esta versión; los contenidos no se destruyen y el rastro de la eliminación queda en auditoría.
- **FR-014**: La eliminación MUST exigir la misma autorización que la carga de documentos (administración del ámbito del expediente) y ser rechazada para usuarios sin autorización, incluidos administradores de otras unidades.
- **FR-015**: Toda eliminación individual MUST quedar registrada en auditoría identificando el archivo, su tipo documental, el expediente, la versión vigente al momento de eliminar, el actor y el momento, sin eliminar eventos de auditoría previos.
- **FR-016**: Si el archivo eliminado tenía versiones publicadas externamente, estas MUST dejar de estar disponibles para la consulta externa.

#### Reglas existentes que se conservan

- **FR-017**: Cada archivo cargado (nuevo o nueva versión) MUST cumplir las validaciones actuales: tipos MIME exactamente los permitidos hoy, tamaño máximo configurado vigente, archivo no vacío, y cálculo de checksum.
- **FR-018**: Las reglas actuales de autorización, publicación externa por versión, descarga y auditoría de las operaciones documentales MUST mantenerse sin cambios para los archivos independientes.
- **FR-019**: La declaración "No aplica" MUST seguir registrándose por tipo documental del expediente: una única declaración con motivo por tipo (con el límite de longitud vigente), que convive con los archivos ya existentes del tipo y no se marca sobre archivos individuales, con su carácter unidireccional actual (sin operación de des-marca). La carga de un archivo de un tipo con declaración vigente MUST reanudar el tipo: la declaración deja de aplicar y el tipo vuelve a presentarse con archivos, conforme al comportamiento actual.

#### Migración

- **FR-020**: La migración MUST convertir cada posición documental existente con al menos una versión en el archivo original de su tipo, conservando todas sus versiones con nombres, metadatos, contenidos, fechas, autores y estados de publicación externa intactos.
- **FR-021**: Las posiciones pendientes sin versiones MUST no generar archivo alguno; el tipo queda presentado como pendiente hasta la primera carga.
- **FR-022**: Las declaraciones "No aplica" existentes y sus motivos MUST conservarse tras la migración a nivel del tipo documental del expediente, conforme a FR-019.
- **FR-023**: La migración MUST ser verificable mediante conteos por expediente y tipo (posiciones, archivos y versiones antes y después) y MUST no mezclar los expedientes de iniciativas y proyectos; el proyecto derivado no hereda archivos de su iniciativa de origen.

#### Compatibilidad con clientes existentes

- **FR-024**: La operación de carga actual por tipo MUST seguir aceptándose y producir el mismo efecto que hoy: una nueva versión del archivo original del tipo; si el tipo no tiene archivo original, crea el primer archivo independiente del tipo.
- **FR-025**: La consulta actual del expediente MUST seguir exponiendo, por cada tipo documental, al menos el archivo original migrado con su historial completo de versiones, de modo que los clientes existentes no pierdan información ni funcionalidad.
- **FR-026**: Los indicadores del resumen del expediente (documentos cargados, pendientes y "No aplica") MUST seguir contando por tipo documental, no por archivos: un tipo cuenta como cargado si tiene al menos un archivo no eliminado, y los totales permanecen en la escala del catálogo de tipos, conservando el significado vigente de los indicadores.

### Key Entities

- **Expediente**: la iniciativa o el proyecto; contiene sus tipos documentales; los expedientes de iniciativas y proyectos permanecen separados entre sí.
- **Tipo documental**: cada uno de los tipos del catálogo, con su orden de presentación y su condición de activo o inactivo.
- **Archivo independiente**: unidad individual de un tipo documental dentro de un expediente; identificable por sí mismo; agrupa su propio historial de versiones; siempre tiene al menos una versión mientras no esté eliminado.
- **Versión**: carga puntual de un archivo, con nombre de archivo, tipo MIME, tamaño, checksum, fecha, autor, contenido y estado de publicación externa.
- **Declaración "No aplica"**: manifestación con motivo de que un tipo documental no es aplicable al expediente; es única por tipo y convive con los archivos que este tenga.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las cargas sucesivas de un mismo tipo como archivos nuevos produce archivos independientes, cada uno con versión 1.
- **SC-002**: Crear una nueva versión de un archivo produce cero cambios en los demás archivos del mismo tipo.
- **SC-003**: Eliminar un archivo produce cero cambios en los demás archivos del mismo tipo ni en sus historiales.
- **SC-004**: El 100% de las operaciones de carga, versionado, eliminación y descarga intentadas sin autorización es rechazado.
- **SC-005**: La migración conserva el 100% de los archivos, versiones, nombres, metadatos y contenidos existentes (conteos idénticos antes y después, por expediente y tipo).
- **SC-006**: La interfaz muestra los archivos individualmente y un usuario puede distinguir sin ambigüedad las acciones "Agregar archivo", "Nueva versión" y "Eliminar archivo".
- **SC-007**: El 100% de las eliminaciones individuales queda registrado en auditoría.
- **SC-008**: Cero mezclas de archivos o versiones entre expedientes de iniciativas y proyectos.

## Contradicciones y supersesión de antecedentes

Identificadas durante el análisis del código y la documentación vigentes:

1. La especificación histórica de documentos (specs/003-documents), la documentación del modelo de datos (docs/architecture) y el modelo actual (una única posición por expediente y tipo, con restricción de unicidad registro+tipo) fijan una sola posición por tipo: esta feature **supersede expresamente esa cardinalidad**; el resto de aquella especificación (versiones por posición, contenido diferido, publicación externa, validaciones) se conserva.
2. Las pruebas actuales que fijan dos cargas del mismo tipo como versiones 1 y 2 de una misma posición quedan supersededas por FR-002 y FR-007.
3. La interfaz actual muestra solo la versión más reciente por posición y no expone el historial: es un vacío frente a FR-009, no una contradicción del backend, y se resuelve dentro de esta feature.
4. No existe política de borrado documental en el código, la documentación ni las especificaciones: se adopta la inhabilitación lógica como comportamiento predeterminado (FR-013) y su alcance de visibilidad y restauración se resolvió en la Clarificación Q2.
5. La declaración "No aplica" es hoy una marca de la posición (registro+tipo) y la migración a archivos individuales la volvía ambigua: se resolvió en la Clarificación Q1 como declaración por tipo documental del expediente (FR-019).

## Assumptions

- La carga por la operación actual de tipo crea una nueva versión del archivo original migrado; si el tipo no tiene archivo original, crea el primer archivo (A1).
- No hay límite de cantidad de archivos por tipo más allá de las reglas por archivo (A2).
- Un archivo siempre tiene al menos una versión; no existen archivos vacíos (A3).
- La identidad visible de un archivo se basa en los metadatos existentes de su versión vigente; no se añade un campo de título (A4).
- La eliminación es lógica por defecto, por ausencia de política de borrado físico en todas las fuentes: el archivo y su historial quedan ocultos para todos los roles y sin restauración en esta versión (A5).
- Eliminar el único archivo de un tipo hace que el tipo vuelva a presentarse como pendiente (A6).
- Los límites por archivo (tamaño máximo configurado y tipos MIME permitidos) son los vigentes y aplican igual a archivos nuevos y nuevas versiones (A7).
- La notificación a la consulta externa por publicación se mantiene por versión, conforme al comportamiento actual (A8).

## Out of Scope

- Selección múltiple de archivos en una sola operación de carga.
- Borrado físico de archivos o purga de contenidos.
- Restauración de archivos eliminados (excluida conforme a la Clarificación Q2).
- Cambios a roles, tipos MIME, límites de tamaño, estados del portafolio o transiciones del ciclo de vida.
- Copia de documentos al proyecto derivado (siguen sin copiarse, conforme a la especificación histórica de documentos).
- Análisis antimalware en línea (prerrequisito de producción ya registrado en la especificación histórica, pendiente).
- Edición o reemplazo parcial del contenido de una versión existente.

## Clarifications

### Session 1 - 2026-09-06

- **Q1: ¿A qué nivel se marca "No aplica" cuando un tipo documental tiene varios archivos?**
  **Respuesta**: A — La declaración "No aplica" se mantiene por tipo documental del expediente: una única declaración con motivo por tipo, que convive con los archivos existentes del tipo y no se marca sobre archivos individuales. La migración conserva las declaraciones actuales a nivel del tipo. Aplicado en FR-019 y FR-022.
- **Q2: ¿Qué visibilidad tiene el historial de un archivo eliminado y existe restauración?**
  **Respuesta**: A — El archivo eliminado y su historial dejan de ser visibles para todos los roles en todas las consultas, sin operación de restauración en esta versión; el rastro queda en auditoría y los contenidos no se destruyen. Aplicado en FR-013, A5 y el alcance excluido.

### Session 2 - 2026-09-06

- Q: ¿Qué debe ocurrir cuando se carga un archivo de un tipo documental que tiene una declaración "No aplica" vigente? → A: La carga reanuda el tipo: la declaración deja de aplicar y el tipo vuelve a presentarse con archivos (conserva el comportamiento actual; el motivo histórico queda trazado en auditoría). Aplicado en FR-019 y Edge Cases.
- Q: ¿Qué deben contar los indicadores del resumen del expediente cuando un tipo tiene varios archivos? → A: Por tipo documental, como hoy: un tipo cuenta como cargado si tiene al menos un archivo no eliminado; los totales permanecen en la escala del catálogo. Aplicado en FR-026.
