---
name: fe-implement-standalone-ui
description: Implementar o modificar pantallas Angular standalone del PIIP, incluidos formularios, listados, rutas, accesibilidad, paginación y estados de carga. Usar solo para cambios en apps/frontend que no requieran modificar el contrato backend.
---

# Implementar UI standalone PIIP

1. Leer `AGENTS.md`, la especificación activa y los componentes/rutas existentes relacionados.
2. Confirmar que el comportamiento solicitado está respaldado por `docs/architecture/piip-fields.md` o por la especificación; marcar contradicciones como `NEEDS CLARIFICATION`.
3. Preservar el shell, las rutas, los estilos y los componentes standalone existentes.
4. Reutilizar señales, formularios tipados, `PiipRepository` y componentes compartidos antes de crear nuevas abstracciones.
5. Mantener filtros, paginación fija, estados de carga/error y controles accesibles consistentes entre vistas equivalentes.
6. No mostrar acciones fuera del rol y ámbito efectivo; la ocultación visual no reemplaza la autorización backend.
7. Modificar únicamente `apps/frontend/**` y devolver un handoff si el cambio necesita backend.
8. No borrar archivos ni ejecutar pruebas o builds sin autorización explícita del usuario en el turno actual.
