# Contrato de UI: Unidades Orgánicas Involucradas

Contrato de presentación e interacción para `specs/017-unidades-organicas-involucradas` (FR-001..FR-017, FR-022..FR-024, clarificaciones de la sesión 2026-09-07). El detalle visual pertenece a la implementación; este contrato fija comportamiento, contenido y accesibilidad verificables.

## Denominación visible

- El campo se denomina **"Unidades Orgánicas Involucradas"** en: formularios de alta (iniciativa, proyecto derivado, proyecto preexistente), edición, revisión previa, detalle y auditoría visible.
- Sustituye a la denominación visible "Unidad Orgánica responsable"; los nombres técnicos internos (`responsibleUnits`) no cambian.

## Estructura de la lista (registro y edición)

Cada fila muestra exclusivamente, de solo lectura:

1. **Nro**: número autonumérico derivado de la posición (1..N).
2. **Descripción**: nombre de la Unidad Orgánica del maestro institucional.
3. **Abreviatura**: sigla de la Unidad Orgánica del maestro institucional.

- En escritorio: columnas Nro, Descripción y Abreviatura.
- En pantallas pequeñas: la misma información con presentación adaptable y legible (sin pérdida de datos).

## Controles e interacción

- **Agregar**: incorpora una fila nueva al final de la lista; el orden funcional es el orden de incorporación; no existe reordenamiento manual.
- **Retirar**: disponible en cualquier fila excepto cuando solo queda una; el mínimo de una unidad es obligatorio para confirmar.
- **Renumeración**: al retirar una fila, los números visuales se recalculan automáticamente y siempre forman la secuencia continua 1..N.
- **Selección**: el catálogo de opciones ofrece únicamente unidades activas de la Unidad Ejecutora del registro con sigla no vacía; las unidades sin sigla no se ofrecen (Q2=A) y no se inventan abreviaturas; las opciones ya seleccionadas en otras filas no se reofrecen (FR-012).
- **Proyecto derivado**: el formulario inicia la lista precargada con las unidades confirmadas de la iniciativa de origen, en el mismo orden, como valor inicial editable (Q1=A, FR-033).
- **Edición**: el conjunto completo es editable (incluidos registros históricos con varias unidades); editar otros campos sin tocar la lista conserva las asociaciones íntegramente; el envío incluye la lista solo si cambió respecto al valor de partida.

## Estados del catálogo

- **Cargando**: se informa el estado y se impide una confirmación inválida.
- **Vacío**: se informa que no hay Unidades Orgánicas disponibles para la UE y se bloquea la confirmación.
- **Error**: se informa el error y se ofrece reintentar la consulta; no se permite confirmar con catálogo no disponible.
- Si una unidad seleccionada deja de estar disponible (p. ej. queda inactiva), la fila lo indica y la confirmación se rechaza sin perder las selecciones válidas.

## Errores y validación

- Los errores identifican la fila afectada y explican la causa (inactiva, otra UE, sin sigla, duplicada, inexistente).
- Ningún error de fila descarta las selecciones válidas ya ingresadas.
- El rechazo del backend (422/409) se presenta con mensaje claro; en conflicto de versión (409) se conservan los cambios locales y se ofrece recargar el valor más reciente (comportamiento vigente).

## Accesibilidad

- Los controles de agregar, seleccionar y retirar son operables por teclado y annonados para tecnologías de asistencia.
- La tabla/lista expone la relación de encabezados y celdas de forma accesible; los mensajes de error por fila están asociados a la fila correspondiente.

## Revisión previa, detalle y auditoría visible

- La revisión previa a la confirmación presenta todas las unidades en el orden confirmado, con Nro, Descripción y Abreviatura (los diálogos actuales no muestran unidades; pasan a mostrarlas).
- El detalle del registro presenta la lista completa en el orden confirmado, incluidas asociaciones históricas inactivas como contexto.
- La auditoría visible presenta, para altas, la lista ordenada confirmada y, para modificaciones, los valores anterior y nuevo; cada elemento con unidad, nombre, sigla y Nro.

## Datos de demostración (mock)

- El repositorio mock del frontend expone unidades con sigla y genera listas coherentes con el contrato, de modo que la UI sea navegable sin backend.
