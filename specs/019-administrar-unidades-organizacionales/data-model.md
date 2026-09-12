# Modelo de datos: Administración de Unidades Organizacionales

**Feature**: `019-administrar-unidades-organizacionales`  
**Fecha**: 2026-09-11

## Alcance de datos

El modelo funcional mantiene la cadena `INSTITUCION -> UNIDAD_EJECUTORA -> UNIDAD_ORGANICA`. La implementación inicial se ejecuta únicamente con los datos sintéticos del perfil exacto `test,test-reset`; no incluye carga ni modificación de datos institucionales reales.

## Institución

**Entidad JPA existente**: `InstitutionEntity`  
**Tabla Oracle**: `INSTITUCION`

| Campo | Regla en esta feature |
|-------|------------------------|
| `id` | Identificador del ámbito institucional. |
| `code`, `name`, `active`, `version` | Baseline; no se modifican. |
| Relación con UE | Una institución contiene UE. Se bloquea pesimistamente solo al crear una UE para serializar su código. |

La institución llega por contexto y no se acepta en cuerpos de creación o edición de UE.

## Unidad Ejecutora

**Entidad JPA**: `ExecutingUnitEntity`  
**Tabla Oracle**: `UNIDAD_EJECUTORA`

| Campo | Persistencia | Reglas |
|-------|--------------|--------|
| `id` | Existente | Identificador técnico. |
| `institution` | Existente, obligatorio | Heredado del contexto autorizado; inmutable. |
| `code` | Existente, obligatorio | Generado por backend como `UE-<consecutivo>` por institución; inmutable y único sin distinción de caso dentro del ámbito. |
| `name` | Existente, obligatorio | Editable con versión vigente. La descripción visible se deriva de este campo. |
| `active` | Existente | Alta activa; desactivación y reactivación explícitas y reversibles. |
| `displayOrder` | Nuevo, `@Column(name = "ORDEN_PRESENTACION")` | Entero no negativo de presentación solamente; no expresa relación jerárquica. En el alta es opcional: si no se informa, el backend asigna el mayor orden de la institución más uno, o `0` si no existe una UE. El listado ordena ascendentemente y desempata por `name` e identificador técnico. |
| `registeredAt` | Nuevo, `@Column(name = "FECHA_REGISTRO")` | Instante asignado por servidor al alta; no editable. |
| `activatedAt` | Nuevo, `@Column(name = "FECHA_ACTIVACION")` | Inicio de vigencia actual: igual a `registeredAt` al alta activa; se conserva al desactivar; cambia al reactivar. |
| `version` | Existente | Requerida para editar, desactivar o reactivar una entidad existente. |

Cada UE sintética debe contener valores deterministas de orden y de ambas fechas en el seed `test,test-reset`.

## Unidad Orgánica

**Entidad JPA**: `OrganizationalUnitEntity`  
**Tabla Oracle**: `UNIDAD_ORGANICA`

| Campo | Persistencia | Reglas |
|-------|--------------|--------|
| `id` | Existente | Identificador técnico. |
| `executingUnit` | Existente, obligatorio | Heredado del contexto autorizado; inmutable. |
| `code` | Existente, obligatorio | Generado como `UO-<consecutivo>` por UE; inmutable y único sin distinción de caso dentro de la UE. |
| `name` | Existente, obligatorio | Editable con versión vigente. |
| `acronym` | Existente, nullable | La columna conserva su nulabilidad histórica, pero el alta exige sigla no nula ni vacía y toda UO activa debe conservarla. Solo una UO activa con sigla no vacía aparece en el catálogo de portafolio; no se reactiva sin sigla. |
| `active` | Existente | Estado reversible sin eliminación física. El alta recibe un valor booleano explícito, sin predeterminado. |
| `version` | Existente | Requerida para editar, desactivar o reactivar una entidad existente. |

### Relación excluida

`parent`, `parentId` e `ID_UNIDAD_PADRE` permanecen en el baseline de persistencia por compatibilidad, pero no son parte de este modelo administrativo. No se crean ni modifican valores para esa relación.

## Autorización

**Tablas Oracle**: `USUARIO`, `ROL`, `USUARIO_ROL_AMBITO`

| Elemento | Regla |
|----------|-------|
| Rol | Solo `ADMINISTRADOR_PIIP` permite estas mutaciones. |
| Ámbito | Debe ser institucional, activo y vigente sobre la institución real de UE/UO. |
| Verificación | El servicio re-resuelve la autorización desde Oracle; no confía en ID de contexto enviado por cliente ni en controles Angular. |

## Auditoría

**Entidad JPA existente**: `AuditEventEntity`  
**Tabla Oracle**: `EVENTO_AUDITORIA`

Cada mutación confirmada añade un evento append-only con actor, instante, tipo/ID de entidad, acción, estado y diferencia funcional segura. Para consultar sin colisiones de códigos UO, `AuditEventEntity` incorporará `entityId` (`ID_ENTIDAD`), `institutionId` (`ID_INSTITUCION`), `executingUnitId` (`ID_UNIDAD_EJECUTORA`) y `organizationalUnitId` (`ID_UNIDAD_ORGANICA`) como columnas JPA estructuradas. `AuditService` las recibe y persiste dentro de la misma transacción funcional. La lectura compatible de `GET /audit/events?executingUnitId=...` expone el tipo e identificador de entidad y el ámbito correspondiente; si no recibe UE, se restringe a las UE cubiertas por los ámbitos autorizados del actor. `detailJson` complementa el evento con valores relevantes y nunca contiene token ni cuerpo HTTP.

Eventos previstos: `UE_CREADA`, `UE_ACTUALIZADA`, `UE_DESACTIVADA`, `UE_REACTIVADA`, `UO_CREADA`, `UO_ACTUALIZADA`, `UO_DESACTIVADA`, `UO_REACTIVADA`.

## Invariantes y transiciones

| Invariante o transición | Regla |
|-------------------------|-------|
| Alta UE | Requiere institución autorizada; genera código; queda activa; `registeredAt = activatedAt`. |
| Edición UE | Solo cambia nombre y orden de presentación; no cambia código ni institución. |
| Desactivación UE | Se permite incluso con UO activas; no modifica las UO ni `activatedAt`. |
| Reactivación UE | Cambia a activa y actualiza `activatedAt`. |
| Alta UO | Requiere UE de institución autorizada, nombre y sigla no vacía y un valor booleano explícito para `active`; genera código; no asigna padre. |
| Edición UO | Solo cambia nombre y sigla; no cambia código ni UE. Una UO activa no puede quedar con sigla vacía. |
| Desactivación UO | No elimina el registro y la excluye del catálogo de portafolio. |
| Reactivación UO | Requiere sigla no nula ni vacía para volver a ser elegible en el catálogo. |
| Versión obsoleta | Rechaza la mutación, no sobrescribe datos y no deja auditoría de éxito. |

## Concurrencia y generación

La creación bloquea el padre del ámbito con JPA: institución para UE y UE para UO. Dentro de la misma transacción se calcula el máximo de los códigos del prefijo correspondiente, se verifica el candidato sin distinguir mayúsculas y se persiste la entidad junto con su evento de auditoría. No se usa tabla de contadores, `REQUIRES_NEW` ni SQL nativo.
