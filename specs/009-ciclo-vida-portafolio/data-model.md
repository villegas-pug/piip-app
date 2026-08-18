# Modelo de datos y dominio: Ciclo de vida del portafolio PIIP

## Principio de persistencia

La feature reutiliza el modelo JPA existente. No agrega tablas, columnas, secuencias, triggers, procedimientos ni un segundo control de versiones. `database/generated/piip-oracle.sql` no tiene cambio lógico previsto.

## Registro de portafolio (`PortfolioRecordEntity` / `REGISTRO_PORTAFOLIO`)

| Atributo | Persistencia | Uso en la feature |
|---|---|---|
| `id` | PK existente | Identidad y referencia de origen. |
| `recordType` | tipo de registro | Separa `INITIATIVE` de `PROJECT`. |
| `code` | código único | Identificador HTTP y de auditoría. |
| `originRecord` / `originCode` | relación opcional | “Proyecto derivado” es esta relación, no un estado. |
| `status` | `ESTADO` | Solo cambia mediante comportamiento de dominio. |
| `closingDate` | `FECHA_CIERRE` | Solo se establece al entrar en `FINISHED`. |
| `executingUnit` | Unidad Ejecutora | Base de autorización y auditoría. |
| `updatedAt` | instante de actualización | Instante efectivo de transición. |
| `version` | `@Version VERSION` | Único control optimista. |

### Invariantes y matriz de iniciativa

Una iniciativa nace en `PRESENTED`; la aprobación existente produce `INITIATIVE_APPROVED`. `INITIATIVE_ARCHIVED` y `NOT_ADMISSIBLE` son terminales. Una iniciativa vinculada permanece aprobada y no admite acciones. Nunca acepta estados de proyecto ni `NOT_APPLICABLE`.

| Origen | Destino | Condiciones adicionales |
|---|---|---|
| `PRESENTED` | `INITIATIVE_ARCHIVED` | Sin proyecto; versión vigente; ámbito autorizado. |
| `PRESENTED` | `NOT_ADMISSIBLE` | Sin proyecto; versión vigente; ámbito autorizado. |
| `INITIATIVE_APPROVED` | `INITIATIVE_ARCHIVED` | Sin proyecto; versión vigente; ámbito autorizado. |

`PRESENTED → INITIATIVE_APPROVED` permanece en `/approval`.

### Invariantes y matriz de proyecto

Un proyecto nace en `PROJECT_IN_PROGRESS`; `CANCELLED` y `FINISHED` son terminales. `NOT_APPLICABLE` no es destino. `closingDate` solo se asigna al pasar de `PRODUCT_APPROVED` a `FINISHED`; cualquier otro destino conserva su valor previo.

| Origen | Destino permitido |
|---|---|
| `PROJECT_IN_PROGRESS` | `PRODUCT_APPROVED`, `PRODUCT_NOT_APPROVED`, `SUSPENDED`, `CANCELLED` |
| `SUSPENDED` | `PROJECT_IN_PROGRESS`, `CANCELLED` |
| `PRODUCT_NOT_APPROVED` | `PROJECT_IN_PROGRESS`, `CANCELLED` |
| `PRODUCT_APPROVED` | `FINISHED` |
| `CANCELLED` | Ninguno |
| `FINISHED` | Ninguno |

## Solicitud de transición (no persistente)

| Campo | Tipo | Validación | Persistencia |
|---|---|---|---|
| `code` | path `String` | no vacío y del tipo correcto | No independiente. |
| `version` | `long` | obligatoria e igual a vigente | Compara `VERSION`. |
| `targetStatus` | `PortfolioStatus` | obligatorio, contextual y permitido | Resultado en `ESTADO`. |
| `observation` | `String` | opcional, máximo 1000; `null` se normaliza a `""` | Solo `DETALLE_JSON`, nunca `Nota`. |

Los requests de iniciativa y proyecto son tipos distintos aunque compartan forma.

## Evento funcional (`AuditEventEntity` / `EVENTO_AUDITORIA`)

| Evidencia | Ubicación |
|---|---|
| Registro | entidad/identificador y código existentes. |
| Actor | columna existente desde autenticación. |
| Fecha | `occurredAt` existente. |
| Estados | `DETALLE_JSON.estadoAnterior`, `estadoNuevo`. |
| Rol | `DETALLE_JSON.rol` = `ADMINISTRADOR_PIIP`. |
| Unidad Ejecutora | `unidadEjecutoraId`, `unidadEjecutora`. |
| Observación | `observacion`, preservada o vacía. |
| Resultado | `resultado` = `EXITOSO`. |

Tipos: `ESTADO_INICIATIVA_CAMBIADO` y `ESTADO_PROYECTO_CAMBIADO`. La aprobación conserva `INICIATIVA_APROBADA`. Los rechazos solo quedan en auditoría de acceso.

## Consistencia transaccional

La unidad atómica incluye lectura/bloqueo, autorización, validación de versión/tipo/estado/destino/vínculo, mutación de estado/cierre/actualización, incremento JPA de versión e inserción del evento. Una falla revierte todo el conjunto funcional.

## Concurrencia

- Dos mutaciones con la misma versión: la primera confirma; la segunda recibe `409` sin éxito funcional.
- Archivado versus derivación: ambas bloquean la iniciativa. Si gana derivación, archivado detecta vínculo; si gana archivado, derivación detecta estado no aprobado. Nunca queda una combinación inválida.

## Fuente temporal

Un `Clock` inyectable en `America/Lima` produce el `Instant` de actualización y el `LocalDate` de cierre. La fecha del evento continúa en la columna `occurredAt` de la auditoría existente. El request no contiene fecha de cierre.
