# Guía de ejecución y cierre: Ciclo de vida del portafolio PIIP

Este documento distingue la implementación realizada de las validaciones que aún requieren autorización. La invocación de `/speckit-implement` autorizó las tareas de producto, pero no autoriza por sí misma generar OpenAPI, ejecutar pruebas, compilar, iniciar servicios ni conectarse a Oracle.

## 1. Precondiciones Spec Kit

1. Revisar todos los artefactos de esta carpeta.
2. Generar `tasks.md` mediante `/speckit-tasks`.
3. El pendiente documental no bloquea v1 porque ningún documento es precondición.
4. Invocar explícitamente `/speckit-implement` para autorizar solo las tareas vigentes.
5. Solicitar por separado autorización para pruebas, builds, OpenAPI, servidores u Oracle.

## 2. Orden de implementación

1. Reloj y comportamientos de dominio.
2. Lectura JPA bloqueante y coordinación con `createDerived(...)`.
3. Casos de uso, auditoría, DTO y controlador.
4. Pruebas backend y contrato, sin ejecutarlas aún.
5. Con autorización, generar OpenAPI y revisar diff.
6. Regenerar cliente Angular y adaptar repositorio.
7. Implementar detalles, acciones, filtros, navegación y auditoría visible.
8. Actualizar `docs/funcional/guia-funcional-piip.md`.
9. Ejecutar solo validaciones autorizadas.

## 3. Estado de esta entrega

- Implementado el reloj de `America/Lima`, las matrices de dominio, el bloqueo pesimista de la iniciativa origen, los DTO separados, los servicios transaccionales y las rutas HTTP de iniciativa y proyecto.
- Escritas y ejecutadas pruebas unitarias focalizadas para matrices, estados terminales, cierre, documentos pendientes y autorización.
- Añadidas y ejecutadas la regresión del flujo existente, las pruebas MVC de `400/403/404/409/422`, la concurrencia JPA sin mocks, el rollback/auditoría transaccional y la presentación de eventos de cambio de estado.
- Añadidos los modelos/repositorio de transición, el detalle general de proyecto, navegación desde el listado, acciones de iniciativa y pruebas de componentes; el cliente generado fue regenerado desde el contrato vigente.
- Actualizada la guía funcional y reconciliado el contrato OpenAPI de diseño.
- `T018`, `T019`, `T021`, `T022`, `T029` y `T038` se completaron con evidencia de ejecución; no se editó manualmente `api/generated/`.
- `T039` se ejecutó, pero permanece abierta porque la suite frontend terminó con dos fallos baseline ajenos a esta feature: la notificación de salida de Administración de usuarios y la etiqueta de asignación suspendida.
- No se modificó el esquema lógico ni se creó un segundo mecanismo de versionado.

## 4. Comandos ejecutados y resultado

```powershell
cd apps/backend
./gradlew.bat test --tests "pe.gob.midagri.piip.portfolio.*"
./gradlew.bat test --tests "pe.gob.midagri.piip.contract.OpenApiGenerationTest"
```

Resultado registrado: ambas ejecuciones backend terminaron con `BUILD SUCCESSFUL`.

La generación vigente produce `apps/backend/target/piip-openapi.json`.

```powershell
cd apps/frontend
npm run api:generate
npm test -- --watch=false
```

`npm run api:generate` terminó correctamente. La suite frontend ejecutó 26 archivos y 133 pruebas: 24 archivos y 131 pruebas pasaron; fallaron únicamente `app-shell.component.spec.ts` (notificación al salir de Administración de usuarios) y `user-administration.component.spec.ts` (texto esperado de asignación suspendida), ambos fuera del alcance de la feature 009.

La integración Oracle es opcional, también requiere autorización y un entorno válido:

```powershell
cd apps/backend
./gradlew.bat integrationTest
```

Nunca se deben exponer wallets, tokens ni credenciales.

## 5. Casos mínimos de aceptación

- Registrar → aprobar por operación existente → crear derivado conserva `Presentado`, `Iniciativa aprobada` y `Proyecto en ejecución`.
- Una iniciativa vinculada permanece aprobada y rechaza toda acción.
- Se aceptan exactamente las 3 aristas nuevas de iniciativa y 9 de proyecto.
- Estados del contexto contrario y `No Aplicable` se rechazan.
- Los listados solo filtran; los detalles confirman transiciones.
- Una segunda mutación con la misma versión recibe `409`.
- Archivado y derivación concurrentes nunca dejan una combinación inválida.
- Una falla de auditoría revierte estado, cierre y versión.
- `Finalizado` usa la fecha de `America/Lima`; otros destinos conservan `closingDate`.
- Los documentos pendientes nunca bloquean v1.

## 6. Evidencia de cierre esperada

- Diff limitado a las rutas previstas.
- OpenAPI y cliente sincronizados si su generación fue autorizada.
- Resultados de validaciones autorizadas con comando y entorno.
- Guía funcional actualizada con flujo cronológico y contextos separados.
- Confirmación de que no cambió el esquema lógico ni el versionado.
