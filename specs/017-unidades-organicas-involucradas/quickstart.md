# Quickstart: Unidades Orgánicas Involucradas

Guía de validación end-to-end de `specs/017-unidades-organicas-involucradas`. No incluye implementación: los detalles viven en `tasks.md` y la fase de implementación. Contratos: [contracts/http-contract.md](contracts/http-contract.md) y [contracts/ui-contract.md](contracts/ui-contract.md); modelo: [data-model.md](data-model.md).

## Prerrequisitos

1. Inicialización autorizada del esquema descartable con los perfiles exactos y ordenados `test,test-reset` (deja 2 UEs sintéticas con ≥2 UOs activas cada una, con código, nombre y sigla; tablas operativas vacías).
2. Backend y frontend en ejecución con autorización explícita (esto no ocurre durante el plan).
3. Sesión iniciada con el usuario administrador sintético y ámbito sobre la Unidad Ejecutora de prueba.

## Escenarios de validación

### E1. Alta de iniciativa con lista múltiple (SC-001, SC-004)

1. Abrir el registro de iniciativa; en "Unidades Orgánicas Involucradas" agregar dos unidades activas de la misma UE con sigla.
2. Verificar filas: Nro 1 y 2, Descripción y Abreviatura de solo lectura del maestro; la segunda fila no reofrece la opción ya seleccionada.
3. Confirmar: la revisión previa muestra la lista completa ordenada; al confirmar, el detalle presenta las unidades en el orden elegido y el evento de auditoría del alta registra la lista con unidad, nombre, sigla y Nro por elemento.

### E2. Edición de la lista (SC-002, SC-003)

1. Editar el registro: agregar una tercera unidad al final y confirmar → la lista queda 1..3 en orden de incorporación.
2. Retirar la fila intermedia → los Nro se renumeran a la secuencia continua 1..2.
3. Con una sola fila, el control de retiro no está disponible.
4. Editar otro campo sin tocar la lista → las asociaciones se conservan íntegramente y la auditoría no reporta cambio de lista.
5. El evento de modificación conserva los valores anterior y nuevo de la lista.

### E3. Rechazos atómicos (SC-001, SC-003)

Confirmar cada caso y verificar que ninguna asociación previa cambia y que el error identifica la fila y la causa:

- Lista vacía.
- Unidad duplicada en dos filas.
- Unidad inactiva como nueva incorporación.
- Unidad de otra Unidad Ejecutora.
- Unidad sin sigla (no aparece en el catálogo; si se envía directamente, el backend la rechaza).
- Catálogo en carga, vacío o con error → confirmación bloqueada y reintento disponible.
- Versión desactualizada (409) → se conservan los cambios locales y se ofrece recargar.

### E4. Proyecto derivado precargado (FR-033)

1. Desde una iniciativa aprobada con varias unidades, abrir el registro de proyecto derivado.
2. La lista inicia precargada con las unidades de la iniciativa en el mismo orden y permanece editable.
3. Retirar una unidad y confirmar → el proyecto queda con la lista confirmada propia.

### E5. Compatibilidad histórica (SC-005)

1. Con un registro histórico con varias unidades (incluida una inactiva): el detalle las muestra todas en su orden original.
2. Editar otro campo sin tocar la lista → no hay migración, renumeración ni saneo; la asociación inactiva permanece como contexto y no se reofrece.
3. Modificar la lista conservando la fila inactiva y agregando una activa válida → las nuevas asociaciones cumplen vigencia, pertenencia, sigla y unicidad; la retenida permanece como contexto.

### E6. Datos sintéticos e inicialización (SC-006)

1. Ejecutar la inicialización autorizada `test,test-reset`: cada UE sintética queda con ≥2 UOs activas con código, Descripción y Abreviatura completos; las tablas operativas permanecen vacías.
2. Ejecutarla de nuevo: el mismo conjunto, sin duplicados.
3. Las pruebas de postvalidación (unitarias, sin Oracle) verifican que una UO sintética sin sigla o con asociación inválida declara la inicialización incompleta (fallo seguro).
4. Arranques con perfiles ordinarios (`dev`/`prod`) no insertan estos datos (verificado por guardias existentes).

### E7. Alineación documental (SC-008)

1. `docs/funcional/guia-funcional-piip.md` y `docs/architecture/piip-fields.md` describen el campo como lista ordenada 1..N con denominación "Unidades Orgánicas Involucradas", sin menciones vigentes de la regla exactamente-una.

## Comandos de verificación (requieren autorización explícita)

| Verificación | Comando | Desde |
|---|---|---|
| Pruebas backend (validaciones, atomicidad, auditoría, postvalidación, contratos) | `gradlew.bat test` | `apps/backend` |
| Publicación del contrato OpenAPI | `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` | `apps/backend` |
| Sincronización del cliente generado (tras contrato publicado) | `npm run api:generate` | `apps/frontend` |
| Pruebas frontend (lista dinámica, renumeración, catálogo, accesibilidad, precarga) | `npm test -- --watch=false` | `apps/frontend` |
| Integración Oracle del reset (destructiva, autorización separada) | `gradlew.bat integrationTest` | `apps/backend` |

## Resultado esperado

Los tres tipos de alta y la edición confirman listas 1..N válidas con orden de incorporación y renumeración continua; revisión, detalle y auditoría presentan el orden confirmado; los históricos permanecen intactos; la inicialización sintética es idempotente y fail-safe; la documentación funcional queda alineada.
