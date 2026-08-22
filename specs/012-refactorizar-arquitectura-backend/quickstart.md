# Quickstart de implementación supervisada

## Propósito

Guía para ejecutar la futura refactorización por incrementos sin ampliar el alcance. Este documento no autoriza implementación ni comandos restringidos; `/speckit-implement` y las autorizaciones adicionales de `AGENTS.md` siguen siendo obligatorios.

## Preflight documental

1. Confirmar que `.specify/feature.json` apunta a `specs/012-refactorizar-arquitectura-backend`.
2. Leer `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/http-compatibility.md` y `tasks.md` cuando exista.
3. Confirmar ausencia de checklists incompletas o `NEEDS CLARIFICATION` bloqueantes.
4. Tratar cambios de producto ya presentes como baseline; no marcarlos como trabajo ejecutado de la feature.
5. Verificar que el turno actual contiene `/speckit-implement` antes de modificar `apps/backend/**`.
6. Solicitar autorización separada antes de pruebas, builds, servidores, contenedores, OpenAPI, Oracle o Git.

## Ciclo por incremento

1. Elegir un único incremento de `plan.md` y registrar rutas, endpoint, caso de uso, dependencias y criterio de reversión.
2. Proponer o escribir primero la caracterización focalizada del comportamiento vigente.
3. Mover la frontera con el cambio mínimo, conservando DTO HTTP, mensajes, orden y efectos.
4. Realizar comprobaciones estáticas autorizadas: imports, anotaciones, dependencias de capas, placeholders y diff de rutas protegidas.
5. Si el usuario autoriza pruebas, ejecutar únicamente las suites focalizadas del incremento y registrar el resultado real.
6. Comparar con `contracts/http-compatibility.md`; si existe diferencia no explicada, no iniciar el siguiente incremento.
7. Actualizar trazabilidad y checkpoint no verificado. El cierre canónico del Vault requiere aprobación separada.

## Orden operativo

```text
Baseline
  -> shared errors + guards
  -> organization views/queries
  -> audit + current identity
  -> documents
  -> work + notifications
  -> dashboard summary
  -> portfolio
  -> architecture documentation + final verification
```

## Checklist de cada entrega

- [ ] Controller limitado a binding, validación, delegación y respuesta HTTP.
- [ ] Transacción ubicada en application.
- [ ] Autorización evaluada con grants exactos dentro del caso de uso.
- [ ] Ninguna entidad JPA cruza hacia API.
- [ ] Ningún modelo compartido pertenece a un controller.
- [ ] Error interno tipado y `ProblemDetail` equivalente.
- [ ] Atomicidad de mutación, tareas, notificaciones y auditoría preservada.
- [ ] Valores deterministas idénticos y variables bajo la misma regla.
- [ ] Cero cambios en frontend, OpenAPI, cliente generado, JPA, DDL y Oracle.
- [ ] Riesgo, evidencia y reversión documentados antes del siguiente incremento.

## Validaciones propuestas, no ejecutadas

Con autorización explícita futura, las validaciones focalizadas seguirán este orden conceptual:

1. pruebas MVC/contrato del módulo;
2. pruebas unitarias de application y autorización;
3. pruebas de persistencia/atomicidad/concurrencia existentes cuando apliquen;
4. reglas de arquitectura;
5. suite backend completa;
6. comprobación de compatibilidad del consumidor frontend sin regenerar contrato.

No ejecutar por defecto `gradlew.bat test`, `gradlew.bat check`, `gradlew.bat integrationTest`, generación OpenAPI, servidor Spring, Docker ni Oracle.

## Señales de detención

Detener el incremento y registrar el bloqueo si ocurre cualquiera de estos casos:

- se necesita cambiar un endpoint, DTO, status o mensaje externo;
- una regla vigente no puede confirmarse en código/spec/documentación;
- la separación exige modificar una entidad, esquema o dato;
- una transacción atómica tendría que dividirse;
- aparecen permisos o transiciones no autorizados;
- un módulo conforme tendría que reescribirse por uniformidad;
- una prueba propuesta revela una diferencia funcional no explicada.

La salida correcta es revertir o abrir una nueva aclaración/feature, no inventar una regla ni normalizar la diferencia.
