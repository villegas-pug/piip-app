# Quickstart: Inicialización Oracle

## Alcance

Este procedimiento prepara un esquema Oracle descartable para desarrollo o pruebas. No clona una base institucional, no carga datos reales y no debe ejecutarse en producción.

## Prerrequisitos DBA

- Instancia Oracle, listener, servicio/SID y conectividad disponibles.
- Esquema `SISPIIP` descartable.
- Privilegios para crear y eliminar las tablas, índices, constraints e identidades del modelo, además de DML.
- Tablespace, cuota, charset, timezone y políticas de respaldo definidos por el DBA.
- Usuario Keycloak existente cuyo `sub` coincida con el valor literal aprobado en el seed.
- Credencial Oracle proporcionada fuera del repositorio y fuera de los artefactos versionados.

La aplicación no crea la instancia, el usuario Oracle, el tablespace, las cuotas, los grants, el listener ni el usuario Keycloak.

## Configuración

1. Configurar en el entorno local la URL SID directa, usuario y `ORACLE_PASSWORD` del esquema Oracle; `test-reset` heredará esos valores de `application.yml`.
2. Mantener `spring.profiles.default: dev` para el arranque normal sin perfil explícito.
3. Activar `prod` desde el entorno cuando corresponda; no fijarlo en `application.yml`.
4. Para el reset, activar exactamente `test,test-reset`, en ese orden, sin `prod` ni perfiles adicionales.
5. Configurar el fingerprint autorizado y el esquema `SISPIIP` en el perfil `test-reset`.
6. No configurar variables `PIIP_BOOTSTRAP_*`; el seed contiene los datos aprobados del usuario inicial y no incorpora contraseñas, tokens ni wallets.

La conexión SID directa no depende de `oracle.net.tns_admin`, `tnsnames.ora` ni wallet. `ORACLE_DEV_TNS_ADMIN` solo sería aplicable a una configuración ATP/TNS y no forma parte de este flujo.

## Secuencia operativa

La siguiente secuencia describe la operación prevista. No se ejecutó durante la planificación.

1. Confirmar que la Constitución 1.3.0 vigente respalda la activación exacta de `test,test-reset` como autorización operativa.
2. Detener cualquier instancia de la aplicación que use el esquema descartable.
3. Ejecutar el proceso no web con el perfil exacto `test,test-reset`.
4. El guard temprano debe rechazar perfiles adicionales, `prod` y cualquier `ddl-auto` distinto de `none` antes de construir JPA.
5. El guard posterior debe comprobar URL/fingerprint, esquema, conexión y metadata.
6. Hibernate debe eliminar las 19 tablas en orden hijo-a-padre y recrearlas en orden padre-a-hijo.
7. Spring debe ejecutar el seed SQL DML-only después de crear la estructura.
8. En arranque ordinario, ningún componente debe crear identidad; `prod` solo debe validar que exista un ámbito administrador activo.
9. La postvalidación debe comprobar las 19 tablas, las cantidades del dataset, sus relaciones y el vacío de tablas operativas, notificaciones y auditoría.
10. Detener el proceso de reset y retirar privilegios DDL si la política del entorno los separa del runtime.
11. Arrancar normalmente sin perfil explícito para usar `dev`, o con `prod` activado externamente; ambos deben usar `ddl-auto=validate` y no volver a ejecutar el seed.

## DDL derivado

`database/generated/piip-oracle.sql` se actualiza únicamente después de generar el script desde las entidades JPA mediante el mecanismo autorizado. La aplicación no ejecuta ese archivo. La comparación debe cubrir las 19 tablas, índices, FKs, constraints e identidades.

## Fallos y recuperación

- Un fingerprint, esquema o perfil incorrecto debe detener el proceso antes del primer borrado.
- Una metadata distinta de la allowlist de 19 tablas debe detener el proceso.
- `ORA-00942` solo puede tolerarse durante el `DROP` de la tabla allowlisted que se está procesando, después del preflight exitoso.
- Cualquier fallo en creación, seed o postvalidación es fatal; se corrige la causa y se repite el flujo completo.
- No se reanuda desde una etapa intermedia.

## Validación autorizada posteriormente

La ejecución de pruebas Gradle, generación/comparación del DDL, integración Oracle y arranque del perfil destructivo requiere autorización explícita en el turno correspondiente. La planificación no constituye esa autorización.
