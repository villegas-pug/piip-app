# Reinicio controlado de Oracle en pruebas

## Propósito y límites

La combinación exacta y ordenada de perfiles `test,test-reset` reconstruye un esquema Oracle descartable completo mediante la metadata de Hibernate y carga un dataset sintético externo. Es destructiva, no se activa por defecto y no constituye una migración productiva.

Las ejecuciones sobre el esquema descartable deben serializarse: `USUARIO_ROL_AMBITO` no declara una unicidad física para conservar su historial de asignaciones.

No debe ejecutarse en operación normal ni en producción. Tampoco reemplaza una estrategia futura de migración no destructiva para datos productivos.

## Guardias obligatorias

Antes de permitir cualquier escritura, el proceso exige simultáneamente:

- perfiles activos exactamente `test,test-reset`, en ese orden;
- ausencia del perfil `prod` y de cualquier perfil adicional;
- `spring.jpa.hibernate.ddl-auto=none` efectivo antes de crear JPA;
- ausencia de un override de `hibernate.hbm2ddl.auto` distinto de `none`;
- huella JDBC allowlisted;
- esquema Oracle `SISPIIP` allowlisted;
- conexión directa al destino autorizado por la huella SHA-256 configurada;
- coincidencia exacta entre la metadata JPA y la allowlist de 19 tablas;
- seed validado como DML idempotente sin DDL, PL/SQL ni IDs numéricos hardcodeados.

La autorización separada de confirmación fue sustituida por la activación exacta de perfiles en la Constitución 1.3.0. El seed contiene los datos personales aprobados del usuario inicial de este entorno descartable. Las credenciales, contraseñas, tokens y wallets se mantienen fuera del repositorio.

El perfil `test-reset` no redefine `spring.datasource`: hereda la URL, el usuario,
la contraseña y el driver de `application.yml`. La configuración H2 de
`src/test/resources/application-test.yml` solo se activa con `test` cuando no está
presente `test-reset`.

## Frontera destructiva

El reset recrea exactamente estas 19 tablas:

1. `DOCUMENTO_CONTENIDO`
2. `DOCUMENTO_VERSION`
3. `DOCUMENTO`
4. `REGISTRO_UNIDAD_RESPONSABLE`
5. `TAREA_TRABAJO`
6. `NOTIFICACION`
7. `EVENTO_AUDITORIA`
8. `AUDITORIA_ACCESO`
9. `REGISTRO_PORTAFOLIO`
10. `CONTADOR_CODIGO`
11. `CATALOGO_ITEM`
12. `TIPO_DOCUMENTO`
13. `USUARIO_ROL_AMBITO`
14. `UNIDAD_ORGANICA`
15. `USUARIO`
16. `ROL`
17. `UNIDAD_EJECUTORA`
18. `INSTITUCION`
19. `CATALOGO`

El orden anterior es hijo-a-padre. El proceso recrea las mismas tablas en orden padre-a-hijo, empezando por `CATALOGO`, `INSTITUCION` y `ROL`. No existe una frontera de tablas protegidas durante este procedimiento.

La auditoría, las notificaciones y todas las tablas operativas del ambiente reiniciado se descartan completamente y quedan vacías.

## Secuencia

1. Validar perfiles, `ddl-auto`, conexión y esquema sin habilitar escrituras.
2. Capturar la misma `Metadata` utilizada por Hibernate JPA.
3. Comparar la metadata con la allowlist cerrada de 19 tablas y validar el seed.
4. Eliminar las 19 tablas en orden hijo-a-padre.
5. Recrearlas desde JPA en orden padre-a-hijo.
6. Ejecutar `db/test/catalog-data.sql`.
7. Comprobar conteos, claves naturales, relaciones y tablas vacías.
8. Comunicar éxito solo después de completar todas las postcondiciones.

Ante el primer error, el proceso registra de manera segura la etapa y la tabla, omite las etapas posteriores y termina con fallo. Después de corregir la causa se ejecuta nuevamente el flujo completo; no se reanuda desde una etapa intermedia.

## Recuperación limitada de `ORA-00942`

`ORA-00942` solo es recuperable cuando se cumplen todas estas condiciones:

- la causa raíz Oracle tiene código `942`;
- el preflight finalizó correctamente;
- la etapa actual es `DROP`;
- la operación corresponde exactamente a la tabla allowlisted actualmente procesada.

El mismo código es fatal en preflight, create, seed, postvalidación, cualquier tabla no allowlisted o cualquier otra operación. Cualquier otro error Oracle también detiene el reset.

## Datos iniciales

El seed contiene únicamente DML Oracle idempotente. Localiza roles, institución, unidades ejecutoras, unidades orgánicas, usuario, ámbitos, cabeceras, ítems y tipos documentales mediante claves naturales. El usuario inicial está definido directamente en el SQL con los datos personales aprobados para este entorno descartable.

El dataset resultante contiene dos roles (`ADMINISTRADOR_PIIP` y `CONSULTA_EXTERNA`), una institución `MIDAGRI`, dos UE (`UE-001` y `UE-002`), cuatro UO sintéticas (`UE-001-UO-01`, `UE-001-UO-02`, `UE-002-UO-01`, `UE-002-UO-02`), un usuario local y dos ámbitos administrativos activos, además de cuatro catálogos, 17 ítems y seis tipos documentales. El usuario debe existir previamente en Keycloak; el seed no crea usuarios, contraseñas ni tokens allí.

Los Objetivos PEI, Actividades POI y Unidades Orgánicas agregadas por el seed son datos sintéticos exclusivos de pruebas. Esta condición se conserva en comentarios técnicos y en esta guía; no se agrega un campo `official`, `synthetic`, `testData` ni una marca visible en la interfaz.

## Verificación prevista

La validación operativa requiere autorización separada para pruebas Gradle, generación del DDL, integración Oracle y ejecución del perfil destructivo. Como mínimo debe demostrar:

- rechazo antes de escribir cuando falla cualquier guardia;
- orden exacto de drop y create;
- detención en el primer fallo;
- dos ejecuciones consecutivas sin duplicados;
- recuperación después de un drop parcial;
- auditoría y notificaciones vacías;
- recreación exacta de las 19 tablas;
- creación de la identidad y organización sintéticas esperadas;
- arranque posterior con el perfil normal usando `ddl-auto=validate` y sin repetir el reset.
