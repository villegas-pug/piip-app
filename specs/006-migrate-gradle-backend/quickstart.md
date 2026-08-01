# Quickstart documental: backend Gradle

Este documento describe comprobaciones posteriores para una sesión autorizada. No fueron ejecutadas al generar Spec Kit.

## Windows

Desde `apps/backend`:

```powershell
.\gradlew.bat test
```

Para iniciar el backend desde VS Code se debe usar la configuración Java vigente y las variables Oracle locales autorizadas. El endpoint esperado es:

```text
http://127.0.0.1:4001/api/v1/actuator/health
```

## Artefactos esperados

Una ejecución autorizada debe dejar:

- `apps/backend/target/piip-openapi.json`
- `apps/backend/target/piip-oracle.sql`

El DDL debe compararse con `database/generated/piip-oracle.sql`.

## Alcance de esta fase

- No se ejecutó ningún comando de build o test.
- No se ejecutó Docker ni Testcontainers.
- No se ejecutó `integrationTest` ni una conexión Oracle.
- No se modificó frontend, código Java, Gradle ni documentación operativa.
