# PIIP Monorepo

## Fuente funcional

- `docs/architecture/piip-fields.md` define los 23 campos y seis catálogos.
- No inventes estados, obligatoriedades ni transiciones. La única transición de escritura confirmada es `Presentado -> Iniciativa aprobada`.
- `NA` y `No aplica` tienen significados diferentes.

## Arquitectura

- `apps/backend` es un monolito modular Java 21/Spring Boot 4.1.
- `apps/frontend` es Angular 22 con componentes standalone.
- Keycloak autentica. Oracle autoriza mediante `USUARIO`, `ROL` y `USUARIO_ROL_AMBITO`.
- Hibernate JPA es la fuente canónica del esquema. No uses SQL nativo, `JdbcTemplate`, procedimientos almacenados, Flyway ni Liquibase.
- Los controladores delegan; las reglas y transacciones pertenecen a servicios de aplicación.
- No expongas entidades JPA en contratos HTTP.

## Comandos (manual, no automático)

⚠️ NUNCA ejecutes estos comandos por tu cuenta tras un cambio. Solo ante pedido explícito del usuario en el turno actual. Terminar un cambio ≠ autorización para validarlo.

- Backend: `mvn test` / `mvn verify` (`apps/backend`).
- Frontend: `npm test -- --watch=false` / `npm run build` (`apps/frontend`).
- Integración Oracle: `mvn verify -Pintegration-tests` (requiere Docker o variables Oracle).

## Seguridad

- Nunca versiones secretos, wallets, tokens ni credenciales.
- Un usuario autenticado sin asignación Oracle activa no recibe permisos funcionales.
- Toda operación sensible exige autorización en el servicio y evidencia en auditoría.
- `AUDITORIA_ACCESO` no guarda tokens, cuerpos HTTP ni contenido documental.

## Spec Kit y Codex

- La constitución está en `.specify/memory/constitution.md`.
- Las skills compartidas están en `.agents/skills`.
- Los subagentes están en `.codex/agents` como TOML.
- Las especificaciones y documentación se escriben en español.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
