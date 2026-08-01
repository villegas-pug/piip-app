---
name: fe-sync-openapi-client
description: Sincronizar el cliente Angular generado desde el contrato OpenAPI backend y adaptar PiipRepository o PiipHttpRepository. Usar cuando exista un artefacto piip-openapi.json aprobado o cambien DTO y endpoints consumidos por apps/frontend.
---

# Sincronizar cliente OpenAPI frontend

1. Verificar que `apps/backend/target/piip-openapi.json` proviene del contrato backend vigente y que el usuario autorizó la generación.
2. Comparar el contrato con `apps/frontend/ng-openapi-gen.json` y los consumidores actuales.
3. Ejecutar `npm run api:generate` solo ante autorización explícita del usuario en el turno actual.
4. No editar manualmente `apps/frontend/src/app/api/generated/**`.
5. Adaptar mapeos y puertos en `PiipRepository`, `PiipHttpRepository` y modelos de presentación sin filtrar DTO generados a la UI.
6. Conservar manejo de errores, carga, versiones optimistas y ámbitos autorizados.
7. Informar incompatibilidades al agente principal; no modificar el backend desde esta SKILL.
8. No borrar archivos ni ejecutar tests/builds adicionales sin autorización explícita.
