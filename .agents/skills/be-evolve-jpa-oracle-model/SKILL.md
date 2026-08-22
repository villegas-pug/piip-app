---
name: be-evolve-jpa-oracle-model
description: Evolucionar el modelo JPA y el esquema Oracle derivado para los campos y catálogos definidos por la arquitectura PIIP. Usar siempre ante solicitudes como "agrega o cambia un campo", "modifica una entidad o relación", "crea una restricción", "ajusta un catálogo", "cambia un repositorio" o "actualiza el esquema o DDL" dentro de `apps/backend`. No fijar en el triggering cantidades de campos o catálogos que puedan cambiar.
---

# Evolucionar modelo JPA Oracle

## Validar la fuente funcional

1. Contrastar el cambio con `docs/architecture/piip-fields.md`, la constitución y la especificación activa. Esto evita convertir la fotografía actual del modelo en una regla permanente.
2. Distinguir `NA` de `No aplica` y separar institución, unidad ejecutora y unidad orgánica, porque fusionar esos conceptos altera el significado funcional de los datos.
3. No convertir observaciones o datos de ejemplo en obligatoriedades, estados o restricciones no confirmadas. Los ejemplos describen casos observados, no reglas aprobadas.

## Mantener el modelo canónico

1. Mantener entidades, asociaciones, constraints e índices JPA como fuente canónica del esquema. Introducir una segunda fuente de definición provocaría divergencias entre persistencia y código.
2. Usar repositorios Spring Data y JPQL cuando corresponda. No usar SQL nativo, `JdbcTemplate`, procedimientos, Flyway ni Liquibase, porque romperían la estrategia de persistencia definida para PIIP.
3. Evaluar carga, cardinalidad, índices derivados y concurrencia sin introducir optimizaciones especulativas. Una optimización sin evidencia puede aumentar el acoplamiento o imponer restricciones incorrectas.

## Preservar perfiles y DDL derivado

1. Mantener `ddl-auto=validate` en runtime ordinario y producción, `create-drop` solo en pruebas aisladas y `ddl-auto=none` en `test-reset`, donde el reset destructivo permanece fail-closed, allowlisted y prohibido en producción.
2. Tratar la copia versionada del DDL como derivada de JPA. No editarla a mano ni declarar freshness por existencia: registrar commit fuente, mecanismo de generación autorizado y comparación con el modelo vigente.
3. Si el DDL derivado está fuera de `apps/backend/**`, entregar al agente principal o DBA propietario el cambio JPA, el impacto esperado y la evidencia necesaria para regenerar y revisar.

## Límites de alcance y ejecución

1. Modificar únicamente `apps/backend/**`. La copia versionada del DDL queda bajo control del agente principal o DBA para evitar que el especialista atraviese su scope.
2. No ejecutar generación de esquema, integración Oracle ni borrados sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Fuente funcional y especificación utilizadas.
- Entidades, asociaciones, restricciones y repositorios afectados.
- Justificación de cada cambio respecto del modelo vigente.
- Impacto esperado, perfil afectado, ownership y evidencia de freshness requeridos para la copia versionada del DDL.
- Comandos ejecutados y resultados, o comandos pendientes de autorización.
- Contradicciones o reglas ausentes como `NEEDS CLARIFICATION`.
