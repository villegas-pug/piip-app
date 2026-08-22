# Modelo de datos: Actualización controlada de registros de portafolio

## Decisión estructural

La feature reutiliza íntegramente el modelo JPA/Oracle vigente. No agrega tablas, columnas, secuencias, índices, constraints, triggers, procedimientos, DDL, datos iniciales ni un segundo mecanismo de versión.

```text
UNIDAD_EJECUTORA 1 ─── * REGISTRO_PORTAFOLIO
                              │
                              ├── @Version VERSION
                              ├── * ─── 1 CATALOGO_ITEM (solución)
                              ├── * ─── 1 CATALOGO_ITEM (fuente)
                              ├── * ─── 0..1 CATALOGO_ITEM (PEI)
                              ├── * ─── 0..1 CATALOGO_ITEM (POI)
                              ├── 0..1 origen ─── 1 REGISTRO_PORTAFOLIO
                              └── 1 ─── * REGISTRO_UNIDAD_RESPONSABLE * ─── 1 UNIDAD_ORGANICA

REGISTRO_PORTAFOLIO 1 ─── * EVENTO_AUDITORIA (por tipo/código de entidad)
```

`database/generated/piip-oracle.sql` no tiene cambio lógico previsto.

## `PortfolioRecordEntity` / `REGISTRO_PORTAFOLIO`

### Campos editables por variante

| Campo de aplicación | Persistencia | Iniciativa | Proyecto derivado | Proyecto preexistente | Nulo de edición |
|---------------------|--------------|------------|-------------------|------------------------|-----------------|
| `name` | `NOMBRE` | Sí | Sí | Sí | No |
| `solutionTypeId` | `ID_TIPO_SOLUCION` | Sí | Sí | No, conserva `NOT_APPLICABLE` | No |
| `sourceId` | `ID_FUENTE_ORIGEN` | Sí | Sí | Sí | No |
| `startDate` | `FECHA_INICIO` | Sí | Sí | Sí | No |
| `responsible` | `RESPONSABLE` | Sí | Sí | Sí | No |
| `peiObjectiveId` | `ID_OBJETIVO_PEI` | Sí | Sí | Sí | Sí |
| `poiActivityId` | `ID_ACTIVIDAD_POI` | Sí | Sí | Sí | Sí |
| `description` | `DESCRIPCION` | Sí | Sí | Sí | No |
| `keyResults` | `RESULTADOS_CLAVE` | No | Sí | Sí | Sí |
| `note` | `NOTA` | Sí | Sí | Sí | Sí |
| `digitalComponent` | `COMPONENTE_DIGITAL` | Sí | Sí | Sí | No |
| `responsibleUnits` | Asociación hija | Sí | Sí | Sí | No; lista no vacía |

### Campos protegidos

| Campo | Razón de protección |
|-------|---------------------|
| `id`, `code`, `recordType` | Identidad del mismo registro. |
| `originRecord`, `originMode` | Relación única y modo de procedencia. |
| `executingUnit` | Ámbito persistido de autorización y coherencia organizacional. |
| `status` | Solo cambia mediante aprobación/transiciones vigentes. |
| `finalProductType`, `closingDate` | Pertenecen al ciclo de vida y cierre. |
| `createdBySubject`, `createdAt` | Evidencia técnica de origen. |
| `updatedAt` | Se asigna automáticamente al confirmar un cambio efectivo. |
| `version` | Lo gestiona exclusivamente `@Version`. |

Los métodos de actualización de la entidad reciben únicamente el candidato ya validado y un `Instant` del `Clock` PIIP. No se agregan setters públicos genéricos para los campos protegidos.

## Estado y elegibilidad

La actualización no produce transiciones.

```text
Iniciativa:
  PRESENTED + sin proyecto derivado ──PATCH válido──> PRESENTED
  cualquier otro caso                ──422──────────> sin cambio

Proyecto derivado o preexistente:
  PROJECT_IN_PROGRESS ──PATCH válido──> PROJECT_IN_PROGRESS
  cualquier otro caso ──422──────────> sin cambio
```

La comprobación de versión ocurre antes de interpretar la copia antigua: versión distinta produce 409. La creación derivada y la actualización de iniciativa comparten lock de escritura sobre la iniciativa.

## Modelo lógico del command parcial

```text
FieldUpdate<T>
├── present: boolean
└── value: T | null

InitiativeUpdateCommand / ProjectUpdateCommand
├── version: long
├── name: FieldUpdate<String>
├── ...
└── responsibleUnits: FieldUpdate<List<ResponsibleUnitInput>>
```

Reglas:

- `present = false`: conservar el valor persistido.
- `present = true, value != null`: validar y proponer el valor.
- `present = true, value = null`: retirar solo PEI, POI, nota o resultados clave de proyecto.
- ningún campo editable presente: 422.
- candidato completo igual al snapshot vigente: 422.
- la igualdad de UO incluye identidad y posición; reordenar es un cambio.

## `ResponsibleUnitEntity` / `REGISTRO_UNIDAD_RESPONSABLE`

La tabla existente es el único modelo de responsabilidad organizacional.

| Campo | Regla durante reemplazo |
|-------|--------------------------|
| `ID_REGISTRO` | Conserva el mismo registro padre. |
| `ID_UNIDAD_ORGANICA` | Debe existir, estar activa, no repetirse y pertenecer a la UE del padre. |
| `DENOMINACION_ORIGINAL` | Se fotografía desde el nombre vigente de la UO aceptada. |
| `ORDEN_PRESENTACION` | Se asigna `1..n` según el orden del request. |

Algoritmo atómico:

1. Resolver y validar toda la lista sin escribir.
2. Comparar IDs y orden con el conjunto actual.
3. Si es igual, no tocar filas.
4. Si cambia, eliminar el conjunto anterior y hacer flush.
5. Insertar el conjunto validado en orden.
6. Cambiar `updatedAt` del padre y hacer flush para avanzar `VERSION`.

Cualquier error revierte el conjunto completo.

## Referencias de catálogo

| Campo | Catálogo esperado | Escritura nula |
|-------|-------------------|----------------|
| `solutionTypeId` | `SOLUTION_TYPE` | No |
| `sourceId` | `SOURCE_ORIGIN` | No |
| `peiObjectiveId` | `PEI_OBJECTIVE` | Sí |
| `poiActivityId` | `POI_ACTIVITY` | Sí |

Una referencia incluida debe existir, pertenecer al catálogo esperado y estar activa junto con su catálogo. Una referencia histórica inactiva que no aparezca en el PATCH permanece sin revalidación de escritura. PEI y POI se resuelven independientemente.

## Concurrencia y fecha de modificación

Secuencia de versión:

```text
request.version = versionAnterior
        │
        ├── != entity.version ──> 409, rollback, sin evento
        │
        └── == entity.version
              ├── diff vacío ──> 422, misma versión/fecha, sin evento
              └── diff efectivo
                    ├── updatedAt = clock.instant()
                    ├── flush JPA
                    ├── entity.version = versionNueva
                    └── evento(versionAnterior, versionNueva)
```

No se usan `ETag`, timestamp como versión ni tabla histórica adicional.

## `AuditEventEntity` / `EVENTO_AUDITORIA`

Se conservan las columnas existentes. `DETALLE_JSON` recibe un snapshot lógico, no el body HTTP.

Eventos:

- `INICIATIVA_ACTUALIZADA`
- `PROYECTO_ACTUALIZADO`

Forma aprobada:

```json
{
  "tipoRegistro": "Iniciativa",
  "unidadEjecutoraId": 10,
  "unidadEjecutora": "Unidad Ejecutora",
  "versionAnterior": 4,
  "versionNueva": 5,
  "cambios": {
    "name": {
      "anterior": "Nombre anterior",
      "nuevo": "Nombre actualizado"
    },
    "peiObjective": {
      "anterior": { "id": 21, "code": "PEI-001", "name": "Objetivo" },
      "nuevo": null
    },
    "responsibleUnits": {
      "anterior": [
        { "id": 31, "code": "UO-01", "name": "Unidad A", "displayOrder": 1 },
        { "id": 32, "code": "UO-02", "name": "Unidad B", "displayOrder": 2 }
      ],
      "nuevo": [
        { "id": 32, "code": "UO-02", "name": "Unidad B", "displayOrder": 1 },
        { "id": 31, "code": "UO-01", "name": "Unidad A", "displayOrder": 2 }
      ]
    }
  },
  "resultado": "EXITOSO"
}
```

Actor y fecha permanecen en `ACTOR_SUBJECT`/`ID_USUARIO` y `FECHA_EVENTO`. El JSON no incluye motivo adicional, request completo, token, archivo ni contenido documental. Un intento rechazado solo deja la auditoría de acceso vigente.

## Response y modelo de presentación

`PortfolioRecordResponse` continúa siendo la representación completa. Contiene referencias resueltas, UO en orden, `updatedAt` y nueva `version`.

Angular proyecta el response a un modelo editable con:

- identidad y metadatos read-only;
- referencias actuales, incluidas históricas inactivas;
- lista ordenada de UO;
- `version` de la copia fresca;
- variante `INITIATIVE`, `DERIVED_PROJECT` o `PREEXISTING_PROJECT`.

El formulario conserva un snapshot inicial y genera un PATCH sparse por comparación. La respuesta exitosa sustituye la representación local y su versión; no existe un borrador persistente.
