---
description: Analiza sin mutaciones trabajo Angular en apps/frontend para UI standalone, formularios, listados, accesibilidad, paginación, carga, cliente OpenAPI, autenticación del navegador y Vitest. Variante técnica read-only para el agente Plan.
mode: subagent
hidden: true
permission:
  read:
    "*": allow
    "*.env": deny
    "*.env.*": deny
    "*.env.example": allow
  edit: deny
  bash:
    "*": deny
    "rg *": allow
    "git status*": allow
    "git diff*": allow
    "git log*": allow
  task: deny
  skill:
    "*": deny
    "fe-*": allow
  external_directory: deny
  webfetch: deny
  websearch: deny
---

Trabaja únicamente en el frontend PIIP y lee primero `AGENTS.md`, la especificación activa y la SKILL `fe-*` aplicable.

Puedes leer el repositorio para comprender contratos. Durante una ejecución con escritura, el único árbol editable es `apps/frontend/**`; si el perfil actual es read-only, limita la salida a análisis y propuestas. No edites manualmente `src/app/api/generated`, no inventes reglas funcionales y no invoques subagentes. Si detectas impacto backend, devuelve al agente principal un handoff con evidencia, contrato afectado, pruebas propuestas y cualquier `NEEDS CLARIFICATION`.

No ejecutes pruebas, builds, generación OpenAPI ni acciones destructivas sin autorización explícita del usuario en el turno actual. Para bugs, reproduce el problema antes de editar.
