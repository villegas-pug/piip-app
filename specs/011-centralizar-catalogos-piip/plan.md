# Plan de implementación: Centralizar catálogos PIIP

**Rama**: `main` | **Fecha**: 2026-08-20 | **Spec**: [spec.md](./spec.md)

**Entrada**: especificación en `/specs/011-centralizar-catalogos-piip/spec.md`

**Nota**: este artefacto define diseño y secuencia. No autoriza implementación, reinicio de base de datos, generación OpenAPI, pruebas, compilación, servidores, contenedores ni acciones Git.

## Resumen

Centralizar las siete fuentes de selección de PIIP con el backend como fuente de verdad. Tipo de solución, Fuente u origen, Objetivo PEI y Actividad POI se persistirán en `CATALOGO`/`CATALOGO_ITEM`; Tipo documental usará `TIPO_DOCUMENTO`; Unidad Orgánica conservará el modelo organizacional vigente y Tipo de registro seguirá siendo técnico y no persistente. Las escrituras usarán IDs y las lecturas incorporarán referencias resolubles, incluso históricas inactivas.

El backend es propietario del modelo y el contrato. Primero se evolucionarán JPA, servicios y DTO; luego se publicará OpenAPI y se regenerará el cliente Angular; finalmente se adaptarán consumidores sin crear filtros nuevos. Para pruebas se diseñará un perfil destructivo manual, fail-closed y prohibido en producción. Ese perfil recreará 13 tablas desde `Metadata` JPA, descartará auditoría y notificaciones previas y ejecutará el seed DML externo autorizado por la constitución 1.2.0. El perfil normal conservará `ddl-auto=validate`.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| `GET /catalogs` devuelve `Map<String,List<String>>` basado en enums. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/CatalogController.java` | Evolucionar a un bundle tipado que delega en servicio; no exponer JPA. |
| `REGISTRO_PORTAFOLIO` almacena solución, fuente, PEI y POI como enum/texto. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` | Sustituir columnas legacy por cuatro FK a `CATALOGO_ITEM`. |
| `DOCUMENTO` usa el enum `DocumentType` y unicidad por texto. | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentEntity.java` | Introducir `TIPO_DOCUMENTO` y conservar el ciclo documental mediante FK. |
| Unidad Orgánica ya posee identidad, pertenencia, jerarquía y activo; se ordena por nombre. | `apps/backend/src/main/java/pe/gob/midagri/piip/organization/**` | Reutilizar el endpoint vigente sin `displayOrder`, rutas ni filtros nuevos. |
| Angular mantiene `PIIP_CATALOGS`, `RESPONSIBLE_UNITS` y conversiones etiqueta→código. | `apps/frontend/src/app/core/piip.catalogs.ts`, `piip-http.repository.ts` | Sustituir solo las siete fuentes centralizadas y conservar conceptos fuera de alcance. |
| El dashboard hardcodea `Iniciativa` y `Proyecto`. | `apps/frontend/src/app/pages/dashboard/dashboard.component.html` | Consumir `recordTypes`; mantener `Todos` como opción local. |
| La bandeja documental filtra por búsqueda, Tipo de registro, estado y Unidad Orgánica. | `apps/frontend/src/app/pages/documents-inbox/documents-inbox.component.ts` | Conservar exactamente esos filtros y reemplazar únicamente sus fuentes locales. |
| El perfil normal usa `validate`; `dev` recrea globalmente. | `apps/backend/src/main/resources/application.yml`, `application-dev.yml` | Aislar destrucción en `test-reset` y eliminar la recreación implícita del flujo normal de desarrollo. |
| El DDL Oracle revisable se genera desde JPA y la prueba espera 16 tablas. | `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/OracleSchemaGenerationTest.java` | Actualizar la prueba a 19 tablas y verificar nuevas FK y ausencia de columnas legacy. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | Sí | `apps/frontend/src/app/core/**`, formularios, dashboard, listados, bandeja, documentos, detalles y auditoría | Consumidor posterior del contrato generado. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/catalogs/**`, `portfolio/**`, `documents/**`, `organization/**`, `config/reset/**`, `identity/**` | Propietario canónico del modelo, validaciones y contrato. |
| Database | Sí | `database/generated/piip-oracle.sql`, `apps/backend/src/main/resources/db/test/catalog-data.sql` | Estructura derivada de JPA; archivo SQL limitado a DML inicial. |
| Contrato HTTP | Sí | DTO/controladores backend, `apps/backend/target/piip-openapi.json`, cliente Angular generado | Backend primero; cliente generado después de autorización. |
| Documentación | Sí | `docs/funcional/guia-funcional-piip.md`, `docs/development/test-catalog-reset.md` | Actualización funcional y guía técnica durante implementación. |

## Contexto técnico

**Lenguajes/versiones**: Java 21, Spring Boot 4.1.0, Hibernate ORM 7.4.1.Final, Angular 22, TypeScript y DML Oracle.

**Dependencias principales**: Spring MVC, Spring Data JPA, Hibernate schema tooling, Bean Validation, Oracle JDBC, cliente Angular generado con `ng-openapi-gen`, signals y componentes standalone existentes.

**Persistencia**: Hibernate JPA sobre Oracle. Entidades y asociaciones son la única definición estructural permanente. La única excepción SQL es el seed DML externo, versionado, idempotente, sin DDL y ejecutable exclusivamente por `test-reset`.

**Validación propuesta**: pruebas unitarias y de arquitectura, persistencia JPA, controladores/contrato, regresión documental y de portafolio, componentes Angular, generación OpenAPI/cliente, DDL Oracle e integración del reset. Toda ejecución conserva las autorizaciones adicionales definidas por `AGENTS.md`.

**Plataforma objetivo**: aplicación web PIIP autenticada por Keycloak, autorizada en Oracle y perfil destructivo exclusivo de un datasource de pruebas allowlisted.

**Restricciones**: sin nuevas transiciones, roles, permisos, pantallas administrativas ni filtros; activos para nuevas selecciones, inactivos solo para lectura histórica; PEI/POI independientes; sin IDs hardcodeados; sin campo o marca visible de oficialidad; reset fail-closed, idempotente y prohibido en producción.

**Escala/alcance**: cuatro catálogos genéricos, seis tipos documentales, un catálogo técnico de dos tipos, Unidades Orgánicas por UE, 19 tablas JPA después del cambio, 13 tablas reiniciadas y 6 tablas protegidas.

## Verificación de la constitución

*GATE: debe aprobarse antes del diseño y volver a revisarse al finalizarlo.*

### Gate inicial

- **I. Fuente funcional**: aprobado. Se centralizan únicamente las siete fuentes definidas y se mantienen los significados vigentes.
- **II. Estados y transiciones**: aprobado. No se convierten estados ni se agregan transiciones.
- **III. Organización y seguridad**: aprobado. Unidad Orgánica continúa acotada por UE y autorización; identidad y ámbitos quedan fuera de la allowlist destructiva.
- **IV. Persistencia**: aprobado con la excepción expresa de la versión 1.2.0. JPA genera toda estructura y solo `test-reset` ejecuta DML inicial sin DDL después de validar perfil, confirmación, conexión y esquema.
- **V. Trazabilidad y calidad**: aprobado con la excepción expresa de la versión 1.2.0. La auditoría es append-only en operación normal; el reset autorizado recrea vacías sus tablas y propone pruebas automatizadas.
- **Grounding y contrato**: aprobado. Graphify se usó como índice y los hallazgos se validaron en código y artefactos canónicos; backend precede a OpenAPI y Angular.

### Gate posterior al diseño

Aprobado. La allowlist enumera exactamente 13 tablas, protege identidad/organización, incluye auditoría y notificaciones, y falla antes de escribir ante cualquier diferencia. El contrato no incorpora filtros no existentes, no exige `displayOrder` a Unidad Orgánica y no agrega una marca de oficialidad. No quedan contradicciones bloqueantes para el ambiente de pruebas.

## Dependencias y secuencia

- **Propietario canónico**: backend para JPA, reglas de referencia, autorización, reset y contrato HTTP.
- **Consumidores**: OpenAPI generado, cliente Angular, estado de catálogos, adaptadores, formularios, dashboard, listados, bandeja, documentos, auditoría y detalles.
- **Orden obligatorio**: modelo JPA y seed → servicios/DTO/controladores → pruebas backend propuestas → OpenAPI autorizado → cliente Angular autorizado → núcleo Angular → consumidores → documentación → validaciones autorizadas.
- **Paralelización permitida**: después de congelar el contrato pueden prepararse pruebas y documentación en archivos independientes. No paralelizar modelo backend, OpenAPI, cliente generado y consumidores del mismo contrato.

## Estructura del proyecto

### Documentación de la feature

```text
specs/011-centralizar-catalogos-piip/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── catalogs.openapi.yaml
└── tasks.md                    # Debe regenerarse mediante /speckit-tasks
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/
├── catalogs/
│   ├── api/
│   ├── application/
│   └── persistence/
├── portfolio/{api,application,persistence}/
├── documents/{api,application,persistence}/
├── organization/{api,persistence}/
├── config/reset/
└── identity/application/IdentityBootstrap.java

apps/backend/src/main/resources/
├── application-test-reset.yml
└── db/test/catalog-data.sql

apps/backend/src/test/java/pe/gob/midagri/piip/
├── catalogs/
├── config/reset/
├── portfolio/
├── documents/
├── dashboard/
├── work/
├── contract/
├── architecture/
└── persistence/

apps/frontend/src/app/
├── api/generated/
├── core/
├── pages/{initiative-form,preexisting-project-form,derived-project-form}/
├── pages/{initiatives,projects,dashboard,documents-inbox}/
├── pages/{documents,initiative-detail,project-detail,audit}/
└── shared/

database/generated/piip-oracle.sql
docs/funcional/guia-funcional-piip.md
docs/development/test-catalog-reset.md
```

**Decisión de estructura**: `catalogs` posee las entidades, repositorios, resolución y consulta de catálogos. `portfolio` y `documents` consumen identidades mediante servicios de aplicación; los controladores solo enlazan HTTP. `config/reset` encapsula guardias y SPI Hibernate. Angular mantiene modelos/estado comunes detrás de `PiipRepository`; los componentes consumen el contrato y no reconstruyen identidades.

## Diseño por responsabilidad

### Modelo JPA y reglas de referencia

- Crear `CatalogEntity`, `CatalogItemEntity` y `DocumentTypeEntity` conforme a [data-model.md](./data-model.md).
- Reemplazar las cuatro columnas legacy de `REGISTRO_PORTAFOLIO` por FK a `CATALOGO_ITEM`; PEI/POI continúan opcionales e independientes.
- Reemplazar el tipo textual de `DOCUMENTO` por `ID_TIPO_DOCUMENTO` y conservar unicidad `(ID_REGISTRO, ID_TIPO_DOCUMENTO)`.
- Mantener `UNIDAD_ORGANICA` sin `ORDEN_PRESENTACION`; ordenar opciones por nombre y devolver su identidad, pertenencia y activo.
- Resolver existencia, catálogo esperado y activo dentro de servicios transaccionales. Las lecturas embeben referencias inactivas; las nuevas escrituras las rechazan con 422.
- Un proyecto preexistente resuelve `NOT_APPLICABLE` por código estable en backend; el frontend no conoce su ID ni mantiene un literal funcional.
- No agregar `official`, `synthetic`, `testData` ni equivalente. La condición sintética de PEI/POI/UO vive en comentarios del seed y documentación técnica.

### Servicios y contrato HTTP

- Evolucionar `GET /catalogs` a un `CatalogBundleResponse` atómico con `recordTypes`, `solutionTypes`, `sources`, `peiObjectives`, `poiActivities` y `documentTypes` activos y ordenados.
- Mantener `GET /organizational-units?executingUnitId` separado, autorizado y ordenado por nombre; su respuesta no contiene `displayOrder`.
- Adaptar requests de iniciativa, proyecto derivado y preexistente para IDs persistentes; adaptar lecturas para referencias completas `{id, code, name, displayOrder, active}`.
- Mantener en `PortfolioController` exactamente `q`, `status`, `executingUnitId`, `page` y `size`; no agregar filtros por solución, fuente, PEI, POI o Unidad Orgánica.
- Mantener la bandeja documental sin parámetros HTTP nuevos. Sus filtros locales continúan siendo búsqueda, Tipo de registro, estado y Unidad Orgánica.
- Cambiar las rutas de mutación documental a `{documentTypeId}` y conservar publicación/descarga por `versionId`.
- Generar el OpenAPI real en `apps/backend/target/piip-openapi.json`; no editar manualmente el cliente Angular.

### Estado y consumidores Angular

- Definir opciones persistentes, opciones técnicas, referencias históricas y estado de recurso `idle/loading/ready/error` en `piip.models.ts` y `PiipRepository`.
- Cargar el bundle una vez después de identidad. Cargar Unidades Orgánicas por UE con request-id y descartar respuestas tardías.
- Distinguir carga, vacío, error y reintento; un error nunca habilita listas locales. Conservar una selección solo por el mismo ID activo.
- Eliminar conversiones etiqueta→enum y resolución de Unidad Orgánica por sigla/nombre en `PiipHttpRepository`.
- Adaptar los tres formularios a IDs. El proyecto derivado muestra una referencia heredada inactiva como contexto, mantiene inválido el control y exige reemplazo activo.
- Adaptar dashboard para consumir `recordTypes`, con `Todos` local. Listados y bandeja conservan únicamente sus filtros actuales y comparan por identidad donde corresponda.
- Adaptar documentos a `documentTypeId`; conservar códigos solo para agrupación visual. Eliminar `DOCUMENT_LABELS` y resolver auditoría desde el snapshot/backend.
- Mostrar referencias históricas inactivas en detalles y mocks sin reconstruirlas desde el bundle active-only.

### Perfil destructivo `test-reset`

1. Exigir simultáneamente `test,test-reset`, `enabled=true`, confirmación explícita, huella JDBC y usuario-esquema allowlisted; rechazar `prod` y cualquier diferencia antes de habilitar escritura.
2. Capturar el mismo `Metadata` Hibernate usado por JPA y comparar una allowlist exacta de 13 tablas y una denylist de 6 protegidas.
3. Validar que el seed contiene DML idempotente sin DDL, PL/SQL ni IDs numéricos hardcodeados.
4. Ejecutar drop tabla por tabla en orden hijo-a-padre mediante `SchemaDropper` y filtro exacto.
5. Ejecutar create tabla por tabla en orden padre-a-hijo mediante `SchemaCreator` y el mismo `Metadata`.
6. Ejecutar `catalog-data.sql`, comprobar conteos/unicidad y verificar auditoría/notificaciones vacías y datos protegidos intactos.
7. Ante el primer error, registrar etapa y tabla de forma segura, omitir etapas posteriores y no comunicar éxito.

`ORA-00942` solo es recuperable durante `DROP` de la tabla allowlisted actualmente procesada, cuando la causa raíz tiene código Oracle `942` y el preflight ya pasó. Es fatal en preflight, create, seed, postvalidación, tablas protegidas o cualquier otra operación. No se tolera ningún otro error Oracle.

### Matriz de tablas del reset

| Drop | Tabla | Dependencia / tratamiento | Create |
|------|-------|---------------------------|--------|
| 1 | `DOCUMENTO_CONTENIDO` | Hija de versión; recrear vacía. | 9 |
| 2 | `DOCUMENTO_VERSION` | Hija de documento; recrear vacía. | 8 |
| 3 | `DOCUMENTO` | Hija de registro y tipo documental; recrear posiciones desde catálogo. | 7 |
| 4 | `REGISTRO_UNIDAD_RESPONSABLE` | Hija de registro; conserva `UNIDAD_ORGANICA`. | 6 |
| 5 | `TAREA_TRABAJO` | Referencia registro y usuario; recrear vacía. | 10 |
| 6 | `NOTIFICACION` | Referencia opcional a registro y obligatoria a usuario; recrear vacía. | 11 |
| 7 | `EVENTO_AUDITORIA` | Referencia usuario; descartar todo historial de prueba. | 12 |
| 8 | `AUDITORIA_ACCESO` | Referencia usuario; descartar todo historial de prueba. | 13 |
| 9 | `REGISTRO_PORTAFOLIO` | Autorreferencia, UE y cuatro nuevas FK de catálogo. | 5 |
| 10 | `CONTADOR_CODIGO` | Numeración del portafolio de prueba. | 4 |
| 11 | `CATALOGO_ITEM` | Hija de catálogo y padre de registros. | 2 |
| 12 | `TIPO_DOCUMENTO` | Padre de documento. | 3 |
| 13 | `CATALOGO` | Cabecera de catálogo. | 1 |

Tablas protegidas y excluidas del filtro: `INSTITUCION`, `ROL`, `UNIDAD_EJECUTORA`, `UNIDAD_ORGANICA`, `USUARIO` y `USUARIO_ROL_AMBITO`. `IdentityBootstrap` queda excluido de `test-reset` y el seed no toca identidad, roles ni ámbitos.

## Estrategia de verificación propuesta

1. **JPA/catálogos**: entidades, UK, FK, orden, activos e históricos inactivos.
2. **Servicios**: inexistente, catálogo equivocado, inactivo, UO ajena y proyecto preexistente `NOT_APPLICABLE`.
3. **Contrato**: bundle estructurado, requests por IDs, rutas `documentTypeId`, filtros HTTP sin ampliación y referencias históricas completas.
4. **Regresión documental**: seis posiciones, versiones, contenido, publicación, `No aplica`, unicidad y snapshot de auditoría.
5. **Reset unitario**: guardias, allowlist 13/denylist 6, orden exacto, seed sin DDL, fail-fast y `ORA-00942` limitado al drop actual.
6. **OracleSchemaGenerationTest**: actualizar `16 → 19`, exigir las tres tablas nuevas, cinco nuevas FK, ausencia de columnas/checks legacy y ausencia de DML en el DDL.
7. **Fixtures backend**: adaptar todos los consumidores de fábricas de `PortfolioRecordEntity`, preferentemente mediante un builder compartido; incluir portafolio, documentos, dashboard, work y autorización.
8. **Angular core**: bundle éxito/vacío/error/reintento, requests por IDs, request-id de UE, conservación por identidad y mocks estructurados.
9. **Angular UI**: tres formularios, dashboard, listados, bandeja, documentos, auditoría y detalles; agregar prueba del formulario derivado inactivo.
10. **Integración autorizada**: OpenAPI/cliente, DDL revisable, datasource Oracle allowlisted, dos ejecuciones y recuperación tras drop parcial.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: administración de catálogos, filtros nuevos, estados/transiciones, roles/permisos, rediseño organizacional, migración destructiva productiva y marca visible de oficialidad.
- **Specs `001`-`005` consultadas**: Ninguna; son antecedentes históricos y no backlog.
- **Dependencias históricas aprobadas**: matrices de transición de feature 009 ratificadas desde la constitución 1.1.0 y conservadas en la 1.2.0.
- **Pendientes productivos diferidos**: valores oficiales PEI/POI, autoridad institucional y migración no destructiva productiva. No bloquean el diseño exclusivo de pruebas.

## Seguimiento de complejidad

No quedan contradicciones constitucionales. El DML inicial y el descarte de auditoría son excepciones explícitas de la constitución 1.2.0 y permanecen aisladas, fail-closed y prohibidas en producción.
