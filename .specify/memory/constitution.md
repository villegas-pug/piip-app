<!--
Sync Impact Report
- Cambio de versión: 1.0.0 -> 1.1.0
- Principio modificado: II. Estados y transiciones (ratificación de la feature 009)
- Secciones agregadas: ninguna
- Secciones eliminadas: ninguna
- Artefactos alineados: AGENTS.md y specs/009-ciclo-vida-portafolio/plan.md
- Templates revisados sin cambios: plan-template.md, spec-template.md y tasks-template.md
- Pendientes: ninguno
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

Hibernate JPA es la fuente canónica del esquema Oracle. Se prohíbe SQL nativo. Los binarios documentales se almacenan como BLOB separado de sus metadatos.

## V. Trazabilidad y calidad

Las escrituras generan eventos append-only. Las llamadas API generan auditoría de acceso sin cuerpos ni secretos. Los cambios relevantes requieren pruebas automatizadas.

**Versión:** 1.1.0  
**Ratificada:** 2026-07-28  
**Última enmienda:** 2026-08-18
