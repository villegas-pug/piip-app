# Investigación técnica: Administración integral de usuarios

## D1 — El backend conserva la propiedad del contrato HTTP

- **Decisión**: ampliar `UserAdministrationController` y `AdminDtos` antes de adaptar Angular; publicar el contrato y regenerar el cliente solo con autorización explícita.
- **Rationale**: el backend ya expone lectura, alta y suspensión bajo `/admin`, y el cliente Angular está generado desde OpenAPI. Mantener ese orden evita que el consumidor invente DTOs o rutas.
- **Alternativas consideradas**: llamadas HTTP manuales y modelos locales en Angular; se descartan porque duplican un contrato que el backend ya publica.

## D2 — La edición mantiene la identidad de la asignación

- **Decisión**: `PUT /admin/role-assignments/{scopeId}` actualiza rol, institución y Unidad Ejecutora en la misma asignación, con la versión esperada y una respuesta `ScopeResponse` actualizada.
- **Rationale**: satisface la decisión aprobada de conservar la asignación y permite que `@Version` detecte cambios concurrentes. La auditoría registra valores anteriores y nuevos.
- **Alternativas consideradas**: suspender y crear una nueva asignación; se descarta porque contradice la decisión aprobada de editar la misma asignación.

## D3 — Una suspensión reversible requiere reactivación explícita

- **Decisión**: agregar `PUT /admin/role-assignments/{scopeId}/reactivation` con versión esperada para reactivar la misma fila suspendida; conserva rol, institución y Unidad Ejecutora.
- **Rationale**: la especificación establece suspensión reversible. Reactivar sin crear una fila adicional mantiene la identidad e historial de la asignación.
- **Alternativas consideradas**: volver a crear la asignación; se descarta porque no restaura la misma asignación y obliga a interpretar registros duplicados.

## D4 — Keycloak conserva el ciclo de vida de la cuenta

- **Decisión**: retirar la habilitación e inhabilitación local de usuario y no integrar Keycloak Admin API en esta feature. PIIP administra exclusivamente asignaciones de rol y ámbito.
- **Rationale**: separar autenticación y ciclo de vida de cuenta de la autorización funcional evita dos fuentes contradictorias de estado para una misma identidad.
- **Alternativas consideradas**: conservar `USUARIO.ACTIVO` como interruptor de acceso o sincronizar Keycloak en cada operación; se descartan porque ambos duplican la autoridad de Keycloak y el segundo añade una dependencia administrativa fuera del alcance.

## D5 — Las reglas de integridad se comprueban de manera transaccional y serializada

- **Decisión**: usar consultas y bloqueos JPA para serializar comprobaciones de duplicidad y de último administrador; volver a resolver el actor desde persistencia al iniciar la escritura.
- **Rationale**: el contador actual y la existencia previa no protegen dos solicitudes concurrentes sobre asignaciones diferentes. El contexto del filtro es una instantánea anterior a la transacción.
- **Alternativas consideradas**: confiar únicamente en `@Version` o agregar una restricción SQL; se descartan porque no protegen la combinación con Unidad Ejecutora nula ni la cobertura administrativa, y la constitución prohíbe SQL nativo.

## D6 — El listado administrativo representa el estado administrable, no la autorización del consultante

- **Decisión**: conservar el listado de usuarios y asignaciones dentro de ámbitos cubiertos, incluidas las asignaciones suspendidas necesarias para reactivación, sin exponer un estado de cuenta local.
- **Rationale**: el estado de la asignación es administrable en PIIP; el estado de la cuenta pertenece a Keycloak y no debe condicionar la visibilidad ni la autorización local.
- **Alternativas consideradas**: mostrar o mutar un estado local de usuario; se descarta porque induciría a administrarlo desde una fuente que no es autoridad.

## D7 — Se usarán exclusivamente catálogos organizacionales y roles activos

- **Decisión**: alta, edición y reactivación validarán que rol, institución y Unidad Ejecutora estén activos y que la Unidad pertenezca a la institución seleccionada.
- **Rationale**: las entidades ya expresan el estado activo de los catálogos y una nueva autorización no debe otorgarse sobre valores inactivos.
- **Alternativas consideradas**: validar solo existencia; se descarta porque permitiría asignar accesos a ámbitos retirados del uso funcional.

## D8 — Los candidatos sin asignación se exponen fuera del listado administrable

- **Decisión**: publicar `GET /admin/users/assignment-candidates` para usuarios locales sin ninguna fila de `USUARIO_ROL_AMBITO`. El endpoint es accesible a cualquier Administrador PIIP y sólo alimenta el combo de “Nueva asignación”.
- **Rationale**: un usuario sin asignación no posee institución ni Unidad Ejecutora para aplicarle el filtro del listado administrable. Separar la operación preserva la semántica de `GET /admin/users` y permite conceder el primer rol; `assign` sigue validando de forma transaccional que el ámbito elegido está cubierto por el actor.
- **Alternativas consideradas**: ampliar el listado administrable con identidades sin ámbito —se descarta porque mezclaría candidatos con usuarios operables—; consultar un directorio Keycloak —se descarta porque la feature administra sólo registros locales PIIP.

## D9 — El frontend previene duplicados conocidos; el backend decide

- **Decisión**: antes de enviar el formulario de alta, Angular compara el `subject`, rol, institución y Unidad Ejecutora seleccionados con las asignaciones activas visibles del mismo usuario. Si coincide, muestra un mensaje y no hace la llamada HTTP.
- **Rationale**: evita una operación y un mensaje de error innecesarios para datos ya cargados, sin sustituir los bloqueos ni la validación de duplicidad del servicio ante concurrencia u otros ámbitos no visibles.
- **Alternativas consideradas**: confiar sólo en la respuesta 422 del backend —se descarta como única experiencia porque el duplicado visible es determinista—; hacer del chequeo frontend la única regla —se descarta porque el cliente no es autoridad ni es seguro frente a concurrencia.

## D10 — La asignación exacta es la unidad canónica de autorización

- **Decisión**: conservar cada grant como la tupla inmutable `rol + institución + Unidad Ejecutora opcional`; las proyecciones agregadas se derivan únicamente para compatibilidad y consultas de lectura.
- **Rationale**: separar roles y ámbitos produce un producto cartesiano implícito que permite usar Administrador PIIP de una UE con la cobertura de Consulta externa de otra.
- **Alternativas consideradas**: mantener conjuntos separados y añadir excepciones por servicio; se descarta porque repite la causa raíz y facilita nuevas omisiones.

## D11 — El rol efectivo depende de la Unidad Ejecutora activa

- **Decisión**: Angular deriva el rol visible desde los grants exactos y la UE seleccionada; Administrador PIIP prevalece si ambos roles cubren la misma UE.
- **Rationale**: el encabezado y las acciones deben representar el contexto operativo actual, no el rol más privilegiado disponible en cualquier ámbito.
- **Alternativas consideradas**: mostrar siempre el rol global más alto —se descarta por inducir permisos inexistentes—; selector manual de rol —se descarta porque sería meramente visual sin transportar un contexto de seguridad adicional al backend.

## D12 — La UE activa gobierna la entrada, pero no reduce la bandeja administrativa

- **Decisión**: Administración de usuarios sólo se habilita cuando la UE activa está cubierta por un grant Administrador PIIP. Si el actor administra otra UE, la opción permanece visible pero deshabilitada e indica dónde está disponible. Una vez dentro, la bandeja reúne todas las asignaciones de las instituciones donde el actor tenga al menos un grant Administrador.
- **Rationale**: la cabecera y la entrada continúan expresando el rol operativo exacto, mientras la gestión institucional evita obligar a otorgar un segundo rol operativo únicamente para administrar asignaciones de otra UE de MIDAGRI.
- **Alternativas consideradas**: permitir la entrada desde cualquier UE cuando exista algún grant Administrador —se descarta por resultar visualmente contradictorio—; exigir un grant Administrador exacto por cada UE gestionada —se reemplaza porque mezclaría la administración de accesos con las capacidades funcionales del actor—.

## D14 — El alcance institucional requiere confirmación explícita

- **Decisión**: `Toda la institución` está disponible para cualquier Administrador PIIP de esa institución y una creación o edición que la seleccione exige confirmar que el cambio alcanza todas sus UE. La misma regla permite autoasignación.
- **Rationale**: el usuario confirmó que la gestión de accesos es institucional y que un administrador de UE puede ampliar asignaciones propias o de terceros; la confirmación hace visible el alcance antes de persistir.
- **Alternativas consideradas**: exigir previamente un grant Administrador institucional —se reemplaza por la decisión funcional confirmada—; prohibir autoasignación —se descarta explícitamente—.

## D15 — La cobertura de Administración de usuarios no es un rol operativo heredado

- **Decisión**: derivar la cobertura administrativa desde las instituciones de los grants Administrador, pero aplicarla únicamente en `UserAdministrationService` y en su catálogo HTTP. Los demás servicios y el rol visible continúan evaluando grants exactos.
- **Rationale**: permite que `Administrador PIIP · UE-002` gestione asignaciones de UE-001 sin aparentar ni conceder capacidades de creación, aprobación, carga o publicación sobre UE-001.
- **Alternativas consideradas**: tratar cualquier Administrador de una UE como Administrador operativo institucional —se descarta porque cambiaría el rol mostrado y ampliaría privilegios funcionales—; agregar una tabla de delegaciones administrativas —se descarta por YAGNI, ya que la regla confirmada es uniforme dentro de la institución.

## D16 — Un catálogo administrativo separado evita contaminar las UE legibles

- **Decisión**: publicar un endpoint específico con instituciones y UE administrables para la pantalla de usuarios. `/executing-units` conserva las UE cubiertas por grants operativos y continúa alimentando el selector superior.
- **Rationale**: un Administrador de UE-002 puede gestionar asignaciones de una UE que no sea operativamente legible para él; reutilizar el selector general confundiría cobertura administrativa con acceso funcional.
- **Alternativas consideradas**: ampliar `/executing-units` para administradores —se descarta porque mostraría como seleccionables UE sin rol operativo—; inferir opciones desde las filas visibles —se descarta porque una institución puede tener UE activas aún sin asignaciones.

## D13 — El contrato de identidad evoluciona de forma aditiva

- **Decisión**: `GET /identity/me` añade `roleScopes[]` con rol, institución y UE opcional. Los campos agregados actuales permanecen temporalmente, pero el frontend deja de usarlos para autorizar.
- **Rationale**: el cambio aditivo evita romper consumidores mientras elimina la inferencia insegura en Angular.
- **Alternativas consideradas**: retirar inmediatamente los campos agregados; se pospone para no introducir una ruptura contractual innecesaria durante la corrección.
