# Despliegue de desarrollo institucional

## Datos requeridos

Antes del primer despliegue institucional se deben confirmar, sin incorporarlos al repositorio:

- URL JDBC, usuario y contraseña del esquema Oracle. Para la instancia local prevista se usa la URL SID directa `jdbc:oracle:thin:@srvdb-oracle-desa.domainminag.gob:1521:DEVELOPER` y el esquema `SISPIIP`.
- URL del issuer, audiencia de la API, realm y client ID público de Keycloak.
- URL del frontend y orígenes CORS exactos.
- `subject` Keycloak del administrador inicial productivo. El seed descartable contiene una identidad aprobada independiente para pruebas.
- código y nombre de la institución, unidades ejecutoras y unidades orgánicas iniciales.

Usar [.env.example](../../.env.example) como matriz de variables. Los valores de ejemplo no son datos maestros confirmados.

## Base de datos

1. Para un esquema descartable de desarrollo o pruebas, configurar externamente `ORACLE_PASSWORD`; `test-reset` hereda URL, usuario, contraseña y driver desde `application.yml`. La identidad inicial aprobada está definida directamente en `db/test/catalog-data.sql`.
2. Ejecutar manualmente el proceso no web con los perfiles exactos `test,test-reset`; Hibernate recreará las 19 tablas y Spring ejecutará el seed sintético DML-only.
3. Para un esquema institucional, comparar `apps/backend/target/piip-oracle.sql` con `database/generated/piip-oracle.sql`, generado por Hibernate desde las entidades JPA y versionado como entrega al DBA.
4. Entregar el artefacto al DBA para revisión y aplicación en un esquema vacío institucional.
5. Iniciar la aplicación sin perfil explícito para usar `dev`, o con `prod` activado externamente; ambos deben usar `ddl-auto=validate`.

No usar el perfil `test-reset`, `create`, `create-drop` ni el seed sintético en el esquema institucional o en producción. Hibernate JPA es la fuente canónica; el único SQL externo permitido es el DML del perfil destructivo de pruebas.

## Keycloak

Configurar un cliente público para Angular con Authorization Code Flow y PKCE `S256`. Registrar exactamente `<URL_FRONTEND>/login` como redirect URI y `<ORIGEN_FRONTEND>` como Web Origin; no asumir comodines ni habilitar client secret en el navegador.

Publicar `runtime-config.institutional.example.js` como `config/runtime-config.js`, reemplazando únicamente URL, realm y client ID. Este archivo no contiene credenciales y Keycloak es obligatorio también en el entorno local. El frontend conserva la ruta interna solicitada durante el redireccionamiento, retorna por `/login` y renueva el token antes de consumir `/api/v1`.

Keycloak autentica la identidad. Los roles del token no conceden permisos PIIP: `/identity/me` resuelve en Oracle el usuario, los dos roles permitidos y sus ámbitos vigentes.

## Bootstrap

El seed descartable crea la fila local del primer `Administrador PIIP` con los datos aprobados directamente en `db/test/catalog-data.sql`, más los ámbitos de `UE-001` y `UE-002`. El `subject` debe corresponder a un usuario existente en Keycloak. No se crea ninguna contraseña ni usuario remoto. En el arranque ordinario no se crean identidades; `prod` valida que exista un ámbito activo `ADMINISTRADOR_PIIP` mediante `ProductionAdminGuard`.

## Contrato frontend-backend

El test `OpenApiGenerationTest` genera `apps/backend/target/piip-openapi.json`. Desde `apps/frontend`, `npm run api:generate` produce el cliente Angular en `src/app/api/generated`. El código generado no se edita manualmente.

## Aceptación institucional

Ejecutar el recorrido con ambos roles y comprobar persistencia tras reinicio, carga y descarga documental, aprobación, tareas, notificaciones, auditoría, proyecto derivado y proyecto preexistente. Esta prueba requiere Oracle y Keycloak institucionales; la suite H2 local no sustituye esa aceptación.
