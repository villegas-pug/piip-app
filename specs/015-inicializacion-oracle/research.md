# Investigación: Inicialización Oracle

**Feature**: `015-inicializacion-oracle`
**Fecha**: 2026-09-05

## Objetivo de la investigación

Resolver las decisiones técnicas necesarias para reconstruir un esquema Oracle descartable y cargar datos sintéticos reproducibles, sin confundir este proceso con una clonación institucional ni alterar el arranque ordinario.

## Decisiones

### 1. Inicialización limpia, no clonación

**Decisión**: La feature implementará una inicialización limpia y reproducible para desarrollo y pruebas. No extraerá ni copiará filas, BLOB, auditoría, identidades Oracle ni datos maestros reales de otra instancia.

**Rationale**: La especificación limita el dataset a 19 tablas y a cantidades concretas de identidad, organización, catálogos y tipos documentales. Una clonación fiel requiere un procedimiento DBA independiente, preservación de LOB, contadores e identidades, y no puede derivarse únicamente del repositorio.

**Alternatives considered**:

- Importación Oracle Data Pump: adecuada para clonación, pero fuera del alcance y no reproducible desde la aplicación.
- Carga fila a fila desde una instancia origen: requiere acceso, extracción, reconciliación de identidades y política de datos reales; se descarta para esta feature.

### 2. Hibernate JPA como fuente estructural

**Decisión**: `SchemaCreator` y `SchemaDropper` de Hibernate operarán sobre la `Metadata` capturada del `EntityManagerFactory` para eliminar y recrear exactamente las 19 tablas. `database/generated/piip-oracle.sql` seguirá siendo un artefacto derivado para revisión y entrega al DBA, no una fuente que la aplicación ejecute.

**Rationale**: La Constitución y `AGENTS.md` declaran Hibernate JPA como fuente canónica del esquema. Las entidades vigentes usan `GenerationType.IDENTITY`; no hay secuencias PIIP nombradas que deban crearse por separado.

**Alternatives considered**:

- Ejecutar el DDL versionado desde la aplicación: introduciría una segunda fuente estructural y contradice la Constitución.
- Usar Flyway, Liquibase, `JdbcTemplate` o SQL nativo para el esquema: prohibido por la Constitución y por la arquitectura del monorepo.
- Cambiar el arranque ordinario a `create` o `create-drop`: rompería la separación entre inicialización destructiva y runtime `validate`.

### 3. Guard de configuración antes de JPA y guard de metadata después de JPA

**Decisión**: Se separarán dos controles:

- Un `ApplicationContextInitializer` registrado desde `PiipApplication` validará el orden exacto `test,test-reset`, la ausencia de `prod` o perfiles adicionales y el valor efectivo `spring.jpa.hibernate.ddl-auto=none` antes de refrescar el contexto y construir el `EntityManagerFactory`.
- `TestResetEnvironmentGuard` y `TestResetCoordinator` conservarán la validación posterior de conexión, fingerprint, esquema, metadata JPA, allowlist y errores Oracle, una vez que Hibernate haya capturado su metadata.

**Rationale**: El `ApplicationRunner` actual se ejecuta después de construir el `EntityManagerFactory`. Es demasiado tarde para impedir que un override externo de `ddl-auto` provoque una acción de esquema. La metadata, sin embargo, solo está disponible durante el bootstrap de Hibernate, por lo que no puede trasladarse toda la validación a la fase temprana.

**Alternatives considered**:

- Mantener solo `ApplicationRunner`: no protege contra DDL previo al runner.
- Cambiar `test-reset` a `validate`: impediría construir el contexto contra un esquema vacío.
- Crear el esquema con un DDL independiente antes del contexto: duplicaría la fuente de estructura y aumentaría el riesgo de divergencia.

**Evidencia externa consultada**: la referencia oficial de Spring Boot 4.1 documenta que las propiedades externas se resuelven antes de refrescar el contexto y que los `ApplicationRunner` se ejecutan durante la fase posterior de arranque. La interfaz `ApplicationContextInitializer` permite aplicar el guard antes del refresh; el registro se hará en el `SpringApplication` construido por `PiipApplication` y se probará con la versión `4.1.0` declarada en `apps/backend/build.gradle.kts`.

### 4. SQL externo para datos, solo DML

**Decisión**: Se conservará el recurso `db/test/catalog-data.sql` para minimizar el cambio de integración y se ampliará su alcance. Spring lo ejecutará después de recrear las tablas. El recurso contendrá bloques `MERGE`/DML idempotentes, comentarios y referencias por claves naturales; no contendrá DDL, PL/SQL ni IDs identity literales.

**Rationale**: El usuario eligió conservar el mecanismo SQL externo actual. La Constitución permite esta excepción únicamente para datos iniciales en un perfil destructivo de desarrollo/pruebas, con guardias fail-closed y prohibición productiva. El recurso actual ya se ejecuta mediante `ResourceDatabasePopulator` y tiene una prueba de política DML.

**Alternatives considered**:

- Cargar todos los datos con repositorios JPA: sería compatible con una política ORM estricta, pero contradice la decisión explícita de conservar el seed SQL externo y exige introducir un reconciliador adicional.
- Ejecutar el seed durante `dev` o `prod`: queda prohibido; esos perfiles deben conservar `validate` sin carga automática.

### 5. Identidad local referenciada por Keycloak

**Decisión**: El seed insertará `ROL`, `INSTITUCION`, `UNIDAD_EJECUTORA`, `UNIDAD_ORGANICA`, `USUARIO` y `USUARIO_ROL_AMBITO` junto con catálogos y tipos documentales. `USUARIO.KEYCLOAK_SUBJECT` será la clave natural del usuario; los IDs identity se resolverán mediante subconsultas por códigos y subject.

**Rationale**: El reset total debe funcionar sobre un esquema vacío. El seed es la única fase que llena esas tablas en el entorno descartable. Oracle solo almacena la referencia al subject; no crea usuarios, credenciales ni roles en Keycloak.

**Alternatives considered**:

- Mantener `IdentityBootstrap`: duplicaría la responsabilidad del seed y permitiría creación implícita de datos en arranques ordinarios.
- Crear un usuario de `CONSULTA_EXTERNA`: no existe un requisito ni un subject aprobado para él; se descarta.
- Hardcodear IDs: rompería la reproducibilidad de columnas identity y la idempotencia por claves naturales.

### 6. Identidad productiva sin creación implícita

**Decisión**: Retirar `IdentityBootstrap` y sus propiedades de configuración. En `prod`, un guard no mutante comprobará que exista al menos un ámbito activo `ADMINISTRADOR_PIIP`; la provisión inicial productiva será externa al arranque de la aplicación.

**Rationale**: El seed descartable ya contiene la identidad aprobada y debe ser la única fuente de inicialización de ese entorno. La validación productiva conserva el fallo seguro sin crear usuarios, instituciones, unidades ejecutoras ni ámbitos de forma implícita.

**Alternatives considered**:

- Mantener `IdentityBootstrap` sin propiedades: dejaría un componente sin fuente de datos para crear la identidad y requeriría conservar valores implícitos.
- Mantener un bootstrap productivo configurable: preservaría la creación automática, contradiciendo la decisión de que la identidad se provisiona explícitamente.

### 7. Idempotencia y concurrencia

**Decisión**: El proceso será de ejecución manual y serializada. Se validará que reejecuciones secuenciales no dupliquen datos; no se agregará una constraint única a `USUARIO_ROL_AMBITO` sin evidencia funcional, porque el modelo permite historial de asignaciones y no declara esa unicidad.

**Rationale**: El reset elimina y recrea todo el esquema antes del seed, por lo que la concurrencia no forma parte del caso operativo aprobado. Agregar una restricción sobre ámbitos podría alterar asignaciones históricas o estados activos sin una regla funcional explícita.

**Alternatives considered**:

- Constraint única sobre usuario, rol, institución y UE: más fuerte frente a concurrencia, pero requiere decidir qué ocurre con asignaciones históricas y vigencias.
- Ejecutar varias cargas concurrentes: se descarta; el quickstart exige una sola ejecución a la vez.

### 8. Perfiles y conexión Oracle

**Decisión**: `dev` será el perfil por defecto mediante `spring.profiles.default`; `prod` se activará externamente; `test` conservará H2 y `create-drop`; `test-reset` seguirá siendo no web y usará `ddl-auto=none`. La conexión local prevista usará la URL SID directa y no `oracle.net.tns_admin`.

**Rationale**: `spring.profiles.default` no impide que `SPRING_PROFILES_ACTIVE=prod` reemplace el default y evita mezclar `dev` en pruebas con perfiles explícitos. Una URL SID directa no necesita resolver `tnsnames.ora` ni wallet para conectarse al listener Oracle tradicional.

**Alternatives considered**:

- `spring.profiles.active=dev` versionado: fijaría el perfil y dificultaría la sustitución limpia en contenedores.
- Mantener el alias ATP y `oracle.net.tns_admin`: conserva una dependencia de wallet/TNS ajena a la conexión SID objetivo.
- Crear un perfil nuevo: contradice la decisión de mantener los perfiles existentes.

## Dependencia normativa

La Constitución vigente 1.3.0 establece que la activación exacta y ordenada
`test,test-reset` sustituye la confirmación separada dentro de las guardias del
proceso destructivo. El resto de guardias, incluido `ddl-auto=none`, fingerprint,
esquema, metadata, aplicación no web, allowlist y prohibición de producción,
permanece obligatorio.

## Fuentes

- `specs/015-inicializacion-oracle/spec.md`
- `.specify/memory/constitution.md`
- `AGENTS.md`
- `docs/development/spec-kit-adoption.md`
- `docs/development/test-catalog-reset.md`
- `docs/deployment/institutional-development.md`
- `apps/backend/build.gradle.kts`
- `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetEnvironmentGuard.java`
- `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetCoordinator.java`
- `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/TestResetSchemaFilterProvider.java`
- `apps/backend/src/main/java/pe/gob/midagri/piip/config/reset/HibernateMetadataCapture.java`
- `apps/backend/src/main/java/pe/gob/midagri/piip/identity/application/ProductionAdminGuard.java`
- `apps/backend/src/main/resources/db/test/catalog-data.sql`
- `apps/backend/src/test/java/pe/gob/midagri/piip/persistence/OracleSchemaGenerationTest.java`
- Documentación oficial consultada: `https://docs.spring.io/spring-boot/4.1/reference/features/spring-application.html` y `https://docs.spring.io/spring-boot/4.1/reference/features/external-config.html`
