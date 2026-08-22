# Evidencia de implementación — feature 012

Este registro distingue cambios escritos, comprobaciones estáticas y validaciones que todavía requieren autorización para ejecutarse. Un checkbox de `tasks.md` no sustituye evidencia de runtime.

## Baseline

| Área | Baseline | Propietario | Criterio observable | Estado |
|---|---|---|---|---|
| HTTP | `contracts/http-compatibility.md` y pruebas MVC existentes | API/application | método, ruta, binding, JSON, status y errores sin cambios | caracterización pendiente de ejecución |
| Autorización | `LocalAuthorizationServiceTest`, concurrencia y pruebas de portfolio | identity/application | grant exacto, UE real y revocación conservan decisión | código actualizado; ejecución pendiente |
| Efectos | pruebas de portfolio, documents y auditoría | application | versión, tareas, notificaciones, documentos y auditoría mantienen atomicidad | ejecución pendiente |
| Protección | frontend, OpenAPI, cliente, JPA, DDL, Oracle y guía funcional | repositorio | cero cambios fuera de backend y documentación arquitectónica | revisión estática pendiente; la configuración de launch Oracle se ajustó por autorización explícita posterior |

## Incrementos

| Incremento | Resultado escrito | Riesgo | Dependencias | Reversión | Verificación |
|---|---|---|---|---|---|
| 0 | Caracterización y matriz congeladas en `tasks.md` | salida no caracterizada | ninguna | restaurar baseline antes de mover código | pruebas escritas; no ejecutadas |
| 1 | Errores tipados en `shared/application/error` y handler sin catch genérico | diferencia 422/500 | incremento 0 | restaurar adaptador de errores | revisión estática; pruebas pendientes |
| 2 | `OrganizationQueryService` y `OrganizationReadModels` | cambio de filtros/orden | shared | restaurar controller | pruebas pendientes |
| 3 | `AuditReadModels`, `AuditQueryService` e identity application | actor nullable o scopes alterados | shared + organization | restaurar servicios por módulo | pruebas pendientes |
| 4 | `DocumentUploadInput`, adaptador multipart y modelo organizacional interno | orden de validación o bytes | organization | restaurar adaptador | pruebas pendientes |
| 5 | work/notifications y política de reasignación exacta | pertenencia o 204 | identity + shared | restaurar controllers | pruebas pendientes |
| 6 | `DashboardSummaryService` | conteos/orden | work + identity | restaurar summary adapter | pruebas pendientes |
| 7 | servicios cohesionados de consulta, iniciativa y proyecto; `PortfolioDocumentService`/`PortfolioWorkService` integrados y controller delegado | atomicidad o DTO | incrementos 1-6 | revertir caso de uso individual | suite portfolio y backend completas |
| 8 | guardas arquitectónicas y documentación | regla demasiado amplia | todos | retirar regla/documentación nueva | revisión estática |

## Verificaciones fuera de alcance o no ejecutadas

- `gradlew.bat test` y `gradlew.bat check` backend sí fueron ejecutados con el wallet activo y pasaron; la suite completa registra 129 tests y 0 fallos.
- No se ejecutaron servidores ni generación OpenAPI. `npm test -- --watch=false` frontend fue ejecutado y conserva un fallo baseline de TypeScript fuera del alcance protegido.
- `LocalE2eUserProvisionerTest` permanece en `integration` porque requiere propiedades JVM explícitas y escritura intencional en Oracle; no se declara como integración Oracle ejecutada.
- Si aparece una diferencia no explicada, marcar `NEEDS CLARIFICATION` y detener el incremento afectado.

## Validación focalizada ejecutada

| Fecha | Comando | Resultado | Evidencia |
|---|---|---|---|
| 2026-08-22 | `apps/backend\\gradlew.bat test --tests "pe.gob.midagri.piip.shared.*" --tests "pe.gob.midagri.piip.organization.*" --tests "pe.gob.midagri.piip.audit.*" --tests "pe.gob.midagri.piip.identity.*"` | FALLÓ en `test`: `compileJava`, `compileTestJava` y `testClasses` pasaron; se ejecutaron 35 tests y fallaron 5. | Cuatro `UserAdministrationServiceTest` esperan excepciones deprecated de `shared.api`, pero el servicio ahora lanza los tipos canónicos de `shared.application.error`. `LocalE2eUserProvisionerTest` no carga el contexto por `ORA-12263` porque `ORACLE_DEV_TNS_ADMIN` permanece como placeholder inaccesible. No se marca T077 como completada. |
| 2026-08-22 | `apps/backend\\gradlew.bat test --tests "pe.gob.midagri.piip.shared.*" --tests "pe.gob.midagri.piip.organization.*" --tests "pe.gob.midagri.piip.audit.*" --tests "pe.gob.midagri.piip.identity.*"` con `ORACLE_DEV_TNS_ADMIN` y `TNS_ADMIN` apuntando al wallet activo | FALLÓ en `test`: 35 tests ejecutados, 1 falló. | El contexto Oracle ya cargó correctamente; el único fallo fue la guardia intencional de `LocalE2eUserProvisionerTest` por ausencia de `piip.provision.local-e2e=true` y propiedades de identidad. |
| 2026-08-22 | Mismo comando focal con wallet activo, después de clasificar `LocalE2eUserProvisionerTest` como `@Tag("integration")` | PASÓ: `BUILD SUCCESSFUL`; 34 tests no-`integration` ejecutados, 0 fallos. | La compatibilidad de excepciones quedó alineada a `shared.application.error`; la resolución del wallet activo eliminó `ORA-12263` del contexto de pruebas. La prueba E2E manual se conserva para ejecución explícita mediante `integrationTest`. |
| 2026-08-22 | `apps/backend\\gradlew.bat test --tests "pe.gob.midagri.piip.documents.*" --tests "pe.gob.midagri.piip.work.*" --tests "pe.gob.midagri.piip.dashboard.*" --tests "pe.gob.midagri.piip.portfolio.*"` con wallet activo | Primer intento: FALLÓ con 53 tests ejecutados y 6 fallos. Segundo intento: `BUILD SUCCESSFUL`, 0 fallos. | Los seis fallos iniciales fueron imports deprecated de excepciones en pruebas de portfolio/work. Se actualizaron a `shared.application.error` y la repetición pasó; T078 queda completada. |
| 2026-08-22 | `apps/backend\\gradlew.bat test --tests "pe.gob.midagri.piip.architecture.*"` | `BUILD SUCCESSFUL`; 10 tests, 0 fallos. | Pasaron las fronteras de controllers sin transacciones/repositorios/entidades, límites application/API, ownership de modelos compartidos, reglas negativas auto-probadas y las pruebas baseline de persistencia, binding, JSON y filtros por UE; T079 queda completada. |
| 2026-08-22 | `apps/backend\\gradlew.bat test` con wallet activo | Primer intento: 117 tests, 1 fallo en `TestResetSchemaFilterTest` por usar el constructor Hibernate 7 de `Table` con un solo argumento. Tras corregir el fixture a `(contributor, tableName)`: `BUILD SUCCESSFUL`. `gradlew.bat check`: `BUILD SUCCESSFUL`. | No hubo fallo productivo; el fixture baseline quedó actualizado para Hibernate 7. T080 queda completada. |
| 2026-08-22 | `apps/frontend\\npm test -- --watch=false` | FALLÓ durante compilación del bundle por errores TypeScript baseline en `src/app/pages/projects/projects.component.spec.ts:118-131` (`querySelector` genérico sobre `nativeElement` sin tipado y elementos inferidos como `{}`). | No se modificó frontend ni se regeneró OpenAPI/cliente, conforme al alcance protegido. Las advertencias NG8011 son no bloqueantes; T081 queda registrada con fallo baseline. |
| 2026-08-22 | `apps/backend\\gradlew.bat test` después de añadir las pruebas de caracterización y aplicación | `BUILD SUCCESSFUL`; 129 tests, 0 fallos. `gradlew.bat check`: `BUILD SUCCESSFUL`. | La corrección del fixture Hibernate y del test de stale completion quedó verificada junto con las nuevas pruebas. |

## Matriz de escenarios y restauración

| Incremento | Escenario caracterizado | Expectativa determinista | Rollback/restauración |
|---|---|---|---|
| 0 | Organization, audit, identity, documents, work, dashboard y portfolio | Mantener rutas, JSON, filtros, orden, estados HTTP y campos heredados; pruebas unitarias/contractuales asociadas en `apps/backend/src/test/java` | Restaurar únicamente el módulo afectado y retirar su read model/adaptador sin tocar frontend/OpenAPI |
| 1 | Errores funcionales y técnicos | Errores tipados traducen 403/404/409/422; `IllegalStateException` técnica no se convierte en regla | Restaurar excepciones/handler del incremento 1 |
| 2-3 | Consultas organization/audit/identity | Read models completos, actor nullable, scopes exactos y autorización por UE | Restaurar el controller y conservar servicios/repositorios previos |
| 4-6 | Upload, work, notifications y dashboard | Multipart, pertenencia, versión, alertas, notificaciones y conteos equivalentes | Restaurar adaptadores por módulo; no cambiar entidades ni contratos HTTP |
| 7 | Portfolio y efectos coordinados | Orden de versión → tareas/notificaciones/documentos/auditoría y rollback conjunto | Revertir por caso de uso, conservando servicios cohesionados y el contrato HTTP |
| 8 | Fronteras arquitectónicas | Controllers sin transacciones/repositorios/JPA; application sin tipos API/errores HTTP | Retirar únicamente la regla o wrapper que falle, conservando la matriz de evidencia |

## Matriz componente → responsabilidad → propietario → consumidor → prueba

| Componente | Responsabilidad | Propietario | Consumidor | Prueba |
|---|---|---|---|---|
| `OrganizationQueryService` | Consultas autorizadas y read models organizacionales | `organization.application` | `OrganizationController` | `OrganizationQueryServiceTest`, `OrganizationControllerTest` |
| `AuditQueryService` | Consultas y mapeo de auditoría | `audit.application` | `AuditController` | `AuditQueryServiceTest`, `AuditControllerTest` |
| `CurrentIdentityService` | Contexto, usuario y autenticación | `identity.application` | `IdentityController` | `CurrentIdentityServiceTest`, `IdentityControllerTest` |
| `DocumentUploadInput`/`PortfolioDocumentService` | Adaptación multipart y slots documentales | `documents.application` | `DocumentController`, portfolio | `DocumentUploadInputTest`, `DocumentControllerContractTest`, `PortfolioDocumentServiceTest` |
| `WorkTaskService`/`NotificationService` | Tareas, alertas, lectura y pertenencia | `work.application` | controllers work/notifications | `WorkTaskServiceTest`, `NotificationServiceTest`, controllers existentes |
| `DashboardSummaryService` | Conteos y resumen visible | `dashboard.application` | `DashboardController` | `DashboardSummaryServiceTest`, `DashboardControllerTest` |
| `PortfolioQueryService`/`InitiativeApplicationService`/`ProjectApplicationService` | Lista, detalle y comandos de portfolio | `portfolio.application` | `PortfolioController` | `PortfolioQueryServiceTest`, `PortfolioControllerContractTest`, suite portfolio existente |

## Equivalencia y límites

- Backend completo y `check` pasaron con el wallet activo; las pruebas arquitectónicas y focalizadas pasaron.
- Frontend conserva un fallo baseline de tipado en `projects.component.spec.ts`; no se modifica por el alcance protegido de la feature.
- `LocalE2eUserProvisionerTest` permanece como integración manual y no se declara equivalente ejecutada sin propiedades JVM explícitas.
- `PortfolioService.java` fue retirado después de migrar consumidores productivos y pruebas a `PortfolioQueryService`, `InitiativeApplicationService` y `ProjectApplicationService`. La extracción conserva locks, versión, autorización, auditoría, orden de efectos y rollback; la suite focal de portfolio (38 tests) y la suite backend completa (129 tests) pasaron.

## Cierre de T062/T066

- **T062 — completada:** los servicios de comandos invocan directamente `PortfolioDocumentService` y `PortfolioWorkService` dentro de sus límites `@Transactional`; la secuencia existente de persistencia, responsables, slots, tareas/notificaciones y auditoría se conserva.
- **T066 — completada:** `PortfolioService.java` ya no existe; no quedan consumidores en `apps/backend/src/main` ni `apps/backend/src/test`. Las pruebas de transición, auditoría, concurrencia, persistencia y contratos usan los servicios cohesionados.
