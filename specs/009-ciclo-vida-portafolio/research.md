# Investigación técnica: Ciclo de vida del portafolio PIIP

## Alcance de la investigación

Se contrastó la especificación con controladores, DTO, servicio de aplicación, entidad/repositorio JPA, auditoría, rutas y repositorio Angular, generación OpenAPI, guía funcional y constitución. Graphify se utilizó solo como índice de orientación; cada decisión se validó en fuentes canónicas del repositorio.

## Decisión 1: rutas separadas por contexto y aprobación intacta

- **Decisión**: agregar `POST /api/v1/initiatives/{code}/status-transitions` y `POST /api/v1/projects/{code}/status-transitions`, con requests separados; conservar `POST /api/v1/initiatives/{code}/approval` sin cambios.
- **Fundamento**: `PortfolioController` ya organiza rutas por `/initiatives/**` y `/projects/**`. Separar contratos impide sugerir que ambos contextos aceptan el mismo catálogo y evita romper tareas, notificaciones y auditoría de la aprobación vigente.
- **Alternativas descartadas**: endpoint genérico `/portfolio/{code}/status`, reutilizar `/approval` o usar `PATCH` directo; todos mezclan contextos o degradan una decisión de dominio a edición de campo.

## Decisión 2: matrices explícitas en el dominio, sin persistencia adicional

- **Decisión**: expresar reglas separadas de iniciativa y proyecto en comportamientos de `PortfolioRecordEntity`; `PortfolioService` orquesta y la entidad valida la matriz antes de mutar.
- **Fundamento**: la matriz es pequeña y definida para v1. El repositorio ya trata la entidad como autoridad de estado mediante `approve(...)`.
- **Alternativas descartadas**: setter genérico, tabla de transiciones o reglas solo en frontend; permitirían combinaciones inválidas, añadirían sobreingeniería o no serían autoritativas.

## Decisión 3: versión optimista más bloqueo localizado de la iniciativa

- **Decisión**: conservar `@Version VERSION` y la versión esperada; agregar lectura JPA `PESSIMISTIC_WRITE` de la iniciativa en transición y creación derivada.
- **Fundamento**: la versión evita sobrescrituras. El bloqueo serializa la carrera archivar-versus-derivar, que decide sobre el mismo estado y relación. `existsByOriginRecordId(...)` se revalida dentro de la transacción.
- **Alternativas descartadas**: solo `@Version`, bloqueo global o SQL nativo; no cubren la carrera completa, penalizan lecturas o contradicen la arquitectura.

## Decisión 4: fuente temporal inyectable en `America/Lima`

- **Decisión**: declarar un bean `Clock` con zona `America/Lima`, inyectarlo en `PortfolioService` y pasar `Instant`/`LocalDate` al dominio.
- **Fundamento**: la fecha de cierre no debe depender de la zona del host ni de un valor del cliente. La inyección hace deterministas las pruebas de borde de día.
- **Alternativas descartadas**: `LocalDate.now()` directo, fecha en request o trigger Oracle.

## Decisión 5: auditoría funcional existente y atómica

- **Decisión**: llamar `AuditService.event(...)` en la transacción de `PortfolioService` y crear `ESTADO_INICIATIVA_CAMBIADO` y `ESTADO_PROYECTO_CAMBIADO`; conservar `INICIATIVA_APROBADA`.
- **Fundamento**: `AuditEventEntity` ya admite detalle JSON append-only. Actor y fecha ya son columnas; estado, versión, cierre y evento se confirman o revierten juntos.
- **Alternativas descartadas**: nueva tabla de historial, auditoría `REQUIRES_NEW` o copiar observación a `Nota`.

## Decisión 6: backend propietario del contrato; Angular consumidor posterior

- **Decisión**: estabilizar controlador/DTO/OpenAPI antes de regenerar `apps/frontend/src/app/api/generated/**`; luego adaptar repositorio y UI.
- **Fundamento**: `ng-openapi-gen.json` consume el OpenAPI generado por `OpenApiGenerationTest`.
- **Alternativas descartadas**: editar código generado, implementar ambos lados contra un contrato transitorio o introducir un cliente HTTP paralelo.

## Decisión 7: detalle contextual de proyecto y listas separadas

- **Decisión**: crear `ProjectDetailComponent`, conservar el detalle de iniciativa, mover la navegación principal del proyecto al detalle y alimentar filtros/selectores con listas explícitas por tipo.
- **Fundamento**: la aplicación ya concentra aprobación en detalle; el proyecto solo posee listado y expediente. Las listas contextuales previenen mezcla accidental.
- **Alternativas descartadas**: selector en listado, catálogo global sin filtrar o convertir el expediente documental en detalle general.

## Decisión 8: documentos informativos, nunca bloqueantes en v1

- **Decisión**: no consultar ni validar documentos como precondición. Si la UI los muestra, será una advertencia que no deshabilita la confirmación.
- **Fundamento**: `FR-023` define v1; la relación documento-transición queda para una versión posterior.
- **Alternativas descartadas**: bloqueo preventivo genérico, porque inventaría reglas.

## Resultado

No quedan incógnitas técnicas bloqueantes. Permanece un `NEEDS CLARIFICATION` futuro sobre qué documentos podrían bloquear transiciones posteriores; no altera v1.
