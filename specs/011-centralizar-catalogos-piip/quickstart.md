# Quickstart de implementación y verificación: Catálogos centralizados PIIP

## Propósito

Esta guía orienta una implementación futura de la feature 011. No constituye autorización para modificar producto, ejecutar el reset, generar OpenAPI, compilar, probar, iniciar servicios, usar Oracle ni realizar acciones Git.

## Prerrequisitos de artefactos

Antes de `/speckit-implement` deben existir y estar aprobados:

- [spec.md](./spec.md)
- [plan.md](./plan.md)
- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/catalogs.openapi.yaml](./contracts/catalogs.openapi.yaml)
- `tasks.md`, generado posteriormente mediante `/speckit-tasks`
- checklist sin pendientes bloqueantes ni `NEEDS CLARIFICATION` aplicable al ambiente de pruebas

Los tres pendientes productivos de `spec.md` no autorizan valores oficiales ni una migración destructiva en producción.

## Secuencia de trabajo

### 1. Modelo y datos canónicos backend

1. Crear el módulo `catalogs` con entidades, repositorios y servicio de consulta.
2. Adaptar `PortfolioRecordEntity`, `DocumentEntity` y `ResponsibleUnitEntity` conforme a `data-model.md`.
3. Mantener tipo de registro como catálogo técnico; no convertir estados, componente digital ni producto final.
4. Incorporar el seed DML externo con códigos estables e idempotencia.
5. Implementar validaciones de existencia, catálogo esperado, activo y pertenencia organizacional en servicios transaccionales.

Punto de control: ninguna entidad JPA se expone por HTTP y no quedan columnas legacy junto a las nuevas en el esquema de pruebas derivado.

### 2. Perfil de reset exclusivo de pruebas

1. Crear `application-test-reset.yml` con `ddl-auto=none` y aplicación no web.
2. Exigir perfiles `test,test-reset`; rechazar `prod` y cualquier perfil no autorizado.
3. Exigir confirmación explícita y allowlist de huella JDBC/usuario-esquema; compararlas con metadata de conexión antes de cualquier acción.
4. Excluir `IdentityBootstrap` del perfil de reset.
5. Construir el preflight sobre el `Metadata` Hibernate y la allowlist exacta de tablas de `plan.md`.
6. Ejecutar drop/create mediante `SchemaDropper`/`SchemaCreator` sobre las trece tablas permitidas, incluidas las dos tablas de auditoría y `NOTIFICACION`.
7. Comprobar que auditoría y notificaciones se recrearon vacías y cargar el SQL DML autorizado por la constitución 1.2.0.
8. Verificar postcondiciones y finalizar. Ante un error, detener etapas posteriores y devolver nombre de etapa y causa segura.

Ejemplo conceptual de guardias —los nombres definitivos deben quedar documentados en configuración sin incluir secretos:

```properties
spring.profiles.active=test,test-reset
piip.test-reset.enabled=true
piip.test-reset.confirmation=RESET-PIIP-TEST
piip.test-reset.allowed-jdbc-fingerprint=<sha256 esperado>
piip.test-reset.allowed-schema=<usuario-esquema esperado>
```

La huella evita registrar la URL JDBC completa en logs. Perfil y confirmación sin coincidencia de conexión no habilitan el proceso.

Solo `ORA-00942` durante el `DROP` de la tabla allowlisted actualmente procesada puede tratarse como recuperación de una ejecución parcial. El mismo código en otra etapa y cualquier otro error Oracle detienen el proceso.

### 3. Servicios y contrato HTTP

1. Implementar `CatalogQueryService` y hacer que `CatalogController` delegue.
2. Adaptar DTO y servicios de creación/lectura de portafolio.
3. Adaptar DTO, rutas y servicios documentales para `documentTypeId`.
4. Mantener los filtros HTTP vigentes; no agregar parámetros por catálogo ni filtros nuevos a la bandeja documental.
5. Mantener `GET /organizational-units` acotado por Unidad Ejecutora, autorización y orden por nombre, sin agregar `displayOrder`.
6. Revisar errores 401/403/404/409/422 y no convertir fallos de referencia en errores genéricos.
7. Comparar el OpenAPI real con [contracts/catalogs.openapi.yaml](./contracts/catalogs.openapi.yaml).

Punto de control: una respuesta histórica lleva la referencia inactiva embebida; el bundle de opciones solo contiene activos.

### 4. OpenAPI y cliente Angular

Con autorización explícita para generación:

1. Generar `apps/backend/target/piip-openapi.json` desde controladores/DTO vigentes.
2. Revisar el diff contractual antes de consumirlo.
3. Regenerar `apps/frontend/src/app/api/generated/**` mediante el flujo del repositorio.
4. No editar archivos generados a mano.

Punto de control: los requests generados usan IDs y las respuestas contienen referencias estructuradas; las rutas documentales ya no reciben el enum textual.

### 5. Estado y adaptadores Angular

1. Definir modelos de opción activa y referencia histórica.
2. Incorporar una fachada/store de catálogos globales y Unidades Orgánicas por UE.
3. Modelar `loading`, `loaded-empty`, `error`, reintento y conservación por ID.
4. Proteger la carga por UE frente a respuestas tardías.
5. Adaptar `PiipRepository`, `PiipHttpRepository` y `PiipMockRepository`.
6. Retirar conversiones etiqueta→enum, búsqueda de unidades por sigla/nombre y fallbacks funcionales.

Cuando un refresh o un 422 indique que la selección quedó inactiva, conservarla solo como contexto, invalidar el control y pedir elegir una opción vigente; nunca reemplazarla automáticamente.

Punto de control: `PIIP_CATALOGS` solo conserva conceptos fuera de las siete fuentes y opciones locales como `Todos/Todas` no se envían al backend.

### 6. Consumidores funcionales

Adaptar en este orden para limitar el impacto:

1. Crear iniciativa.
2. Crear proyecto preexistente.
3. Crear proyecto derivado, incluida referencia heredada inactiva.
4. Gestión documental de iniciativa/proyecto y presentación de auditoría.
5. Dashboard, sustituyendo `Iniciativa`/`Proyecto` hardcodeados por `recordTypes` y manteniendo `Todos` local.
6. Listados/filtros vigentes de iniciativas y proyectos, sin crear filtros nuevos.
7. Bandeja documental, conservando búsqueda, Tipo de registro, estado y Unidad Orgánica.
8. Detalles de iniciativa y proyecto.

Cada consumidor debe cubrir:

- carga desde backend;
- carga en curso;
- respuesta vacía;
- error y reintento;
- selección por identidad;
- conservación de una selección aún activa;
- histórico inactivo visible pero no seleccionable;
- ausencia de fallback hardcodeado.

### 7. Documentación

Actualizar `docs/funcional/guia-funcional-piip.md` como recorrido cronológico:

1. la persona abre el registro y espera los catálogos;
2. PIIP valida opciones y Unidad Ejecutora;
3. la persona selecciona y guarda IDs;
4. PIIP muestra nombres resueltos en listados/detalles;
5. un valor inactivo permanece visible en historia, pero se reemplaza en una nueva operación;
6. los tipos documentales conservan su ciclo vigente.

Documentar el reset en una guía técnica separada o en la sección de desarrollo pertinente, señalando que es destructivo, exclusivo de pruebas y fail-closed.

## Recorridos de aceptación propuestos

### Catálogo global

```text
GET /api/v1/catalogs
→ seis grupos globales
→ solo activos
→ orden determinista
→ IDs solo en los cinco grupos persistentes
→ PEI/POI sin marca visible de oficialidad
```

### Unidad Orgánica

```text
seleccionar Unidad Ejecutora
→ GET /api/v1/organizational-units?executingUnitId={id}
→ descartar una respuesta anterior si cambió la UE
→ guardar organizationalUnitId
→ backend valida activo + pertenencia + ámbito
```

### Histórico inactivo

```text
ítem usado pasa a inactivo
→ desaparece de GET /catalogs
→ sigue embebido con active=false en el registro histórico
→ una nueva escritura con su ID responde 422
```

### Proyecto derivado

```text
iniciativa origen contiene ítem inactivo
→ formulario muestra referencia histórica aparte
→ control de nueva escritura queda inválido
→ persona elige ID activo
→ creación permitida
```

### Documento

```text
seleccionar documentType.id
→ cargar versión por /documents/{documentTypeId}/versions
→ conservar unicidad registro + tipo
→ conservar contenido, metadata, publicación, No aplica y auditoría
```

### Reset de pruebas

```text
perfil normal
→ validate
→ cero limpieza/carga

test + test-reset + confirmación + conexión allowlisted
→ preflight
→ drop selectivo de trece tablas, incluidas toda la auditoría y NOTIFICACION
→ create selectivo desde Metadata JPA
→ auditoría y notificaciones vacías
→ seed DML idempotente
→ postvalidación
→ éxito inequívoco
```

## Matriz de validación propuesta

| Nivel | Verificación | Evidencia esperada |
|-------|--------------|--------------------|
| Dominio/servicio | ID inexistente, catálogo incorrecto, inactivo y UO ajena | Rechazo sin escritura parcial. |
| JPA | UK, FK, asociaciones y lectura de inactivos | Modelo derivado sin columnas legacy. |
| Contrato | Bundle, requests por ID, respuestas estructuradas | OpenAPI coincide con contrato de diseño. |
| Reset | guardias, allowlist, orden, fallo por etapa, reejecución | Nunca toca tablas protegidas; segunda ejecución no duplica. |
| Oracle | recreación selectiva y preservación | Usuarios/roles/ámbitos/institución/UE/UO intactos; auditoría y notificaciones recreadas vacías. |
| Angular core | loading/vacío/error/request-id/conservación | Estado determinista sin fallback. |
| Formularios | tres creaciones e inactivo heredado | IDs enviados y bloqueo correcto. |
| Documentos | seis posiciones y ciclo completo | Sin regresión de versiones/contenido/publicación/No aplica. |
| Consultas | listados, filtros, bandeja y detalles | Misma identidad/nombre en todos los consumidores. |
| Documentación | guía funcional y guía de reset | Flujo comprensible y límites explícitos. |

## Comandos manuales futuros

Los comandos exactos deben tomarse del repositorio en el turno autorizado. Como categorías:

- pruebas backend focalizadas y de arquitectura;
- prueba de generación Oracle desde JPA;
- generación de OpenAPI;
- regeneración del cliente Angular;
- comprobación estática TypeScript;
- pruebas de componentes Angular;
- integración Oracle del reset en un datasource desechable/allowlisted;
- pruebas funcionales del recorrido completo.

No ejecutar el perfil `test-reset` contra una conexión no verificada ni usar `application-dev.yml` como sustituto del flujo selectivo.
