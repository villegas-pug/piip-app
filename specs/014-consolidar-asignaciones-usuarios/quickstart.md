# Quickstart: Consolidación de asignaciones de usuarios

## Precondiciones funcionales

- Dos usuarios locales autenticados previamente en PIIP.
- Al menos dos asignaciones activas `ADMINISTRADOR_PIIP` que cubran la UE de prueba, salvo en los casos controlados de último administrador.
- Una asignación propia `CONSULTA_EXTERNA` separada del grant administrador.
- Una o más asignaciones suspendidas con combinación exacta conocida para comprobar auto-reactivación.
- Institución y UEs activas; no crear roles, catálogos, cuentas Keycloak ni modificar `USUARIO.ACTIVO`.
- Datos reversibles y sin credenciales, tokens o información personal innecesaria en la evidencia.

## Orden de implementación

1. Crear commands/read models de `identity.application` y mapper HTTP; eliminar `application -> api` en Administración de usuarios.
2. Incorporar códigos estables de error y `ProblemDetail.problemCode` con atributo seguro para auditoría.
3. Añadir `MOTIVO_SEGURO` mediante `AccessAuditEntity` y propagarlo por servicio, filtro y read model de Auditoría.
4. Implementar locks ordenados, revalidación persistida, selección suspendida y cobertura posterior por cada UE.
5. Consolidar assign/update/suspend/reactivate y snapshots funcionales atómicos.
6. Adaptar controller y OpenAPI a `POST 201/200`, errores y cuerpos reales.
7. Con autorización explícita, generar/revisar OpenAPI y regenerar el cliente Angular.
8. Encapsular mutaciones en `PiipRepository`/adaptadores; adaptar UI, accesibilidad, errores y recuperación fail-closed.
9. Actualizar guía funcional y DDL derivado.
10. Ejecutar validaciones únicamente cuando el usuario las autorice en el turno correspondiente.

## Estado de esta implementación

Se aplicaron las tareas de código y cobertura verificable T001–T040, T042–T055, T057–T058 y T070. Las pruebas T014, T019 y T031 cubren grant institucional sobre varias UEs, suspensiones serializadas sobre una misma UE y dos POST simultáneos, con una única mutación aceptada. T024 cubre los cuatro éxitos, rechazos sin evento y rollback transaccional ante fallo de auditoría mediante `UserAdministrationTransactionalAuditTest`; estas pruebas no se presentan como equivalentes a una prueba Oracle concurrente.

Se ejecutaron satisfactoriamente la suite backend focalizada, `gradlew.bat check`, la generación/revisión OpenAPI, `npm run api:generate`, la suite frontend completa (39 archivos, 249 pruebas), `npm run build`, la generación y sincronización DDL y `git diff --check`. El build conserva advertencias de presupuesto Angular sobre el bundle inicial (563.18 kB frente a 500 kB) y `dashboard.component.scss` (12.96 kB frente a 12 kB), sin errores. T069 se cerró con `gradlew.bat integrationTest --tests "pe.gob.midagri.piip.identity.UserAdministrationOracleIntegrationTest"` usando el wallet Oracle y el perfil `dev`: 3 pruebas ejecutadas, 0 omitidas y 0 fallos. La prueba confirmó lectura/escritura JPA de `AUDITORIA_ACCESO.MOTIVO_SEGURO`, serialización del lock pesimista de `USUARIO_ROL_AMBITO` entre dos transacciones Oracle y orden ascendente del lock combinado de usuarios. No se usaron Docker ni `test-reset`; los tests de contenedor y reset permanecen fuera de T069.

El E2E reversible (T070) se completó con `Usuario e2e`: creación institucional `201`, suspensión, auto-reactivación exacta `200` sin nueva fila, edición de UE conservando identidad, suspensión y reactivación. La mutación de prueba se restauró mediante nuevas operaciones auditables; no se borraron filas ni eventos. La pantalla de Auditoría disponible en esta versión muestra expedientes, por lo que la integridad de eventos de asignación se confirmó con la prueba transaccional backend y no con una vista UI específica.

## Matriz funcional mínima

### Asignar y auto-reactivar

1. Solicitar una combinación sin historia: debe crear una fila, devolver `201` y un único evento con el ID nuevo.
2. Suspender esa fila y solicitar la misma combinación: debe reactivar el mismo ID, devolver `200` y no aumentar la cantidad histórica exacta.
3. Preparar varias coincidencias suspendidas: debe reactivar la de `VIGENTE_HASTA` más reciente; ante empate, la de mayor ID.
4. Solicitar una combinación activa exacta: `422 ACTIVE_ASSIGNMENT_DUPLICATE`, sin cambio ni evento funcional.
5. Enviar dos solicitudes concurrentes de la misma combinación: debe terminar con una sola asignación activa.

### Editar

1. Cambiar rol, institución o UE de una asignación activa ajena: conservar ID y cambiar solo esos campos.
2. Editar una asignación propia: refrescar identidad y UE antes de habilitar otra acción.
3. Enviar versión desactualizada: `409 STALE_VERSION`, formulario/bandeja recargables y cero evento funcional.
4. Editar hacia un duplicado activo: `422 ACTIVE_ASSIGNMENT_DUPLICATE`.
5. Mover un grant institucional administrador: validar la cobertura posterior de todas las UEs activas afectadas.

### Suspender y reactivar

1. Suspender una asignación ajena `CONSULTA_EXTERNA`: `204`, fila conservada y evento completo.
2. Suspender una asignación ajena `ADMINISTRADOR_PIIP` cuando permanece cobertura: `204`.
3. Intentar autosuspender `ADMINISTRADOR_PIIP`: la UI no envía la petición y una llamada directa responde `422 SELF_ADMIN_SUSPENSION`.
4. Autosuspender `CONSULTA_EXTERNA` conservando otro grant administrador: permitido, seguido de refresco propio.
5. Retirar la última cobertura de una UE: `422 LAST_ACTIVE_ADMIN`.
6. Editar/suspender una suspendida o reactivar una activa: `422 INCOMPATIBLE_ASSIGNMENT_STATE`.
7. Reactivar con rol/institución/UE inactiva o incoherente: `422 INVALID_ACTIVE_REFERENCE`.

### Autorización y revocación concurrente

1. Abrir la pantalla como administrador de una institución.
2. Revocar el último grant administrativo del actor antes de enviar otra mutación.
3. Confirmar `403 FORBIDDEN_SCOPE`, ausencia de cambio/evento funcional y auditoría HTTP con el mismo motivo seguro.
4. Confirmar que un grant administrador de otra institución no autoriza la operación.

## Refresco del actor y fail-closed

1. Capturar la UE activa original antes de una mutación propia.
2. Ejecutar creación, edición, suspensión y reactivación propias en casos separados.
3. Confirmar que cada éxito reobtiene `/identity/me`, ámbitos y UEs antes de permitir otra acción.
4. Si el actor conserva Administración sobre la UE original, recargar bandeja, candidatos y detalle.
5. Si la pierde, limpiar datos administrativos y navegar a `/inicio` aunque la reconciliación haya elegido otra UE.
6. Simular fallo del refresco después de una mutación confirmada: limpiar, navegar a `/inicio`, avisar que el cambio ocurrió y mostrar `Reintentar`.
7. Antes de que el retry finalice correctamente, confirmar que Administración no repuebla datos ni acciones.

## Errores visibles

| HTTP | Código mínimo | Respuesta esperada en Angular |
|------|---------------|-------------------------------|
| 400 | `INVALID_REQUEST` | Indicar solicitud inválida/campo y permitir corregir. |
| 403 | `FORBIDDEN_SCOPE` | Informar pérdida o falta de cobertura; limpiar si el contexto propio quedó inválido. |
| 404 | `RESOURCE_NOT_FOUND` | Informar referencia/registro ausente y recargar datos. |
| 409 | `STALE_VERSION` | Informar cambio concurrente y solicitar recarga. |
| 422 | cinco reglas específicas o `BUSINESS_RULE_VIOLATION` | Mensaje accionable por `problemCode`; el fallback genérico solo cubre una regla controlada sin código más específico y nunca se infiere mediante parsing de `detail`. |
| cualquiera con código desconocido | código no reconocido | Mostrar `detail` seguro como fallback, sin presentarlo como éxito. |

## Auditoría

Para cada mutación exitosa:

- exactamente un evento funcional `USUARIO_ROL_AMBITO`;
- `codigoEntidad = assignmentId`;
- actor, acción, usuario afectado, snapshot anterior/nuevo, resultado y fecha;
- rollback de mutación si falla el evento.

Para cada rechazo:

- cero eventos funcionales de éxito o fallo;
- una fila de `AUDITORIA_ACCESO` con método, ruta normalizada, estado, correlación y `MOTIVO_SEGURO`;
- ningún token, body, credencial ni texto libre de error persistido.

## Contrato y cliente

Revisar estáticamente antes de generar:

- `POST /api/v1/admin/role-assignments`: respuestas con cuerpo `201` y `200`;
- PUT de edición/reactivación `200`, DELETE `204`;
- `version` obligatoria en mutaciones de fila existente;
- errores `application/problem+json` para `400/403/404/409/422`;
- `ProblemDetail.problemCode` documentado;
- `GET /api/v1/audit/accesses` publica `safeReason` nullable;
- el cliente generado expone respuesta completa para `POST` y el modelo `ProblemDetail` tipado.

## Verificaciones propuestas que requieren autorización

No ejecutar automáticamente. Cuando el usuario las autorice:

```powershell
cd F:\work-space\piip-monorepo\apps\backend
.\gradlew.bat test --tests "pe.gob.midagri.piip.identity.*" --tests "pe.gob.midagri.piip.audit.*" --tests "pe.gob.midagri.piip.shared.api.*" --tests "pe.gob.midagri.piip.contract.OpenApiGenerationTest"
.\gradlew.bat check
```

La publicación OpenAPI también depende de la prueba autorizada del proyecto:

```powershell
cd F:\work-space\piip-monorepo\apps\backend
.\gradlew.bat test --tests "*OpenApiGenerationTest"
```

Después de revisar `apps/backend/target/piip-openapi.json`:

```powershell
cd F:\work-space\piip-monorepo\apps\frontend
npm run api:generate
npm test -- --watch=false
npm run build
```

Por el cambio `AUDITORIA_ACCESO.MOTIVO_SEGURO`, la integración Oracle autorizada se ejecutó con el wallet existente, sin Docker ni `test-reset`:

```powershell
cd F:\work-space\piip-monorepo\apps\backend
.\gradlew.bat integrationTest --tests "pe.gob.midagri.piip.identity.UserAdministrationOracleIntegrationTest"
```

## Aceptación funcional integral (SC-010)

Con datos controlados y reversibles, un `ADMINISTRADOR_PIIP` autorizado debe completar estos recorridos:

1. asignar una combinación sin historia y confirmar creación `201` en bandeja y detalle;
2. solicitar una combinación suspendida exacta y confirmar auto-reactivación `200` del mismo ID;
3. editar rol, institución o Unidad Ejecutora y confirmar que se conserva el ID;
4. suspender una asignación permitida y confirmar su estado en bandeja y detalle;
5. reactivar una asignación suspendida y confirmar ID, versión y estado;
6. comprobar que cada operación respeta permisos, errores y auditoría definidos, sin correcciones manuales de datos en la interfaz.

## Restauración

- Reactivar toda asignación suspendida solo para la prueba, salvo que el caso exija conservarla suspendida como fixture acordada.
- Revertir ediciones de rol/ámbito mediante una nueva mutación autorizada y auditable; no borrar filas ni eventos.
- No intentar eliminar auditoría append-only.
- Registrar cualquier dato que no pueda restaurarse sin acciones destructivas y detener el E2E antes de improvisar.
