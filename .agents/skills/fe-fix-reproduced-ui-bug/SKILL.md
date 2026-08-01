---
name: fe-fix-reproduced-ui-bug
description: Reproducir y corregir regresiones del frontend Angular PIIP con evidencia previa, cambio mínimo y prueba focalizada. Usar siempre cuando el usuario diga que "la pantalla falla", "el botón no funciona", "el formulario no guarda", "la lista no carga", "la navegación se rompe", "queda cargando", "el login falla" o reporte un error visual, de autenticación o consumo HTTP en `apps/frontend`. No editar hasta reproducir el problema o reunir evidencia verificable equivalente.
---

# Corregir bug frontend reproducido

## Reproducir antes de editar

1. Obtener pasos, entrada, resultado esperado y resultado observado. Sin esta comparación no puede demostrarse que el comportamiento sea una regresión.
2. Reproducir mediante una prueba existente o nueva, o mediante evidencia verificable del flujo afectado, antes de editar cualquier archivo.
3. Si la reproducción requiere ejecutar tests o una aplicación y no existe autorización explícita en el turno actual, detenerse antes de editar y solicitarla.
4. Si no se reproduce, detener la corrección y devolver evidencia, hipótesis y `NEEDS CLARIFICATION`. Corregir por intuición puede ocultar la causa o romper otro flujo.

## Corregir la causa

1. Localizar la causa en componentes, estado, rutas, autenticación o adaptador HTTP sin asumir reglas funcionales.
2. Aplicar el cambio mínimo exclusivamente en `apps/frontend/**`, para aislar la causa y reducir regresiones laterales.
3. Añadir o ajustar una prueba diseñada para fallar antes del cambio y cubrir la regresión después. No afirmar que pasa si no fue ejecutada con autorización.

## Límites de alcance y ejecución

1. No cambiar contratos backend para acomodar el bug. Si la causa está fuera del frontend, devolver un handoff al agente principal.
2. No borrar archivos ni ejecutar tests o builds sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Evidencia de reproducción previa.
- Causa raíz localizada.
- Cambio mínimo y archivos afectados.
- Prueba de regresión añadida o ajustada.
- Resultado de las verificaciones autorizadas y cobertura no ejecutada.
- Handoff backend cuando la causa o el contrato estén fuera del frontend.
- `NEEDS CLARIFICATION` si el comportamiento esperado no está confirmado.
