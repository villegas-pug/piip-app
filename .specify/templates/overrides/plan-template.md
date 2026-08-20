# Plan de implementación: [FEATURE]

**Rama**: `[###-feature-name]` | **Fecha**: [DATE] | **Spec**: [link]

**Entrada**: especificación en `/specs/[###-feature-name]/spec.md`

**Nota**: esta plantilla se completa mediante `/speckit-plan`. El plan debe partir del estado real del monorepo y del protocolo `docs/development/spec-kit-adoption.md`.

## Resumen

[Requisito principal, resultado esperado y enfoque técnico basado en evidencia del repositorio]

## Baseline y evidencia existente

<!--
  Registrar comportamiento ya satisfecho, rutas inspeccionadas, contratos y
  documentación vigente. Esta sección no genera tareas ni convierte trabajo
  histórico en trabajo pendiente.
-->

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| [Comportamiento o decisión existente] | `[ruta real]` | [Reutilizar, adaptar o mantener] |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | [Sí/No] | [`apps/frontend/...` o N/A] | [Consumidor, independiente o N/A] |
| Backend | [Sí/No] | [`apps/backend/...` o N/A] | [Propietario, independiente o N/A] |
| Database | [Sí/No] | [`database/...` o N/A] | [Derivada de JPA, revisión o N/A] |
| Contrato HTTP | [Sí/No] | [endpoint/DTO/OpenAPI o N/A] | [Propietario canónico y consumidores] |
| Documentación | [Sí/No] | [`docs/...` o N/A] | [Fuente que debe actualizarse] |

## Contexto técnico

**Lenguajes/versiones**: [Java 21, Spring Boot 4.1, Angular 22 u otros aplicables]

**Dependencias principales**: [dependencias existentes relevantes o NEEDS CLARIFICATION]

**Persistencia**: [Hibernate JPA/Oracle, N/A o NEEDS CLARIFICATION]

**Validación propuesta**: [pruebas y builds aplicables; su ejecución requiere autorización explícita]

**Plataforma objetivo**: [entorno aplicable o NEEDS CLARIFICATION]

**Restricciones**: [restricciones funcionales, técnicas, seguridad y compatibilidad]

**Escala/alcance**: [alcance medible de la feature o NEEDS CLARIFICATION]

## Verificación de la constitución

*GATE: debe aprobarse antes del diseño y volver a revisarse al finalizarlo.*

[Evidencia de cumplimiento de `.specify/memory/constitution.md`. Toda contradicción no resuelta se marca NEEDS CLARIFICATION. Si se usa la excepción de DML inicial o de reset destructivo de auditoría, documentar perfil exclusivo, guardias fail-closed, prohibición productiva, alcance exacto y verificaciones propuestas.]

## Dependencias y secuencia

- **Propietario canónico**: [área que define el contrato o N/A]
- **Consumidores**: [áreas dependientes o N/A]
- **Orden obligatorio**: [propietario antes que consumidor; o trabajo independiente]
- **Paralelización permitida**: [solo archivos y áreas sin dependencias compartidas]

## Estructura del proyecto

### Documentación de la feature

```text
specs/[###-feature]/
├── spec.md
├── plan.md
├── research.md          # Solo si el plan lo requiere
├── data-model.md        # Solo si el plan lo requiere
├── quickstart.md        # Solo si el plan lo requiere
├── contracts/           # Solo si cambia o documenta contratos
└── tasks.md
```

### Código y documentación afectados

```text
[Incluir únicamente rutas reales del monorepo relacionadas con la feature.
Eliminar áreas sin impacto.]
```

**Decisión de estructura**: [responsabilidades, capas y rutas elegidas]

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: [comportamientos y rutas que no se modificarán]
- **Specs `001`-`005` consultadas**: [referencias concretas o Ninguna]
- **Dependencias históricas aprobadas**: [requisito y aprobación o Ninguna]
- **NEEDS CLARIFICATION**: [contradicciones o Ninguna]

## Seguimiento de complejidad

> Completar solo si una decisión contradice la constitución y ha sido aprobada y justificada.

| Contradicción | Necesidad | Alternativa más simple descartada porque |
|---------------|-----------|------------------------------------------|
| [Decisión] | [Motivo verificable] | [Razón] |
