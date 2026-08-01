# Despliegue de desarrollo institucional

## Datos requeridos

Antes del primer despliegue se deben confirmar, sin incorporarlos al repositorio:

- URL JDBC, usuario y contraseña del esquema Oracle.
- URL del issuer, audiencia de la API, realm y client ID público de Keycloak.
- URL del frontend y orígenes CORS exactos.
- `subject` Keycloak del administrador inicial.
- código y nombre de la institución, unidades ejecutoras y unidades orgánicas iniciales.

Usar [.env.example](../../.env.example) como matriz de variables. Los valores de ejemplo no son datos maestros confirmados.

## Base de datos

1. Ejecutar `gradlew.bat test` en Windows o `./gradlew test` en Linux/macOS, desde `apps/backend`.
2. Comparar `apps/backend/target/piip-oracle.sql` con `database/generated/piip-oracle.sql`, generado por Hibernate desde las entidades JPA y versionado como entrega al DBA.
3. Entregar el artefacto al DBA para revisión y aplicación en un esquema vacío de desarrollo.
4. Iniciar la aplicación con `PIIP_DDL_AUTO=validate`.

No usar `create`, `create-drop` ni scripts SQL manuales en el esquema institucional. Hibernate JPA es la fuente canónica y la aplicación no ejecuta SQL nativo.

## Keycloak

Configurar un cliente público para Angular con Authorization Code Flow y PKCE `S256`. Registrar exactamente `<URL_FRONTEND>/login` como redirect URI y `<ORIGEN_FRONTEND>` como Web Origin; no asumir comodines ni habilitar client secret en el navegador.

Publicar `runtime-config.institutional.example.js` como `config/runtime-config.js`, reemplazando únicamente URL, realm y client ID. Este archivo no contiene credenciales y Keycloak es obligatorio también en el entorno local. El frontend conserva la ruta interna solicitada durante el redireccionamiento, retorna por `/login` y renueva el token antes de consumir `/api/v1`.

Keycloak autentica la identidad. Los roles del token no conceden permisos PIIP: `/identity/me` resuelve en Oracle el usuario, los dos roles permitidos y sus ámbitos vigentes.

## Bootstrap

El primer `Administrador PIIP` se crea con las variables `PIIP_BOOTSTRAP_*`. El `subject`, institución y unidades ejecutoras deben existir y estar confirmados. El proceso es idempotente y debe deshabilitarse después de comprobar el acceso inicial.

## Contrato frontend-backend

El test `OpenApiGenerationTest` genera `apps/backend/target/piip-openapi.json`. Desde `apps/frontend`, `npm run api:generate` produce el cliente Angular en `src/app/api/generated`. El código generado no se edita manualmente.

## Aceptación institucional

Ejecutar el recorrido con ambos roles y comprobar persistencia tras reinicio, carga y descarga documental, aprobación, tareas, notificaciones, auditoría, proyecto derivado y proyecto preexistente. Esta prueba requiere Oracle y Keycloak institucionales; la suite H2 local no sustituye esa aceptación.
