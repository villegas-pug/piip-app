# Investigación: nombrar parámetros HTTP del backend

## Decisión 1: explicitar los nombres públicos existentes

- **Decisión**: cada entrada de ruta, consulta o multipart usará el mismo nombre público actualmente expuesto por la operación.
- **Rationale**: preserva las solicitudes del frontend y de otros consumidores, y evita depender de metadatos internos de compilación para asociar la entrada HTTP.
- **Alternativas consideradas**:
  - Confiar únicamente en la opción de compilación existente: descartada porque la feature busca que la asociación sea explícita en todos los artefactos de ejecución.
  - Cambiar nombres públicos para normalizarlos: descartada porque rompería la compatibilidad sin aportar valor funcional.

## Decisión 2: cubrir el inventario completo de controladores afectados

- **Decisión**: actualizar los seis controladores que contienen las 36 entradas identificadas y validar el inventario como una unidad.
- **Rationale**: un cambio parcial mantendría puntos de fallo equivalentes y no cumpliría el requisito de cobertura total.
- **Alternativas consideradas**:
  - Corregir solo la operación que manifestó el problema: descartada porque no previene el mismo fallo en los demás enlaces implícitos.

## Decisión 3: validar sin infraestructura externa

- **Decisión**: añadir una prueba unitaria basada en reflexión sobre las anotaciones de enlace HTTP, complementada por escenarios manuales propuestos.
- **Rationale**: verifica todos los nombres explícitos sin levantar el backend ni requerir Oracle, credenciales o archivos externos.
- **Alternativas consideradas**:
  - Solo pruebas de extremo a extremo: descartada como única evidencia porque no cubre exhaustivamente el inventario y requiere infraestructura adicional.
