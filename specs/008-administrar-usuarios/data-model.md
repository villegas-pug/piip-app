# Modelo de datos: Administración integral de usuarios

## Entidades existentes afectadas

| Entidad | Campos relevantes | Cambios de comportamiento | Relaciones |
|---------|-------------------|----------------------------|------------|
| `Usuario local` (`USUARIO`) | identificador, subject de autenticación, nombre, correo, `activo` heredado, `version` | Su campo `activo` heredado se conserva sin migración ni efecto funcional; Keycloak gobierna el ciclo de vida de la cuenta. | Tiene muchas asignaciones, eventos de auditoría y accesos auditados. |
| `Asignación de rol y ámbito` (`USUARIO_ROL_AMBITO`) | identificador, usuario, rol, institución, Unidad Ejecutora opcional, `activo`, vigencias, `version` | Se crea, actualiza conservando su identificador, suspende y reactiva. | Pertenece a un usuario, rol, institución y opcionalmente Unidad Ejecutora. |
| `Rol PIIP` (`ROL`) | código, nombre, `activo` | Solo los roles activos pueden concederse o conservarse al reactivar. | Una asignación referencia un rol. |
| `Institución` (`INSTITUCION`) | código, nombre, `activo`, `version` | Solo una institución activa puede ser destino de una asignación. | Contiene Unidades Ejecutoras y delimita ámbitos. |
| `Unidad Ejecutora` (`UNIDAD_EJECUTORA`) | institución, código, nombre, `activo`, `version` | Es opcional para ámbito institucional y debe pertenecer a la institución activa seleccionada. | Delimita el ámbito específico de la asignación. |
| `Evento de auditoría` (`EVENTO_AUDITORIA`) | tipo, entidad, detalle, actor, fecha | Recibe eventos append-only de creación, actualización, suspensión y reactivación de asignaciones. | Referencia opcionalmente al actor local. |

## Proyección de candidatos de primera asignación

`UserAssignmentCandidateResponse` no es una entidad ni modifica el esquema. Proyecta `id`, `subject`, `fullName` y `email` de `USUARIO` cuando no existe ninguna fila relacionada de `USUARIO_ROL_AMBITO`. El frontend la combina con los usuarios administrables sólo para el selector de alta.

## Grant funcional exacto

`RoleScopeGrant` es un valor de aplicación, no una entidad ni una tabla nueva. Conserva `role`, `institutionId` y `executingUnitId` opcional de una misma asignación activa y vigente. Un `executingUnitId` nulo representa cobertura institucional únicamente dentro de la institución y con el rol del mismo grant.

`RoleScopeResponse` publica esa misma forma dentro de `/identity/me`. Las colecciones agregadas existentes se conservan temporalmente por compatibilidad, pero no son fuente de decisiones sensibles.

## Proyección de cobertura administrativa

`AdministrableScopeResponse` tampoco es una entidad ni crea tablas. Proyecta cada institución donde el actor posee al menos un `RoleScopeGrant` activo `ADMINISTRADOR_PIIP`, junto con todas sus Unidades Ejecutoras activas y la disponibilidad de `Toda la institución`.

Esta proyección sólo autoriza operaciones sobre asignaciones de usuarios. No crea grants heredados, no cambia `/identity/me`, no añade UE al selector operativo y no concede capacidades funcionales sobre iniciativas, proyectos, documentos o tareas.

## Transiciones de estado

| Recurso | Estado inicial | Operación | Estado final | Regla |
|---------|----------------|-----------|--------------|-------|
| Asignación | Activa | Actualizar rol/ámbito | Activa | Conserva identificador, debe pasar autorización de origen y destino y no duplicar una asignación vigente. |
| Asignación | Activa | Suspender | Suspendida | Cambia a inactiva y fija la vigencia final; no puede dejar una cobertura sin Administrador PIIP. |
| Asignación | Suspendida | Reactivar | Activa | Conserva identificador y devuelve vigencia; rol y ámbito deben seguir activos y autorizados. |

## Reglas de validación e integridad

- La versión esperada debe coincidir antes de toda escritura; `@Version` conserva la protección ante actualizaciones optimistas.
- El actor debe poseer un grant Administrador PIIP persistido en la institución de los ámbitos origen y destino afectados por Administración de usuarios.
- Para operaciones funcionales, el rol Administrador PIIP y la cobertura del ámbito deben proceder del mismo `RoleScopeGrant`; no se pueden combinar grants diferentes.
- La lectura puede usar cualquier grant que cubra el ámbito, pero las capacidades de escritura permanecen ligadas al rol de ese grant.
- La excepción anterior sólo aplica a la gestión de asignaciones: un grant Administrador en cualquier UE de una institución permite administrar asignaciones de todas sus UE, sin convertirse en un grant operativo institucional.
- No puede existir más de una asignación activa para igual usuario, rol, institución y Unidad Ejecutora, excluyendo la misma asignación durante la edición.
- La Unidad Ejecutora, si existe, debe pertenecer a la institución seleccionada y ambos catálogos deben estar activos.
- La suspensión, edición o reactivación no puede eliminar la última cobertura activa de Administrador PIIP de un ámbito.
- Las comprobaciones de duplicidad y última cobertura se serializan mediante bloqueos JPA dentro de la transacción.
- Ninguna transición modifica el subject de autenticación, el ciclo de vida de la cuenta en Keycloak, credenciales ni contenido de auditoría previo.
- El rechazo preventivo del frontend considera sólo asignaciones activas ya visibles; el servicio conserva la comprobación bloqueada y transaccional como regla definitiva.
- El usuario objetivo puede ser el propio actor; una autoasignación institucional pasa las mismas validaciones y, una vez persistida, sí modifica sus grants operativos futuros.
