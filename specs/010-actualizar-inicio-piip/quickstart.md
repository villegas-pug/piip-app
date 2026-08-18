# Quickstart de implementación: Actualización de Inicio PIIP

## Propósito

Guía de ejecución futura para implementar la feature en el orden correcto. Este artefacto no autoriza ejecutar comandos restringidos.

## Prerrequisitos

- `spec.md`, `plan.md`, `research.md`, `data-model.md` y el contrato revisados.
- `tasks.md` generado y aprobado antes de `/speckit-implement`.
- Sin `NEEDS CLARIFICATION` ni checklists bloqueantes.
- Autorización separada para pruebas, builds, generación OpenAPI, servidores u Oracle.

## Secuencia recomendada

1. Implementar el repositorio JPA de consulta con predicados compartidos, página global, agrupación y conteo base.
2. Implementar `DashboardPortfolioService` con autorización exacta de UE, validación y normalización de página.
3. Exponer `GET /api/v1/dashboard/portfolio` y sus DTO sin modificar los endpoints legados.
4. Preparar pruebas focalizadas del contrato, servicio, persistencia y regresión.
5. Con autorización explícita, generar/publicar OpenAPI y regenerar el cliente Angular; no editar manualmente el código generado.
6. Adaptar modelos y repositorio Angular al nuevo contrato.
7. Actualizar Inicio, AppShell, notificaciones, accesibilidad y pruebas de presentación.
8. Actualizar `docs/funcional/guia-funcional-piip.md` con el recorrido real.
9. Con autorización explícita, ejecutar validaciones focalizadas y luego las integrales pertinentes.

## Casos mínimos de verificación

- Usuario con dos UE: Inicio solo refleja la UE activa y una UE fuera de alcance devuelve 403.
- `Tipo: Todos`: iniciativas y proyectos cruzan páginas en orden global estable.
- Búsqueda, tipo y estado: tabla, total, indicadores y barras se reconcilian.
- Cada página contiene como máximo cinco registros y un estado incompatible vuelve a `Estado: Todos`.
- UE sin registros y filtros sin coincidencias producen mensajes distintos.
- Cambio rápido de UE/filtros no deja datos obsoletos.
- El resumen compacto contiene tres avisos; campana, pestañas y `Ver todas` no los marcan.
- La campana lleva a Inicio y enfoca `Mis notificaciones` desde cualquier pantalla autenticada.
- `Marcar como leída` cambia solo una notificación y reduce el badge exactamente en uno.
- Los errores de portafolio y notificaciones son independientes y reintentables.

## Comandos previstos, no ejecutados

Los nombres exactos deben confirmarse contra los scripts vigentes al implementar. Su ejecución requiere autorización explícita del usuario:

```powershell
# Backend: pruebas focalizadas y/o suite autorizada
Set-Location apps/backend
.\gradlew.bat test

# OpenAPI: ejecutar el mecanismo vigente y luego regenerar el cliente
Set-Location ..\frontend
npm run api:generate

# Frontend: pruebas y build autorizados
npm test -- --watch=false
npm run build
```

Las comprobaciones estáticas permitidas y `git diff --check` pueden emplearse durante la implementación sin sustituir las validaciones funcionales.

## Criterio de cierre futuro

La implementación solo estará lista cuando el contrato y el cliente estén sincronizados, los escenarios de la especificación tengan cobertura, la guía funcional corresponda al comportamiento real y se documenten claramente las verificaciones ejecutadas y no ejecutadas.
