# Plan de implementación: Consolidación de asignaciones de usuarios

**Rama**: `refactor/backend` | **Fecha**: 2026-08-23 | **Spec**: [spec.md](./spec.md)

**Entrada**: especificación en `/specs/014-consolidar-asignaciones-usuarios/spec.md`

## Resumen

Consolidar la creación, auto-reactivación, edición, suspensión y reactivación de asignaciones locales de rol y ámbito, manteniendo a Keycloak fuera del ciclo de vida funcional. El backend seguirá siendo propietario de autorización, concurrencia, cobertura mínima, auditoría y contrato; Angular consumirá el OpenAPI publicado, aplicará prevención coherente y refrescará el contexto del actor después de toda mutación propia. La asignación suspendida exacta más reciente se reutilizará con `POST 200`, una creación real devolverá `201`, y cada error publicará `ProblemDetail.problemCode` para compartir una semántica estable con la auditoría HTTP.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| Controlador delgado con las cinco mutaciones y queries administrativas vigentes | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/api/UserAdministrationController.java` | Conservar rutas; adaptar `POST` a `ResponseEntity` para distinguir `201/200` y documentar errores. |
| Servicio transaccional con revalidación persistida, versión, duplicados y cobertura parcial | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/UserAdministrationService.java` | Reforzar locks, reglas y auditoría; retirar la dependencia `application -> api` dentro del alcance tocado. |
| Entidad versionada con suspensión/reactivación reversible | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/persistence/UserRoleScopeEntity.java` | Reutilizar sin cambiar `USUARIO_ROL_AMBITO`; no crear otra identidad histórica cuando exista una coincidencia suspendida. |
| Locks JPA para usuario, scope, duplicados y administradores | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/persistence/{UserRepository.java,UserRoleScopeRepository.java}` | Ampliar consultas bloqueantes y ordenar locks para actor, destinatario, UEs y scopes. |
| Auditoría HTTP `REQUIRES_NEW` y eventos funcionales transaccionales | `apps/backend/src/main/java/pe/gob/midagri/piip/audit/{api/AccessAuditFilter.java,application/AuditService.java,persistence/AccessAuditEntity.java}` | Mantener fronteras; agregar un motivo seguro codificado a accesos y completar snapshots funcionales. |
| `ProblemDetail` transversal sin discriminador estable | `apps/backend/src/main/java/pe/gob/midagri/piip/shared/api/ApiExceptionHandler.java` | Añadir `problemCode`; no acoplar Angular a `detail`. |
| Cliente Angular generado y repositorio HTTP manual | `apps/frontend/src/app/api/generated/`, `apps/frontend/src/app/core/piip-http.repository.ts` | Regenerar solo después del OpenAPI autorizado y adaptar el repositorio al estado HTTP de `POST`. |
| Vista existente de administración con edición y transiciones | `apps/frontend/src/app/pages/user-administration/` | Consolidar matriz de acciones, errores por regla y refresco fail-closed sin crear otra pantalla. |
| Guía funcional ya distingue asignación, UE, ámbito institucional y Keycloak | `docs/funcional/guia-funcional-piip.md` | Actualizar auto-reactivación, autosuspensión, resultados HTTP, errores y refresco propio. |
| Diseño histórico aprobado de administración integral | `specs/008-administrar-usuarios/` | Usar como antecedente, no como backlog ni prueba de comportamiento ejecutado. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | Sí | `apps/frontend/src/app/{api/generated,core}/**`, `apps/frontend/src/app/pages/user-administration/**`, `apps/frontend/src/app/pages/audit/**` y pruebas focalizadas | Consumidor posterior del contrato backend. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/{identity,audit,shared,config}/**` y pruebas equivalentes | Propietario canónico de reglas, transacción y contrato. |
| Database | Sí | `apps/backend/.../audit/persistence/AccessAuditEntity.java`, `database/generated/piip-oracle.sql` | Cambio derivado de JPA: columna nullable `MOTIVO_SEGURO`; sin SQL funcional, migrador ni acceso Oracle durante planificación. |
| Contrato HTTP | Sí | `/api/v1/admin/**`, `/api/v1/identity/me`, `/api/v1/audit/accesses`, `ProblemDetail`, OpenAPI y cliente generado | Backend primero; Angular consume después de publicación autorizada. |
| Documentación | Sí | `docs/funcional/guia-funcional-piip.md`, `specs/014-consolidar-asignaciones-usuarios/**` | Reflejar reglas visibles y límites sin sustituir código/spec. |

## Contexto técnico

**Lenguajes/versiones**: Java 21, Spring Boot 4.1.0, Angular 22, TypeScript 6.0, RxJS 7.8 y Vitest 4.

**Dependencias principales**: Spring Web/Security/OAuth2 Resource Server, Spring Data JPA/Hibernate, Bean Validation, springdoc-openapi 3.0.1, Oracle JDBC 23.26, Angular Material, Keycloak JS 26.2 y `ng-openapi-gen` 1.0.5.

**Persistencia**: Hibernate JPA sobre Oracle. `USUARIO_ROL_AMBITO` permanece intacta; `AUDITORIA_ACCESO` incorpora únicamente `MOTIVO_SEGURO` nullable, generado desde la entidad JPA.

**Validación propuesta**: pruebas unitarias y JPA de servicio/repositorios, concurrencia y rollback backend; pruebas de controller/handler/auditoría/contrato; pruebas Vitest de repositorio, componente, navegación y refresco; generación OpenAPI, regeneración de cliente, suites y builds solo con autorización explícita posterior.

**Plataforma objetivo**: monorepo PIIP en Windows, backend Spring/Oracle y SPA Angular autenticada mediante Keycloak.

**Restricciones**: Keycloak solo autentica; Oracle autoriza. No cambiar `USUARIO.ACTIVO`, roles ni catálogos; no exponer entidades; no SQL nativo, `JdbcTemplate`, procedimientos, Flyway o Liquibase; no capturar tokens, cuerpos ni `detail` libre en auditoría; backend autoritativo; cliente generado no se edita a mano.

**Escala/alcance**: ocho rutas relacionadas, cuatro operaciones de escritura directas más la auto-reactivación de `POST`, cinco reglas `422` específicas más el fallback compatible `BUSINESS_RULE_VIOLATION`, cuatro componentes del snapshot de asignación y validación de todas las UEs activas cubiertas por un grant institucional.

## Verificación de la constitución

*GATE inicial y posterior al diseño: APROBADO.*

- La feature no altera los 23 campos, seis catálogos ni transiciones del portafolio.
- Keycloak conserva autenticación; las asignaciones Oracle activas y vigentes siguen gobernando capacidades funcionales.
- Los controllers validan/adaptan; autorización, transacción, locks, reglas y auditoría permanecen en application/persistence.
- La dependencia vigente `UserAdministrationService -> AdminDtos` se elimina en el alcance tocado mediante commands/read models de application y un mapper HTTP.
- `USUARIO_ROL_AMBITO` conserva JPA, identidad y `@Version`; el único cambio estructural es `MOTIVO_SEGURO` en `AccessAuditEntity`, del que se deriva el DDL versionado.
- Los eventos funcionales exitosos son append-only y atómicos con la mutación; los rechazos solo producen auditoría HTTP `REQUIRES_NEW`.
- No se usa la excepción de DML inicial ni el reset destructivo de auditoría.
- La guía funcional se actualiza porque cambian resultados visibles, reglas de suspensión, auto-reactivación y recuperación de sesión.
- No quedan contradicciones ni `NEEDS CLARIFICATION` bloqueantes después del diseño.

## Dependencias y secuencia

- **Propietario canónico**: backend `identity` para casos de uso y contrato; `shared` para `ProblemDetail`; `audit` para evidencia.
- **Consumidores**: cliente OpenAPI, `PiipHttpRepository`, pantalla de Administración de usuarios y proyección de accesos de Auditoría.
- **Orden obligatorio**: commands/read models y códigos de problema → locks/modelo de auditoría → casos de uso y auditoría funcional → controller/OpenAPI → publicación autorizada → cliente generado → repositorio/UI → guía y validación autorizada.
- **Paralelización permitida**: pruebas backend independientes después de estabilizar firmas; pruebas de UI independientes entre sí después de regenerar el cliente. No paralelizar backend y frontend mientras comparten contrato.

## Estructura del proyecto

### Documentación de la feature

```text
specs/014-consolidar-asignaciones-usuarios/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── user-assignment-administration.openapi.yaml
└── tasks.md
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/
├── identity/
│   ├── api/{AdminDtos,UserAdministrationController,UserAdministrationHttpMapper}.java
│   ├── application/{UserAdministrationService,UserAdministrationCommands,UserAdministrationReadModels}.java
│   └── persistence/{UserRepository,UserRoleScopeRepository,UserRoleScopeEntity}.java
├── audit/
│   ├── api/{AccessAuditFilter,AuditController}.java
│   ├── application/{AuditService,AuditQueryService,AuditReadModels}.java
│   └── persistence/AccessAuditEntity.java
├── shared/{api/ApiExceptionHandler.java,application/error/**}
└── config/OpenApiConfig.java

apps/backend/src/test/java/pe/gob/midagri/piip/{identity,audit,shared,contract}/**
apps/frontend/src/app/{api/generated,core}/**
apps/frontend/src/app/pages/{user-administration,audit}/**
database/generated/piip-oracle.sql
docs/funcional/guia-funcional-piip.md
```

**Decisión de estructura**: `identity.application` recibe commands y devuelve read models/resultados propios; `identity.api` adapta HTTP sin filtrar DTO hacia application. Los repositorios JPA serializan usuarios, coincidencias y coberturas. `shared` define la discriminación transversal de errores y `audit` persiste el mismo código seguro. Angular solo interpreta el contrato y nunca reemplaza la validación servidor.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: cuentas/credenciales/estado Keycloak; alta o eliminación de `USUARIO`; `USUARIO.ACTIVO`; eliminación física de asignaciones; nuevos roles o catálogos; Unidad Orgánica; autenticación, issuer, audience, PKCE o tokens; capacidades ajenas a Administración de usuarios; cambios del portafolio; ejecución de Oracle, servidores, pruebas, builds o generación durante `plan`.
- **Specs `001`-`005` consultadas**: Ninguna; son antecedentes históricos y no backlog.
- **Dependencias históricas aprobadas**: feature 008 para administración integral y grants exactos; constitución 1.2.0 para arquitectura/autorización/auditoría. La feature 013 se leyó solo por el marcador operativo de `AGENTS.md` y no aporta reglas funcionales a esta feature.
- **NEEDS CLARIFICATION**: Ninguna.

## Seguimiento de complejidad

No se identifican contradicciones con la constitución ni excepciones que requieran justificación.
