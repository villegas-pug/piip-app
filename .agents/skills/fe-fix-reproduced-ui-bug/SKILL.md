---
name: fe-fix-reproduced-ui-bug
description: Reproducir y corregir bugs del frontend Angular PIIP con evidencia antes de editar, cambio mínimo y prueba focalizada. Usar cuando el usuario reporte una regresión visual, de formulario, navegación, carga, autenticación o consumo HTTP en apps/frontend.
---

# Corregir bug frontend reproducido

1. Obtener pasos, entrada, resultado esperado y resultado observado.
2. Reproducir el problema mediante una prueba existente/nueva o evidencia verificable del flujo afectado antes de editar cualquier archivo.
3. Si no se reproduce, detener la corrección y devolver evidencia, hipótesis y `NEEDS CLARIFICATION`.
4. Localizar la causa en componentes, estado, rutas, autenticación o adaptador HTTP sin asumir reglas funcionales.
5. Aplicar el cambio mínimo exclusivamente en `apps/frontend/**`.
6. Añadir o ajustar una prueba que falle antes y cubra la regresión después.
7. No cambiar contratos backend para acomodar el bug; emitir un handoff si la causa está fuera del frontend.
8. No borrar archivos ni ejecutar tests/builds sin autorización explícita del usuario en el turno actual.
