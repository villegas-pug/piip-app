---
name: be-implement-transactional-use-case
description: Implementar funcionalidades y casos de uso transaccionales del backend PIIP con adapters HTTP, commands, read models, servicios de aplicación, dominio y persistencia JPA. Usar siempre ante solicitudes como "crea un endpoint", "guarda o actualiza este registro", "implementa este caso de uso", "agrega un servicio transaccional" o "incorpora lógica backend" en `apps/backend`. No usar para inventar reglas funcionales ni para modificar Angular.
---

# Implementar caso de uso transaccional

## Validar el contexto

1. Leer `AGENTS.md`, la especificación activa y el módulo backend propietario. Esto evita implementar el caso de uso con convenciones o reglas ajenas al módulo.
2. Confirmar entradas, salidas, autorización, auditoría y reglas respaldadas por una fuente. Marcar los vacíos como `NEEDS CLARIFICATION`, porque completar reglas por inferencia cambiaría el comportamiento funcional.

## Mantener las responsabilidades

1. Usar `api` como adapter: convertir HTTP y DTO hacia commands de aplicación, y read models hacia responses. La validación de forma pertenece al borde; no convertir DTO de API en dependencia de `application`.
2. Ubicar casos de uso, commands, read models, autorización, auditoría y límites `@Transactional` en `application`. No declarar transacciones en controllers ni introducir nuevas dependencias `application -> api`.
3. Mantener invariantes puras en `domain` y acceso JPA/JPQL en `persistence`. Los acoplamientos existentes que contradigan estos límites son baseline, no patrones para código nuevo.
4. Mantener controladores delgados. No inyectar ni consultar repositorios desde controladores, ni exponer repositorios o entidades JPA en endpoints.
5. Aplicar en el servicio, y dentro de la misma orquestación transaccional, validación funcional, autorización por asignación vigente, cambio de estado y evento de auditoría. La auditoría de acceso conserva su frontera independiente cuando corresponda.

## Límites de alcance y ejecución

1. Modificar únicamente `apps/backend/**`. Si el consumidor Angular debe adaptarse, devolver un handoff al agente principal.
2. No usar SQL nativo ni borrar archivos, porque estas acciones contradicen la arquitectura y no son necesarias para implementar el caso de uso.
3. No ejecutar tareas Gradle de prueba, build o `integrationTest` sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Caso de uso, entradas, salidas y fuente funcional.
- Capas y archivos afectados.
- Commands, read models y adapters introducidos o reutilizados.
- Límite transaccional, validación, autorización y auditoría aplicados.
- Dependencias modulares nuevas y baseline preexistente detectado.
- Pruebas ejecutadas o pendientes de autorización.
- Handoff para OpenAPI o frontend cuando el consumidor deba adaptarse.
- Decisiones pendientes como `NEEDS CLARIFICATION`.
