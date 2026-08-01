---
name: be-evolve-jpa-oracle-model
description: Evolucionar el modelo JPA y su esquema Oracle derivado para los 23 campos y seis catálogos PIIP. Usar al cambiar entidades, relaciones, repositorios, restricciones o generación de esquema dentro de apps/backend.
---

# Evolucionar modelo JPA Oracle

1. Contrastar el cambio con `docs/architecture/piip-fields.md`, la constitución y la especificación activa.
2. Distinguir `NA` de `No aplica` y separar institución, unidad ejecutora y unidad orgánica.
3. Mantener entidades y asociaciones JPA como fuente canónica del esquema.
4. Usar repositorios Spring Data y JPQL cuando corresponda; rechazar SQL nativo, `JdbcTemplate`, procedimientos, Flyway y Liquibase.
5. No convertir observaciones o datos de ejemplo en obligatoriedades, estados o restricciones no confirmadas.
6. Evaluar carga, cardinalidad, índices derivados y concurrencia sin introducir optimizaciones especulativas.
7. Modificar únicamente `apps/backend/**`; la copia versionada de DDL queda bajo control del agente principal.
8. No ejecutar generación, integración Oracle ni borrados sin autorización explícita del usuario.
