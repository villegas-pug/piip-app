# Comparación del DDL anterior y el modelo final

## Fuentes y criterio

La comparación se realizó contra las 13 tablas declaradas en `database/ddl/001_baseline_kallpa_piip.sql`, ubicado fuera del monorepo. El archivo original no se modifica. El Excel y su resumen funcional prevalecen para los 23 campos, seis catálogos y transiciones.

## Revisión tabla por tabla

| Tabla anterior | Decisión | Modelo final y motivo |
|---|---|---|
| `UNIDAD_EJECUTORA` | Reestructurar | Se separa en `INSTITUCION`, `UNIDAD_EJECUTORA` y `UNIDAD_ORGANICA`; evita mezclar entidad, ámbito operativo y jerarquía orgánica. |
| `USUARIO` | Conservar y ajustar | `USUARIO` conserva el `subject` de Keycloak, identidad visible, estado y última autenticación. No almacena contraseña. |
| `ROL` | Conservar y restringir | `ROL` queda limitado por enum a `ADMINISTRADOR_PIIP` y `CONSULTA_EXTERNA`. |
| `USUARIO_ROL_UNIDAD` | Reemplazar | `USUARIO_ROL_AMBITO` asigna rol por institución y, opcionalmente, Unidad Ejecutora, con vigencia y suspensión. |
| `PROYECTO` | Reemplazar | `REGISTRO_PORTAFOLIO` representa iniciativas y proyectos, incluyendo los 18 campos canónicos no documentales. |
| `PROYECTO_UNIDAD_ORGANICA` | Reemplazar | `REGISTRO_UNIDAD_RESPONSABLE` normaliza el campo multivaluado y conserva `denominacion_original`. |
| `TRANSICION_PERMITIDA` | Descartar | No se mantiene un catálogo de transiciones no confirmadas. El dominio expone únicamente `Presentado -> Iniciativa aprobada`. |
| `TIPO_DOCUMENTO` | Reemplazar | Los seis tipos documentales se definen como enum Java: cinco campos Excel más la Ficha de Iniciativa de la aplicación. |
| `DOCUMENTO` | Dividir | `DOCUMENTO` representa la posición; `DOCUMENTO_VERSION` los metadatos y publicación; `DOCUMENTO_CONTENIDO` el BLOB diferido. |
| `TRANSICION_ESTADO` | Reemplazar | `EVENTO_AUDITORIA` registra el hecho funcional append-only sin convertir estados no confirmados en reglas. |
| `SECUENCIA_CODIGO` | Reemplazar | `CONTADOR_CODIGO` genera correlativos por tipo y año mediante bloqueo pesimista JPA. |
| `AUDITORIA_ACCESO` | Conservar y minimizar | Registra ruta normalizada, respuesta, rol snapshot, correlación y duración; excluye cuerpos, tokens y binarios. |
| `AUDITORIA_EVENTO` | Renombrar | `EVENTO_AUDITORIA` conserva eventos funcionales append-only con actor y detalle acotado. |

## Nuevas tablas justificadas por PIIP Web 2

| Tabla final | Justificación |
|---|---|
| `INSTITUCION` | Permite reutilizar PIIP sin confundir institución con Unidad Ejecutora. |
| `UNIDAD_ORGANICA` | Representa direcciones, oficinas y jerarquía responsable. |
| `DOCUMENTO_VERSION` | Versionado y publicación externa explícita. |
| `DOCUMENTO_CONTENIDO` | Aísla el BLOB para carga diferida. |
| `TAREA_TRABAJO` | Persistencia de bandeja, vencimientos y alertas derivadas. |
| `NOTIFICACION` | Avisos persistentes y lectura por usuario. |

El SQL anterior se elimina únicamente dentro del monorepo para impedir su aplicación accidental. Las entidades Hibernate JPA son la fuente canónica y `target/piip-oracle.sql` es un artefacto regenerable para revisión DBA.
