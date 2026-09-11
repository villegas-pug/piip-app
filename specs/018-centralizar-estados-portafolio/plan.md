# Plan de implementación: Centralización de estados del portafolio

**Rama**: `018-centralizar-estados-portafolio` | **Fecha**: 2026-09-10 | **Spec**: [spec.md](./spec.md)

**Entrada**: especificación en `specs/018-centralizar-estados-portafolio/spec.md`

**Nota**: este plan se completó mediante `/speckit-plan` a partir del estado real del monorepo y del protocolo `docs/development/spec-kit-adoption.md`.

## Resumen

La feature centraliza los once estados del portafolio en un catálogo persistente y consultable, sin alterar códigos, denominaciones iniciales, significados, históricos ni matrices vigentes. El enfoque técnico, basado en evidencia del repositorio y en los análisis read-only de los especialistas backend y frontend, es:

1. **Catálogo especializado por código natural**: nueva tabla `ESTADO_PORTAFOLIO` derivada de una entidad JPA del módulo `portfolio` (`PortfolioStatusCatalogEntity`), con `CODIGO` (tipado como el enum `PortfolioStatus`) como clave primaria inmutable, `NOMBRE`, `ORDEN_PRESENTACION`, `ACTIVO` y `APLICABILIDAD`. Se descarta reutilizar `CATALOGO_ITEM` porque exige identificador interno o FK compuesta y no modela aplicabilidad; el proyecto ya tiene precedente de catálogo especializado (`TIPO_DOCUMENTO`).
2. **Columna `ESTADO` conservada**: `REGISTRO_PORTAFOLIO.ESTADO` sigue almacenando el código textual del enum; se añade una asociación JPA de solo lectura (`insertable=false, updatable=false`) hacia `ESTADO_PORTAFOLIO.CODIGO`, que crea la FK de integridad y carga la metadata vigente sin reescribir valores históricos.
3. **Identidad por código en todo el stack**: el enum `PortfolioStatus` permanece como tipo del código y sede de las matrices; `label()` y `values()` dejan de ser fuente de inventario o denominación visibles. Contratos, filtros, transiciones, dashboard, documentos y auditoría resuelven denominaciones desde el catálogo o desde la metadata incluida en cada registro.
4. **Validación por causas distinguibles**: creación, aprobación y transición validan el destino en orden existencia → actividad → aplicabilidad → matriz, con cuatro `ProblemCode` HTTP estables (422). El estado de origen inactivo no se rechaza: puede abandonarse si la matriz y el destino lo permiten.
5. **Seed descartable coordinado**: los once estados se incorporan a los dos artefactos DML existentes (`apps/backend/src/main/resources/db/test/catalog-data.sql` y `database/dml/seed/catalog-data.sql`) mediante MERGE por código natural con solo `WHEN NOT MATCHED INSERT`, postvalidación fail-closed del inventario exacto, y ejecución exclusiva bajo la activación exacta y ordenada `test,test-reset`. La preparación productiva queda explícitamente incompleta (FR-029).

El detalle de decisiones está en [research.md](./research.md), el modelo persistente en [data-model.md](./data-model.md), el contrato de diseño en [contracts/portfolio-statuses-api.md](./contracts/portfolio-statuses-api.md) y la guía de validación manual en [quickstart.md](./quickstart.md).

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|------------------------------|
| Enum `PortfolioStatus` con exactamente los once códigos y etiquetas en el orden oficial | `apps/backend/.../portfolio/domain/PortfolioStatus.java` | Conservar las constantes como identidad de código; retirar `label()` como fuente de nombre visible |
| Matrices exactas de iniciativa y proyecto en la entidad | `apps/backend/.../portfolio/persistence/PortfolioRecordEntity.java` (`approve`, `transitionInitiativeTo`, `transitionProjectTo`) | Mantenerlas intactas y separadas del catálogo; nunca derivarlas de metadata |
| Estados iniciales asignados en factories (`PRESENTED`, `PROJECT_IN_PROGRESS`) | `PortfolioRecordEntity.java` | Conservar la asignación; añadir validación persistente de existencia/actividad/aplicabilidad |
| Columna `ESTADO` como enum textual con índice `(TIPO_REGISTRO, ESTADO)` | `PortfolioRecordEntity.java` | Conservar nombre, tipo y valores; añadir asociación read-only a `ESTADO_PORTAFOLIO` |
| Consulta central sin estados (recordTypes, 4 catálogos persistentes, documentTypes) | `apps/backend/.../catalogs/application/CatalogQueryService.java`, `catalogs/api/CatalogDtos.java` | Ampliar el bundle con `portfolioStatuses` siguiendo el patrón vigente |
| Precedente de catálogo especializado con código único | `apps/backend/.../documents/persistence/DocumentTypeEntity.java`, `DocumentTypeRepository.java` | Reutilizar el patrón para `ESTADO_PORTAFOLIO` |
| `parseStatus` acepta código o etiqueta recorriendo `values()` | `apps/backend/.../portfolio/application/PortfolioApplicationSupport.java` | Restringir a solo código; eliminar la dependencia por denominación |
| Dashboard usa etiquetas como claves e inventario desde `values()` | `DashboardSummaryService.java`, `DashboardPortfolioService.java`, `dashboard/api/DashboardDtos.java` | Reestructurar identidad por código con referencias estructuradas |
| Bandeja documental retorna etiqueta | `apps/backend/.../documents/application/DocumentInboxService.java` | Cambiar a referencia estructurada |
| Auditoría persiste etiquetas en `detailJson` opaco | `PortfolioApplicationSupport.java`, `audit/persistence/AuditEventEntity.java`, `AuditQueryService.java` | Eventos nuevos agregan códigos estables; lectura enriquecida con metadata vigente; legados sin reescritura |
| Reset validado en 20 tablas, 4 catálogos, 17 ítems; guardias fail-closed de perfiles | `apps/backend/.../config/reset/TestResetCoordinator.java`, `TestResetSchemaFilterProvider.java`, `TestResetEnvironmentGuard.java` | Incorporar tabla 21, órdenes y postvalidación de los once estados |
| Seeds actuales sin estados, con MERGE por claves naturales | `apps/backend/src/main/resources/db/test/catalog-data.sql`, `database/dml/seed/catalog-data.sql` | Añadir once MERGE coordinados e idempotentes en ambos artefactos |
| `dev`/`prod` con `ddl-auto=validate` y sin seed | `application-dev.yml`, `application-prod.yml`, `ProfileConfigurationTest.java` | Mantener intacto; la validación fallará hasta la provisión institucional (readiness incompleto) |
| Frontend con identidad por denominación e inventarios locales | `apps/frontend/src/app/core/piip.models.ts`, `piip.catalogs.ts`, `piip-http.repository.ts` (mapa `portfolioStatusCode`) | Redefinir por código; eliminar inventarios y fallbacks funcionales; matrices por código |
| Store de catálogos con fases `idle/loading/ready/error` | `apps/frontend/src/app/core/piip-catalogs.store.ts` | Acciones de estado exigen `ready`; distinguir vacío, error, carga e histórico inactivo |
| Cliente generado sin estados en el bundle y `status` textual | `apps/frontend/src/app/api/generated/**`, `ng-openapi-gen.json` | Regenerar desde el contrato backend publicado; sin edición manual |
| Specs históricas 009 (matrices), 011 (patrón de catálogos), 015 (perfiles/DML), 016 (baseline 20 tablas) | `specs/009-*`, `specs/011-*`, `specs/015-*`, `specs/016-*` | Grounding ratificado; no se reabren |
| Documentación con duplicación de estados declarada y conteos desfasados | `docs/architecture/piip-fields.md`, `docs/funcional/guia-funcional-piip.md`, `docs/development/test-catalog-reset.md` | Actualizar en la misma entrega de implementación (FR-030) |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | Sí | `apps/frontend/src/app/core/piip.models.ts`, `piip.catalogs.ts`, `piip-http.repository.ts`, `piip-catalogs.store.ts`, `piip-mock.repository.ts`, `pages/**` (formularios, filtros, listados, detalles, dashboard, documentos, auditoría, diálogos), `api/generated/**` (regenerado) | Consumidor del contrato; depende de la publicación backend y de `npm run api:generate` |
| Backend | Sí | `portfolio/domain`, `portfolio/persistence` (entidad y repositorio nuevos), `portfolio/application`, `portfolio/api`, `catalogs/application`+`catalogs/api`, `dashboard/**`, `documents/application`, `audit/**`, `config/reset/**`, `shared/application/error/ProblemCode.java`, `db/test/catalog-data.sql` | Propietario canónico del esquema, las reglas y el contrato |
| Database | Sí | `database/dml/seed/catalog-data.sql` (espejo coordinado), `database/generated/piip-oracle.sql` (DDL derivado regenerado) | Derivada de JPA para estructura; espejo DML sincronizado con el seed backend |
| Contrato HTTP | Sí | `GET /catalogs`, respuestas de portafolio (listado/detalle/mutaciones), `GET /dashboard`, `GET /dashboard/portfolio`, `GET /documents`, `GET /audit/events`, requests de transición, filtros `status` | Propietario: backend; consumidor: cliente Angular generado |
| Documentación | Sí | `docs/architecture/piip-fields.md`, `docs/architecture/data-model-final.md`, `docs/funcional/guia-funcional-piip.md`, `docs/development/test-catalog-reset.md`, `docs/deployment/institutional-development.md` | Fuente que debe actualizarse en la misma entrega (FR-030) |

## Contexto técnico

**Lenguajes/versiones**: Java 21, Spring Boot 4.1.0, Hibernate JPA sobre Oracle (JDBC 23.26); Angular 22, TypeScript 6.0.2, RxJS 7.8, Keycloak 26.2.4; springdoc-openapi 3.0.1; ng-openapi-gen 1.0.5; Vitest 4.0.8; Playwright 1.63.0; JUnit/Spring Test/ArchUnit/Testcontainers/H2.

**Dependencias principales**: todas existentes; la feature no agrega dependencias nuevas.

**Persistencia**: Hibernate JPA como fuente canónica del esquema. Nueva entidad `PortfolioStatusCatalogEntity` (tabla `ESTADO_PORTAFOLIO`) con PK natural por código; `REGISTRO_PORTAFOLIO.ESTADO` conservado y asociado read-only. El DDL revisable se regenera a `database/generated/piip-oracle.sql` desde el artefacto del build; sin DDL manual.

**Validación propuesta**: backend `gradlew.bat test`, `gradlew.bat check`, `gradlew.bat integrationTest` (Windows) o `./gradlew` equivalentes; OpenAPI mediante `OpenApiGenerationTest` (produce `apps/backend/target/piip-openapi.json`); frontend `npm run api:generate`, `npm test -- --watch=false`, `npm run build`. **Todas requieren autorización explícita del usuario en el turno; este plan no las ejecuta.**

**Plataforma objetivo**: monolito modular Spring Boot + SPA Angular; Oracle institucional en `dev`/`prod`; entornos descartables de prueba para el reset.

**Restricciones**: sin UI ni endpoints de administración del catálogo; sin roles o permisos nuevos; matrices y códigos intactos y separados del catálogo; código como única identidad funcional compartida; sin DDL, PL/SQL ni identificadores numéricos en seeds; activación exacta y ordenada `test,test-reset` con guardias fail-closed; `dev`/`prod` mantienen `validate` y no cargan seed; auditoría append-only en operación normal; no exponer entidades JPA en contratos HTTP; especificación y documentación en español.

**Escala/alcance**: 11 estados; 1 tabla nueva (reset pasa de 20 a 21 tablas); 2 artefactos seed coordinados; 4 `ProblemCode` nuevos; ~10 endpoints/DTO afectados; 6 documentos a actualizar; cero cambios a códigos, significados o matrices.

## Verificación de la constitución

*GATE: debe aprobarse antes del diseño y volver a revisarse al finalizarlo.*

**Evaluación inicial (antes del diseño)**

| Principio | Resultado | Evidencia del diseño |
|-----------|-----------|----------------------|
| I. Fuente funcional | Aprobado | Los 23 campos y seis catálogos no cambian; el estado del portafolio pasa de duplicado (enums backend + `PIIP_CATALOGS` frontend) a catálogo persistente único; el inventario inicial es idéntico al enum vigente, sin estados, significados ni obligatoriedades nuevos. |
| II. Estados y transiciones | Aprobado | Las matrices exactas de la feature 009 se conservan en el dominio; la metadata del catálogo nunca autoriza transiciones; `NOT_APPLICABLE` queda excluido por aplicabilidad `NONE` y por no participar en matrices. |
| III. Organización y seguridad | Aprobado | Sin cambios de organización, roles ni permisos; `GET /catalogs` conserva la autenticación Keycloak y la autorización funcional Oracle vigentes. |
| IV. Persistencia | Aprobado con excepción acotada | Tabla nueva derivada de entidad JPA (fuente canónica, sin SQL estructural). El DML de datos iniciales de estados se integra al artefacto externo existente: versionado, DML-only, idempotente, activación exacta y ordenada `test,test-reset`, guardias fail-closed, deshabilitado por defecto y prohibido en operación normal y producción. |
| V. Trazabilidad y calidad | Aprobado | Auditoría append-only: los eventos nuevos solo agregan códigos estables al `detailJson` y nunca se reescriben eventos existentes; se proponen pruebas automatizadas por área (ver [quickstart.md](./quickstart.md)). |

**Excepción del principio IV (documentada)**: perfil destructivo exclusivo de desarrollo/pruebas; activación exacta y ordenada `test,test-reset` validada por `TestResetEnvironmentGuard`/`TestResetStartupGuard` antes de JPA; DML externo versionado en dos artefactos coordinados, sin DDL ni PL/SQL, idempotente por MERGE insert-only sobre claves naturales; postvalidación fail-closed del inventario exacto de once estados; prohibido en `dev`, `prod` y operación normal.

**Reevaluación final (tras el diseño)**: aprobado. El diseño agrega una tabla derivada de JPA con FK por código natural, mantiene las matrices en el dominio, no reescribe auditoría ni históricos, y acota el DML de prueba a la excepción vigente. Sin contradicciones ni `NEEDS CLARIFICATION`.

## Dependencias y secuencia

- **Propietario canónico**: backend (esquema JPA, validaciones, read models, contrato HTTP).
- **Consumidores**: cliente Angular generado y consumidores frontend; espejo DML `database/dml/seed/catalog-data.sql`; documentación.
- **Orden obligatorio**: modelo JPA y validaciones de aplicación → read models y DTO/contrato backend → seed y reset (tabla 21, MERGE, postvalidación) → publicación OpenAPI → sincronización del espejo DML → regeneración del cliente Angular → adaptación de consumidores frontend → actualización documental coordinada.
- **Paralelización permitida**: la documentación puede avanzar en paralelo una vez cerrado el contrato; los árboles `apps/backend/**` y `apps/frontend/**` se delegan a sus especialistas; no paralelizar frontend contra un contrato no publicado.

## Estructura del proyecto

### Documentación de la feature

```text
specs/018-centralizar-estados-portafolio/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── portfolio-statuses-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                 # Generado con /speckit.tasks (73 tareas)
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/
├── portfolio/
│   ├── domain/PortfolioStatus.java                  # constantes y matrices intactas; label() fuera de uso visible
│   ├── persistence/PortfolioRecordEntity.java       # asociación read-only a ESTADO_PORTAFOLIO
│   ├── persistence/PortfolioStatusCatalogEntity.java # nueva
│   ├── persistence/PortfolioStatusRepository.java    # nueva
│   ├── application/                                  # validaciones por causa, read models con referencia
│   └── api/                                           # referencia de estado, targetStatus String
├── catalogs/application/CatalogQueryService.java     # bundle con portfolioStatuses
├── catalogs/api/CatalogDtos.java, CatalogController.java
├── dashboard/                                         # identidad por código, referencias y conteos estructurados
├── documents/application/DocumentInboxService.java   # referencia de estado
├── audit/                                             # códigos en eventos nuevos, lectura enriquecida
├── config/reset/                                      # 21 tablas, órdenes, postvalidación de estados
└── shared/application/error/ProblemCode.java          # cuatro causas 422
apps/backend/src/main/resources/db/test/catalog-data.sql
apps/backend/src/test/java/...                         # pruebas adaptadas y nuevas
database/dml/seed/catalog-data.sql                     # espejo coordinado
database/generated/piip-oracle.sql                     # DDL derivado regenerado
apps/frontend/src/app/
├── api/generated/**                                   # regenerado desde piip-openapi.json
├── core/piip.models.ts, piip.catalogs.ts, piip-http.repository.ts,
│       piip-catalogs.store.ts, piip-mock.repository.ts
└── pages/**                                           # formularios, filtros, listas, detalles,
                                                       # dashboard, documentos, auditoría, diálogos
docs/architecture/piip-fields.md
docs/architecture/data-model-final.md
docs/funcional/guia-funcional-piip.md
docs/development/test-catalog-reset.md
docs/deployment/institutional-development.md
```

**Decisión de estructura**: el catálogo de estados vive en el módulo `portfolio` porque su dominio y sus consumidores son de portafolio, siguiendo el precedente del catálogo documental especializado; la consulta central solo lo agrega al bundle. Las matrices permanecen en el dominio. Los controladores siguen delgados y las reglas y transacciones en servicios de aplicación.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: interfaz o endpoints de administración del catálogo; escritura administrativa de denominación/orden/actividad/aplicabilidad; roles o permisos nuevos; cambios a los once códigos, sus significados o las matrices; DML directo o automático en producción; provisión, migración o reconciliación de datos en esquemas institucionales no descartables (incluida la reconciliación de auditoría legada); reapertura de features históricas; declaración de preparación productiva completa.
- **Specs `001`-`005` consultadas**: Ninguna (antecedentes históricos, no backlog).
- **Dependencias históricas aprobadas**: feature 009 (matrices ratificadas por el principio II de la Constitución), feature 011 (patrón de consulta central y catálogos persistentes con históricos inactivos legibles), feature 015 (activación exacta `test,test-reset` y DML-only), feature 016 (baseline vigente de 20 tablas).
- **NEEDS CLARIFICATION**: Ninguna.

## Seguimiento de complejidad

> Completar solo si una decisión contradice la constitución y ha sido aprobada y justificada.

| Contradicción | Necesidad | Alternativa más simple descartada porque |
|---------------|-----------|------------------------------------------|
| Ninguna | Ninguna | Ninguna decisión del diseño contradice la constitución |
