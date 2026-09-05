# Plan de implementación: Inicialización Oracle con Hibernate y seed externo

**Rama**: `015-inicializacion-oracle` | **Fecha**: 2026-09-05 | **Spec**: [spec.md](spec.md)

**Entrada**: especificación en `specs/015-inicializacion-oracle/spec.md`

**Protocolo**: `docs/development/spec-kit-adoption.md`. Este plan no implementa producto, no ejecuta validaciones y no autoriza Oracle, builds, pruebas, generación de DDL ni Git.

## Resumen

La feature convertirá `test-reset` en un procedimiento explícito para reconstruir las 19 tablas JPA del esquema Oracle descartable y cargar un dataset sintético completo mediante el seed SQL externo actual, ampliado para identidad y organización. El runtime ordinario conservará `validate`, `dev` será el perfil por defecto mediante `spring.profiles.default`, `prod` se activará externamente y la conexión local usará una URL SID directa sin `oracle.net.tns_admin`.

La dependencia constitucional ya fue resuelta: la Constitución 1.3.0 establece que la activación exacta y ordenada `test,test-reset` es la autorización operativa, sin variables adicionales de habilitación o confirmación. Se conservan las demás guardas fail-closed.

## Baseline y evidencia existente

| Evidencia | Ruta o referencia | Consecuencia para la feature |
|---|---|---|
| Configuración común Oracle y perfiles | `apps/backend/src/main/resources/application.yml` | Consolidar el default de perfil y la conexión SID sin versionar credenciales ni secretos. |
| Runtime de desarrollo valida el esquema | `apps/backend/src/main/resources/application-dev.yml` | Mantener `ddl-auto=validate`; no ejecutar reset ni seed en `dev`. |
| Producción valida y recibe límites externamente | `apps/backend/src/main/resources/application-prod.yml` | Mantener `validate`, sin reset ni seed. |
| Pruebas aisladas usan H2 Oracle-mode | `apps/backend/src/test/resources/application-test.yml` | Mantener `test` con `create-drop`; no mezclarlo con `dev`. |
| Perfil destructivo no web | `apps/backend/src/main/resources/application-test-reset.yml` | Mantener `ddl-auto=none` y guards de destino; heredar datasource Oracle desde `application.yml` sin duplicar credenciales. |
| Metadata JPA genera 19 tablas | `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/OracleSchemaGenerationTest.java` | Mantener generación como evidencia derivada y revisar la copia versionada solo tras regeneración autorizada. |
| DDL derivado vigente | `database/generated/piip-oracle.sql` | No editar manualmente; comparar/actualizar mediante Hibernate. |
| Reset actual es `ApplicationRunner` posterior a EMF | `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java` | Separar el guard temprano de configuración del guard posterior que usa metadata Hibernate. |
| Metadata se captura durante bootstrap Hibernate | `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/HibernateMetadataCapture.java` | Reutilizar la misma metadata para validar y recrear el esquema. |
| Filtro actual separa 13 tablas destructibles y 6 protegidas | `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetSchemaFilterProvider.java` | Sustituir la frontera por una allowlist cerrada de las 19 tablas y órdenes completos. |
| Seed actual carga 4 catálogos, 17 ítems, 6 tipos y UO condicionales | `apps/backend/src/main/resources/db/test/catalog-data.sql` | Ampliar/formatear el recurso para roles, organización, usuario y ámbitos con los datos personales aprobados; conservar DML-only e idempotencia. |
| Identidad inicial y validación productiva | `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/ProductionAdminGuard.java` | El seed carga la identidad descartable; `prod` solo valida que exista un ámbito administrador y no crea datos. |
| Pruebas normales excluyen `integration` | `apps/backend/build.gradle.kts` | Mantener la separación entre pruebas locales y la integración Oracle destructiva. |
| Guías describen actualmente seis tablas protegidas y confirmación | `docs/development/test-catalog-reset.md`, `docs/deployment/institutional-development.md` | Actualizar ambas para reflejar la nueva frontera, seed, perfiles, diferencia con clonación y dependencia constitucional. |
| Feature histórica fijó el reset 13+6 | `specs/011-centralizar-catalogos-piip/spec.md`, `plan.md` | Registrar que 015 supersede explícitamente esa decisión para este procedimiento, sin reabrir ni marcar tareas históricas. |

## Impacto en el monorepo

| Área | Impacto | Rutas reales previstas | Propietario / dependencia |
|---|---|---|---|
| Frontend | No | N/A | Sin contrato HTTP ni cambios de UI. |
| Backend | Sí | `apps/backend/src/main/java/**/config/reset/**`, `apps/backend/src/main/java/**/identity/**`, `apps/backend/src/main/resources/application*.yml`, `apps/backend/src/main/resources/db/test/**`, pruebas bajo `apps/backend/src/test/**` | `backend-specialist`; opera bajo la Constitución 1.3.0 y mantiene JPA como fuente. |
| Database | Sí, como derivado | `database/generated/piip-oracle.sql` | Agente principal/DBA; regeneración desde Hibernate, nunca edición manual. |
| Contrato HTTP | No | N/A | No cambia endpoints, DTO ni OpenAPI. |
| Documentación | Sí | `docs/development/test-catalog-reset.md`, `docs/deployment/institutional-development.md`, posible nota de supersesión en `specs/011-centralizar-catalogos-piip/spec.md` o su documentación asociada | Agente principal; actualizar según el comportamiento implementado. |
| Gobernanza | Sí, como dependencia previa | `.specify/memory/constitution.md` | Flujo constitucional explícito; no se modifica implícitamente durante la implementación. |
| Contexto de agentes | Sí | `AGENTS.md` | Referencia activa actualizada a la feature 015. |

## Contexto técnico

**Lenguajes/versiones**: Java 21, Spring Boot 4.1.0, Hibernate JPA, Oracle JDBC 23.26.2, Gradle; H2 y Testcontainers solo en pruebas existentes.

**Dependencias principales**: `spring-boot-starter-data-jpa`, driver Oracle existente y APIs Hibernate ya usadas por `SchemaManagementTool`, `Metadata`, `SchemaDropper` y `SchemaCreator`. No se agrega Flyway, Liquibase, `JdbcTemplate` ni una nueva dependencia de migración.

**Persistencia**: Hibernate JPA es la fuente estructural. El reset usa metadata JPA para las 19 tablas. El seed de datos es la excepción DML externa versionada permitida por la Constitución, ejecutada únicamente bajo `test-reset` y después del DDL Hibernate.

**Validación propuesta**: pruebas unitarias para guards, órdenes, allowlist, tolerancia de `ORA-00942` y política del seed; prueba de generación/comparación DDL; prueba de integración Oracle para reset y reejecución; `git diff --check`. No se ejecutan durante este plan.

**Plataforma objetivo**: esquema Oracle descartable `SISPIIP` accesible por URL SID directa `jdbc:oracle:thin:@srvdb-oracle-desa.domainminag.gob:1521:DEVELOPER`, con fingerprint autorizado `5888eca4876f8583aea30c29da4bcacd944f8d2529b4eef71945943906521428`. La contraseña se proporciona fuera del repositorio.

**Restricciones**:

- Solo `test,test-reset`, en ese orden, puede activar la operación destructiva.
- `prod` y cualquier perfil adicional deben ser rechazados.
- `ddl-auto=none` debe ser efectivo y comprobarse antes de crear el `EntityManagerFactory`.
- URL/fingerprint, esquema, conexión y metadata deben validarse antes del primer borrado.
- La allowlist debe contener exactamente las 19 tablas JPA y bloquear secuencias no autorizadas.
- El seed debe ser DML-only, idempotente, legible, sin IDs identity, secretos, wallets, tokens, contraseñas ni creación de Keycloak.
- `dev` y `prod` conservan `ddl-auto=validate` y no cargan datos automáticamente.
- La auditoría permanece append-only fuera del reset; el reset destructivo de auditoría solo aplica al esquema descartable.
- No se agregan constraints funcionales especulativas a ámbitos.

**Escala/alcance**: una ejecución serializada sobre un esquema descartable; 19 tablas, 2 roles, 1 institución, 2 UE, 4 UO, 1 usuario, 2 ámbitos, 4 catálogos, 17 ítems y 6 tipos documentales. No hay frontend ni contrato HTTP.

## Verificación de la constitución

### Gate inicial

El diseño respeta la Constitución en la estructura, el alcance y la separación de perfiles, pero existe una dependencia normativa explícita antes de implementar el cambio de guardias:

- Se conserva Hibernate JPA como fuente canónica del esquema.
- El seed externo usa únicamente la excepción constitucional de DML inicial, sin DDL, con idempotencia y solo en desarrollo/pruebas.
- `dev` y `prod` no son destructivos y mantienen `validate`.
- `test-reset` es no web, exige destino Oracle autorizado y no se habilita en producción.
- La Constitución 1.3.0 permite eliminar las variables de habilitación/confirmación porque la activación exacta de `test,test-reset` es la autorización operativa.

**Resultado**: gate de diseño satisfecho por la Constitución 1.3.0 vigente. La implementación debe respetar la autorización exacta por perfiles y las guardas restantes.

### Gate posterior al diseño

El diseño propuesto sigue cumpliendo después de la enmienda prevista:

- La autorización operativa será la activación exacta y ordenada `test,test-reset`, sin perfiles extra.
- La validación temprana impedirá que un override de `ddl-auto` escriba antes del guard posterior.
- La validación posterior comprobará fingerprint, esquema, conexión, metadata exacta y frontera de 19 tablas.
- El seed queda limitado a DML inicial sintético y no se ejecuta en `dev` ni `prod`.
- Las auditorías y notificaciones se recrean vacías solo en el entorno descartable.
- No se cambia ninguna transición, obligatoriedad, campo funcional ni contrato HTTP.

La comprobación final requiere revisar la Constitución actualizada, las pruebas modificadas y la documentación; esas verificaciones quedan pendientes de autorización operativa.

## Dependencias y secuencia

- **Propietario canónico**: backend para el comportamiento de reset; Constitución para la autorización normativa; JPA para el esquema; DBA para prerrequisitos Oracle.
- **Consumidores**: pruebas backend, runbook de desarrollo y DBA que recibe el DDL derivado. Frontend y OpenAPI no son consumidores.
- **Orden obligatorio**:
  1. Implementar guard temprano y simplificar `PiipProperties.TestReset`/YAML.
  2. Ampliar allowlist, órdenes, coordinator y validaciones a 19 tablas.
  3. Ampliar y formatear el seed, incluyendo identidad y ámbitos.
  4. Actualizar pruebas y documentación.
  5. Regenerar y revisar DDL desde JPA con autorización.
  6. Ejecutar validaciones locales y luego integración Oracle con autorización separada.
- **Paralelización permitida**: la documentación puede prepararse en paralelo con pruebas unitarias una vez fijado el diseño; no se paralelizan cambios que compartan seed, modelo JPA, configuración o contrato de guardias.

## Estructura del proyecto

### Documentación de la feature

```text
specs/015-inicializacion-oracle/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/requirements.md
└── tasks.md                 # tareas de implementación y validación autorizada
```

No se crea `contracts/` porque la feature no cambia interfaces HTTP, DTO, OpenAPI ni una CLI pública.

### Código y documentación afectados

```text
AGENTS.md
.specify/memory/constitution.md                 # dependencia de gobernanza previa
apps/backend/src/main/resources/application.yml
apps/backend/src/main/resources/application-dev.yml
apps/backend/src/main/resources/application-prod.yml
apps/backend/src/main/resources/application-test-reset.yml
apps/backend/src/main/resources/db/test/catalog-data.sql
apps/backend/src/main/java/pe/gob/midagri/piip/config/PiipProperties.java
apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/
apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/ProductionAdminGuard.java
apps/backend/src/test/java/pe/gob/midagri/piip/config/reset/
apps/backend/src/test/java/pe/gob/midagri/piip/persistence/
database/generated/piip-oracle.sql
docs/development/test-catalog-reset.md
docs/deployment/institutional-development.md
specs/011-centralizar-catalogos-piip/spec.md  # solo nota explícita de supersesión si procede
```

**Decisión de estructura**: el coordinador de reset seguirá en `config.reset`; `TestResetStartupGuard` implementará `ApplicationContextInitializer` y se registrará mediante `META-INF/spring.factories`, sin acceso a repositorios; la carga del seed seguirá siendo un recurso SQL ejecutado por el coordinador después de crear el esquema; los arranques ordinarios no crearán identidades y `ProductionAdminGuard` validará únicamente la existencia de un ámbito administrador en `prod`. No se introducen controladores ni DTO.

## Alcance excluido y antecedentes históricos

- **Fuera de alcance**: clonación/migración de una instancia existente; infraestructura Oracle; Keycloak; datos institucionales reales; frontend; endpoints; OpenAPI; reglas de negocio; 23 campos y transiciones; ejecución automática en `dev` o `prod`; instalación de Flyway/Liquibase; constraints especulativas.
- **Specs `001`-`005` consultadas**: Ninguna; no son necesarias para esta feature y permanecen históricas.
- **Dependencias históricas aprobadas**: `specs/011-centralizar-catalogos-piip` aporta el antecedente del seed DML y del reset controlado. La feature 015 supersede explícitamente su frontera 13+6 para este procedimiento, sin alterar ni reabrir sus tareas históricas. La feature 014 queda fuera de alcance.
- **NEEDS CLARIFICATION**: Ninguna en la spec. La aprobación de la enmienda constitucional y las autorizaciones de ejecución son gates de gobernanza/operación, no preguntas funcionales abiertas.

## Seguimiento de complejidad

La eliminación de la confirmación separada está respaldada por la Constitución 1.3.0 vigente. No se agrega una alternativa más compleja como constraint nueva, loader JPA paralelo o DDL independiente.
