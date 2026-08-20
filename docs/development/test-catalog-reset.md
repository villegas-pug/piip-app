# Reinicio controlado de catálogos en pruebas

## Propósito y límites

El perfil `test-reset` reconstruye únicamente el modelo transaccional afectado por la centralización de catálogos. Es destructivo, está deshabilitado por defecto y no constituye una migración productiva.

No debe ejecutarse en operación normal ni en producción. Tampoco reemplaza una estrategia futura de migración no destructiva para datos productivos.

## Guardias obligatorias

Antes de permitir cualquier escritura, el proceso exige simultáneamente:

- perfiles activos `test` y `test-reset`;
- ausencia del perfil `prod`;
- `piip.test-reset.enabled=true`;
- confirmación explícita esperada;
- huella JDBC allowlisted;
- usuario o esquema Oracle allowlisted;
- coincidencia exacta entre la metadata JPA y la allowlist de tablas;
- seed validado como DML idempotente sin DDL, PL/SQL ni IDs numéricos hardcodeados.

La configuración no debe registrar URLs JDBC completas, credenciales, tokens ni valores de confirmación. Los valores sensibles o específicos del ambiente se suministran externamente y no se versionan.

## Frontera destructiva

El reset recrea exactamente estas trece tablas:

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
13. `CATALOGO`

Las siguientes seis tablas están protegidas y nunca forman parte del filtro destructivo:

- `INSTITUCION`
- `ROL`
- `UNIDAD_EJECUTORA`
- `UNIDAD_ORGANICA`
- `USUARIO`
- `USUARIO_ROL_AMBITO`

La auditoría y las notificaciones del ambiente reiniciado se descartan completamente. Sus tablas se recrean vacías, mientras usuarios, roles, ámbitos y estructura organizacional permanecen intactos.

## Secuencia

1. Validar perfiles, confirmación, conexión y esquema sin habilitar escrituras.
2. Capturar la misma `Metadata` utilizada por Hibernate JPA.
3. Comparar allowlist y denylist y validar el seed.
4. Eliminar las trece tablas en orden hijo-a-padre.
5. Recrearlas desde JPA en orden padre-a-hijo.
6. Ejecutar `db/test/catalog-data.sql`.
7. Comprobar conteos, unicidad, datos protegidos y tablas vacías.
8. Comunicar éxito solo después de completar todas las postcondiciones.

Ante el primer error, el proceso registra de manera segura la etapa y la tabla, omite las etapas posteriores y termina con fallo. Después de corregir la causa se ejecuta nuevamente el flujo completo; no se reanuda desde una etapa intermedia.

## Recuperación limitada de `ORA-00942`

`ORA-00942` solo es recuperable cuando se cumplen todas estas condiciones:

- la causa raíz Oracle tiene código `942`;
- el preflight finalizó correctamente;
- la etapa actual es `DROP`;
- la operación corresponde exactamente a la tabla allowlisted actualmente procesada.

El mismo código es fatal en preflight, create, seed, postvalidación, tablas protegidas o cualquier otra operación. Cualquier otro error Oracle también detiene el reset.

## Datos iniciales

El seed contiene únicamente DML Oracle idempotente. Localiza cabeceras, ítems, tipos documentales y Unidades Ejecutoras mediante códigos estables; no utiliza identificadores numéricos conocidos de antemano.

Los Objetivos PEI, Actividades POI y Unidades Orgánicas agregadas por el seed son datos sintéticos exclusivos de pruebas. Esta condición se conserva en comentarios técnicos y en esta guía; no se agrega un campo `official`, `synthetic`, `testData` ni una marca visible en la interfaz.

## Verificación prevista

La validación operativa requiere autorización separada para pruebas Gradle, generación del DDL, integración Oracle y ejecución del perfil destructivo. Como mínimo debe demostrar:

- rechazo antes de escribir cuando falla cualquier guardia;
- orden exacto de drop y create;
- detención en el primer fallo;
- dos ejecuciones consecutivas sin duplicados;
- recuperación después de un drop parcial;
- auditoría y notificaciones vacías;
- preservación de las seis tablas protegidas;
- arranque posterior con el perfil normal usando `ddl-auto=validate` y sin repetir el reset.
