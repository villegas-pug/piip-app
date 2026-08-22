# Arquitectura modular interna del backend PIIP

## Fronteras

- `api` recibe binding HTTP, validación de entrada, delegación y construcción de respuestas externas.
- `application` posee casos de uso, autorización funcional, transacciones, coordinación de repositorios y read models no persistentes.
- `domain` conserva invariantes y operaciones existentes cuando ya tienen responsabilidad justificada.
- `persistence` encapsula entidades y repositorios JPA. No se agregan SQL nativo, `JdbcTemplate`, procedimientos, Flyway ni Liquibase.

## Modelos propietarios

`organization/application/OrganizationReadModels` posee `OrganizationalUnitView`; audit, identity, work y dashboard poseen sus respectivos read models. Los cinco campos documentales heredados de `PortfolioRecordResponse` continúan presentes y nulos cuando esa es la salida vigente.

## Errores y seguridad

Los errores funcionales viven en `shared/application/error` y `ApiExceptionHandler` traduce tipos conocidos a `ProblemDetail`. `IllegalStateException` técnico no se convierte genéricamente en 422. `LocalAuthorizationService` conserva `SecurityContextHolder`, `AccessDeniedException` y evalúa grants exactos; `recordAuthentication` mantiene su transacción independiente.

## Transacciones y efectos

Las consultas de application son `readOnly`. Portfolio separa consultas en `PortfolioQueryService` y comandos en `InitiativeApplicationService` y `ProjectApplicationService`; estos integran `PortfolioDocumentService` y `PortfolioWorkService` dentro de una única transacción para versión, entidad, tareas/notificaciones, documentos y `AuditService.event`. `AuditService.access` mantiene `REQUIRES_NEW`. `DashboardPortfolioService` permanece como patrón conforme.

## Alcance

La feature no modifica frontend, cliente generado, OpenAPI, entidades JPA, DDL, Oracle, credenciales, defaults de pruebas ni guía funcional cuando la equivalencia observable se conserva.
