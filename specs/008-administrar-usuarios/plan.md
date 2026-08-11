# Plan de implementación: Autorización exacta por rol y ámbito

**Rama**: `main` | **Fecha**: 2026-08-10 | **Spec**: [spec.md](spec.md)

## Resumen

Corregir la autorización para que cada decisión sensible evalúe una misma asignación `rol + institución + Unidad Ejecutora`, sin combinar Administrador PIIP de un ámbito con la cobertura de otro. El backend conservará los grants exactos como fuente canónica y Angular derivará el rol visible y las acciones desde esos grants y la UE correspondiente.

El CRUD de asignaciones, los candidatos y Keycloak como autoridad de cuenta son baseline implementado. Este incremento no los reimplementa, no cambia el esquema y mantiene la auditoría global fuera de la corrección de ámbito.

## Contexto técnico y restricciones

- **Tecnologías**: Java 21/Spring Boot 4.1/JPA Oracle; Angular 22/TypeScript/Material; OpenAPI con `ng-openapi-gen`.
- **Seguridad**: Keycloak autentica; Oracle/JPA autoriza con asignaciones activas y vigentes. Spring Security conserva autoridades agregadas como barrera gruesa y los servicios aplican la autorización definitiva.
- **Persistencia**: no se modifican entidades, tablas, constraints ni datos. `RoleScopeGrant` es un valor de aplicación derivado de `USUARIO_ROL_AMBITO`.
- **Contrato**: `/identity/me` añade `roleScopes[]` de forma compatible; los campos agregados existentes permanecen temporalmente y Angular deja de usarlos para autorizar.
- **UX**: el rol visible depende de la UE activa; Administrador PIIP prevalece ante doble rol en la misma UE. Administración de usuarios es transversal, limitada exclusivamente por grants Administrador.
- **Alcance excluido**: Keycloak Admin API, estado de cuenta local, rediseño de auditoría global, migraciones y nuevas reglas funcionales.

## Diseño backend

### Contexto canónico

- Crear `RoleScopeGrant(role, institutionId, executingUnitId)` como valor inmutable.
- Hacer que `LocalAccessContext` conserve grants exactos y derive sólo cuando sea necesario los roles y ámbitos agregados para compatibilidad, autoridades y consultas de lectura.
- Exponer operaciones explícitas para rol global, lectura de UE, rol en UE y rol institucional. Una cobertura institucional sólo cubre UE de su institución y conserva el rol del grant.
- Mapear cada `UserRoleScopeEntity` activa y vigente a un grant en `LocalAuthorizationService.resolve`.
- Corregir `requireUnit` para exigir el rol y la cobertura en el mismo grant; `requireReadableUnit` seguirá aceptando cualquier grant que cubra la UE.

### Consumidores de autorización

- **Portafolio**: aplicar rol exacto en creación y aprobación de iniciativas, proyectos derivados/preexistentes e iniciativas elegibles. Los listados mantienen cobertura legible por cualquier grant.
- **Documentos**: usar rol exacto del registro para carga, publicación, visibilidad interna y descarga; Administrador en otra UE no podrá ver una versión no publicada.
- **Trabajo y dashboard**: filtrar tareas administrativas por la UE del registro y exigir rol exacto en completar o reasignar.
- **Administración de usuarios**: `currentAdministrator` exige al menos un grant Administrador; listado, origen, destino y opción institucional se validan sólo con grants Administrador. Consulta externa no amplía la cobertura administrativa.
- **Organización y filtros**: `/executing-units` continúa devolviendo todas las UE legibles. `LocalAuthorityFilter` conserva autoridades agregadas como control grueso.

## Contrato y frontend

### Contrato HTTP

- Añadir `RoleScopeResponse { role, institutionId, executingUnitId? }` y `roleScopes[]` a `CurrentUserResponse`.
- Mantener `roles`, `institutionIds`, `executingUnitIds` e `institutionWide` durante esta corrección para compatibilidad.
- Actualizar la prueba de generación OpenAPI, publicar el contrato con autorización explícita y después regenerar el cliente Angular sin editar generated manualmente.

### Estado y presentación Angular

- Añadir capacidades de repositorio `canReadExecutingUnit`, `canAdministerExecutingUnit`, `hasAnyAdministratorScope` y rol efectivo por UE.
- Derivar el rol visible desde `roleScopes` y `selectedExecutingUnitId`; no asumir Consulta externa antes de que el contexto esté cargado.
- Mantener todas las UE legibles en el selector superior. Al cambiar de UE, recalcular rol, navegación y acciones y limpiar cargas privilegiadas que ya no correspondan.
- Separar el guard de Administrador en cualquier ámbito, usado por Administración de usuarios y Auditoría, del guard Administrador de la UE activa usado por rutas de creación.
- Calcular acciones sobre iniciativas, proyectos y documentos con la UE real del registro para cubrir enlaces directos.
- En Administración de usuarios, filtrar instituciones y UE con grants Administrador y mostrar `Toda la institución` sólo para cobertura institucional Administrador.
- Mantener 403 como protección de respaldo y refrescar el contexto ante una revocación concurrente.

## Verificación

### Backend

- Reproducir con prueba la combinación `CONSULTA_EXTERNA · UE-001` más `ADMINISTRADOR_PIIP · UE-002` y confirmar que UE-001 es legible pero no administrable.
- Cubrir grants institucionales, doble rol en la misma UE, identidad exacta, portafolio, documentos no publicados, tareas y Administración de usuarios.
- Conservar duplicidad, último administrador, vigencia, concurrencia y auditoría de escrituras.

### Frontend

- Cubrir cambio de UE y rol visible, precedencia Administrador, guards, acciones por UE real y limpieza de estado privilegiado.
- Confirmar que Administración de usuarios permanece transversal pero sólo ofrece ámbitos Administrador y que la opción institucional respeta ese rol.
- Mantener pruebas de errores 403/409/422, duplicados, doble envío y accesibilidad.

### Integración

- Publicar OpenAPI y regenerar Angular en secuencia sólo con autorización explícita.
- Ejecutar pruebas focalizadas backend y frontend sólo con autorización explícita.
- Recorrer E2E con UE-001 Consulta externa y UE-002 Administrador PIIP, restaurando cualquier operación reversible.

## Dependencias y orden

1. Incorporar el grant exacto y las pruebas de regresión backend.
2. Corregir todos los consumidores backend antes de publicar el contrato.
3. Añadir `roleScopes`, publicar OpenAPI y regenerar el cliente Angular en ese orden.
4. Adaptar el repositorio, guards, shell, pantallas y pruebas frontend.
5. Ejecutar validaciones autorizadas, E2E reversible, `git diff --check` y actualización incremental de Graphify.

No hay `NEEDS CLARIFICATION`: el rol visible es el de la UE activa, Administrador prevalece en doble rol y Administración de usuarios es transversal con cobertura exclusivamente administrativa.
