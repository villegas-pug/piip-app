# PIIP Monorepo

## Fuente funcional

- `docs/architecture/piip-fields.md` define los 23 campos y seis catálogos; `NA` y `No aplica` tienen significados distintos.
- No inventes estados, obligatoriedades ni transiciones. El flujo base conserva `Presentado -> Iniciativa aprobada`; las únicas transiciones adicionales autorizadas son las matrices de la feature 009 ratificadas por la constitución 1.1.0. La presencia de un estado en el catálogo no autoriza otras transiciones.
- Ante un cambio que altere flujos, roles, permisos, datos visibles, documentos o reglas de negocio, evaluar el impacto en la guía funcional de `docs/`. Si hay impacto, actualizarla en la misma entrega con evidencia verificable; si no lo hay, declararlo y justificarlo al cierre. La guía funcional no sustituye las especificaciones, el código, la configuración ni las pruebas como autoridad.

## Arquitectura

- `apps/backend` es un monolito modular Java 21/Spring Boot 4.1; `apps/frontend` usa Angular 22 con componentes standalone.
- Keycloak autentica; Oracle autoriza mediante `USUARIO`, `ROL` y `USUARIO_ROL_AMBITO`.
- Hibernate JPA es la fuente canónica del esquema: no uses SQL nativo, `JdbcTemplate`, procedimientos almacenados, Flyway ni Liquibase para acceso funcional o definición estructural. La única excepción es el DML externo, versionado, sin DDL e idempotente de un perfil destructivo exclusivo de desarrollo/pruebas, con guardias fail-closed y prohibido en producción, conforme a la constitución. Los controladores delegan y las reglas y transacciones pertenecen a servicios de aplicación.
- No expongas entidades JPA en contratos HTTP.

## Contexto: Graphify -> código fuente

- Ante preguntas o tareas sobre arquitectura, módulos, dependencias, símbolos, flujos o impacto, si existe `graphify-out/graph.json`, aplicar la skill global `graphify` mediante una consulta acotada con `graphify query "<pregunta>"` antes de explorar ampliamente el repositorio. Si ya se conoce el archivo o símbolo exacto, inspeccionarlo directamente; si el grafo falta o falla, informarlo y continuar con las fuentes canónicas.
- No cargar `graph.json` ni `GRAPH_REPORT.md` completos; usar el resultado acotado de Graphify y validarlo en código fuente, especificaciones, pruebas o configuración antes de responder, planificar o modificar.
- Graphify es un índice estructural derivado; el repositorio es la autoridad canónica. No derivar reglas de negocio del grafo.
- Después de cambios materiales de código, ejecutar `graphify update .`.

## Comandos (manual, no automático)

⚠️ Nunca ejecutes estos comandos por tu cuenta tras un cambio. Terminar un cambio no autoriza validarlo: requieren pedido explícito del usuario en el turno actual.

- Backend (`apps/backend`): `gradlew.bat test` / `gradlew.bat check` en Windows, o `./gradlew test` / `./gradlew check` en Linux/macOS.
- Frontend (`apps/frontend`): `npm test -- --watch=false` / `npm run build`.
- Integración Oracle: `gradlew.bat integrationTest` en Windows, o `./gradlew integrationTest` en Linux/macOS; requiere Docker o variables Oracle.

## Seguridad

- Nunca versiones secretos, wallets, tokens ni credenciales.
- Un usuario autenticado sin asignación Oracle activa no recibe permisos funcionales.
- Toda operación sensible exige autorización en el servicio y evidencia en auditoría.
- `AUDITORIA_ACCESO` no guarda tokens, cuerpos HTTP ni contenido documental.
- La auditoría es append-only durante la operación normal. El perfil destructivo exclusivo de desarrollo/pruebas puede eliminar y recrear íntegramente sus tablas; esta excepción permanece deshabilitada por defecto y prohibida en producción.

## Spec Kit, Codex y OpenCode

- SpecKit v1.0.3 (CLI `specify-cli`, commit `6906bc5`); la constitución está en `.specify/memory/constitution.md`, los workflows compartidos en `.specify/workflows/speckit/workflow.yml` y la extensión `git` en `.specify/extensions/git/`.
- **Coexistencia Codex + OpenCode**: `installed_integrations = ["codex", "opencode"]`; default es `opencode`. Ambos viven en paralelo, no se reemplazan. Codex usa `.agents/skills/speckit-*/SKILL.md` (frontmatter con `name` + hyphen) y OpenCode usa `.opencode/commands/speckit.*.md` (frontmatter con `description` + dot). OpenCode también auto-descubre las 14 SKILL.md de Codex desde `.agents/skills/` (project agent-compatible). Los subagentes específicos del dominio viven en `.codex/agents/*.toml` (Codex) y `.opencode/agents/*.md` + `.opencode/commands/*.md` (OpenCode); ninguno se replica en el otro.
- **Invocaciones**:
  - Codex (hyphen): `/speckit-specify`, `/speckit-plan`, `/speckit-implement`, etc.
  - OpenCode (dot): `/speckit.specify`, `/speckit.plan`, `/speckit.implement`, etc.
  - Las dos formas apuntan a instrucciones distintas; usar la forma del agente activo. En OpenCode moderno, las SKILL.md en `.agents/skills/` se cargan con su nombre original (`speckit-specify`) como skill nativo.
- Las especificaciones y documentación se escriben en español; el protocolo canónico de adopción incremental está en `docs/development/spec-kit-adoption.md`.
- `specs/001-*` a `specs/005-*` son antecedentes históricos: no son backlog ni se completan retroactivamente. Las nuevas features empiezan en `006` y requieren grounding contra código, arquitectura, contratos y documentación reales, con rutas e impacto por área explícitos.
- Toda contradicción histórica se marca `NEEDS CLARIFICATION`; no amplía el alcance ni autoriza modificar el código.

## Gate Spec Kit

`specify`, `plan` y `tasks` no implementan producto; `analyze` es solo lectura. Ejecutar `implement` solo si la feature activa tiene `spec.md`, `plan.md` y `tasks.md`, no conserva checklists o `NEEDS CLARIFICATION` bloqueantes y el usuario invoca explícitamente el comando de implementación (`/speckit-implement` en Codex, `/speckit.implement` en OpenCode) en el turno actual. Esa invocación aprueba los artefactos vigentes y autoriza únicamente sus tareas de implementación; no autoriza por sí misma pruebas, builds, servidores, OpenAPI, Oracle ni Git. Un plan pegado o `PLEASE IMPLEMENT THIS PLAN` no sustituye la invocación. Los cambios existentes se registran como baseline, no como tarea completada ni trabajo por reimplementar.

## Routing de especialistas

- Delega trabajo exclusivamente Angular a `frontend-specialist` y trabajo exclusivamente Spring/JPA/Oracle a `backend-specialist`. En modo Plan, delega solo análisis sin mutaciones; en ejecución, cada especialista escribe únicamente en su árbol: `apps/frontend/**` o `apps/backend/**`.
- Si frontend y backend son independientes, invoca ambos en paralelo. No paralelices cuando compartan contrato HTTP, artefactos generados, catálogos, reglas funcionales, documentación o configuración; para cambios dependientes, delega primero al propietario canónico y luego al consumidor.
- Los especialistas no se invocan entre sí: devuelven al agente principal evidencia, impacto cross-domain, pruebas propuestas y cualquier `NEEDS CLARIFICATION`. Los perfiles OpenCode `*-plan` son variantes técnicas ocultas y read-only de los mismos dos roles lógicos.
- Ningún especialista ejecuta pruebas, builds, generación OpenAPI, integración Oracle ni acciones destructivas sin autorización explícita del usuario en el turno actual.

<!-- SPECKIT START -->
Para la feature activa, leer `specs/014-consolidar-asignaciones-usuarios/plan.md` antes de planificar o implementar.
<!-- SPECKIT END -->
