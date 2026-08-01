---
name: fe-maintain-auth-session
description: Mantener autenticación y sesión del navegador PIIP con Keycloak, PKCE, callback login, guards, interceptores y retorno seguro. Usar para cambios en apps/frontend/core relacionados con inicio de sesión, tokens o protección de rutas.
---

# Mantener autenticación frontend

1. Leer la configuración runtime, `PiipAuthService`, guards, interceptores y pruebas existentes antes de cambiar el flujo.
2. Conservar `check-sso`, flujo estándar, PKCE `S256` y callback local configurado salvo requisito aprobado distinto.
3. Validar todo retorno como ruta interna y evitar redirecciones abiertas.
4. Distinguir autenticación Keycloak de autorización funcional Oracle; no deducir permisos PIIP solo del token.
5. Mantener el envío de tokens limitado a la API PIIP y limpiar estado local al cerrar sesión.
6. No modificar realm, clientes o configuración servidor Keycloak desde frontend.
7. Modificar únicamente `apps/frontend/**` y devolver un handoff si `/identity/me` o el backend deben cambiar.
8. No borrar archivos ni ejecutar pruebas/builds sin autorización explícita del usuario.
