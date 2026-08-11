# Plan de implementación: Administración de roles y ámbitos con cuentas Keycloak

**Rama**: `main` | **Fecha**: 2026-08-10 | **Spec**: [spec.md](spec.md)

## Resumen

Keycloak es la única autoridad del ciclo de vida de las cuentas. PIIP mantiene la autenticación JWT y la autorización local exclusivamente mediante asignaciones vigentes de rol, institución y Unidad Ejecutora. La administración deja de habilitar o inhabilitar usuarios locales y conserva la gestión de asignaciones, cobertura, concurrencia y auditoría.

## Contexto técnico y restricciones

- **Tecnologías**: Java 21/Spring Boot 4.1/JPA Oracle; Angular 22/TypeScript/Material; OpenAPI con `ng-openapi-gen`.
- **Seguridad**: Keycloak autentica y decide el estado de la cuenta; Oracle/JPA resuelve los roles y ámbitos funcionales. PIIP no llama a Keycloak Admin API ni modifica realm, clientes, tokens o sesiones.
- **Persistencia**: `USUARIO.ACTIVO` se mantiene como dato heredado, sin migración ni DDL, pero deja de participar en consultas, autorizaciones y mutaciones. Las asignaciones y catálogos conservan sus propios estados.
- **Contratos**: se retira la operación de cambio de estado de usuario y los campos de estado/versionado de usuario de la respuesta administrativa. OpenAPI se publica antes de regenerar Angular.
- **Calidad**: el frontend previene solamente duplicados visibles; el backend conserva la validación bloqueada, transaccional y autoritativa.

## Diseño

### Backend propietario

- Retirar el cambio de estado de usuario del controlador, servicio, DTOs, auditoría y pruebas asociadas.
- Hacer que `UserResponse` represente identidad local y sus asignaciones, sin estado ni versión de cuenta.
- Eliminar los predicados basados en `scope.user.active` de las consultas de autorización, cobertura, último administrador y destinatarios; una asignación efectiva requiere rol activo, asignación activa y vigencia temporal.
- Cambiar la consulta de candidatos a usuarios locales sin historial de `USUARIO_ROL_AMBITO`, sin filtrar el valor heredado `USUARIO.ACTIVO` ni consultar Keycloak.
- Conservar controles de Administrador PIIP, cobertura, bloqueo JPA, duplicidad exacta, edición, suspensión, reactivación y auditoría de asignaciones. La garantía de último administrador se calcula sobre asignaciones locales; la disponibilidad de cuentas la gestiona Keycloak.

### Frontend consumidor

- Regenerar el cliente desde el contrato publicado para retirar la operación y tipos de cambio de estado.
- Eliminar de la pantalla el indicador, confirmación, estado ocupado y acciones de habilitar/inhabilitar usuario. La única etiqueta de estado será la de la asignación activa o suspendida.
- Mantener la carga paralela de listado y candidatos, su unión exclusiva para el combo, la prevención de duplicado exacto, paginación, accesibilidad y mensajes 403, 409 y 422.

## Verificación

- Backend: una asignación activa y vigente autoriza aunque el valor heredado de usuario sea falso; candidatos sin historial aparecen sin depender de ese valor; continúan cobertura, duplicidad y último administrador.
- Contrato: no publica `PUT /admin/users/{userId}/status` ni campos `active`/`version` en `UserResponse`; conserva las operaciones de asignación.
- Frontend: no renderiza ni invoca controles de estado de usuario; conserva gestión de asignaciones y no realiza HTTP ante un duplicado visible.
- E2E: con un Administrador PIIP, comprobar la ausencia de administración de cuentas y los flujos de asignación. La comprobación de una cuenta deshabilitada se realiza en Keycloak según su procedimiento operativo, sin modificar PIIP.

## Secuencia y supuestos

1. Ajustar backend y pruebas focalizadas.
2. Publicar OpenAPI con autorización explícita y regenerar el cliente Angular en secuencia.
3. Adaptar pantalla y pruebas de componente.
4. Ejecutar pruebas autorizadas y recorrido manual.

Una cuenta deshabilitada puede conservar asignaciones preparadas en PIIP, pero no obtiene acceso funcional hasta autenticarse válidamente en Keycloak. El comportamiento de tokens o sesiones ya emitidos se rige por la política de Keycloak y queda fuera de este cambio.
