# Plan de implementación: Actualización de Inicio PIIP

**Rama**: `main` | **Fecha**: 2026-08-18 | **Spec**: [spec.md](./spec.md)

**Entrada**: especificación en `/specs/010-actualizar-inicio-piip/spec.md`

**Nota**: este plan define diseño y secuencia; no autoriza implementación, generación OpenAPI, pruebas, compilaciones, servidores, Oracle ni acciones Git.

## Resumen

Actualizar Inicio para consultar una página global de iniciativas y proyectos de la Unidad Ejecutora activa, reconciliar listado, total e indicadores con los mismos filtros y presentar notificaciones personales sin lecturas implícitas. El backend será propietario de un contrato aditivo `GET /api/v1/dashboard/portfolio` con orden fijo `updatedAt DESC, id DESC`, páginas de cinco registros y conteos agrupados. Angular consumirá ese contrato, mostrará tres notificaciones en el resumen compacto y mantendrá independientes los estados de carga del portafolio y las notificaciones. La campana llevará a Inicio y enfocará `Mis notificaciones`. El resumen legado `/dashboard` se conserva por compatibilidad, pero Inicio deja de depender de sus métricas.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| Iniciativas y proyectos se consultan mediante páginas independientes | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/api/PortfolioController.java` | No permite una página global correcta para `Tipo: Todos`; se requiere una consulta unificada aditiva. |
| La consulta vigente filtra por estado, UE y alcance autorizado | `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/application/PortfolioService.java` | Reutilizar criterios y autorización, limitando en Inicio la búsqueda a código o nombre. |
| El resumen legado mezcla todos los ámbitos, tareas, alertas y notificaciones | `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/api/DashboardController.java` | Mantener por compatibilidad; no ampliarlo ni usarlo como fuente del nuevo Inicio. |
| Las notificaciones son personales y tienen lectura explícita | `apps/backend/src/main/java/pe/gob/midagri/piip/work/api/NotificationController.java` | Reutilizar `GET /notifications` y `PUT /notifications/{id}/read`. |
| El frontend concatena hasta 100 iniciativas y 100 proyectos | `apps/frontend/src/app/core/piip-http.repository.ts` | Sustituir solo la fuente de Inicio; conservar listados completos existentes. |
| La campana actual provoca una lectura automática | `apps/frontend/src/app/layout/app-shell.component.ts` | Eliminar ese efecto; mostrar el contador y enfocar el bloque de notificaciones. |
| Existe paginación accesible de cinco elementos | `apps/frontend/src/app/shared/pagination/piip-pagination.component.ts` | Reutilizarla con páginas calculadas en backend. |
| Los estados están modelados por tipo | `apps/frontend/src/app/core/piip.models.ts` y `apps/backend/src/main/java/pe/gob/midagri/piip/portfolio/domain/PortfolioStatus.java` | Derivar opciones y etiquetas canónicas sin crear estados ni transiciones. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | Sí | `apps/frontend/src/app/pages/dashboard/**`, `apps/frontend/src/app/layout/**`, `apps/frontend/src/app/core/**`, cliente generado | Consumidor del contrato backend. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/**`, pruebas focalizadas | Propietario de consulta, autorización y agregación. |
| Database | No | N/A | No cambian entidades, tablas, columnas ni relaciones. |
| Contrato HTTP | Sí | `GET /api/v1/dashboard/portfolio`, DTO y OpenAPI generado | Backend canónico; cliente Angular se regenera después. |
| Documentación | Sí | `docs/funcional/guia-funcional-piip.md` | Actualizar el recorrido de Inicio durante la implementación. |

## Contexto técnico

**Lenguajes/versiones**: Java 21, Spring Boot 4.1, Angular 22 y TypeScript.

**Dependencias principales**: Spring MVC, Spring Data JPA/Criteria API, Hibernate, cliente Angular generado desde OpenAPI, signals y componentes standalone existentes.

**Persistencia**: Hibernate JPA sobre Oracle, sin SQL nativo, `JdbcTemplate`, procedimientos, Flyway ni Liquibase.

**Validación propuesta**: pruebas MVC, aplicación y persistencia; pruebas del repositorio Angular, Inicio y AppShell; sincronización OpenAPI; typecheck, suites y build. No se ejecutan en planificación y las acciones restringidas requieren autorización explícita.

**Plataforma objetivo**: aplicación web PIIP autenticada con Keycloak y autorización funcional Oracle.

**Restricciones**: UE activa obligatoria y autorizada; notificaciones del destinatario; etiquetas canónicas; orden estable; sin datos ficticios, lectura automática, nuevos estados, transiciones, tareas ni enlaces contextuales de notificación.

**Escala/alcance**: cinco registros por página; backend admite tamaño entre 1 y 100, pero Inicio solicita siempre cinco. Página y agregados se resuelven en base de datos sin cargar el portafolio completo.

## Verificación de la constitución

### Gate inicial

- **I. Fuente funcional**: aprobado. La feature consulta registros y catálogos existentes sin inferir obligatoriedades.
- **II. Estados y transiciones**: aprobado. Los once estados son consultables; no se agregan ni autorizan transiciones.
- **III. Organización y seguridad**: aprobado. Se exige UE activa legible y las notificaciones conservan destinatario individual.
- **IV. Persistencia**: aprobado. La consulta usa Hibernate JPA y no modifica el esquema.
- **V. Trazabilidad y calidad**: aprobado. No hay nuevas escrituras de negocio; la lectura explícita vigente conserva su auditoría y se proponen pruebas focalizadas.
- **Grounding y contrato**: aprobado. Se inspeccionó implementación real y backend precede a OpenAPI y cliente Angular.

### Gate posterior al diseño

Aprobado sin excepciones. El contrato es aditivo, las capas están separadas, página y conteos comparten predicados, la UI conserva lectura explícita y no quedan `NEEDS CLARIFICATION` bloqueantes.

## Dependencias y secuencia

- **Propietario canónico**: backend para contrato, autorización, orden global, página y conteos.
- **Consumidores**: OpenAPI, cliente Angular generado, repositorio de presentación, Inicio y AppShell.
- **Orden obligatorio**: consulta backend → pruebas backend → generación OpenAPI autorizada → cliente Angular → repositorio/UI → guía funcional → validación integral autorizada.
- **Paralelización permitida**: documentación y pruebas de presentación pueden prepararse cuando el contrato esté estabilizado; no paralelizar propietario y consumidor del mismo contrato.

## Estructura del proyecto

### Documentación de la feature

```text
specs/010-actualizar-inicio-piip/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── home-portfolio.openapi.yaml
└── tasks.md                         # Se generará mediante /speckit-tasks
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/dashboard/
├── api/
│   ├── DashboardController.java
│   └── DashboardDtos.java
├── application/
│   └── DashboardPortfolioService.java
└── persistence/
    └── DashboardPortfolioQueryRepository.java

apps/backend/src/test/java/pe/gob/midagri/piip/dashboard/
├── api/DashboardPortfolioControllerTest.java
├── application/DashboardPortfolioServiceTest.java
└── persistence/DashboardPortfolioQueryRepositoryTest.java

apps/frontend/src/app/
├── core/
│   ├── piip.repository.ts
│   ├── piip-http.repository.ts
│   ├── piip-mock.repository.ts
│   └── piip.models.ts
├── pages/dashboard/
│   ├── dashboard.component.ts
│   ├── dashboard.component.html
│   ├── dashboard.component.scss
│   └── dashboard.component.spec.ts
└── layout/
    ├── app-shell.component.ts
    ├── app-shell.component.html
    └── app-shell.component.spec.ts

docs/funcional/guia-funcional-piip.md
```

**Decisión de estructura**: `DashboardPortfolioService` aplica autorización, normaliza filtros/página y compone la respuesta; un repositorio de consulta focalizado centraliza los predicados JPA de página, agrupación y conteo base. El controlador solo enlaza HTTP. En Angular, el repositorio adapta el contrato, Inicio gobierna filtros/estados visuales y AppShell presenta el badge y coordina el foco.

## Diseño por responsabilidad

### Consulta unificada backend

- Agregar `GET /api/v1/dashboard/portfolio` sin modificar `/dashboard`, `/initiatives` ni `/projects`.
- Exigir `executingUnitId`; aceptar `q`, `type`, `status`, `page` y `size`. `type` ausente representa Todos; `page=0`; `size=5`, máximo 100.
- Validar `LocalAuthorizationService.requireReadableUnit(executingUnitId)` antes de consultar; una UE fuera del alcance produce 403.
- Buscar solo por código o nombre y reutilizar un conjunto de predicados para UE, búsqueda, tipo y estado.
- Ordenar por `updatedAt DESC, id DESC`; el identificador es un desempate técnico estable.
- Consultar página, agrupación positiva por estado y total base de la UE mediante JPA. `executingUnitTotalElements` distingue vacío real de filtros sin coincidencias.
- Derivar `totalElements` de la suma de `statusCounts` y normalizar páginas fuera de rango a 0.

### Contrato y modelos

- `HomePortfolioResponse`: `content`, `page`, `size`, `totalElements`, `totalPages`, `executingUnitTotalElements`, `statusCounts`.
- `HomePortfolioItemResponse`: `recordType`, `code`, `name`, `status`, `executingUnitId`, `executingUnit`, `updatedAt`.
- `PortfolioStatusCountResponse`: `status`, `count`.
- Parámetros `type` y `status` usan códigos enum; las respuestas usan etiquetas canónicas.
- Invariantes: solo conteos positivos, suma igual al total filtrado, UE exacta y ningún DTO expone JPA.

### Presentación Angular

- Añadir `HomePortfolioQuery`, `HomePortfolioItem`, `HomePortfolioResult` y `PortfolioStatusCount` y mapearlos desde el cliente generado.
- Inicio deja de consumir métricas del dashboard legado; listados completos y modelo de tareas permanecen sin cambios.
- Mantener búsqueda con debounce de 300 ms, tipo, estado y página. Cualquier cambio de filtro o UE reinicia la página.
- Derivar opciones de estado de los catálogos existentes. Si el tipo invalida el estado seleccionado, restablecer `Estado: Todos` y reiniciar la página.
- Renderizar tabla, indicadores y barras desde una respuesta y reutilizar `PiipPaginationComponent` con exactamente cinco registros por página.
- Separar `loading`, `error`, `emptyPortfolio` y `noResults`; descartar respuestas obsoletas ante cambios rápidos.
- Resolver `Ver detalle` por tipo real y conservar `/iniciativas` y `/proyectos`.

### Notificaciones y accesibilidad

- Reutilizar la lista personal ordenada por fecha. El resumen compacto muestra exactamente las tres notificaciones más recientes; `Ver todas` expande en línea la lista completa de la pestaña activa.
- Mantener pestañas `Todas` y `No leídas`; el badge se deriva de la lista personal, no del dashboard legado.
- `Marcar como leída` actúa por fila, deshabilita solo esa acción y actualiza únicamente el aviso confirmado.
- La campana muestra el total exacto; al activarla navega a `/inicio` si hace falta y enfoca `#mis-notificaciones`, sin cambiar lecturas.
- Añadir nombres accesibles, pestañas semánticas, `aria-live`, `aria-busy`, foco visible, `aria-expanded` y alternativa textual para la distribución.
- Portafolio y notificaciones tienen cargas, errores y reintentos independientes.

## Estrategia de verificación propuesta

1. **Backend API**: bindings, defaults, DTO, 400 para enums inválidos y 403 para UE no autorizada.
2. **Aplicación**: UE exacta, búsqueda código/nombre, filtros, conteos positivos, totales, vacío, sin resultados y página fuera de rango.
3. **Persistencia**: mezcla global por `updatedAt DESC, id DESC`, cruces entre páginas, agrupación y estados de feature 009.
4. **Regresión backend**: `/initiatives`, `/projects`, `/dashboard` y notificaciones permanecen compatibles.
5. **Frontend repositorio**: mapeo, parámetros y descarte de respuestas obsoletas.
6. **Inicio/AppShell**: cinco filas, filtros, reseteo de estado, paginación, navegación, tres avisos compactos, expansión, lectura, badge y foco.
7. **Accesibilidad**: teclado, foco, anuncios y alternativa textual del gráfico.
8. **Contrato/integración**: generación OpenAPI, cliente, typecheck, suites y build, solo con autorización.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: tareas, prioridades, vencimientos, avance físico, disparadores/destinatarios, referencias contextuales, estados/transiciones, módulos completos, esquema Oracle y contrato legado `/dashboard`.
- **Specs `001`-`005` consultadas**: Ninguna; no son backlog de esta feature.
- **Dependencias históricas aprobadas**: estados y transiciones ratificados por feature 009 y constitución 1.1.0; solo se consultan.
- **NEEDS CLARIFICATION**: Ninguna.

## Seguimiento de complejidad

No existen contradicciones ni excepciones constitucionales aprobadas para este diseño.
