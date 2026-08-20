# Investigación técnica: Centralizar catálogos PIIP

## Fuentes y límite

La investigación fue estática. Se consultaron la especificación 011, constitución 1.2.0, plan activo anterior, código backend/frontend, configuración, contrato generado, esquema Oracle derivado y documentación oficial de Hibernate. No se ejecutaron pruebas, compilación, generación OpenAPI, servicios, contenedores ni operaciones de base de datos.

## Decisión 1: contrato agregado para catálogos globales

**Decisión**: evolucionar `GET /catalogs` a un `CatalogBundleResponse` tipado y atómico con `recordTypes`, `solutionTypes`, `sources`, `peiObjectives`, `poiActivities` y `documentTypes`. `GET /organizational-units` permanece separado porque depende de Unidad Ejecutora y autorización.

**Razón**: el endpoint ya existe y el flujo inicial necesita todas las opciones globales. Un bundle reduce coordinación, permite un único estado de carga/error y evita incorporar las Unidades Orgánicas fuera de su ámbito. Si falla un grupo, falla la respuesta completa; no se entregan catálogos parcialmente confiables.

**Alternativas descartadas**:

- Un endpoint por catálogo: multiplica llamadas y estados sin aportar independencia operativa en esta feature de solo lectura.
- Mantener `Map<String,List<String>>`: carece de identidad, código, orden y activo; no sirve para escrituras ni históricos.
- Incluir Unidades Orgánicas en el bundle: rompe la consulta acotada por Unidad Ejecutora y su autorización vigente.

## Decisión 2: persistencia según naturaleza del concepto

**Decisión**:

- `CATALOGO`/`CATALOGO_ITEM` solo para Tipo de solución, Fuente u origen, Objetivo PEI y Actividad POI.
- `TIPO_DOCUMENTO` independiente para los seis tipos vigentes.
- `RecordType` como catálogo técnico de solo lectura, sin tabla.
- `UNIDAD_ORGANICA`/`REGISTRO_UNIDAD_RESPONSABLE` como modelo organizacional vigente.

**Razón**: coincide con la delimitación funcional y evita un metamodelo genérico para conceptos con reglas distintas. Tipo documental tiene unicidad y ciclo propio; tipo de registro no requiere administración; Unidad Orgánica ya tiene identidad, pertenencia y autorización.

**Alternativas descartadas**:

- Convertir los siete conceptos en `CATALOGO_ITEM`: diluye restricciones documentales y organizacionales.
- Persistir tipo de registro: añade una tabla administrable sin necesidad funcional.
- Crear `PROYECTO_UNIDAD_ORGANICA`: duplica una asociación ya satisfecha por `REGISTRO_UNIDAD_RESPONSABLE`.

## Decisión 3: IDs en escritura y referencias completas en lectura

**Decisión**: los DTO de creación y los filtros persistentes que ya existen usan IDs. Las respuestas de registros y documentos embeben `{id, code, name, displayOrder, active}`; las del tipo técnico omiten `id`.

**Razón**: el frontend debe conservar la identidad provista por el backend. Una lectura histórica no puede depender del endpoint de opciones activas porque un ítem inactivo ya no aparece allí. La referencia embebida permite mostrar su nombre y estado sin reactivarlo.

**Alternativas descartadas**:

- Escribir por código: el código es estable para carga e integración, pero la especificación exige identidad persistente en el consumidor.
- Escribir por etiqueta: es mutable y hoy obliga a conversiones manuales en `PiipHttpRepository`.
- Resolver toda lectura desde el bundle activo: ocultaría referencias históricas inactivas.

## Decisión 4: Tipo de solución del proyecto preexistente

**Decisión**: `PreexistingProjectRequest` no obliga al frontend a conocer el ID de `NOT_APPLICABLE`; el backend lo resuelve por el código estable dentro de `SOLUTION_TYPE` y valida que el seed lo haya provisto activo.

**Razón**: el valor es una regla técnica del caso de uso, no una elección de la persona. El cliente deja de hardcodear texto o ID y el servicio conserva la semántica vigente.

**Alternativas descartadas**:

- Enviar `solutionTypeId` desde un campo oculto: mantiene acoplamiento innecesario y permite adulterar un valor no seleccionable.
- Hardcodear un ID en backend: los IDs son asignados por Oracle y no son portables.

## Decisión 5: filtros y consumidores existentes

**Decisión**: adaptar únicamente filtros y superficies vigentes, usando IDs donde el concepto persistente ya participa. `PortfolioController` conserva `q`, `status`, `executingUnitId` y paginación; no incorpora parámetros nuevos por catálogo. `Todos/Todas` sigue como valor local. La bandeja conserva búsqueda, Tipo de registro, estado y Unidad Orgánica; no se inventa un filtro de Tipo documental. La gestión documental usa el ID del tipo en su selector y mutaciones. El dashboard obtiene `Iniciativa` y `Proyecto` desde `recordTypes` y mantiene `Todos` local.

**Razón**: el frontend actual filtra principalmente en memoria y el backend lista por `q`, estado y Unidad Ejecutora. La feature exige identidades consistentes, no ampliar funcionalidad de búsqueda fuera de los consumidores mínimos. Cada filtro vigente conserva su semántica y reemplaza solo su fuente local por la identidad canónica disponible.

**Alternativas descartadas**:

- Añadir todos los filtros posibles aunque no existan: amplía alcance y semántica sin aprobación.
- Conservar comparaciones por etiqueta: rompe identidad al renombrar un ítem.

## Decisión 6: Unidad Orgánica sin fallback

**Decisión**: las operaciones adaptadas exigen `organizationalUnitId`; el servicio valida existencia, activo, Unidad Ejecutora y ámbito. `originalDesignation` queda como snapshot calculado por el backend. La carga Angular por Unidad Ejecutora tendrá estados explícitos y protección contra respuestas tardías.

**Razón**: el backend ya consulta unidades activas por Unidad Ejecutora, pero el frontend actualmente cae a siglas locales y puede enviar una designación sin ID. El patrón de request-id ya existe en el dashboard y evita que una respuesta antigua reemplace el contexto nuevo.

**Alternativas descartadas**:

- Conservar `RESPONSIBLE_UNITS`: reproduce identidades no verificadas.
- Resolver por sigla o nombre: son atributos presentacionales, no claves.
- Hacer global el catálogo organizacional: ignora pertenencia y autorización.

El contrato de `OrganizationalUnitOptionResponse` conserva `id`, `code`, `name`, `acronym`, `parentId`, `executingUnitId` y `active`. No agrega `displayOrder`: el endpoint vigente ordena por nombre y el modelo JPA no posee esa columna.

## Decisión 7: estado Angular dedicado

**Decisión**: introducir una fachada/store cohesiva de catálogos detrás de `PiipRepository`, separada de las conversiones de portafolio. Modelará `loading`, `loaded-empty`, `error` y datos; reconciliará selecciones por ID y manejará la carga dependiente de Unidad Ejecutora con un token/request-id.

**Razón**: hoy los catálogos son constantes y `PiipHttpRepository` concentra inicialización, adaptaciones y fallback. Un estado dedicado permite que todos los formularios compartan resultados y distingan ausencia de carga, vacío y error.

**Alternativas descartadas**:

- Consultar desde cada componente: duplica llamadas y criterios de error.
- Agregar más conversiones a `PiipHttpRepository`: aumenta un adaptador ya amplio y perpetúa el acoplamiento por etiquetas.
- Usar listas locales como fallback: contradice la fuente de verdad y enmascara errores.

## Decisión 8: derivación con referencia heredada inactiva

**Decisión**: el detalle de iniciativa conserva la referencia histórica completa. El formulario derivado muestra esa referencia en un bloque contextual separado; si `active=false`, el control de escritura queda vacío/inválido y exige elegir una opción activa. Si sigue activa, se puede preseleccionar por ID.

**Razón**: una opción inactiva debe ser visible como historia pero no aceptable en una nueva escritura. Separar contexto y control evita insertar temporalmente un valor que no existe entre las opciones activas.

**Alternativas descartadas**:

- Incluir el inactivo en el selector: lo hace parecer seleccionable.
- Ocultarlo y limpiar silenciosamente: pierde trazabilidad del origen.
- Comparar por nombre: una etiqueta puede cambiar sin cambiar identidad.

## Decisión 9: reset selectivo desde metadata JPA

**Decisión**: encapsular en infraestructura del perfil `test-reset` los SPI públicos de Hibernate 7.4 (`SchemaManagementTool`, `SchemaDropper`, `SchemaCreator`, `SchemaFilterProvider`) y aplicar una allowlist de tablas. Antes de habilitar el target JDBC, un preflight compara tablas incluidas, excluye explícitamente las protegidas y valida el seed. El perfil normal usa `validate`.

**Razón**: `jakarta.persistence.SchemaManager`/`org.hibernate.relational.SchemaManager` actúa sobre todos los objetos mapeados y no satisface la preservación selectiva. En la versión local, `SchemaFilter.includeTable(Table)` permite filtrar las tablas que procesan `SchemaDropper` y `SchemaCreator`, conservando como fuente el mismo `Metadata` de las entidades. Es una integración version-sensitive, por lo que queda aislada y cubierta por prueba específica.

**Evidencia técnica**:

- Hibernate `SchemaManager` ofrece export/drop/truncate global de objetos mapeados: <https://docs.hibernate.org/orm/7.0/javadocs/org/hibernate/relational/SchemaManager.html>.
- El paquete de schema tooling documenta `SchemaCreator`, `SchemaDropper`, `SchemaFilterProvider` y contratos asociados: <https://docs.hibernate.org/orm/7.1/javadocs/org/hibernate/tool/schema/spi/package-summary.html>.
- La versión resuelta localmente es `hibernate-core:7.4.1.Final`; `javap` confirmó `SchemaFilter.includeTable(Table)` y los métodos de creación/eliminación con `Metadata`.

**Alternativas descartadas**:

- `ddl-auto=create` en el datasource normal: destruye también tablas preservadas.
- `ddl-auto=update`: no elimina columnas legacy y no materializa el reinicio pedido.
- DDL escrito a mano, `JdbcTemplate`, Flyway o Liquibase: contradice la arquitectura del repositorio.
- Reiniciar un esquema completo efímero: no demuestra que los maestros y asignaciones ya existentes se preserven.

**Condición de seguridad**: si el filtro no produce exactamente la allowlist o el SPI cambia de forma incompatible, el proceso falla en preflight. No existe fallback estructural automático.

## Decisión 10: carga DML externa, idempotente y verificable

**Decisión**: aplicar la excepción de persistencia de la constitución 1.2.0 y versionar `apps/backend/src/main/resources/db/test/catalog-data.sql` con `MERGE`/subconsultas por códigos. Solo el perfil destructivo autorizado lo ejecuta después de crear tablas; una validación previa rechaza tokens DDL fuera de comentarios/literales y la postvalidación comprueba unicidad/conteos.

**Razón**: separa datos iniciales de Java y estructura JPA, permite respaldo manual y reejecución sin IDs conocidos. Las Unidades Orgánicas sintéticas se insertan solo para Unidades Ejecutoras de prueba identificadas por código y sin modificar las ya existentes.

**Alternativas descartadas**:

- Objetos Java hardcodeados: dificulta respaldo manual y mezcla datos con orquestación.
- `data.sql` global en todo arranque: podría alterar ambientes normales.
- `INSERT` con IDs numéricos: no es portable ni idempotente.

## Decisión 11: orden y frontera de auditoría del reset

**Decisión**: usar la matriz hijo-a-padre de `plan.md` para drop y el orden inverso para create. `EVENTO_AUDITORIA`, `AUDITORIA_ACCESO` y `NOTIFICACION` forman parte de la allowlist destructiva, se eliminan por completo y se recrean vacías desde el mismo `Metadata` JPA.

**Razón**: el proyecto está en desarrollo y la auditoría y notificaciones del ambiente reiniciado no requieren conservación. Descartarlas evita retener mensajes o evidencia inconsistentes con los datos recreados, mientras usuarios, roles, ámbitos, instituciones, Unidades Ejecutoras, Unidades Orgánicas y demás maestros continúan protegidos.

**Alternativas descartadas**:

- Eliminar solo `REGISTRO_PORTAFOLIO`: falla por claves foráneas y deja huérfanos conceptuales.
- Preservar auditoría completa o parcialmente: conserva historia de un estado de desarrollo que el reset elimina y puede dejar evidencia incoherente con los datos recreados.
- Preservar notificaciones sin registro: conserva mensajes de un estado de prueba que ya no existe y complica la frontera destructiva sin valor funcional aprobado.

## Decisión 12: estrategia de contrato y cliente

**Decisión**: implementar DTO/controladores backend, revisar el OpenAPI resultante y regenerar el cliente Angular antes de adaptar consumidores. No editar `apps/frontend/src/app/api/generated/**` manualmente.

**Razón**: el backend es propietario canónico; el cliente generado debe reflejar el contrato aprobado y `PiipHttpRepository` debe mapear tipos reales, no anticiparlos.

**Alternativas descartadas**:

- Cambiar frontend y backend en paralelo sobre tipos supuestos: aumenta retrabajo y riesgo de divergencia.
- Mantener un cliente manual paralelo: duplica contrato.

## Decisión 13: operaciones sobre una posición documental histórica inactiva

**Decisión**: si una posición documental ya referencia un Tipo documental que luego queda inactivo, se mantiene visible y puede continuar las operaciones vigentes de su ciclo. La validación de activo se exige al crear una referencia/posición nueva, no al agregar versiones, descargar, publicar o marcar `No aplica` sobre la posición existente.

**Razón**: esas operaciones no seleccionan ni crean otra referencia al tipo; actúan sobre una asociación histórica ya persistida. Bloquearlas alteraría el proceso documental que la feature obliga a preservar.

**Alternativas descartadas**:

- Bloquear toda mutación del slot inactivo: rompe la continuidad de versiones y `No aplica`.
- Volver a ofrecer el tipo inactivo como opción para posiciones nuevas: contradice la política de disponibilidad.

## Decisión 14: denominación de auditoría y cambio de activo durante la sesión

**Decisión**: los nuevos eventos documentales conservan en su detalle un snapshot inmutable del código y nombre presentados al ocurrir el evento. La interfaz deja de reconstruir auditoría mediante `DOCUMENT_LABELS`. Cuando una opción seleccionada se vuelve inactiva al guardar o refrescar, se conserva como contexto, el control queda inválido y se muestra “La opción seleccionada ya no está disponible. Elige una opción vigente.”

**Razón**: la auditoría debe describir lo observado en el momento del evento, mientras las pantallas operativas muestran la denominación vigente de la asociación. El mensaje propuesto explica el rechazo 422 sin sustituir automáticamente la elección de la persona.

**Alternativas descartadas**:

- Resolver auditoría histórica contra el nombre vigente: reescribe semánticamente eventos pasados.
- Borrar o reemplazar automáticamente la selección: oculta la causa y puede producir una escritura no intencional.

## Decisión 15: identificación técnica de datos sintéticos

**Decisión**: PEI, POI y Unidades Orgánicas sintéticas se presentan por su nombre normal. Su condición de datos de prueba se registra únicamente en comentarios del seed y documentación técnica; no se agrega `official`, `synthetic`, `testData` ni otra marca a JPA, Oracle, DTO, OpenAPI o Angular.

**Razón**: la aclaración funcional descartó una marca visible. Mantener la distinción en los artefactos operativos evita ampliar el contrato y conserva la restricción de no publicar esos valores como información oficial.

**Alternativas descartadas**:

- Agregar un booleano de oficialidad: introduce estado no solicitado en todos los consumidores.
- Modificar las denominaciones: mezcla metadatos de ambiente con el nombre mostrado.

## Decisión 16: recuperación acotada del reset

**Decisión**: `ORA-00942` solo es recuperable durante `DROP`, cuando la causa raíz tiene código Oracle `942` y la operación corresponde exactamente a la tabla allowlisted que se está eliminando después de superar el preflight. Es fatal en preflight, create, seed, postvalidación, tablas protegidas o cualquier otra etapa; ningún otro código Oracle se tolera.

**Razón**: permite reejecutar un reset parcialmente eliminado sin convertir errores de permisos, constraints, objetos o conexión en falsos éxitos.

**Alternativas descartadas**:

- Ignorar todo `ORA-00942`: podría ocultar una consulta o validación contra una estructura faltante.
- Tolerar cualquier error de drop/create: rompe el comportamiento fail-fast exigido.

## Decisión 17: cobertura del esquema y fixtures afectados

**Decisión**: actualizar explícitamente `OracleSchemaGenerationTest` de 16 a 19 tablas, exigir `CATALOGO`, `CATALOGO_ITEM`, `TIPO_DOCUMENTO`, sus FK y la ausencia de columnas/checks legacy. Adaptar todos los tests que construyen `PortfolioRecordEntity`, preferentemente mediante un builder compartido, incluidos portafolio, documentos, dashboard, work y autorización.

**Razón**: ejecutar la prueba de DDL sin cambiar su conteo fijo fallaría aunque el modelo fuera correcto, y adaptar solo dos fixtures dejaría consumidores directos sin compilar o sin representar las nuevas asociaciones.

**Alternativas descartadas**:

- Eliminar el conteo del esquema: perdería una señal de cambios estructurales inesperados.
- Corregir fixtures únicamente cuando fallen: oculta el impacto conocido y dispersa configuración repetida.

## Impacto funcional confirmado

La guía funcional requiere actualización durante implementación porque cambian:

- la carga y selección en los tres registros;
- la diferencia entre opciones activas e históricos inactivos;
- la elección de Unidad Orgánica por identidad;
- la presentación y selección de tipos documentales;
- los estados visibles de carga, vacío y error.

No cambian estados, transiciones, roles, permisos, alcances, versiones documentales, publicación ni `No aplica`.

## Pendientes no bloqueantes de producción

Se mantienen sin resolver y fuera del ambiente de pruebas:

1. valores oficiales PEI/POI;
2. autoridad institucional responsable de esos catálogos;
3. migración no destructiva de datos productivos.

No quedan incógnitas técnicas bloqueantes para elaborar `tasks.md`; la implementación del reset debe respetar la condición fail-closed del SPI descrita arriba.
