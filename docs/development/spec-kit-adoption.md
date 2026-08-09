# Adopción incremental de Spec Kit

## Propósito

Este protocolo conecta Spec Kit con el estado real del monorepo PIIP sin convertir documentación histórica en trabajo pendiente. Se aplica únicamente a features nuevas, comenzando por `specs/006-nombre-feature/`.

Las especificaciones `001` a `005` son antecedentes funcionales e históricos. No son backlog, no bloquean nuevas features y no deben completarse retroactivamente con `plan.md` o `tasks.md`.

## Flujo obligatorio

### 1. Grounding previo

Antes de crear una especificación se debe inspeccionar el código, la arquitectura, los contratos y la documentación relacionados con la solicitud. El resultado debe identificar:

- comportamiento existente que constituye el baseline;
- rutas reales potencialmente afectadas;
- impacto en frontend, backend, database, contrato HTTP y documentación;
- propietario canónico de cualquier contrato compartido;
- dependencias y orden entre áreas.

Las specs `001` a `005` se consultan solo como antecedentes del dominio. Si contradicen el código o una fuente vigente, se registra `NEEDS CLARIFICATION`; no se ajusta el código silenciosamente ni se amplía el alcance.

### 2. Especificación

Cada feature nueva se crea en `specs/006-nombre-feature/` o en el siguiente número secuencial disponible. Su `spec.md` debe contener historias priorizadas, requisitos identificados, criterios medibles, límites explícitos y la clasificación de impacto del monorepo.

Un requisito histórico solo puede incorporarse cuando la nueva feature lo modifica directamente y el usuario aprueba esa dependencia. Lo que el código ya satisface se documenta como evidencia del baseline, no como requisito pendiente ni como tarea marcada artificialmente como completada.

### 3. Plan conectado al monorepo

El `plan.md` debe usar rutas y componentes existentes, declarar el impacto por área y respetar `.specify/memory/constitution.md`. Cuando frontend y backend compartan un contrato, el plan identifica al propietario canónico y ordena primero su cambio; el consumidor se adapta después.

Toda contradicción sin resolución se mantiene como `NEEDS CLARIFICATION`. La implementación no puede utilizar una spec histórica para introducir trabajo fuera del alcance aprobado.

### 4. Tareas trazables

El `tasks.md` contiene IDs, checkboxes, rutas concretas y vínculo con al menos una historia o requisito. Debe:

- separar trabajo frontend y backend;
- ordenar dependencias de contrato antes que consumidores;
- incluir exclusivamente trabajo nuevo aprobado;
- mantener la evidencia del baseline fuera del checklist ejecutable;
- distinguir las validaciones propuestas de las validaciones autorizadas.

Las tareas no pueden importar pendientes de `001` a `005` salvo dependencia directa, explícita y aprobada.

### 5. Aprobación e implementación

El ciclo recomendado es `specify -> clarify -> plan -> tasks -> analyze -> implement`. No se ejecuta `implement` hasta que la feature activa tenga `spec.md`, `plan.md` y `tasks.md`, sin checklists ni `NEEDS CLARIFICATION` bloqueantes, y el usuario invoque explícitamente `/speckit-implement` en el turno actual. Esa invocación constituye la aprobación de los artefactos vigentes; no requiere un mensaje de aprobación separado.

La implementación se limita a las tareas aprobadas. Un checkbox cambia a `[X]` únicamente con evidencia del cambio correspondiente. Las pruebas, builds, generación OpenAPI e integración Oracle requieren autorización explícita del usuario en el turno en que se pretendan ejecutar.

## Cierre de una feature

El cierre debe informar tareas completadas y pendientes, cambios realmente realizados, validaciones realmente ejecutadas y cualquier riesgo o `NEEDS CLARIFICATION` todavía abierto. No debe afirmar que una validación pasó si no se ejecutó.
