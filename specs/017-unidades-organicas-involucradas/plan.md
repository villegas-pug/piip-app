# Plan de implementación: Unidades Orgánicas Involucradas

**Rama**: `017-unidades-organicas-involucradas` | **Fecha**: 2026-09-07 | **Spec**: [spec.md](spec.md)

**Entrada**: especificación en `specs/017-unidades-organicas-involucradas/spec.md`

**Protocolo**: `docs/development/spec-kit-adoption.md`. Este plan no implementa producto, no ejecuta validaciones y no autoriza Oracle, builds, pruebas, generación de OpenAPI/DDL ni Git.

## Resumen

La feature reemplaza la cardinalidad exactamente-una del campo "Unidad Orgánica responsable" (feature 013, solo en cardinalidad) por una lista dinámica y ordenada de "Unidades Orgánicas Involucradas" (mínimo una, sin máximo funcional, sin duplicados, orden de incorporación, renumeración 1..N, confirmación atómica) en los tres tipos de alta, edición, revisión previa, detalle y auditoría. El esquema JPA vigente ya soporta N asociaciones ordenadas por registro (`REGISTRO_UNIDAD_RESPONSABLE` con unicidad por registro y orden), por lo que los cambios se concentran en: contratos de entrada/salida (relajar `@Size(max=1)` a mínimo uno sin máximo), el servicio de aplicación (validación de lista: existencia, actividad y sigla para nuevas incorporaciones, misma Unidad Ejecutora, unicidad del conjunto completo, orden continuo), la auditoría (lista ordenada en eventos de alta y sigla en cada elemento auditado), la UI Angular (lista dinámica con filas Nro/Descripción/Abreviatura, edición completa del conjunto, precarga del derivado, revisión/detalle ordenados) y la postvalidación del reset `test,test-reset` (sigla no vacía, asociación Unidad Orgánica–Unidad Ejecutora, mínimo dos activas por Unidad Ejecutora sintética), reutilizando los valores sintéticos versionados. La guía funcional y la matriz de campos se alinean en la misma entrega.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|---|---|---|
| `REGISTRO_UNIDAD_RESPONSABLE` soporta N filas ordenadas por registro con UK (registro, orden) | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/ResponsibleUnitEntity.java`, `ResponsibleUnitRepository.java` | Reutilizar el esquema sin cambios estructurales; la cardinalidad-1 la imponen solo DTOs y servicio. |
| `ResponsibleUnitService` impone exactamente una; `replace` ya resuelve/valida toda la lista antes de tocar persistencia y reinserta con orden continuo | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitService.java` | Sustituir `requireExactlyOne` por validación de lista (mín 1, sin duplicados); añadir sigla para nuevas incorporaciones; conservar atomicidad de `replace`. |
| DTOs de alta con `@NotEmpty @Size(max = 1)` y de edición con `@Size(min=1,max=1)` sobre `responsibleUnits` | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioDtos.java` | Relajar a mínimo uno sin máximo en los tres create y en los dos update (presencia JSON preservada). |
| `OrganizationalUnitEntity.SIGLA` nullable; catálogo de escritura solo activas de la UE ordenadas por nombre; lectura histórica resuelve inactivas | `organization/persistence/OrganizationalUnitEntity.java`, `OrganizationalUnitRepository.java`, `organization/api/OrganizationController.java` | Filtrar unidades sin sigla del catálogo de opciones (Q2=A) y validar sigla en el servicio para nuevas incorporaciones; la lectura histórica conserva inactivas como contexto. |
| Eventos de alta sin lista de unidades; auditoría de actualización con snapshot `{id, code, name, displayOrder}` y diff anterior/nuevo | `InitiativeApplicationService.java`, `ProjectApplicationService.java`, `PortfolioUpdateAuditDetail.java` | Extender el detalle de altas con la lista ordenada y añadir sigla a cada elemento auditado (altas y modificaciones). |
| Formularios de alta con select único; edición que fuerza `[ids[0]]` con histórico >1; revisión previa sin unidades; detalle con etiqueta | `apps/frontend/src/app/pages/initiative-form/`, `derived-project-form/`, `preexisting-project-form/`, `portfolio-record-edit/`, diálogos de revisión, `initiative-detail/` | Lista dinámica reutilizable (agregar al final, retirar salvo última fila, renumeración), edición completa del conjunto, revisión/detalle con la lista ordenada. |
| Repositorio/HTTP frontend envían `organizationalUnitId` singular y mapean etiquetas `acronym \|\| name` | `apps/frontend/src/app/core/piip.http.repository.ts`, `piip.repository.ts`, `piip.models.ts`, `piip-mock.repository.ts` | Adaptar contratos internos y mock a la lista ordenada; cliente OpenAPI regenerado tras publicar el contrato backend. |
| Seed sintético carga 4 UOs (2 por UE sintética) con código, nombre y sigla no vacíos, idempotente | `apps/backend/src/main/resources/db/test/catalog-data.sql` (sección 3) | Reutilizar valores sin reemplazo; la adecuación se centra en la postvalidación, no en el SQL. |
| Postvalidación del reset valida conteos y vaciado de tablas operativas, sin siglas ni asociación UO–UE | `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` | Extender postvalidación: sigla no vacía por UO sintética, asociación UO–UE correcta, ≥2 activas por UE sintética; fallo → inicialización incompleta. |
| Guía funcional y matriz de campos describen el campo como única referencia activa con posición técnica 1 | `docs/funcional/guia-funcional-piip.md` (secciones de campo, consulta, derivado y edición), `docs/architecture/piip-fields.md` | Alinear denominación "Unidades Orgánicas Involucradas" y regla de lista ordenada (FR-032). |
| Flujo de contrato del monorepo: OpenAPI backend primero, cliente Angular después | `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java`, `apps/frontend/ng-openapi-gen.json` | Mantener el orden: publicar contrato backend y luego sincronizar `api/generated/`. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|---|---|---|---|
| Frontend | Sí | `apps/frontend/src/app/core/piip.models.ts`, `piip.repository.ts`, `piip-http.repository.ts`, `piip-mock.repository.ts`, `api/generated/**`, `pages/initiative-form/**`, `pages/derived-project-form/**`, `pages/preexisting-project-form/**`, diálogos de revisión, `pages/portfolio-record-edit/**`, páginas de detalle, `pages/audit/**` y pruebas asociadas | `frontend-specialist` (consumidor); depende del contrato backend publicado. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioDtos.java`, `portfolio/application/ResponsibleUnitService.java`, `InitiativeApplicationService.java`, `ProjectApplicationService.java`, `PortfolioUpdateAuditDetail.java`, `organization/**` (catálogo con sigla), `config/reset/TestResetCoordinator.java`, pruebas bajo `apps/backend/src/test/**` | `backend-specialist` (propietario canónico del contrato y las reglas). |
| Database | No (sin cambios estructurales) | N/A (el esquema vigente ya soporta N filas ordenadas; `database/generated/piip-oracle.sql` solo se tocaría por regeneración autorizada si Hibernate derivara cambios, lo que no se espera) | Derivada de JPA; sin migraciones. |
| Contrato HTTP | Sí | DTO de create/update (arreglo `responsibleUnits` 1..N), respuesta con lista ordenada (incluye sigla vía unidad), detalle de eventos de auditoría de alta; generación `apps/backend/target/piip-openapi.json` con autorización | Propietario: backend; consumidores: frontend y docs. |
| Documentación | Sí | `docs/funcional/guia-funcional-piip.md`, `docs/architecture/piip-fields.md` | Agente principal; alineación en la misma entrega (FR-032). |

## Contexto técnico

**Lenguajes/versiones**: Java 21, Spring Boot 4.1, Hibernate JPA, Oracle JDBC existente; Angular 22 con componentes standalone.

**Dependencias principales**: `spring-boot-starter-data-jpa`, validación Bean Validation existente en DTOs, cliente Angular generado por `ng-openapi-gen` existente. No se agregan dependencias nuevas.

**Persistencia**: Hibernate JPA canónico; sin SQL nativo funcional, sin `JdbcTemplate`, sin Flyway/Liquibase. El seed DML existente permanece como la única excepción constitucional, bajo los perfiles exactos `test,test-reset`; esta feature no agrega DML: reutiliza el seed vigente y extiende la postvalidación en código Java.

**Validación propuesta**: pruebas unitarias backend de validación de lista (vacía, duplicada, inactiva, otra UE, sin sigla, históricas retenidas), atomicidad de `replace`, auditoría de altas/modificaciones con sigla y orden, DTO contract tests, postvalidación del reset (idempotencia, siglas, asociación, ≥2 activas por UE); pruebas frontend de la lista dinámica (renumeración, catálogo loading/vacío/error, accesibilidad, precarga del derivado); `OpenApiGenerationTest` y sincronización del cliente; integración Oracle del reset con autorización separada. Su ejecución requiere autorización explícita y no ocurre durante este plan.

**Plataforma objetivo**: monorepo vigente (`apps/backend`, `apps/frontend`); esquema Oracle SISPIIP solo para la integración autorizada del reset.

**Restricciones**:

- Supersede a la feature 013 únicamente en la cardinalidad del campo; sus demás reglas (estados editables, inmutables, versión, campo ausente, sin cambios efectivos, sustitución atómica) permanecen.
- Conservar nombres técnicos internos (`responsibleUnits`, `REGISTRO_UNIDAD_RESPONSABLE`); solo cambia la denominación visible.
- Vigencia, pertenencia y sigla aplican a nuevas incorporaciones; unicidad al conjunto completo confirmado; históricas retenidas (incluidas inactivas) permanecen como contexto, sin migraciones ni saneos automáticos.
- No inventar ni completar siglas; unidades sin sigla ocultas del catálogo de selección (Q2=A) y rechazadas por el servicio.
- Auditoría append-only sin cuerpos ni secretos; operación rechazada no registra modificación exitosa.
- Perfiles exactos `test,test-reset` para el seed; sin carga automática en `dev`/`prod`; sin perfil adicional.
- No alterar estados, transiciones, documentos, tareas ni notificaciones.

**Escala/alcance**: un campo funcional de los 23 en sus flujos de alta/edición/lectura/auditoría; listas de 1..N sin máximo funcional; 4 UOs sintéticas (2 por UE) reutilizadas; documentación funcional alineada.

## Verificación de la constitución

### Gate inicial (pre-diseño)

- **I. Fuente funcional**: el campo permanece entre los 23; el cambio de cardinalidad y denominación proviene de una decisión funcional aprobada del solicitante; la guía y la matriz se alinean en la misma entrega (FR-032). Cumple.
- **II. Estados y transiciones**: sin cambios de estados, transiciones ni condiciones de edición; la lista no altera el flujo base. Cumple.
- **III. Organización y seguridad**: la regla de pertenencia (unidades del mismo ámbito que la Unidad Ejecutora del registro) se conserva y se extiende a N unidades; autorización por rol/ámbito vigente (FR-025). Cumple.
- **IV. Persistencia**: JPA sigue canónico; no hay cambios estructurales (el esquema ya soporta N); no se agrega SQL nativo funcional; el seed vigente permanece dentro de la excepción constitucional (`test,test-reset`, DML-only, idempotente, fail-closed, prohibido en producción) y esta feature solo extiende su postvalidación en Java. Cumple.
- **V. Trazabilidad y calidad**: auditoría append-only; el detalle de altas/modificaciones se extiende con datos funcionales (unidad, nombre, sigla, Nro), sin cuerpos de solicitudes ni secretos; operación rechazada no registra éxito; los cambios requieren pruebas automatizadas (previstas). Cumple.

**Resultado**: gate de diseño satisfecho; no se requiere la excepción de DML más allá del seed vigente ya autorizado por la Constitución 1.3.0.

### Gate posterior al diseño

Re-evaluado tras generar `research.md`, `data-model.md`, `contracts/` y `quickstart.md`:

- El diseño no introduce tablas, columnas ni constraints nuevas: reutiliza la asociación ordenada vigente y su UK (registro, orden), respetando el principio IV.
- El contrato relaja únicamente la cardinalidad del arreglo `responsibleUnits` (mínimo uno, sin máximo) y extiende el detalle de eventos de auditoría con datos funcionales ya permitidos por el principio V.
- La postvalidación del reset se extiende en código Java dentro del coordinador existente, sin tocar las guardias fail-closed ni los perfiles exactos `test,test-reset` (principios IV y V).
- La UI conserva la validación efectiva de permisos en el backend (principio III) y la denominación visible se alinea con la documentación funcional (principio I).

**Resultado**: gate satisfecho tras el diseño. Las verificaciones ejecutables quedan pendientes de autorización operativa en la fase de tareas.

## Dependencias y secuencia

- **Propietario canónico**: backend define el contrato (DTO, reglas de lista, auditoría) y la postvalidación del reset.
- **Consumidores**: frontend (UI + cliente generado) y documentación funcional.
- **Orden obligatorio**:
  1. Backend: DTOs, servicio de lista (validaciones y atomicidad), auditoría de altas/modificaciones, catálogo con sigla, pruebas unitarias y contract tests.
  2. Publicación del contrato OpenAPI (`OpenApiGenerationTest`) con autorización explícita.
  3. Frontend: sincronización del cliente generado, lista dinámica en los tres alta-formularios (con precarga del derivado), edición completa, revisión/detalle/auditoría visible, mock y pruebas.
  4. Backend (paralelizable tras el paso 1): postvalidación extendida del reset y verificación del seed vigente.
  5. Documentación funcional (guía y matriz) una vez consolidado el comportamiento.
- **Paralelización permitida**: la postvalidación del reset (paso 4) y la documentación (paso 5) pueden avanzar en paralelo con el frontend (paso 3) una vez fijado el contrato; no se paralelizan cambios que compartan DTO, servicio, contrato o cliente generado.

## Estructura del proyecto

### Documentación de la feature

```text
specs/017-unidades-organicas-involucradas/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── http-contract.md
│   └── ui-contract.md
├── checklists/requirements.md
└── tasks.md                 # pendiente: /speckit.tasks
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/
├── portfolio/api/PortfolioDtos.java
├── portfolio/application/ResponsibleUnitService.java
├── portfolio/application/InitiativeApplicationService.java
├── portfolio/application/ProjectApplicationService.java
├── portfolio/application/PortfolioUpdateAuditDetail.java
├── organization/api/ OrganizationController.java y OrganizationQueryService.java
├── organization/persistence/OrganizationalUnitRepository.java   # solo si el filtro de sigla requiere un método nuevo
└── config/reset/TestResetCoordinator.java                       # postvalidación extendida
apps/backend/src/test/java/pe/gob/midagri/piip/
├── portfolio/**   (validaciones, aplicación, auditoría, contrato)
├── organization/** (catálogo con sigla, si cambia)
├── config/reset/** (postvalidación)
└── contract/OpenApiGenerationTest.java
apps/frontend/src/app/
├── core/ piip.models.ts, piip.repository.ts, piip-http.repository.ts, piip-mock.repository.ts
├── api/generated/**                      # regenerado, nunca editado a mano
├── pages/initiative-form/**, derived-project-form/**, preexisting-project-form/**
├── pages/portfolio-record-edit/**
├── páginas y diálogos de revisión y detalle de iniciativas y proyectos
├── pages/audit/**
└── pruebas asociadas
docs/funcional/guia-funcional-piip.md
docs/architecture/piip-fields.md
```

**Decisión de estructura**: sin entidades ni tablas nuevas; la lista se implementa sobre la asociación ordenada vigente con validación en el servicio de aplicación (mínimo uno, unicidad, orden continuo 1..N, sigla para nuevas incorporaciones); los formularios reutilizan un componente de presentación de lista (columnas Nro/Descripción/Abreviatura, adaptable, accesible) compartido por los tres alta-formularios y la edición; la denominación técnica interna se conserva y solo cambia la visible.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: CRUD del maestro de Unidades Orgánicas; Descripción/Abreviatura personalizadas por registro; reordenamiento manual; máximo funcional; inventar siglas; cambios de roles, ámbitos, estados o transiciones; funcionalidades documentales, tareas o notificaciones ajenas al requisito; migraciones/saneos/renumeraciones automáticos de históricos; renombrados técnicos de tabla/entidad/contrato; datos institucionales reales; iniciativas/proyectos mock en la inicialización; activación automática del perfil destructivo o perfiles adicionales; uso del seed en producción.
- **Specs `001`-`005` consultadas**: Ninguna; permanecen históricas.
- **Dependencias históricas aprobadas**: `specs/013-actualizar-registros-portafolio` — esta feature supersede exclusivamente su cardinalidad exactamente-una del campo (clarificación 2026-08-22, FR-014, FR-022A) por decisión aprobada del solicitante; sus demás reglas permanecen vigentes. `specs/015-inicializacion-oracle` — se reutilizan su seed sintético y su coordinador de reset sin alterar guardias ni perfiles; la divergencia menor 19/20 tablas permanece registrada sin acción.
- **NEEDS CLARIFICATION**: Ninguna; las decisiones del solicitante están aprobadas y las dos clarificaciones de la sesión 2026-09-07 (precarga del derivado, ocultar unidades sin sigla) están integradas en la spec.

## Seguimiento de complejidad

Ninguna decisión contradice la constitución; no aplica.
