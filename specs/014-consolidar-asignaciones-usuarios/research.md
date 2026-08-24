# Investigación: Consolidación de asignaciones de usuarios

## 1. Frontera backend y adaptación HTTP

**Decisión**: `UserAdministrationService` recibirá commands y devolverá read models/resultados de `identity.application`. `UserAdministrationHttpMapper` adaptará `AdminDtos` en `identity.api`.

**Razón**: el servicio actual importa `identity.api.AdminDtos`, lo que invierte la dependencia esperada después del refactor. Separar las formas evita exponer DTO HTTP o entidades JPA como contrato de aplicación.

**Alternativas consideradas**:

- Mantener los DTO en el servicio: preserva deuda `application -> api` en un área que la feature modifica materialmente.
- Mover reglas al controller: rompería el controller delgado y la frontera transaccional.
- Crear un módulo nuevo: no aporta cohesión; el propietario vigente es `identity`.

## 2. Resultado dual de `POST /role-assignments`

**Decisión**: el caso de uso devuelve `AssignmentMutationResult` con `outcome = CREATED | REACTIVATED` y el scope confirmado. El controller responde `201` para `CREATED` y `200` para `REACTIVATED`, ambos con `ScopeResponse` JSON.

**Razón**: el estado HTTP es parte de la aclaración aprobada y no debe inferirse en el controller comparando datos. Un resultado explícito mantiene la decisión funcional dentro de application.

**Alternativas consideradas**:

- Siempre `201`: ocultaría la reactivación y contradice FR-037.
- Agregar un campo `outcome` a `ScopeResponse`: duplicaría la semántica ya expresada por el estado y alteraría el recurso sin necesidad.
- Devolver `204` al reactivar: impediría reconciliar la fila confirmada.

## 3. Selección de una coincidencia suspendida

**Decisión**: bajo lock del usuario objetivo, buscar primero duplicados activos; si no existen, bloquear coincidencias exactas suspendidas y ordenar `VIGENTE_HASTA DESC, ID_USUARIO_ROL_AMBITO DESC`. Reactivar solo la primera; crear únicamente cuando la lista está vacía.

**Razón**: `VIGENTE_HASTA` representa el momento de suspensión y el ID desempata timestamps iguales de forma determinista. El lock del usuario serializa dos `POST` aun cuando todavía no exista una fila para bloquear; el lock de las suspendidas protege la identidad elegida.

**Alternativas consideradas**:

- Ordenar solo por ID: no representa necesariamente la suspensión más reciente.
- Crear otra fila: contradice FR-012 y multiplica historia equivalente.
- Confiar solo en `@Version`: `POST` no recibe versión y el caso “ninguna fila activa” admite phantoms.

## 4. Revalidación persistida y orden de locks

**Decisión**: bloquear actor y destinatario por IDs ascendentes, releer los grants activos del actor bajo lock, validar cobertura y luego bloquear UEs/scope administradores en orden ascendente.

**Razón**: `currentAdministrator()` ya relee persistencia, pero sin lock existe una ventana entre autorización y mutación. Un orden global reduce deadlocks cuando actor y destinatario coinciden o dos operaciones afectan la misma cobertura.

**Alternativas consideradas**:

- Confiar en el JWT o snapshot del navegador: no observa revocaciones posteriores.
- Revalidar sin lock: conserva una carrera con otra suspensión/edición.
- Bloquear en el orden de llegada: aumenta riesgo de deadlock.

## 5. Cobertura mínima por Unidad Ejecutora

**Decisión**: calcular el conjunto de UEs afectadas por el scope administrador de origen. Para una asignación específica es una UE; para una institucional son todas las UEs activas de la institución. Para cada UE se simula el estado posterior, excluyendo el scope mutado e incluyéndolo solo si el destino aún la cubre.

**Razón**: una asignación institucional cubre varias UEs; contar un único “ámbito” no demuestra que todas conserven administrador después de editar o suspender.

**Alternativas consideradas**:

- `size() <= 1` sobre una sola consulta: falla al mover un grant institucional o conservar solo parte de su cobertura.
- Prohibir toda edición institucional: inventaría una restricción no aprobada.
- Constraint Oracle: la regla depende de vigencia, rol y múltiples filas; corresponde a application con repositorios JPA.

## 6. Autosuspensión administrativa

**Decisión**: antes de evaluar cobertura, rechazar con `SELF_ADMIN_SUSPENSION` si el scope pertenece al actor y su rol es `ADMINISTRADOR_PIIP`. La asignación propia `CONSULTA_EXTERNA` sigue el flujo normal y exige que la revalidación encuentre otro grant administrador vigente.

**Razón**: la prohibición es incondicional para el scope administrador propio, aunque existan reemplazos. Separarla de “último administrador” evita que el número de administradores cambie el resultado.

**Alternativas consideradas**:

- Reutilizar `LAST_ACTIVE_ADMIN`: no cubre el caso con varios administradores.
- Prohibir toda autosuspensión: contradice FR-016.
- Solo ocultar el botón: no protege llamadas directas.

## 7. Discriminador transversal de errores

**Decisión**: publicar la extensión `ProblemDetail.problemCode` y usar códigos estables: `INVALID_REQUEST`, `FORBIDDEN_SCOPE`, `RESOURCE_NOT_FOUND`, `STALE_VERSION`, `ACTIVE_ASSIGNMENT_DUPLICATE`, `SELF_ADMIN_SUSPENSION`, `LAST_ACTIVE_ADMIN`, `INCOMPATIBLE_ASSIGNMENT_STATE`, `INVALID_ACTIVE_REFERENCE` y `BUSINESS_RULE_VIOLATION` como fallback para reglas fuera de la matriz.

**Razón**: Angular necesita distinguir reglas que comparten `422` sin analizar texto traducible. El mismo código es seguro para auditoría HTTP y OpenAPI puede documentarlo.

**Alternativas consideradas**:

- Analizar `detail`: acopla el cliente a textos y traducciones.
- Usar solo `type` por estado: no diferencia reglas con el mismo HTTP.
- Introducir un envelope distinto: rompería la convención `application/problem+json`.

## 8. Motivo seguro en auditoría de acceso

**Decisión**: añadir `AccessAuditEntity.safeReason` mapeado a `MOTIVO_SEGURO varchar2(80 char)` nullable. `ApiExceptionHandler` coloca `problemCode` en un atributo de request; `AccessAuditFilter` lo lee al finalizar y usa un código general por estado si no fue colocado. El dato se propaga a `AccessView` y `AccessResponse`.

**Razón**: la tabla actual no tiene un campo que cumpla “estado y motivo seguro”. Persistir el código contractual satisface observabilidad sin capturar `detail`, body, token ni credencial. Nullable mantiene compatibilidad histórica.

**Alternativas consideradas**:

- Guardar `detail` en `CODIGO_REGISTRO`: mezcla conceptos y puede exponer texto libre.
- Parsear el body de respuesta en el filtro: introduce buffering y captura prohibida.
- Solo log de aplicación: no satisface la evidencia persistida de auditoría de acceso.

## 9. Auditoría funcional atómica

**Decisión**: usar `tipoEntidad = USUARIO_ROL_AMBITO`, `codigoEntidad = scope.id` y un detalle estructurado con `actor`, `action`, `assignmentId`, usuario afectado, `before`, `after` y `result = SUCCESS`. Cada snapshot contiene rol, institución, UE opcional y estado/vigencia aplicable. El evento participa en la misma transacción.

**Razón**: la asignación, no el subject del usuario, es la entidad mutada. Snapshots completos permiten demostrar el cambio y una falla de serialización/persistencia debe revertir la mutación.

**Alternativas consideradas**:

- Conservar el subject como código de entidad: no identifica cuál de varias asignaciones cambió.
- Auditar después del commit: permite cambios sin evento.
- Crear eventos de fallo: contradice la aclaración que reserva rechazos a auditoría HTTP.

## 10. Repositorio Angular y respuesta completa

**Decisión**: exponer en `PiipRepository` las cuatro operaciones de escritura. `PiipHttpRepository` será el único consumidor del cliente generado y `assign$Response()`; validará `200/201` y cuerpo. El componente no inyectará `UserAdministrationControllerService`.

**Razón**: hoy la pantalla salta la abstracción de datos, dificulta mocks y mezcla semántica HTTP con presentación. El repositorio puede devolver un resultado tipado y reconciliar modelos de presentación.

**Alternativas consideradas**:

- Mantener llamadas directas: perpetúa dos rutas de acceso a datos.
- Editar el cliente generado: se perdería en la siguiente generación.
- Inferir auto-reactivación por el estado previo de la lista: falla con concurrencia.

## 11. Mutación propia y recuperación fail-closed

**Decisión**: determinar propiedad por subject, capturar la UE activa original antes de enviar y, tras cualquier mutación propia exitosa, ejecutar `refreshAuthorizationContext()` antes de habilitar otra acción. Si continúa autorizado sobre la UE original, recargar datos; si perdió acceso, limpiar y navegar a `/inicio`. Si el refresco falla, limpiar usuarios/candidatos/ámbitos, navegar a `/inicio`, mostrar aviso y acción `Reintentar`; no recargar Administración hasta que el retry termine correctamente.

**Razón**: una mutación confirmada puede invalidar el contexto usado para presentar acciones. La reconciliación no debe considerar válida una UE distinta seleccionada automáticamente ni restaurar datos con permisos obsoletos.

**Alternativas consideradas**:

- Recargar solo la bandeja: mantiene identidad y navegación obsoletas.
- Conservar la vista ante fallo de refresco: viola FR-039.
- Cerrar la sesión: amplía el efecto sin requisito; `/inicio` y retry son suficientes.

## 12. Prevención accesible y errores Angular

**Decisión**: mostrar las acciones por estado, impedir el envío de autosuspensión `ADMINISTRADOR_PIIP` propia y conservar una explicación accesible mediante estado/descripcion asociada. Mapear `problemCode` a mensajes accionables; para códigos desconocidos mostrar el `detail` seguro y el estado sin declararlo éxito.

**Razón**: la prevención reduce errores sin sustituir backend. La explicación mantiene comprensible por qué una acción no está disponible y el fallback soporta evolución compatible.

**Alternativas consideradas**:

- Ocultar sin explicación: dificulta entender la restricción.
- Deshabilitar toda suspensión propia: bloquearía `CONSULTA_EXTERNA` permitida.
- Mapear solo por HTTP: no diferencia los cinco casos `422`.

## 13. OpenAPI, cliente, DDL y documentación

**Decisión**: estabilizar backend → generar/revisar OpenAPI → regenerar cliente Angular → adaptar repositorio/UI → regenerar `database/generated/piip-oracle.sql` desde JPA → actualizar guía funcional. Cada generación, prueba o build requiere autorización explícita en su turno.

**Razón**: evita deriva entre propietario y consumidor. El DDL es un artefacto derivado, no una fuente estructural manual.

**Alternativas consideradas**:

- Diseñar Angular contra tipos manuales definitivos: puede divergir del contrato generado.
- Editar el DDL a mano: contradice la constitución.
- Omitir la guía: las reglas y resultados visibles sí cambian.

## Resultado

No quedan `NEEDS CLARIFICATION` bloqueantes. El diseño agrega una sola columna nullable a auditoría de acceso y no cambia el modelo persistente de asignaciones, cuentas Keycloak, `USUARIO.ACTIVO`, roles, catálogos ni capacidades ajenas a Administración de usuarios.
