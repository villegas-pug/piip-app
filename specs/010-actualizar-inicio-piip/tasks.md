---
description: "Tareas ejecutables para la actualización de Inicio PIIP"
---

# Tareas: Actualización de Inicio PIIP

**Entrada**: documentos de `/specs/010-actualizar-inicio-piip/`

**Prerrequisitos**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/home-portfolio.openapi.yaml` y `quickstart.md` vigentes; sin checklists ni `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza implementación, pruebas, builds, generación OpenAPI ni integración Oracle. `/speckit-implement` autoriza únicamente las tareas de implementación; T007, T008 y T037–T040 requieren autorización separada en el turno de ejecución.

## Formato obligatorio

Cada tarea usa `- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta exacta`. `[P]` identifica trabajo en archivos distintos y sin dependencia pendiente.

## Evidencia de baseline — no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| US1 / FR-002 | Los registros persistentes ya contienen tipo, código, nombre, estado, UE y `updatedAt` | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` | Reutilizar entidad y catálogos; no cambiar esquema. |
| US1 / FR-026 | La autorización local ya valida una UE legible | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/LocalAuthorizationService.java` | Invocar `requireReadableUnit`; no duplicar RBAC. |
| US1 / FR-004 | Existen rutas y detalles diferenciados por tipo | `apps/frontend/src/app/app.routes.ts` | Resolver `Ver detalle` con rutas reales existentes. |
| US2 / FR-007 | Los estados canónicos ya están disponibles por tipo | `apps/frontend/src/app/core/piip.catalogs.ts` | Derivar opciones y restablecer estados incompatibles. |
| US2 / FR-008 | Existe paginación accesible con `PIIP_PAGE_SIZE = 5` | `apps/frontend/src/app/shared/pagination/piip-pagination.component.ts` y `apps/frontend/src/app/shared/pagination/piip-pagination.utils.ts` | Reutilizar componente y tamaño; no crear otra paginación. |
| US3 / FR-023 | La API ya lista avisos personales y marca uno como leído | `apps/backend/src/main/java/pe/gob/midagri/piip/work/api/NotificationController.java` | Conservar contrato, destinatario y lectura explícita. |
| US3 / FR-017 | AppShell ya contiene la campana | `apps/frontend/src/app/layout/app-shell.component.ts` y `apps/frontend/src/app/layout/app-shell.component.html` | Adaptar badge y foco; eliminar lectura implícita. |

## Phase 1: Preparación compartida

**Propósito**: definir modelos de frontera antes de implementar propietario y consumidor.

- [X] T001 [FR-003] Definir `HomePortfolioResponse`, `HomePortfolioItemResponse` y `PortfolioStatusCountResponse` sin exponer JPA en `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/api/DashboardDtos.java`
- [X] T002 [P] [FR-003] Definir `HomePortfolioQuery`, `HomePortfolioItem`, `HomePortfolioResult` y `PortfolioStatusCount` de presentación en `apps/frontend/src/app/core/piip.models.ts`

---

## Phase 2: Contrato backend bloqueante

**Propósito**: establecer la consulta unificada y publicar el cliente antes de modificar Inicio.

- [X] T003 [FR-002] Implementar con JPA los predicados compartidos, la página `updatedAt DESC, id DESC`, la agrupación positiva y el conteo base de UE en `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/persistence/DashboardPortfolioQueryRepository.java`
- [X] T004 [FR-009] Implementar autorización exacta, normalización de filtros/página, reconciliación de totales y transacción `readOnly` en `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/application/DashboardPortfolioService.java` (depende de T001 y T003)
- [X] T005 [FR-002] Exponer `GET /api/v1/dashboard/portfolio` con `executingUnitId`, `q`, `type`, `status`, `page` y `size` en `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/api/DashboardController.java` (depende de T001 y T004)
- [X] T006 [P] [FR-003] Añadir aserciones del endpoint, parámetros, esquemas y respuestas Problem Detail en `apps/backend/src/test/java/pe/gob/midagri/piip/contract/OpenApiGenerationTest.java`
- [X] T007 [FR-003] Generar `apps/backend/target/piip-openapi.json` ejecutando `gradlew.bat test --tests pe.gob.midagri.piip.contract.OpenApiGenerationTest` desde `apps/backend` — autorización explícita requerida; depende de T005 y T006
- [X] T008 [FR-003] Regenerar `apps/frontend/src/app/api/generated/**` ejecutando `npm run api:generate` desde `apps/frontend` sin editar archivos generados manualmente — autorización explícita requerida; depende de T007

**Checkpoint**: el endpoint unificado, el OpenAPI y el cliente generado están disponibles antes de adaptar el consumidor Angular.

---

## Phase 3: User Story 1 — Comprender el portafolio de la UE activa (P1) 🎯 MVP

**Objetivo**: mostrar exclusivamente el portafolio real de la UE activa, con total, estados, detalle y vacíos coherentes.

**Prueba independiente**: seleccionar una UE con iniciativas y proyectos conocidos y comprobar que encabezado, filas, total, indicadores y distribución pertenecen solo a esa UE; una UE sin registros muestra un único estado vacío.

- [X] T009 [P] [US1] Probar UE estricta, mezcla de tipos, orden estable, agrupación y conteo base en `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/persistence/DashboardPortfolioQueryRepositoryTest.java`
- [X] T010 [P] [US1] Probar autorización, reconciliación del total, vacío real y DTO mínimo en `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/application/DashboardPortfolioServiceTest.java`
- [X] T011 [P] [US1] Probar bindings por defecto, 200, 401/403 y compatibilidad del resumen legado en `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/api/DashboardControllerTest.java`
- [X] T012 [US1] Añadir estado y operaciones de carga del portafolio de Inicio al contrato y mock en `apps/frontend/src/app/core/piip.repository.ts` y `apps/frontend/src/app/core/piip-mock.repository.ts` (depende de T002 y T008)
- [X] T013 [US1] Mapear el endpoint generado, UE activa, página de cinco y errores independientes en `apps/frontend/src/app/core/piip-http.repository.ts` (depende de T008 y T012)
- [X] T014 [US1] Implementar el estado base, derivaciones por estado, navegación de detalle y descarte de respuestas obsoletas en `apps/frontend/src/app/pages/dashboard/dashboard.component.ts` (depende de T013)
- [X] T015 [US1] Renderizar contexto de UE, total, indicadores, distribución textual/visual, tabla, acciones, carga, error, vacío y reintento en `apps/frontend/src/app/pages/dashboard/dashboard.component.html` (depende de T014)
- [X] T016 [US1] Aplicar composición responsive, etiquetas de estado, tabla y foco visible sin copiar datos ilustrativos en `apps/frontend/src/app/pages/dashboard/dashboard.component.scss` (depende de T015)
- [X] T017 [US1] Probar alcance de UE, seis datos por fila, conteos reconciliados, vacío/error, reintento y rutas de detalle en `apps/frontend/src/app/pages/dashboard/dashboard.component.spec.ts` (depende de T014 y T015)

**Checkpoint**: US1 funciona como incremento independiente sin filtros interactivos ni cambios de notificación.

---

## Phase 4: User Story 2 — Buscar y filtrar sin perder coherencia (P2)

**Objetivo**: aplicar búsqueda, tipo, estado y paginación al mismo conjunto global del listado, indicadores y gráfico.

**Prueba independiente**: combinar búsqueda por código/nombre, tipo y estado sobre más de cinco registros y reconciliar todas las páginas, el total y los conteos; un filtro sin coincidencias no se presenta como UE vacía.

- [X] T018 [P] [US2] Probar búsqueda solo por código/nombre, tipo, estado, conteos positivos, cruces de página y normalización a página 0 en `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/persistence/DashboardPortfolioQueryRepositoryTest.java`
- [X] T019 [P] [US2] Probar enums inválidos, tamaño fuera de rango y filtros combinados en `apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/api/DashboardControllerTest.java`
- [X] T020 [US2] Enviar `q`, `type`, `status` y `page`, reconciliar respuestas fuera de orden y mantener cinco elementos en `apps/frontend/src/app/core/piip-http.repository.ts` (depende de T013)
- [X] T021 [US2] Implementar debounce de búsqueda, opciones canónicas, reseteo a `Estado: Todos`, reinicio de página y estado `noResults` en `apps/frontend/src/app/pages/dashboard/dashboard.component.ts` (depende de T014 y T020)
- [X] T022 [US2] Añadir controles accesibles de búsqueda/tipo/estado, paginación reutilizada y mensaje sin coincidencias en `apps/frontend/src/app/pages/dashboard/dashboard.component.html` (depende de T021)
- [X] T023 [US2] Ajustar distribución responsive de filtros, indicadores, gráfico y paginación en `apps/frontend/src/app/pages/dashboard/dashboard.component.scss` (depende de T022)
- [X] T024 [US2] Probar debounce, filtros combinados, reseteo de estado incompatible, cinco filas, paginación, sin resultados y cambio rápido de UE en `apps/frontend/src/app/pages/dashboard/dashboard.component.spec.ts` (depende de T021 y T022)

**Checkpoint**: US2 conserva un único conjunto filtrado y es comprobable sin depender de notificaciones.

---

## Phase 5: User Story 3 — Consultar y leer notificaciones personales (P2)

**Objetivo**: mostrar avisos del usuario, expandirlos en Inicio y cambiar lectura únicamente mediante el botón de cada fila.

**Prueba independiente**: alternar `Todas`/`No leídas`, expandir más de tres avisos y activar la campana sin cambiar lecturas; marcar una fila reduce el badge exactamente en uno y no modifica otros avisos.

- [X] T025 [P] [US3] Probar que listar no muta lecturas, que solo se devuelve el destinatario y que `PUT /notifications/{id}/read` no afecta avisos ajenos en `apps/backend/src/test/java/pe/gob/midagri/piip/work/api/NotificationControllerTest.java`
- [X] T026 [US3] Separar carga/error de notificaciones, estado de filas en lectura y actualización puntual sin recargar el dashboard legado en `apps/frontend/src/app/core/piip-http.repository.ts`
- [X] T027 [P] [US3] Probar lista personal, contador, fallo recuperable y lectura puntual sin efectos colaterales en `apps/frontend/src/app/core/piip-http.repository.spec.ts`
- [X] T028 [US3] Implementar pestañas, resumen de tres avisos, expansión en línea y acción por fila en `apps/frontend/src/app/pages/dashboard/dashboard.component.ts` (depende de T026)
- [X] T029 [US3] Renderizar `#mis-notificaciones`, pestañas semánticas, estados leído/no leído, `Ver todas`, `aria-expanded`, carga, error y reintento en `apps/frontend/src/app/pages/dashboard/dashboard.component.html` (depende de T028)
- [X] T030 [US3] Estilizar resumen compacto, lista expandida, tabs, estados y acciones con foco visible en `apps/frontend/src/app/pages/dashboard/dashboard.component.scss` (depende de T029)
- [X] T031 [US3] Probar tres avisos, expansión, filtros, ausencia, errores y que solo `Marcar como leída` cambia una fila en `apps/frontend/src/app/pages/dashboard/dashboard.component.spec.ts` (depende de T028 y T029)
- [X] T032 [P] [US3] Sustituir la lectura implícita de la campana por badge numérico, navegación a `/inicio` y foco de `#mis-notificaciones` en `apps/frontend/src/app/layout/app-shell.component.ts` y `apps/frontend/src/app/layout/app-shell.component.html` (depende de T026)
- [X] T033 [US3] Probar badge exacto, navegación/foco desde otra ruta y ausencia de lectura automática en `apps/frontend/src/app/layout/app-shell.component.spec.ts` (depende de T032)

**Checkpoint**: US3 mantiene notificaciones personales e independientes del alcance de UE y no inventa enlaces contextuales.

---

## Phase 6: Documentación, consistencia y cierre

- [X] T034 [FR-001] Actualizar el recorrido cronológico de Inicio, filtros, estados, navegación y lectura explícita en `docs/funcional/guia-funcional-piip.md`
- [X] T035 [P] [FR-030] Revisar formato, placeholders, rutas y alcance mediante `git diff --check` y búsquedas estáticas sobre `specs/010-actualizar-inicio-piip/`, `apps/backend/`, `apps/frontend/` y `docs/funcional/guia-funcional-piip.md`
- [X] T036 [FR-030] Actualizar el índice estructural ejecutando `graphify update .` desde `F:/work-space/piip-monorepo` después de los cambios materiales de código y documentación

## Validaciones propuestas — requieren autorización

- [X] T037 [US1] [US3] Ejecutar pruebas backend focalizadas con `gradlew.bat test --tests "pe.gob.midagri.piip.dashboard.*" --tests "pe.gob.midagri.piip.work.api.NotificationControllerTest"` desde `apps/backend` — autorización explícita requerida
- [X] T038 [US2] [US3] Ejecutar pruebas frontend focalizadas con `npm test -- --watch=false --include src/app/core/piip-http.repository.spec.ts --include src/app/pages/dashboard/dashboard.component.spec.ts --include src/app/layout/app-shell.component.spec.ts` desde `apps/frontend` — autorización explícita requerida
- [X] T039 [FR-030] Ejecutar `npm run build` desde `apps/frontend` — autorización explícita requerida
- [ ] T040 [FR-030] Ejecutar `gradlew.bat check` desde `apps/backend` — autorización explícita requerida
- [X] T041 [FR-030] Verificar exclusiones de formato para dependencias, artefactos y cliente generado en `apps/frontend/.prettierignore`

## Dependencias y orden de ejecución

- **Propietario canónico**: T001 y T003–T007 en backend definen DTO, consulta, servicio, endpoint y OpenAPI.
- **Consumidores**: T008 regenera el cliente; T012–T024 adaptan repositorio e Inicio; T026–T033 adaptan notificaciones y AppShell.
- **Orden obligatorio**: T001/T003 → T004 → T005 → T007 → T008 → T012/T013 → historias frontend. T006 puede prepararse en paralelo, pero T007 espera T005 y T006.
- **Historias**: US1 depende de Phase 2; US2 depende del incremento US1 de Inicio; US3 depende de Phase 2 y puede desarrollarse después de T026 en paralelo con US2 porque usa archivos compartidos de Dashboard y debe integrarse secuencialmente allí.
- **Cierre**: T034 y T035 siguen a las historias; T036 sigue a cambios materiales; T037–T040 solo se ejecutan con autorización.

## Oportunidades de ejecución paralela

### Preparación y contrato

- T001 y T002 usan árboles distintos.
- T006 puede prepararse mientras se implementan T003–T005.

### User Story 1

- T009, T010 y T011 cubren capas backend distintas.
- Tras T008, T012 puede avanzar mientras se completan pruebas backend.

### User Story 2

- T018 y T019 cubren persistencia y HTTP en archivos distintos.
- T024 puede prepararse a partir de criterios de aceptación mientras T021–T023 se implementan, pero se completa después de ellos.

### User Story 3

- T025 y T027 cubren backend y frontend en árboles distintos.
- Tras T026, T032 puede avanzar en AppShell mientras T028–T031 avanzan en Dashboard.

## Estrategia de implementación

### MVP primero

1. Completar Phase 1 y Phase 2 con las autorizaciones necesarias para OpenAPI/cliente.
2. Completar US1 (T009–T017).
3. Verificar independientemente el portafolio base de la UE activa antes de añadir filtros o notificaciones.

### Entrega incremental

1. **MVP**: portafolio real, alcance exacto, totales y vacío coherente.
2. **Incremento 2**: búsqueda, filtros, página global e indicadores reconciliados.
3. **Incremento 3**: notificaciones personales, expansión, lectura explícita y campana accesible.
4. **Cierre**: guía funcional, Graphify y validaciones autorizadas.

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- Dependencia histórica aprobada: estados y transiciones ratificados por feature 009 y constitución 1.1.0; esta feature solo los consulta.
- No crear tareas para tareas, prioridades, vencimientos, avance físico, nuevos disparadores/destinatarios, referencias contextuales, estados/transiciones, esquema Oracle o cambios del `/dashboard` legado.
- `NEEDS CLARIFICATION`: Ninguna.
- `T040` fue ejecutada con autorización, pero `gradlew.bat check` quedó fallida por `LocalE2eUserProvisionerTest` al no poder conectar con Oracle/TNS; no se atribuye a esta feature.

## Notas

- Cada tarea representa trabajo nuevo y apunta a rutas concretas.
- No marcar `[X]` sin evidencia de implementación o validación ejecutada.
- No editar manualmente `apps/frontend/src/app/api/generated/**`.
- No paralelizar cambios que compartan contrato, catálogo, regla funcional, documentación o configuración.
