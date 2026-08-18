# Modelo de datos: Actualización de Inicio PIIP

## Alcance

La feature no modifica el modelo persistente. Reutiliza `PortfolioRecordEntity`, `ExecutingUnitEntity`, `NotificationEntity`, `RecordType` y `PortfolioStatus`. Los siguientes son modelos de consulta y presentación, no nuevas entidades JPA.

## Consulta de portafolio de Inicio

| Campo | Tipo | Regla |
|-------|------|-------|
| `executingUnitId` | `Long` | Obligatorio, positivo y legible por el usuario autenticado. |
| `q` | `String?` | Opcional; se normaliza y busca solo en código o nombre. |
| `type` | `RecordType?` | `INITIATIVE`, `PROJECT` o ausente para Todos. |
| `status` | `PortfolioStatus?` | Código canónico aplicable al tipo consultado. |
| `page` | `int` | Base cero, predeterminado 0. |
| `size` | `int` | Predeterminado 5; rango 1..100. |

El orden no forma parte de la entrada: siempre es `updatedAt DESC, id DESC`.

## Resultado de portafolio de Inicio

### `HomePortfolioResponse`

| Campo | Tipo | Regla |
|-------|------|-------|
| `content` | `HomePortfolioItemResponse[]` | Registros de la página solicitada o normalizada. |
| `page` | `int` | Página efectiva. Una página inexistente vuelve a 0. |
| `size` | `int` | Tamaño efectivo. |
| `totalElements` | `long` | Total filtrado; equivale a la suma de `statusCounts`. |
| `totalPages` | `int` | Derivado de `totalElements` y `size`. |
| `executingUnitTotalElements` | `long` | Total de la UE antes de aplicar `q`, `type` y `status`. |
| `statusCounts` | `PortfolioStatusCountResponse[]` | Solo estados con conteo positivo, en orden canónico. |

### `HomePortfolioItemResponse`

| Campo | Tipo | Regla |
|-------|------|-------|
| `recordType` | `String` | Etiqueta canónica `Iniciativa` o `Proyecto`. |
| `code` | `String` | Código real del registro. |
| `name` | `String` | Nombre real del registro. |
| `status` | `String` | Etiqueta canónica del estado vigente. |
| `executingUnitId` | `Long` | Debe coincidir con la UE consultada. |
| `executingUnit` | `String` | Nombre real de la UE. |
| `updatedAt` | `Instant` | Dato técnico usado para orden y trazabilidad. |

### `PortfolioStatusCountResponse`

| Campo | Tipo | Regla |
|-------|------|-------|
| `status` | `String` | Etiqueta canónica, sin alias ilustrativos. |
| `count` | `long` | Estrictamente mayor que cero. |

## Invariantes

1. Todos los elementos pertenecen a `executingUnitId` y al alcance autorizado.
2. `sum(statusCounts.count) == totalElements`.
3. `executingUnitTotalElements == 0` implica portafolio realmente vacío.
4. `executingUnitTotalElements > 0 && totalElements == 0` implica filtros sin coincidencias.
5. `content.length <= size` y ningún conteo se limita a la página visible.
6. El filtro de estado no autoriza transiciones; solo consulta estados existentes.

## Notificación personal existente

| Campo | Tipo | Uso en Inicio |
|-------|------|---------------|
| `id` | `Long` | Identifica la lectura explícita por fila. |
| `type` | `String` | Tipo real del aviso. |
| `message` | `String` | Contenido mostrado sin inventar referencias. |
| `read` | `boolean` | Determina pestaña, estado y contador. |
| `createdAt` | `Instant` | Orden y fecha visible. |

### Transición permitida

```text
NO_LEÍDA --PUT /notifications/{id}/read--> LEÍDA
```

Renderizar, enfocar, filtrar o expandir no produce transición. La operación afecta únicamente una notificación del destinatario autenticado.

## Impacto de persistencia

- Sin nuevas tablas, columnas, claves, relaciones o catálogos.
- Sin cambios en el esquema derivado de Hibernate.
- No se propone índice nuevo sin evidencia de volumen y plan de ejecución Oracle.
