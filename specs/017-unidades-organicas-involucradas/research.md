# Research: Unidades Orgánicas Involucradas

Decisiones de diseño de `specs/017-unidades-organicas-involucradas` consolidadas a partir del grounding verificado contra código, contratos, pruebas y documentación vigentes (fuentes en `spec.md` § Fuentes canónicas consultadas). No quedan NEEDS CLARIFICATION.

## D1. Reutilizar el esquema de asociación ordenada vigente

- **Decisión**: implementar la lista sobre `REGISTRO_UNIDAD_RESPONSABLE` tal como existe (identidad de unidad, denominación original, orden de presentación, UK por registro y orden), sin tablas, columnas, constraints ni migraciones nuevas.
- **Rationale**: el esquema ya soporta N filas ordenadas por registro; la cardinalidad-1 la imponen solo DTOs y servicio. La constitución (principio IV) exige JPA canónico y prohíbe DDL ad hoc; un cambio estructural sería innecesario y añadiría riesgo.
- **Alternativas descartadas**: tabla nueva de "unidades involucradas" (duplicaría la asociación existente y exigiría migración); columna de posición calculada o Nro persistido aparte del orden (el Nro ya deriva 1..N de la posición).

## D2. Validación de lista en el servicio de aplicación

- **Decisión**: las reglas de la lista (mínimo una, sin duplicados, existencia, actividad, misma Unidad Ejecutora y sigla para nuevas incorporaciones, orden continuo 1..N) se validan en el servicio transaccional, dentro de la sustitución atómica vigente; los DTOs solo imponen mínimo uno y elementos válidos.
- **Rationale**: la sustitución `replace` ya resuelve y valida toda la lista antes de tocar persistencia; concentrar las reglas ahí conserva la atomicidad (FR-007) y la validación efectiva de permisos y datos en el backend (FR-025), independientemente de la UI.
- **Alternativas descartadas**: constraint de BD para unicidad por (registro, unidad) — posible pero redundante con la validación del servicio y no cubre el mínimo ni la sigla; validación solo en frontend — la presentación no reemplaza la validación efectiva.

## D3. Nuevas incorporaciones vs. asociaciones históricas retenidas

- **Decisión**: en una sustitución, las reglas de vigencia (activa), pertenencia (misma UE) y sigla aplican solo a las unidades que no estaban en el conjunto anterior; la unicidad aplica al conjunto completo confirmado; las filas históricas retenidas (incluidas inactivas o sin sigla) se conservan como contexto.
- **Rationale**: la spec (FR-024, Assumptions) distingue expresamente "nuevas asociaciones confirmadas" de asociaciones históricas; exigir actividad/sigla a filas retenidas bloquearía la edición de registros históricos válidos y contradiría la compatibilidad histórica sin saneos (FR-022/FR-023).
- **Alternativas descartadas**: validar todas las filas como nuevas (rompería la conservación histórica); bloquear toda edición de registros con histórico >1 (comportamiento actual del frontend; la feature exige permitir editar la lista completa).

## D4. Unidades sin sigla: ocultar del catálogo y validar en el servicio

- **Decisión**: el catálogo de opciones para nuevas asociaciones ofrece únicamente unidades activas de la UE del registro con sigla no vacía (Q2=A: ocultar); el servicio rechaza igualmente cualquier nueva incorporación sin sigla (defensa en profundidad). No se inventa ni completa ninguna abreviatura.
- **Rationale**: la ausencia de sigla es un problema de datos maestros que impide usar la unidad hasta su regularización (FR-011); ocultarla evita filas no confirmables y el rechazo en servicio garantiza el cumplimiento aunque el catálogo se consulte por otra vía.
- **Alternativas descartadas**: mostrar la unidad deshabilitada con causa (mayor superficie UI para un estado no confirmable; la causa queda documentada en el edge case y en la guía); derivar siglas automáticamente (prohibido expresamente).

## D5. Auditoría: extender el detalle de los eventos existentes

- **Decisión**: los eventos de alta vigentes (registro de iniciativa, proyecto derivado y proyecto preexistente) incorporan al detalle la lista ordenada confirmada; los de modificación conservan valor anterior y nuevo; cada elemento auditado identifica unidad, nombre, sigla y Nro de presentación.
- **Rationale**: reutiliza el mecanismo append-only vigente con datos funcionales (principio V: sin cuerpos ni secretos); el patrón de detalle ya existe para actualizaciones, por lo que extenderlo a altas es la vía de menor riesgo y sin eventos nuevos.
- **Alternativas descartadas**: evento de auditoría separado por lista (duplicaría trazabilidad y complicaría la lectura cronológica); registrar el cuerpo de la solicitud (prohibido por la constitución).

## D6. Contrato: relajar solo la cardinalidad del arreglo

- **Decisión**: los DTO de alta pasan de `@NotEmpty @Size(max = 1)` a mínimo uno sin máximo; los de edición pasan de `@Size(min=1, max=1)` a `@Size(min=1)` cuando el campo está presente (presencia JSON preservada vigente); la respuesta conserva su forma (unidad con código, nombre, sigla, activo, UE; denominación original; orden de presentación) y simplemente expone N elementos ordenados.
- **Rationale**: cambio mínimo del contrato vigente (mismos endpoints, mismos nombres técnicos `responsibleUnits`); el flujo del monorepo exige publicar primero el contrato backend y sincronizar después el cliente Angular.
- **Alternativas descartadas**: renombrar el campo a `involvedOrganizationalUnits` (fuera de alcance: renombres técnicos); nuevo endpoint de lista (rompería consumidores vigentes sin beneficio).

## D7. UI: componente de lista reutilizable

- **Decisión**: un componente de presentación de lista (filas Nro/Descripción/Abreviatura de solo lectura, control de selección filtrado, agregar al final, retirar salvo la última fila, renumeración automática, estados de catálogo con reintento, errores por fila, accesible por teclado y adaptable a pantallas pequeñas) compartido por los tres formularios de alta y la edición; la revisión previa, el detalle y la auditoría visible presentan la lista en el orden confirmado con la denominación "Unidades Orgánicas Involucradas".
- **Rationale**: los tres altas y la edición comparten exactamente el mismo comportamiento (FR-001..FR-017); un componente único evita divergencia y concentra la accesibilidad y la presentación adaptable.
- **Alternativas descartadas**: implementar la lista en cada formulario por separado (duplicación y riesgo de inconsistencia); reordenamiento manual drag-and-drop (fuera de alcance).

## D8. Precarga del proyecto derivado

- **Decisión**: el formulario de proyecto derivado inicia la lista con las unidades confirmadas de la iniciativa de origen, en el mismo orden, como valor inicial editable (Q1=A); el usuario puede retirar o agregar antes de confirmar, sujeto a las reglas de validez.
- **Rationale**: extiende el comportamiento vigente de precarga de la unidad responsable; reduce fricción en el alta más común posterior a una iniciativa; la aclaración está registrada en `spec.md` § Clarifications y materializada en FR-033.
- **Alternativas descartadas**: iniciar vacía (fricción y riesgo de omisión); precargar solo la primera unidad (no hay criterio funcional para elegir una).

## D9. Seed sintético: reutilizar valores y extender la postvalidación

- **Decisión**: el SQL del seed no cambia en valores (4 UOs sintéticas: 2 activas por UE, código, nombre y sigla no vacíos, MERGE idempotente por UE+código); la adecuación exigida por la feature se implementa como extensión de la postvalidación de `TestResetCoordinator`: sigla no vacía por UO sintética, asociación UO–UE correcta, mínimo dos activas por UE sintética, además de los conteos y del vaciado de tablas operativas vigentes.
- **Rationale**: FR-027 manda reutilizar los valores versionados; los datos actuales ya cumplen los requisitos de datos, y la brecha real está en la postvalidación, que hoy no verifica siglas ni asociación. Sin perfil adicional, sin DDL, sin carga automática en `dev`/`prod` (constitución IV).
- **Alternativas descartadas**: reescribir el seed con nuevos valores (prohibido sin decisión funcional); validar en SQL (la postvalidación Java es la vía existente y fall-safe); nuevo perfil de carga (fuera de alcance).

## D10. Orden de entrega: contrato backend antes que cliente frontend

- **Decisión**: secuencia obligatoria del monorepo: (1) backend cambia DTOs/servicio/auditoría y sus pruebas; (2) con autorización, `OpenApiGenerationTest` publica `apps/backend/target/piip-openapi.json`; (3) frontend regenera el cliente (`npm run api:generate`), adapta repositorios/modelos/mock y UI; (4) documentación funcional alineada.
- **Rationale**: el cliente Angular se genera desde el contrato backend; paralelizar frontend y backend sobre un contrato no publicado produciría código generado inconsistente.
- **Alternativas descartadas**: editar a mano el cliente generado (prohibido); congelar el contrato por duplicado manual (deriva).
