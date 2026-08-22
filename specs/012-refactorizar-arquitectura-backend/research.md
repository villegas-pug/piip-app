# Investigación técnica: Refactorizar la arquitectura backend

## Fuentes y método

Las decisiones se basan en `spec.md`, `.specify/memory/constitution.md`, `docs/development/spec-kit-adoption.md`, código y pruebas de `apps/backend/**`. `graphify-out/graph.json` se utilizó para localizar relaciones y símbolos; los recorridos amplios resultaron ruidosos y ninguna relación del grafo se trató como regla funcional sin confirmación en fuentes canónicas.

## Decisión 1: conservar el monolito modular y refactorizar por responsabilidades confirmadas

**Decision**: Mantener los módulos y tecnologías actuales. Mover únicamente las responsabilidades clasificadas como `DESVIACIÓN CONFIRMADA`, mediante incrementos pequeños y reversibles.

**Rationale**: La spec prohíbe reescritura, microservicios, Vertical Slice obligatorio y dominio POJO paralelo. Los paquetes actuales ya expresan módulos y varios controllers/servicios son patrones conformes.

**Alternatives considered**:

- Reescritura hexagonal completa: descartada por alcance, riesgo y abstracciones sin necesidad demostrada.
- Un servicio por endpoint: descartado porque fragmenta responsabilidades cohesionadas.
- Conservar lógica en controllers: descartado por contradecir la regla vigente y FR-008 a FR-012.

## Decisión 2: application será propietaria de casos de uso y read models no persistentes

**Decision**: Cada caso de uso afectado se ejecutará en application y devolverá un record/read model que no conserve entidades JPA. API mapeará esos modelos a los DTO externos existentes.

**Rationale**: Evita asociaciones lazy fuera de la transacción, dependencias persistence en API y acoplamiento de application a tipos definidos dentro de controllers, sin cambiar JSON.

**Alternatives considered**:

- Devolver entidades y mapear en controllers: descartada por la desviación confirmada en audit y FR-015.
- Reutilizar siempre DTO HTTP dentro de application: descartada en las fronteras refactorizadas porque convierte transporte en propietario del caso de uso.
- Duplicar modelos por consumidor interno: descartada cuando existe un propietario modular claro.

## Decisión 3: organization posee la vista interna compartida

**Decision**: Definir `OrganizationReadModels.OrganizationalUnitView` en `organization/application` y usarla en organization, documents y portfolio. Cada controller mantiene o crea su DTO externo equivalente.

**Rationale**: La identidad, pertenencia, jerarquía y activo de la Unidad Orgánica pertenecen a organization. Elimina el import de `OrganizationController.OrganizationalUnitResponse` sin trasladar la propiedad a `shared`.

**Alternatives considered**:

- Mover el record del controller a `shared`: descartada porque diluye la propiedad funcional.
- Mantener imports al controller: descartada por FR-016 y FR-025.
- Crear modelos distintos con los mismos campos en documents y portfolio: descartada por duplicación y riesgo de divergencia.

## Decisión 4: conservar LocalAuthorizationService y centralizar operaciones semánticas

**Decision**: Mantener `SecurityContextHolder`, `AccessDeniedException`, `LocalAccessContext` y los mensajes actuales. Añadir o reutilizar métodos semánticos que evalúen una asignación exacta para lectura, escritura funcional por UE y reasignación de tareas.

**Rationale**: La aclaración del usuario eligió preservar la integración Spring Security. La desviación está en la reconstrucción manual/distribuida, no en esa integración.

**Alternatives considered**:

- Crear un puerto de autenticación y eliminar Spring Security de application: descartada por la aclaración.
- Combinar roles y coberturas agregadas: descartada por riesgo de escalada horizontal.
- Reutilizar la cobertura institucional de Administración de usuarios para operaciones funcionales: descartada por ser una política distinta.

## Decisión 5: errores internos tipados y handler HTTP explícito

**Decision**: Ubicar errores funcionales en `shared/application/error`, conservar sus nombres/datos y hacer que `ApiExceptionHandler` traduzca tipos explícitos. Retirar `IllegalStateException` de la captura 422 genérica.

**Rationale**: Los casos de uso necesitan errores independientes de HTTP y un fallo técnico no debe presentarse como regla funcional. Las invariantes que hoy lanzan `IllegalStateException` se convierten explícitamente dentro del caso de uso, preservando mensajes esperados.

**Alternatives considered**:

- Mantener todo en `shared/api`: descartada por acoplar application a transporte.
- Crear códigos y mensajes nuevos: descartada porque cambiaría `ProblemDetail` observable.
- Capturar toda excepción runtime como 422: descartada porque oculta fallos técnicos.

## Decisión 6: adaptar multipart en API sin fragmentar documents

**Decision**: Definir `DocumentUploadInput` en application y envolver `MultipartFile` con un adaptador de `documents/api`. El input expone metadatos y lectura diferida; `DocumentService` conserva el orden de validación, lectura, persistencia, checksum, auditoría, publicación, descarga y notificación del ciclo documental.

**Rationale**: El tipo Spring MVC pertenece al binding HTTP; el resto forma una responsabilidad documental cohesionada y no debe dividirse por cantidad de dependencias.

**Alternatives considered**:

- Mantener `MultipartFile` en application: descartada por FR-027.
- Construir un record con bytes en API antes de validar metadatos: descartada porque podría cambiar cuál error se observa si la lectura falla sobre un archivo ya inválido.
- Separar upload, publicación, descarga y `No aplica` en cuatro servicios: descartada por falta de una desviación funcional adicional.
- Introducir almacenamiento externo o streaming: descartada por alcance y posible cambio observable.

## Decisión 7: separar PortfolioService y devolver efectos a sus módulos propietarios

**Decision**: Dividir consultas, comandos de iniciativa y comandos de proyecto. Centralizar la construcción de `PortfolioReadModel`; usar `PortfolioWorkService` y `PortfolioDocumentService` para efectos pertenecientes a work/documents, siempre unidos a la transacción llamante.

**Rationale**: Las responsabilidades distintas están confirmadas en los métodos públicos y dependencias de `PortfolioService`. Los contratos internos por ID/código evitan cruzar entidades y conservan propiedad modular y rollback conjunto.

**Alternatives considered**:

- Mantener el servicio monolítico: descartada por FR-026.
- Crear un servicio por endpoint: descartada por fragmentación.
- Mantener acceso directo de portfolio a repositorios work/documents: descartada porque perpetúa la propiedad modular difusa confirmada.
- Migrar primero portfolio: descartada porque depende de organization, documents, work, autorización y errores compartidos.

## Decisión 8: equivalencia semántica como criterio de regresión

**Decision**: Exigir igualdad de valores deterministas y normalizar exclusivamente valores legítimamente variables, comprobando idénticas reglas de generación, cálculo, orden y efecto.

**Rationale**: Es la aclaración aprobada y evita falsos positivos entre ejecuciones independientes sin debilitar el contrato.

**Alternatives considered**:

- Comparación byte por byte de toda respuesta: descartada por timestamps, correlaciones, códigos y versiones dependientes del estado.
- Comparación solo estructural: descartada porque no verifica autorización ni efectos.

## Decisión 9: documentar compatibilidad, no generar un OpenAPI alternativo

**Decision**: Crear `contracts/http-compatibility.md` como matriz de congelamiento. No crear ni modificar OpenAPI porque la feature no cambia la interfaz externa.

**Rationale**: El contrato apropiado para esta refactorización es la invariancia de la superficie actual. Un segundo OpenAPI sería una fuente paralela y podría insinuar cambios no autorizados.

**Alternatives considered**:

- Regenerar el OpenAPI para verificar: descartada porque requiere autorización y no hay cambio contractual.
- Omitir contratos: descartada porque la compatibilidad es el principal criterio de éxito.

## Decisión 10: reglas arquitectónicas focalizadas con ArchUnit existente

**Decision**: Ampliar las pruebas arquitectónicas con reglas para controllers, cruces JPA/API, propiedad de DTOs y errores de application. No introducir una herramienta nueva.

**Rationale**: `build.gradle.kts` ya incluye ArchUnit 1.4.1 y las cuatro pruebas actuales no cubren las fronteras de esta feature.

**Alternatives considered**:

- Revisión manual exclusiva: descartada porque no previene reintroducciones.
- Incorporar otro framework de análisis: descartada porque ArchUnit ya satisface la necesidad.

## Resultado de investigación

Todas las decisiones técnicas necesarias quedaron resueltas. No hay `NEEDS CLARIFICATION` ni dependencia nueva. La implementación posterior puede descomponerse en tareas después de invocar `/speckit-tasks`.
