# Plan de implementación: Ciclo de vida del portafolio PIIP

**Rama**: `main` | **Fecha**: 2026-08-18 | **Spec**: [spec.md](./spec.md)

**Entrada**: especificación en `/specs/009-ciclo-vida-portafolio/spec.md`

**Nota**: este artefacto define el diseño técnico. No autoriza implementación, generación OpenAPI, pruebas, builds, servidores, Oracle ni acciones Git.

## Resumen

Ampliar el ciclo de vida de iniciativas y proyectos sin alterar la aprobación existente ni convertir la relación de derivación en un estado. El backend mantendrá `PortfolioService` como límite transaccional y aplicará matrices explícitas separadas en el dominio, con autorización por `Administrador PIIP` y Unidad Ejecutora, `@Version`, coordinación bloqueante de la iniciativa frente a la derivación, reloj inyectable para `America/Lima` y auditoría funcional atómica. El frontend incorporará acciones contextuales en el detalle de iniciativa y un nuevo detalle de proyecto; los listados conservarán consulta y filtros, pero nunca ejecutarán transiciones.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| La aprobación vigente usa `POST /api/v1/initiatives/{code}/approval` con `ApprovalRequest` y devuelve `PortfolioRecordResponse`. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java`, `PortfolioDtos.java` | Conservar ruta, request, response y comportamiento; las demás decisiones de iniciativa tendrán un endpoint separado. |
| `PortfolioService.approve(...)` autoriza por Unidad Ejecutora, valida versión y coordina tareas, notificaciones y auditoría. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java` | Extender el mismo servicio sin duplicar el límite transaccional ni la autorización. |
| El estado, `FECHA_CIERRE`, la relación de origen y `@Version VERSION` ya pertenecen a `PortfolioRecordEntity`. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` | No crear columnas, tablas ni un segundo mecanismo de versión. |
| La derivación comprueba estado aprobado y usa `existsByOriginRecordId(...)`, pero la lectura actual no serializa una carrera con archivado. | `PortfolioService.java`, `PortfolioRecordRepository.java` | Incorporar lectura JPA con bloqueo de escritura para la iniciativa tanto en derivación como en transición. |
| `AuditService.event(...)` participa en la transacción llamadora y `AuditEventEntity` almacena detalle JSON append-only. | `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditService.java`, `audit/persistence/AuditEventEntity.java` | Reutilizar la auditoría funcional existente; una falla debe revertir la mutación. |
| La auditoría de acceso se registra de forma independiente. | `apps/backend/src/main/java/pe/gob/midagri/piip/audit/api/AccessAuditFilter.java`, `AuditService.java` | Los rechazos no crearán éxito funcional, pero conservarán el resultado HTTP técnico. |
| El detalle de iniciativa ya obtiene iniciativa, versión y proyecto derivado y contiene la aprobación. | `apps/frontend/src/app/pages/initiative-detail/**`, `apps/frontend/src/app/core/piip-http.repository.ts` | Ampliar el contexto sin mover la aprobación al listado. |
| Los proyectos solo tienen listado y expediente documental; no existe detalle general de proyecto. | `apps/frontend/src/app/pages/projects/**`, `apps/frontend/src/app/app.routes.ts` | Crear `ProjectDetailComponent` y mantener la ruta documental específica. |
| Los dos listados reutilizan el catálogo global de estados. | `apps/frontend/src/app/core/piip.catalogs.ts`, `pages/initiatives/**`, `pages/projects/**` | Derivar listas contextuales para evitar mezclar estados, sin eliminar los filtros. |
| El cliente Angular se genera desde el OpenAPI producido por el backend. | `apps/backend/src/test/java/pe/gob/midagri/piip/OpenApiGenerationTest.java`, `apps/frontend/ng-openapi-gen.json` | El backend es propietario del contrato; la regeneración del cliente se ejecutará solo con autorización posterior. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | Sí | `apps/frontend/src/app/core/**`, `pages/initiative-detail/**`, `pages/project-detail/**`, `pages/initiatives/**`, `pages/projects/**`, `pages/audit/**`, `app.routes.ts`, `api/generated/**` | Consumidor del contrato backend; se adapta después de estabilizar OpenAPI. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/**`, `config/**`, pruebas de `portfolio/**` y `OpenApiGenerationTest.java` | Propietario canónico de reglas, transacción y contrato. |
| Database | No | `database/generated/piip-oracle.sql` no requiere cambio lógico | JPA existente ya contiene `ESTADO`, `FECHA_CIERRE`, `VERSION`, origen y auditoría. |
| Contrato HTTP | Sí | `POST /api/v1/initiatives/{code}/status-transitions`, `POST /api/v1/projects/{code}/status-transitions`, dos DTO de request y OpenAPI generado | Backend primero; cliente Angular después. |
| Documentación | Sí | `docs/funcional/guia-funcional-piip.md`, `specs/009-ciclo-vida-portafolio/**` | La guía debe reflejar el flujo implementado con evidencia, sin sustituir spec/código. |

## Contexto técnico

**Lenguajes/versiones**: Java 21, Spring Boot 4.1, TypeScript, Angular 22.

**Dependencias principales**: Spring Web/Jakarta Validation, Spring Data JPA/Hibernate, Spring Security JWT, Oracle, springdoc OpenAPI, Angular standalone, RxJS, Angular Material y cliente `ng-openapi-gen` existentes.

**Persistencia**: Hibernate JPA sobre Oracle; `REGISTRO_PORTAFOLIO` y `EVENTO_AUDITORIA` existentes. No hay migración ni SQL nativo.

**Validación propuesta**: pruebas unitarias del dominio y servicios, pruebas MVC/contrato, pruebas JPA de concurrencia, pruebas Vitest de repositorio/componentes/presentador, regeneración y comparación OpenAPI y, si se autoriza, integración Oracle focalizada. Ninguna se ejecuta durante `/speckit-plan` y todas las ejecuciones requieren autorización explícita.

**Plataforma objetivo**: aplicación web PIIP actual, backend Spring Boot y frontend Angular, con Oracle y Keycloak según la configuración vigente.

**Restricciones**: conservar aprobación y creación derivada; no mezclar estados; no ofrecer `NOT_APPLICABLE`; no bloquear por documentos; no persistir matriz ni historial paralelo; no exponer entidades JPA; no confiar en opciones del frontend; fecha de cierre automática en `America/Lima`; errores `ProblemDetail` con `400/403/404/409/422`.

**Escala/alcance**: 2 endpoints nuevos, 2 DTO de request, 12 transiciones nuevas permitidas (3 de iniciativa y 9 de proyecto), 2 eventos funcionales, 1 detalle general de proyecto, 0 cambios lógicos de esquema y 0 mecanismos nuevos de versionado.

## Verificación de la constitución

*GATE inicial: APROBADO. Revisión posterior al diseño: APROBADO.*

- Los 23 campos y catálogos se conservan; `No Aplicable` permanece como valor catalogado, pero fuera de las transiciones.
- La constitución 1.1.0 ratifica expresamente las matrices contextuales de la feature 009. Esta feature no infiere transiciones desde el catálogo: solo admite las aristas autorizadas por la constitución y detalladas en `spec.md`.
- La autorización se resuelve en el servicio con la identidad Keycloak y las asignaciones Oracle de `Administrador PIIP` para la Unidad Ejecutora real.
- JPA sigue siendo fuente canónica; no se introduce SQL nativo, `JdbcTemplate`, procedimientos, Flyway ni Liquibase.
- Los controladores solo adaptan HTTP; las matrices, concurrencia, atomicidad y auditoría pertenecen al dominio y servicio de aplicación.
- Los contratos usan DTO y `ProblemDetail`; ninguna entidad JPA se expone.
- La auditoría funcional es append-only y atómica; la auditoría de acceso no almacena token, body ni contenido documental.
- La guía funcional se actualizará en la entrega de implementación por existir impacto en flujos y acciones visibles.
- El `NEEDS CLARIFICATION` sobre posibles bloqueos documentales es deliberadamente no bloqueante: la regla decidida para esta versión es que los documentos nunca impiden una transición.

## Dependencias y secuencia

- **Propietario canónico**: backend `portfolio` para matrices, autorización, concurrencia, persistencia, auditoría y contrato HTTP.
- **Consumidores**: cliente Angular generado, `PiipHttpRepository`, detalles/listados, presentador de auditoría y guía funcional.
- **Orden obligatorio**: (1) dominio y persistencia JPA sin cambio de esquema; (2) servicio transaccional y controlador/DTO; (3) pruebas del contrato; (4) generación OpenAPI solo si se autoriza; (5) regeneración/adaptación del cliente Angular; (6) UI contextual y filtros; (7) documentación y validación autorizada.
- **Paralelización permitida**: únicamente pruebas/documentación o componentes que no compartan contrato ni modelos. Backend y frontend no deben implementarse en paralelo mientras el contrato esté cambiando.

## Estructura del proyecto

### Documentación de la feature

```text
specs/009-ciclo-vida-portafolio/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── portfolio-status-transitions.openapi.yaml
└── tasks.md                         # Se generará únicamente con /speckit-tasks
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/
├── config/
│   └── TimeConfig.java                              # nuevo reloj inyectable America/Lima
├── portfolio/
│   ├── api/PortfolioController.java
│   ├── api/PortfolioDtos.java
│   ├── application/PortfolioService.java
│   └── persistence/
│       ├── PortfolioRecordEntity.java
│       └── PortfolioRecordRepository.java
└── audit/application/AuditService.java              # reutilización; solo adaptar si requiere constantes

apps/backend/src/test/java/pe/gob/midagri/piip/
├── OpenApiGenerationTest.java
└── portfolio/                                       # pruebas focalizadas existentes y nuevas

apps/frontend/src/app/
├── api/generated/**                                 # regenerado desde contrato autorizado
├── core/
│   ├── piip.models.ts
│   ├── piip.catalogs.ts
│   ├── piip.repository.ts
│   └── piip-http.repository.ts
├── pages/
│   ├── initiative-detail/**
│   ├── project-detail/**                            # nuevo detalle contextual
│   ├── initiatives/**
│   ├── projects/**
│   └── audit/audit-event.presenter.ts
└── app.routes.ts

docs/funcional/guia-funcional-piip.md
```

**Decisión de estructura**: las matrices se codifican como comportamientos explícitos separados de `PortfolioRecordEntity`, sin setter genérico ni tabla de reglas. `PortfolioService` orquesta autorización, bloqueo, versión, reloj y auditoría. `PortfolioController` expone rutas separadas por contexto. El frontend usa modelos y listas contextuales distintas, un detalle nuevo para proyecto y conserva la aprobación existente en el detalle de iniciativa.

## Diseño por responsabilidad

### Dominio y persistencia

- Agregar comportamientos separados para transición de iniciativa y proyecto que reciban el destino y los valores temporales ya calculados; cada método valida tipo, origen y matriz antes de mutar.
- Mantener `@Version` como único control optimista. Comparar `version` del request antes de aplicar la operación y dejar que JPA incremente `VERSION` al confirmar.
- Agregar en `PortfolioRecordRepository` una consulta con `@Lock(PESSIMISTIC_WRITE)` para obtener la iniciativa por código, sin SQL nativo.
- Usar la lectura bloqueante tanto en `createDerived(...)` como en las transiciones de iniciativa. Bajo el mismo bloqueo, validar estado y ausencia/presencia del proyecto vinculado; así solo puede ganar una de las operaciones concurrentes.
- No modificar `closingDate` excepto al entrar en `FINISHED`; no limpiarla ni recalcularla en otros destinos.

### Aplicación, seguridad y auditoría

- Agregar `transitionInitiativeStatus(...)` y `transitionProjectStatus(...)` como métodos `@Transactional` independientes en `PortfolioService`.
- Autorizar sobre la Unidad Ejecutora real y validar código, tipo, versión, estado, destino y vínculo con datos recargados por el servidor.
- Inyectar un `Clock` configurado con `ZoneId.of("America/Lima")`; obtener de él tanto `Instant updatedAt` como `LocalDate closingDate` para resultados deterministas.
- Registrar `ESTADO_INICIATIVA_CAMBIADO` o `ESTADO_PROYECTO_CAMBIADO` mediante `AuditService.event(...)` antes de salir de la transacción. El JSON contendrá estados, rol, identificador/nombre de Unidad Ejecutora, observación normalizada a texto vacío y `EXITOSO`; actor y fecha permanecen en columnas existentes.
- No registrar evento funcional de éxito ante rechazo. Conservar la auditoría de acceso y las convenciones `ProblemDetail`.

### HTTP y OpenAPI

- Agregar requests separados con `@NotNull version`, `@NotNull targetStatus` y `@Size(max = 1000) observation`.
- Conservar `ApprovalRequest`, `/approval` y `PortfolioRecordResponse` sin sustitución.
- Exponer los dos `POST .../status-transitions`; el servidor rechaza código/tipo cruzado, destinos fuera de matriz y `NOT_APPLICABLE`.
- Mantener el response exitoso con estado/fecha/version actualizados y documentar `400/403/404/409/422` con el `ProblemDetail` existente.

### Frontend

- Definir inputs separados para iniciativa/proyecto y arreglos de estados contextuales. El catálogo global puede conservarse para visualización, pero no alimentará directamente selectores ni filtros.
- Extender `PiipRepository` y `PiipHttpRepository` con lectura de proyecto y dos operaciones de transición. Reutilizar `recordVersions`; enviar la versión cargada, actualizarla desde la respuesta y conservar el tratamiento de `409` que exige recarga.
- En `InitiativeDetailComponent`, mantener el botón de aprobación actual; mostrar archivado/no admisible solo según la matriz y ocultar todas las acciones si existe `derivedProject`.
- Crear `ProjectDetailComponent` con estado actual, datos generales, origen, fecha de cierre, acceso a documentos/auditoría y selector de destinos calculado desde la matriz. Los terminales no presentan selector.
- Declarar `proyectos/:code/documentos` antes de `proyectos/:code`; cambiar “Abrir” del listado al detalle general y conservar el acceso documental.
- Mantener listados como consulta. Sus filtros tendrán únicamente estados de su tipo y nunca confirmarán una transición.
- Incorporar etiquetas y campos de detalle de los dos eventos nuevos al presentador de auditoría.

## Estrategia de verificación propuesta

- **Dominio**: tabla parametrizada con todas las aristas permitidas, orígenes/destinos prohibidos, estados terminales, mezcla de tipos y exclusión de `NOT_APPLICABLE`.
- **Servicio**: rol/ámbito, versión obsoleta, iniciativa vinculada, observación, fecha Lima, preservación de `closingDate`, evento único y rollback cuando la auditoría falla.
- **Concurrencia**: escenarios derivar-versus-archivar sobre la misma iniciativa y dos transiciones con la misma versión; nunca debe persistir una combinación inválida.
- **HTTP/OpenAPI**: requests/response y códigos `400/403/404/409/422`; `/approval` continúa igual.
- **Frontend**: opciones contextuales por estado/tipo, bloqueo por proyecto vinculado, ausencia de transición en listados, `409`, navegación de proyecto y detalle de auditoría.
- **Regresión**: registrar → aprobar → crear derivado conserva exactamente `Presentado`, `Iniciativa aprobada` y `Proyecto en ejecución`; documentos pendientes y `No Aplicable` no bloquean ni aparecen como destinos.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: bloquear por documentos; transición a `No Aplicable`; reabrir estados terminales; creación automática de proyecto; convertir “Proyecto derivado” en estado; tabla de matriz o historial de estados; segundo versionado; edición de `closingDate`; transiciones desde listados; cambios a tareas/notificaciones de aprobación; cambios de esquema Oracle.
- **Specs `001`-`005` consultadas**: Ninguna; son antecedentes históricos y el grounding se realizó contra código, documentación y contratos vigentes.
- **Dependencias históricas aprobadas**: flujo actual de aprobación y derivación registrado como baseline, no como trabajo pendiente.
- **NEEDS CLARIFICATION**: regla futura sobre qué documentos podrían bloquear cada transición. No bloquea esta versión porque `FR-023` establece explícitamente que ningún documento impide transiciones en v1.

## Seguimiento de complejidad

No se introducen contradicciones aprobadas que requieran excepción constitucional. La lectura pesimista se limita a la iniciativa en operaciones mutuamente excluyentes y complementa, no reemplaza, el control optimista existente.
