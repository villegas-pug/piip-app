# Modelo de datos: Centralización de estados del portafolio

**Rama**: `018-centralizar-estados-portafolio` | **Fecha**: 2026-09-10 | **Spec**: [spec.md](./spec.md)

Hibernate JPA es la fuente canónica del esquema: toda la estructura proviene de entidades; el DDL revisable se regenera a `database/generated/piip-oracle.sql` desde el artefacto del build. Sin SQL nativo ni DDL manual.

## Entidad nueva: Estado del portafolio

- **Tabla**: `ESTADO_PORTAFOLIO`
- **Entidad**: `pe.gob.midagri.piip.portfolio.persistence.PortfolioStatusCatalogEntity`
- **Clave primaria**: natural, por código (`CODIGO`), sin secuencia ni identificador interno.
- **Sin** endpoints ni capacidades de escritura administrativa.

| Atributo | Columna | Tipo y restricción | Descripción |
|----------|---------|--------------------|-------------|
| Código | `CODIGO` | `VARCHAR2(40)` PK; tipado como enum `PortfolioStatus` | Identidad funcional inmutable; uno de los once códigos oficiales |
| Denominación | `NOMBRE` | `VARCHAR2(180)` NOT NULL | Texto visible configurable |
| Orden | `ORDEN_PRESENTACION` | entero NOT NULL, `CHECK (>= 0)` | Posición de presentación |
| Actividad | `ACTIVO` | booleano NOT NULL | Disponibilidad para nuevas asignaciones |
| Aplicabilidad | `APLICABILIDAD` | enum NOT NULL: `INITIATIVE`, `PROJECT`, `NONE` | Tipo de registro al que puede asignarse |

**Índice de consulta**: `(ACTIVO, APLICABILIDAD, ORDEN_PRESENTACION, CODIGO)` para resolver el bundle activo ordenado.

**Reglas de integridad**: el código no tiene setter ni vía de cambio (FR-003); la unicidad de identidad queda garantizada por la PK natural (edge case de código duplicado); la eliminación de un estado referenciado la impide la FK descrita abajo.

## Enum de aplicabilidad (nuevo)

`INITIATIVE` (Iniciativa), `PROJECT` (Proyecto), `NONE` (Ninguno). Restringe selección y asignación; nunca habilita transiciones (FR-021).

## Enum `PortfolioStatus` (conservado)

Las once constantes siguen siendo el tipo del código y la sede de las matrices y estados iniciales. `label()` deja de usarse como fuente de denominación visible y `values()` deja de usarse como inventario funcional (FR-011). El significado de cada constante no cambia.

## Relación con los registros del portafolio

`REGISTRO_PORTAFOLIO.ESTADO` (`VARCHAR2(40)` NOT NULL) conserva nombre, tipo y valores actuales (los códigos del enum). Se añade la asociación `statusCatalog`:

- `@ManyToOne` de solo lectura: `insertable = false`, `updatable = false`, join column `ESTADO` → `ESTADO_PORTAFOLIO.CODIGO`.
- Genera la **FK de integridad** por código natural: impide eliminar estados referenciados y permite cargar la metadata vigente sin cambiar valores históricos (FR-019).
- Cardinalidad: 1 estado → 0..N registros del portafolio.
- El índice existente `(TIPO_REGISTRO, ESTADO)` se conserva.

## Repositorios y consultas

- `PortfolioStatusRepository.findByCode(PortfolioStatus)`: resolución y validación por código.
- `PortfolioStatusRepository.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc()`: bundle central (solo activos, ordenados).
- `PortfolioStatusRepository.findAllByOrderByDisplayOrderAscCodeAsc()`: postvalidación del reset y orden de dashboard.
- `PortfolioRecordRepository`: los `EntityGraph` existentes agregan `statusCatalog`; las queries de dashboard cargan/joinean la metadata para evitar N+1.

## Reglas de validación (servicios de aplicación)

- **Creación**: iniciativa → `PRESENTED`; proyecto derivado o preexistente → `PROJECT_IN_PROGRESS`; se valida existencia → actividad → aplicabilidad (FR-016).
- **Aprobación**: `INITIATIVE_APPROVED` se valida igual antes de aprobar.
- **Transición**: el destino se valida existencia → actividad → aplicabilidad → matriz; el origen inactivo no se rechaza y puede abandonarse con destino válido (FR-020).
- **Causas distinguibles (422)**: `PORTFOLIO_STATUS_NOT_FOUND`, `PORTFOLIO_STATUS_INACTIVE`, `PORTFOLIO_STATUS_NOT_APPLICABLE`, `PORTFOLIO_STATUS_TRANSITION_NOT_ALLOWED` (FR-015).
- **`NOT_APPLICABLE`**: activo con aplicabilidad `NONE`; se rechaza por aplicabilidad antes de evaluar matriz (FR-017).
- **Elegibilidad**: `eligibleInitiatives()` selecciona por código `INITIATIVE_APPROVED` aunque su metadata esté inactiva (decisión D10 de [research.md](./research.md)).

## Read models y referencias de contrato

- **Catálogo (bundle central)**: `PortfolioStatusCatalogResponse(code, name, displayOrder, active, applicability)`; solo activos.
- **Referencia (por registro)**: `PortfolioStatusReferenceResponse(code, name, active)`; incluye inactivos referenciados con denominación vigente.
- Ninguna de las dos expone el identificador interno de fila; el código es la identidad contractual (FR-004, FR-009).

## Auditoría

- **Eventos nuevos**: `detailJson` agrega `statusCode`, `previousStatusCode`, `newStatusCode` (códigos estables) conservando el detalle técnico actual; append-only, sin reescritura de eventos existentes (principio V).
- **Lectura**: `EventView` y la respuesta HTTP agregan `status`, `previousStatus`, `newStatus` como referencias con metadata vigente; los eventos legados sin código se muestran con su texto histórico, sin mapeo etiqueta→código (decisión D13).

## Inventario inicial (seed descartable)

Once estados, todos activos, en el orden y con la aplicabilidad de la spec:

| Orden | Código | Denominación | Actividad | Aplicabilidad |
|------:|--------|--------------|-----------|---------------|
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

## Inicialización descartable y derivación estructural

- `ESTADO_PORTAFOLIO` entra a la allowlist y a los órdenes de creación/eliminación del reset: drop después de `REGISTRO_PORTAFOLIO` (hijo primero), create antes del registro. Conteo de tablas del reset: **20 → 21**.
- Once MERGE por código natural con solo `WHEN NOT MATCHED INSERT` en **ambos** artefactos: `apps/backend/src/main/resources/db/test/catalog-data.sql` y `database/dml/seed/catalog-data.sql` (FR-022 a FR-026).
- Postvalidación fail-closed: exactamente los once códigos con nombre/orden/actividad/aplicabilidad esperados, cero extras; discrepancia = fallo cerrado.
- Ejecución exclusiva tras reconstrucción descartable bajo la activación exacta y ordenada `test,test-reset`; `dev`/`prod` mantienen `ddl-auto=validate` y no cargan seed (FR-028). La provisión institucional queda para una feature posterior (FR-029).
