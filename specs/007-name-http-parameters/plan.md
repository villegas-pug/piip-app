# Plan de implementación: Nombrar parámetros HTTP del backend

**Rama**: `main` | **Fecha**: 2026-08-09 | **Spec**: [spec.md](spec.md)

**Entrada**: especificación en `specs/007-name-http-parameters/spec.md`

## Resumen

Hacer explícita la asociación de las 36 entradas HTTP existentes basadas en ruta, consulta y multipart. El cambio se limita a los controladores backend y conserva todos los nombres públicos, rutas, métodos, valores por defecto y respuestas.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|-----------|-------------------|-------------------------------|
| 36 enlaces HTTP sin nombre explícito | `apps/backend/src/main/java/**/api/*Controller.java` | Nombrar cada enlace conservando su identificador público actual. |
| Configuración que preserva nombres de parámetros | `apps/backend/build.gradle.kts` | Mantenerla sin cambios; no debe ser el único mecanismo de resolución. |
| Pruebas unitarias JUnit existentes | `apps/backend/src/test/java/pe/gob/midagri/piip/audit/api/AuditControllerTest.java` | Añadir una prueba focalizada, sin contexto de aplicación ni Oracle. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|------|---------|------------------------|---------------------------|
| Frontend | No | N/A | Consumidor sin adaptación; conserva el contrato. |
| Backend | Sí | `apps/backend/src/main/java/pe/gob/midagri/piip/{organization,documents,portfolio,identity,work}/api/` | Propietario canónico de los enlaces HTTP. |
| Database | No | N/A | Sin entidades, esquema ni migraciones. |
| Contrato HTTP | No, compatibilidad documentada | `specs/007-name-http-parameters/contracts/http-parameter-compatibility.md` | Backend conserva el contrato; frontend no cambia. |
| Documentación | Sí | `specs/007-name-http-parameters/` y `AGENTS.md` | Artefactos de Spec Kit. |

## Contexto técnico

**Lenguajes/versiones**: Java 21 y Spring Boot 4.1 en el backend.

**Dependencias principales**: Spring Web para el enlace HTTP; JUnit 5 y AssertJ existentes para la prueba focalizada.

**Persistencia**: N/A; no intervienen JPA, Oracle ni el esquema.

**Validación propuesta**: prueba unitaria de reflexión que cubra los 36 enlaces y solicitudes representativas de ruta, consulta y multipart. La ejecución de `gradlew.bat test` y de cualquier backend requiere autorización explícita en el turno correspondiente.

**Plataforma objetivo**: backend PIIP ejecutado desde Windows mediante Gradle.

**Restricciones**: no cambiar rutas, métodos, DTO, cuerpos, OpenAPI, autorización, auditoría, lógica funcional ni cliente Angular; conservar nombres públicos y valores por defecto.

**Escala/alcance**: seis controladores y 36 enlaces HTTP existentes: 16 variables de ruta, 19 parámetros de consulta y una parte multipart.

## Verificación de la constitución

*GATE: aprobado antes y después del diseño.*

- La feature no introduce campos, catálogos, estados ni transiciones funcionales.
- No modifica autorización Oracle/Keycloak, auditoría ni contenido documental.
- No usa SQL nativo, JPA ni cambios de esquema.
- La calidad queda cubierta por una prueba automatizada propuesta; su ejecución permanece pendiente de autorización.
- No hay contradicciones ni `NEEDS CLARIFICATION` abiertos.

## Dependencias y secuencia

- **Propietario canónico**: backend, mediante las anotaciones de enlace HTTP de sus controladores.
- **Consumidores**: frontend y demás clientes HTTP; no requieren cambios porque el contrato se mantiene.
- **Orden obligatorio**: actualizar los enlaces backend y su prueba de regresión; confirmar después la compatibilidad documentada.
- **Paralelización permitida**: los seis controladores pueden editarse en paralelo solo si la prueba de cobertura se integra al final sobre el inventario común.

## Estructura del proyecto

### Documentación de la feature

```text
specs/007-name-http-parameters/
├── spec.md
├── plan.md
├── research.md
├── quickstart.md
├── contracts/
│   └── http-parameter-compatibility.md
└── tasks.md
```

### Código y documentación afectados

```text
apps/backend/src/main/java/pe/gob/midagri/piip/
├── documents/api/DocumentController.java
├── identity/api/UserAdministrationController.java
├── organization/api/OrganizationController.java
├── portfolio/api/PortfolioController.java
└── work/api/{NotificationController,WorkController}.java
apps/backend/src/test/java/pe/gob/midagri/piip/**/HttpParameterBindingTest.java
```

**Decisión de estructura**: se editarán únicamente las anotaciones de entrada HTTP de los controladores existentes. La prueba vive en el árbol de pruebas backend y verifica el inventario contractual, sin crear capas, DTOs ni entidades.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: entradas `@RequestBody`, encabezados, respuestas, documentación OpenAPI, frontend, configuración de compilación, persistencia, seguridad y auditoría.
- **Specs `001`-`005` consultadas**: Ninguna; no hay dependencia funcional directa y el grounding parte del código actual.
- **Dependencias históricas aprobadas**: Ninguna.
- **NEEDS CLARIFICATION**: Ninguna.
