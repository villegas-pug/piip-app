---
description: "Lista trazable de tareas para la inicialización Oracle del monorepo PIIP"
---

# Tareas: Inicialización Oracle con Hibernate y seed externo

**Entrada**: documentos de `specs/015-inicializacion-oracle/`

**Prerrequisitos**: `spec.md`, `plan.md`, `research.md`, `data-model.md` y `quickstart.md` vigentes; sin `NEEDS CLARIFICATION` bloqueantes.

**Autorización**: generar esta lista no autoriza `implement`, pruebas, builds, generación OpenAPI, generación DDL, integración Oracle ni Git. La invocación explícita de `/speckit-implement` autoriza las tareas de implementación de la feature activa; la enmienda constitucional, las pruebas, el DDL y Oracle conservan sus autorizaciones específicas.

## Formato obligatorio

Cada tarea usa `- [ ] T### [P?] [US#?] [FR-###] Acción concreta en ruta/archivo exacto`. `[US#]` aparece únicamente en fases de historias de usuario; `[P]` solo aparece cuando la tarea puede ejecutarse en paralelo sin compartir archivos ni dependencias pendientes. Ninguna tarea se marca `[X]` sin evidencia.

## Evidencia de baseline — no ejecutable

| Historia/requisito | Evidencia actual | Ruta o referencia | Consecuencia |
|---|---|---|---|
| FR-001/FR-002 | Hibernate ya genera un script revisable con 19 tablas | `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/OracleSchemaGenerationTest.java` | Mantener como fuente de evidencia y ajustar solo si el modelo cambia. |
| FR-006 | El coordinador ya usa `SchemaDropper` y `SchemaCreator` | `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` | Extender la frontera actual de 13 tablas sin reimplementar la integración Hibernate. |
| FR-007/FR-008 | El seed actual es SQL DML con `MERGE` y sin IDs identity | `apps/backend/src/main/resources/db/test/catalog-data.sql` | Conservar el recurso y sus propiedades de idempotencia, ampliándolo por dependencias. |
| FR-015 | `dev` y `prod` ya usan `ddl-auto=validate` | `apps/backend/src/main/resources/application-dev.yml`, `application-prod.yml` | Preservar el comportamiento y añadir cobertura de no-reset. |
| FR-018 | La identidad inicial se carga desde el seed | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/ProductionAdminGuard.java` | Evitar creación implícita y validar en `prod` la existencia de un ámbito administrador. |

## Phase 1: Preparación necesaria

**Propósito**: resolver el gate normativo y dejar explícitos los límites que bloquean la implementación.

- [X] T001 [FR-019] Obtener la aprobación formal de la enmienda que sustituye la confirmación separada por la activación exacta y ordenada `test,test-reset`, registrando la decisión en `.specify/memory/constitution.md` mediante el flujo constitucional y sin modificarla implícitamente desde esta feature. Evidencia: Constitución 1.3.0 ratificada el 2026-09-05.

**Checkpoint**: la Constitución 1.3.0 vigente respalda retirar las variables de habilitación y confirmación del reset.

---

## Phase 2: Fundamentos bloqueantes

**Propósito**: consolidar configuración y guards antes de modificar el ciclo destructivo.

- [X] T002 [P] [FR-015] Ajustar los documentos de configuración en `apps/backend/src/main/resources/application.yml`, `apps/backend/src/main/resources/application-dev.yml` y `apps/backend/src/main/resources/application-prod.yml` para usar `spring.profiles.default: dev`, la URL SID directa, `validate` en runtime ordinario y ningún secreto, wallet o PII real versionado, cubriendo también FR-016, FR-017 y FR-021.
- [X] T003 [FR-003] Eliminar de `PiipProperties.TestReset` y de `apps/backend/src/main/resources/application-test-reset.yml` los campos y variables obsoletos de habilitación/confirmación, conservar fingerprint/esquema, y centralizar la validación de perfiles exactos, destino y acción efectiva de esquema en `apps/backend/src/main/java/pe/gob/midagri/piip/config/PiipProperties.java` y `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetEnvironmentGuard.java`, cubriendo también FR-004 y FR-005.
- [X] T004 [FR-004] Crear `TestResetStartupGuard` como `ApplicationContextInitializer` en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetStartupGuard.java` y registrarlo mediante `apps/backend/src/main/resources/META-INF/spring.factories` para rechazar antes del refresh cualquier perfil distinto de `test,test-reset` o cualquier `spring.jpa.hibernate.ddl-auto` distinto de `none`.
- [X] T005 [FR-005] Fortalecer el preflight posterior en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetEnvironmentGuard.java` para validar conexión, fingerprint y esquema `SISPIIP` antes del primer borrado, sin registrar credenciales ni URLs completas en mensajes, cubriendo también FR-006.

**Checkpoint**: la operación destructiva queda autorizada solo por el perfil exacto y no puede iniciar JPA con una acción de esquema de escritura.

---

## Phase 3: User Story 1 — Reconstruir el esquema Oracle descartable

**Objetivo**: borrar y recrear exactamente las 19 tablas mediante la metadata JPA y completar el ciclo de reset en un esquema descartable.

- [X] T006 [US1] [FR-002] Sustituir la frontera 13+6 por una allowlist cerrada de las 19 tablas en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetSchemaFilterProvider.java`, con órdenes completos hijo-a-padre y padre-a-hijo y bloqueo de secuencias no autorizadas.
- [X] T007 [US1] [FR-006] Adaptar `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` para validar metadata exacta, eliminar y recrear las 19 tablas mediante `SchemaDropper`/`SchemaCreator`, tolerar `ORA-00942` solo para la tabla allowlisted en `DROP` y conservar detención en el primer fallo.
- [X] T008 [US1] [FR-014] Actualizar en `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` las etapas, mensajes y postcondiciones para comprobar estructura completa, tablas operativas/auditoría/notificaciones vacías y conteos del seed sin afirmar éxito antes de terminar.
- [X] T009 [US1] [FR-002] Ajustar `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetSchemaFilterTest.java` y `TestResetCoordinatorTest.java` para cubrir allowlist de 19 tablas, órdenes, secuencias bloqueadas, tolerancia única de `ORA-00942` y rechazo de metadata o perfiles no autorizados.
- [X] T010 [US1] [FR-001] Actualizar `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/TestResetOracleIntegrationTest.java` para eliminar el gate de variables retirado, conservar `@Tag("integration")` y verificar el ciclo completo de reset y reejecución sobre Oracle descartable.

**Checkpoint**: existe una implementación estructural del reset de 19 tablas y pruebas focalizadas preparadas, pero sus resultados aún requieren autorización para ejecutarse.

---

## Phase 4: User Story 2 — Proteger el arranque ordinario

**Objetivo**: garantizar que `dev`, `prod` y las pruebas ordinarias no ejecuten el reset ni el seed.

- [X] T011 [P] [US2] [FR-015] Retirar `IdentityBootstrap` y sus propiedades de configuración, y añadir `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/ProductionAdminGuard.java` para validar en `prod` la existencia de un ámbito administrador sin crear datos.
- [X] T012 [US2] [FR-003] Crear o actualizar `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/TestResetStartupGuardTest.java` para demostrar rechazo de `prod`, perfiles adicionales, orden incorrecto y acciones `create`, `create-drop` o `update` antes del refresh, cubriendo también FR-004.
- [X] T013 [US2] [FR-015] Añadir cobertura de configuración en `apps/backend/src/test/java/pe/gob/midagri/piip/config/ProfileConfigurationTest.java` para comprobar `dev` por defecto, `prod` activado externamente, `validate` ordinario y ausencia de ejecución automática del seed, cubriendo también FR-016.

**Checkpoint**: los perfiles ordinarios mantienen datos existentes y validan el esquema; el único flujo destructivo continúa aislado en `test,test-reset`.

---

## Phase 5: User Story 3 — Cargar identidad y datos maestros mínimos

**Objetivo**: dejar el esquema recién creado listo para autenticación y pruebas funcionales con el dataset sintético acordado.

- [X] T014 [US3] [FR-007] Ampliar y formatear `apps/backend/src/main/resources/db/test/catalog-data.sql` con bloques DML ordenados para roles, `MIDAGRI`, `UE-001`, `UE-002`, cuatro UO sintéticas, un usuario local activo, dos ámbitos `ADMINISTRADOR_PIIP`, cuatro catálogos, 17 ítems y seis tipos documentales, resolviendo FKs por claves naturales y sin IDs identity, DDL, PL/SQL, contraseñas ni creación de Keycloak; cubre FR-008 a FR-013.
- [X] T015 [US3] [FR-007] Actualizar `apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/CatalogSeedPolicyTest.java` para comprobar formato legible, DML-only, ausencia de IDs identity, ausencia de secretos y placeholders, y presencia de los datos personales aprobados, `USUARIO` y `USUARIO_ROL_AMBITO`, cubriendo también FR-008.
- [X] T016 [US3] [FR-011] Extender `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/TestResetOracleIntegrationTest.java` para verificar subject, usuario activo, rol administrador, institución, UE, UO, ámbitos vigentes y cantidades exactas del dataset después del reset, cubriendo también FR-012.
- [X] T017 [US3] [FR-014] Añadir a `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` postvalidaciones de relaciones y conteos: 2 roles, 1 institución, 2 UE, 4 UO, 1 usuario, 2 ámbitos, 4 catálogos, 17 ítems y 6 tipos documentales.

**Checkpoint**: el seed y su integración dejan una identidad local utilizable solo si el subject correspondiente existe previamente en Keycloak.

---

## Phase 6: Documentación y cierre transversal

- [X] T018 [P] [FR-020] Actualizar `docs/development/test-catalog-reset.md` para describir reset total de 19 tablas, guards sin variables adicionales tras la enmienda, dataset de identidad, orden de ejecución, `ORA-00942`, serialización y diferencia con clonación.
- [X] T019 [P] [FR-020] Actualizar `docs/deployment/institutional-development.md` para documentar conexión SID directa, `spring.profiles.default: dev`, `validate` ordinario, prerrequisitos DBA, identidad Keycloak y prohibición de usar el seed sintético en producción.
- [X] T020 [P] [FR-020] Registrar en `specs/011-centralizar-catalogos-piip/spec.md` que `specs/015-inicializacion-oracle` supersede la frontera 13+6 únicamente para este procedimiento, sin reabrir ni marcar tareas históricas de 011.
- [X] T021 [FR-021] Resolver la presencia del fallback de contraseña gestionado manualmente en `apps/backend/src/main/resources/application.yml` y confirmar que los artefactos versionados no contienen credenciales reales, tokens ni wallets; la PII del usuario seed queda permitida por aprobación explícita. Evidencia: el fallback literal fue retirado; la contraseña se proporciona mediante `ORACLE_PASSWORD` y `test-reset` hereda la configuración común sin duplicar credenciales.
- [X] T022 [FR-002] Regenerar mediante Hibernate y revisar `database/generated/piip-oracle.sql` únicamente con autorización explícita, comprobando las 19 tablas, índices, FKs, constraints e identidades sin editar el DDL a mano. Evidencia: el script generado coincide exactamente con el artefacto versionado.

## Validaciones propuestas — requieren autorización

- [X] T023 [FR-002] Ejecutar la prueba focalizada de generación DDL desde `apps/backend` con `gradlew.bat test --tests pe.gob.midagri.piip.persistence.OracleSchemaGenerationTest` y comparar el resultado con `database/generated/piip-oracle.sql`. Evidencia: BUILD SUCCESSFUL y comparación idéntica.
- [X] T024 [FR-007] Ejecutar las pruebas focalizadas del reset y política del seed desde `apps/backend` con `gradlew.bat test --tests pe.gob.midagri.piip.config.reset.CatalogSeedPolicyTest --tests pe.gob.midagri.piip.config.reset.TestResetSchemaFilterTest --tests pe.gob.midagri.piip.config.reset.TestResetCoordinatorTest --tests pe.gob.midagri.piip.config.reset.TestResetStartupGuardTest`. Evidencia: 10 tests completados y BUILD SUCCESSFUL.
- [X] T025 [FR-015] Ejecutar la suite backend no integrativa desde `apps/backend` con `gradlew.bat test` para validar perfiles ordinarios y regresiones. Evidencia: el test focalizado de auditoría pasó con 2 tests; la suite completa terminó con 187 tests, 0 fallos y 0 errores.
- [X] T026 [FR-001] Ejecutar la integración Oracle desde `apps/backend` con `gradlew.bat integrationTest` contra el esquema descartable autorizado y registrar conteos, reejecución, guardias y datos de US3. Evidencia: ejecución completada con `BUILD SUCCESSFUL` en 34 s.
- [X] T027 [FR-015] Arrancar posteriormente la aplicación con `dev` y con `prod` activado externamente, verificando `ddl-auto=validate`, ausencia de seed, ausencia de creación implícita y validación de `ProductionAdminGuard`. Evidencia: ambos arranques conectaron a Oracle, Hibernate validó el esquema y la aplicación inició correctamente en puertos separados; no quedaron procesos activos.

## Dependencias y orden de ejecución

- **Propietario canónico**: T001/T002 para gobernanza y configuración; T006-T010 para el backend de reset; T014 para el recurso seed; `database/generated/piip-oracle.sql` se deriva de JPA y pertenece al flujo de revisión DBA.
- **Consumidores**: T009-T010 consumen el reset; T015-T017 consumen el contrato del seed; T018-T019 consumen el comportamiento final; T023-T027 consumen código y configuración terminados.
- **Orden obligatorio**: T001 antes de T002-T005; T002-T005 antes de T006-T017; T006-T008 antes de T009-T010; T014 antes de T015-T017; T006-T017 antes de T018-T027; T022 antes de comparar o entregar el DDL.
- **Oportunidades paralelas**: T018, T019 y T020 pueden prepararse en paralelo después de fijar el comportamiento; T009 puede avanzar en paralelo con T011; las validaciones T023-T025 son independientes entre sí, aunque no deben ejecutarse sin autorización y T026/T027 requieren Oracle.

## Control de alcance histórico

- Las specs `001` a `005` son referencias históricas, no backlog.
- `specs/011-centralizar-catalogos-piip` se usa solo como antecedente directo del seed DML y su frontera previa; 015 la supersede expresamente para el reset total sin reabrir sus tareas.
- `specs/014-consolidar-asignaciones-usuarios` queda fuera de alcance; no se crean tareas para su funcionalidad.
- Contradicciones sin resolver: ninguna. La enmienda constitucional de T001 está aprobada y versionada en la Constitución 1.3.0.

## Estrategia de implementación

1. Completar el gate constitucional y los fundamentos de seguridad/configuración.
2. Entregar primero el MVP de User Story 1: reset Hibernate de 19 tablas con guardias fail-closed.
3. Completar User Story 2 para blindar los perfiles ordinarios.
4. Completar User Story 3 con el seed de identidad y datos maestros.
5. Cerrar documentación, DDL derivado y validaciones autorizadas.

## Notas

- Cada tarea representa trabajo nuevo aprobado y apunta a rutas reales.
- Las capacidades existentes se registran como baseline y no se repiten como tareas completadas.
- La feature no agrega frontend, endpoints, DTO, OpenAPI, migración productiva ni infraestructura Oracle/Keycloak.
- T025-T027 quedan cerradas con las evidencias registradas en la sección siguiente.

## Resultados de validación ejecutada

- T025 se ejecutó con `gradlew.bat test`: 187 tests completados, 0 fallos y 0 errores.
- T026 se ejecutó con `gradlew.bat integrationTest`: `BUILD SUCCESSFUL` en 34 s, incluyendo la integración Oracle autorizada.
- T027 se verificó con arranques autorizados en `dev` y `prod`: conexión Oracle exitosa, validación Hibernate/JPA y arranque completo de `PiipApplication`; los procesos terminaron por timeout normal de servidor y los puertos quedaron libres.
