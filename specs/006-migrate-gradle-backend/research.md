# Research: Migrar backend a Gradle

## Decisiones

### D1 — Tratar la migración como baseline existente

- **Decisión**: documentar los archivos Gradle ya presentes y no crear tareas para reimplementar el cambio.
- **Razón**: la implementación ocurrió antes de completar Spec Kit y el objetivo actual es recuperar la trazabilidad.
- **Alternativas descartadas**: revertir el árbol o volver a ejecutar la migración; ambas acciones ampliarían el alcance y podrían perder trabajo existente.

### D2 — Conservar `target/` como ubicación de artefactos

- **Decisión**: mantener `apps/backend/target/` para OpenAPI y DDL.
- **Razón**: frontend, CI y documentación ya consumen esas rutas.
- **Alternativas descartadas**: mover artefactos a `build/`, porque rompería consumidores existentes.

### D3 — No ejecutar infraestructura en esta fase

- **Decisión**: no ejecutar Docker, Testcontainers, integración Oracle, pruebas ni builds.
- **Razón**: esta fase solo completa artefactos Spec Kit y el usuario indicó que la implementación ya está realizada.
- **Alternativas descartadas**: validar ahora el backend; requiere autorización independiente y puede modificar artefactos de trabajo.

## Evidencia consultada

- `apps/backend/build.gradle.kts`
- `apps/backend/settings.gradle.kts`
- `apps/backend/gradlew`
- `apps/backend/gradlew.bat`
- `.github/workflows/ci.yml`
- `README.md`
- `docs/deployment/institutional-development.md`
- `.codex/hooks/piip_scope_guard.py`
