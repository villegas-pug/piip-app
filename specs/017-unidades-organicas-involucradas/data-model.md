# Modelo de datos: Unidades Orgánicas Involucradas

Derivado de `spec.md` (Key Entities, FR-001..FR-033). **Sin cambios estructurales**: la feature reutiliza el esquema JPA vigente; Hibernate JPA permanece como fuente canónica (constitución IV). No se agregan tablas, columnas, constraints ni migraciones; no se modifican datos históricos.

## Entidades

### Unidad Orgánica (maestro institucional) — sin cambios

Tabla `UNIDAD_ORGANICA` (entidad `OrganizationalUnitEntity`).

| Campo | Tipo/restricción vigente | Papel en la feature |
|---|---|---|
| id | identity, PK | Identidad referida por cada fila de la lista. |
| executingUnitId | FK a Unidad Ejecutora, not null | Pertenencia: toda unidad incorporada debe ser de la misma UE del registro. |
| parentId | FK opcional a Unidad Orgánica | Sin papel; no se usa para la lista. |
| codigo | not null, único dentro de su UE (UK) | Identidad organizacional; no editable por registro. |
| nombre | not null | **Descripción** de la fila (solo lectura, del maestro). |
| sigla (acronym) | **nullable** | **Abreviatura** de la fila (solo lectura, del maestro). Regla de asociación: no vacía para nuevas incorporaciones; nunca se inventa ni completa. |
| activo | not null | Solo unidades activas pueden incorporarse como nuevas asociaciones. |
| version | @Version optimista | Control del maestro; sin cambios. |

### Unidades Orgánicas Involucradas (asociación ordenada) — sin cambios estructurales, semántica extendida

Tabla `REGISTRO_UNIDAD_RESPONSABLE` (entidad `ResponsibleUnitEntity`, nombres técnicos conservados).

| Campo | Tipo/restricción vigente | Semántica con la feature |
|---|---|---|
| id | identity, PK | Identidad de la asociación. |
| registroId | FK al registro del portafolio, not null | Registro (iniciativa, proyecto derivado o preexistente). |
| unidadOrganicaId | FK a Unidad Orgánica, not null | Unidad incorporada; sin duplicados dentro del registro (validado en servicio). |
| denominacionOriginal | not null (copia del nombre al asociar) | Copia histórica de la denominación; se conserva tal cual, sin saneos. |
| ordenPresentacion | int not null, UK (registroId, ordenPresentacion) | **Nro** de presentación: secuencia continua 1..N asignada por el orden de incorporación; recalculada al reinsertar el conjunto. |

- Cardinalidad por registro: **1..N** (mínimo una, sin máximo funcional). El mínimo y la unicidad se imponen en DTO/servicio; el esquema ya permite N filas y garantiza orden único por registro.
- El Nro visible no se persiste aparte: deriva de la posición (orden 1..N).

### Registro del portafolio — sin cambios

Entidad vigente (`PortfolioRecordEntity`): código, tipo, origen, Unidad Ejecutora (inmutable), estado, fecha de cierre (inmutables), `version` para control de concurrencia (FR-026). La lista es un aspecto del registro, no una entidad nueva.

### Evento de auditoría — sin cambios de tabla, detalle extendido

Evento append-only vigente. El **detalle** de los eventos de registro/actualización incorpora:

- Altas: lista ordenada de unidades confirmadas; cada elemento identifica unidad (identidad y código), nombre, sigla y Nro de presentación (FR-018, FR-020).
- Modificaciones: valor anterior y nuevo de la lista completa, con los mismos atributos por elemento (FR-019, FR-020).
- Se conservan actor, fecha, Unidad Ejecutora y contexto vigentes; sin cuerpos de solicitudes ni información sensible (FR-021, constitución V).

## Reglas de validación por capa

| Regla (FR) | DTO (contrato HTTP) | Servicio de aplicación | UI |
|---|---|---|---|
| Mínimo una unidad (FR-002) | `responsibleUnits` con mínimo un elemento en altas; en edición, mínimo uno si el campo está presente | Rechaza lista vacía | Bloquea confirmación sin filas; retiro de la última fila no disponible (FR-004) |
| Sin máximo funcional (FR-002) | Sin límite superior | Sin límite superior | Lista dinámica |
| Sin duplicados (FR-003) | — | Rechaza unidad repetida en el conjunto completo confirmado, identificando la fila | Opciones ya seleccionadas no se reofrecen (FR-012) |
| Unidad existente | `organizationalUnitId` válido | Resuelve por identidad, incluidas inactivas para lectura; nueva incorporación inexistente → rechazo | — |
| Activa (FR-010) | — | Nuevas incorporaciones: solo activas; históricas retenidas inactivas se conservan (FR-022, FR-024) | Catálogo solo activas; histórico inactivo visible como contexto |
| Misma UE (FR-010) | — | Nueva incorporación de otra UE → rechazo | Catálogo filtrado por UE del registro |
| Sigla no vacía (FR-011) | — | Nuevas incorporaciones sin sigla → rechazo; nunca se inventa/completa | Unidades sin sigla ocultas del catálogo (Q2=A) |
| Orden 1..N continuo (FR-005) | — | Asigna/reasigna posiciones al confirmar; UK (registro, orden) lo respalda | Renumeración automática al retirar filas |
| Atomicidad (FR-007) | — | Resuelve y valida toda la lista antes de modificar persistencia; error → conjunto anterior intacto | Errores por fila sin perder selecciones válidas (FR-017) |
| Campo ausente en edición (FR-009) | Presencia JSON preservada vigente | Campo ausente conserva asociaciones | Envío disperso solo si la lista cambió |

## Compatibilidad histórica

- Los registros históricos (incluidos los de más de una unidad, escenarios ya previstos por la feature 013) permanecen legibles sin migración, renumeración ni saneo (FR-022, FR-023).
- Una asociación histórica inactiva permanece visible como contexto y no se reofrece para nuevas asociaciones (FR-024).
- Editar otros campos sin incluir la lista conserva íntegramente las asociaciones (FR-009).

## Datos sintéticos (reset `test,test-reset`)

- El seed DML vigente (sección de Unidades Orgánicas de `apps/backend/src/main/resources/db/test/catalog-data.sql`) se reutiliza sin cambios de valores: 4 UOs sintéticas (código, nombre y sigla no vacíos; código único dentro de su UE; MERGE idempotente por UE+código; 2 activas por cada una de las 2 UEs sintéticas).
- La postvalidación de `TestResetCoordinator` se extiende (FR-030): sigla no vacía por UO sintética, asociación UO–UE correcta, mínimo dos activas por UE sintética, además de conteos y vaciado de tablas operativas vigentes; cualquier detección → inicialización incompleta (fallo seguro).
- Las tablas operativas (incluida `REGISTRO_UNIDAD_RESPONSABLE`) permanecen vacías tras la inicialización (FR-030).
