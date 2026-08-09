---
description: "Lista trazable de tareas para una feature nueva del monorepo PIIP"
---

# Tareas: [FEATURE NAME]

**Entrada**: documentos de `/specs/[###-feature-name]/`

**Prerrequisitos**: `spec.md`, `plan.md` y `tasks.md` vigentes; documentos de diseño adicionales cuando existan; sin checklists ni `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI ni integración Oracle. La invocación explícita de `/speckit-implement` autoriza las tareas de implementación de la feature activa y aprueba sus artefactos vigentes; las demás acciones requieren autorización separada.

## Formato obligatorio

Cada tarea usa:

`- [ ] T### [P?] [US# y/o FR-###] Acción concreta en ruta/archivo exacto`

- **[P]**: puede ejecutarse en paralelo porque no comparte archivos ni una dependencia pendiente.
- **[US#] / [FR-###]**: toda tarea se vincula con al menos una historia o requisito de la spec.
- La descripción incluye una ruta real del monorepo; no se aceptan ubicaciones genéricas.
- `[X]` se utiliza solo después de obtener evidencia de que el cambio fue realizado.

## Evidencia de baseline — no ejecutable

<!--
  Registrar capacidades ya satisfechas. No usar checkboxes, IDs T### ni presentar
  este contenido como trabajo completado durante la nueva feature.
-->

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|--------------------|------------------|-------------------|--------------|
| [US#/FR-###] | [Comportamiento existente] | `[ruta real]` | [Reutilizar o mantener] |

## Phase 1: Preparación necesaria

**Propósito**: cambios mínimos compartidos que la feature realmente necesita. Eliminar esta fase si no aplica.

- [ ] T001 [US#/FR-###] [Acción concreta] en `[ruta real]`

---

## Phase 2: Contrato o fundamento bloqueante

**Propósito**: establecer primero el propietario canónico cuando exista una dependencia compartida. Eliminar esta fase si no aplica.

- [ ] T002 [US#/FR-###] [Modificar el contrato o fundamento canónico] en `[ruta real del propietario]`
- [ ] T003 [US#/FR-###] [Publicar o documentar el artefacto consumible] en `[ruta real]` (depende de T002)

**Checkpoint**: el contrato o fundamento está definido antes de modificar consumidores.

---

## Phase 3: Backend — [US#/FR-###]

**Objetivo**: [resultado backend independiente]

- [ ] T004 [US#/FR-###] [Acción backend concreta] en `apps/backend/[ruta real]`

**Checkpoint**: [evidencia esperada sin afirmar que las validaciones fueron ejecutadas].

---

## Phase 4: Frontend — [US#/FR-###]

**Objetivo**: [resultado frontend independiente]

- [ ] T005 [US#/FR-###] [Adaptar el consumidor después del contrato, si aplica] en `apps/frontend/[ruta real]` (depende de T003 cuando corresponda)

**Checkpoint**: [evidencia esperada sin afirmar que las validaciones fueron ejecutadas].

---

## Phase 5: Historias adicionales

<!--
  Crear una fase por historia priorizada. Mantener backend y frontend separados
  cuando el orden contractual importe. Eliminar ejemplos y fases sin trabajo real.
-->

- [ ] T006 [P] [US#/FR-###] [Acción concreta] en `[ruta real]`

---

## Phase N: Documentación y cierre

- [ ] TXXX [US#/FR-###] [Actualizar documentación afectada] en `docs/[ruta real]`
- [ ] TXXX [US#/FR-###] [Registrar tareas completadas, pendientes y validaciones realmente ejecutadas] en `specs/[###-feature-name]/[archivo real]`

## Validaciones propuestas — requieren autorización

<!--
  Enumerar solo las validaciones pertinentes. No marcarlas [X] sin haber recibido
  autorización explícita en el turno y sin conservar evidencia del resultado.
-->

- [ ] TXXX [US#/FR-###] [Validación focalizada] desde `[directorio exacto]` con `[comando exacto]` — autorización requerida

## Dependencias y orden de ejecución

- **Propietario canónico**: [tareas y área propietaria o N/A]
- **Consumidores**: [tareas dependientes o N/A]
- **Orden obligatorio**: [IDs y justificación]
- **Oportunidades paralelas**: [IDs [P] sin archivos ni contratos compartidos]

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- No crear tareas a partir de pendientes históricos por arrastre.
- Si la feature depende directamente de un requisito histórico, registrar aquí el requisito, la dependencia y su aprobación explícita: [referencia o Ninguna].
- Registrar contradicciones sin resolver como `NEEDS CLARIFICATION`: [detalle o Ninguna].

## Notas

- Cada tarea representa trabajo nuevo aprobado y apunta a una ruta real.
- No reimplementar capacidades incluidas en la evidencia de baseline.
- No paralelizar cambios que compartan contrato, catálogo, regla funcional, documentación o configuración.
- El cierre distingue cambios realizados, tareas pendientes y validaciones no ejecutadas.
