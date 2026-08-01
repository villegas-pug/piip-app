# PIIP Monorepo

Sistema de Gestión de Iniciativas y Proyectos de Innovación Pública.

## Estructura

- `apps/frontend`: Angular 22.
- `apps/backend`: Spring Boot 4.1, Java 21 y Hibernate JPA.
- `docs/architecture`: modelo final y trazabilidad con el Excel.
- `.specify`: constitución y artefactos de GitHub Spec Kit.

El backend requiere Oracle y un emisor OIDC configurados mediante variables de entorno. No uses valores reales en archivos versionados.

## Especificaciones

GitHub Spec Kit `v0.8.15` está fijado para este repositorio. En Windows se inicializó con integración Codex, skills y scripts PowerShell. Las especificaciones `specs/001-*` a `specs/005-*` se conservan como referencias históricas y no constituyen backlog. La adopción completa se aplica únicamente a features nuevas desde `006`, mediante el ciclo `specify -> plan -> tasks -> aprobación explícita -> implement`.

El protocolo, el grounding obligatorio contra el monorepo y las reglas de trazabilidad están en [docs/development/spec-kit-adoption.md](docs/development/spec-kit-adoption.md). La constitución PIIP prevalece sobre cualquier plantilla genérica.

## Verificación

- Backend: `cd apps/backend; mvn verify`.
- DDL Oracle revisable: `database/generated/piip-oracle.sql`; `mvn verify` regenera la copia de trabajo en `apps/backend/target/piip-oracle.sql`.
- Cliente API: `cd apps/frontend; npm run api:generate` después de generar `apps/backend/target/piip-openapi.json`.
- Frontend: `cd apps/frontend; npm test -- --watch=false; npm run build`.
- Oracle: `mvn verify -Pintegration-tests` en CI con Docker o una instancia Oracle de pruebas.

La configuración y el recorrido de aceptación del ambiente institucional están en [docs/deployment/institutional-development.md](docs/deployment/institutional-development.md). La configuración local no usa datos mock ni tokens manuales: si el backend o Keycloak no están disponibles, la UI muestra el error de integración.
