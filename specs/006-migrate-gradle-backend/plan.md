# Implementation Plan: Migrar backend a Gradle

**Branch**: `006-migrate-gradle-backend` | **Date**: 2026-08-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-migrate-gradle-backend/spec.md`

## Summary

Esta feature documenta una migración Maven → Gradle que ya está presente en el árbol de trabajo. El alcance de este plan es dejar trazabilidad Spec Kit; no reimplementar la migración ni ejecutar validaciones, Docker o integración Oracle.

## Baseline implementado

- `apps/backend/settings.gradle.kts` define el proyecto Gradle `piip-backend`.
- `apps/backend/build.gradle.kts` conserva Java 21, Spring Boot, dependencias Oracle y separación de pruebas.
- `apps/backend/gradlew` y `apps/backend/gradlew.bat` proporcionan el wrapper versionado.
- El directorio de compilación continúa siendo `apps/backend/target/`, incluyendo OpenAPI y DDL.
- `pom.xml` fue sustituido y las referencias operativas fueron migradas a Gradle.
- CI, documentación y guard de ámbito ya reflejan el flujo Gradle.
- No se alteran `apps/frontend/**`, entidades, wallets, secretos ni el esquema Oracle.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Gradle Kotlin DSL, Gradle Wrapper 9.6.1, Spring Boot 4.1.0, Springdoc, Oracle JDBC/PKI

**Storage**: Oracle institucional existente; sin cambios de esquema

**Testing**: Source sets `test` e `integrationTest` ya definidos; no se ejecutan en esta fase documental

**Target Platform**: Windows y CI Linux

**Project Type**: Spring Boot web service monolítico modular

**Performance Goals**: No se introducen objetivos nuevos; conservar el comportamiento existente

**Constraints**: Mantener rutas `target/`, no tocar frontend, no ejecutar Docker ni integración Oracle

**Scale/Scope**: Solo artefactos Spec Kit de la feature 006

## Constitution Check

- PASS: no se modifica código Java, entidades, contratos funcionales ni esquema Oracle.
- PASS: no se modifica `apps/frontend/**`.
- PASS: no se incorporan secretos, wallets ni credenciales.
- PASS: el baseline existente se registra como evidencia y no como trabajo a reimplementar.
- PASS: no se ejecutan pruebas, builds, Docker ni integración Oracle en esta fase.

## Project Structure

### Documentation (this feature)

```text
specs/006-migrate-gradle-backend/
├── spec.md
├── checklists/requirements.md
├── plan.md
├── research.md
├── quickstart.md
└── tasks.md
```

### Existing implementation evidence

```text
apps/backend/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── target/
```

**Structure Decision**: los artefactos nuevos se limitan a `specs/006-migrate-gradle-backend/`; las rutas de implementación se citan únicamente como evidencia del baseline.

## Execution Boundary

Este plan no autoriza `/speckit-implement`. La implementación ya existe y cualquier validación posterior requiere autorización explícita en otro turno.

## Complexity Tracking

No aplica: no se añade complejidad de implementación.
