---
name: be-fix-reproduced-backend-bug
description: Reproducir y corregir regresiones del backend Spring PIIP con evidencia previa, cambio mínimo y prueba focalizada. Usar siempre cuando el usuario diga que "la API falla", "devuelve un resultado incorrecto", "no guarda", "duplica registros", "la transacción no funciona", "la autorización falla" o reporte errores en documentos, auditoría, concurrencia o persistencia de `apps/backend`. No editar hasta reproducir el problema o reunir evidencia determinista equivalente.
---

# Corregir bug backend reproducido

## Reproducir antes de editar

1. Obtener entrada, precondiciones, resultado esperado, resultado observado y error exacto. Sin una diferencia verificable entre ambos resultados no existe una reproducción suficiente.
2. Reproducir mediante una prueba unitaria, de contrato o persistencia, o mediante evidencia determinista equivalente, antes de editar cualquier archivo.
3. Si la reproducción requiere ejecutar una tarea Gradle u Oracle y no existe autorización explícita en el turno actual, detenerse antes de editar y solicitarla.
4. Si el problema no se reproduce, detener la corrección y devolver evidencia, hipótesis y `NEEDS CLARIFICATION`. Cambiar código sin reproducción podría ocultar la causa o introducir otra regresión.

## Corregir la causa

1. Localizar la causa en API, aplicación, dominio, autorización o persistencia sin cambiar reglas funcionales para hacer pasar pruebas.
2. Aplicar el cambio mínimo exclusivamente en `apps/backend/**`. Un cambio más amplio dificulta demostrar qué resolvió la regresión.
3. Preservar los límites portables: `api` adapta HTTP/DTO/validación/`ProblemDetail`/OpenAPI; `application` orquesta casos de uso, transacciones, autorización y auditoría; `domain` conserva invariantes; `persistence` usa JPA/JPQL. No introducir dependencias `application -> api` ni repositorios o entidades JPA en controllers.
4. Tratar violaciones preexistentes como baseline, no como permiso para repetirlas. Si el cambio no puede evitar el baseline, aislarlo y reportarlo sin ampliar la corrección.
5. Si cambia un error HTTP observable, conservar `application/problem+json`, schema `ProblemDetail` y assertions estructurales OpenAPI; entregar la publicación del artefacto como fase autorizada separada.
6. Mantener transacciones, autorización y auditoría correctas aunque el defecto observado pertenezca a una sola capa.
7. Añadir o ajustar una prueba diseñada para fallar antes del cambio y cubrir la regresión después. No afirmar que pasa hasta haberla ejecutado con autorización.

## Límites de alcance y ejecución

1. No modificar Angular. Si cambia el comportamiento HTTP observable, devolver un handoff al agente principal.
2. No usar SQL nativo ni borrar archivos, porque ninguna de esas acciones es necesaria para una corrección mínima.
3. No ejecutar tareas Gradle de prueba, build o `integrationTest` sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Evidencia de reproducción previa.
- Causa raíz localizada.
- Cambio mínimo y archivos afectados.
- Guardas de límites modulares y contrato HTTP preservadas.
- Prueba de regresión añadida o ajustada.
- Resultado de las pruebas autorizadas y cobertura no ejecutada.
- Handoff por cambios HTTP observables.
- `NEEDS CLARIFICATION` si la reproducción o la regla esperada continúa incierta.
