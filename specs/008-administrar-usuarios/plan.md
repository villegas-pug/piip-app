# Plan de implementación: Administración de usuarios por institución

**Rama**: `main` | **Fecha**: 2026-08-11 | **Spec**: [spec.md](spec.md)

## Resumen

Mantener la autorización funcional exacta `rol + institución + Unidad Ejecutora` ya implementada y separar de ella una cobertura exclusiva para Administración de usuarios. La UE activa seguirá determinando el rol visible y el acceso al módulo; una vez dentro desde una UE con Administrador PIIP, el actor podrá gestionar asignaciones de todas las UE de la misma institución.

El CRUD, los candidatos, `roleScopes`, los guards por UE activa y Keycloak como autoridad de cuenta son baseline completado. Este incremento amplía únicamente la cobertura de Administración de usuarios, añade un catálogo administrativo explícito y adapta su consumidor Angular.

## Contexto técnico y restricciones

- **Tecnologías**: Java 21/Spring Boot 4.1/JPA Oracle; Angular 22/TypeScript/Material; OpenAPI con `ng-openapi-gen`.
- **Seguridad**: Keycloak autentica y Oracle autoriza. Los grants exactos continúan gobernando portafolio, documentos, trabajo, dashboard y el rol operativo visible.
- **Cobertura administrativa**: una institución es administrable cuando el actor posee al menos un grant activo y vigente `ADMINISTRADOR_PIIP` en cualquier UE de esa institución o con alcance institucional.
- **Persistencia**: no se modifican entidades, tablas, constraints ni datos. La cobertura administrativa se deriva de `USUARIO_ROL_AMBITO` y de los catálogos organizacionales activos.
- **Contrato**: se añade un endpoint administrativo para exponer instituciones y UE gestionables. `/identity/me` y `/executing-units` conservan su semántica actual.
- **UX**: UE-001 puede mostrar Consulta externa y UE-002 Administrador PIIP; sólo UE-002 habilita la entrada, pero la bandeja abierta desde allí incluye las asignaciones de toda MIDAGRI.
- **Decisiones sensibles confirmadas**: cualquier administrador de una UE puede conceder `Toda la institución` y administrar sus propias asignaciones.
- **Alcance excluido**: Keycloak Admin API, estado de cuenta local, migraciones, administración entre instituciones y cambios de autorización funcional fuera del módulo de usuarios.

## Diseño backend

### Cobertura especializada

- Conservar `RoleScopeGrant` y `LocalAccessContext` como fuente exacta de autorización funcional.
- Derivar para Administración de usuarios el conjunto de `institutionIds(ADMINISTRADOR_PIIP)` del actor persistido; no reutilizar `coversExecutingUnit(ADMINISTRADOR_PIIP, ...)` para decidir el destino administrativo.
- Limitar `UserAdministrationService` a esas instituciones para listado, alta, edición, suspensión y reactivación.
- Aceptar cualquier UE activa perteneciente a una institución administrable, aunque el actor no tenga un grant operativo en esa UE.
- Aceptar `executingUnitId = null` para crear o editar `Toda la institución` cuando la institución sea administrable.
- Permitir que el usuario objetivo sea el propio actor, sin omitir duplicidad, versión, último administrador, catálogos, bloqueos ni auditoría.
- Mantener la autorización exacta existente en los demás servicios; gestionar asignaciones de UE-001 desde UE-002 no concede escritura funcional sobre UE-001.

### Catálogo administrativo

- Publicar `GET /api/v1/admin/users/administrable-scopes`.
- Responder una colección por institución administrable con sus datos y todas sus UE activas, además de indicar que el alcance institucional está permitido.
- Obtener el catálogo desde las instituciones derivadas de los grants Administrador del actor y los repositorios JPA existentes.
- No ampliar `GET /executing-units`: ese endpoint sigue representando UE operativamente legibles y alimentando el selector superior.

### Auditoría y errores

- Conservar `ROL_ASIGNADO`, `ROL_ACTUALIZADO`, `ROL_SUSPENDIDO` y `ROL_REACTIVADO` con actor y antes/después no sensible.
- Mantener 403 para otra institución, 404 para recursos inexistentes, 409 para versión desactualizada y 422 para duplicados o reglas de último administrador.
- Revalidar al actor contra persistencia antes de cada operación para que una revocación concurrente invalide inmediatamente la cobertura administrativa.

## Contrato y frontend

### Contrato HTTP

- Añadir `AdministrableScopeResponse` con institución, `institutionWideAllowed` y UE activas.
- Actualizar la prueba OpenAPI; publicar y regenerar el cliente Angular sólo con autorización explícita, sin editar archivos generados manualmente.
- Mantener sin cambios `RoleAssignmentRequest`, `RoleAssignmentUpdateRequest`, `UserResponse`, candidatos y `/identity/me`.

### Angular

- Mantener el rol visible y las capacidades funcionales derivados exclusivamente de `roleScopes` exactos.
- Mantener `activeScopeAdministratorGuard` para `/administracion/usuarios`: UE-001 Consulta externa no abre el módulo y UE-002 Administrador PIIP sí.
- Cargar el nuevo catálogo administrativo junto con usuarios y candidatos al abrir la pantalla.
- Construir instituciones, UE y `Toda la institución` desde el catálogo administrativo, no desde `roleScopes` ni desde el selector superior.
- Mantener la bandeja transversal dentro de las instituciones administrables y explicar que UE-002 habilita la entrada mientras el módulo gestiona toda MIDAGRI.
- Permitir seleccionar al usuario actual y conservar confirmación expresa para `Toda la institución`.
- Al cambiar a una UE sin Administrador mientras el módulo está abierto, limpiar datos y redirigir a Inicio.

## Verificación

### Backend

- Probar que `ADMINISTRADOR_PIIP · UE-002` permite listar y gestionar asignaciones de UE-001 dentro de MIDAGRI, incluida la propia cuenta.
- Probar que ese mismo grant no permite crear, aprobar, cargar, publicar ni completar recursos funcionales de UE-001.
- Cubrir catálogo administrativo, UE activas, `Toda la institución`, otra institución, duplicados, versión, último administrador, bloqueos y auditoría.

### Frontend

- Confirmar UE-001 `Consulta externa`, UE-002 `Administrador PIIP`, opción deshabilitada en UE-001 y acceso válido sólo desde UE-002.
- Confirmar que la bandeja y formularios abiertos desde UE-002 incluyen UE-001, UE-002 y las demás UE activas de MIDAGRI.
- Cubrir autoasignación, confirmación institucional, errores 403/409/422, doble envío, accesibilidad y cambio de UE.

### Integración

- Publicar OpenAPI y regenerar Angular en secuencia sólo con autorización explícita.
- Ejecutar pruebas backend/frontend y E2E únicamente con autorización explícita.
- En E2E, usar Consulta externa en UE-001 y Administrador PIIP en UE-002; administrar reversiblemente una asignación de UE-001 desde UE-002 y confirmar que el rol operativo no cambia.

## Dependencias y orden

1. Incorporar pruebas backend de separación entre autorización funcional y cobertura administrativa.
2. Implementar la cobertura institucional y el endpoint de catálogo administrativo.
3. Publicar OpenAPI y regenerar el cliente Angular.
4. Adaptar la pantalla y sus pruebas sin modificar los guards funcionales existentes.
5. Ejecutar validaciones autorizadas, E2E reversible, `git diff --check` y `graphify update .`.

No hay `NEEDS CLARIFICATION`: la entrada depende de una UE activa con Administrador, la gestión de usuarios abarca toda la institución, el rol operativo permanece exacto y la autoasignación y `Toda la institución` están permitidas.
