# Fundación, organización y seguridad

## Alcance

Configurar el monolito modular, institución, unidades ejecutoras y orgánicas, autenticación Keycloak y autorización Oracle con `ADMINISTRADOR_PIIP` y `CONSULTA_EXTERNA`.

## Aceptación

- Un JWT válido no concede permisos sin una asignación local activa.
- Un administrador solo opera dentro de sus ámbitos.
- El sistema impide suspender al último administrador válido del ámbito.
- Cada acceso API genera auditoría sin cuerpos, tokens ni contenido documental.
