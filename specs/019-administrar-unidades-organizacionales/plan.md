# Plan de implementación: Administración de Unidades Organizacionales

**Rama**: `019-administrar-unidades-organizacionales` | **Fecha**: 2026-09-11 | **Spec**: [`spec.md`](./spec.md)

**Entrada**: especificación en `/specs/019-administrar-unidades-organizacionales/spec.md`

**Nota**: esta plantilla se completa mediante `/speckit-plan`. El plan debe partir del estado real del monorepo y del protocolo `docs/development/spec-kit-adoption.md`.

## Resumen

Incorporar dos CRUD administrativos separados para Unidades Ejecutoras (UE) y Unidades Orgánicas (UO). El backend será el propietario de la autorización institucional, el modelo JPA, la generación concurrente de códigos, las mutaciones auditadas y el contrato HTTP nuevo. El frontend consumirá ese contrato después de su publicación y sincronización OpenAPI para proveer rutas, listados y formularios independientes.

La entrega preserva las tres consultas organizacionales existentes y no usa, expone ni modifica `ID_UNIDAD_PADRE`. La habilitación inicial se limita al perfil exacto `test,test-reset`; no se habilitará en bases Oracle con UE preexistentes ni se definirá una migración de datos reales en esta feature.

## Baseline y evidencia existente

<!--
  Registrar comportamiento ya satisfecho, rutas inspeccionadas, contratos y
  documentación vigente. Esta sección no genera tareas ni convierte trabajo
  histórico en trabajo pendiente.
-->

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| Cadena organizacional persistida | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/persistence/ExecutingUnitEntity.java` y `OrganizationalUnitEntity.java` | Conservar `Institución -> UE -> UO`; ampliar solo los atributos funcionales de UE. |
| Consultas organizacionales actuales | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/api/OrganizationController.java` y `application/OrganizationQueryService.java` | Preservar `GET /institutions`, `GET /executing-units` y `GET /organizational-units?executingUnitId=...` sin cambiar su semántica. |
| Catálogo de UO para portafolio | `OrganizationQueryService.organizationalUnits(...)` | Mantener el filtro de UE solicitada, UO activa y sigla no nula ni vacía. |
| Concurrencia existente | Entidades `ExecutingUnitEntity` y `OrganizationalUnitEntity` | Reutilizar `@Version` para edición y estado; añadir bloqueo del contexto padre solo para serializar la generación. |
| Autorización local | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/LocalAuthorizationService.java` | Añadir una comprobación explícita de `ADMINISTRADOR_PIIP` por institución, revalidada desde Oracle en el servicio. |
| Datos sintéticos | `apps/backend/src/main/resources/db/test/catalog-data.sql` y `config/reset/TestResetCoordinator.java` | Ajustar exclusivamente el seed y sus postvalidaciones a los códigos y campos nuevos bajo `test,test-reset`. |
| Cliente y catálogo Angular actuales | `apps/frontend/src/app/core/piip-http.repository.ts`, `piip.models.ts` y `piip-catalogs.store.ts` | Conservar los modelos y estado legados de portafolio; crear modelos y estado administrativos separados. |
| Relación pendiente | `UNIDAD_ORGANICA.ID_UNIDAD_PADRE` y `OrganizationalUnitEntity.parent` | No tocarla ni incluirla en contratos, UI o casos de uso nuevos. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | Sí | `apps/frontend/src/app/app.routes.ts`, `layout/app-shell.component.*`, `core/piip*.ts`, páginas administrativas nuevas | Consumidor posterior al contrato y a la sincronización autorizada del cliente OpenAPI. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/**`, `identity/**`, `audit/**` | Propietario canónico de reglas, transacciones, seguridad y contrato. |
| Database | Sí | Entidades JPA; `database/generated/piip-oracle.sql` como salida derivada para revisión | Modelo derivado de JPA. Solo perfil `test,test-reset`; sin migración productiva. |
| Contrato HTTP | Sí | Nuevo prefijo `/admin/organization` documentado en `contracts/organization-administration.md` | Backend publica primero; frontend genera y adapta después. |
| Documentación | Sí | `docs/architecture/data-model-final.md`, `docs/funcional/guia-funcional-piip.md` | Actualizar durante la implementación con evidencia verificable. |

## Contexto técnico

**Lenguajes/versiones**: Java 21 con Spring Boot 4.1; TypeScript con Angular 22 standalone.

**Dependencias principales**: Spring MVC, Spring Data JPA, Hibernate, Spring Security/Keycloak, Oracle; Angular Material, señales Angular y cliente OpenAPI generado.

**Persistencia**: Hibernate JPA sobre Oracle. `ExecutingUnitEntity` incorporará `displayOrder`, `registeredAt` y `activatedAt` mapeados respectivamente a `ORDEN_PRESENTACION`, `FECHA_REGISTRO` y `FECHA_ACTIVACION`. En el alta de UE el orden es opcional; si se omite, la transacción bloqueada por institución asigna el mayor orden existente más uno, o `0` si no existe una UE. `AuditEventEntity` incorporará `entityId`, `institutionId`, `executingUnitId` y `organizationalUnitId` como referencias estructuradas. `AuditService` las persistirá dentro de la transacción funcional. La lectura compatible de `GET /audit/events` filtrará siempre por las UE cubiertas por el actor, incluso sin parámetro. El DDL generado se revisará como salida derivada, sin editar `database/generated/piip-oracle.sql` manualmente.

**Validación propuesta**: pruebas focalizadas de servicio, controlador, persistencia, contrato, restablecimiento sintético, repositorio y rutas/componentes. Su ejecución, la generación OpenAPI y el arranque del backend requieren autorización explícita; para la implementación el usuario autorizó levantar el backend solo con el perfil `test,test-reset`.

**Plataforma objetivo**: monorepo PIIP en Windows; entorno funcional habilitado inicialmente solo como `test,test-reset`.

**Restricciones**: JPA es la fuente estructural; no SQL nativo, `JdbcTemplate`, Flyway, Liquibase ni procedimientos. Solo `ADMINISTRADOR_PIIP` con ámbito institucional explícito puede escribir. No hay borrado físico, traslados de contexto ni `ID_UNIDAD_PADRE` en los contratos nuevos. Los códigos se generan en backend por ámbito bajo transacción, sin reutilizar `CodeGeneratorService` de portafolio. Las rutas legadas permanecen compatibles.

**Escala/alcance**: dos flujos administrativos, cinco operaciones administrativas por entidad (alta, consulta, edición, desactivación y reactivación), tres consultas legadas preservadas y una navegación UE a UO. No hay requisito de capacidad adicional; las operaciones se acotan al ámbito institucional autorizado.

## Verificación de la constitución

*GATE: debe aprobarse antes del diseño y volver a revisarse al finalizarlo.*

| Principio / gate | Estado antes y después del diseño | Evidencia y decisión |
|------------------|-----------------------------------|----------------------|
| Autenticación y autorización | Cumple | Keycloak sigue autenticando; el servicio revalida `ADMINISTRADOR_PIIP` y ámbito institucional desde Oracle para cada mutación. |
| Límite transaccional | Cumple | El servicio de administración será `@Transactional`; controlador delgado y entidades no expuestas por HTTP. |
| Esquema JPA canónico | Cumple | Campos de UE y cualquier extensión de auditoría se declaran primero en entidades/anotaciones. El DDL generado solo se revisa como derivado. |
| Prohibición de SQL funcional y migraciones | Cumple | No se crearán SQL nativo, `JdbcTemplate`, Flyway, Liquibase ni procedimientos. La generación de códigos se resuelve con repositorios JPA, lock pesimista y `@Version`. |
| Datos sintéticos restringidos | Cumple con alcance explícito | `catalog-data.sql` sigue siendo DML idempotente exclusivo de `test,test-reset`, con guardias y postvalidaciones. La feature no se habilita en bases existentes ni toca datos reales. |
| Auditoría append-only | Cumple | Cada mutación exitosa deja evento transaccional con actor, entidad, ámbito y diferencia funcional segura; rechazos no crean eventos de éxito. |
| Compatibilidad y documentación | Cumple | Se preservan las tres lecturas existentes; la guía funcional y el modelo de datos se actualizan en la implementación. |

No hay contradicciones constitucionales aprobadas ni `NEEDS CLARIFICATION` bloqueantes.

## Dependencias y secuencia

- **Propietario canónico**: backend de organización, identidad y auditoría; define entidades, autorización, errores y contrato.
- **Consumidores**: cliente OpenAPI generado y frontend Angular; documentación funcional y de arquitectura.
- **Orden obligatorio**: entidades y repositorios JPA -> servicio/autorización/auditoría -> controlador, DTO y pruebas de contrato -> publicación OpenAPI autorizada -> sincronización del cliente -> repositorios, guard, rutas y formularios Angular -> documentación y pruebas de regresión. El seed y sus postvalidaciones se actualizan junto al modelo backend, solo para `test,test-reset`.
- **Paralelización permitida**: tras estabilizar contrato y modelos backend, la documentación puede actualizarse en paralelo con pruebas backend no contractuales. El frontend no comienza cambios dependientes del cliente generado hasta que se publique y sincronice el contrato.

## Estructura del proyecto

### Documentación de la feature

```text
specs/019-administrar-unidades-organizacionales/
├── spec.md
├── plan.md
├── research.md          # Solo si el plan lo requiere
├── data-model.md        # Solo si el plan lo requiere
├── quickstart.md        # Solo si el plan lo requiere
├── contracts/           # Solo si cambia o documenta contratos
└── tasks.md             # Se crea solo mediante /speckit.tasks
```

### Código y documentación afectados

```text
apps/backend/
├── src/main/java/pe/gob/midagri/piip/
│   ├── organization/
│   │   ├── api/OrganizationAdministrationController.java
│   │   ├── api/OrganizationAdministrationDtos.java
│   │   ├── application/OrganizationAdministrationService.java
│   │   ├── application/OrganizationAdministrationCommands.java
│   │   ├── application/OrganizationAdministrationReadModels.java
│   │   └── persistence/{ExecutingUnitEntity,OrganizationalUnitEntity,*Repository}.java
│   ├── identity/application/LocalAuthorizationService.java
│   └── audit/{application,persistence}/**
├── src/main/resources/db/test/catalog-data.sql
└── src/test/java/pe/gob/midagri/piip/{organization,config/reset,contract,persistence}/**

apps/frontend/src/app/
├── app.routes.ts
├── layout/app-shell.component.*
├── core/{piip.models,piip.repository,piip-http.repository,piip-mock.repository}.ts
├── core/*organization-administration*.ts
└── pages/{executing-unit-administration,organizational-unit-administration}/**

docs/
├── architecture/data-model-final.md
└── funcional/guia-funcional-piip.md
```

**Decisión de estructura**: se añade un vertical administrativo de organización separado del controlador y de los modelos de catálogo vigentes. Los DTO, commands y read models administrativos no reutilizan el modelo legado con `parentId`. El frontend mantiene la separación entre catálogo de portafolio y estado administrativo, y navega a UO con `executingUnitId` en la ruta.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: eliminación física, traslado de institución o UE, una jerarquía UO, exposición de `parentId`, cambio de reglas de portafolio, cambio de roles, migración o datos reales, y habilitación en bases Oracle con UE preexistentes.
- **Specs `001`-`005` consultadas**: Ninguna; son antecedentes históricos y no extienden el alcance.
- **Dependencias históricas aprobadas**: feature 017 solo como evidencia del catálogo vigente de UO; no se reimplementa.
- **NEEDS CLARIFICATION**: Ninguna. La habilitación se restringe a `test,test-reset`; el tratamiento de UE reales se difiere explícitamente fuera de esta feature.

## Seguimiento de complejidad

> Completar solo si una decisión contradice la constitución y ha sido aprobada y justificada.

| Contradicción | Necesidad | Alternativa más simple descartada porque |
|---------------|-----------|------------------------------------------|
| No aplica | No hay decisiones que contradigan la constitución. | No aplica. |
