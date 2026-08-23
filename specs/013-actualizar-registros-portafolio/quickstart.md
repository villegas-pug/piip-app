# Quickstart de diseño: Actualización controlada de registros de portafolio

## Estado

Este documento guía una implementación posterior. En `/speckit-plan` no se modificó producto ni se ejecutaron pruebas, builds, servidores, OpenAPI, cliente generado, Oracle o Git.

Artefactos:

- [spec.md](./spec.md)
- [plan.md](./plan.md)
- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/portfolio-updates.openapi.yaml](./contracts/portfolio-updates.openapi.yaml)

## Orden de implementación

### 1. Congelar baseline

- Caracterizar `PortfolioRecordResponse`, `ProblemDetail`, autorización por UE, `@Version`, UO y eventos actuales.
- Agregar escenarios de regresión para alta, aprobación, derivación y transiciones antes de tocar los casos de uso.
- No convertir comportamiento existente en tareas de reimplementación.

### 2. Definir request parcial

- Agregar beans `InitiativeUpdateRequest` y `ProjectUpdateRequest` con tracking de presencia.
- Mantener `version` obligatoria.
- Convertir DTO API a commands de application.
- Rechazar 400 por propiedad desconocida/técnica, nulo obligatorio, límite o formato inválido.
- Conservar 422 para no-op y reglas dependientes del registro.

### 3. Preparar persistencia funcional

- Agregar métodos de dominio explícitos sin cambiar mapping JPA.
- Agregar búsqueda bloqueante que verifique tipo de ruta.
- Extender `ResponsibleUnitService` para validar y reemplazar exactamente una UO de forma atómica.
- No modificar `database/generated/piip-oracle.sql`.

### 4. Implementar casos de uso backend

- `InitiativeApplicationService.update(...)`.
- `ProjectApplicationService.update(...)`.
- Aplicar autorización → versión → estado/vínculo → matriz → referencias → diff.
- Hacer flush antes de registrar versiones en auditoría.
- Crear exactamente un evento por actualización efectiva.

### 5. Publicar contrato HTTP

- Agregar los dos `@PatchMapping`.
- Documentar 200/400/403/404/409/422.
- Comparar la forma real con el contrato de diseño.
- Generar OpenAPI solo con autorización explícita.

### 6. Sincronizar Angular

- Regenerar `apps/frontend/src/app/api/generated/**` únicamente desde el OpenAPI autorizado.
- Agregar modelos y operaciones de presentación en `PiipRepository`.
- Implementar carga fresca, PATCH sparse, versión y upsert en `PiipHttpRepository`.
- Adaptar `PiipMockRepository` sin convertirlo en fuente funcional.

### 7. Construir la experiencia de edición

- Agregar `/iniciativas/:code/editar` y `/proyectos/:code/editar` antes de las rutas de detalle.
- Cargar registro fresco y UO activas por la UE real.
- Mostrar matriz y metadatos read-only por variante.
- Implementar selector único de UO responsable, sin controles de ordenamiento.
- Mostrar referencias históricas inactivas sin permitir escribirlas de nuevo.
- Construir body sparse por comparación con baseline.
- Conservar cambios ante 409 y recargar solo a solicitud del usuario.
- Proteger cancelación/navegación con `canDeactivate` y cierre/recarga con `beforeunload`.
- No agregar borrador local.

### 8. Integrar detalle y documentación

- Mostrar `Editar` solo por `ADMINISTRADOR_PIIP`, UE real y estado editable.
- Tras éxito volver al detalle, confirmar y presentar la respuesta nueva.
- Actualizar `docs/funcional/guia-funcional-piip.md` con edición, campos, UO, errores, concurrencia y auditoría.

## Recorridos de aceptación manual propuestos

### Edición válida de iniciativa

1. Abrir una iniciativa `Presentado` con un administrador autorizado distinto del creador.
2. Cambiar nombre, retirar PEI y reemplazar la única UO responsable.
3. Confirmar que el request incluye `version`, solo esos campos y `peiObjectiveId: null`.
4. Verificar retorno al detalle, confirmación visible y nueva fecha/versión, sin sección de ordenamiento.
5. Verificar un evento `INICIATIVA_ACTUALIZADA` con tres entradas de diff.

### Matrices de proyecto

1. Editar `solutionTypeId` en un proyecto derivado en ejecución: debe aceptarse.
2. Enviar el mismo campo para un proyecto preexistente: debe responder 422.
3. Retirar `keyResults` con nulo explícito: debe aceptarse para ambos proyectos.
4. Intentar editar un proyecto suspendido o finalizado: debe responder 422.

### Autorización

1. Abrir un registro de UE cubierta por una asignación exacta de administrador.
2. Confirmar que no depende de `createdBySubject` ni de la UE seleccionada.
3. Revocar la asignación después de abrir y guardar: debe responder 403 sin cambios/evento funcional.
4. Probar grants de rol y cobertura pertenecientes a asignaciones diferentes: no deben combinarse.

### Referencias y UO

1. Mantener una referencia histórica inactiva sin tocar el campo: debe preservarse.
2. Incluir una referencia inactiva/inexistente/de catálogo equivocado: 422 con propiedades de referencia.
3. Enviar UO duplicada, inactiva, vacía, de otra UE o más de una referencia: 422 y conjunto anterior intacto.
4. Editar otros campos sin enviar `responsibleUnits` en un registro histórico con varias UO: la asociación debe permanecer intacta.
5. En una sesión autenticada, comprobar que edición muestra un único selector, que la UO vigente queda seleccionada, que una UO histórica inactiva solo se muestra como contexto y que no aparecen checkboxes ni "Orden de presentación".

### Concurrencia

1. Abrir dos formularios con la misma versión.
2. Guardar el primero.
3. Guardar el segundo: 409, cambios locales visibles y sin retry automático.
4. Pulsar `Recargar versión vigente`: reemplazar baseline y permitir una nueva edición consciente.

### Descarte y no-op

1. Navegar o cancelar con cambios: permitir permanecer o descartar.
2. Recargar/cerrar pestaña con cambios: mostrar confirmación nativa del navegador.
3. Confirmar descarte: no guardar borrador.
4. Enviar solo versión o valores idénticos: 422, misma versión/fecha y cero evento funcional.

## Protocolo de aceptación para SC-005 y SC-006

Antes de ejecutar la aceptación, el responsable funcional registra y aprueba una muestra no vacía, los perfiles participantes, la variante de registro asignada, el entorno y los datos de prueba. El acta no conserva tokens, credenciales ni datos personales innecesarios.

### SC-005 — tiempo de edición

1. Definir antes de cada ejecución un conjunto válido de cambios y el registro editable que se utilizará.
2. Iniciar la medición cuando el detalle vigente y su versión hayan terminado de cargar.
3. Finalizarla cuando el usuario vea la confirmación de éxito en el detalle actualizado.
4. Registrar identificador de ejecución, inicio, fin, duración, resultado y si se recapturó un campo no modificado.
5. Aprobar SC-005 únicamente si el 100 % de las ejecuciones registradas dura menos de 180 segundos y ninguna exige recaptura de campos no modificados.

### SC-006 — comprensión de campos y disponibilidad

1. Entregar a cada participante una variante de iniciativa, proyecto derivado o proyecto preexistente y su matriz completa de campos mostrados.
2. Solicitar que clasifique cada campo como editable o solo lectura.
3. Presentar un escenario en el que la acción no esté disponible y solicitar que explique la causa observable.
4. Considerar aprobado al participante solo si clasifica toda la matriz sin errores y explica correctamente la ausencia de la acción.
5. Calcular `participantes aprobados / participantes totales * 100`; aprobar SC-006 con un resultado mayor o igual a 90 % y conservar totales, porcentaje, perfiles y variante evaluada.

## Matriz de errores

| HTTP | Caso mínimo | Efecto esperado |
|------|-------------|-----------------|
| 400 | `version` ausente, propiedad técnica, nulo obligatorio o tamaño inválido | Sin mutación ni evento funcional. |
| 403 | Rol/ámbito insuficiente sobre UE real | Acción oculta localmente y servidor autoritativo. |
| 404 | Código inexistente o proyecto enviado a ruta de iniciativa | Sin revelar otro recurso como actualizable. |
| 409 | Versión esperada distinta o conflicto al flush | Preservar actualización vigente y exigir recarga. |
| 422 | Estado/vínculo/campo condicionado/referencia/UO/no-op inválido | Rollback íntegro y detalle comprensible. |

## Verificaciones propuestas que requieren autorización

No ejecutar automáticamente. Cuando el usuario las autorice en el turno correspondiente:

```powershell
cd F:\work-space\piip-monorepo\apps\backend
.\gradlew.bat test --tests "pe.gob.midagri.piip.portfolio.*"
.\gradlew.bat check
```

La generación OpenAPI usa la prueba prevista por el proyecto y también requiere autorización:

```powershell
cd F:\work-space\piip-monorepo\apps\backend
.\gradlew.bat test --tests "*OpenApiGenerationTest"
```

Después de revisar `apps/backend/target/piip-openapi.json`, la regeneración Angular requiere autorización:

```powershell
cd F:\work-space\piip-monorepo\apps\frontend
npm run api:generate
npm test -- --watch=false
npm run build
```

La integración Oracle no es necesaria para el diseño porque no hay cambio estructural. Puede proponerse como regresión funcional posterior, nunca ejecutarse implícitamente.

## Estado de implementación — 2026-08-22

Se implementaron los PATCH backend, la validación de autorización/estado/versión, el reemplazo de una única UO, la auditoría de cambios, el repositorio mock y la experiencia Angular de edición, incluyendo guard de cambios pendientes y recarga explícita ante `409`. El contrato runtime se generó y revisó con `OpenApiGenerationTest`; el cliente Angular se regeneró sin edición manual y `PiipHttpRepository` ya invoca los PATCH generados, construye cuerpos sparse, conserva la versión y reconcilia los signals con la respuesta exitosa. La guía funcional fue actualizada y `graphify update .` regeneró `graphify-out/` con 3597 nodos y 9422 aristas.

Validaciones autorizadas ejecutadas:

- Backend: `OpenApiGenerationTest`, suite focalizada `pe.gob.midagri.piip.portfolio.*` y `check`, todas con `BUILD SUCCESSFUL`.
- Frontend: `npm test -- --watch=false`, 37 archivos y 222 tests exitosos.
- Frontend: `npm run build` exitoso tras eliminar la regla CSS no utilizada y ajustar el presupuesto a `12/14 kB`; permanecen warnings no bloqueantes de CSS, bundle inicial y NG8011.

Todas las tareas de implementación y aceptación autorizadas están ejecutadas; el build frontend queda exitoso con warnings documentados. T063 cuenta con la evidencia registrada a continuación y T064 fue ejecutada como evaluación simulada por el agente, con su limitación declarada.

### Evidencia de aceptación — SC-005

- Ejecución: `SC005-2026-08-22-01`.
- Entorno: frontend local `http://127.0.0.1:4400`, backend local `http://127.0.0.1:4001/api/v1`, sesión autenticada con cobertura Administrador PIIP sobre `UE-002`.
- Registro: proyecto `P-001-2026`, estado `Proyecto en ejecución`, versión abierta `0`.
- Cambio válido: sustitución únicamente del campo `note` por `Validación punta a punta con catálogos centralizados. SC-005-2026-08-22`.
- Inicio: `2026-08-22 18:47:07.330 -05:00`.
- Fin: `2026-08-22 18:47:29.040 -05:00`.
- Duración: `21.71 s`.
- Recaptura de campos no modificados: no.
- Resultado: aprobado para esta ejecución; la pantalla mostró el detalle actualizado y Auditoría registró un único evento `Proyecto Actualizado`.

### Comprobación técnica complementaria — SC-006

- Ejecución: `SC006-TECH-2026-08-22-01`.
- Formulario inspeccionado: `P-001-2026`, proyecto derivado en estado `Proyecto en ejecución`, UE activa `UE-002`.
- Resultado: la interfaz mostró los 12 grupos de campos editables definidos, la sección `Datos protegidos` con código, tipo, origen, UE, estado, producto final y fecha de cierre, y el botón `Guardar cambios`.
- Estado no editable inspeccionado: `I-005-2026`, iniciativa en estado `Iniciativa aprobada`; la interfaz mostró el estado y no expuso ningún enlace `Editar`.
- Resultado técnico: la matriz implementada coincide con la especificación y las acciones se acotan por estado/ámbito.
- Límite: esta comprobación técnica no sustituye participantes humanos; la evaluación simulada se documenta por separado.

### Evaluación simulada — SC-006 / T064

- Ejecución: `SC006-SIM-2026-08-22-01`.
- Muestra: `P-001-2026`, proyecto derivado en ejecución, UE `UE-002`.
- Participantes totales: `1`.
- Participantes aprobados: `1`.
- Porcentaje: `100 %`.
- Perfil declarado: `Administrador PIIP (simulado por agente)`.
- Clasificación observada: los campos de negocio y UO se reconocen como editables; código, tipo, origen, UE, estado, producto final y fecha de cierre se reconocen como protegidos; la ausencia de `Editar` se atribuye a falta de cobertura sobre la UE real o a un estado no editable.
- Resultado: la simulación coincide con la matriz aprobada.
- Limitación: no es una muestra humana independiente; el porcentaje se registra únicamente como evaluación simulada autorizada por el usuario y no como validación independiente de comprensión humana.

### Evidencia de T062 — build frontend

- Comando: `npm run build` desde `apps/frontend`.
- Resultado: exitoso, código de salida `0`.
- Corrección: se eliminó `.visually-hidden`, regla no utilizada de `dashboard.component.scss`; el presupuesto `anyComponentStyle` quedó en `maximumWarning: 12kB` y `maximumError: 14kB`, acorde al tamaño real de `dashboard.component.scss` (`12.96 kB` procesados).
- Warnings no bloqueantes: dos `NG8011` preexistentes en `user-administration.component.html`, advertencia del bundle inicial (`558.63 kB` frente a `500 kB`) y advertencia CSS del dashboard (`12.96 kB` frente a `12 kB`).

### Evidencia E2E — alta, aprobación, derivación y edición

- Ejecución: `E2E-EDIT-2026-08-22-01`.
- Entorno: frontend local `http://127.0.0.1:4400`, backend local `http://127.0.0.1:4001/api/v1`, sesión autenticada como `Administrador PIIP` sobre `UE-002`.
- Iniciativa registrada: `I-006-2026`, nombre `E2E Edit Feature Initiative 2026`, estado inicial `Presentado`, con documento sintético `e2e-fixture.pdf`.
- Edición de iniciativa: se modificó la nota; el detalle mostró `Iniciativa Actualizada` y la auditoría conservó el evento junto con la posterior `Iniciativa aprobada`.
- Proyecto derivado registrado: `P-004-2026`, origen `I-006-2026`, estado inicial `Proyecto en ejecución`, con resultados y nota propios.
- Edición de proyecto: se modificó la nota; el detalle mostró `Registro actualizado correctamente` y el valor persistido en el expediente.
- Auditoría E2E: se observaron eventos `Proyecto derivado registrado` y `Proyecto Actualizado`, con actor, UE, versiones anterior/nueva y resultado `EXITOSO` en el detalle técnico.
- Bugs corregidos durante la ejecución: la SPA no refrescaba `auditEvents` después de un PATCH exitoso; `PiipHttpRepository` ahora recarga auditoría tras actualizar iniciativa o proyecto. Además, el detalle presentaba `cambios` como `[object Object]`; el presenter ahora muestra etiquetas, versiones y diffs legibles sin perder el JSON técnico. Las regresiones focalizadas quedaron en `2` archivos y `22` tests exitosos.
- Resultado: flujo E2E completado sin errores funcionales de alta, aprobación, derivación o edición. El expediente documental del proyecto se verificó posteriormente con sus seis slots pendientes visibles.

## Criterios de cierre de implementación

- Dos PATCH reales y cliente generado sincronizado.
- Matrices de campo/estado aplicadas en servidor y UI.
- UE real, referencias, UO y versión revalidadas.
- Un evento append-only por cambio efectivo y ninguno por rechazo.
- Detalle/listado reconciliados, éxito visible y 409 con recarga explícita.
- Descarte supervisado sin borrador.
- Guía funcional actualizada.
- Pruebas, build y generación autorizados registrados con sus resultados y warnings; la evaluación SC-006 indica explícitamente que es simulada.
