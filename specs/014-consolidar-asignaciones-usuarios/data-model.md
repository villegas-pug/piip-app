# Modelo de datos: Consolidación de asignaciones de usuarios

## Decisión estructural

La feature conserva íntegramente `USUARIO`, `ROL`, `INSTITUCION`, `UNIDAD_EJECUTORA` y `USUARIO_ROL_AMBITO`. El único cambio persistente es una columna nullable de motivo seguro en `AUDITORIA_ACCESO`, derivada de JPA.

```text
USUARIO 1 ─── * USUARIO_ROL_AMBITO * ─── 1 ROL
                     │
                     ├── * ─── 1 INSTITUCION
                     ├── * ─── 0..1 UNIDAD_EJECUTORA
                     └── @Version VERSION

USUARIO 0..1 ─── * AUDITORIA_ACCESO
USUARIO 0..1 ─── * EVENTO_AUDITORIA
```

No se agregan tablas, secuencias, índices, constraints, triggers, procedimientos ni un segundo mecanismo de versión.

## `UserRoleScopeEntity` / `USUARIO_ROL_AMBITO`

| Campo | Uso en la feature | Cambio estructural |
|-------|-------------------|--------------------|
| `ID_USUARIO_ROL_AMBITO` | Identidad conservada al editar, suspender, reactivar o auto-reactivar. | Ninguno. |
| `ID_USUARIO` | Destinatario de la asignación y punto de serialización de `POST`. | Ninguno. |
| `ID_ROL` | Campo editable; debe referir a un rol PIIP activo. | Ninguno. |
| `ID_INSTITUCION` | Campo editable y límite administrativo. | Ninguno. |
| `ID_UNIDAD_EJECUTORA` | Campo editable nullable; nulo significa alcance institucional. | Ninguno. |
| `ACTIVO` | `true` para asignación vigente; `false` para suspendida. | Ninguno. |
| `VIGENTE_DESDE` | Se renueva al reactivar. | Ninguno. |
| `VIGENTE_HASTA` | Marca la suspensión y ordena la coincidencia histórica más reciente. | Ninguno. |
| `ASIGNADO_POR`, `FECHA_ASIGNACION` | Evidencia original; no se reemplaza al editar o reactivar. | Ninguno. |
| `VERSION` | Control optimista de edición, suspensión y reactivación explícita. | Ninguno. |

### Identidad exacta y unicidad activa

La combinación lógica es:

```text
(ID_USUARIO, ID_ROL, ID_INSTITUCION, ID_UNIDAD_EJECUTORA nullable)
```

- Dos alcances son iguales cuando los cuatro componentes coinciden y ambos `ID_UNIDAD_EJECUTORA` son iguales o nulos.
- Puede existir historia suspendida repetida por datos preexistentes.
- Solo una combinación exacta puede estar activa y vigente después de una mutación.
- La garantía se aplica en application bajo locks JPA; no se introduce un constraint que trate incorrectamente el nulo institucional.

### Selección de historia suspendida

```text
coincidencias exactas con ACTIVO = false y VIGENTE_HASTA != null
    ordenar por VIGENTE_HASTA DESC, ID_USUARIO_ROL_AMBITO DESC
    seleccionar como máximo la primera
```

El segundo orden es únicamente un desempate determinista. La fila seleccionada conserva su ID y avanza su `VERSION` mediante JPA al reactivarse.

## Estados y transiciones

| Estado actual | Operación | Estado resultante | Efecto persistente |
|---------------|-----------|-------------------|---------------------|
| No existe coincidencia activa ni suspendida | Asignar | Activa nueva | Inserta una fila y devuelve `201`. |
| Existe coincidencia suspendida exacta y ninguna activa | Asignar | Activa existente | Reactiva la más recientemente suspendida y devuelve `200`. |
| Existe coincidencia activa exacta | Asignar | Sin cambio | `422 ACTIVE_ASSIGNMENT_DUPLICATE`. |
| Activa | Editar | Activa | Conserva ID; cambia solo rol, institución o UE; valida `VERSION`. |
| Activa | Suspender | Suspendida | `ACTIVO=false`, fija `VIGENTE_HASTA`; conserva fila. |
| Suspendida | Reactivar | Activa | `ACTIVO=true`, renueva `VIGENTE_DESDE`, limpia `VIGENTE_HASTA`. |
| Suspendida | Editar o suspender | Sin cambio | `422 INCOMPATIBLE_ASSIGNMENT_STATE`. |
| Activa | Reactivar | Sin cambio | `422 INCOMPATIBLE_ASSIGNMENT_STATE`. |

No existe transición a eliminación física ni efecto sobre `USUARIO.ACTIVO` o Keycloak.

## Proyección de cobertura administrativa

La protección `LAST_ACTIVE_ADMIN` se evalúa sobre cada UE activa afectada:

```text
scope origen con UE ──> {esa UE}
scope origen institucional ──> {todas las UEs activas de la institución}

para cada UE:
  cobertura posterior = admins activos actuales
                        - scope mutado
                        + scope mutado si su destino aún cubre la UE
  cobertura posterior vacía ──> 422 LAST_ACTIVE_ADMIN
```

Una asignación institucional administradora cubre cada UE activa de su institución. Una asignación específica cubre solo su UE. Múltiples administradores activos por UE continúan permitidos.

## Modelo lógico de aplicación

### Commands

```text
AssignRoleScopeCommand
├── userSubject
├── role
├── institutionId
└── executingUnitId?

UpdateRoleScopeCommand
├── role
├── institutionId
└── executingUnitId?
```

Los path params `scopeId` y `expectedVersion` permanecen argumentos separados del caso de uso para mutaciones de una fila existente.

### Resultado de asignación

```text
AssignmentMutationResult
├── outcome: CREATED | REACTIVATED
└── scope: RoleScopeReadModel
```

`outcome` decide el estado HTTP y no se persiste.

### Snapshot funcional

```text
RoleScopeAuditSnapshot
├── assignmentId
├── userSubject
├── role
├── institutionId
├── executingUnitId?
├── active
├── validFrom
└── validUntil?
```

El evento incluye `before` y/o `after` según la operación, más `action` y `result = SUCCESS`. La fecha del evento continúa en `AuditEventEntity.occurredAt`.

## `AccessAuditEntity` / `AUDITORIA_ACCESO`

### Cambio JPA

| Campo Java | Columna Oracle derivada | Tipo / nulabilidad | Regla |
|------------|-------------------------|-------------------|-------|
| `safeReason` | `MOTIVO_SEGURO` | `varchar2(80 char)`, nullable | Código estable `problemCode` o categoría segura de resultado; nunca `detail`, body, token o credencial. |

Las filas históricas permanecen válidas con `MOTIVO_SEGURO = null`. No se requiere índice porque el requisito es evidenciar y presentar el motivo junto con un acceso, no filtrar por él.

### Flujo del motivo seguro

```text
excepción controlada
  └── ApiExceptionHandler crea ProblemDetail.problemCode
        └── request attribute = problemCode
              └── AccessAuditFilter finally
                    └── AuditService.access(..., safeReason)
                          └── AUDITORIA_ACCESO.MOTIVO_SEGURO
```

Cuando no existe un código específico, el filtro usa una categoría estable asociada al estado; no intenta leer o parsear la respuesta.

## `ProblemDetail`

El schema transversal conserva `type`, `title`, `status`, `detail`, `instance` y extensiones existentes. Añade:

```text
problemCode: string requerido en respuestas de error controladas
```

Valores de contrato de esta feature:

| HTTP | `problemCode` |
|------|---------------|
| 400 | `INVALID_REQUEST` |
| 403 | `FORBIDDEN_SCOPE` |
| 404 | `RESOURCE_NOT_FOUND` |
| 409 | `STALE_VERSION` |
| 422 | `ACTIVE_ASSIGNMENT_DUPLICATE` |
| 422 | `SELF_ADMIN_SUSPENSION` |
| 422 | `LAST_ACTIVE_ADMIN` |
| 422 | `INCOMPATIBLE_ASSIGNMENT_STATE` |
| 422 | `INVALID_ACTIVE_REFERENCE` |
| 422 | `BUSINESS_RULE_VIOLATION` como fallback compatible |

## Locks y concurrencia

Orden global propuesto:

```text
1. usuarios involucrados por ID ascendente
2. grants vigentes del actor
3. asignación objetivo o coincidencias exactas
4. UEs afectadas por ID ascendente
5. scopes administradores por ID ascendente
6. validar versión/reglas y mutar
7. flush/evento funcional dentro de la misma transacción
```

Escenarios que debe serializar:

- dos `POST` para la misma combinación sin fila activa;
- `POST` frente a reactivación explícita de la misma coincidencia;
- dos suspensiones/ediciones que retirarían la cobertura de una misma UE;
- revocación del último grant del actor mientras envía otra mutación;
- edición de un grant institucional que afecta varias UEs.

## DDL derivado

`database/generated/piip-oracle.sql` debe regenerarse desde el modelo JPA cuando exista autorización explícita. El cambio lógico esperado es solo `AUDITORIA_ACCESO.MOTIVO_SEGURO`; no se escribe una migración manual ni se accede a Oracle durante esta fase.
