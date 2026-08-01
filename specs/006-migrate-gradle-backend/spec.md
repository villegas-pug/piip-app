# Feature Specification: Migrar backend a Gradle

**Feature Branch**: `006-migrate-gradle-backend`  
**Created**: 2026-08-01  
**Status**: Draft  
**Input**: User description: "Migrar el backend de Maven a Gradle Kotlin DSL como único flujo de compilación, pruebas, empaquetado y CI, conservando los artefactos OpenAPI y DDL en target."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Construir el backend de forma reproducible (Priority: P1)

Como integrante del equipo, necesito una única forma versionada de preparar y verificar el backend para no depender de instalaciones o configuraciones de Maven distintas entre equipos y CI.

**Why this priority**: El backend debe seguir siendo entregable sin alterar sus funcionalidades ni su integración institucional.

**Independent Test**: Se ejecuta el flujo de verificación habitual con el ejecutable versionado y se obtienen resultados equivalentes a los entregables actuales.

**Acceptance Scenarios**:

1. **Given** un clon limpio del repositorio con Java 21 disponible, **When** una persona ejecuta el flujo documentado del backend, **Then** el sistema compila y ejecuta las verificaciones no integradas.
2. **Given** una ejecución de verificación del backend, **When** termina correctamente, **Then** quedan disponibles el contrato público y el DDL revisable en las rutas consumidas por el repositorio.

---

### User Story 2 - Mantener la entrega automatizada (Priority: P1)

Como responsable de entrega, necesito que la automatización continúe validando backend, DDL, contrato y pruebas de integración para detectar regresiones antes de integrar cambios.

**Why this priority**: La migración no debe reducir la cobertura ni interrumpir la entrega continua.

**Independent Test**: La automatización ejecuta las mismas categorías de control y publica el contrato que utiliza el cliente web.

**Acceptance Scenarios**:

1. **Given** un cambio enviado al repositorio, **When** se ejecuta la automatización, **Then** se verifican las pruebas no integradas, la vigencia del DDL y las pruebas de integración.
2. **Given** la generación del contrato finaliza, **When** comienza la validación del cliente web, **Then** este recibe el contrato en su ubicación vigente.

### Edge Cases

- Si el entorno no dispone de Docker o una base Oracle de pruebas, la prueba de integración informa el requisito faltante sin sustituirse por una prueba distinta.
- Si falta Java 21, el flujo informa una incompatibilidad antes de producir entregables incompletos.
- Si el DDL o contrato generado difiere de su entrega versionada, la automatización detiene la integración.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer un único flujo versionado para compilar, probar, ejecutar y empaquetar el backend.
- **FR-002**: El flujo DEBE conservar la compatibilidad con Java 21 y los componentes actualmente usados por el backend, incluida la conectividad Oracle institucional.
- **FR-003**: El flujo DEBE separar las verificaciones no integradas de las verificaciones que requieren Oracle o Docker.
- **FR-004**: La ejecución no integrada DEBE mantener disponibles el DDL revisable y el contrato público en sus rutas actuales.
- **FR-005**: La automatización DEBE mantener la comprobación del DDL, la publicación del contrato y la ejecución separada de verificaciones de integración.
- **FR-006**: Las instrucciones operativas DEBEN identificar un único flujo vigente y no presentar Maven como alternativa soportada.
- **FR-007**: La migración NO DEBE modificar entidades, esquema Oracle, wallets, secretos, configuración confidencial ni el código del cliente web.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los comandos operativos de backend y de automatización usan el flujo único definido por esta feature.
- **SC-002**: Una ejecución no integrada produce los dos entregables requeridos en el 100% de los casos exitosos.
- **SC-003**: La automatización conserva cuatro controles: pruebas no integradas, vigencia del DDL, publicación del contrato y pruebas de integración.
- **SC-004**: El cliente web continúa encontrando el contrato sin cambios en su configuración de entrada.

## Assumptions

- El equipo dispone de Java 21 en estaciones de trabajo y automatización.
- La conectividad Oracle institucional se validará en un entorno autorizado; no se almacenarán credenciales ni wallets durante la migración.
- Las especificaciones `001` a `005` son antecedentes históricos y esta feature inicia la numeración vigente `006`.
