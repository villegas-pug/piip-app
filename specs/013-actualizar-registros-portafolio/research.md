# Investigación: Actualización controlada de registros de portafolio

## 1. Método y separación de contratos

**Decisión**: usar `PATCH /initiatives/{code}` y `PATCH /projects/{code}` con `application/json`, requests separados y `PortfolioRecordResponse` como respuesta completa.

**Razón**: la operación modifica solo campos expresamente suministrados y no reemplaza identidad, UE, origen, estado, documentos ni los 23 campos completos. Separar las rutas mantiene las reglas de iniciativa y proyecto en sus propietarios de application y permite validar que el tipo real coincide con la ruta.

**Alternativas consideradas**:

- `PUT`: implicaría reemplazo completo y aumentaría el riesgo de sobrescribir datos no editables u omitidos.
- Un único `/portfolio/{code}`: mezclaría matrices y errores de tipos distintos.
- JSON Patch RFC 6902: expresaría presencia, pero introduciría operaciones genéricas, paths dinámicos y validación más compleja sin beneficio funcional.

## 2. Propiedad ausente frente a nulo explícito

**Decisión**: modelar cada request como bean API mutable con tracking interno de propiedades suministradas. Sus setters Jackson marcan presencia y el controller lo convierte a commands de application con `FieldUpdate<T>(present, value)`.

**Razón**: un record con campos nullable no distingue una propiedad omitida de una propiedad enviada como `null`. El tracking permite preservar campos ausentes, retirar solo PEI/POI/nota/resultados clave y rechazar nulos obligatorios, sin agregar una dependencia productiva.

**Alternativas consideradas**:

- `JsonNullable`: resuelve el triestado, pero requeriría una dependencia y configuración adicionales para una capacidad que puede aislarse en dos DTO.
- `Optional<T>`: no representa de forma segura ausente frente a nulo y no es un contrato de request adecuado.
- Recibir `JsonNode` o `Map`: perdería tipado, documentación OpenAPI y validación declarativa.

## 3. Validación HTTP y funcional

**Decisión**: validar forma/tamaño/presencia inválida como 400 y reservar 422 para reglas evaluadas contra el registro persistido. Los requests son cerrados a las propiedades documentadas.

**Razón**: JSON ilegible, versión ausente/negativa, propiedad desconocida o nulo en un campo obligatorio son defectos del request. Estado, origen, referencias, UO y no-op dependen del recurso real y constituyen reglas funcionales.

**Alternativas consideradas**:

- Traducir todo a 422: borraría la distinción aprobada entre request inválido y regla funcional.
- Validar solo en Angular: no protege llamadas directas ni revocaciones posteriores a la carga.

## 4. Propietarios de aplicación y deuda previa

**Decisión**: agregar `update(...)` a `InitiativeApplicationService` y `ProjectApplicationService`; mantener `PortfolioController` como adaptador y no reconstruir `PortfolioService`. Los nuevos commands no importan DTO API.

**Razón**: es la estructura vigente después de la feature 012 y conserva transacciones y reglas en application. El PATCH no debe aumentar el acoplamiento residual que todavía existe en firmas de alta y response.

**Alternativas consideradas**:

- Crear `PortfolioUpdateService`: duplicaría propietarios y volvería a concentrar reglas de iniciativa/proyecto.
- Refactorizar todas las firmas antiguas a commands/read models: es deuda separada y ampliaría el alcance.

## 5. Lock, versión y orden transaccional

**Decisión**: cargar con lock de escritura por código/tipo, autorizar la UE real, comparar la versión esperada, validar estado/origen/referencias, calcular el diff, mutar, hacer `flush()` y auditar dentro de un único `@Transactional`.

**Razón**: el lock serializa edición con creación derivada y otras mutaciones; `@Version` sigue siendo el único control optimista observable. Hacer flush antes del evento permite registrar la versión nueva real. Un fallo posterior revierte registro, UO, versión y evento.

**Alternativas consideradas**:

- Solo `@Version`: detecta algunas colisiones al flush, pero no serializa la carrera iniciativa-editable frente a derivación.
- Segundo token, `ETag` o tabla de versiones: duplica el mecanismo existente sin requisito.
- Auditar después del commit: rompería la atomicidad y permitiría cambios sin evidencia.

## 6. Actualización del modelo persistente

**Decisión**: agregar métodos de comportamiento explícitos a `PortfolioRecordEntity` y operaciones JPA estándar a repositorios; no cambiar anotaciones, columnas, relaciones ni DDL.

**Razón**: todos los campos, `FECHA_MODIFICACION`, `VERSION`, la asociación ordenada y `DETALLE_JSON` ya existen. Los métodos de dominio protegen identidad, UE, origen, estado, cierre y creador mejor que setters públicos genéricos.

**Alternativas consideradas**:

- Actualización masiva JPQL/SQL: eludiría el dominio, lifecycle JPA y versionado; SQL nativo está prohibido.
- Nueva tabla de historial o versión: duplica auditoría y `@Version`.

## 7. Referencias y sustitución de Unidades Orgánicas

**Decisión**: resolver todas las referencias incluidas antes de escribir. Para UO, validar lista no vacía, IDs únicos, activo y misma UE; comparar identidades y orden; solo si cambia, reemplazar el conjunto completo después de flush de eliminaciones.

**Razón**: la lista representa un agregado ordenado. Validar primero evita cambios parciales; liberar las posiciones antiguas antes de insertar evita conflicto con `UK_RUR_REGISTRO_ORDEN`. Una edición solo de UO actualiza también el padre para avanzar versión y fecha.

**Alternativas consideradas**:

- Actualizar hijos uno por uno: complica orden, rollback y detección de duplicados.
- Mantener simultáneamente filas viejas/nuevas: puede violar unicidad y dejar estados intermedios.
- Crear `PROYECTO_UNIDAD_ORGANICA`: contradice el modelo vigente ratificado por feature 011.

## 8. Diff y auditoría append-only

**Decisión**: usar eventos `INICIATIVA_ACTUALIZADA` y `PROYECTO_ACTUALIZADO`. `cambios` será un mapa ordenado por clave de campo con `{anterior,nuevo}`; catálogos se fotografían con `id/code/name` y UO con `id/code/name/displayOrder`.

**Razón**: produce evidencia estable y legible sin guardar el body completo. Comparar snapshots resueltos evita auditar IDs sin contexto y reconoce el reordenamiento como cambio. Value objects o `LinkedHashMap` admiten nuevos valores nulos; `Map.of` no.

**Alternativas consideradas**:

- Guardar el request: incluiría campos sin cambio y podría capturar datos excluidos.
- Guardar solo nombres o solo IDs: pierde identidad o contexto histórico.
- Un evento por campo: rompe el requisito de exactamente un evento por actualización.

## 9. Estado de repositorio Angular y carga fresca

**Decisión**: exponer carga fresca y actualización en `PiipRepository`. `PiipHttpRepository` obtiene el registro por código al abrir, carga UO por la UE devuelta, construye PATCH sparse, usa la versión vigente y hace upsert del response en signals y mapa de versiones.

**Razón**: los getters actuales son cache-first y la UO global depende de la UE seleccionada. Edición necesita una copia actual y opciones correspondientes a la UE real sin cambiar el contexto activo. El response completo basta para reconciliar detalle y listados.

**Alternativas consideradas**:

- Reutilizar solo `getInitiativeDetail/getProjectDetail`: puede abrir datos y versión obsoletos.
- Cambiar automáticamente la UE activa: altera el contexto de navegación y no es una autorización.
- Mantener una segunda versión dentro del formulario sin sincronizar repositorio: crea estados divergentes.

## 10. Componente de edición y UO ordenables

**Decisión**: crear un único `PortfolioRecordEditComponent` para las rutas de iniciativa/proyecto, con matriz computada por tipo/origen y un editor de UO con agregar, retirar y botones accesibles subir/bajar.

**Razón**: las tres variantes comparten interacción, carga y la mayoría de campos. Una matriz explícita conserva diferencias sin duplicar tres componentes. Los controles de orden no dependen exclusivamente de drag-and-drop y son utilizables con teclado/tecnologías de asistencia.

**Alternativas consideradas**:

- Reutilizar formularios de alta: arrastraría borradores, documentos y confirmaciones que edición excluye.
- Tres formularios de edición: duplicaría validación, conflicto, descarte y mapping sparse.
- Solo drag-and-drop: reduce accesibilidad y dificulta pruebas deterministas.

## 11. Referencias históricas inactivas

**Decisión**: mostrar el valor histórico inactivo proveniente del detalle como “Inactiva — valor registrado”, pero no incorporarlo a opciones nuevas. Si el campo permanece igual se omite; si cambia, la nueva referencia debe ser activa.

**Razón**: preserva lectura histórica y permite cambiar otro campo sin forzar una sustitución ajena. Cumple la regla de que toda referencia incluida en una escritura debe estar activa.

**Alternativas consideradas**:

- Ocultar el valor inactivo: haría parecer vacío un dato existente.
- Incluirlo como opción elegible: permitiría volver a escribir una referencia inactiva.
- Obligar a reemplazar todo valor inactivo en cualquier edición: ampliaría las reglas aprobadas.

## 12. Conflicto, éxito y cambios sin guardar

**Decisión**: ante 409 conservar el formulario, bloquear reenvío de esa copia y ofrecer recarga explícita. Tras 200 reconciliar, marcar limpio, navegar al detalle y confirmar. Usar `canDeactivate` para navegación interna y `beforeunload` para cierre/recarga; nunca guardar borrador.

**Razón**: evita pérdida o reenvío silencioso, respeta la decisión humana tras un conflicto y cubre todas las salidas indicadas. El diálogo de cierre del navegador es nativo y no admite texto personalizado.

**Alternativas consideradas**:

- Reintento o merge automático: puede sobrescribir cambios concurrentes.
- Descartar al recibir 409: pierde trabajo local sin consentimiento.
- Persistir borrador: puede restaurar una versión obsoleta y contradice la spec.

## 13. Acceso desde detalle y listados

**Decisión**: incorporar la acción principal solo en detalles durante la primera entrega. No agregar edición al listado de iniciativas mientras su read model no acredite de forma fiable la relación derivada; FR-006 es opcional.

**Razón**: el detalle concentra autorización, estado y relación contextual. La invariancia vigente ya impide que una iniciativa `Presentado` tenga derivado, pero el backend conserva la comprobación defensiva.

**Alternativas consideradas**:

- Acción en todos los listados: puede mostrar una elegibilidad basada en datos parciales y no aporta una mutación inline autorizada.
- Edición inline: está expresamente excluida.

## 14. OpenAPI, cliente y documentación

**Decisión**: estabilizar backend, generar `piip-openapi.json`, regenerar Angular y luego adaptar el consumidor. Actualizar la guía funcional en el mismo incremento de implementación.

**Razón**: backend es propietario del contrato y el cliente generado no se edita manualmente. La guía cambia porque aparecen acciones, campos, autorización, conflicto y trazabilidad observables.

**Alternativas consideradas**:

- Escribir manualmente el cliente: crea deriva frente al contrato.
- Adaptar Angular antes del OpenAPI: obliga a adivinar nulabilidad y nombres.
- Omitir la guía: contradice el gate documental del repositorio.

## Resultado

No quedan `NEEDS CLARIFICATION` bloqueantes. Los valores oficiales PEI/POI y su autoridad institucional continúan como pendientes heredados de la feature 011 y no cambian la edición: se consumen únicamente referencias activas vigentes.
