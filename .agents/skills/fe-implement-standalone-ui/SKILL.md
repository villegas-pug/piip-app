---
name: fe-implement-standalone-ui
description: Implementar o modificar interfaces Angular standalone del PIIP, incluidos formularios, listados, rutas, botones, filtros, accesibilidad, paginación y estados de carga o error. Usar siempre ante solicitudes como "crea o cambia una pantalla", "agrega un formulario", "muestra una lista", "añade paginación", "mejora la accesibilidad", "muestra un spinner" o "corrige la presentación" en `apps/frontend`, siempre que no sea necesario modificar el contrato backend.
---

# Implementar UI standalone PIIP

## Validar el contexto

1. Leer `AGENTS.md`, la especificación activa y los componentes y rutas relacionados. Esto evita crear una pantalla desconectada de la navegación y convenciones existentes.
2. Confirmar que el comportamiento solicitado esté respaldado por `docs/architecture/piip-fields.md` o por la especificación. Marcar contradicciones como `NEEDS CLARIFICATION`, porque resolverlas por inferencia introduciría reglas funcionales nuevas.

## Mantener coherencia de interfaz

1. Preservar el shell, las rutas, los estilos y los componentes standalone existentes, para evitar regresiones visuales o de navegación fuera del cambio solicitado.
2. Reutilizar señales, formularios tipados, `PiipRepository` y componentes compartidos antes de crear abstracciones nuevas. Duplicarlos produciría comportamientos divergentes y mayor mantenimiento.
3. Mantener filtros, paginación fija, estados de carga y error, y controles accesibles consistentes entre vistas equivalentes.
4. No mostrar acciones fuera del rol y ámbito efectivo. La ocultación visual reduce acciones improcedentes, pero no reemplaza la autorización backend.

## Límites de alcance y ejecución

1. Modificar únicamente `apps/frontend/**`. Si la funcionalidad requiere backend o un contrato nuevo, devolver un handoff al agente principal.
2. No borrar archivos ni ejecutar pruebas o builds sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Pantallas, rutas y componentes afectados.
- Fuente funcional o especificación utilizada.
- Reutilización de componentes, formularios y estado existente.
- Consideraciones de accesibilidad, carga, error y paginación aplicables.
- Pruebas ejecutadas o pendientes de autorización.
- Handoff backend y `NEEDS CLARIFICATION`, cuando correspondan.
