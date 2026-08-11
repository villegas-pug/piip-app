# Guía de verificación: Administración integral de usuarios

## Precondiciones funcionales

- Contar con al menos dos usuarios locales con asignaciones de Administrador PIIP que cubran el ámbito de prueba.
- Contar con un usuario local provisionado y una asignación suspendida dentro de ese mismo ámbito.
- Iniciar sesión como Administrador PIIP con cobertura institucional o de Unidad Ejecutora adecuada.

## Recorrido manual propuesto

1. Abrir Administración de usuarios y comprobar que solo aparecen usuarios y asignaciones del ámbito administrable.
2. Abrir “Nueva asignación”, comprobar que el combo combina usuarios administrables con candidatos locales sin asignación y crear una asignación válida para uno de ellos.
3. Editar su rol y ámbito, confirmar que conserva el mismo identificador y que el listado se actualiza.
4. Intentar duplicar una asignación activa visible y confirmar que el frontend la rechaza antes del envío; repetir desde una versión o sesión desactualizada y confirmar que el backend la rechaza sin cambio de estado.
5. Suspender la asignación, comprobar su estado y luego reactivarla sin crear una asignación nueva.
6. Confirmar que la pantalla no ofrece habilitar ni inhabilitar usuarios y que la gestión de cuentas se realiza fuera de PIIP, en Keycloak.
7. Intentar suspender, mover o convertir al último Administrador PIIP de un ámbito y confirmar el rechazo.
8. Repetir una modificación con una versión previa y confirmar el conflicto de concurrencia y un mensaje accionable.
9. Revisar el historial de auditoría para confirmar que registra actor, evento y cambios no sensibles de las asignaciones.

## Automatización propuesta, no ejecutada

- Backend: pruebas focalizadas de servicio, repositorio y contrato MVC para autorización, transiciones, bloqueos y `ProblemDetail`.
- Frontend: pruebas Vitest del componente para edición, reactivación, estados de asignación, errores 403/409/422 y prevención de doble envío.
- Contrato: publicación OpenAPI y regeneración del cliente Angular.

La ejecución de Gradle, pruebas Angular, generación OpenAPI, regeneración del cliente, servidores e integración Oracle requiere autorización explícita del usuario en el turno correspondiente.
