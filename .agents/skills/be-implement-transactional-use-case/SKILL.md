---
name: be-implement-transactional-use-case
description: Implementar funcionalidades y casos de uso transaccionales del backend PIIP con Spring Boot, DTO, servicios de aplicación, dominio y repositorios JPA. Usar siempre ante solicitudes como "crea un endpoint", "guarda o actualiza este registro", "implementa este caso de uso", "agrega un servicio transaccional" o "incorpora lógica backend" en `apps/backend`. No usar para inventar reglas funcionales ni para modificar Angular.
---

# Implementar caso de uso transaccional

## Validar el contexto

1. Leer `AGENTS.md`, la especificación activa y el módulo backend propietario. Esto evita implementar el caso de uso con convenciones o reglas ajenas al módulo.
2. Confirmar entradas, salidas, autorización, auditoría y reglas respaldadas por una fuente. Marcar los vacíos como `NEEDS CLARIFICATION`, porque completar reglas por inferencia cambiaría el comportamiento funcional.

## Mantener las responsabilidades

1. Mantener controladores delgados y contratos HTTP separados de entidades JPA. No inyectar ni consultar repositorios desde controladores, porque hacerlo expondría persistencia y reglas desde la capa API.
2. No declarar `@Transactional` en controladores. Ubicar la orquestación, la autorización funcional y el límite transaccional en servicios de aplicación, para que el caso de uso conserve una transacción reutilizable.
3. Mantener la persistencia en repositorios Spring Data JPA. Introducir accesos alternativos rompería la fuente canónica del esquema.
4. Validar el rol y ámbito efectivo dentro del servicio para operaciones sensibles, porque proteger solo el endpoint permite omitir la autorización desde otros flujos.
5. Registrar auditoría sin tokens, cuerpos HTTP ni contenido documental, para conservar trazabilidad sin exponer información sensible.

## Límites de alcance y ejecución

1. Modificar únicamente `apps/backend/**`. Si el consumidor Angular debe adaptarse, devolver un handoff al agente principal.
2. No usar SQL nativo ni borrar archivos, porque estas acciones contradicen la arquitectura y no son necesarias para implementar el caso de uso.
3. No ejecutar tareas Gradle de prueba, build o `integrationTest` sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Caso de uso, entradas, salidas y fuente funcional.
- Capas y archivos afectados.
- Límite transaccional, autorización y auditoría aplicados.
- Pruebas ejecutadas o pendientes de autorización.
- Handoff para OpenAPI o frontend cuando el consumidor deba adaptarse.
- Decisiones pendientes como `NEEDS CLARIFICATION`.
