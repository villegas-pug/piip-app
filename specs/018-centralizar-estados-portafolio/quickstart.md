# Guía rápida de validación: Centralización de estados del portafolio

**Rama**: `018-centralizar-estados-portafolio` | **Fecha**: 2026-09-10 | **Spec**: [spec.md](./spec.md)

> ⚠️ **Advertencia**: esta guía es solo referencia. Ningún comando se ejecuta de forma automática; todos requieren autorización explícita del usuario en el turno vigente. La ejecución de la inicialización descartable requiere además la activación exacta y ordenada `test,test-reset` en un entorno reconstruido y descartable; está prohibida en `dev`, `prod` y operación normal.

## Comandos de referencia (manual, con autorización)

| Área | Comando (Windows) | Notas |
|------|-------------------|-------|
| Backend unitarias | `gradlew.bat test` (o `./gradlew test`) | Incluye `OpenApiGenerationTest`, que produce `apps/backend/target/piip-openapi.json`. |
| Backend completo | `gradlew.bat check` (o `./gradlew check`) | Lint + pruebas. |
| Integración Oracle | `gradlew.bat integrationTest` | Requiere Docker o variables Oracle; autorización adicional. |
| Seed descartable | Arranque con perfiles `test,test-reset` | Solo tras reconstrucción descartable; guardias fail-closed. |
| Cliente API frontend | `npm run api:generate` (en `apps/frontend`) | Consume `../backend/target/piip-openapi.json`; elimina artefactos obsoletos. |
| Frontend pruebas | `npm test -- --watch=false` | Vitest. |
| Frontend build | `npm run build` | Angular 22. |

## Escenarios de validación (mapeo a FR y SC)

1. **Catálogo central**: la consulta central presenta exactamente 11 estados activos en el orden 1-11, cada uno con código, denominación, orden, actividad y aplicabilidad; sin identificadores internos (SC-001, FR-005/FR-008/FR-009).
2. **Renombrado**: cambiar la denominación persistida de un código (p. ej. `SUSPENDED`) y verificar que filtros, listas, detalles, dashboard, documentos y auditoría visible muestran la nueva denominación, sin cambiar códigos, referencias históricas ni matrices (SC-002/SC-003, FR-010).
3. **Catálogo vacío o no disponible**: los consumidores presentan la condición de vacío/error, no ofrecen opciones, no usan inventarios locales ni fallbacks, y bloquean selección y transición; distinción entre `ready` vacío, error, carga y valor previo stale (FR-011, edge cases).
4. **Estados iniciales**: crear iniciativa (`PRESENTED`) y proyecto derivado/preexistente (`PROJECT_IN_PROGRESS`) con validación de existencia, actividad y aplicabilidad (FR-016).
5. **Transiciones con causas distinguibles**: destino inexistente, inactivo, no aplicable y fuera de matriz producen 422 con `ProblemCode` propio y en el orden existencia → actividad → aplicabilidad → matriz; las matrices permiten exactamente los pares de la spec y cero adicionales (SC-004/SC-005, FR-013/FR-014/FR-015).
6. **Histórico inactivo**: desactivar un estado referenciado y verificar que el registro lo muestra con `{code, name, active}`, no aparece en la consulta central, no es seleccionable, y que el registro puede salir de un origen inactivo hacia un destino válido por matriz (SC-006, FR-018/FR-020).
7. **`NOT_APPLICABLE`**: nunca se ofrece para iniciativa ni proyecto; su asignación se rechaza por aplicabilidad (FR-017).
8. **Dashboard, documentos y auditoría estructurados**: referencias de estado y conteos por código ordenados por catálogo, incluyendo inactivos con conteo; auditoría visible resuelve denominaciones vigentes vía referencias (FR-010, D11/D12/D13).
9. **Visual neutro**: un código desconocido usa presentación neutral sin inventar estados ni habilitar acciones funcionales (FR-012).
10. **Seed exacto e idempotente**: tras reconstrucción descartable con `test,test-reset`, quedan exactamente los once códigos con nombre/orden/actividad/aplicabilidad esperados; la reejecución no duplica; una discrepancia simulada falla cerrada; cualquier otra activación ejecuta cero DML de estados (SC-007/SC-008, FR-022 a FR-026).
11. **`dev`/`prod` sin seed**: `ddl-auto=validate` intacto, sin carga automática; la documentación declara la preparación productiva incompleta hasta la feature posterior (FR-028/FR-029).

## Entornos

| Perfil | Uso | Restricción |
|--------|-----|-------------|
| `test` | Pruebas automatizadas backend | Sin DML destructivo. |
| `test,test-reset` (exacto y ordenado) | Reconstrucción descartable + seed | Guardias fail-closed; exclusivo de desarrollo/pruebas; prohibido en producción. |
| `dev` / `prod` | Desarrollo local / institucional | `ddl-auto=validate`; sin seed; la nueva tabla requiere provisión institucional futura (feature posterior). |

## Trazabilidad documental

La implementación debe actualizar en la misma entrega: `docs/architecture/piip-fields.md`, `docs/architecture/data-model-final.md`, `docs/funcional/guia-funcional-piip.md`, `docs/development/test-catalog-reset.md` (conteos 21 tablas / 11 estados) y `docs/deployment/institutional-development.md`, con evidencia verificable (FR-030, SC-009).
