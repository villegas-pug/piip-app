# PIIP Monorepo

## Fuente funcional

- `docs/architecture/piip-fields.md` define los 23 campos y seis catálogos; `NA` y `No aplica` tienen significados distintos.
- No inventes estados, obligatoriedades ni transiciones. La única transición de escritura confirmada es `Presentado -> Iniciativa aprobada`.

## Arquitectura

- `apps/backend` es un monolito modular Java 21/Spring Boot 4.1; `apps/frontend` usa Angular 22 con componentes standalone.
- Keycloak autentica; Oracle autoriza mediante `USUARIO`, `ROL` y `USUARIO_ROL_AMBITO`.
- Hibernate JPA es la fuente canónica del esquema: no uses SQL nativo, `JdbcTemplate`, procedimientos almacenados, Flyway ni Liquibase. Los controladores delegan y las reglas y transacciones pertenecen a servicios de aplicación.
- No expongas entidades JPA en contratos HTTP.

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

## Spec Kit y Codex

- La constitución está en `.specify/memory/constitution.md`, las skills compartidas en `.agents/skills` y los subagentes en `.codex/agents` como TOML.
- Las especificaciones y documentación se escriben en español; el protocolo canónico de adopción incremental está en `docs/development/spec-kit-adoption.md`.
- `specs/001-*` a `specs/005-*` son antecedentes históricos: no son backlog ni se completan retroactivamente. Las nuevas features empiezan en `006` y requieren grounding contra código, arquitectura, contratos y documentación reales, con rutas e impacto por área explícitos.
- Toda contradicción histórica se marca `NEEDS CLARIFICATION`; no amplía el alcance ni autoriza modificar el código.

## Gate Spec Kit

`specify`, `plan` y `tasks` no implementan producto; `analyze` es solo lectura. Ejecutar `implement` solo si existen `spec.md`, `plan.md` y `tasks.md` y el usuario los aprobó explícitamente en el turno actual. Un plan pegado o `PLEASE IMPLEMENT THIS PLAN` no es autorización. Los cambios existentes se registran como baseline, no como tarea completada ni trabajo por reimplementar.

## Routing de especialistas

- Delega trabajo exclusivamente Angular a `frontend-specialist` y trabajo exclusivamente Spring/JPA/Oracle a `backend-specialist`. En modo Plan, delega solo análisis sin mutaciones; en ejecución, cada especialista escribe únicamente en su árbol: `apps/frontend/**` o `apps/backend/**`.
- Si frontend y backend son independientes, invoca ambos en paralelo. No paralelices cuando compartan contrato HTTP, artefactos generados, catálogos, reglas funcionales, documentación o configuración; para cambios dependientes, delega primero al propietario canónico y luego al consumidor.
- Los especialistas no se invocan entre sí: devuelven al agente principal evidencia, impacto cross-domain, pruebas propuestas y cualquier `NEEDS CLARIFICATION`. Los perfiles OpenCode `*-plan` son variantes técnicas ocultas y read-only de los mismos dos roles lógicos.
- Ningún especialista ejecuta pruebas, builds, generación OpenAPI, integración Oracle ni acciones destructivas sin autorización explícita del usuario en el turno actual.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
