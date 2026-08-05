---
name: be-diagnose-oracle-runtime
description: Diagnosticar fallos de conexión, arranque o integración Oracle del backend PIIP sin exponer secretos. Usar siempre ante mensajes como "no conecta a Oracle", "falla el datasource", errores ORA, TLS o wallet, perfiles Spring incorrectos, incompatibilidades del controlador JDBC Oracle, `oraclepki`, Docker o Testcontainers en `apps/backend`. Mantener el trabajo read-only; no usar esta SKILL para aplicar una corrección de código aún no reproducida.
---

# Diagnosticar runtime Oracle

## Reunir evidencia

1. Obtener el error exacto, perfil activo, versión Java, dependencias runtime y configuración no secreta. Sin esos datos, errores de red, wallet y dependencias pueden parecer el mismo problema.
2. Inspeccionar `build.gradle.kts`, el Gradle Wrapper, la configuración Spring y las pruebas Oracle antes de proponer cambios. Esto evita recomendar dependencias, tareas o propiedades que el proyecto ya tiene o no utiliza.

## Separar hipótesis

1. Evaluar por separado la resolución de dependencias, carga del wallet, red/TLS, credenciales y compatibilidad del datasource. Mezclar estas causas puede producir una corrección aparente que oculte el fallo real.
2. No declarar resuelta la conectividad basándose únicamente en compilación o pruebas sin ADB real. Esas comprobaciones validan código y dependencias, pero no demuestran acceso al servicio Oracle objetivo.

## Proteger información y ejecución

1. No leer, imprimir, copiar ni versionar contraseñas, tokens, wallets o archivos `.env` sensibles, porque el diagnóstico no justifica exponer credenciales.
2. Mantener el diagnóstico read-only. Cualquier corrección de código requiere una tarea backend explícita y reproducción suficiente para no modificar el runtime por hipótesis.
3. Ejecutar Docker, Testcontainers, una conexión real o `gradlew.bat integrationTest` en Windows (`./gradlew integrationTest` en Linux/macOS) solo con autorización explícita del usuario en el turno actual y con sus prerrequisitos confirmados.
4. No borrar wallets, cachés, contenedores ni archivos, porque su eliminación destruye evidencia y puede afectar otros entornos.

## Entrega

Presentar:

- Error y contexto reunidos sin incluir secretos.
- Hipótesis comprobadas, descartadas y pendientes.
- Conclusión diferenciando diagnóstico técnico de conectividad ADB confirmada.
- Comandos ejecutados y sus resultados.
- Comandos no ejecutados y la autorización o prerrequisito que requieren.
- Corrección backend propuesta, si corresponde, sin aplicarla desde esta SKILL.
