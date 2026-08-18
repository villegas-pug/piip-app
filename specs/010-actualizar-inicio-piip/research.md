# Investigación: Actualización de Inicio PIIP

## Alcance de la investigación

Se contrastó la especificación con el backend, el frontend, la autorización, los contratos y la feature 009. La investigación fue estática: no se ejecutaron pruebas, builds, servidores, generación OpenAPI ni Oracle.

## Decisión 1: consulta unificada de portafolio para Inicio

**Decisión**: agregar `GET /api/v1/dashboard/portfolio` y conservar los endpoints y el resumen legado existentes.

**Fundamento**: `/initiatives` y `/projects` producen páginas y totales independientes. Fusionarlas en Angular no puede garantizar una página global ordenada ni conteos reconciliados sin recuperar todos los registros.

**Alternativas descartadas**:

- Fusionar dos páginas en Angular: rompe el orden y el total global.
- Recuperar todas las páginas y ordenarlas en navegador: correcto en conjuntos pequeños, pero no escala.
- Ampliar `/dashboard`: mezclaría el nuevo portafolio con tareas y alertas legadas y aumentaría el riesgo de ruptura.

## Decisión 2: filtros, orden y paginación pertenecen al backend

**Decisión**: `executingUnitId` obligatorio; `q`, `type` y `status` opcionales; página cero y tamaño cinco por defecto; orden fijo `updatedAt DESC, id DESC`.

**Fundamento**: la especificación exige una única secuencia global estable y una UE activa exacta. El desempate por identificador evita saltos cuando coinciden fechas sin añadir una regla visible al negocio.

**Alternativas descartadas**:

- Exponer `sort` y `direction`: amplía una decisión funcional que ya está cerrada.
- Usar únicamente `updatedAt`: deja resultados inestables ante empates.

## Decisión 3: una fuente canónica de predicados

**Decisión**: centralizar en un repositorio de consulta JPA los predicados de UE, código/nombre, tipo y estado, reutilizados por página y agrupación.

**Fundamento**: evita que listado e indicadores representen conjuntos distintos. La búsqueda de Inicio no reutiliza el criterio adicional por responsable de los módulos completos.

**Alternativas descartadas**:

- Duplicar Specifications: facilita deriva entre página y conteos.
- SQL nativo o `JdbcTemplate`: contradice la arquitectura del proyecto.

## Decisión 4: reconciliación y distinción de vacíos

**Decisión**: incluir `statusCounts` y `executingUnitTotalElements`; calcular `totalElements` como suma de los conteos positivos filtrados.

**Fundamento**: el primer dato reconcilia indicadores y listado; el segundo diferencia una UE realmente vacía de filtros sin coincidencias.

**Alternativas descartadas**:

- Inferir vacío desde la página: no distingue ambos estados.
- Contar solo la página visible: contradice FR-009 y FR-010.

## Decisión 5: autorización exacta de Unidad Ejecutora

**Decisión**: el servicio valida `requireReadableUnit(executingUnitId)` antes de consultar.

**Fundamento**: Inicio se limita a la UE activa, aunque el usuario tenga cobertura institucional o varias unidades. Una UE no autorizada debe producir 403, no aparentar ausencia de datos.

## Decisión 6: contrato mínimo y aditivo

**Decisión**: crear DTO específicos de Inicio y no exponer `PortfolioRecordEntity` ni reutilizar un DTO sobredimensionado.

**Fundamento**: el listado necesita únicamente tipo, código, nombre, estado, UE y fecha técnica para el orden. El contrato aditivo preserva consumidores existentes.

## Decisión 7: notificaciones existentes, lectura explícita

**Decisión**: conservar `GET /notifications` y `PUT /notifications/{id}/read`; no añadir referencia contextual, paginación, lectura masiva ni disparadores.

**Fundamento**: el contrato actual ya contiene la información exigida y pertenece al usuario autenticado. No ofrece una referencia confiable para enlaces.

**Alternativas descartadas**:

- Usar notificaciones del resumen `/dashboard`: mezcla responsabilidades y no es la fuente personal completa.
- Marcar al abrir la campana o expandir: contradice FR-022 y FR-023.

## Decisión 8: estado y experiencia Angular

**Decisión**: Inicio administra consulta, presentación y estados independientes; el repositorio adapta el contrato. Se reutiliza la paginación de cinco elementos, se aplica debounce de 300 ms, se restablece `Estado: Todos` cuando el tipo invalida la selección y se descartan respuestas obsoletas.

**Fundamento**: cambios rápidos de UE o filtros no deben mostrar resultados de una consulta anterior. Separar portafolio y notificaciones evita representar un fallo parcial como vacío general.

## Decisión 9: resumen de notificaciones

**Decisión**: mostrar tres avisos en modo compacto, expandir en línea la lista completa de la pestaña activa y hacer que la campana lleve y enfoque `Mis notificaciones` sin cambiar lecturas.

**Fundamento**: las aclaraciones funcionales fijan tres elementos y el comportamiento de la campana; se conserva la composición aprobada sin copiar datos ni crear un segundo panel de notificaciones.

## Riesgos y mitigaciones

| Riesgo | Mitigación de diseño |
|--------|----------------------|
| Deriva entre página y agrupación | Constructor único de predicados y pruebas de reconciliación. |
| Empates de fecha | Desempate fijo por `id DESC`. |
| Cambios concurrentes entre lecturas | Transacción de solo lectura y total autoritativo derivado del agregado; no se promete aislamiento adicional no especificado. |
| Consulta costosa a gran volumen | Paginación y agregación en base de datos; evaluar índices solo con plan de ejecución Oracle autorizado. |
| Respuestas frontend fuera de orden | Clave/cancelación de consulta y descarte de respuestas obsoletas. |
| Etiquetas de estado divergentes | Códigos enum en parámetros y etiquetas canónicas en respuesta/presentación. |

## Resultado

No quedan `NEEDS CLARIFICATION` funcionales o técnicos bloqueantes para generar tareas. La consistencia física de snapshot bajo escrituras concurrentes no fue definida por negocio y no se eleva a regla inventada.
