---
name: be-publish-openapi-contract
description: Evolucionar y publicar el contrato OpenAPI del backend PIIP a partir de controladores, DTO y validaciones HTTP. Usar siempre cuando el usuario solicite "cambiar un endpoint", "agregar o quitar un campo del DTO", "actualizar Swagger u OpenAPI", "generar piip-openapi.json" o adaptar un contrato consumido por Angular. Publicar primero el contrato backend y entregar después la sincronización a `fe-sync-openapi-client`.
---

# Publicar contrato OpenAPI backend

## Validar el contrato propietario

1. Identificar el caso de uso, DTO y endpoint propietario. Sin esa relación, el contrato podría describir una estructura que el backend no implementa.
2. No exponer entidades JPA, porque hacerlo acoplaría el contrato HTTP al modelo de persistencia.
3. Comparar el cambio con `PiipRepository` y los consumidores Angular sin modificar frontend, para detectar incompatibilidades antes de entregar el contrato.

## Mantener coherencia HTTP

1. Conservar validaciones de entrada y códigos HTTP coherentes con el comportamiento. Documentar respuestas de error como `application/problem+json` con schema `ProblemDetail`; no declarar un DTO de éxito ni `application/json` para esos errores.
2. Añadir assertions estructurales sobre paths, operaciones, schemas, required/nullable, media types y referencias de `ProblemDetail`; una búsqueda textual no demuestra la forma del contrato.
3. Actualizar la prueba de contrato correspondiente para que el artefacto publicado represente el comportamiento implementado.

## Publicar de forma secuencial

1. Tratar controladores, DTO y manejo HTTP del backend como contrato fuente; `apps/backend/target/piip-openapi.json` es un artefacto generado, no otra fuente editable.
2. Generar el artefacto mediante `OpenApiGenerationTest` y el Gradle Wrapper solo cuando el usuario autorice expresamente en el turno actual `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` en Windows o su equivalente `./gradlew` en Linux/macOS.
3. Comprobar freshness: el artefacto debe provenir del checkout y revisión actuales, y las assertions estructurales deben pasar. Existencia o timestamp aislados no prueban vigencia.
4. No afirmar que el contrato fue publicado si no se generó y verificó en el turno autorizado.
5. Entregar después al agente principal el artefacto verificado y el cambio observable; el frontend regenera su cliente mediante `fe-sync-openapi-client` en una fase separada.

## Límites de alcance y ejecución

1. No modificar frontend; cualquier adaptación se devuelve al agente principal.
2. No borrar archivos ni ejecutar tareas Gradle u Oracle sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar un handoff con:

- Endpoints, métodos y DTO afectados.
- Campos agregados, modificados o retirados.
- Compatibilidad o ruptura respecto del contrato anterior.
- Assertions estructurales de contrato actualizadas.
- Estado y evidencia de freshness de `piip-openapi.json`, con comando utilizado o pendiente de autorización.
- Handoff al agente principal y pasos separados requeridos para `fe-sync-openapi-client`.
