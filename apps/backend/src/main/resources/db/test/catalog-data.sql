-- =============================================================================
-- SEED INICIAL PIIP - Datos sinteticos para test-reset
-- Este archivo se ejecuta exclusivamente bajo el perfil test,test-reset
-- despues de que Hibernate recrea las 21 tablas.
--
-- Contenido:
--   1. Roles del sistema (2)
--   2. Organizacion: institucion y unidades ejecutoras (1 + 2)
--   3. Organizacion: unidades organicas sinteticas (4)
--   4. Identidad: usuario administrador (1)
--   5. Identidad: ambitos administrativos (2)
--   6. Catalogos: cabeceras (4)
--   7. Catalogos: items (17)
--   8. Tipos documentales (6)
--   9. Estados del portafolio (11)
--
-- Total: 50 filas DML, cero DDL, cero IDs identity, cero secretos.
-- =============================================================================


-- =============================================================================
-- 1. ROLES DEL SISTEMA
-- =============================================================================
-- Roles fijos: ADMINISTRADOR_PIIP y CONSULTA_EXTERNA.
-- Resueltos por CODIGO (unique constraint UK_ROL_CODIGO).

MERGE INTO ROL r
USING (
    SELECT 'ADMINISTRADOR_PIIP' AS codigo,
           'Administrador PIIP' AS nombre
    FROM   dual
) s
ON (r.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET r.NOMBRE  = s.nombre,
               r.ACTIVO  = 1,
               r.SISTEMA = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ACTIVO, SISTEMA)
    VALUES (s.codigo, s.nombre, 1, 1);

MERGE INTO ROL r
USING (
    SELECT 'CONSULTA_EXTERNA' AS codigo,
           'Consulta externa' AS nombre
    FROM   dual
) s
ON (r.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET r.NOMBRE  = s.nombre,
               r.ACTIVO  = 1,
               r.SISTEMA = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ACTIVO, SISTEMA)
    VALUES (s.codigo, s.nombre, 1, 1);


-- =============================================================================
-- 2. ORGANIZACION: Institucion y unidades ejecutoras
-- =============================================================================
-- MIDAGRI como institucion unica.
-- UE-001 y UE-002 como unidades ejecutoras sinteticas.
-- Resueltos por CODIGO (unique constraints).

MERGE INTO INSTITUCION i
USING (
    SELECT 'MIDAGRI' AS codigo,
           'MIDAGRI' AS nombre
    FROM   dual
) s
ON (i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE = s.nombre,
               i.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ACTIVO, VERSION)
    VALUES (s.codigo, s.nombre, 1, 0);

MERGE INTO UNIDAD_EJECUTORA u
USING (
    SELECT i.ID_INSTITUCION AS institucion_id,
           v.codigo,
           v.nombre
    FROM   INSTITUCION i
    CROSS JOIN (
        SELECT 'UE-001' AS codigo,
               'UE-001' AS nombre
        FROM   dual
        UNION ALL
        SELECT 'UE-002', 'UE-002'
        FROM   dual
    ) v
    WHERE  i.CODIGO = 'MIDAGRI'
) s
ON (u.ID_INSTITUCION = s.institucion_id
    AND u.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET u.NOMBRE = s.nombre,
               u.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (ID_INSTITUCION, CODIGO, NOMBRE, ACTIVO, VERSION)
    VALUES (s.institucion_id, s.codigo, s.nombre, 1, 0);


-- =============================================================================
-- 3. ORGANIZACION: Unidades organicas sinteticas
-- =============================================================================
-- 4 UO: dos por cada UE.
-- Codigo sigue convencion UE-XXX-UO-NN.
-- Resueltos por ID_UNIDAD_EJECUTORA + CODIGO (unique constraint UK_UO_EJECUTORA_CODIGO).

MERGE INTO UNIDAD_ORGANICA u
USING (
    SELECT ue.ID_UNIDAD_EJECUTORA AS ejecutora_id,
           v.codigo,
           v.nombre,
           v.sigla
    FROM   UNIDAD_EJECUTORA ue
    CROSS JOIN (
        SELECT 'UE-001-UO-01' AS codigo,
               'UE-001-UO-01' AS nombre,
               'UO1'          AS sigla
        FROM   dual
        UNION ALL
        SELECT 'UE-001-UO-02', 'UE-001-UO-02', 'UO2'
        FROM   dual
        UNION ALL
        SELECT 'UE-002-UO-01', 'UE-002-UO-01', 'UO1'
        FROM   dual
        UNION ALL
        SELECT 'UE-002-UO-02', 'UE-002-UO-02', 'UO2'
        FROM   dual
    ) v
    WHERE  ue.CODIGO = SUBSTR(v.codigo, 1, 6)
) s
ON (u.ID_UNIDAD_EJECUTORA = s.ejecutora_id
    AND u.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET u.NOMBRE = s.nombre,
               u.SIGLA  = s.sigla,
               u.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (ID_UNIDAD_EJECUTORA, CODIGO, NOMBRE, SIGLA, ACTIVO, VERSION)
    VALUES (s.ejecutora_id, s.codigo, s.nombre, s.sigla, 1, 0);


-- =============================================================================
-- 4. IDENTIDAD: Usuario administrador
-- =============================================================================
-- Un usuario local activo.
-- El keycloak_subject debe existir en Keycloak para autenticacion real.
-- Resuelto por KEYCLOAK_SUBJECT (unique constraint UK_USUARIO_SUBJECT).

MERGE INTO USUARIO u
USING (
    SELECT 'ed3742bc-f2c2-4884-ae09-07e3f9ab98fc' AS subject,
           'Cristopher Guevara Villegas'            AS nombre,
           'rguevarav@midagri.gob.pe'              AS correo
    FROM   dual
) s
ON (u.KEYCLOAK_SUBJECT = s.subject)
WHEN MATCHED THEN
    UPDATE SET u.NOMBRE_COMPLETO = s.nombre,
               u.CORREO          = s.correo,
               u.ACTIVO          = 1
WHEN NOT MATCHED THEN
    INSERT (KEYCLOAK_SUBJECT, NOMBRE_COMPLETO, CORREO, ACTIVO, VERSION)
    VALUES (s.subject, s.nombre, s.correo, 1, 0);


-- =============================================================================
-- 5. IDENTIDAD: Ambitos administrativos
-- =============================================================================
-- Dos asignaciones ADMINISTRADOR_PIIP: una por UE.
-- El usuario debe existir previamente (seccion 4).
-- Los roles y la institucion deben existir previamente (secciones 1-2).
-- Resueltos por ID_USUARIO + ID_ROL + ID_INSTITUCION + ID_UNIDAD_EJECUTORA.

MERGE INTO USUARIO_ROL_AMBITO a
USING (
    SELECT u.ID_USUARIO            AS usuario_id,
           r.ID_ROL                AS rol_id,
           i.ID_INSTITUCION        AS institucion_id,
           e.ID_UNIDAD_EJECUTORA   AS ejecutora_id
    FROM   USUARIO u
    CROSS JOIN ROL r
    CROSS JOIN INSTITUCION i
    CROSS JOIN UNIDAD_EJECUTORA e
    WHERE  u.KEYCLOAK_SUBJECT = 'ed3742bc-f2c2-4884-ae09-07e3f9ab98fc'
      AND  r.CODIGO           = 'ADMINISTRADOR_PIIP'
      AND  i.CODIGO           = 'MIDAGRI'
      AND  e.ID_INSTITUCION   = i.ID_INSTITUCION
      AND  e.CODIGO           IN ('UE-001', 'UE-002')
) s
ON (a.ID_USUARIO            = s.usuario_id
    AND a.ID_ROL            = s.rol_id
    AND a.ID_INSTITUCION    = s.institucion_id
    AND a.ID_UNIDAD_EJECUTORA = s.ejecutora_id)
WHEN MATCHED THEN
    UPDATE SET a.ACTIVO           = 1,
               a.VIGENTE_DESDE    = COALESCE(a.VIGENTE_DESDE, CURRENT_TIMESTAMP),
               a.VIGENTE_HASTA    = NULL,
               a.ASIGNADO_POR     = 'BOOTSTRAP',
               a.FECHA_ASIGNACION = COALESCE(a.FECHA_ASIGNACION, CURRENT_TIMESTAMP)
WHEN NOT MATCHED THEN
    INSERT (ID_USUARIO, ID_ROL, ID_INSTITUCION, ID_UNIDAD_EJECUTORA,
            ACTIVO, VIGENTE_DESDE, ASIGNADO_POR, FECHA_ASIGNACION, VERSION)
    VALUES (s.usuario_id, s.rol_id, s.institucion_id, s.ejecutora_id,
            1, CURRENT_TIMESTAMP, 'BOOTSTRAP', CURRENT_TIMESTAMP, 0);


-- =============================================================================
-- Catalogos
-- =============================================================================
-- 4 catalogos fijos: SOLUTION_TYPE, SOURCE_ORIGIN, PEI_OBJECTIVE, POI_ACTIVITY.
-- Resueltos por CODIGO (unique constraint UK_CATALOGO_CODIGO).

MERGE INTO CATALOGO c
USING (
    SELECT 'SOLUTION_TYPE'  AS codigo,
           'Tipo de solucion' AS nombre,
           10                AS orden
    FROM   dual
) s
ON (c.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET c.NOMBRE             = s.nombre,
               c.ORDEN_PRESENTACION = s.orden,
               c.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO c
USING (
    SELECT 'SOURCE_ORIGIN' AS codigo,
           'Fuente u origen' AS nombre,
           20                AS orden
    FROM   dual
) s
ON (c.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET c.NOMBRE             = s.nombre,
               c.ORDEN_PRESENTACION = s.orden,
               c.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO c
USING (
    SELECT 'PEI_OBJECTIVE' AS codigo,
           'Objetivo PEI'  AS nombre,
           30              AS orden
    FROM   dual
) s
ON (c.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET c.NOMBRE             = s.nombre,
               c.ORDEN_PRESENTACION = s.orden,
               c.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO c
USING (
    SELECT 'POI_ACTIVITY'  AS codigo,
           'Actividad POI' AS nombre,
           40              AS orden
    FROM   dual
) s
ON (c.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET c.NOMBRE             = s.nombre,
               c.ORDEN_PRESENTACION = s.orden,
               c.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);


-- =============================================================================
-- Items de catalogo
-- =============================================================================
-- 3 tipos de solucion, 6 fuentes/origenes, 4 PEI, 4 POI.
-- Cada bloque MERGE resuelve por ID_CATALOGO + CODIGO
-- (unique constraint UK_CATALOGO_ITEM_CODIGO).

-- 7.1 Tipo de solucion (3 items)
MERGE INTO CATALOGO_ITEM i
USING (
    SELECT c.ID_CATALOGO AS catalogo_id,
           v.codigo,
           v.nombre,
           v.orden
    FROM   CATALOGO c
    CROSS JOIN (
        SELECT 'POTENTIAL_OR_ADAPTABLE' AS codigo,
               'Solucion potencial o adaptable' AS nombre,
               10 AS orden
        FROM   dual
        UNION ALL
        SELECT 'TO_BE_DEFINED',
               'Solucion por definir',
               20
        FROM   dual
        UNION ALL
        SELECT 'NOT_APPLICABLE',
               'No aplica',
               30
        FROM   dual
    ) v
    WHERE  c.CODIGO = 'SOLUTION_TYPE'
) s
ON (i.ID_CATALOGO = s.catalogo_id
    AND i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE             = s.nombre,
               i.ORDEN_PRESENTACION = s.orden,
               i.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (ID_CATALOGO, CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.catalogo_id, s.codigo, s.nombre, s.orden, 1);

-- 7.2 Fuente u origen (6 items)
MERGE INTO CATALOGO_ITEM i
USING (
    SELECT c.ID_CATALOGO AS catalogo_id,
           v.codigo,
           v.nombre,
           v.orden
    FROM   CATALOGO c
    CROSS JOIN (
        SELECT 'INITIATIVE_SHEET'  AS codigo,
               'Ficha de iniciativa de innovacion publica' AS nombre,
               10 AS orden
        FROM   dual
        UNION ALL
        SELECT 'INTERNAL_CONTEST',
               'Concurso interno',
               20
        FROM   dual
        UNION ALL
        SELECT 'OPEN_INNOVATION',
               'Innovacion abierta',
               30
        FROM   dual
        UNION ALL
        SELECT 'MANAGEMENT_PROPOSAL',
               'Propuesta de jefatura o directivos',
               40
        FROM   dual
        UNION ALL
        SELECT 'OTHER',
               'Otros',
               50
        FROM   dual
        UNION ALL
        SELECT 'CALL',
               'Convocatoria',
               60
        FROM   dual
    ) v
    WHERE  c.CODIGO = 'SOURCE_ORIGIN'
) s
ON (i.ID_CATALOGO = s.catalogo_id
    AND i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE             = s.nombre,
               i.ORDEN_PRESENTACION = s.orden,
               i.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (ID_CATALOGO, CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.catalogo_id, s.codigo, s.nombre, s.orden, 1);

-- 7.3 Objetivo PEI (4 items)
MERGE INTO CATALOGO_ITEM i
USING (
    SELECT c.ID_CATALOGO AS catalogo_id,
           v.codigo,
           v.nombre,
           v.orden
    FROM   CATALOGO c
    CROSS JOIN (
        SELECT 'PEI-001' AS codigo,
               'Fortalecer la gestion institucional orientada a resultados.' AS nombre,
               10 AS orden
        FROM   dual
        UNION ALL
        SELECT 'PEI-002',
               'Mejorar la calidad de los servicios brindados a la ciudadania.',
               20
        FROM   dual
        UNION ALL
        SELECT 'PEI-003',
               'Impulsar la transformacion digital institucional.',
               30
        FROM   dual
        UNION ALL
        SELECT 'PEI-004',
               'Fortalecer las capacidades institucionales para la innovacion.',
               40
        FROM   dual
    ) v
    WHERE  c.CODIGO = 'PEI_OBJECTIVE'
) s
ON (i.ID_CATALOGO = s.catalogo_id
    AND i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE             = s.nombre,
               i.ORDEN_PRESENTACION = s.orden,
               i.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (ID_CATALOGO, CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.catalogo_id, s.codigo, s.nombre, s.orden, 1);

-- 7.4 Actividad POI (4 items)
MERGE INTO CATALOGO_ITEM i
USING (
    SELECT c.ID_CATALOGO AS catalogo_id,
           v.codigo,
           v.nombre,
           v.orden
    FROM   CATALOGO c
    CROSS JOIN (
        SELECT 'POI-001' AS codigo,
               'Ejecutar acciones de mejora de procesos institucionales.' AS nombre,
               10 AS orden
        FROM   dual
        UNION ALL
        SELECT 'POI-002',
               'Implementar servicios digitales para la atencion de usuarios.',
               20
        FROM   dual
        UNION ALL
        SELECT 'POI-003',
               'Realizar el seguimiento de indicadores de desempeno institucional.',
               30
        FROM   dual
        UNION ALL
        SELECT 'POI-004',
               'Fortalecer las capacidades del personal en gestion e innovacion.',
               40
        FROM   dual
    ) v
    WHERE  c.CODIGO = 'POI_ACTIVITY'
) s
ON (i.ID_CATALOGO = s.catalogo_id
    AND i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE             = s.nombre,
               i.ORDEN_PRESENTACION = s.orden,
               i.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (ID_CATALOGO, CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.catalogo_id, s.codigo, s.nombre, s.orden, 1);


-- =============================================================================
-- Tipos documentales
-- =============================================================================
-- Resueltos por CODIGO (unique constraint UK_TIPO_DOCUMENTO_CODIGO).

MERGE INTO TIPO_DOCUMENTO t
USING (
    SELECT 'PUBLIC_INNOVATION_INITIATIVE_SHEET'  AS codigo,
           'Ficha de Iniciativa de Innovacion Publica' AS nombre,
           10 AS orden
    FROM   dual
    UNION ALL
    SELECT 'INITIATIVE_TECHNICAL_OPINION',
           'Informe de opinion tecnica de evaluacion de iniciativa',
           20
    FROM   dual
    UNION ALL
    SELECT 'FORMAL_APPROVAL_DECISION',
           'Documento formal de decision de aprobacion',
           30
    FROM   dual
    UNION ALL
    SELECT 'FINAL_PRODUCT_APPROVAL',
           'Documento formal de aprobacion de producto final',
           40
    FROM   dual
    UNION ALL
    SELECT 'PROJECT_MANAGEMENT_DOCUMENTATION',
           'Documentacion de la gestion del proyecto',
           50
    FROM   dual
    UNION ALL
    SELECT 'FINAL_CLOSURE_REPORT',
           'Informe final de cierre',
           60
    FROM   dual
) s
ON (t.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET t.NOMBRE             = s.nombre,
               t.ORDEN_PRESENTACION = s.orden,
               t.ACTIVO             = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);


-- =============================================================================
-- Estados del portafolio
-- =============================================================================
-- Once estados fijos con identidad por CODIGO (PK natural). Insert-only:
-- una divergencia de datos se reporta como fallo de la postvalidacion en lugar
-- de corregirse silenciosamente (FR-022/FR-025/FR-026). Sin rama UPDATE.

MERGE INTO ESTADO_PORTAFOLIO e
USING (
    SELECT 'PRESENTED' AS codigo,
           'Presentado' AS nombre,
           1 AS orden,
           'INITIATIVE' AS aplicabilidad
    FROM   dual
    UNION ALL
    SELECT 'INITIATIVE_APPROVED', 'Iniciativa aprobada', 2, 'INITIATIVE' FROM dual
    UNION ALL
    SELECT 'INITIATIVE_ARCHIVED', 'Iniciativa archivada', 3, 'INITIATIVE' FROM dual
    UNION ALL
    SELECT 'PROJECT_IN_PROGRESS', 'Proyecto en ejecución', 4, 'PROJECT' FROM dual
    UNION ALL
    SELECT 'PRODUCT_APPROVED', 'Producto aprobado', 5, 'PROJECT' FROM dual
    UNION ALL
    SELECT 'PRODUCT_NOT_APPROVED', 'Producto no aprobado', 6, 'PROJECT' FROM dual
    UNION ALL
    SELECT 'SUSPENDED', 'Suspendido', 7, 'PROJECT' FROM dual
    UNION ALL
    SELECT 'CANCELLED', 'Cancelado', 8, 'PROJECT' FROM dual
    UNION ALL
    SELECT 'FINISHED', 'Finalizado', 9, 'PROJECT' FROM dual
    UNION ALL
    SELECT 'NOT_APPLICABLE', 'No Aplicable', 10, 'NONE' FROM dual
    UNION ALL
    SELECT 'NOT_ADMISSIBLE', 'No Admisible', 11, 'INITIATIVE' FROM dual
) s
ON (e.CODIGO = s.codigo)
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO, APLICABILIDAD)
    VALUES (s.codigo, s.nombre, s.orden, 1, s.aplicabilidad);
