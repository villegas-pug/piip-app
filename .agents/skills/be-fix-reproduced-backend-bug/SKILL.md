---
name: be-fix-reproduced-backend-bug
description: Reproducir y corregir bugs del backend Spring PIIP con evidencia antes de editar, cambio mínimo y prueba focalizada. Usar para regresiones de API, transacciones, autorización, concurrencia, documentos, auditoría o persistencia en apps/backend.
---

# Corregir bug backend reproducido

1. Obtener entrada, precondiciones, resultado esperado, resultado observado y error exacto.
2. Reproducir mediante prueba unitaria, de contrato o persistencia, o mediante evidencia determinista equivalente, antes de editar cualquier archivo.
3. Si no se reproduce, detener la corrección y devolver evidencia, hipótesis y `NEEDS CLARIFICATION`.
4. Localizar la causa en API, aplicación, dominio, autorización o persistencia sin cambiar reglas para hacer pasar pruebas.
5. Aplicar el cambio mínimo exclusivamente en `apps/backend/**` y mantener transacciones/autorización/auditoría correctas.
6. Añadir o ajustar una prueba que falle antes y cubra la regresión después.
7. No modificar Angular; devolver un handoff si cambia el comportamiento HTTP observable.
8. No usar SQL nativo, borrar archivos ni ejecutar Maven/Oracle sin autorización explícita del usuario.
