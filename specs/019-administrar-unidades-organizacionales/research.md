# Investigación: Administración de Unidades Organizacionales

**Feature**: `019-administrar-unidades-organizacionales`  
**Fecha**: 2026-09-11

## Decisiones

### Habilitación inicial solo en `test,test-reset`

**Decisión**: la feature se habilitará inicialmente solo con los perfiles exactos `test,test-reset`. La semilla sintética incorporará orden, fecha de registro y fecha de activación para las UE que crea.

**Rationale**: las nuevas columnas de UE requieren valores para registros existentes. El usuario descartó habilitar esta feature en bases con UE preexistentes y el repositorio prohíbe emplear la semilla como migración productiva o inventar un mecanismo de migración.

**Alternativas consideradas**: carga histórica externa y completar fechas con el instante de despliegue. Ambas quedan fuera de esta feature.

### Controlador administrativo separado y compatibilidad de lecturas

**Decisión**: añadir un controlador administrativo bajo `/admin/organization` sin modificar los endpoints de `OrganizationController`.

**Rationale**: las lecturas actuales sirven al catálogo de portafolio y exponen una representación con `parentId`; la administración necesita incluir entidades inactivas, versión, contexto y campos nuevos de UE, sin introducir `ID_UNIDAD_PADRE` en contratos nuevos.

**Alternativas consideradas**: extender o sustituir las tres consultas existentes. Se descartan por compatibilidad y por la separación entre catálogo y administración.

### Autorización administrativa por institución revalidada

**Decisión**: añadir en `LocalAuthorizationService` una operación de autorización por institución y llamarla desde `OrganizationAdministrationService` antes de toda operación.

**Rationale**: el rol `ADMINISTRADOR_PIIP` por sí solo no acredita el ámbito. El filtro de petición puede contener una instantánea de permisos; las mutaciones sensibles existentes vuelven a resolver los grants Oracle y esta administración debe aplicar el mismo patrón.

**Alternativas consideradas**: autorizar solo en Angular o solo por UE seleccionada. Se descartan porque el backend es la autoridad final y la UE se crea a nivel institucional.

### Generación de códigos bajo lock de contexto

**Decisión**: generar `UE-<consecutivo>` bajo un lock pesimista de la institución y `UO-<consecutivo>` bajo un lock pesimista de la UE. El cálculo inspecciona códigos del ámbito sin distinguir mayúsculas y persiste la entidad y su auditoría dentro de una única transacción.

**Rationale**: el consecutivo se reinicia por ámbito y dos altas concurrentes no pueden recibir el mismo código. Las entidades ya poseen `@Version`, que protege mutaciones posteriores; el lock del padre serializa solo las altas del mismo ámbito.

**Alternativas consideradas**: reutilizar `CodeGeneratorService` de portafolio, tabla de contadores y SQL nativo. Se descartan por su semántica, alcance y restricciones constitucionales.

**Límite conocido**: las constraints Oracle actuales no protegen contra DML externo con distinta capitalización. Para datos creados por PIIP la generación en mayúsculas, verificación sin distinción de caso y lock satisfacen la regla. Una garantía física ante DML externo requiere una decisión de esquema posterior.

### Modelo y ciclo de vida de UE

**Decisión**: persistir en `ExecutingUnitEntity` el orden de presentación, la fecha de registro y la fecha de activación; mantener código e institución inmutables. En una alta de UE, el orden es opcional: si no llega, el backend usa el mayor orden de la institución más uno, o `0` si aún no existe una UE. La creación activa iguala ambas fechas, desactivar preserva activación y reactivar la actualiza.

**Rationale**: refleja FR-010 a FR-013 sin una columna de descripción ni semántica jerárquica para el orden.

**Alternativas consideradas**: derivar fechas sin persistirlas o usar el orden como relación estructural. Se descartan por requisitos funcionales explícitos.

### UO sin jerarquía interna

**Decisión**: los commands, DTO, read models, endpoints y UI administrativos de UO omiten `parent`, `parentId` e `ID_UNIDAD_PADRE`.

**Rationale**: la FK actual es baseline técnico, pero no una regla funcional aprobada. La relación futura apunta a una tabla externa todavía desconocida.

**Alternativas consideradas**: reutilizar `parentId` del endpoint legado o eliminar la FK. Ambas se descartan por compatibilidad y falta de diseño aprobado.

### Auditoría identificable por ámbito

**Decisión**: extender el modelo de auditoría mediante JPA con `entityId`, `institutionId`, `executingUnitId` y `organizationalUnitId`, además del detalle funcional seguro. `AuditService` recibe esas referencias y las persiste en la misma transacción de creación, edición, desactivación o reactivación. La consulta compatible sin parámetro de UE devuelve solo eventos de las UE autorizadas al actor, nunca el conjunto global.

**Rationale**: `entityCode` no identifica una UO de forma global porque `UO-001` puede repetirse entre UE. El filtro actual por código de portafolio no permite una consulta segura por UE/UO.

**Alternativas consideradas**: guardar solo ámbito en `detailJson` o reutilizar el filtro de portafolio. Se descartan para evitar colisiones y semántica incorrecta.

### Contrato backend antes de frontend

**Decisión**: el backend publica primero DTO, errores y contrato OpenAPI administrativo. Después de una generación autorizada, el frontend sincroniza el cliente y adapta sus repositorios y UI.

**Rationale**: `ng-openapi-gen.json` consume el contrato generado del backend y el código de `src/app/api/generated` no se edita a mano.

**Alternativas consideradas**: escribir cliente generado manualmente o iniciar formularios contra endpoints legados. Se descartan por política y porque no contienen mutaciones, versión o campos administrativos.

### Estado y sigla de UO

**Decisión**: conservar la nulabilidad física existente de `acronym`, pero exigir una sigla no vacía al crear UO y mientras una UO se mantenga activa. El alta recibe siempre un valor booleano explícito para `active`, sin predeterminado. La reactivación se rechaza si la sigla está vacía.

**Rationale**: la sigla identifica la UO que debe ser elegible en el catálogo de portafolio. Exigirla desde el alta y en toda UO activa evita crear o conservar UO activas que no puedan seleccionarse en iniciativas y proyectos. El estado explícito evita que el backend o la interfaz introduzcan una regla no aprobada de activación inicial.

**Alternativas consideradas**: permitir UO activas sin sigla o incluirlas en catálogo. Ambas se descartan por la decisión funcional aprobada.
