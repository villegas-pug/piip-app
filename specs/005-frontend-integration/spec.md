# Integración Angular

## Alcance

Sustituir el repositorio mock por un adaptador HTTP sin cambiar las rutas ni la identidad visual de PIIP Web 2, y añadir administración de usuarios desde el menú de perfil.

## Aceptación

- El token Bearer se adjunta a las llamadas API.
- PIIP presenta una portada pública de inicio de sesión antes de redirigir al cliente público de Keycloak; Angular no captura credenciales.
- El acceso ocupa el primer nivel visual y semántico de `/login`; la presentación institucional es contenido secundario y en móvil aparece después de la acción de ingreso.
- Keycloak es obligatorio en todos los entornos y conserva la ruta interna solicitada después del flujo Authorization Code con PKCE `S256`.
- La ruta de retorno de Keycloak es `/login`; cualquier retorno externo o inválido se sustituye por `/inicio`.
- El rol visual procede del backend y no puede simularse en modo integrado.
- El alta de iniciativa carga realmente la ficha inicial.
- Los mocks permanecen disponibles únicamente para pruebas y demostraciones aisladas.
- Después de autenticar, el inicio de PIIP mantiene un estado visual bloqueante hasta completar identidad, ámbitos y datos iniciales.
- El cambio de Unidad Ejecutora bloquea interacciones y comunica que se actualiza el ámbito hasta completar todas las consultas concurrentes.
- Cuando hay más de una Unidad Ejecutora autorizada, el encabezado muestra el ámbito activo mediante un botón accesible con código y nombre; su menú identifica la unidad seleccionada y conserva el bloqueo durante el cambio.
- La navegación y todas las llamadas API muestran progreso global sin parpadeos en operaciones breves.
- Cada acción CRUD visible impide envíos duplicados, muestra una animación contextual y libera el estado tanto en éxito como en error.
- En el expediente documental, cada fila con operaciones permitidas presenta un único engranaje accesible; su menú muestra solo descargar, publicar o retirar publicación y marcar como No aplica según el estado del documento y el rol Administrador PIIP.
- Mientras se procesa una operación documental, el menú se cierra, todos los engranajes quedan deshabilitados y el engranaje de la fila activa muestra una animación hasta finalizar.
- El panel de carga documental presenta un único selector compacto y accesible, informa los formatos admitidos y el archivo elegido, permite reemplazarlo y descarta la selección al cancelar sin mostrar el control nativo duplicado.
- Las tablas y listados equivalentes muestran cinco registros por página, conservan sus filtros y ofrecen paginación accesible sin modificar los contratos HTTP.
