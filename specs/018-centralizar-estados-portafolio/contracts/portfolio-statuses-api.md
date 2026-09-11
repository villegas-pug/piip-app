# Contrato HTTP: estados del portafolio (diseño)

**Rama**: `018-centralizar-estados-portafolio` | **Fecha**: 2026-09-10 | **Spec**: [spec.md](./spec.md)

**Estado**: diseño aprobado en este plan. La publicación real (código backend, `piip-openapi.json` y cliente Angular generado) ocurre durante la implementación autorizada; este documento es la fuente previa de diseño del contrato, consistente con el formato markdown usado en la feature 016.

**Rutas sin cambios**; los esquemas cambian (cambio rompiente documentado). La autenticación Keycloak y la autorización funcional Oracle vigentes no cambian. Ningún endpoint de administración del catálogo de estados se agrega.

## Esquemas nuevos

### `PortfolioStatusCatalogResponse` — entrada del bundle central (solo activos)

```json
{
  "code": "SUSPENDED",
  "name": "Suspendido",
  "displayOrder": 7,
  "active": true,
  "applicability": "PROJECT"
}
```

- `code`: enum de los once códigos oficiales (`PRESENTED`, `INITIATIVE_APPROVED`, `INITIATIVE_ARCHIVED`, `PROJECT_IN_PROGRESS`, `PRODUCT_APPROVED`, `PRODUCT_NOT_APPROVED`, `SUSPENDED`, `CANCELLED`, `FINISHED`, `NOT_APPLICABLE`, `NOT_ADMISSIBLE`).
- `name`: denominación vigente.
- `displayOrder`: entero ≥ 0.
- `active`: siempre `true` en el bundle (solo activos).
- `applicability`: `INITIATIVE` | `PROJECT` | `NONE`.
- Sin identificador interno: el código es la identidad contractual.

### `PortfolioStatusReferenceResponse` — estado de un registro

```json
{ "code": "SUSPENDED", "name": "Suspendido", "active": false }
```

- `name`: denominación vigente del código.
- `active = false`: el registro referencia un estado actualmente inactivo (histórico legible, no seleccionable).

## Endpoints afectados

| Endpoint | Cambio |
|----------|--------|
| `GET /catalogs` | Agrega `portfolioStatuses: PortfolioStatusCatalogResponse[]` (solo activos, orden `displayOrder` asc y `code` asc). Campos existentes sin cambios. |
| Listados, detalle y mutaciones de iniciativas/proyectos | `status` pasa de `String` (etiqueta) a objeto `PortfolioStatusReferenceResponse`. |
| `GET /dashboard/portfolio` | `item.status` pasa a referencia; `PortfolioStatusCountResponse` pasa a `{ "status": referencia, "count": número }`; `statusCounts` se ordena por `displayOrder` del catálogo e incluye estados inactivos con conteo. |
| `GET /dashboard` | `portfolioByStatus` (map etiqueta→conteo) se sustituye por `portfolioStatusCounts: [{ "status": referencia, "count": número }]`. |
| `GET /documents` (bandeja) | `DossierSummary.status` pasa a referencia. |
| `GET /audit/events` | `EventResponse` agrega `status?`, `previousStatus?`, `newStatus?` (referencias opcionales con metadata vigente). `detail` se conserva como detalle técnico. |
| Requests de transición de iniciativa/proyecto | `targetStatus` pasa de enum a `String` con `allowableValues` de los once códigos; ausencia o formato inválido sigue siendo 400 de validación. |
| Filtros `status` (listados, dashboard) | Aceptan exclusivamente el código; un código existente inactivo sigue consultable por llamada directa. |

## Errores funcionales (422)

| `ProblemCode` | Causa |
|---------------|-------|
| `PORTFOLIO_STATUS_NOT_FOUND` | El código no existe en el catálogo persistente. |
| `PORTFOLIO_STATUS_INACTIVE` | El destino está inactivo. |
| `PORTFOLIO_STATUS_NOT_APPLICABLE` | La aplicabilidad no coincide con el tipo de registro (incluye `NOT_APPLICABLE`/`NONE`). |
| `PORTFOLIO_STATUS_TRANSITION_NOT_ALLOWED` | El par origen-destino no está autorizado por la matriz vigente. |

Orden de evaluación del destino: existencia → actividad → aplicabilidad → matriz (FR-015). El estado de origen inactivo no se rechaza (FR-020).

## Compatibilidad y publicación

- **Cambio rompiente**: `status` textual → objeto referencia; map de conteos → lista estructurada; `targetStatus` enum → `String`.
- **Secuencia obligatoria**: backend canónico (código + DTO + pruebas) → `piip-openapi.json` (vía `OpenApiGenerationTest`) → `npm run api:generate` en `apps/frontend` → adaptación de consumidores frontend. El contrato backend se publica antes de sincronizar el cliente (protocolo de `docs/development/spec-kit-adoption.md`).
- **Sin cambios**: rutas, matrices de transición, autenticación/autorización, y el resto de los catálogos del bundle.
