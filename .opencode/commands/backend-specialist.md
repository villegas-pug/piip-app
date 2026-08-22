---
description: Delegar una implementación Spring/JPA al especialista backend PIIP
agent: backend-specialist
subtask: true
---

Atiende la solicitud `$ARGUMENTS` únicamente dentro de `apps/backend/**`. Aplica `AGENTS.md` y la SKILL `be-*` correspondiente. Respeta el ownership post-refactor (`api`: HTTP/DTO/validación/ProblemDetail/OpenAPI; `application`: casos de uso/commands/read models/transacciones/autorización/auditoría; `domain`: invariantes; `persistence`: JPA/JPQL), sin crear dependencias `application -> api` ni exponer repositorios o entidades JPA desde controllers. Devuelve al agente principal evidencia, cambios, baseline detectado e impacto cross-domain con propietario y orden de integración.
