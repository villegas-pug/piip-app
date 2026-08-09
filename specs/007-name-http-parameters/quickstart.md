# Guía de validación: nombrar parámetros HTTP

## Precondiciones

- Tener autorizada la ejecución de pruebas o del backend en el turno correspondiente.
- Trabajar sobre la feature activa `specs/007-name-http-parameters`.

## Validación automatizada propuesta

1. Ejecutar la prueba unitaria focalizada de enlaces HTTP del backend.
2. Verificar que cubre las 36 entradas de ruta, consulta y multipart del inventario contractual.
3. Ejecutar la suite backend solo si se autoriza expresamente.

## Validación manual propuesta

1. Invocar una operación existente que use una variable de ruta y comprobar que recibe el valor previsto.
2. Invocar una operación de listado con parámetros de consulta y comprobar nombres, opcionalidad y valores por defecto actuales.
3. Cargar un documento con la parte multipart `file` y comprobar que la operación mantiene su comportamiento actual.
4. Repetir una solicitud con entrada ausente o inválida y comprobar que conserva la respuesta de validación previa.

## Resultado esperado

Ningún cliente necesita cambiar rutas, nombres de parámetros, formato multipart ni cuerpos de solicitud o respuesta.
