# Inventario de compatibilidad de parámetros HTTP

Este inventario fija los nombres públicos que deben conservarse. No introduce ni modifica un contrato HTTP.

| Controlador | Operaciones | Entradas públicas a conservar | Cantidad |
|-------------|-------------|-------------------------------|----------|
| `OrganizationController` | `organizationalUnits` | `executingUnitId` | 1 |
| `DocumentController` | listado, carga, no aplicable, publicación y descarga | `recordCode`, `type`, `file`, `versionId`, `published`, `version` | 12 |
| `PortfolioController` | iniciativas y proyectos | `q`, `status`, `executingUnitId`, `page`, `size`, `sort`, `direction`, `code` | 17 |
| `UserAdministrationController` | suspensión de asignación | `scopeId`, `version` | 2 |
| `WorkController` | completar y reasignar tarea | `taskId`, `version` | 3 |
| `NotificationController` | marcar leída | `id` | 1 |
| **Total** |  |  | **36** |

## Invariantes

- Rutas, métodos HTTP, nombres, tipos, obligatoriedad y valores por defecto no cambian.
- Las entradas de cuerpo y los encabezados no forman parte de esta feature.
- El frontend y OpenAPI no requieren adaptación.
