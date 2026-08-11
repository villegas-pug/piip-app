# Contrato HTTP: Administración integral de usuarios

## Convenciones

- Prefijo: `/api/v1/admin`.
- Actor requerido: Administrador PIIP con cobertura local persistida.
- Las respuestas de error usan el formato `ProblemDetail` existente: 403 para ámbito no autorizado, 404 para recurso ausente, 409 para versión desactualizada y 422 para reglas de negocio o validación.
- Todas las mutaciones requieren versión esperada y generan auditoría funcional sin tokens, cuerpos HTTP ni contenido documental.

## Operaciones

| Método y ruta | Solicitud | Respuesta correcta | Semántica |
|---------------|-----------|--------------------|-----------|
| `GET /users` | Sin cuerpo | `200 UserResponse[]` | Devuelve únicamente usuarios y asignaciones administrables por el actor, incluidos los estados de asignación necesarios para operar. |
| `GET /users/assignment-candidates` | Sin cuerpo | `200 UserAssignmentCandidateResponse[]` | Devuelve usuarios locales sin ninguna asignación previa; alimenta exclusivamente el combo de primera asignación y no altera el listado administrable. |
| `POST /role-assignments` | `RoleAssignmentRequest { userSubject, role, institutionId, executingUnitId? }` | `201 ScopeResponse` | Crea una asignación activa si no existe duplicado y el ámbito es autorizable. |
| `PUT /role-assignments/{scopeId}?version={scopeVersion}` | `RoleAssignmentUpdateRequest { role, institutionId, executingUnitId? }` | `200 ScopeResponse` | Actualiza la misma asignación, conservando su identificador y registrando antes/después. |
| `DELETE /role-assignments/{scopeId}?version={scopeVersion}` | Sin cuerpo | `204` | Suspende de forma reversible la asignación, sin borrarla. |
| `PUT /role-assignments/{scopeId}/reactivation?version={scopeVersion}` | Sin cuerpo | `200 ScopeResponse` | Reactiva la misma asignación suspendida cuando sus catálogos y cobertura siguen siendo válidos. |

## Identidad funcional

`GET /api/v1/identity/me` añade de forma compatible `roleScopes[]`. Cada elemento corresponde a una única asignación activa y vigente; no se combinan roles y ámbitos de elementos diferentes.

```text
RoleScopeResponse
  role: ADMINISTRADOR_PIIP | CONSULTA_EXTERNA
  institutionId: number
  executingUnitId?: number | null

CurrentUserResponse
  subject, fullName, email
  roleScopes: RoleScopeResponse[]
  roles, institutionIds, executingUnitIds, institutionWide  # compatibilidad temporal
```

Una operación que recibe o resuelve una Unidad Ejecutora y exige Administrador PIIP responde `403` si no existe un `roleScope` Administrador que cubra esa misma Unidad, aunque el actor tenga Administrador en otro ámbito y cualquier otro rol en la Unidad solicitada.

## DTOs publicados

```text
RoleAssignmentRequest
  userSubject: string
  role: ADMINISTRADOR_PIIP | CONSULTA_EXTERNA
  institutionId: number
  executingUnitId?: number | null

RoleAssignmentUpdateRequest
  role: ADMINISTRADOR_PIIP | CONSULTA_EXTERNA
  institutionId: number
  executingUnitId?: number | null

UserAssignmentCandidateResponse
  id, subject, fullName, email

UserResponse
  id, subject, fullName, email, scopes[]

ScopeResponse
  id, role, institutionId, institution, executingUnitId?, executingUnit,
  active, validFrom, validUntil, version
```

El backend es la fuente de estas definiciones. Los archivos de `apps/frontend/src/app/api/generated/` se actualizan exclusivamente mediante el proceso autorizado de generación posterior a la publicación OpenAPI.
