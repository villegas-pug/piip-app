# Guía de verificación: Administración integral de usuarios

## Precondiciones funcionales

- Contar con al menos dos usuarios locales con asignaciones de Administrador PIIP que cubran el ámbito de prueba.
- Contar con un usuario local provisionado y una asignación suspendida dentro de ese mismo ámbito.
- Iniciar sesión como Administrador PIIP con cobertura institucional o de Unidad Ejecutora adecuada.

## Recorrido manual propuesto

1. Abrir Administración de usuarios desde una UE con Administrador PIIP y comprobar que sólo aparecen usuarios y asignaciones de las instituciones donde el actor tenga algún grant Administrador.
2. Abrir “Nueva asignación”, comprobar que el combo combina usuarios administrables con candidatos locales sin asignación y crear una asignación válida para uno de ellos.
3. Editar su rol y ámbito, confirmar que conserva el mismo identificador y que el listado se actualiza.
4. Intentar duplicar una asignación activa visible y confirmar que el frontend la rechaza antes del envío; repetir desde una versión o sesión desactualizada y confirmar que el backend la rechaza sin cambio de estado.
5. Suspender la asignación, comprobar su estado y luego reactivarla sin crear una asignación nueva.
6. Confirmar que la pantalla no ofrece habilitar ni inhabilitar usuarios y que la gestión de cuentas se realiza fuera de PIIP, en Keycloak.
7. Intentar suspender, mover o convertir al último Administrador PIIP de un ámbito y confirmar el rechazo.
8. Repetir una modificación con una versión previa y confirmar el conflicto de concurrencia y un mensaje accionable.
9. Revisar el historial de auditoría para confirmar que registra actor, evento y cambios no sensibles de las asignaciones.

## Regresión de rol y ámbito

Preparar una cuenta con `CONSULTA_EXTERNA` en UE-001 y `ADMINISTRADOR_PIIP` en UE-002.

1. Seleccionar UE-001 y confirmar que el encabezado muestra Consulta externa, permite lectura y oculta acciones de creación, aprobación, carga y publicación.
2. Intentar directamente una escritura sobre UE-001 y confirmar `403` sin cambios persistidos.
3. Seleccionar UE-002 y confirmar que el encabezado muestra Administrador PIIP y habilita las operaciones administrativas válidas.
4. Con UE-001 activa, abrir el perfil y confirmar que “Administrar usuarios” permanece visible pero deshabilitado e indica UE-002 como ámbito disponible.
5. Intentar abrir `/administracion/usuarios` directamente con UE-001 y confirmar la redirección a Inicio con el mensaje de ámbito requerido.
6. Seleccionar UE-002, abrir Administración de usuarios y confirmar que el bloque contextual indica `Administrador PIIP · UE-002` mientras la bandeja reúne las asignaciones de UE-001, UE-002 y las demás UE activas de MIDAGRI.
7. Desde el módulo, cambiar nuevamente a UE-001 y confirmar que se limpia la vista administrativa, se redirige a Inicio y se explica el motivo.
8. Desde UE-002, crear o editar reversiblemente una asignación individual de UE-001 y confirmar que la operación se permite sin cambiar el rol operativo del actor en UE-001.
9. Seleccionar `Toda la institución` para el propio actor o un tercero, confirmar que la interfaz advierte el alcance y cancelar antes de persistir durante este recorrido.
10. Intentar gestionar una asignación de otra institución mediante llamada directa y confirmar `403` sin exposición ni cambio de datos.
11. Abrir mediante URL directa un registro funcional de UE-001 mientras UE-002 está activa y confirmar que las acciones se calculan con la UE real del registro.
12. Validar que documentos no publicados de UE-001 permanecen ocultos y no descargables; el permiso institucional de Administración de usuarios no amplía esa visibilidad.
13. Restaurar la asignación reversible utilizada durante el recorrido y comprobar nuevamente UE-001 `Consulta externa` y UE-002 `Administrador PIIP`.

## Automatización propuesta, no ejecutada

- Backend: pruebas focalizadas de servicio, repositorio y contrato MVC para cobertura administrativa institucional, separación funcional, transiciones, bloqueos y `ProblemDetail`.
- Frontend: pruebas Vitest del componente para catálogo administrativo, roles visibles exactos, edición, reactivación, autoasignación, errores 403/409/422 y prevención de doble envío.
- Contrato: publicación OpenAPI y regeneración del cliente Angular.

La ejecución de Gradle, pruebas Angular, generación OpenAPI, regeneración del cliente, servidores e integración Oracle requiere autorización explícita del usuario en el turno correspondiente.
