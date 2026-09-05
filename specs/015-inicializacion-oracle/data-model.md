# Modelo de datos: Inicialización Oracle

**Feature**: `015-inicializacion-oracle`

## Principios

- Hibernate JPA define las 19 tablas, relaciones, constraints, índices e identidades.
- El seed SQL no declara estructura y no conoce los valores generados por columnas identity.
- Las referencias del seed se resuelven con códigos, subject y combinaciones de claves naturales.
- El dataset es sintético y exclusivo del perfil `test-reset`.

## Entidades y relaciones

| Grupo | Tabla | Clave natural o identificador funcional | Relaciones relevantes | Resultado del reset |
|---|---|---|---|---|
| Identidad | `ROL` | `CODIGO` | Referenciada por `USUARIO_ROL_AMBITO` | 2 filas |
| Identidad | `USUARIO` | `KEYCLOAK_SUBJECT` | Referenciada por ámbitos, tareas y auditoría opcional | 1 fila |
| Identidad | `USUARIO_ROL_AMBITO` | Usuario + rol + institución + UE + vigencia | FKs a usuario, rol, institución y UE | 2 ámbitos activos |
| Organización | `INSTITUCION` | `CODIGO` | Padre de unidades ejecutoras y ámbito institucional | 1 fila |
| Organización | `UNIDAD_EJECUTORA` | Institución + `CODIGO` | Padre de unidades orgánicas y ámbito por UE | 2 filas |
| Organización | `UNIDAD_ORGANICA` | UE + `CODIGO` | UE y padre autorreferenciado opcional | 4 filas |
| Catálogos | `CATALOGO` | Código funcional del catálogo | Padre de ítems y referencia de registros | 4 filas |
| Catálogos | `CATALOGO_ITEM` | Catálogo + `CODIGO` | Catálogo propietario | 17 filas |
| Catálogos | `TIPO_DOCUMENTO` | Código funcional | Referenciada por documentos | 6 filas |
| Portafolio | `REGISTRO_PORTAFOLIO` | ID identity y código generado por aplicación | UE, catálogos y origen autorreferenciado | Vacía |
| Portafolio | `REGISTRO_UNIDAD_RESPONSABLE` | ID identity | Registro y UO | Vacía |
| Portafolio | `CONTADOR_CODIGO` | Tipo o clave de contador definida por entidad | Mantiene correlativos del portafolio | Vacía |
| Documentos | `DOCUMENTO` | ID identity | Registro y tipo documental | Vacía |
| Documentos | `DOCUMENTO_VERSION` | ID identity | Documento | Vacía |
| Documentos | `DOCUMENTO_CONTENIDO` | Versión documental | Versión, contenido BLOB separado | Vacía |
| Trabajo | `TAREA_TRABAJO` | ID identity | Registro y usuario | Vacía |
| Trabajo | `NOTIFICACION` | ID identity | Usuario y registro | Vacía |
| Auditoría | `EVENTO_AUDITORIA` | ID identity | Usuario opcional | Vacía |
| Auditoría | `AUDITORIA_ACCESO` | ID identity | Usuario opcional | Vacía |

## Dataset inicial

### Identidad y organización

- Roles: `ADMINISTRADOR_PIIP` y `CONSULTA_EXTERNA`.
- Institución: `MIDAGRI`, usando el código como nombre según la decisión aprobada.
- Unidades ejecutoras: `UE-001` y `UE-002`, usando cada código como nombre.
- Unidades orgánicas: cuatro filas sintéticas, dos asociadas a cada unidad ejecutora, con códigos estables del seed.
- Usuario: un administrador local activo, identificado por el `keycloak_subject`, nombre y correo literales aprobados para el seed.
- Ámbitos: dos asignaciones activas `ADMINISTRADOR_PIIP`, una para cada unidad ejecutora; ambas pertenecen a `MIDAGRI`.

El usuario debe existir previamente en el realm de Keycloak. El seed no almacena contraseñas y no crea ninguna entidad en Keycloak.

### Datos maestros

- 4 cabeceras de `CATALOGO`.
- 17 filas de `CATALOGO_ITEM`: tipos de solución, fuentes u orígenes, objetivos PEI y actividades POI sintéticos según el seed actual.
- 6 filas de `TIPO_DOCUMENTO`.

Los 23 campos funcionales y los seis catálogos canónicos siguen definidos por `docs/architecture/piip-fields.md`; esta feature solo prepara las filas mínimas para que puedan consumirse en pruebas.

## Reglas de integridad

- Todas las FKs del seed se resuelven después de insertar sus padres.
- No se insertan columnas `ID` identity de forma literal.
- `USUARIO.KEYCLOAK_SUBJECT`, códigos de rol, institución, UE, catálogo e ítem son valores de reconciliación.
- `USUARIO` queda activo; `ULTIMA_AUTENTICACION` permanece nula; `VERSION` inicia en cero.
- Cada ámbito tiene `ACTIVO=1`, `VIGENTE_DESDE` y `FECHA_ASIGNACION` definidos, `VIGENTE_HASTA=NULL`, `ASIGNADO_POR='BOOTSTRAP'` y `VERSION=0`.
- Las tablas operativas, documentales, de notificación y auditoría quedan sin filas producidas por el reset.
- Una reejecución secuencial no aumenta las cantidades esperadas ni crea claves naturales duplicadas.
- No se agrega una constraint nueva a `USUARIO_ROL_AMBITO` en esta feature; la operación debe ser serializada.

## Orden de carga

1. `ROL`.
2. `INSTITUCION`.
3. `UNIDAD_EJECUTORA`.
4. `UNIDAD_ORGANICA`.
5. `USUARIO`.
6. `USUARIO_ROL_AMBITO`.
7. `CATALOGO`.
8. `CATALOGO_ITEM`.
9. `TIPO_DOCUMENTO`.

El orden de eliminación y creación de las tablas lo determina el filtro de metadata Hibernate y es independiente del orden de carga de datos.
