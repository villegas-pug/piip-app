---
name: fe-improve-filter-visual-ux
description: "Mejorar visualmente los filtros Angular de una página PIIP indicada mediante el parámetro obligatorio target_page, preservando comportamiento, accesibilidad y alcance frontend."
---

# Mejorar filtros visuales PIIP

Mejora la apariencia de los filtros de una página Angular del PIIP usando los patrones visuales existentes, sin convertir el trabajo en una modificación funcional.

## Parámetro obligatorio

La solicitud DEBE incluir exactamente una página destino mediante `target_page`, expresada como ruta Angular absoluta, por ejemplo:

```text
target_page=/iniciativas
```

Antes de inspeccionar el repositorio, resolver la ruta o editar archivos:

1. Extraer `target_page` del mensaje actual del usuario.
2. Detenerse si falta, está vacío, contiene varias páginas o no empieza por `/`.
3. En ese caso, responder exactamente:

   `Detenido: falta el parámetro obligatorio target_page. Ejemplo: target_page=/iniciativas`

No usar como sustituto la URL abierta del navegador, el archivo activo, una ruta mencionada en contexto ambiental ni una inferencia del contenido de la solicitud.

Si `target_page` existe pero no coincide con una ruta Angular real, detenerse e informar la ruta no encontrada sin modificar archivos.

## Flujo

1. Resolver `target_page` en las rutas reales de `apps/frontend` y localizar el componente, plantilla y hoja de estilos de la página.
2. Leer el componente destino, sus estilos y una vista comparable existente —preferentemente Inicio/dashboard— para reutilizar tokens, espaciado, colores, radios, estados de foco y breakpoints.
3. Registrar como baseline cualquier cambio previo del usuario y no revertirlo ni sobrescribirlo.
4. Aplicar el cambio mínimo en la plantilla y estilos de la página destino. Mantener los controles existentes y sus bindings; no crear un componente global solo para esta mejora.
5. Comprobar que la composición funciona en escritorio, tablet y móvil, incluyendo campos estrechos, textos largos, selects deshabilitados y botones de limpieza.

## Reglas visuales y de accesibilidad

- Conservar búsqueda, selects, fecha, limpieza, paginación, estados de carga/vacío/error y formularios tal como funcionan actualmente.
- Mantener los elementos `label`, nombres accesibles, `type="button"`, foco visible y navegación por teclado.
- Usar los tokens visuales del proyecto y reutilizar patrones equivalentes antes de introducir valores nuevos.
- En controles flex que combinan iconos e inputs, aplicar `min-width: 0` al contenedor y permitir la contracción del input con `flex: 1 1 0` y `width: 0` cuando sea necesario.
- Para un icono Material dentro de un control, fijar `width`, `height`, `flex-basis`, `font-size` y `line-height` coherentes; no reservar un espacio menor que el glifo.
- En un campo `input[type="date"]`, conservar el input nativo. Ocultar el indicador nativo únicamente si existe un icono personalizado visible, para evitar duplicados y desbordamientos; no ocultar ambos indicadores.
- Usar `overflow: hidden` solo en el contenedor visual cuando no impida leer, enfocar o utilizar el control.

## Límites

- Modificar únicamente `apps/frontend/**` y, por defecto, solo la plantilla y estilos de la página resuelta.
- No modificar backend, contratos HTTP, catálogos, documentación funcional, rutas ni lógica TypeScript.
- Si la mejora requiere cambiar comportamiento, API, estado o TypeScript más allá de una adaptación semántica mínima, detenerse y devolver el bloqueo en lugar de ampliar el alcance.
- No iniciar servidores, ejecutar builds ni pruebas automatizadas sin autorización explícita del usuario en el turno actual.

## Verificación y entrega

- Ejecutar `git diff --check` y revisar `git diff --name-only` para confirmar el alcance.
- Si ya existe un runtime local disponible, revisar visualmente `target_page` en escritorio y un viewport móvil; no iniciar uno nuevo.
- Si no existe runtime, declarar la verificación visual como pendiente y conservar la verificación estática.
- No afirmar que una prueba o build pasó si no fue autorizada y ejecutada.

La entrega debe indicar: `target_page` resuelto, componente y archivos modificados, cambios visuales aplicados, comportamiento y accesibilidad preservados, verificaciones ejecutadas y validaciones pendientes.
