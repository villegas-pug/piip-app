# Adopción incremental de Spec Kit

## Propósito

Este protocolo conecta Spec Kit con el estado real del monorepo PIIP sin convertir documentación histórica en trabajo pendiente. Se aplica únicamente a features nuevas, comenzando por `specs/006-nombre-feature/`.

Las especificaciones `001` a `005` son antecedentes funcionales e históricos. No son backlog, no bloquean nuevas features y no deben completarse retroactivamente con `plan.md` o `tasks.md`.

## Flujo obligatorio

### 1. Grounding previo

Antes de crear una especificación se debe inspeccionar el código, la arquitectura, los contratos y la documentación relacionados con la solicitud. El resultado debe identificar:

- comportamiento existente que constituye el baseline;
- rutas reales potencialmente afectadas;
- impacto en frontend, backend, database, contrato HTTP y documentación;
- propietario canónico de cualquier contrato compartido;
- dependencias y orden entre áreas.

Las specs `001` a `005` se consultan solo como antecedentes del dominio. Si contradicen el código o una fuente vigente, se registra `NEEDS CLARIFICATION`; no se ajusta el código silenciosamente ni se amplía el alcance.

### 2. Especificación

Cada feature nueva se crea en `specs/006-nombre-feature/` o en el siguiente número secuencial disponible. Su `spec.md` debe contener historias priorizadas, requisitos identificados, criterios medibles, límites explícitos y la clasificación de impacto del monorepo.

Un requisito histórico solo puede incorporarse cuando la nueva feature lo modifica directamente y el usuario aprueba esa dependencia. Lo que el código ya satisface se documenta como evidencia del baseline, no como requisito pendiente ni como tarea marcada artificialmente como completada.

### 3. Plan conectado al monorepo

El `plan.md` debe usar rutas y componentes existentes, declarar el impacto por área y respetar `.specify/memory/constitution.md`. Cuando frontend y backend compartan un contrato, el plan identifica al propietario canónico y ordena primero su cambio; el consumidor se adapta después.

Toda contradicción sin resolución se mantiene como `NEEDS CLARIFICATION`. La implementación no puede utilizar una spec histórica para introducir trabajo fuera del alcance aprobado.

### 4. Tareas trazables

El `tasks.md` contiene IDs, checkboxes, rutas concretas y vínculo con al menos una historia o requisito. Debe:

- separar trabajo frontend y backend;
- ordenar dependencias de contrato antes que consumidores;
- incluir exclusivamente trabajo nuevo aprobado;
- mantener la evidencia del baseline fuera del checklist ejecutable;
- distinguir las validaciones propuestas de las validaciones autorizadas.

Las tareas no pueden importar pendientes de `001` a `005` salvo dependencia directa, explícita y aprobada.

### 5. Aprobación e implementación

El ciclo recomendado es `specify -> clarify -> plan -> tasks -> analyze -> implement`. No se ejecuta `implement` hasta que la feature activa tenga `spec.md`, `plan.md` y `tasks.md`, sin checklists ni `NEEDS CLARIFICATION` bloqueantes, y el usuario invoque explícitamente `/speckit-implement` en el turno actual. Esa invocación constituye la aprobación de los artefactos vigentes; no requiere un mensaje de aprobación separado.

La implementación se limita a las tareas aprobadas. Un checkbox cambia a `[X]` únicamente con evidencia del cambio correspondiente. Las pruebas, builds, generación OpenAPI e integración Oracle requieren autorización explícita del usuario en el turno en que se pretendan ejecutar.

## Cierre de una feature

El cierre debe informar tareas completadas y pendientes, cambios realmente realizados, validaciones realmente ejecutadas y cualquier riesgo o `NEEDS CLARIFICATION` todavía abierto. No debe afirmar que una validación pasó si no se ejecutó.

## Estado de la instalación SpecKit (vigente a 2026-09-01)

- **CLI**: `specify-cli` v1.0.3 instalada con `uv tool install specify-cli --from git+https://github.com/github/spec-kit.git@v1.0.3`. Hash pinneado al commit `6906bc582230bb752776e23287ee97990c1af743`.
- **Integraciones**: coexistencia `codex` + `opencode`. `default_integration = "opencode"`; `installed_integrations = ["codex", "opencode"]` en `.specify/integration.json`.
- **Artefactos de cada integración**:
  - Codex: `.agents/skills/speckit-*/SKILL.md` (9 skills core + 5 git), `.codex/agents/*.toml` (subagentes), `.codex/skills/*` (skills de dominio PIIP), `.codex/hooks/piip_scope_guard.py`, `.codex/config.toml`.
  - OpenCode: `.opencode/commands/speckit.*.md` (15 commands nativos), `.opencode/agents/*.md` (subagentes), `.opencode/commands/*.md` (subagentes como commands), `opencode.json` (raíz, permisos de task delegation), `.opencode/node_modules/@opencode-ai/plugin@1.18.12`.
- **Convenio de invocación**:
  - Codex: `/speckit-specify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-implement` (hyphen, estilo Codex).
  - OpenCode: `/speckit.specify`, `/speckit.plan`, `/speckit.tasks`, `/speckit.implement` (dot, estilo OpenCode).
  - Ambas formas resuelven a instrucciones equivalentes pero materialmente distintas; usar la del agente activo.
- **Migración desde v0.8.15**: la regeneración upstream de los 4 scripts PowerShell shared (`common.ps1`, `create-new-feature.ps1`, `setup-plan.ps1`, `setup-tasks.ps1`) y de los 2 templates (`checklist-template.md`, `plan-template.md`) forma parte del upgrade y fue aceptada por contener mejoras legítimas (soporte de `SPECIFY_INIT_DIR`, validación de argumentos desconocidos, notación dot en línea con la convención nativa de OpenCode). Los **overrides** (`templates/overrides/plan-template.md` y `tasks-template.md`) y la **extensión `git`** (`extensions/git/**`) están intactos.
- **Reglas de convivencia**:
  - No desinstalar Codex ni `.codex/` para "limpiar"; el hook `piip_scope_guard.py` y las skills de dominio siguen activas para Codex.
  - No ejecutar `specify init --here --force`: regenera `.specify/` desde cero y borraría los overrides y la Constitución.
  - Para upgrades futuros usar `uv tool upgrade specify-cli` (CLI) y `specify integration upgrade --force` (artefactos de integración), no reinicializaciones destructivas.
  - Toda modificación de `constitution.md`, `extensions.yml` o `extensions/git/git-config.yml` requiere propuesta explícita y ratificación; no se sobreescriben por upgrade.
