# Contrato de administración organizacional

**Estado**: diseño para publicación backend. El contrato OpenAPI definitivo se genera desde los controladores y DTO del backend.  
**Base**: `/admin/organization`

## Reglas comunes

- Todas las operaciones requieren autenticación y `ADMINISTRADOR_PIIP` con ámbito institucional explícito, activo y vigente.
- El backend resuelve la institución real de la UE o UO y rechaza contextos manipulados.
- Las respuestas administrativas no exponen entidades JPA ni `parent`, `parentId` o `ID_UNIDAD_PADRE`.
- La edición, desactivación y reactivación usan versión obligatoria; las altas no la reciben porque crean una representación nueva. No existe `DELETE`.
- Los cuerpos JSON son estrictos: campos no permitidos como `code`, `institutionId`, `executingUnitId` o `parentId` se rechazan con `400`.
- `displayOrder` es opcional solo al crear una UE; si se omite, el backend asigna el mayor orden de la institución más uno, o `0` cuando no existe una UE. Cuando se informa, usa un entero no negativo; el listado desempata por `name` e identificador técnico.
- `acronym` es obligatorio y no vacío al crear una UO. Una UO activa debe conservarlo no vacío; no puede reactivarse mientras la sigla esté vacía.

## Tipos de intercambio

### Contexto institucional administrable

```json
{
  "id": 1,
  "code": "MIDAGRI",
  "name": "Ministerio de Desarrollo Agrario y Riego"
}
```

### Unidad Ejecutora administrativa

```json
{
  "id": 10,
  "code": "UE-001",
  "name": "Unidad Ejecutora de Ejemplo",
  "active": true,
  "displayOrder": 1,
  "registeredAt": "2026-09-11T12:00:00Z",
  "activatedAt": "2026-09-11T12:00:00Z",
  "version": 0,
  "institution": {
    "id": 1,
    "code": "MIDAGRI",
    "name": "Ministerio de Desarrollo Agrario y Riego"
  }
}
```

### Unidad Orgánica administrativa

```json
{
  "id": 20,
  "code": "UO-001",
  "name": "Unidad Orgánica de Ejemplo",
  "acronym": "UOE",
  "active": true,
  "version": 0,
  "executingUnit": {
    "id": 10,
    "code": "UE-001",
    "name": "Unidad Ejecutora de Ejemplo"
  }
}
```

## Operaciones

| Método | Ruta | Cuerpo | Respuesta exitosa |
|--------|------|--------|-------------------|
| `GET` | `/institutions` | Ninguno | Instituciones administrables del actor. |
| `GET` | `/executing-units?institutionId={id}` | Ninguno | Lista administrativa de UE activas e inactivas. |
| `POST` | `/executing-units?institutionId={id}` | `name`, `displayOrder` opcional | UE administrativa creada. |
| `PUT` | `/executing-units/{id}?version={n}` | `name`, `displayOrder` | UE administrativa actualizada. |
| `PUT` | `/executing-units/{id}/deactivation?version={n}` | Ninguno | UE administrativa desactivada. |
| `PUT` | `/executing-units/{id}/reactivation?version={n}` | Ninguno | UE administrativa reactivada. |
| `GET` | `/organizational-units?executingUnitId={id}` | Ninguno | Lista administrativa de UO activas e inactivas. |
| `POST` | `/organizational-units?executingUnitId={id}` | `name`, `acronym` no vacío, `active` booleano obligatorio | UO administrativa creada. |
| `PUT` | `/organizational-units/{id}?version={n}` | `name`, `acronym` | UO administrativa actualizada. |
| `PUT` | `/organizational-units/{id}/deactivation?version={n}` | Ninguno | UO administrativa desactivada. |
| `PUT` | `/organizational-units/{id}/reactivation?version={n}` | Ninguno | UO administrativa reactivada. |

`institutionId` y `executingUnitId` solo son parámetros de contexto de ruta o consulta. Nunca se aceptan dentro de un cuerpo de mutación. Los códigos los genera el backend y solo se entregan en respuestas.

## Cuerpos de mutación

### Crear o editar UE

En el alta, `name` es obligatorio y `displayOrder` es opcional; si llega, es un entero no negativo. En la edición, ambos campos son obligatorios y `displayOrder` es un entero no negativo.

```json
{
  "name": "Unidad Ejecutora de Ejemplo",
  "displayOrder": 1
}
```

### Crear UO

`acronym` es obligatorio y no puede estar vacío. `active` es un booleano obligatorio y no tiene valor predeterminado. Toda UO creada activa queda disponible para el catálogo de portafolio de su UE.

```json
{
  "name": "Unidad Orgánica de Ejemplo",
  "acronym": "UOE",
  "active": true
}
```

### Editar UO

En una UO activa, `acronym` no puede quedar vacío. El estado se cambia exclusivamente mediante las rutas de desactivación o reactivación.

```json
{
  "name": "Unidad Orgánica Actualizada",
  "acronym": "UOA"
}
```

## Consulta de auditoría compatible

La feature conserva y extiende la consulta existente `GET /audit/events?executingUnitId={id}`. Para eventos de UE y UO, la respuesta debe identificar `event`, `entityType`, `entityId`, `entityCode`, `institutionId`, `executingUnitId`, `organizationalUnitId`, actor, fecha y detalle funcional seguro. Cuando `executingUnitId` se informa, debe pertenecer a un ámbito autorizado del actor. Cuando se omite, la respuesta se limita a las UE cubiertas por los ámbitos institucionales autorizados del actor; nunca devuelve el conjunto global sin filtro. La consulta no expone secretos, cuerpos HTTP ni eventos de otros ámbitos.

## Errores

Todas las respuestas de error usan `ProblemDetail` con un `problemCode` estable y un campo de referencia cuando aplique.

| HTTP | `problemCode` esperado | Situación |
|------|------------------------|-----------|
| `400` | `INVALID_REQUEST` | Cuerpo inválido, campo prohibido o validación de forma. |
| `403` | `FORBIDDEN_SCOPE` | Rol o ámbito institucional no autorizado. |
| `404` | `RESOURCE_NOT_FOUND` | Institución, UE o UO inexistente dentro del flujo. |
| `409` | `STALE_VERSION` | Versión de mutación obsoleta. |
| `422` | `INCOMPATIBLE_ASSIGNMENT_STATE` | Desactivar o reactivar una entidad ya en ese estado. |
| `422` | `ORGANIZATION_CODE_DUPLICATE` | Anomalía de unicidad detectada durante generación. |

## Compatibilidad

No se sustituye ni modifica el contrato de:

- `GET /institutions`
- `GET /executing-units`
- `GET /organizational-units?executingUnitId=...`

En especial, el último conserva su modelo legado y su filtro de UO activas con sigla no vacía. El posible `parentId` de esa respuesta no se replica en este contrato.
