---
name: be-enforce-authorization-audit
description: Implementar o revisar autorización funcional Oracle y auditoría del backend PIIP sobre autenticación JWT Keycloak. Usar siempre para solicitudes como "el usuario entra pero no tiene permisos", "restringe esta acción por rol o unidad", "valida el ámbito", "administra usuarios", "registra quién realizó la operación" o "revisa la auditoría" en `apps/backend`. Aplicar a cambios intencionales de autorización o auditoría; una regresión reproducida también debe seguir `be-fix-reproduced-backend-bug`.
---

# Aplicar autorización y auditoría backend

## Validar el contexto de seguridad

1. Identificar la operación, el actor, el rol, el ámbito y la vigencia requeridos antes de editar. Sin ese contexto se corre el riesgo de conceder permisos más amplios que los respaldados.
2. Tratar Keycloak como autenticador y Oracle como fuente de autorización funcional. Confiar solo en el JWT confundiría identidad autenticada con autorización PIIP.
3. Conservar la validación de issuer y audience, y no asumir roles de negocio no confirmados dentro del JWT, porque un token válido no garantiza una asignación funcional vigente.

## Aplicar autorización

1. Rechazar permisos funcionales cuando no exista una asignación Oracle activa y vigente para la institución y unidad ejecutora requeridas; permitirlos dejaría operar a usuarios únicamente autenticados o fuera de su ámbito.
2. Validar la autorización nuevamente en el servicio de aplicación aunque el endpoint esté protegido. La protección exclusiva del controlador puede omitirse desde otros puntos de entrada.
3. Aplicar mínimo privilegio considerando institución, unidad ejecutora y vigencia. Ignorar cualquiera de estos límites puede ampliar indebidamente el alcance de los datos.

## Aplicar auditoría

1. Separar la auditoría de acceso HTTP de los eventos funcionales. El acceso se persiste con `REQUIRES_NEW` para sobrevivir al rollback del caso de uso; no copiar esa propagación al evento funcional.
2. Registrar el evento funcional dentro de la transacción del caso de uso, de modo que cambio y evento confirmen o reviertan juntos.
3. Auditar operaciones sensibles con actor, acción, entidad, resultado y fecha para conservar trazabilidad.
4. Excluir tokens, cuerpos HTTP y contenido documental, porque registrar esos valores puede filtrar credenciales o información sensible.

## Límites de alcance y ejecución

1. Modificar únicamente `apps/backend/**`. Si el resultado requiere mostrar u ocultar acciones en Angular, devolver el impacto al agente principal en lugar de editar frontend.
2. No borrar datos ni archivos, porque la implementación de autorización o auditoría no requiere destrucción directa.
3. No ejecutar pruebas ni integración sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Operaciones, roles, ámbitos y vigencias evaluados.
- Cambios backend propuestos o realizados y su justificación.
- Evidencia de autorización por asignación vigente, institución y unidad ejecutora en el servicio.
- Fronteras transaccionales diferenciadas para auditoría de acceso y evento funcional, sin datos sensibles.
- Pruebas ejecutadas o pendientes de autorización.
- Handoff frontend con las acciones visibles afectadas, si existe.
- Vacíos funcionales marcados como `NEEDS CLARIFICATION`.
