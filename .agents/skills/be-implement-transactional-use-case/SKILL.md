---
name: be-implement-transactional-use-case
description: Implementar casos de uso Spring Boot del PIIP mediante controladores DTO, servicios de aplicación transaccionales, dominio y repositorios JPA. Usar para funcionalidades en apps/backend que no requieran inventar reglas ni modificar Angular.
---

# Implementar caso de uso transaccional

1. Leer `AGENTS.md`, la especificación activa y el módulo backend propietario.
2. Confirmar entradas, salidas, autorización, auditoría y reglas respaldadas por fuente; usar `NEEDS CLARIFICATION` ante vacíos.
3. Mantener controladores delgados y contratos HTTP separados de entidades JPA.
4. Ubicar orquestación y transacciones en servicios de aplicación; mantener persistencia en repositorios Spring Data JPA.
5. Validar rol y ámbito efectivo dentro del servicio para operaciones sensibles.
6. Registrar auditoría sin tokens, cuerpos HTTP o contenido documental.
7. Modificar únicamente `apps/backend/**` y devolver un handoff si el consumidor Angular debe adaptarse.
8. No usar SQL nativo ni borrar archivos; no ejecutar Maven/Oracle sin autorización explícita del usuario.
