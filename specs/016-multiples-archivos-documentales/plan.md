# Plan de implementación: Múltiples archivos independientes por tipo documental

**Rama**: `016-multiples-archivos-documentales` | **Fecha**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

**Entrada**: especificación en `specs/016-multiples-archivos-documentales/spec.md`

## Resumen

La especificación exige coexistencia de varios archivos independientes por tipo documental en un expediente, cada uno con su propio historial de versiones, además de eliminación individual lógica, migración sin pérdida y compatibilidad con clientes existentes. El enfoque parte del modelo vigente: la tabla `DOCUMENTO` se consolida en su rol de **posición/presencia del tipo documental** (una fila por expediente y tipo, con su estado y su declaración "No aplica", unicidad intacta) y se introduce una nueva entidad **archivo documental** (`ARCHIVO_DOCUMENTO`) entre la posición y sus versiones: cada archivo agrupa su propio historial (`DOCUMENTO_VERSION`), su versión vigente, su marca de original y su inhabilitación lógica. La unicidad de versiones pasa de (posición, número) a (archivo, número); la unicidad de posición (registro, tipo) se conserva. La operación de carga actual por tipo mantiene su efecto (nueva versión del archivo original migrado), y se añaden operaciones explícitas de "agregar archivo", "nueva versión de un archivo específico" y "eliminar archivo" (inhabilitación lógica) con la misma autorización y auditoría vigentes. La migración de datos se ejecuta como operación de aplicación transaccional JPA, idempotente y verificable por conteos (sin SQL nativo), y el DDL derivado se regenera desde Hibernate conforme a la constitución. El frontend pasa a listar archivos individuales por tipo con las tres acciones distinguibles y a exponer el historial por archivo.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|------------------------------|
| Posición documental única por expediente y tipo con UK `UK_DOCUMENTO_REGISTRO_TIPO`, estado `PENDING/LOADED/NOT_APPLICABLE` y motivo "No aplica" | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentEntity.java` | Conservar como presencia del tipo: la UK, el estado y el motivo no cambian |
| Versiones por posición con UK `UK_DOC_VERSION (ID_DOCUMENTO, NUMERO_VERSION)`, metadatos de carga, publicación externa y bloqueo optimista | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentVersionEntity.java` | Adaptar: la pertenencia del historial pasa al archivo; la unicidad se reubica sobre el archivo |
| Contenido binario diferido en BLOB 1:1 por versión | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentContentEntity.java` | Mantener sin cambios (constitución IV: BLOB separado de metadatos) |
| Catálogo de tipos documentales con código único, orden de presentación y activo | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentTypeEntity.java` | Mantener sin cambios |
| Consultas por posición (`findByRecordIdAndTypeId`, listado ordenado por tipo) | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/persistence/DocumentRepository.java` | Reutilizar para resolver la posición y navegar a sus archivos |
| Carga de un archivo por operación con validaciones MIME (pdf/docx/xlsx), tamaño máximo configurable, archivo no vacío y checksum | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentService.java` | Reutilizar el mismo núcleo de validación para "agregar archivo" y "nueva versión" |
| Publicación externa y descarga por versión con reglas de ámbito (externos solo versiones publicadas) | ídem | Mantener sin cambios (FR-011, FR-018) |
| "No aplica" por posición, unidireccional, con auditoría `DOCUMENTO_NO_APLICA` | ídem, y `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentController.java` | Mantener a nivel posición (= tipo del expediente), conforme a FR-019 y FR-022 |
| Inicialización de posiciones `PENDING` por tipo activo al crear expedientes | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/PortfolioDocumentService.java` (invocado por `InitiativeApplicationService` y `ProjectApplicationService`) | Mantener: sigue materializando la presentación del tipo; nunca crea archivos |
| Resumen del expediente con conteos por estado de posición | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/application/DocumentInboxService.java` | Mantener conteos por tipo (FR-026); el estado de posición se mantiene correcto mediante las reglas de recálculo |
| Endpoints `/portfolio-records/{recordCode}/documents...` (listado, carga por tipo, "No aplica", publicación, descarga) | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/api/DocumentController.java` y `DocumentDtos.java` | Extender aditivamente: las rutas actuales se conservan y se añaden las de archivos |
| Auditoría append-only de eventos documentales (`DOCUMENTO_CARGADO`, `DOCUMENTO_PUBLICADO`, `DOCUMENTO_RETIRADO`, `DOCUMENTO_NO_APLICA`) | `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditService.java` | Reutilizar para el evento nuevo de eliminación individual (FR-015) |
| DDL derivado versionado desde Hibernate | `database/generated/piip-oracle.sql` (mecanismo de la feature 015) | Regenerar tras el cambio de modelo; jamás edición manual |
| Prueba "dos cargas del mismo tipo → versiones 1 y 2" mediante la operación por tipo | `apps/backend/src/test/java/pe/gob/midagri/piip/documents/application/DocumentCatalogFlowTest.java` | Conservar como prueba de compatibilidad (FR-024); el comportamiento nuevo se prueba con las operaciones nuevas |
| UI documental con selector de tipo, input único de archivo y menú por posición | `apps/frontend/src/app/pages/documents/documents.component.ts` y `.html` | Adaptar a listado por archivo con acciones distinguibles (SC-006) |
| Mapeo `versions[0]` (solo la versión vigente) y modelos de presentación | `apps/frontend/src/app/core/piip-http.repository.ts` y `apps/frontend/src/app/core/piip.models.ts` | Extender para exponer archivos individuales y sus historiales (FR-009) |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | Sí | `apps/frontend/src/app/pages/documents/`, `apps/frontend/src/app/core/piip-http.repository.ts`, `apps/frontend/src/app/core/piip.models.ts`, `apps/frontend/src/app/core/piip-mock.repository.ts`, cliente OpenAPI regenerado en `apps/frontend/src/app/api/generated/` | Consumidor: sincroniza tras publicar el contrato backend |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/documents/` (persistence, application, api) y pruebas en `apps/backend/src/test/java/pe/gob/midagri/piip/documents/` | Propietario canónico del modelo y del contrato |
| Database | Sí | `database/generated/piip-oracle.sql` (DDL derivado regenerado desde Hibernate) | Derivada de JPA: regeneración autorizada, sin edición manual |
| Contrato HTTP | Sí | `DocumentController`/`DocumentDtos`: rutas nuevas de archivos, respuesta extendida del listado y `piip-openapi.json` regenerado | Propietario: backend; consumidores: frontend y clientes existentes |
| Documentación | Sí | `docs/architecture/piip-fields.md`, `docs/architecture/data-model-final.md`, `docs/architecture/data-model-comparison.md` | Actualización en la misma entrega (regla de AGENTS.md) |

## Contexto técnico

**Lenguajes/versiones**: Java 21 con Spring Boot 4.1 (monolito modular); Angular 22 con componentes standalone; Oracle con Hibernate JPA como fuente canónica del esquema; Keycloak autentica y Oracle autoriza (`USUARIO`, `ROL`, `USUARIO_ROL_AMBITO`).

**Dependencias principales**: módulo `documents` vigente (entidades, repositorios, `DocumentService`, `DocumentController`); `AuditService` para eventos append-only; `LocalAuthorizationService` para `requireUnit`/`requireReadableUnit`; `PiipProperties.Documents.maxSizeBytes` (default 10 MiB configurable); mecanismo de la feature 015 para el DDL derivado y el esquema de pruebas.

**Persistencia**: Hibernate JPA/Oracle. Cambios: nueva entidad `ARCHIVO_DOCUMENTO`; `DOCUMENTO_VERSION` añade la referencia al archivo y su unicidad pasa a (archivo, número de versión); `DOCUMENTO` retira `ULTIMA_VERSION` (la versión vigente pasa a ser por archivo) y conserva UK, estado y motivo. El DDL derivado se regenera desde Hibernate. La migración de datos se invoca como paso de despliegue explícito: arranque dedicado con activación por propiedad deshabilitada por defecto, que ejecuta la operación idempotente, registra los conteos por expediente y tipo y termina (research D4).

**Validación propuesta**: `gradlew.bat test` y `gradlew.bat check` (backend), `gradlew.bat integrationTest` (integración Oracle), `npm test -- --watch=false` y `npm run build` (frontend). Su ejecución es manual y requiere autorización explícita del usuario.

**Plataforma objetivo**: despliegue del monolito con Oracle; perfiles vigentes (`dev` por defecto, `validate` en dev/prod, H2 create-drop en pruebas).

**Restricciones**: constitución IV (sin SQL nativo, `JdbcTemplate`, procedimientos almacenados, Flyway o Liquibase para acceso funcional o estructura; la migración de datos es código de aplicación JPA); auditoría append-only (V); misma autorización que la carga para eliminar (III); MIME exactamente pdf/docx/xlsx y tamaño máximo configurado vigentes; sin cambios de roles, estados del portafolio ni transiciones; expedientes de iniciativa y proyecto separados; sin copia de documentos al proyecto derivado.

**Escala/alcance**: catálogo vigente de 6 tipos documentales; N archivos por tipo sin límite adicional; todo archivo con al menos una versión; volúmenes de v1 (sistema en adopción inicial).

## Verificación de la constitución

*GATE: debe aprobarse antes del diseño y volver a revisarse al finalizarlo.*

**Evaluación previa al diseño** (constitución 1.3.0):

- **I. Fuente funcional**: la feature no altera los 23 campos ni los seis catálogos; cambia la cardinalidad documental interna. La guía funcional de `docs/architecture/` fija hoy una posición por tipo y se actualiza en la misma entrega (declarado en Impacto). Cumple.
- **II. Estados y transiciones**: no toca los estados del ciclo de vida del portafolio ni sus transiciones ratificadas. El dominio documental `PENDING/LOADED/NOT_APPLICABLE` se conserva con el mismo significado, a nivel del tipo. Cumple.
- **III. Organización y seguridad**: la eliminación individual exige la misma autorización que la carga (`ADMINISTRADOR_PIIP` sobre la unidad ejecutora del expediente, con rechazo a administradores de otras unidades); la consulta externa solo accede a versiones publicadas de archivos no eliminados; un usuario autenticado sin asignación activa no obtiene permisos. Cumple.
- **IV. Persistencia**: todo cambio estructural se expresa en entidades JPA y el DDL derivado se regenera desde Hibernate (`database/generated/piip-oracle.sql`), sin SQL nativo ni herramientas de migración. La migración de datos existentes se implementa como operación de aplicación transaccional JPA, idempotente y verificable por conteos; no se usa la excepción de DML externo del perfil destructivo (reservada al seed/reset de pruebas de la feature 015, prohibida en producción). Los binarios permanecen como BLOB separado. Cumple.
- **V. Trazabilidad y calidad**: la eliminación es lógica y no elimina eventos; se añade un evento de auditoría append-only para la eliminación individual. Los cambios requieren pruebas automatizadas (previstas). Cumple.

**Resultado del gate previo**: aprobado, sin contradicciones constitucionales.

## Dependencias y secuencia

- **Propietario canónico**: backend (`apps/backend`) — define el modelo, el comportamiento y el contrato HTTP documental.
- **Consumidores**: frontend (`apps/frontend`) mediante el cliente OpenAPI regenerado; documentación de arquitectura (`docs/architecture/`).
- **Orden obligatorio**: modelo y servicios backend → contrato HTTP publicado (`piip-openapi.json`) → sincronización del cliente y UI frontend. La migración de datos se valida junto con el backend. La documentación se actualiza con el contrato definitivo.
- **Paralelización permitida**: ninguna entre backend y frontend sobre el contrato compartido (publicar primero el propietario canónico y después el consumidor); la documentación puede avanzar en paralelo sobre las partes ya definidas.

## Estructura del proyecto

### Documentación de la feature

```text
specs/016-multiples-archivos-documentales/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── documents-api.md
└── tasks.md          # se genera con /speckit.tasks
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/documents/
├── domain/            # DocumentState sin cambios
├── persistence/       # nueva entidad y repositorio de archivo; ajuste de DocumentVersionEntity y DocumentRepository
├── application/       # DocumentService (agregar archivo, nueva versión por archivo, eliminación lógica, migración)
└── api/               # DocumentController y DocumentDtos extendidos; MultipartDocumentUploadAdapter reutilizado
apps/backend/src/test/java/pe/gob/midagri/piip/documents/   # archivos independientes, eliminación, autorización, migración y compatibilidad
apps/frontend/src/app/
├── pages/documents/   # listado por archivo, acciones distinguibles, historial visible
├── core/              # piip-http.repository.ts, piip.models.ts, piip-mock.repository.ts
└── api/generated/     # cliente OpenAPI regenerado (no editar a mano)
database/generated/piip-oracle.sql      # DDL derivado regenerado
docs/architecture/                     # piip-fields.md, data-model-final.md, data-model-comparison.md
```

**Decisión de estructura**: el módulo `documents` absorbe el cambio en sus capas existentes (persistence/application/api); no se crean módulos nuevos. La separación posición → archivo → versión respeta el modelo vigente y añade una sola entidad. El mecanismo de migración vive en la capa de aplicación como operación administrada, invocada como paso de despliegue explícito mediante un arranque dedicado (activación deshabilitada por defecto; ejecuta, registra conteos y termina; research D4), no en controladores HTTP funcionales ni en el arranque normal del servicio.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: selección múltiple por operación, borrado físico o purga de contenidos, restauración de eliminados, cambios de roles, MIME, límites de tamaño, estados del portafolio o transiciones, copia de documentos al proyecto derivado, antimalware en línea y edición parcial de versiones (spec, Out of Scope). Tampoco se modifican las matrices de transiciones ratificadas ni el mecanismo de inicialización Oracle de la feature 015.
- **Specs `001`-`005` consultadas**: `specs/003-documents/spec.md` — su cardinalidad de una posición por tipo queda expresamente supersedea por esta feature; se conservan su modelo de versiones, contenido diferido, publicación externa, validaciones y la regla de no copiar documentos al proyecto derivado.
- **Dependencias históricas aprobadas**: feature 015 (`specs/015-inicializacion-oracle/`) — DDL derivado versionado y regenerable desde Hibernate, seed DML externo exclusivo de desarrollo/pruebas con guardias fail-closed; esta feature no altera ese mecanismo y no usa la excepción DML para migrar datos de producción.
- **NEEDS CLARIFICATION**: ninguno — las decisiones funcionales abiertas se resolvieron en las clarificaciones de la spec ("No aplica" por tipo; eliminación lógica oculta sin restauración; reanudación del tipo por carga; conteos de indicadores por tipo).

## Seguimiento de complejidad

> Sin contradicciones constitucionales aprobadas: ninguna entrada.

## Re-verificación constitucional posterior al diseño

Tras generar `research.md`, `data-model.md`, `contracts/documents-api.md` y `quickstart.md`:

- **IV. Persistencia**: el diseño materializa todo cambio en entidades JPA (nueva `ARCHIVO_DOCUMENTO`, unicidad de versiones sobre (archivo, número), retiro de `ULTIMA_VERSION` de la posición); el DDL derivado se regenera y la migración es una operación de aplicación JPA transaccional, idempotente y verificable por conteos; los BLOB permanecen separados. Cumple.
- **V. Trazabilidad y calidad**: el contrato añade el evento append-only `DOCUMENTO_ARCHIVO_ELIMINADO` y la eliminación lógica no destruye eventos ni contenidos; se prevén pruebas automatizadas por cada grupo de requisitos. Cumple.
- **III. Organización y seguridad**: el contrato mantiene `requireUnit(ADMINISTRADOR_PIIP, unidad ejecutora)` para eliminar y `requireReadableUnit` para consultar/descargar, con la consulta externa limitada a versiones publicadas de archivos no eliminados. Cumple.
- **I y II**: sin cambios respecto del gate previo (guía funcional a actualizar en la misma entrega; sin tocar estados ni transiciones del portafolio).

**Resultado del gate final**: aprobado. El diseño queda habilitado para `/speckit.tasks`.
