# PIIP Web 2

Prototipo funcional de alta fidelidad para validar la gestión de iniciativas y proyectos PIIP.

## Stack

- Angular 22
- Angular Material 22
- Componentes standalone
- Formularios reactivos tipados
- Repositorio mock reemplazable por servicios HTTP

## Fuente funcional

Los 23 campos y los seis catálogos se basan en `../../outputs/analisis-campos-piip-y-relacion-leyenda-v3.md`.

- `Borrador` es una condición local de la interfaz, no un estado del Excel.
- `Evaluación` es una etapa visual, no un estado oficial.
- Los datos de alertas, fechas, responsables y métricas son demostrativos.
- La obligatoriedad documental posterior permanece pendiente de validación.

## Ejecución

```bash
npm install
npm start -- --host 127.0.0.1 --port 4400
```

Rutas principales: `/inicio`, `/iniciativas`, `/iniciativas/nueva`, `/proyectos`, `/proyectos/nuevo/preexistente`, `/documentos` y `/auditoria`.

La bandeja global `/documentos` abre los expedientes mediante rutas contextuales: `/iniciativas/:code/documentos` y `/proyectos/:code/documentos`.

El detalle `/iniciativas/:code` permite consultar la ficha, registrar la aprobación `Presentado → Iniciativa aprobada` y abrir `/proyectos/nuevo/derivado/:initiativeCode`. La aprobación no crea automáticamente el proyecto: habilita un formulario precargado y una confirmación independiente.

La demostración conserva únicamente los perfiles `Administrador PIIP` y `Consulta externa`. Los documentos pendientes generan una advertencia informativa, pero no bloquean la aprobación.

El alta directa de `/proyectos/nuevo/preexistente` se limita a proyectos que ya se encontraban en ejecución sin iniciativa formal de origen. Aplica `NA` al código de origen, `No aplica` al tipo de solución y `Proyecto en ejecución` como estado oficial.

## Verificación

```bash
npm test -- --watch=false
npm run build
```

Las imágenes canónicas usadas para la validación visual están en `docs/ui-reference`.
