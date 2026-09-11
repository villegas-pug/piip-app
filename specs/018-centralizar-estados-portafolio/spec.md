# Feature Specification: Centralización de estados del portafolio

**Feature Branch**: `018-centralizar-estados-portafolio`

**Created**: 2026-09-10

**Status**: Draft

**Input**: User description: "Centralizar los once estados del portafolio en un catálogo persistente y consultable, conservando sus códigos, denominaciones iniciales, significados, históricos y matrices vigentes."

## Clarifications

### Session 2026-09-10

- Q: ¿La consulta central debe devolver también los estados inactivos para que sigan siendo legibles en registros históricos? → A: No. La consulta central devuelve solo estados activos; cada registro histórico incorpora por separado la metadata vigente de su estado inactivo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar un catálogo único de estados (Priority: P1)

Como usuario autenticado con acceso funcional al PIIP, quiero que los estados de iniciativa y proyecto provengan de la consulta central de catálogos para ver un inventario coherente, ordenado y con la denominación vigente en todos los puntos de consulta.

**Why this priority**: El catálogo central es la fuente funcional que elimina la duplicación del inventario. Sin esta capacidad no existe una base confiable para adaptar los demás consumidores.

**Independent Test**: Puede verificarse consultando el catálogo central y comprobando que contiene una única entrada por cada uno de los once códigos esperados, con denominación, orden, actividad y aplicabilidad, sin depender de identificadores internos.

**Acceptance Scenarios**:

1. **Given** que el catálogo persistente contiene los once estados iniciales, **When** un usuario autorizado obtiene la consulta central de catálogos, **Then** recibe los once estados en el orden definido y cada estado incluye código, denominación, orden, actividad y aplicabilidad.
2. **Given** que cambia la denominación de un estado sin cambiar su código, **When** el usuario vuelve a consultar el catálogo, **Then** recibe la nueva denominación y la identidad funcional del estado permanece inalterada.
3. **Given** que dos entradas pretenden usar el mismo código, **When** se intenta conformar el catálogo, **Then** la duplicidad es rechazada y no se presenta un inventario ambiguo.
4. **Given** que el catálogo está vacío o no está disponible, **When** un consumidor solicita los estados, **Then** informa la condición de carga o error y no sustituye el catálogo con un inventario funcional local.

---

### User Story 2 - Usar estados coherentes en todo el portafolio (Priority: P2)

Como usuario que registra, consulta, filtra o cambia el estado de iniciativas y proyectos, quiero que formularios, filtros, listas, selectores, detalles, indicadores, dashboard, documentos y trazas presenten los estados desde la misma fuente para no encontrar etiquetas o alternativas contradictorias.

**Why this priority**: Una fuente central solo entrega valor completo cuando los consumidores dejan de tratar etiquetas o listas duplicadas como identidad funcional.

**Independent Test**: Puede verificarse cambiando la denominación persistida de un código y comprobando que todos los consumidores muestran la nueva denominación, mientras la selección por aplicabilidad y las transiciones permitidas conservan las reglas vigentes.

**Acceptance Scenarios**:

1. **Given** que la denominación de `SUSPENDED` cambia sin modificar el código, **When** el estado aparece en filtros, listas, detalles, dashboard, documentos o auditoría, **Then** todos esos consumidores muestran la denominación vigente del catálogo.
2. **Given** que un usuario crea una iniciativa o un proyecto, **When** se asigna el estado inicial, **Then** la asignación usa respectivamente `PRESENTED` o `PROJECT_IN_PROGRESS` y valida que el estado exista, esté activo y sea aplicable al tipo de registro.
3. **Given** que un estado está activo y es aplicable al tipo de registro pero no es un destino permitido por la matriz vigente, **When** se solicita la transición, **Then** la operación se rechaza por transición no autorizada.
4. **Given** que un destino está autorizado por la matriz pero está inactivo, **When** se solicita la transición, **Then** la operación se rechaza por estado inactivo.
5. **Given** que un estado no es aplicable al tipo de registro, **When** se intenta seleccionarlo o asignarlo, **Then** la operación se rechaza por aplicabilidad sin reinterpretarla como una transición válida.
6. **Given** que existe una asociación visual para un estado, **When** se representa el estado, **Then** la asociación se resuelve por el código estable y no convierte estilos o etiquetas locales en una fuente del inventario funcional.

---

### User Story 3 - Preservar históricos y reglas de transición (Priority: P3)

Como responsable funcional del portafolio, quiero cambiar metadata configurable del catálogo sin reescribir históricos ni ampliar las transiciones autorizadas, para mantener trazabilidad y control del ciclo de vida.

**Why this priority**: La configuración de presentación y disponibilidad debe evolucionar sin alterar el significado histórico de los registros ni convertir la metadata en reglas de negocio.

**Independent Test**: Puede verificarse desactivando un estado referenciado y comprobando que continúa legible en históricos, deja de estar disponible para nuevas asignaciones y solo permite salir de él cuando la matriz vigente lo autoriza y el destino es válido.

**Acceptance Scenarios**:

1. **Given** que un registro histórico referencia un estado que después se inactiva, **When** se consulta el registro, **Then** su respuesta incorpora por separado al menos el código, la denominación vigente y la actividad del estado, y no se modifica ni elimina la referencia histórica.
2. **Given** que el estado de origen de un registro está inactivo, **When** se solicita una transición permitida por la matriz hacia un destino activo y aplicable, **Then** la transición puede realizarse.
3. **Given** que el estado de origen está inactivo, **When** el destino no está autorizado por la matriz o no está activo o no es aplicable, **Then** la transición se rechaza por la causa correspondiente.
4. **Given** que se modifica el orden, actividad o aplicabilidad de un estado, **When** se evalúa una transición, **Then** esa metadata no crea una transición ausente en la matriz vigente.
5. **Given** que un estado está referenciado por registros, **When** se intenta eliminarlo o reutilizar su código con otro significado, **Then** la operación se rechaza y se conserva su identidad histórica.

---

### User Story 4 - Inicializar datos de prueba de forma controlada (Priority: P4)

Como integrante del equipo que trabaja en un entorno descartable de pruebas, quiero que la inicialización controlada cargue el catálogo esperado de estados para disponer de datos reproducibles sin introducir un mecanismo de provisión productiva.

**Why this priority**: La carga reproducible permite comprobar la feature en entornos autorizados, pero es secundaria frente al comportamiento funcional del catálogo y sus consumidores.

**Independent Test**: Puede verificarse ejecutando conceptualmente la inicialización tras una reconstrucción descartable con la activación exacta `test,test-reset`, comprobando los once estados y repitiéndola sin duplicados; fuera de esa activación no debe ejecutarse.

**Acceptance Scenarios**:

1. **Given** un entorno descartable reconstruido y la activación exacta y ordenada `test,test-reset`, **When** se ejecuta la inicialización autorizada, **Then** quedan disponibles exactamente los once códigos con sus denominaciones, orden, actividad y aplicabilidad esperados.
2. **Given** que la inicialización ya se ejecutó correctamente, **When** se repite con los mismos datos, **Then** no crea duplicados ni altera la identidad de los estados.
3. **Given** que los datos cargados difieren de los valores esperados o la postvalidación no encuentra exactamente los once estados, **When** termina la inicialización, **Then** el proceso falla de forma cerrada e informa la discrepancia.
4. **Given** cualquier activación distinta de la combinación exacta y ordenada `test,test-reset`, **When** inicia la aplicación, **Then** la inicialización externa del catálogo no se ejecuta.
5. **Given** un entorno normal o productivo, **When** se despliega la feature, **Then** no se carga automáticamente este conjunto de prueba ni se considera resuelta la provisión institucional de datos.

### Edge Cases

- Catálogo vacío: ningún consumidor debe reconstruir los estados desde etiquetas, enumeraciones, listas locales o valores históricos; debe presentar una condición explícita de vacío o indisponibilidad.
- Catálogo no disponible: la interfaz y los procesos dependientes deben distinguir un fallo de carga de la ausencia legítima de opciones y deben impedir asignaciones no validadas.
- Código duplicado: debe rechazarse antes de que dos estados puedan compartir identidad funcional.
- Cambio de denominación: debe propagarse a los consumidores sin cambiar el código ni invalidar referencias históricas.
- Cambio o reutilización de código: debe rechazarse porque el código es inmutable y no puede adquirir otro significado.
- Estado activo fuera de matriz: no se convierte en destino autorizado por estar activo o ser aplicable.
- Destino autorizado pero inactivo: no puede seleccionarse ni recibir una nueva asignación.
- Estado no aplicable: no puede seleccionarse ni asignarse para ese tipo de registro aunque esté activo.
- `NOT_APPLICABLE`: permanece excluido de asignaciones y transiciones tanto de iniciativas como de proyectos.
- Histórico inactivo: permanece visible y referenciable; no aparece como opción para nuevas asignaciones.
- Origen inactivo: puede abandonarse únicamente cuando la matriz autoriza el destino y este se encuentra activo y es aplicable.
- Eliminación de un estado referenciado: debe impedirse para conservar integridad y trazabilidad.
- Reejecución de la carga de prueba: debe converger al mismo inventario sin duplicados.
- Discrepancia entre valores esperados y cargados: la inicialización autorizada debe fallar cerrada, no corregir silenciosamente significados incompatibles.
- Inicialización fuera de perfiles: no debe ejecutar DML ni producir efectos parciales.
- Consumidor aún dependiente de etiquetas o listas locales: debe identificarse como incumplimiento porque una denominación modificable no puede actuar como identidad.
- Asociación visual sin código conocido: puede usar una presentación neutral para tolerancia visual, pero no debe inventar un estado ni habilitar una acción funcional.

## Requirements *(mandatory)*

### Inventario inicial

Los once estados se cargan activos inicialmente. La aplicabilidad `Ninguno` significa que el estado no puede asignarse ni participar en transiciones de iniciativas o proyectos, aunque esté activo.

| Orden | Código técnico | Denominación inicial | Actividad inicial | Aplicabilidad |
|------:|----------------|----------------------|-------------------|----------------|
| 1 | `PRESENTED` | Presentado | Activo | Iniciativa |
| 2 | `INITIATIVE_APPROVED` | Iniciativa aprobada | Activo | Iniciativa |
| 3 | `INITIATIVE_ARCHIVED` | Iniciativa archivada | Activo | Iniciativa |
| 4 | `PROJECT_IN_PROGRESS` | Proyecto en ejecución | Activo | Proyecto |
| 5 | `PRODUCT_APPROVED` | Producto aprobado | Activo | Proyecto |
| 6 | `PRODUCT_NOT_APPROVED` | Producto no aprobado | Activo | Proyecto |
| 7 | `SUSPENDED` | Suspendido | Activo | Proyecto |
| 8 | `CANCELLED` | Cancelado | Activo | Proyecto |
| 9 | `FINISHED` | Finalizado | Activo | Proyecto |
| 10 | `NOT_APPLICABLE` | No Aplicable | Activo | Ninguno |
| 11 | `NOT_ADMISSIBLE` | No Admisible | Activo | Iniciativa |

### Matrices vigentes preservadas

La existencia, actividad, orden o aplicabilidad de un estado nunca autoriza una transición. Solo las siguientes matrices siguen determinando los cambios permitidos.

#### Iniciativas

- Sin proyecto asociado, `PRESENTED` puede transitar a `INITIATIVE_APPROVED`, `NOT_ADMISSIBLE` o `INITIATIVE_ARCHIVED`.
- Sin proyecto asociado, `INITIATIVE_APPROVED` puede transitar a `INITIATIVE_ARCHIVED`.
- `NOT_ADMISSIBLE` e `INITIATIVE_ARCHIVED` son terminales.
- Cuando la iniciativa tiene proyecto asociado, permanece en `INITIATIVE_APPROVED` y no recibe nuevas transiciones de iniciativa.

#### Proyectos

- `PROJECT_IN_PROGRESS` puede transitar a `PRODUCT_APPROVED`, `PRODUCT_NOT_APPROVED`, `SUSPENDED` o `CANCELLED`.
- `SUSPENDED` puede transitar a `PROJECT_IN_PROGRESS` o `CANCELLED`.
- `PRODUCT_NOT_APPROVED` puede transitar a `PROJECT_IN_PROGRESS` o `CANCELLED`.
- `PRODUCT_APPROVED` puede transitar a `FINISHED`.
- `CANCELLED` y `FINISHED` son terminales.
- `NOT_APPLICABLE` no participa en ninguna matriz.

### Functional Requirements

- **FR-001**: El sistema DEBE mantener un catálogo persistente único de estados del portafolio con exactamente una identidad funcional por código técnico.
- **FR-002**: Cada estado DEBE contener código técnico, denominación, orden, actividad y aplicabilidad a iniciativa, proyecto o ninguno.
- **FR-003**: El código técnico DEBE ser estable e inmutable, no DEBE reutilizarse con otro significado y DEBE ser la identidad compartida entre contratos, reglas, registros y consumidores.
- **FR-004**: Los identificadores internos y las denominaciones NO DEBEN actuar como identidad funcional compartida ni ser necesarios para interpretar un estado fuera del catálogo.
- **FR-005**: El inventario inicial DEBE contener los once códigos, denominaciones, orden y aplicabilidad definidos en esta especificación, sin agregar, omitir o reinterpretar estados.
- **FR-006**: Los once estados del inventario DEBEN cargarse activos inicialmente; su disponibilidad para selección y transición DEBE seguir restringida por aplicabilidad y matriz.
- **FR-007**: La denominación, el orden, la actividad y la aplicabilidad DEBEN poder persistirse y consultarse como metadata del catálogo, sin convertir ninguno de esos atributos en autorización de transiciones.
- **FR-008**: La consulta central existente de catálogos DEBE incluir únicamente los estados activos y devolverlos en orden ascendente, con los cinco atributos definidos para cada entrada.
- **FR-009**: La consulta central DEBE identificar cada estado por código y NO DEBE exigir que sus consumidores conozcan un identificador interno.
- **FR-010**: Formularios, filtros, listas, selectores, detalles, indicadores, dashboard, documentos, auditoría visible y controles de transición DEBEN resolver el inventario activo y sus denominaciones desde el catálogo central. Cuando un registro histórico referencie un estado inactivo, su respuesta de consulta DEBE incorporar por separado al menos el código, la denominación vigente y la actividad del estado sin ofrecerlo como opción seleccionable.
- **FR-011**: Los consumidores NO DEBEN mantener inventarios funcionales alternativos, mapas etiqueta-a-código ni fallbacks de estados que suplan un catálogo vacío o no disponible.
- **FR-012**: Las asociaciones exclusivamente visuales PUEDEN permanecer definidas localmente si se vinculan al código estable; una presentación neutral para un código desconocido NO DEBE inventar estados, opciones ni permisos.
- **FR-013**: Toda nueva selección o asignación DEBE validar de forma independiente que el código exista, esté activo y sea aplicable al tipo de registro.
- **FR-014**: Toda transición DEBE validar además que el par origen-destino esté autorizado por la matriz vigente y DEBE conservar exactamente las matrices declaradas en esta especificación.
- **FR-015**: El sistema DEBE distinguir al menos las causas de rechazo por código inexistente, estado inactivo, estado no aplicable y transición no autorizada, sin tratar una causa como sustituto de otra.
- **FR-016**: Una iniciativa nueva DEBE iniciar en `PRESENTED` y un proyecto nuevo, derivado o preexistente DEBE iniciar en `PROJECT_IN_PROGRESS`, sujetos a las validaciones de existencia, actividad y aplicabilidad.
- **FR-017**: `NOT_APPLICABLE` DEBE estar excluido de nuevas asignaciones y de todas las transiciones de iniciativa y proyecto.
- **FR-018**: Un estado inactivo NO DEBE incluirse en la consulta central ni ofrecerse para nuevas selecciones o asignaciones, pero DEBE permanecer legible cuando esté referenciado por un registro histórico mediante la metadata incluida en la respuesta de ese registro.
- **FR-019**: La inactivación o el cambio de metadata NO DEBE modificar el código almacenado, reasignar registros existentes ni eliminar estados referenciados.
- **FR-020**: Un registro con estado de origen inactivo PUEDE transitar solo cuando la matriz vigente autoriza el destino y este existe, está activo y es aplicable.
- **FR-021**: La metadata del catálogo NO DEBE crear, eliminar ni inferir transiciones; las matrices continúan siendo reglas funcionales controladas y no administrables por esta feature.
- **FR-022**: Los dos artefactos de inicialización de pruebas, `apps/backend/src/main/resources/db/test/catalog-data.sql` y `database/dml/seed/catalog-data.sql`, DEBEN permanecer coordinados e incorporar el mismo inventario esperado de once estados.
- **FR-023**: La inicialización externa de estos datos DEBE ejecutarse únicamente después de una reconstrucción descartable y bajo la activación exacta y ordenada `test,test-reset`; cualquier otra activación DEBE impedir su ejecución.
- **FR-024**: La inicialización DEBE ser externa, versionada, solo de datos, idempotente y basada en códigos naturales; NO DEBE contener definición estructural, bloques procedimentales ni identificadores numéricos internos fijados manualmente.
- **FR-025**: La inicialización DEBE postvalidar exactamente los once códigos, denominaciones, orden, actividad y aplicabilidad esperados; una ausencia, duplicidad o discrepancia DEBE producir un fallo cerrado.
- **FR-026**: La reejecución de la inicialización autorizada DEBE conservar exactamente una entrada por código y NO DEBE duplicar estados.
- **FR-027**: La estructura persistente DEBE continuar derivándose del modelo canónico de la aplicación; la excepción de datos de prueba NO DEBE convertirse en una vía alternativa para definir estructura.
- **FR-028**: Los entornos `dev` y `prod` DEBEN conservar la validación de estructura existente, NO DEBEN autoejecutar la inicialización externa y NO DEBEN recibir DML productivo como parte de esta feature.
- **FR-029**: La feature NO DEBE declararse completamente preparada para producción hasta que una feature posterior aprobada defina la provisión o migración del catálogo en esquemas institucionales no descartables.
- **FR-030**: La implementación futura DEBE actualizar coordinadamente la guía funcional y la documentación de arquitectura para reflejar la fuente única, la identidad por código, la aplicabilidad, la actividad, el comportamiento histórico y la separación de las matrices.

### Key Entities

- **Estado del portafolio**: Entrada persistente que representa un estado funcional. Sus atributos son código técnico inmutable, denominación configurable, orden configurable, actividad configurable y aplicabilidad configurable. Puede estar referenciado por cero o más registros del portafolio.
- **Registro del portafolio**: Iniciativa o proyecto que mantiene un estado actual identificado funcionalmente por el código del catálogo. Su tipo determina qué aplicabilidad acepta y su estado actual participa en las reglas de transición. Su representación de consulta incorpora por separado la metadata vigente cuando el estado referenciado está inactivo.
- **Aplicabilidad del estado**: Clasificación que indica si un estado puede asignarse a iniciativas, proyectos o a ninguno. Restringe selección y asignación, pero no habilita transiciones.
- **Matriz de transición**: Regla funcional controlada que relaciona tipo de registro, estado de origen, estado de destino y condiciones del ciclo de vida. No es metadata administrable del catálogo.
- **Inicialización descartable de catálogos**: Conjunto versionado de datos esperados para entornos de prueba autorizados. Usa códigos naturales y verifica que el resultado coincida exactamente con el inventario aprobado.

## Scope and Impact

### Impacto esperado por área

- **Backend**: incorporar la persistencia y consulta de los cinco atributos del estado; resolver por código las lecturas, asignaciones, transiciones, dashboard, documentos y auditoría visible; conservar las matrices como reglas independientes.
- **Contrato**: ampliar la consulta central con estados activos identificados por código y su metadata; incorporar en las respuestas históricas la metadata vigente del estado inactivo; preservar códigos estables en filtros y operaciones y ofrecer rechazos distinguibles para validaciones funcionales.
- **Frontend**: consumir el catálogo central para inventarios activos en formularios, filtros, listas, selectores, detalles, indicadores, dashboard, documentos y transiciones; presentar estados históricos inactivos con la metadata de la respuesta del registro; dejar de usar etiquetas, inventarios locales y fallbacks como fuente funcional; conservar solo asociaciones visuales por código.
- **Datos**: representar el catálogo persistente y sus relaciones sin cambiar códigos históricos; coordinar los dos artefactos de inicialización descartable y sus postvalidaciones.
- **Documentación**: actualizar `docs/architecture/piip-fields.md`, la guía funcional en `docs/funcional/` y la documentación operativa afectada cuando se implemente la feature, con evidencia verificable del comportamiento final.

### Production Readiness

Esta feature define el comportamiento funcional y la inicialización exclusiva de pruebas, pero su preparación productiva queda explícitamente incompleta. `dev` y `prod` conservan su validación estructural y no cargan automáticamente el catálogo. La provisión o migración de los once estados en esquemas institucionales no descartables requiere una feature posterior aprobada y está fuera de este alcance. Esta limitación no modifica la Constitución ni autoriza mecanismos adicionales de DML productivo.

### Out of Scope

- Una interfaz de administración para crear, editar, activar, inactivar, ordenar o cambiar aplicabilidad.
- Endpoints o capacidades de escritura administrativa del catálogo.
- Nuevos roles, permisos o reglas administrativas.
- Cambios a los once códigos, sus significados iniciales o las matrices vigentes.
- Configuración o administración de matrices de transición.
- DML directo o automático en producción.
- Provisión, migración o reconciliación de datos en esquemas institucionales no descartables.
- Reapertura o modificación retroactiva de las features históricas usadas como grounding.
- Declarar preparación productiva completa.

## Risks and Controls

- **Riesgo de identidad por etiqueta**: un cambio de denominación podría romper filtros, documentos o transiciones. Se controla haciendo del código la única identidad funcional compartida.
- **Riesgo de doble fuente**: listas o fallbacks locales podrían divergir del catálogo. Se controla prohibiendo inventarios funcionales alternativos y verificando todos los consumidores identificados.
- **Riesgo de ampliar transiciones**: actividad o aplicabilidad podrían interpretarse como permiso. Se controla manteniendo las matrices separadas y exigiendo ambas validaciones.
- **Riesgo histórico**: desactivar o renombrar podría ocultar o reescribir registros previos. Se controla conservando referencias por código y legibilidad de estados inactivos.
- **Riesgo operativo**: la carga descartable podría ejecutarse fuera de pruebas o aceptar datos incompatibles. Se controla con activación exacta, postvalidación completa y fallo cerrado.
- **Riesgo productivo**: la existencia del seed de prueba podría confundirse con provisión institucional. Se controla declarando la preparación incompleta y separando una feature posterior.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con el inventario inicial completamente activo, la consulta central presenta exactamente 11 estados únicos, en el orden 1 a 11 definido, y el 100 % contiene código, denominación, orden, actividad y aplicabilidad; tras una inactivación, presenta exclusivamente los estados que permanecen activos.
- **SC-002**: El 100 % de los escenarios de formularios, filtros, listas, selectores, detalles, indicadores, dashboard, documentos, auditoría visible y transiciones obtiene el inventario activo y sus denominaciones desde el catálogo central; los estados históricos inactivos se presentan con la metadata de la respuesta del registro y no existen inventarios funcionales alternativos.
- **SC-003**: Al cambiar únicamente una denominación, el 100 % de los consumidores visibles muestra el nuevo texto sin cambiar códigos, referencias históricas ni matrices.
- **SC-004**: El 100 % de las nuevas asignaciones inválidas se rechaza por una causa distinguible entre inexistencia, inactividad, no aplicabilidad y transición no autorizada.
- **SC-005**: Las matrices resultantes permiten exactamente las transiciones enumeradas en esta especificación y cero transiciones adicionales, aun cuando un estado esté activo y sea aplicable.
- **SC-006**: El 100 % de las respuestas de registros que referencian un estado posteriormente inactivo incorpora al menos su código, denominación vigente y actividad; ninguno de esos estados aparece en la consulta central ni es reasignado o eliminado por el cambio de metadata.
- **SC-007**: Dos ejecuciones consecutivas de la inicialización autorizada producen el mismo conjunto de 11 códigos sin duplicados, y cualquier discrepancia de sus atributos impide completar la inicialización.
- **SC-008**: En el 100 % de las activaciones distintas de `test,test-reset`, incluidos los entornos normales y productivos, la inicialización externa de estados realiza cero modificaciones.
- **SC-009**: La revisión de trazabilidad identifica cobertura explícita para el 100 % de los consumidores y documentos afectados señalados en la sección de impacto.
- **SC-010**: Una persona usuaria puede reconocer el estado vigente y filtrar por opciones aplicables sin encontrar diferencias de denominación entre dos vistas del mismo registro.

## Assumptions

- Los once códigos, denominaciones iniciales, orden y aplicabilidad de esta especificación están ratificados por el comportamiento y la documentación vigentes; no constituyen estados o reglas nuevos.
- El acceso de lectura a la consulta central conserva la autenticación y autorización funcional existentes; esta feature no crea permisos.
- La denominación, el orden, la actividad y la aplicabilidad son metadata configurable persistida, aunque la capacidad administrativa para modificarlos no forma parte de esta feature.
- Los estados históricos se muestran con la denominación vigente del código, sin conservar versiones temporales de etiquetas, porque no se solicitó versionado de metadata.
- Las asociaciones de color, iconografía o estilo pueden permanecer locales si usan el código estable y no determinan inventario, disponibilidad o autorización.
- Las features históricas 009, 011 y 015 y la Constitución se usan solo como grounding; no se reabren ni amplían mediante esta especificación.

## Dependencies

- La consulta central de catálogos existente debe admitir la incorporación del catálogo de estados.
- El ciclo de vida vigente y sus matrices ratificadas continúan siendo la autoridad para transiciones.
- La inicialización descartable existente conserva la activación exacta y ordenada `test,test-reset`, la reconstrucción previa y el comportamiento de fallo cerrado.
- Una feature posterior aprobada deberá resolver la provisión o migración productiva antes de declarar preparación completa para esquemas institucionales no descartables.
