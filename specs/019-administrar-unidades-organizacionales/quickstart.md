# Guía de implementación: Administración de Unidades Organizacionales

Esta guía describe la secuencia obligatoria de implementación. No autoriza por sí sola pruebas, builds, generación OpenAPI, Oracle ni acciones Git.

## 1. Verificar el alcance de datos

- Confirmar que la ejecución se limita al perfil exacto `test,test-reset`.
- No habilitar la feature en una base Oracle con UE preexistentes.
- No crear migraciones, DDL manual, Flyway, Liquibase ni cargas de datos reales.
- Actualizar el DML idempotente de `apps/backend/src/main/resources/db/test/catalog-data.sql` y las postvalidaciones de reset para los campos y códigos sintéticos nuevos.

## 2. Implementar el propietario canónico backend

- Ampliar `ExecutingUnitEntity` y los repositorios JPA requeridos.
- Mantener `OrganizationalUnitEntity.parent` sin cambios funcionales.
- Añadir autorización institucional explícita en el servicio de identidad.
- Implementar el servicio transaccional administrativo con generación bajo lock de institución o UE, `@Version` y auditoría append-only.
- Añadir controlador, DTO, commands y read models administrativos separados de las lecturas legadas.
- Preservar `OrganizationController` y `OrganizationQueryService` actuales, incluido el catálogo de UO activas con sigla.

## 3. Publicar y consumir el contrato

- Validar el contrato administrativo y sus errores desde backend.
- Solo con autorización explícita, generar `piip-openapi.json` y sincronizar el cliente Angular mediante el flujo `fe-sync-openapi-client`.
- No editar archivos de `apps/frontend/src/app/api/generated` de forma manual.

## 4. Implementar frontend después del contrato

- Añadir guard defensivo institucional, entrada de navegación y rutas separadas para UE y UO.
- Mantener `executingUnitId` en la ruta de UO y no depender de la UE global activa.
- Usar modelos y estado administrativos distintos del catálogo de portafolio.
- Mostrar contexto heredado solo lectura; no mostrar código editable ni `parentId`.
- Ante `403` y `404`, limpiar el estado y volver a una vista segura; ante `409`, conservar borrador y ofrecer recarga explícita.

## 5. Documentar y comprobar

- Actualizar `docs/architecture/data-model-final.md` y `docs/funcional/guia-funcional-piip.md` con la administración separada, autorización, herencia, orden/fechas UE y exclusión de `ID_UNIDAD_PADRE`.
- Preparar pruebas focalizadas de autorización, contexto, códigos, estados, fechas, auditoría, concurrencia, contrato, reset, rutas, accesibilidad y regresión del catálogo.
- Ejecutar el backend con `test-reset` solo durante la implementación autorizada por el usuario y según el perfil exacto definido por el repositorio.
