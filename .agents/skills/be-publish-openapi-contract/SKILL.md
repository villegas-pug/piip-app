---
name: be-publish-openapi-contract
description: Evolucionar y publicar el contrato OpenAPI del backend PIIP mediante controladores, DTO y validaciones HTTP. Usar cuando cambien endpoints o modelos consumidos por Angular y se requiera producir piip-openapi.json.
---

# Publicar contrato OpenAPI backend

1. Identificar el caso de uso, DTO y endpoint propietario sin exponer entidades JPA.
2. Mantener validaciones de entrada, códigos HTTP y errores coherentes con los contratos existentes.
3. Evaluar compatibilidad con `PiipRepository` y consumidores Angular, sin modificar frontend.
4. Actualizar la prueba de contrato correspondiente.
5. Generar `apps/backend/target/piip-openapi.json` solo cuando el usuario autorice expresamente el comando Maven necesario.
6. Devolver al agente principal un handoff con endpoints, campos, compatibilidad y pasos para `fe-sync-openapi-client`.
7. Serializar esta publicación antes de cualquier regeneración frontend.
8. No borrar archivos ni ejecutar Maven/Oracle sin autorización explícita.
