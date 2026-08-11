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
