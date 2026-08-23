# Plan de implementación: Actualización controlada de registros de portafolio

**Rama**: `refactor/backend` | **Fecha**: 2026-08-22 | **Spec**: [spec.md](./spec.md)

**Entrada**: especificación en `/specs/013-actualizar-registros-portafolio/spec.md`

**Nota**: este artefacto define diseño y secuencia. No autoriza implementación, pruebas, compilación, servidores, contenedores, generación OpenAPI, integración Oracle ni acciones Git.

## Resumen

Incorporar edición parcial y controlada de iniciativas, proyectos derivados y proyectos preexistentes mediante `PATCH /initiatives/{code}` y `PATCH /projects/{code}`. El backend extenderá los casos de uso propietarios creados por la feature 012, distinguirá propiedades ausentes de valores nulos explícitos, autorizará por `ADMINISTRADOR_PIIP` sobre la Unidad Ejecutora real, aplicará las matrices de campo y estado, conservará `@Version` como único control optimista y registrará un diff append-only dentro de la misma transacción.

Angular añadirá una pantalla dedicada y adaptable de edición, accesible principalmente desde el detalle. Cargará una copia fresca del registro y su versión, enviará solo cambios efectivos, permitirá seleccionar exactamente una Unidad Orgánica responsable mediante el mismo selector de las altas, protegerá cambios sin guardar, resolverá `409` mediante recarga explícita y volverá al detalle con confirmación visible. Backend define primero el contrato; OpenAPI y el cliente generado preceden a su consumo frontend.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| Los controllers de portafolio ya delegan consultas, comandos de iniciativa y comandos de proyecto, pero no exponen actualización general. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java`; `portfolio/application/{InitiativeApplicationService,ProjectApplicationService}.java` | Agregar un método PATCH a cada propietario; no reconstruir `PortfolioService`. |
| `PortfolioRecordEntity` ya contiene todos los campos editables, `updatedAt` y `@Version`. | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/persistence/PortfolioRecordEntity.java` | Agregar comportamiento de dominio, sin columnas ni setters para identidad, UE, origen, estado o cierre. |
| El repositorio dispone de lectura con `PESSIMISTIC_WRITE` por código y la creación derivada usa ese mismo lock. | `portfolio/persistence/PortfolioRecordRepository.java`; `ProjectApplicationService.createDerived(...)` | Serializar edición con derivación y conservar la comparación de versión esperada. |
| La autorización vigente valida una asignación exacta de rol y ámbito sobre la UE real. | `identity/application/LocalAuthorizationService.java`; `identity/application/RoleScopeGrant.java` | Reutilizar `requireUnit(ADMINISTRADOR_PIIP, record.executingUnit.id)` dentro de la transacción; no consultar al creador. |
| Las referencias de catálogo se resuelven por identidad, catálogo y activo. PEI/POI admiten nulo. | `catalogs/application/CatalogReferenceService.java`; `specs/011-centralizar-catalogos-piip/**` | Resolver solo campos presentes; una referencia histórica omitida permanece, pero una incluida debe estar activa. |
| `REGISTRO_UNIDAD_RESPONSABLE` conserva identidad, denominación y posición técnica; el servicio actual valida activo y UE al crear. | `portfolio/{application/ResponsibleUnitService.java,persistence/ResponsibleUnitEntity.java,persistence/ResponsibleUnitRepository.java}` | Extender el servicio con validación completa y sustitución atómica de una única UO; no crear otro modelo. |
| `AuditService.event` guarda JSON en `EVENTO_AUDITORIA.DETALLE_JSON` y participa en la transacción funcional. | `audit/application/AuditService.java`; `audit/persistence/AuditEventEntity.java` | Agregar un único evento por actualización efectiva y hacer rollback conjunto ante fallo. |
| `ApiExceptionHandler` ya traduce autorización, ausencia, versión, negocio y referencias. | `shared/api/ApiExceptionHandler.java`; `shared/application/error/**` | Reutilizar `ProblemDetail`; agregar tratamiento tipado de request PATCH inválido solo si la validación condicional lo necesita. |
| Angular conserva versiones en `PiipHttpRepository`, actualiza signals desde responses y presenta un mensaje específico para `409`. | `apps/frontend/src/app/core/{piip.repository.ts,piip-http.repository.ts,piip.models.ts}` | Incorporar comandos de edición, recarga focalizada y reconciliación por response; no crear un segundo versionado. |
| Los detalles concentran las acciones de ciclo de vida y los formularios de alta ya cargan catálogos/UO por identidad. | `apps/frontend/src/app/pages/{initiative-detail,project-detail,initiative-form,derived-project-form,preexisting-project-form}/**` | Agregar el acceso principal en detalle y reutilizar validadores/presentación sin mezclar edición con alta o borradores. |
| La guía funcional describe altas, ciclo de vida, UE, concurrencia y auditoría, pero no edición de registros. | `docs/funcional/guia-funcional-piip.md` | Actualizar la guía en implementación con acción, campos, restricciones, conflicto y evidencia de auditoría. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | Sí | `apps/frontend/src/app/{app.routes.ts,core/**,pages/initiative-detail/**,pages/project-detail/**,pages/portfolio-record-edit/**}` y pruebas relacionadas | Consumidor posterior al contrato y cliente generado. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/{portfolio/**,shared/**}` y pruebas equivalentes | Propietario canónico de contrato, autorización, transacción y auditoría. |
| Database | No | N/A; protección de `database/generated/piip-oracle.sql` y anotaciones estructurales JPA | El modelo vigente ya satisface campos, relación, orden, versión y auditoría. |
| Contrato HTTP | Sí | Dos endpoints PATCH, dos requests, `PortfolioRecordResponse`, `ProblemDetail`, `apps/backend/target/piip-openapi.json`, `apps/frontend/src/app/api/generated/**` | Backend y OpenAPI antes de Angular; los generados nunca se editan manualmente. |
| Documentación | Sí | `specs/013-actualizar-registros-portafolio/**`; `docs/funcional/guia-funcional-piip.md` | El plan define diseño; la implementación actualizará la guía funcional. |

## Contexto técnico

**Lenguajes/versiones**: Java 21, Spring Boot 4.1.0, Hibernate JPA, Angular 22, TypeScript 6 y RxJS 7.8.

**Dependencias principales**: Spring MVC, Bean Validation, Spring Security OAuth2 Resource Server, Spring Data JPA, Jackson existente, Springdoc, Angular Reactive Forms, Router, Signals, Angular Material y cliente `ng-openapi-gen`. No se agrega una dependencia productiva para modelar PATCH.

**Persistencia**: Hibernate JPA sobre Oracle mediante `REGISTRO_PORTAFOLIO`, `REGISTRO_UNIDAD_RESPONSABLE` y `EVENTO_AUDITORIA` existentes. No se modifica esquema, DDL, datos ni configuración Oracle.

**Validación propuesta**: pruebas MVC del contrato; pruebas unitarias y JPA de autorización, matrices, referencias, UO, no-op, rollback, concurrencia y auditoría; pruebas Vitest de repositorio, formulario, rutas, acciones, guard de descarte y conflicto; generación/comparación OpenAPI; regresión backend/frontend. Ejecutarlas requiere autorización explícita posterior.

**Plataforma objetivo**: monolito modular PIIP autenticado por Keycloak, autorizado con asignaciones Oracle y consumido por la SPA Angular.

**Restricciones**: no modificar alta, consulta, aprobación, derivación, transiciones, documentos, tareas o notificaciones; no editar inline; no reutilizar borradores de alta; no inferir la UE desde la selección activa; no exponer entidades JPA; no guardar cuerpos HTTP, secretos o contenido documental en auditoría; no editar código generado manualmente.

**Escala/alcance**: 2 endpoints PATCH, 2 requests parciales, 3 matrices de campos, 2 eventos funcionales, 2 rutas Angular que reutilizan 1 componente de edición adaptable, 1 selector único de UO, 0 cambios lógicos de esquema y 0 mecanismos nuevos de versión.

## Verificación de la constitución

*GATE: debe aprobarse antes del diseño y volver a revisarse al finalizarlo.*

### Gate inicial

- **I. Fuente funcional**: aprobado. Se conserva la matriz de 23 campos y la diferencia entre `NA` y `No aplica`; solo se habilitan los campos aprobados en la spec.
- **II. Estados y transiciones**: aprobado. Editar no cambia estado ni amplía matrices; solo valida `Presentado` para iniciativa sin derivado y `Proyecto en ejecución` para proyecto.
- **III. Organización y seguridad**: aprobado. Keycloak autentica y Oracle autoriza; la decisión usa una asignación activa de `ADMINISTRADOR_PIIP` que cubre la UE persistida del registro.
- **IV. Persistencia**: aprobado. JPA permanece como fuente canónica y no se introduce SQL, DDL, migración ni la excepción destructiva.
- **V. Trazabilidad y calidad**: aprobado. Cada cambio efectivo y su evento forman una unidad atómica append-only; se proponen pruebas antes de implementar.
- **Grounding SDD**: aprobado. El plan 012, Graphify, código, contratos, specs 009/011, constitución y guía funcional fueron contrastados; el repositorio decide ante diferencias.

### Gate posterior al diseño

Aprobado. [research.md](./research.md) resuelve ausencia frente a nulo, transacción, contrato, UI y secuencia sin `NEEDS CLARIFICATION`; [data-model.md](./data-model.md) demuestra cero cambio estructural y conserva el único `@Version`; [contracts/portfolio-updates.openapi.yaml](./contracts/portfolio-updates.openapi.yaml) cierra los requests y errores; y [quickstart.md](./quickstart.md) separa implementación de verificaciones que necesitan autorización. No se usa excepción constitucional ni se justifica complejidad extraordinaria.

## Dependencias y secuencia

- **Propietario canónico**: backend `portfolio/api` para el contrato HTTP; `InitiativeApplicationService` y `ProjectApplicationService` para reglas/transacciones; `PortfolioRecordEntity` para invariantes de mutación; `ResponsibleUnitService` para la asociación única.
- **Consumidores**: OpenAPI generado, cliente Angular, `PiipRepository`/`PiipHttpRepository`, pantalla de edición, detalles y guía funcional.
- **Orden obligatorio**: caracterización → DTO/commands y error 400 → dominio/repositorios/UO → casos de uso y auditoría → controllers/contrato → generación OpenAPI autorizada → cliente Angular → repositorio/modelos → UI/rutas/guard → guía → regresión autorizada.
- **Paralelización permitida**: solo pruebas o estilos que no compartan DTO, contrato, estado de repositorio, rutas ni fixtures. Backend, OpenAPI y frontend son dependientes y no se implementan en paralelo.

### Incrementos reversibles

| Incremento | Resultado y rutas principales | Dependencias | Criterio de reversión |
|------------|-------------------------------|--------------|-----------------------|
| 0. Baseline | Caracterizar requests/responses, errores, versión, autorización, UO y auditoría existentes. | Ninguna | No agregar PATCH si un comportamiento requerido carece de prueba o evidencia. |
| 1. Contrato parcial | Agregar DTO API con tracking de presencia, commands de application y validación condicional. | Incremento 0 | Ausente, nulo y propiedad inválida deben diferenciarse como especifica el contrato. |
| 2. Persistencia funcional | Agregar métodos de dominio, lectura bloqueante tipada y reemplazo validado de una UO. | Incremento 1 | Cero cambio JPA estructural; cualquier cambio de esquema detiene el incremento. |
| 3. Casos de uso | Implementar update de iniciativa/proyecto, no-op, versión, fecha y diff auditado en una transacción. | Incrementos 1-2 | Rechazos no cambian registro/UO/versión ni crean evento; fallo de auditoría revierte todo. |
| 4. Publicación HTTP | Exponer los dos PATCH y documentar 200/400/403/404/409/422; estabilizar OpenAPI. | Incremento 3 | Los endpoints existentes conservan contrato y regresión. |
| 5. Cliente y repositorio | Regenerar cliente con autorización; agregar comandos sparse, recarga fresca por código/UE y upsert de la respuesta. | Incremento 4 | El generado no se edita manualmente y no aparece un segundo mapa de versiones. |
| 6. Interfaz | Agregar rutas, componente adaptable, selector único de UO, visibilidad por UE/estado, conflicto y descarte supervisado. | Incremento 5 | Sin borrador, sin mutación inline y sin depender de la UE activa para autorizar. |
| 7. Cierre funcional | Actualizar detalles, guía y verificaciones autorizadas. | Incrementos 0-6 | Cero regresión en altas, lifecycle, documentos, tareas y notificaciones. |

## Estructura del proyecto

### Documentación de la feature

```text
specs/013-actualizar-registros-portafolio/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── portfolio-updates.openapi.yaml
├── checklists/
│   └── requirements.md
└── tasks.md                    # Se generará mediante /speckit-tasks
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/
├── portfolio/
│   ├── api/{PortfolioController,PortfolioDtos}.java
│   ├── application/{InitiativeApplicationService,ProjectApplicationService,PortfolioApplicationSupport,ResponsibleUnitService}.java
│   └── persistence/{PortfolioRecordEntity,PortfolioRecordRepository,ResponsibleUnitRepository}.java
└── shared/{api,application/error}/

apps/backend/src/test/java/pe/gob/midagri/piip/portfolio/{api,application}/

apps/frontend/src/app/
├── app.routes.ts
├── api/generated/**
├── core/{piip.models,piip.repository,piip-http.repository,piip-mock.repository}.ts
├── pages/{initiative-detail,project-detail,initiatives,projects}/**
├── pages/portfolio-record-edit/**
└── core/pending-changes.guard.ts

docs/funcional/guia-funcional-piip.md
```

**Decisión de estructura**: API posee binding y presencia JSON; application posee commands, autorización, resolución, transacción y diff; domain muta solo campos autorizados mediante operaciones explícitas; persistence conserva JPA. Angular usa un formulario dedicado común porque la interacción y la mayoría de campos son compartidos, pero deriva una matriz inmutable por tipo/origen y nunca sobrecarga los formularios de alta.

## Diseño por responsabilidad

### Contrato PATCH y validación de presencia

- `InitiativeUpdateRequest` y `ProjectUpdateRequest` serán beans API mutables con setters Jackson que registran qué propiedades llegaron. Un record nullable no sirve porque colapsaría ausente y nulo explícito.
- El controller convierte el DTO a `InitiativeUpdateCommand` o `ProjectUpdateCommand` con `FieldUpdate<T>(present, value)`. Los commands no dependen de Jackson ni de tipos HTTP.
- `version` es obligatoria y no negativa. Propiedades desconocidas o técnicas y nulos en campos obligatorios producen 400. Solo PEI, POI, nota y resultados clave de proyecto admiten nulo explícito.
- El request de proyecto incluye `solutionTypeId` porque aplica al derivado; si el registro real es preexistente, incluirlo produce 422. Iniciativa no declara `keyResults`, de modo que ese intento es un request cerrado inválido.
- Ausente conserva; presente resuelve y propone; sin campo editable o sin diferencia efectiva produce 422. `responsibleUnits` conserva la forma de arreglo por compatibilidad, pero una escritura válida contiene exactamente un elemento.

### Dominio, transacción y concurrencia

- Agregar operaciones de dominio explícitas por iniciativa, proyecto derivado y proyecto preexistente; ninguna acepta código, tipo, origen, UE, estado, producto final, cierre, creador o fechas técnicas como input.
- Buscar con lock por código y tipo de ruta. Código inexistente o de otro tipo produce 404; luego autorizar UE real, comparar versión, validar estado/origen y resolver todas las referencias antes de mutar.
- La secuencia autoritativa es autorización → versión → estado/vínculo → campos permitidos → referencias/UO → diff. Así una copia obsoleta produce 409 antes de reinterpretar sus datos.
- Validar la única nueva UO antes de eliminar. Si cambia, eliminar el hijo, hacer flush de la baja y guardar la asociación con posición técnica `1`; si no cambia, no tocar la asociación. Si la lectura histórica contiene varias UO y el campo está ausente, conservarlas.
- Calcular el candidato y diff antes de cambiar `updatedAt`. Una edición exclusiva de UO también actualiza el padre para que `@Version` avance.
- Ejecutar `records.flush()` antes de formar el evento para capturar la versión nueva real. `OptimisticLockException` y el control explícito mantienen 409.

### Auditoría

- Usar `INICIATIVA_ACTUALIZADA` o `PROYECTO_ACTUALIZADO` y entidad `REGISTRO_PORTAFOLIO`.
- El detalle será un objeto ordenado con `tipoRegistro`, `unidadEjecutoraId`, `unidadEjecutora`, `versionAnterior`, `versionNueva`, `cambios` y `resultado: EXITOSO`.
- `cambios` será un mapa por clave estable de campo; cada entrada contiene `anterior` y `nuevo`. Se usarán value objects o `LinkedHashMap`, no `Map.of`, porque los retiros admiten nulo.
- Catálogos se fotografían como `{id, code, name}` y la UO como una lista compatible de un elemento `{id, code, name, displayOrder: 1}`. Comparar la identidad completa hace efectivo un reemplazo.
- El evento se inserta después del flush del registro y antes de completar la transacción. Cualquier fallo revierte escalares, UO, fecha, versión y evento.

### Repositorio Angular y cliente generado

- Generar el OpenAPI real y después `api/generated/**`; no editar esos archivos a mano. Ambos pasos requieren autorización expresa en el turno de ejecución.
- Agregar modelos de presentación `InitiativeUpdateInput` y `ProjectUpdateInput` con campos opcionales, nulos explícitos permitidos y `responsibleUnitIds` de un único elemento. La versión del detalle debe quedar disponible al formulario sin crear un segundo control de versión.
- `PiipRepository` expondrá carga fresca y actualización por tipo/código. `PiipHttpRepository` construye un body sparse, invoca el cliente, actualiza `recordVersions` y hace upsert del response en los signals de registro/listado.
- La apertura de edición fuerza GET del detalle aunque exista caché. La carga de UO usa la UE del registro mediante el endpoint existente, sin cambiar ni usar como sustituto la UE activa.
- El mock implementará las nuevas firmas, reglas mínimas, versión y evento solo para conservar paridad de desarrollo; no se convierte en autoridad funcional.

### Experiencia de edición

- Crear `/iniciativas/:code/editar` y `/proyectos/:code/editar`, antes de las rutas dinámicas de detalle. Ambas cargan `PortfolioRecordEditComponent` con `recordType` en route data.
- El detalle muestra `Editar` solo cuando el usuario administra la UE real y el estado local es editable. El servidor vuelve a comprobar vínculo, rol, estado y versión. La primera entrega no agrega acción en listados porque FR-006 es opcional y el detalle concentra el contexto autoritativo.
- El formulario muestra como solo lectura los metadatos disponibles del response: código, tipo, origen, UE, estado, producto final, cierre y última modificación. No amplía el contrato para exponer el subject técnico del creador.
- Inicializar controles desde la copia fresca y construir el PATCH por comparación con el snapshot inicial. Una referencia histórica inactiva puede mostrarse como valor registrado y omitirse; si el usuario cambia ese campo, solo puede elegir una referencia activa.
- El editor de UO conserva la lista inicial, permite agregar, retirar y mover arriba/abajo con controles accesibles. Si se modifica, exige al menos una UO activa, sin duplicados y de la UE real.
- Tras 200, marcar la edición como completada, reconciliar signals, navegar al detalle y mostrar confirmación. Tras 409, conservar la copia local, deshabilitar el reenvío de esa copia y ofrecer `Recargar versión vigente`; nunca reintentar ni combinar automáticamente.
- Implementar `PendingChangesAware` y `pendingChangesGuard` para cancelación, navegación y botón atrás; complementar con `beforeunload` para recarga/cierre. La navegación interna usa confirmación accesible y el navegador controla el diálogo de cierre. Guardar o descartar desactiva el aviso. No existe borrador.

## Trazabilidad de requisitos

| Requisitos | Decisión de diseño | Verificación propuesta |
|---|---|---|
| FR-001, FR-002, FR-007, FR-008, FR-009, FR-010, FR-010A | Dos contratos PATCH por tipo de ruta, commands separados y operaciones de dominio que solo aceptan la matriz editable | Contrato MVC, matrices por tipo/origen y regresión de altas/consultas |
| FR-003, FR-004, FR-005, FR-011, FR-015A, FR-015B | Autorización server-side contra la UE real, visibilidad defensiva en UI y gates de estado/vínculo dentro de la transacción | Casos de autorización, revocación posterior a la apertura y estados no editables |
| FR-006 | Acción inicial únicamente en detalle; no hay mutación inline y la acción de listado permanece opcional | Navegación del detalle y ausencia de PATCH desde la fila |
| FR-007A, FR-007B, FR-018B | Carga fresca, guard de cambios pendientes, `beforeunload`, descarte supervisado, sin borrador y retorno al detalle tras guardar | Pruebas de navegación, cierre, cancelación, éxito y reapertura |
| FR-012, FR-013, FR-013A | Resolución de referencias por catálogo/actividad y representación de presencia con `FieldUpdate<T>` | Ausente, nulo explícito, catálogo incorrecto/inactivo y PEI/POI independientes |
| FR-014, FR-015 | Reemplazo de una única UO, validado antes de mutar y aplicado atómicamente | UO única, múltiple, vacío, otra UE, histórico y rollback |
| FR-016, FR-017 | Versión obligatoria, comparación tras autorización y 409 sin reintento automático | Dos copias concurrentes y recarga explícita de la versión vigente |
| FR-018, FR-018A, FR-030 | Response completo, diff previo, actualización automática de fecha/versión y rechazo de no-op | Response/versionado, edición exclusiva de UO y request sin cambio efectivo |
| FR-019, FR-020 | Matriz HTTP 400/403/404/409/422 sobre el mecanismo de errores vigente | Contrato MVC y mensajes con campo, referencia y causa |
| FR-021, FR-022, FR-022A, FR-022B, FR-023, FR-024 | Un evento funcional append-only con diff enriquecido dentro de la misma transacción, sin motivo inventado ni contenido sensible | Evento único, snapshots, nulos, cero evento en rechazo y rollback ante fallo |
| FR-025, FR-026, FR-027, FR-028 | Alcance cerrado: la edición no transiciona ni altera altas, relaciones, documentos, tareas, notificaciones, catálogos o esquema | Regresión de casos vigentes y revisión de ausencia de cambios JPA/DDL |
| FR-029 | Actualización de la guía funcional en el incremento de frontend | Revisión documental junto con acciones, campos y autorización visibles |

## Estrategia de verificación propuesta

1. **Contrato MVC**: PATCH separados, body sparse, ausente/nulo/desconocido, límites, response completo y tabla 400/403/404/409/422.
2. **Autorización**: administrador distinto del creador, grants cruzados, institución/UE y revocación después de abrir.
3. **Matrices**: iniciativa presentada sin derivado; proyecto derivado/preexistente en ejecución; solución prohibida para preexistente; campos técnicos rechazados.
4. **Referencias**: catálogo equivocado/inactivo/inexistente, PEI/POI independientes y retiros nulos.
5. **UO**: única, múltiple, vacío, duplicado, inactiva, otra UE, histórico y rollback íntegro.
6. **Concurrencia**: dos PATCH con la misma versión, edición frente a transición y edición de iniciativa frente a creación derivada.
7. **Auditoría**: diff exacto, snapshots enriquecidos, retiro nulo, versiones, un evento, cero en rechazo y rollback ante fallo.
8. **Repositorio Angular**: body sparse, versión vigente, llamada generada, upsert, carga fresca por UE real, 409 sin retry y recarga explícita.
9. **UI Angular**: rutas, matriz/read-only, selector de UO única, inactivos históricos, visibilidad por rol/UE/estado, success, guard y `beforeunload` sin borrador.
10. **Regresión y documentación**: altas, detalles, aprobación, derivación, transiciones, documentos, tareas y notificaciones; guía funcional consistente.
11. **Aceptación de usabilidad**: ejecuciones cronometradas desde detalle cargado hasta confirmación visible para SC-005 y cuestionario de clasificación completa por variante para SC-006, con muestra no vacía aprobada previamente y evidencia agregada sin datos sensibles.

Ninguna verificación automatizada se ejecuta durante `plan`; su ejecución y la generación OpenAPI/cliente requieren autorización explícita.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: eliminación/archivado como operación nueva; transiciones; CRUD de catálogos, identidad u organización; documentos, tareas y notificaciones; edición inline; borradores; migraciones/DDL/datos; nuevo versionado; cambio de relación derivada; refactorización general de la deuda residual de feature 012.
- **Specs `001`-`005` consultadas**: Ninguna; no son backlog. La autorización y campos se sustentan en fuentes vigentes.
- **Dependencias históricas aprobadas**: matrices de feature 009 ratificadas por constitución 1.2.0; identidades persistentes, PEI/POI independientes y asociación UO de feature 011; límites de servicios de feature 012.
- **Pendientes heredados no ampliados**: valores oficiales PEI/POI y autoridad institucional de feature 011 permanecen fuera de alcance; la edición solo usa referencias activas existentes.
- **NEEDS CLARIFICATION**: Ninguna.

## Seguimiento de complejidad

No existen contradicciones constitucionales. El tracking de presencia es necesario para cumplir ausente frente a nulo sin dependencia nueva; el componente Angular común evita tres formularios duplicados sin mezclar los casos de uso backend. No se introducen buses, eventos asíncronos, repositorios alternativos, tablas, capas vacías ni una refactorización transversal no requerida.
