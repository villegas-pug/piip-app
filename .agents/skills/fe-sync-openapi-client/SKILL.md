---
name: fe-sync-openapi-client
description: Sincronizar el cliente Angular generado desde el contrato OpenAPI vigente y adaptar `PiipRepository`, `PiipHttpRepository` y los modelos de presentación. Usar siempre cuando el usuario diga "actualiza o regenera el cliente API", "cambiaron los DTO o endpoints", "hay errores en el código generado", "consume el nuevo contrato" o exista un `piip-openapi.json` aprobado que deba incorporarse en `apps/frontend`. No modificar el backend desde esta SKILL.
---

# Sincronizar cliente OpenAPI frontend

## Validar el contrato de entrada

1. Verificar que `apps/backend/target/piip-openapi.json` provenga del contrato backend vigente y que su generación haya sido autorizada. Consumir un artefacto desactualizado propagaría modelos o endpoints incorrectos.
2. Comparar el contrato con `apps/frontend/ng-openapi-gen.json` y con los consumidores actuales para identificar el impacto antes de regenerar.

## Regenerar de forma controlada

1. Ejecutar `npm run api:generate` solo con autorización explícita del usuario en el turno actual.
2. Informar antes de ejecutarlo que la configuración `removeStaleFiles` puede retirar archivos generados obsoletos. Ese efecto debe formar parte de la autorización, porque la generación puede cambiar o eliminar artefactos dentro del árbol generado.
3. No editar manualmente `apps/frontend/src/app/api/generated/**`, porque una regeneración posterior sobrescribiría esos cambios.

## Adaptar los consumidores

1. Adaptar mapeos y puertos en `PiipRepository`, `PiipHttpRepository` y modelos de presentación, sin filtrar los DTO generados directamente a la UI. Esto mantiene separado el contrato externo del modelo de presentación.
2. Conservar el manejo de errores, carga, versiones optimistas y ámbitos autorizados, porque regenerar tipos no debe degradar esas garantías.
3. Informar incompatibilidades al agente principal y no modificar backend desde esta SKILL.

## Límites de alcance y ejecución

1. Modificar únicamente `apps/frontend/**`.
2. No borrar archivos manualmente ni ejecutar tests o builds adicionales sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Contrato OpenAPI utilizado y su procedencia.
- Servicios, modelos y operaciones generadas que cambiaron.
- Archivos obsoletos retirados por el generador, si los hubo.
- Adaptaciones realizadas en repositorios y modelos de presentación.
- Incompatibilidades que requieran intervención backend.
- Comando de generación, pruebas y builds ejecutados o pendientes de autorización.
