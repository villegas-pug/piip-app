---
description: "Tareas ejecutables para la actualización controlada de registros de portafolio"
---

# Tareas: Actualización controlada de registros de portafolio

**Entrada**: documentos de `specs/013-actualizar-registros-portafolio/`

**Prerrequisitos**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/portfolio-updates.openapi.yaml` y `quickstart.md` vigentes; checklist de requisitos completo; cero `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza implementación, pruebas, builds, generación OpenAPI, cliente Angular, integración Oracle ni acciones Git. La invocación explícita de `/speckit-implement` autoriza únicamente las tareas de implementación de la feature activa; las ejecuciones marcadas con autorización separada siguen requiriéndola en el turno correspondiente.

## Formato obligatorio

Cada tarea usa `- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta/archivo exacto`.

- **[P]** identifica trabajo posible en paralelo después de satisfacer sus dependencias, sin compartir archivos ni decisiones pendientes.
- **[US#] / [FR-###]** vincula cada tarea con la especificación.
- **[X]** se utilizará solo cuando exista evidencia del cambio realizado.

## Evidencia de baseline — no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|---|---|---|---|
| US1 / FR-001 | Existen altas y consultas separadas, pero ningún PATCH general | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java` | Extender el controller y los servicios propietarios; no reconstruir `PortfolioService`. |
| US1 / FR-016 | El registro ya conserva `@Version` y fecha de modificación | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` | Reutilizar el control optimista; no crear token o tabla adicional. |
| US2 / FR-003 | La autorización exacta por rol y UE real ya existe | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/LocalAuthorizationService.java` | Revalidar dentro del caso de uso y no depender del creador. |
| US4 / FR-014 | La UO responsable ya está normalizada y la posición técnica se conserva | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/ResponsibleUnitEntity.java` | Reemplazar atómicamente una única asociación; no crear otro modelo. |
| US5 / FR-021 | La auditoría funcional vigente es append-only y transaccional | `apps/backend/src/main/java/pe/gob/midagri/piip/audit/application/AuditService.java` | Agregar un evento por cambio efectivo sin guardar el body HTTP. |
| US1 / FR-018 | Angular conserva versiones y reconcilia responses en signals | `apps/frontend/src/app/core/piip-http.repository.ts` | Extender el repositorio sin crear una segunda fuente de versión. |
| US6 / FR-026 | Altas, aprobación, derivación y transiciones ya tienen cobertura | `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/` | Tratar esos recorridos como regresión, no como funcionalidad por reimplementar. |

## Phase 1: Preparación y caracterización

**Propósito**: fijar el comportamiento vigente que la nueva mutación debe preservar antes de cambiar contrato o consumidores.

- [X] T001 [P] [FR-026] Ampliar la caracterización de requests, responses, versión y errores vigentes del portafolio en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioControllerContractTest.java`
- [X] T002 [P] [FR-026] Ampliar la caracterización de caché, mapa de versiones, upsert y mensaje 409 vigentes en `apps/frontend/src/app/core/piip-http.repository.spec.ts`

**Checkpoint**: las capacidades actuales quedan documentadas por pruebas sin declarar todavía soporte PATCH.

---

## Phase 2: Fundamentos bloqueantes del contrato y dominio

**Propósito**: establecer los tipos y operaciones compartidos que necesitan todas las historias antes de publicar endpoints o modificar Angular.

- [X] T003 [FR-001] [FR-013A] Definir `InitiativeUpdateRequest` y `ProjectUpdateRequest` cerrados, con `version` obligatoria, límites y tracking Jackson de propiedades presentes en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioDtos.java`
- [X] T004 [FR-013A] [FR-030] Crear `FieldUpdate<T>`, `InitiativeUpdateCommand` y `ProjectUpdateCommand` independientes de HTTP en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateCommands.java`
- [X] T005 [FR-008] [FR-009] [FR-010] [FR-010A] [FR-018A] Agregar operaciones de dominio explícitas para aplicar candidatos editables y actualizar automáticamente `updatedAt` sin exponer setters técnicos en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java`
- [X] T006 [FR-001] [FR-016] Agregar lectura bloqueante por código y tipo de ruta, preservando `@Version`, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordRepository.java`
- [X] T007 [FR-014] [FR-015] Incorporar lectura determinista, eliminación con flush y reemplazo validado de una única UO responsable en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/ResponsibleUnitRepository.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitService.java`
- [X] T008 [FR-021] [FR-022] [FR-022A] Crear los value objects de snapshot y diff estable para campos, catálogos y la única UO en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateAuditDetail.java`
- [X] T009 [FR-019] [FR-020] [FR-030] Completar el mapeo de request inválido, no-op y conflicto de persistencia a `ProblemDetail` 400/409/422 en `apps/backend/src/main/java/pe/gob/midagri/piip/shared/api/ApiExceptionHandler.java`

**Checkpoint**: ausencia, nulo explícito, versión, mutación de dominio, UO y diff tienen propietarios definidos sin cambios JPA/DDL.

---

## Phase 3: User Story 1 — Editar un registro autorizado (Priority: P1) 🎯 MVP demostrable

**Objetivo**: un administrador cubierto por la UE real actualiza el mismo registro mediante el PATCH correspondiente y recibe la representación completa con nueva fecha y versión.

**Prueba independiente**: abrir una iniciativa o proyecto editable con un administrador distinto del creador, cambiar un campo permitido, guardar y comprobar mismo código, respuesta completa, `updatedAt` nuevo, versión superior y retorno al detalle.

### Pruebas de US1

- [X] T010 [P] [US1] [FR-001] Crear pruebas de aplicación para happy paths de iniciativa, proyecto derivado y proyecto preexistente, incluidas matrices de campos e identidad inmutable, en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateApplicationTest.java`
- [X] T011 [P] [US1] [FR-001] [FR-018] Crear pruebas MVC para los dos PATCH, body sparse, 200, response completo y rechazo 400 de propiedades técnicas/desconocidas en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioControllerUpdateContractTest.java`

### Implementación backend y contrato de US1

- [X] T012 [US1] [FR-001] [FR-002] [FR-008] Implementar `InitiativeApplicationService.update(...)` con carga bloqueante, autorización por UE real, candidato, no-op, mutación, flush y response completo en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java`
- [X] T013 [US1] [FR-001] [FR-002] [FR-009] [FR-010] Implementar `ProjectApplicationService.update(...)` diferenciando proyecto derivado y preexistente sin cambiar origen, relación o UE en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ProjectApplicationService.java`
- [X] T014 [US1] [FR-001] [FR-018] Publicar `PATCH /api/v1/initiatives/{code}` y `PATCH /api/v1/projects/{code}` con mapping DTO-command y respuestas 200/400/403/404/409/422 en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java`
- [X] T015 [US1] [FR-001] Contrastar anotaciones y schemas reales con el contrato de diseño y ajustar únicamente diferencias justificadas en `specs/013-actualizar-registros-portafolio/contracts/portfolio-updates.openapi.yaml`
- [X] T016 [US1] [FR-018] Generar y revisar el contrato runtime en `apps/backend/target/piip-openapi.json` mediante la prueba OpenAPI del proyecto — requiere autorización separada y depende de T014-T015
- [X] T017 [US1] [FR-018] Regenerar, sin edición manual, las operaciones y modelos PATCH en `apps/frontend/src/app/api/generated/` mediante `npm run api:generate` — requiere autorización separada y depende de T016

### Implementación frontend de US1

- [X] T018 [US1] [FR-007] [FR-013A] Definir inputs sparse por variante y operaciones de carga fresca/actualización en `apps/frontend/src/app/core/piip.models.ts` y `apps/frontend/src/app/core/piip.repository.ts`
- [X] T019 [US1] [FR-007] [FR-018] Implementar GET fresco, body sparse, versión vigente, llamada al cliente generado y upsert del response en `apps/frontend/src/app/core/piip-http.repository.ts` (depende de T017-T018)
- [X] T020 [US1] [FR-018] Adaptar creación, actualización, versión y respuesta completa del repositorio de desarrollo en `apps/frontend/src/app/core/piip-mock.repository.ts`
- [X] T021 [P] [US1] [FR-013A] [FR-018] Cubrir body sparse, nulos explícitos, carga fresca, versión y reconciliación de signals en `apps/frontend/src/app/core/piip-http.repository.spec.ts`
- [X] T022 [US1] [FR-007] [FR-007A] [FR-007B] [FR-008] [FR-009] [FR-010] Crear `PortfolioRecordEditComponent` standalone con carga fresca, variante por route data, snapshot inicial, formulario reactivo, guardado y manejo `beforeunload` que advierta solo ante cambios pendientes en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.ts`
- [X] T023 [P] [US1] [FR-007] Crear la presentación de metadatos read-only, campos editables y acciones Guardar/Cancelar en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.html`
- [X] T024 [P] [US1] [FR-007] Crear estilos accesibles y responsivos del formulario dedicado en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.scss`
- [X] T025 [US1] [FR-007] [FR-007A] [FR-007B] [FR-018B] Probar variantes, campos read-only, body sparse, confirmación visible, navegación al detalle tras 200 y evento `beforeunload` condicionado al estado sucio/limpio en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.spec.ts`
- [X] T026 [US1] [FR-007] Registrar `/iniciativas/:code/editar` y `/proyectos/:code/editar` antes de las rutas dinámicas de detalle en `apps/frontend/src/app/app.routes.ts`
- [X] T027 [US1] [FR-007A] [FR-007B] Implementar `PendingChangesAware` y `pendingChangesGuard` para navegación/cancelación sin borrador en `apps/frontend/src/app/core/pending-changes.guard.ts`
- [X] T028 [P] [US1] [FR-007A] [FR-007B] Probar permanecer, descartar, estado limpio y ausencia de persistencia local en `apps/frontend/src/app/core/pending-changes.guard.spec.ts`

**Checkpoint**: US1 puede demostrarse de extremo a extremo con un registro autorizado y editable; todavía no constituye una entrega productiva hasta completar US2-US5.

---

## Phase 4: User Story 2 — Impedir ediciones fuera de rol, ámbito o estado (Priority: P1)

**Objetivo**: ocultar defensivamente la acción y rechazar autoritativamente con 403/422 cualquier confirmación sin rol, UE real o estado/vínculo válido.

**Prueba independiente**: repetir el PATCH con usuario sin rol, administrador de otra UE, asignación revocada y registro no editable; todos conservan datos, versión y ausencia de evento funcional exitoso.

- [X] T029 [P] [US2] [FR-003] [FR-004] [FR-015A] [FR-015B] Crear pruebas de autorización exacta, grants no combinables, revocación posterior a la carga y matrices de estado/vínculo en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateAuthorizationTest.java`
- [X] T030 [US2] [FR-003] [FR-004] [FR-011] [FR-015A] [FR-015B] Aplicar en ambos casos de uso la secuencia UE real → versión → estado/vínculo y preservar la relación derivada en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ProjectApplicationService.java`
- [X] T031 [P] [US2] [FR-019] [FR-020] Cubrir respuestas 403/404/422 y propiedades comprensibles para ruta/tipo/estado en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/api/PortfolioControllerUpdateContractTest.java`
- [X] T032 [P] [US2] [FR-005] Crear y probar una decisión pura de elegibilidad local por tipo, estado, relación y cobertura de UE en `apps/frontend/src/app/core/portfolio-edit-permissions.ts` y `apps/frontend/src/app/core/portfolio-edit-permissions.spec.ts`
- [X] T033 [US2] [FR-005] [FR-006] Incorporar la acción principal `Editar` con navegación contextual y visibilidad defensiva en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.ts` y `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.html`
- [X] T034 [US2] [FR-005] [FR-006] Incorporar la acción principal `Editar` con navegación contextual y visibilidad defensiva en `apps/frontend/src/app/pages/project-detail/project-detail.component.ts` y `apps/frontend/src/app/pages/project-detail/project-detail.component.html`
- [X] T035 [P] [US2] [FR-005] Probar visibilidad y navegación por rol, UE, estado y relación en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.spec.ts`
- [X] T036 [P] [US2] [FR-005] Probar visibilidad y navegación por rol, UE, estado y variante de proyecto en `apps/frontend/src/app/pages/project-detail/project-detail.component.spec.ts`
- [X] T037 [US2] [FR-004] [FR-019] Presentar 403/404/422 sin borrar silenciosamente la copia local y bloquear confirmaciones posteriores inválidas en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.ts` y `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.spec.ts`

**Checkpoint**: invocar una ruta directamente no elude autorización ni estado; la UI no se usa como autoridad.

---

## Phase 5: User Story 3 — Resolver una edición concurrente (Priority: P1)

**Objetivo**: impedir que una copia obsoleta sobrescriba una actualización o una mutación de ciclo de vida y exigir recarga explícita.

**Prueba independiente**: dos sesiones abren la misma versión; la primera guarda, la segunda recibe 409, conserva su copia sin reintento y solo reemplaza el baseline al pulsar recarga.

- [X] T038 [P] [US3] [FR-016] [FR-017] Crear pruebas de dos PATCH concurrentes, edición frente a transición y edición de iniciativa frente a derivación en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateConcurrencyTest.java`
- [X] T039 [US3] [FR-016] [FR-017] Consolidar lock tipado, comparación de versión antes de reglas dependientes de la copia y traducción de conflicto al flush en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordRepository.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ProjectApplicationService.java`
- [X] T040 [P] [US3] [FR-017] Probar que el repositorio HTTP no reintenta un PATCH 409 y ofrece una carga fresca explícita en `apps/frontend/src/app/core/piip-http.repository.spec.ts`
- [X] T041 [US3] [FR-017] Conservar cambios locales, deshabilitar el reenvío obsoleto y ofrecer `Recargar versión vigente` en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.ts`, `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.html` y `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.spec.ts`

**Checkpoint**: una versión antigua nunca sobrescribe la vigente ni genera un evento de éxito.

---

## Phase 6: User Story 4 — Validar referencias y pertenencia organizacional (Priority: P1)

**Objetivo**: aceptar solo catálogos activos correctos y una única UO activa perteneciente a la UE inmutable del registro.

**Prueba independiente**: confirmar referencias y una UO válida, luego repetir con referencia inactiva/equivocada y UO vacía/múltiple/de otra UE; solo el caso válido cambia.

- [X] T042 [P] [US4] [FR-012] [FR-013] [FR-014] [FR-015] Crear pruebas de catálogos, PEI/POI independientes, nulos permitidos, UO única/múltiple, inactividad, otra UE y rollback en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateReferenceTest.java`
- [X] T043 [US4] [FR-012] [FR-013] [FR-014] [FR-015] Resolver solo referencias presentes, validar exactamente una UO antes de escribir y aplicar su reemplazo dentro de ambos casos de uso en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioApplicationSupport.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ResponsibleUnitService.java`, `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ProjectApplicationService.java`
- [X] T044 [US4] [FR-014] Integrar el selector standalone de una única UO responsable con control accesible en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.ts`
- [X] T045 [US4] [FR-014] Crear template y estilos del selector único, sin posición ni ordenamiento, en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.html` y `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.scss`
- [X] T046 [US4] [FR-014] Probar selección única, teclado, vacío, múltiples referencias rechazadas y emisión de un ID en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.spec.ts`
- [X] T047 [US4] [FR-012] [FR-013] Integrar catálogos activos por UE real, PEI/POI independientes y valores históricos inactivos no reescribibles en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.ts` y `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.html`
- [X] T048 [US4] [FR-013A] [FR-014] Probar retiros nulos, referencia histórica omitida, carga de UO por UE real, PATCH con una sola UO y preservación de históricos en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.spec.ts`

**Checkpoint**: un elemento inválido revierte el conjunto completo y la respuesta conserva la única asociación confirmada con posición técnica `1`.

---

## Phase 7: User Story 5 — Conservar trazabilidad de la actualización (Priority: P1)

**Objetivo**: registrar exactamente un evento append-only, atómico y seguro por actualización efectiva, con versiones y diff anterior/nuevo enriquecido.

**Prueba independiente**: actualizar un registro, comprobar un único evento nuevo con actor, UE, versiones y solo cambios efectivos, y forzar un fallo de auditoría para verificar rollback total.

- [X] T049 [US5] [FR-021] [FR-022] [FR-022A] Crear pruebas de diff escalar, catálogo, reemplazo de UO única, eventos previos intactos y ausencia de motivo en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateAuditTest.java`
- [X] T050 [US5] [FR-022] [FR-022A] [FR-022B] Implementar snapshots `{id, code, name}`, UO única con posición técnica `1` y serialización estable con valores nulos en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateAuditDetail.java`
- [X] T051 [US5] [FR-021] [FR-023] [FR-024] Emitir `INICIATIVA_ACTUALIZADA` o `PROYECTO_ACTUALIZADO` después del flush y antes de completar la transacción, sin body HTTP ni datos sensibles, en `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/InitiativeApplicationService.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/ProjectApplicationService.java`
- [X] T052 [US5] [FR-021] [FR-024] Probar exactamente un evento por cambio, cero por 400/403/404/409/422 y rollback de registro/UO/versión ante fallo de auditoría en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/application/PortfolioUpdateAuditTest.java`

**Checkpoint**: cambio y evento se confirman o revierten juntos; los eventos anteriores permanecen inmutables.

---

## Phase 8: User Story 6 — Conservar altas, consultas y ciclo de vida (Priority: P2)

**Objetivo**: asegurar que edición no modifica contratos ni efectos de alta, consulta, aprobación, derivación, transición, documentos, tareas o notificaciones.

**Prueba independiente**: ejecutar los recorridos existentes antes y después de incorporar PATCH y comprobar que conservan rutas, requests, responses, estados, relaciones y efectos.

- [X] T053 [P] [US6] [FR-025] [FR-026] [FR-028] Ampliar regresión backend de alta, aprobación, derivación, preexistente, consultas y transiciones sin cambios de esquema en `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioFlowPersistenceTest.java` y `apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/PortfolioTransitionTest.java`
- [X] T054 [P] [US6] [FR-025] [FR-026] Verificar por pruebas frontend que detalle, listados y acciones de ciclo de vida mantienen sus contratos fuera de edición en `apps/frontend/src/app/pages/initiative-detail/initiative-detail.component.spec.ts`, `apps/frontend/src/app/pages/project-detail/project-detail.component.spec.ts`, `apps/frontend/src/app/pages/initiatives/initiatives.component.spec.ts` y `apps/frontend/src/app/pages/projects/projects.component.spec.ts`
- [X] T055 [P] [US6] [FR-029] Documentar acción, matrices, UE real, UO única, errores, conflicto, descarte y auditoría sin ampliar el flujo vigente en `docs/funcional/guia-funcional-piip.md`

**Checkpoint**: la edición es una mutación acotada y la guía funcional refleja su impacto observable.

---

## Phase 9: Pulido y controles transversales

- [X] T056 [US1] [FR-007] Revisar accesibilidad, foco, etiquetas, orden por teclado, estados de carga/error y responsive del editor en `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.html` y `apps/frontend/src/app/pages/portfolio-record-edit/portfolio-record-edit.component.scss`
- [X] T057 [FR-027] [FR-028] Auditar estáticamente el diff para confirmar ausencia de cambios en `database/generated/piip-oracle.sql`, DDL, CRUD excluidos, borradores, edición inline y archivos generados manualmente desde `F:/work-space/piip-monorepo`
- [X] T058 [FR-026] Actualizar el índice estructural después de los cambios materiales de código mediante `graphify update .` desde `F:/work-space/piip-monorepo` y revisar el resultado en `graphify-out/` antes del checkpoint de sesión

## Validaciones propuestas — requieren autorización separada

- [X] T059 [FR-026] Ejecutar la suite focalizada backend desde `apps/backend` con `.\gradlew.bat test --tests "pe.gob.midagri.piip.portfolio.*"` — autorización requerida
- [X] T060 [FR-026] Ejecutar la verificación backend completa desde `apps/backend` con `.\gradlew.bat check` — autorización requerida
- [X] T061 [FR-026] Ejecutar la suite frontend desde `apps/frontend` con `npm test -- --watch=false` — autorización requerida
- [X] T062 [FR-026] Ejecutar el build frontend desde `apps/frontend` con `npm run build` — autorización requerida
- [X] T063 [US1] [FR-007] Ejecutar el protocolo cronometrado de SC-005 y registrar inicio, fin, duración, recaptura y resultado en `specs/013-actualizar-registros-portafolio/quickstart.md` — autorización y coordinación de aceptación requeridas
- [X] T064 [US1] [FR-005] Ejecutar el protocolo de comprensión de SC-006 con la muestra no vacía aprobada y registrar total, aprobados, porcentaje, perfiles y variantes en `specs/013-actualizar-registros-portafolio/quickstart.md` — autorización y coordinación de aceptación requeridas
- [X] T065 [FR-029] Registrar cambios realizados, validaciones ejecutadas y comprobaciones aún no autorizadas en `specs/013-actualizar-registros-portafolio/quickstart.md` después de T058-T064

La integración Oracle no es requisito de esta feature porque no existe cambio estructural; solo se propondrá si una incidencia funcional concreta la justifica y el usuario la autoriza.

## Dependencias y orden de ejecución

### Grafo de historias

```text
Phase 1 Baseline
      │
      v
Phase 2 Fundamentos
      │
      v
US1 Edición autorizada y contrato
      │
      v
US2 Protección
      │
      v
US3 Concurrencia
      │
      v
US4 Referencias y UO
      │
      v
US5 Auditoría
      │
      v
US6 Regresión y guía
      │
      v
Pulido, Graphify y validaciones
```

- **Propietario canónico**: T003-T016 en backend y contrato; T017 no puede comenzar antes del OpenAPI runtime revisado.
- **Consumidores**: T018-T028 consumen el contrato; T032-T048 completan protección, concurrencia y referencias sobre esa base.
- **Orden obligatorio**: T001-T002 → T003-T009 → T010-T015 → T016 → T017 → T018-T028. La integración compartida en `InitiativeApplicationService` y `ProjectApplicationService` sigue T030 → T039 → T043 → T051; solo sus pruebas o consumidores en archivos distintos pueden adelantarse. US6 requiere US1-US5 y T058 se ejecuta después de cambios materiales de código y antes del checkpoint.
- **Gates de autorización**: T016, T017 y T059-T064 no se ejecutan por efecto de `/speckit-implement`; requieren autorización explícita adicional. T065 registra únicamente la evidencia realmente obtenida.
- **Sin dependencia de esquema**: ninguna tarea modifica `database/generated/piip-oracle.sql`, mappings estructurales JPA o datos Oracle.

## Oportunidades de ejecución paralela

- **Preparación**: T001 y T002 pueden escribirse en paralelo porque pertenecen a árboles distintos y solo caracterizan baseline.
- **US1**: T010 y T011 pueden redactarse en paralelo después de T003-T009; T023 y T024 pueden avanzar en paralelo después de fijar el modelo del componente T022; T021 y T028 no comparten archivos.
- **US2-US5**: pueden prepararse en paralelo únicamente pruebas o consumidores que no compartan archivos; la integración productiva T030 → T039 → T043 → T051 permanece secuencial.
- **US3**: T038 y T040 pueden prepararse en paralelo después de congelar contrato y repositorio; la integración productiva T039/T041 permanece secuencial por dominio.
- **US4**: T042 y T044 pueden prepararse en paralelo tras US1; T045 puede avanzar junto con pruebas backend una vez definida la API del editor.
- **US6**: T053, T054 y T055 pueden avanzar en paralelo después de US2-US5 porque separan backend, frontend y documentación.

## Ejemplos paralelos por historia

```text
US1: T010 (application tests) || T011 (MVC tests)
US2: T029 (authorization tests) || T032 (frontend eligibility helper)
US3: T038 (backend concurrency tests) || T040 (HTTP repository conflict test)
US4: T042 (backend reference tests) || T044 (UO editor component)
US5: T049 primero; T050-T052 son secuenciales porque comparten modelo/evento/pruebas
US6: T053 (backend regression) || T054 (frontend regression) || T055 (guide)
```

## Estrategia de implementación

### MVP demostrable

1. Completar Phase 1 y Phase 2.
2. Completar US1 hasta T028, incluidos los gates autorizados de OpenAPI y cliente.
3. Demostrar edición válida por ruta directa con mismo código, response completo y nueva versión.

Este MVP sirve para demostrar el flujo principal, pero **no es liberable a producción** sin US2-US5, porque autorización negativa, carreras, integridad referencial/UO y auditoría son requisitos P1 de seguridad e integridad.

### Entrega incremental liberable

1. Backend y contrato canónico: T001-T016.
2. Cliente y experiencia base: T017-T028.
3. Protecciones P1: T029-T052.
4. Regresión, guía, pulido y actualización estructural: T053-T058.
5. Ejecutar únicamente las validaciones y aceptaciones T059-T064 que el usuario autorice.
6. Registrar en T065 solo la evidencia realmente obtenida y las comprobaciones aún pendientes.

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- Dependencias históricas directas aprobadas: matrices de estados de feature 009, identidades persistentes y UO normalizadas de feature 011, y propietarios de aplicación de feature 012.
- No se crean tareas para completar pendientes heredados de PEI/POI, reorganizar la arquitectura 012 o sustituir `PROYECTO_UNIDAD_ORGANICA`.
- `NEEDS CLARIFICATION`: Ninguna.

## Notas

- Cada checkbox representa trabajo futuro; ningún ítem se marca completado durante `/speckit-tasks`.
- Las pruebas se incluyen porque la spec define criterios independientes y el plan exige caracterización/regresión; escribirlas es implementación, ejecutarlas requiere autorización separada.
- No paralelizar cambios que compartan el contrato HTTP, servicios de aplicación, reglas de referencia/UO, auditoría o archivos de detalle/editor.
- El hook Git posterior es opcional y no autoriza commit por sí mismo.
