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

1. Conservar validaciones de entrada, códigos HTTP y errores coherentes con los contratos existentes. Cambiarlos incidentalmente produciría una incompatibilidad funcional adicional.
2. Actualizar la prueba de contrato correspondiente para que el artefacto publicado represente el comportamiento implementado.

## Publicar de forma secuencial

1. Generar `apps/backend/target/piip-openapi.json` mediante `OpenApiGenerationTest` y el Gradle Wrapper solo cuando el usuario autorice expresamente en el turno actual `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` en Windows o su equivalente `./gradlew` en Linux/macOS.
2. No afirmar que el contrato fue publicado si el artefacto no se generó.
3. Completar esta publicación antes de regenerar el cliente frontend, porque Angular debe consumir el contrato canónico y no una propuesta intermedia.

## Límites de alcance y ejecución

1. No modificar frontend; cualquier adaptación se devuelve al agente principal.
2. No borrar archivos ni ejecutar tareas Gradle u Oracle sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar un handoff con:

- Endpoints, métodos y DTO afectados.
- Campos agregados, modificados o retirados.
- Compatibilidad o ruptura respecto del contrato anterior.
- Prueba de contrato actualizada.
- Estado de `piip-openapi.json` y comando utilizado o pendiente de autorización.
- Pasos requeridos para `fe-sync-openapi-client`.
