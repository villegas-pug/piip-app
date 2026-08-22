# Contrato de compatibilidad HTTP

## Regla general

Esta feature no crea una versión nueva del API. Los métodos, rutas, parámetros, bindings, validaciones, status, headers, JSON, nulabilidad, orden y `ProblemDetail` del checkout actual permanecen como contrato. Este documento es una matriz de congelamiento, no un OpenAPI alternativo.

## Endpoints afectados internamente

| Módulo | Métodos y rutas preservados | Respuesta/efecto observable preservado |
|--------|-----------------------------|----------------------------------------|
| Identity | `GET /identity/me` | Mismo usuario, `roleScopes` ordenados, roles, instituciones, UE, `institutionWide` y registro de última autenticación. |
| Organization | `GET /institutions`; `GET /executing-units`; `GET /organizational-units?executingUnitId` | Mismos campos, filtros por grants, orden actual y rechazo fuera de ámbito. |
| Audit | `GET /audit/accesses[?executingUnitId]`; `GET /audit/events[?executingUnitId]` | Máximo 100, mismo orden y campos; actor nombre/email nullable; filtro de UE equivalente. |
| Work | `GET /work-tasks`; `PUT /work-tasks/{taskId}/complete?version`; `PUT /work-tasks/{taskId}/assignee` | Misma lista/alerta/versión; completar conserva 204; reasignar conserva payload y autorización exacta. |
| Notifications | `GET /notifications`; `PUT /notifications/{id}/read` | Mismo destinatario, orden y JSON; lectura conserva 204 y 404 para notificación ajena/inexistente. |
| Dashboard | `GET /dashboard` | Mismos conteos y `portfolioByStatus`; `GET /dashboard/portfolio` permanece sin cambios internos ni contractuales. |
| Documents | `GET /documents[?executingUnitId]`; todas las rutas bajo `/portfolio-records/{recordCode}/documents` | Mismos dossiers, responsables, slots/versiones, 201/204, headers de descarga, contenido, autorización, auditoría y notificaciones. |
| Portfolio | Rutas actuales `/initiatives/**` y `/projects/**` | Mismos filtros, paginación, creaciones, aprobación, transiciones, DTO, cinco campos heredados nulos y efectos coordinados. |

## DTO y binding

- No se renombran records ni propiedades externas.
- No se cambia tipo, nulabilidad ni estructura JSON.
- Los DTO anidados de API pueden reubicarse o mapearse internamente solo si la forma serializada permanece idéntica.
- `MultipartFile` continúa siendo el binding de `@RequestPart("file")` en API; application recibe `DocumentUploadInput` con lectura diferida para conservar el orden de validación y error.
- `recordCode`, `documentTypeId`, `versionId`, `taskId`, `id`, `executingUnitId`, `q`, `status`, `page`, `size`, `published` y `version` conservan nombres y ubicación actuales.
- No se regenera ni edita el OpenAPI o el cliente Angular.

## Errores observables

| Caso | Status | Título | Propiedades adicionales |
|------|--------|--------|-------------------------|
| Validación Bean Validation | 400 | `Validación` | Detalle del primer campo conforme al handler actual. |
| Acceso denegado | 403 | `Acceso denegado` | Sin propiedades nuevas. |
| Recurso inexistente | 404 | `Recurso no encontrado` | Sin propiedades nuevas. |
| Versión obsoleta | 409 | `Conflicto de versión` | Detalle fijo vigente. |
| Regla funcional | 422 | `Regla de negocio` | Sin propiedades nuevas. |
| Referencia inválida | 422 | `Referencia inválida` | `referenceField`, `referenceId`, `reason`. |

El `type` conserva `https://piip.midagri.gob.pe/problems/{status}`. Los mensajes concretos se caracterizan por caso antes de mover código.

## Valores variables

La comparación es semántica:

- valores deterministas: igualdad exacta;
- timestamps: misma fuente temporal, precisión, orden y momento funcional;
- correlaciones: misma regla de generación y propagación;
- versiones: mismo incremento y rechazo concurrente;
- códigos: mismo generador, tipo, año y secuencia para un estado de partida equivalente;
- colecciones/mapas: mismo criterio de filtrado y orden.

Solo se normalizan valores legítimamente variables. No se normalizan decisiones de autorización, status, campos, mensajes ni efectos persistentes.

## Contratos internos de frontera

1. API no recibe ni devuelve entidades JPA.
2. Application no devuelve entidades JPA hacia API.
3. Controllers no poseen `@Transactional`, repositorios, entidades, auditoría funcional, versión ni reconstrucción de grants.
4. Modelos compartidos no se definen dentro de controllers.
5. Los errores funcionales internos no dependen de Spring MVC; la traducción a `ProblemDetail` pertenece a `shared/api`.
6. `LocalAuthorizationService` conserva Spring Security y `AccessDeniedException` según la aclaración aprobada.
7. `PortfolioWorkService` y `PortfolioDocumentService` se unen a la transacción de portfolio y no aceptan/devuelven entidades como contrato entre módulos.

## Criterio de incompatibilidad

Cualquier diferencia no explicada por un valor legítimamente variable bloquea el incremento y obliga a revertirlo o corregirlo antes de avanzar. Una necesidad real de cambiar el contrato requiere una feature independiente y no puede incorporarse a esta refactorización.
