# E2E PIIP con evidencia HTTP

Esta suite requiere que frontend, backend y la sesión Keycloak estén disponibles. No guarda credenciales ni crea una sesión automáticamente.

Configura una sesión autenticada fuera del repositorio y ejecuta la prueba mutante solo con un expediente de prueba en estado `Presentado`:

```powershell
$env:PIIP_E2E_STORAGE_STATE = 'C:\ruta\fuera\del\repositorio\auth.json'
$env:PIIP_E2E_INITIATIVE_CODE = 'I-XXX-2026'
$env:PIIP_E2E_ALLOW_MUTATIONS = 'true'
npm run e2e -- --grep "navega al proyecto"
```

La salida JSON se escribe en `test-results/playwright-results.json` y cada prueba adjunta `http-evidence.json`, con respuestas, `X-Correlation-Id`, errores de consola y solicitudes fallidas.
