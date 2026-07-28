# Constitución PIIP

## I. Fuente funcional

Los 23 campos y seis catálogos del Excel PIIP son la fuente funcional de v1. Una observación de la carga no se convierte en obligatoriedad ni transición.

## II. Estados y transiciones

Los once estados oficiales son consultables. La única transición de escritura confirmada es `Presentado -> Iniciativa aprobada`. Aprobar no crea automáticamente un proyecto.

## III. Organización y seguridad

Cada registro pertenece a una Unidad Ejecutora y sus unidades orgánicas responsables pertenecen al mismo ámbito. Keycloak autentica; Oracle autoriza con Administrador PIIP y Consulta externa.

## IV. Persistencia

Hibernate JPA es la fuente canónica del esquema Oracle. Se prohíbe SQL nativo. Los binarios documentales se almacenan como BLOB separado de sus metadatos.

## V. Trazabilidad y calidad

Las escrituras generan eventos append-only. Las llamadas API generan auditoría de acceso sin cuerpos ni secretos. Los cambios relevantes requieren pruebas automatizadas.

**Versión:** 1.0.0  
**Ratificada:** 2026-07-28
