# Trabajo, notificaciones y auditoría

## Alcance

Persistir tareas y notificaciones, calcular alertas desde vencimientos y registrar eventos funcionales y accesos técnicos append-only.

## Aceptación

- Registrar una iniciativa crea una tarea de decisión a 20 días calendario.
- Aprobar completa esa tarea y crea la tarea de proyecto derivado sin vencimiento.
- Una alerta se considera próxima a vencer cuando restan tres días.
- La auditoría de acceso sobrevive a rollbacks funcionales.
- El historial de eventos muestra usuario, evento y observación en lenguaje funcional; el subject y JSON originales permanecen disponibles únicamente en el detalle técnico para Administrador PIIP.
- En pantallas pequeñas, el historial presenta cada evento como tarjeta sin desplazamiento horizontal.
