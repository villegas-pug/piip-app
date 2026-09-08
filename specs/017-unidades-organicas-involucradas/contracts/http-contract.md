# Contrato HTTP: Unidades Orgánicas Involucradas

Delta del contrato vigente para `specs/017-unidades-organicas-involucradas`. **No cambian rutas ni nombres técnicos**: el arreglo conserva el nombre `responsibleUnits`; solo cambia su cardinalidad (mínimo uno, sin máximo), el comportamiento del catálogo de selección y el detalle de eventos de auditoría. La fuente canónica publicada es el OpenAPI generado por `OpenApiGenerationTest` (`apps/backend/target/piip-openapi.json`) con autorización explícita; el cliente Angular se sincroniza después (`npm run api:generate`). Este documento describe el contrato funcional; ante divergencia, manda el OpenAPI generado.

## Endpoints afectados (rutas sin cambios)

| Operación | Endpoint | Cambio |
|---|---|---|
| Alta de iniciativa | `POST /initiatives` | `responsibleUnits` admite 1..N elementos |
| Alta de proyecto derivado | `POST /projects/derived` | `responsibleUnits` admite 1..N elementos |
| Alta de proyecto preexistente | `POST /projects/preexisting` | `responsibleUnits` admite 1..N elementos |
| Edición de iniciativa | `PATCH /initiatives/{code}` | `responsibleUnits` (si presente) admite 1..N elementos |
| Edición de proyecto | `PATCH /projects/{code}` | `responsibleUnits` (si presente) admite 1..N elementos |
| Detalle | `GET /initiatives/{code}`, `GET /projects/{code}` | `responsibleUnits` devuelve 1..N elementos ordenados |
| Catálogo de unidades | `GET /organizational-units?executingUnitId={id}` | Devuelve únicamente activas de la UE **con sigla no vacía** (Q2=A) |

## Cuerpos de entrada

### Altas (`POST /initiatives`, `POST /projects/derived`, `POST /projects/preexisting`)

```json
{
  "responsibleUnits": [
    { "organizationalUnitId": 101 },
    { "organizationalUnitId": 102 }
  ]
}
```

- `responsibleUnits`: arreglo **obligatorio, mínimo un elemento, sin máximo**; el OpenAPI publicado expresa `minItems: 1` y `maxItems: 2147483647`; cada elemento exige `organizationalUnitId` no nulo.
- El orden del arreglo es el orden funcional de incorporación (posición 1..N).
- El resto de los campos del cuerpo permanece sin cambios.

### Edición (`PATCH ...`)

- Semántica de presencia JSON vigente: campo ausente conserva las asociaciones existentes (FR-009); campo presente sustituye el conjunto completo de forma atómica (FR-007) con mínimo un elemento y sin máximo.
- `version` sigue siendo obligatoria para el control de concurrencia (409 ante versión desactualizada).

## Respuesta de registro (detalle y post-alta)

`responsibleUnits`: arreglo ordenado por posición de presentación; cada elemento conserva la forma vigente:

```json
{
  "responsibleUnits": [
    {
      "organizationalUnit": {
        "id": 101, "code": "UE-001-UO-01", "name": "UE-001-UO-01",
        "acronym": "UO1", "active": true, "parentId": null, "executingUnitId": 1
      },
      "originalDesignation": "UE-001-UO-01",
      "displayOrder": 1
    }
  ]
}
```

- `organizationalUnit.acronym` es la **Abreviatura** y `organizationalUnit.name` la **Descripción**; ambos provienen del maestro y son de solo lectura.
- Registros históricos: la respuesta incluye todas las asociaciones persistidas en su orden original, incluidas las inactivas como contexto (FR-022).

## Errores (semántica vigente ampliada)

| Condición | HTTP | Identificación |
|---|---|---|
| Lista vacía o elemento inválido de forma sintáctica | 400 | Validación del cuerpo (mínimo un elemento). El servicio conserva además el guard `INVALID_SIZE` para llamadas internas, sin reemplazar la respuesta HTTP primaria del DTO. |
| Unidad inexistente | 422 | Referencia inválida sobre `responsibleUnits` (código vigente de referencia no encontrada) |
| Unidad inactiva como nueva incorporación | 422 | Referencia inválida (código vigente de unidad inactiva) |
| Unidad de otra Unidad Ejecutora | 422 | Referencia inválida (código vigente de unidad fuera de la UE) |
| Nueva incorporación sin sigla | 422 | Referencia inválida con código `MISSING_ACRONYM`, identificando la fila (fijado en T002; documentado en el OpenAPI) |
| Unidad duplicada en la lista | 422 | Rechazo identificado con código `DUPLICATED_UNIT`, indicando la fila duplicada y su primera ocurrencia (fijado en T002; documentado en el OpenAPI) |
| Sin permiso sobre la UE | 403 | Autorización vigente |
| Registro no encontrado | 404 | Vigente |
| Versión desactualizada | 409 | Concurrencia vigente |
| Edición sin cambios efectivos | 422 | Vigente |

Toda operación rechazada no modifica asociaciones previas (atomicidad) ni se audita como modificación exitosa.

Cuando un rechazo corresponde a una fila de la lista, el detalle del problema usa `referenceField: responsibleUnits[n]`, donde `n` es la posición visible basada en 1. El cliente convierte esa referencia a su índice interno basado en 0 para mostrar el error en la fila correspondiente sin descartar las selecciones válidas.

## Detalle de eventos de auditoría (extensión)

Eventos vigentes afectados: `INICIATIVA_REGISTRADA`, `PROYECTO_DERIVADO_REGISTRADO`, `PROYECTO_PREEXISTENTE_REGISTRADO` (altas) y `INICIATIVA_ACTUALIZADA`, `PROYECTO_ACTUALIZADO` (ediciones). El mecanismo append-only, actor, fecha, Unidad Ejecutora y contexto no cambian; el detalle se extiende con datos funcionales (sin cuerpos ni secretos):

- **Altas**: clave técnica `responsibleUnits` con lista ordenada de elementos `{id, code, name, sigla, nro}`; conserva además el contexto vigente del evento.
- **Ediciones**: valor anterior y nuevo de la lista completa, con los mismos elementos `{id, code, name, sigla, nro}` dentro del diff de cambios vigente. `nro` corresponde a la posición de presentación 1..N y reemplaza la clave de auditoría previa `displayOrder`.

La auditoría visible del frontend presenta estos valores bajo la denominación "Unidades Orgánicas Involucradas".
