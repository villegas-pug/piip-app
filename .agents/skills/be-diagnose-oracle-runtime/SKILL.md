---
name: be-diagnose-oracle-runtime
description: Diagnosticar conectividad y runtime Oracle del backend PIIP con ojdbc11, oraclepki, wallet, perfiles y Testcontainers sin exponer secretos. Usar para errores de arranque, datasource, TLS, wallet o integración Oracle en apps/backend.
---

# Diagnosticar runtime Oracle

1. Reunir el error exacto, perfil activo, versión Java, dependencias runtime y configuración no secreta.
2. No leer, imprimir, copiar ni versionar contraseñas, tokens, wallets o archivos `.env` sensibles.
3. Separar resolución de dependencias, carga del wallet, red/TLS, credenciales y compatibilidad del datasource como hipótesis distintas.
4. Inspeccionar `pom.xml`, configuración Spring y pruebas Oracle antes de proponer cambios.
5. Ejecutar Docker, Testcontainers, conexión real o `mvn verify -Pintegration-tests` solo con autorización explícita y prerequisitos confirmados.
6. No declarar conectividad resuelta basándose únicamente en compilación o pruebas sin ADB real.
7. Mantener el diagnóstico read-only; cualquier corrección de código requiere una tarea backend explícita y reproducción suficiente.
8. No borrar wallets, caches, contenedores o archivos.
