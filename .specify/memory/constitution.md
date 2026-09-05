<!--
Sync Impact Report
- Cambio de versión: 1.2.0 -> 1.3.0
- Principios modificados: IV. Persistencia
- Secciones agregadas: ninguna
- Secciones eliminadas: ninguna
- Artefactos alineados: spec.md, plan.md y tasks.md de la feature 015
- Templates actualizados: ninguno; la regla se expresa en el principio IV
- Templates revisados sin cambios: plan-template.md, spec-template.md,
  tasks-template.md y sus overrides
- Documentación revisada sin cambios: docs/development/spec-kit-adoption.md,
  README.md y AGENTS.md
- Comandos revisados: .specify/templates/commands/ no existe en este checkout
- Pendientes: sincronizar la documentación operativa y la implementación de la
  feature 015 con la nueva autorización por perfiles exactos
-->

# Constitución PIIP

## I. Fuente funcional

Los 23 campos y seis catálogos del Excel PIIP son la fuente funcional de v1. Una observación de la carga no se convierte en obligatoriedad ni transición.

## II. Estados y transiciones

Los once estados oficiales son consultables, pero su presencia en el catálogo no autoriza
transiciones de escritura. El flujo base confirmado conserva
`Presentado -> Iniciativa aprobada`; aprobar no crea automáticamente un proyecto y el proyecto
derivado nace, mediante una operación separada, en `Proyecto en ejecución`.

Para la feature 009 se ratifican exclusivamente estas transiciones de iniciativa mientras no
exista un proyecto vinculado: `Presentado -> Iniciativa aprobada`,
`Presentado -> No Admisible`, `Presentado -> Iniciativa archivada` e
`Iniciativa aprobada -> Iniciativa archivada`. Una iniciativa con proyecto vinculado conserva
`Iniciativa aprobada` y no admite cambios de estado.

Para la misma feature se ratifican exclusivamente estas transiciones de proyecto:
`Proyecto en ejecución -> Producto aprobado`,
`Proyecto en ejecución -> Producto no aprobado`, `Proyecto en ejecución -> Suspendido`,
`Proyecto en ejecución -> Cancelado`, `Suspendido -> Proyecto en ejecución`,
`Suspendido -> Cancelado`, `Producto no aprobado -> Proyecto en ejecución`,
`Producto no aprobado -> Cancelado` y `Producto aprobado -> Finalizado`.
`No Aplicable` permanece excluido. Cualquier otra transición requiere una especificación
explícita y una nueva revisión constitucional.

## III. Organización y seguridad

Cada registro pertenece a una Unidad Ejecutora y sus unidades orgánicas responsables pertenecen al mismo ámbito. Keycloak autentica; Oracle autoriza con Administrador PIIP y Consulta externa.

## IV. Persistencia

Hibernate JPA es la fuente canónica del esquema Oracle. Se prohíben SQL nativo,
`JdbcTemplate`, procedimientos almacenados, Flyway y Liquibase para el acceso funcional y la
definición estructural permanente. Como única excepción, un perfil destructivo explícito y
exclusivo de desarrollo o pruebas PUEDE ejecutar un archivo externo versionado con DML de datos
iniciales, siempre que no contenga DDL, sea idempotente, permanezca deshabilitado por defecto y
valide de forma fail-closed la activación exacta y ordenada de los perfiles
`test,test-reset`, la conexión y el esquema antes de escribir. Esa activación
explícita sustituye cualquier variable adicional de habilitación o confirmación.
Esta excepción NO PUEDE habilitarse en operación normal ni en producción. Los binarios
documentales se almacenan como BLOB separado de sus metadatos.

## V. Trazabilidad y calidad

Durante la operación normal, las escrituras generan eventos append-only y las llamadas API
generan auditoría de acceso sin cuerpos ni secretos. Un perfil destructivo exclusivo de desarrollo
o pruebas PUEDE eliminar y recrear por completo las tablas de auditoría y descartar su contenido,
siempre que cumpla las guardias fail-closed del principio IV y no pueda activarse en producción.
Los cambios relevantes requieren pruebas automatizadas.

**Versión:** 1.3.0

**Ratificada:** 2026-07-28

**Última enmienda:** 2026-09-05
