# Integración Angular

## Alcance

Sustituir el repositorio mock por un adaptador HTTP sin cambiar las rutas ni la identidad visual de PIIP Web 2, y añadir administración de usuarios desde el menú de perfil.

## Aceptación

- El token Bearer se adjunta a las llamadas API.
- El rol visual procede del backend y no puede simularse en modo integrado.
- El alta de iniciativa carga realmente la ficha inicial.
- Los mocks permanecen disponibles únicamente para pruebas y demostraciones aisladas.
- El inicio de PIIP mantiene un estado visual bloqueante hasta completar autenticación, identidad, ámbitos y datos iniciales.
- El cambio de Unidad Ejecutora bloquea interacciones y comunica que se actualiza el ámbito hasta completar todas las consultas concurrentes.
- La navegación y todas las llamadas API muestran progreso global sin parpadeos en operaciones breves.
- Cada acción CRUD visible impide envíos duplicados, muestra una animación contextual y libera el estado tanto en éxito como en error.
- En el expediente documental, cada fila con operaciones permitidas presenta un único engranaje accesible; su menú muestra solo descargar, publicar o retirar publicación y marcar como No aplica según el estado del documento y el rol Administrador PIIP.
- Mientras se procesa una operación documental, el menú se cierra, todos los engranajes quedan deshabilitados y el engranaje de la fila activa muestra una animación hasta finalizar.
