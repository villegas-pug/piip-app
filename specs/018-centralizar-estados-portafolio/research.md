# Investigación y decisiones: Centralización de estados del portafolio

**Rama**: `018-centralizar-estados-portafolio` | **Fecha**: 2026-09-10 | **Spec**: [spec.md](./spec.md)

Estado: investigación completa. Las aclaraciones de la spec (actividad inicial: todos activos; consulta central: solo activos con metadata separada en históricos) están integradas. **No quedan `NEEDS CLARIFICATION`.**

Las decisiones se grounding en fuentes canónicas (código backend/frontend, Constitución 1.3.0, specs históricas 009/011/015/016, docs de arquitectura y operación) y en los análisis read-only de los especialistas. Cada entrada usa el formato Decisión / Fundamento / Alternativas descartadas.

## D1: Catálogo especializado en el módulo portfolio, no `CATALOGO_ITEM`

- **Decisión**: tabla propia `ESTADO_PORTAFOLIO` con entidad `PortfolioStatusCatalogEntity` en `pe.gob.midagri.piip.portfolio.persistence`, PK natural `CODIGO` tipada como el enum `PortfolioStatus`.
- **Fundamento**: reutilizar `CATALOGO_ITEM` exigiría aplicabilidad nullable para todos los tipos de ítem, referenciar el identificador interno o introducir FK compuesta `(ID_CATALOGO, CODIGO)` y validar pertenencia al catálogo correcto; eso rompe la identidad natural por código que exigen FR-003 y FR-004. El repositorio ya tiene precedente aprobado de catálogo especializado (`TIPO_DOCUMENTO` / `DocumentTypeEntity`), validado en la feature 011.
- **Alternativas descartadas**: (a) quinto catálogo genérico en `CATALOGO_ITEM`: contamina el modelo común y complica la identidad; (b) tabla genérica de pares código-atributo: meta-modelo innecesario para un dominio estable de once valores.

## D2: Columna `ESTADO` conservada con asociación JPA read-only

- **Decisión**: `REGISTRO_PORTAFOLIO.ESTADO` conserva nombre, tipo textual y valores actuales (códigos del enum). Se añade `@ManyToOne` hacia `PortfolioStatusCatalogEntity` con `insertable=false, updatable=false` y join por `CODIGO`, produciendo la FK de integridad y la carga de metadata vigente.
- **Fundamento**: evita reescribir históricos (FR-019); la FK real impide eliminar estados referenciados (edge case de eliminación); la metadata se lee sin N+1 si los `EntityGraph` del repositorio incluyen la asociación.
- **Alternativas descartadas**: (a) columna FK nueva con migración de datos: innecesaria y riesgosa para históricos; (b) lookup lógico sin FK: pierde integridad referencial y el rechazo de eliminación.

## D3: Enum `PortfolioStatus` conservado como tipo de código y sede de matrices

- **Decisión**: el enum mantiene las once constantes y su papel en las matrices y estados iniciales. `label()` deja de usarse como fuente de denominación visible y `values()` deja de usarse como inventario funcional en todos los consumidores.
- **Fundamento**: las matrices son reglas ratificadas (principio II de la Constitución) que no deben derivarse de datos; el nombre visible pasa a ser metadata persistente (FR-007, FR-010, FR-011).
- **Alternativas descartadas**: eliminar el enum y tipar todo por `String`: pierde tipado de matrices y validación estática; mantener `values()` como inventario visible: viola FR-011.

## D4: Respuesta de catálogo especializada sin identificador interno

- **Decisión**: `PortfolioStatusCatalogResponse(code, name, displayOrder, active, applicability)` y campo `portfolioStatuses` en el bundle de `GET /catalogs`; solo estados activos, ordenados por `displayOrder` asc y `code` asc.
- **Fundamento**: FR-008 y FR-009; replica el patrón de la feature 011 pero sin `id`, porque el código es la identidad contractual del estado.
- **Alternativas descartadas**: reutilizar `PersistentCatalogItemResponse` (exige `id`, contradice FR-004); reutilizar `TechnicalCatalogItemResponse` (no tiene aplicabilidad).

## D5: Referencia estructurada en cada respuesta de registro

- **Decisión**: `PortfolioStatusReferenceResponse(code, name, active)` como objeto `status` en todas las respuestas de registros (listado, detalle, mutaciones), dashboard, bandeja documental y auditoría. `name` es la denominación vigente del código; `active` distingue históricos inactivos.
- **Fundamento**: la aclaración del 2026-09-10 definió que la consulta central solo devuelve activos y que cada registro histórico incorpora por separado la metadata vigente. Una sola forma para estado normal e histórico simplifica los consumidores y satisface FR-010 y FR-018 sin insertar inactivos en el bundle.
- **Alternativas descartadas**: campo `String` más objeto opcional solo para inactivos: dos formas para un mismo concepto, con casts y ramas en la UI.

## D6: Filtros y `parseStatus` solo por código

- **Decisión**: `PortfolioApplicationSupport.parseStatus` acepta exclusivamente el código; se elimina la aceptación de etiquetas. Un código existente pero inactivo sigue siendo consultable por llamada directa (filtro de lectura); la UI solo ofrece activos.
- **Fundamento**: FR-003 y FR-011: la denominación no puede ser identidad ni vía de consulta. Permitir el filtro directo de inactivos habilita la lectura de históricos sin ofrecerlos como opciones.
- **Alternativas descartadas**: rechazar códigos inactivos en el filtro: impediría consultar históricos; aceptar etiquetas además de códigos: perpetúa la dependencia por denominación.

## D7: Validación de destino en orden existencia → actividad → aplicabilidad → matriz

- **Decisión**: creación (`PRESENTED` / `PROJECT_IN_PROGRESS`), aprobación (`INITIATIVE_APPROVED`) y transición (destino) resuelven el código en el catálogo y validan en ese orden. El estado de origen inactivo no se rechaza: puede abandonarse si la matriz autoriza y el destino es válido.
- **Fundamento**: FR-013, FR-014, FR-015 y FR-020; el orden produce causas distinguibles (por ejemplo, `NOT_APPLICABLE` activo se rechaza por aplicabilidad, no como transición no autorizada). El origen es un hecho histórico, no una nueva asignación.
- **Alternativas descartadas**: validar matriz primero: colapsaría las causas y violaría FR-015; validar también el origen: contradice FR-020.

## D8: Cuatro `ProblemCode` 422 y `targetStatus` como `String`

- **Decisión**: `ProblemCode` agrega `PORTFOLIO_STATUS_NOT_FOUND`, `PORTFOLIO_STATUS_INACTIVE`, `PORTFOLIO_STATUS_NOT_APPLICABLE` y `PORTFOLIO_STATUS_TRANSITION_NOT_ALLOWED` (HTTP 422). Los requests de transición reciben `targetStatus` como `String` con `allowableValues` de los once códigos (para typing generado); su ausencia o formato inválido sigue siendo 400 de validación.
- **Fundamento**: hoy un valor JSON desconocido falla como 400 de Jackson antes de llegar a la aplicación, impidiendo representar "estado inexistente" como causa funcional (FR-015). Con `String`, el código desconocido llega al servicio y produce un 422 distinguible.
- **Alternativas descartadas**: mantener el enum en el request: imposible distinguir inexistente de malformado; un único código de error genérico: viola FR-015.

## D9: Matrices fuera del catálogo

- **Decisión**: las matrices permanecen en el dominio (entidad/política controlada) exactamente como hoy; ninguna metadata del catálogo las crea, elimina o infiere.
- **Fundamento**: FR-021, principio II de la Constitución y regla del `AGENTS.md` raíz.
- **Alternativas descartadas**: derivar destinos de actividad+aplicabilidad: inventaría transiciones; tabla de transiciones administrable: fuera de alcance y requiere revisión constitucional explícita.

## D10: Elegibilidad con `INITIATIVE_APPROVED` inactivo

- **Decisión**: `eligibleInitiatives()` sigue seleccionando registros cuyo código actual es `INITIATIVE_APPROVED` aunque su metadata esté inactiva: se selecciona un registro histórico, no se reasigna ese estado. Al crear el proyecto derivado se valida el nuevo `PROJECT_IN_PROGRESS` (activo y aplicable) y la iniciativa permanece en `INITIATIVE_APPROVED`.
- **Fundamento**: coherente con FR-019 y FR-020: la inactividad de un estado no bloquea operaciones sobre el registro que no reasignan ese estado. La spec no exige bloquear la derivación por inactividad del estado de la iniciativa, y hacerlo cambiaría silenciosamente las reglas de derivación ratificadas en 009.
- **Alternativas descartadas**: excluir iniciativas cuyo estado esté inactivo: altera reglas vigentes sin FR que lo respalde y castiga a históricos.

## D11: Dashboard con identidad por código y referencias estructuradas

- **Decisión**: `GET /dashboard/portfolio`: `item.status` pasa a referencia estructurada; `statusCounts` pasa a lista de `{ status: referencia, count }` ordenada por `displayOrder` del catálogo, incluyendo estados inactivos con conteo. `GET /dashboard` legado: `portfolioByStatus` (map etiqueta→conteo) se sustituye por una lista estructurada equivalente.
- **Fundamento**: FR-010; el código backend actual usa etiquetas como claves del mapa (riesgo de fusión ante renombres con denominaciones iguales) e itera `values()` como inventario (viola FR-011).
- **Alternativas descartadas**: conservar el map por etiqueta: colisión de claves y sin soporte para inactivos; map por código con lookup en frontend: reintroduce inventario local.

## D12: Bandeja documental con referencia de estado

- **Decisión**: `DossierSummary.status` pasa de etiqueta a `PortfolioStatusReferenceResponse`.
- **Fundamento**: FR-010; la bandeja es un consumidor identificado en el grounding (backend y frontend).
- **Alternativas descartadas**: solo código con lookup en frontend: reintroduce dependencia del bundle para estados inactivos presentes.

## D13: Auditoría con códigos estables y enriquecimiento solo en lectura

- **Decisión**: los eventos nuevos de alta y transición persisten en `detailJson` códigos estables (`statusCode`, `previousStatusCode`, `newStatusCode`) junto al detalle técnico actual; `EventView` y la respuesta HTTP agregan referencias opcionales estructuradas `status`, `previousStatus`, `newStatus` con metadata vigente. Los eventos legados que solo contienen etiquetas se muestran con su texto histórico, sin intentar mapear etiqueta→código. La reconciliación de auditoría institucional queda para una feature posterior.
- **Fundamento**: FR-019 y el principio V (append-only: nunca reescribir eventos). FR-010 exige que la auditoría visible resuelva denominaciones vigentes. En `test,test-reset` la auditoría se descarta por completo, por lo que el entorno descartable no genera legado.
- **Alternativas descartadas**: reescribir eventos legados: viola el principio V; mapear etiqueta→código para legados: inventaría identidad, prohibido por FR-011.

## D14: Seed coordinado 20→21 tablas con MERGE insert-only

- **Decisión**: `ESTADO_PORTAFOLIO` entra a la allowlist y a los órdenes del reset (drop después de `REGISTRO_PORTAFOLIO`, create antes del registro); el conteo de tablas pasa de 20 a 21. En ambos artefactos (`apps/backend/src/main/resources/db/test/catalog-data.sql` y `database/dml/seed/catalog-data.sql`) se agregan once MERGE por código natural usando solo `WHEN NOT MATCHED INSERT` (sin rama UPDATE correctora); la postvalidación exige exactamente los once códigos con nombre/orden/actividad/aplicabilidad esperados y ausencia de extras; toda discrepancia produce fallo cerrado. La reejecución es idempotente.
- **Fundamento**: FR-022 a FR-026 y el principio IV (DML-only, idempotente, activación exacta y ordenada `test,test-reset`, fail-closed). El insert-only garantiza que una divergencia de datos se reporte como fallo en vez de corregirse silenciosamente (edge case de discrepancias).
- **Alternativas descartadas**: MERGE con UPDATE: corregiría en lugar de fallar; INSERT simple: no idempotente; seed en `dev`/`prod`: prohibido por Constitución y por FR-028.

## D15: Frontend por código con gate de catálogo ready

- **Decisión**: `PiipStatus` se redefine como unión de los once códigos; `CatalogBundle` agrega `portfolioStatuses` (opción con cinco atributos, sin `id`); registros, dashboard, documentos y auditoría usan referencias estructuradas; `piip.catalogs.ts` elimina `PIIP_CATALOGS.statuses`, `INITIATIVE_STATUSES` y `PROJECT_STATUSES`; las matrices `INITIATIVE_STATUS_TRANSITIONS` y `PROJECT_STATUS_TRANSITIONS` permanecen indexadas por código; el mapa `portfolioStatusCode` y los casts del repositorio HTTP desaparecen; las acciones de estado exigen `phase === 'ready'` del `PiipCatalogsStore` (con distinción de vacío, error y carga); los mapas visuales quedan indexados por código con presentación neutral para códigos desconocidos.
- **Fundamento**: FR-010 a FR-012; los consumidores exactos fueron identificados con rutas por el especialista frontend (formularios, filtros, listas, selectores, detalles, indicadores, dashboard, documentos, auditoría, permisos de edición, diálogos de transición).
- **Alternativas descartadas**: conservar la unión de etiquetas como identidad: dependencia por denominación prohibida; ofrecer opciones con el valor previo del store durante error: usaría datos stale.

## D16: Publicación backend primero

- **Decisión**: el contrato se publica primero en backend (código y OpenAPI) y luego se regenera el cliente Angular con `npm run api:generate`; mientras tanto, el contrato de diseño vive en `contracts/portfolio-statuses-api.md`.
- **Fundamento**: `docs/development/spec-kit-adoption.md` y el routing de especialistas del `AGENTS.md` raíz: cuando frontend y backend comparten contrato, primero el propietario canónico y después el consumidor.
- **Alternativas descartadas**: paralelizar el frontend contra un contrato provisional: produce drift y retrabajo.

## D17: DDL regenerado y preparación productiva incompleta

- **Decisión**: `database/generated/piip-oracle.sql` se regenera desde el artefacto del build (`apps/backend/target/piip-oracle.sql`), sin edición manual. `dev` y `prod` mantienen `ddl-auto=validate` sin seed; la validación estructural fallará hasta la provisión institucional, que requiere una feature posterior aprobada; la feature no declara producción completa (FR-029).
- **Fundamento**: FR-027 a FR-029 y el principio IV.
- **Alternativas descartadas**: DDL editado a mano: no canónico; auto-carga del catálogo en `dev`: prohibida; declarar readiness completo: contradice la spec.

## D18: Mock como modelo del contrato, nunca fallback

- **Decisión**: el mock (`piip-mock.repository.ts`) se alinea al contrato (bundle con estados, referencias estructuradas, matrices por código) y nunca se importa como fuente funcional productiva.
- **Fundamento**: FR-011; el mock es fixture de pruebas y desarrollo, no catálogo.
- **Alternativas descartadas**: mantener los inventarios y matrices duplicados actuales en el mock desincronizados del contrato: escondería incompatibilidades reales.

## Resumen de necesidades resueltas

Las tres necesidades señaladas por el análisis (presentación de auditoría legada, elegibilidad con estado inactivo, coordinación del segundo seed) quedaron resueltas por D13, D10 y D14 respectivamente, dentro del alcance de la spec y sin `NEEDS CLARIFICATION` remanentes.
