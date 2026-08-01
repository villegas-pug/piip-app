---
name: be-enforce-authorization-audit
description: Implementar o revisar autorización Oracle y auditoría backend PIIP sobre autenticación JWT Keycloak. Usar para roles, ámbitos, filtros de autoridad, administración de usuarios u operaciones sensibles en apps/backend.
---

# Aplicar autorización y auditoría backend

1. Tratar Keycloak como autenticador y Oracle como fuente de roles y ámbitos funcionales.
2. Rechazar permisos funcionales cuando no exista asignación Oracle activa y vigente.
3. Validar autorización nuevamente en el servicio de aplicación, incluso si el endpoint ya está protegido.
4. Aplicar mínimo privilegio considerando institución, unidad ejecutora y vigencia.
5. Auditar operaciones sensibles con actor, acción, entidad, resultado y fecha; excluir tokens, cuerpos HTTP y contenido documental.
6. Conservar validación de issuer/audience y evitar confiar en roles de negocio no confirmados del JWT.
7. Modificar únicamente `apps/backend/**` y devolver un handoff sobre efectos visibles requeridos en frontend.
8. No borrar datos/archivos ni ejecutar pruebas o integración sin autorización explícita del usuario.
