---
name: fe-maintain-auth-session
description: Mantener o revisar la autenticación y sesión del navegador PIIP con Keycloak, PKCE, callback de login, guards, interceptores, tokens, cierre de sesión y retorno seguro. Usar siempre para solicitudes como "cambia el inicio de sesión", "la sesión se pierde", "protege esta ruta", "ajusta el callback", "corrige el retorno después de login", "revisa el interceptor" o "limpia la sesión al salir" en `apps/frontend/src/app/core`. Si se trata de una regresión que debe corregirse, aplicar también la reproducción previa de `fe-fix-reproduced-ui-bug`.
---

# Mantener autenticación frontend

## Validar el flujo vigente

1. Leer la configuración runtime, `PiipAuthService`, guards, interceptores y pruebas existentes antes de cambiar el flujo. Omitir una de estas piezas puede dejar rutas o solicitudes con comportamientos de sesión distintos.
2. Conservar `check-sso`, el flujo estándar, PKCE `S256` y el callback local configurado salvo requisito aprobado distinto. Cambiarlos incidentalmente alteraría el protocolo de autenticación vigente.

## Proteger navegación y tokens

1. Validar todo retorno como ruta interna para evitar redirecciones abiertas hacia destinos controlados externamente.
2. Mantener el envío de tokens limitado a la API PIIP, porque adjuntarlos a otros destinos puede exponer credenciales.
3. Limpiar el estado local al cerrar sesión para impedir que datos de una sesión anterior sobrevivan al logout.
4. Distinguir autenticación Keycloak de autorización funcional Oracle. No deducir permisos PIIP solo del token, porque la asignación efectiva se resuelve en backend.

## Límites de alcance y ejecución

1. No modificar realm, clientes ni configuración del servidor Keycloak desde frontend, porque esos recursos no pertenecen al scope Angular.
2. Modificar únicamente `apps/frontend/**`.
3. Si `/identity/me` o el backend deben cambiar, devolver un handoff al agente principal en lugar de atravesar el scope.
4. No borrar archivos ni ejecutar pruebas o builds sin autorización explícita del usuario en el turno actual.

## Entrega

Presentar:

- Flujo de login, callback, retorno o cierre de sesión afectado.
- Guards, interceptores y estado local involucrados.
- Medidas aplicadas para proteger retornos y tokens.
- Separación conservada entre autenticación Keycloak y autorización Oracle.
- Pruebas ejecutadas o pendientes de autorización.
- Handoff backend o Keycloak y cualquier `NEEDS CLARIFICATION`.
