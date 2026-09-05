-- Roles
MERGE INTO ROL r
USING (
    SELECT 'ADMINISTRADOR_PIIP' codigo, 'Administrador PIIP' nombre
    FROM dual
) s
ON (r.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET r.NOMBRE = s.nombre, r.ACTIVO = 1, r.SISTEMA = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ACTIVO, SISTEMA)
    VALUES (s.codigo, s.nombre, 1, 1);

MERGE INTO ROL r
USING (
    SELECT 'CONSULTA_EXTERNA' codigo, 'Consulta externa' nombre
    FROM dual
) s
ON (r.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET r.NOMBRE = s.nombre, r.ACTIVO = 1, r.SISTEMA = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ACTIVO, SISTEMA)
    VALUES (s.codigo, s.nombre, 1, 1);

-- Institucion y unidades ejecutoras
MERGE INTO INSTITUCION i
USING (
    SELECT 'MIDAGRI' codigo, 'MIDAGRI' nombre
    FROM dual
) s
ON (i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE = s.nombre, i.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ACTIVO, VERSION)
    VALUES (s.codigo, s.nombre, 1, 0);

MERGE INTO UNIDAD_EJECUTORA u
USING (
    SELECT i.ID_INSTITUCION institucion_id, v.codigo, v.nombre
    FROM INSTITUCION i
    CROSS JOIN (
        SELECT 'UE-001' codigo, 'UE-001' nombre
        FROM dual
        UNION ALL
        SELECT 'UE-002', 'UE-002'
        FROM dual
    ) v
    WHERE i.CODIGO = 'MIDAGRI'
) s
ON (u.ID_INSTITUCION = s.institucion_id AND u.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET u.NOMBRE = s.nombre, u.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (ID_INSTITUCION, CODIGO, NOMBRE, ACTIVO, VERSION)
    VALUES (s.institucion_id, s.codigo, s.nombre, 1, 0);

-- Unidades organicas
MERGE INTO UNIDAD_ORGANICA u
USING (
    SELECT ue.ID_UNIDAD_EJECUTORA ejecutora_id, v.codigo, v.nombre, v.sigla
    FROM UNIDAD_EJECUTORA ue
    CROSS JOIN (
        SELECT 'UE-001-UO-01' codigo, 'UE-001-UO-01' nombre, 'UO1' sigla
        FROM dual
        UNION ALL
        SELECT 'UE-001-UO-02', 'UE-001-UO-02', 'UO2'
        FROM dual
        UNION ALL
        SELECT 'UE-002-UO-01', 'UE-002-UO-01', 'UO1'
        FROM dual
        UNION ALL
        SELECT 'UE-002-UO-02', 'UE-002-UO-02', 'UO2'
        FROM dual
    ) v
    WHERE ue.CODIGO = SUBSTR(v.codigo, 1, 6)
) s
ON (u.ID_UNIDAD_EJECUTORA = s.ejecutora_id AND u.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET u.NOMBRE = s.nombre, u.SIGLA = s.sigla, u.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (ID_UNIDAD_EJECUTORA, CODIGO, NOMBRE, SIGLA, ACTIVO, VERSION)
    VALUES (s.ejecutora_id, s.codigo, s.nombre, s.sigla, 1, 0);

-- Usuario
MERGE INTO USUARIO u
USING (
    SELECT 'ed3742bc-f2c2-4884-ae09-07e3f9ab98fc' subject,
           'Cristopher Guevara Villegas' nombre,
           'rguevarav@midagri.gob.pe' correo
    FROM dual
) s
ON (u.KEYCLOAK_SUBJECT = s.subject)
WHEN MATCHED THEN
    UPDATE SET u.NOMBRE_COMPLETO = s.nombre, u.CORREO = s.correo, u.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (KEYCLOAK_SUBJECT, NOMBRE_COMPLETO, CORREO, ACTIVO, VERSION)
    VALUES (s.subject, s.nombre, s.correo, 1, 0);

-- Ambitos
MERGE INTO USUARIO_ROL_AMBITO a
USING (
    SELECT u.ID_USUARIO usuario_id,
           r.ID_ROL rol_id,
           i.ID_INSTITUCION institucion_id,
           e.ID_UNIDAD_EJECUTORA ejecutora_id
    FROM USUARIO u
    CROSS JOIN ROL r
    CROSS JOIN INSTITUCION i
    CROSS JOIN UNIDAD_EJECUTORA e
    WHERE u.KEYCLOAK_SUBJECT = 'ed3742bc-f2c2-4884-ae09-07e3f9ab98fc'
      AND r.CODIGO = 'ADMINISTRADOR_PIIP'
      AND i.CODIGO = 'MIDAGRI'
      AND e.ID_INSTITUCION = i.ID_INSTITUCION
      AND e.CODIGO IN ('UE-001', 'UE-002')
) s
ON (a.ID_USUARIO = s.usuario_id
    AND a.ID_ROL = s.rol_id
    AND a.ID_INSTITUCION = s.institucion_id
    AND a.ID_UNIDAD_EJECUTORA = s.ejecutora_id)
WHEN MATCHED THEN
    UPDATE SET a.ACTIVO = 1,
               a.VIGENTE_DESDE = COALESCE(a.VIGENTE_DESDE, CURRENT_TIMESTAMP),
               a.VIGENTE_HASTA = NULL,
               a.ASIGNADO_POR = 'BOOTSTRAP',
               a.FECHA_ASIGNACION = COALESCE(a.FECHA_ASIGNACION, CURRENT_TIMESTAMP)
WHEN NOT MATCHED THEN
    INSERT (ID_USUARIO, ID_ROL, ID_INSTITUCION, ID_UNIDAD_EJECUTORA,
            ACTIVO, VIGENTE_DESDE, ASIGNADO_POR, FECHA_ASIGNACION, VERSION)
    VALUES (s.usuario_id, s.rol_id, s.institucion_id, s.ejecutora_id,
            1, CURRENT_TIMESTAMP, 'BOOTSTRAP', CURRENT_TIMESTAMP, 0);

-- Catalogos
MERGE INTO CATALOGO c
USING (
    SELECT 'SOLUTION_TYPE' codigo, 'Tipo de solución' nombre, 10 orden
    FROM dual
) s
ON (c.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET c.NOMBRE = s.nombre, c.ORDEN_PRESENTACION = s.orden, c.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO c
USING (
    SELECT 'SOURCE_ORIGIN' codigo, 'Fuente u origen' nombre, 20 orden
    FROM dual
) s
ON (c.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET c.NOMBRE = s.nombre, c.ORDEN_PRESENTACION = s.orden, c.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO c
USING (
    SELECT 'PEI_OBJECTIVE' codigo, 'Objetivo PEI' nombre, 30 orden
    FROM dual
) s
ON (c.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET c.NOMBRE = s.nombre, c.ORDEN_PRESENTACION = s.orden, c.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO c
USING (
    SELECT 'POI_ACTIVITY' codigo, 'Actividad POI' nombre, 40 orden
    FROM dual
) s
ON (c.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET c.NOMBRE = s.nombre, c.ORDEN_PRESENTACION = s.orden, c.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);

-- Items de catalogo
MERGE INTO CATALOGO_ITEM i
USING (
    SELECT c.ID_CATALOGO catalogo_id, v.codigo, v.nombre, v.orden
    FROM CATALOGO c
    CROSS JOIN (
        SELECT 'POTENTIAL_OR_ADAPTABLE' codigo,
               'Solución potencial o adaptable' nombre, 10 orden
        FROM dual
        UNION ALL
        SELECT 'TO_BE_DEFINED', 'Solución por definir', 20
        FROM dual
        UNION ALL
        SELECT 'NOT_APPLICABLE', 'No aplica', 30
        FROM dual
    ) v
    WHERE c.CODIGO = 'SOLUTION_TYPE'
) s
ON (i.ID_CATALOGO = s.catalogo_id AND i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE = s.nombre, i.ORDEN_PRESENTACION = s.orden, i.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (ID_CATALOGO, CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.catalogo_id, s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO_ITEM i
USING (
    SELECT c.ID_CATALOGO catalogo_id, v.codigo, v.nombre, v.orden
    FROM CATALOGO c
    CROSS JOIN (
        SELECT 'INITIATIVE_SHEET' codigo,
               'Ficha de iniciativa de innovación pública' nombre, 10 orden
        FROM dual
        UNION ALL
        SELECT 'INTERNAL_CONTEST', 'Concurso interno', 20
        FROM dual
        UNION ALL
        SELECT 'OPEN_INNOVATION', 'Innovación abierta', 30
        FROM dual
        UNION ALL
        SELECT 'MANAGEMENT_PROPOSAL', 'Propuesta de jefatura o directivos', 40
        FROM dual
        UNION ALL
        SELECT 'OTHER', 'Otros', 50
        FROM dual
        UNION ALL
        SELECT 'CALL', 'Convocatoria', 60
        FROM dual
    ) v
    WHERE c.CODIGO = 'SOURCE_ORIGIN'
) s
ON (i.ID_CATALOGO = s.catalogo_id AND i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE = s.nombre, i.ORDEN_PRESENTACION = s.orden, i.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (ID_CATALOGO, CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.catalogo_id, s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO_ITEM i
USING (
    SELECT c.ID_CATALOGO catalogo_id, v.codigo, v.nombre, v.orden
    FROM CATALOGO c
    CROSS JOIN (
        SELECT 'PEI-001' codigo,
               'Fortalecer la gestión institucional orientada a resultados.' nombre, 10 orden
        FROM dual
        UNION ALL
        SELECT 'PEI-002', 'Mejorar la calidad de los servicios brindados a la ciudadanía.', 20
        FROM dual
        UNION ALL
        SELECT 'PEI-003', 'Impulsar la transformación digital institucional.', 30
        FROM dual
        UNION ALL
        SELECT 'PEI-004', 'Fortalecer las capacidades institucionales para la innovación.', 40
        FROM dual
    ) v
    WHERE c.CODIGO = 'PEI_OBJECTIVE'
) s
ON (i.ID_CATALOGO = s.catalogo_id AND i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE = s.nombre, i.ORDEN_PRESENTACION = s.orden, i.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (ID_CATALOGO, CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.catalogo_id, s.codigo, s.nombre, s.orden, 1);

MERGE INTO CATALOGO_ITEM i
USING (
    SELECT c.ID_CATALOGO catalogo_id, v.codigo, v.nombre, v.orden
    FROM CATALOGO c
    CROSS JOIN (
        SELECT 'POI-001' codigo,
               'Ejecutar acciones de mejora de procesos institucionales.' nombre, 10 orden
        FROM dual
        UNION ALL
        SELECT 'POI-002', 'Implementar servicios digitales para la atención de usuarios.', 20
        FROM dual
        UNION ALL
        SELECT 'POI-003', 'Realizar el seguimiento de indicadores de desempeño institucional.', 30
        FROM dual
        UNION ALL
        SELECT 'POI-004', 'Fortalecer las capacidades del personal en gestión e innovación.', 40
        FROM dual
    ) v
    WHERE c.CODIGO = 'POI_ACTIVITY'
) s
ON (i.ID_CATALOGO = s.catalogo_id AND i.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET i.NOMBRE = s.nombre, i.ORDEN_PRESENTACION = s.orden, i.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (ID_CATALOGO, CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.catalogo_id, s.codigo, s.nombre, s.orden, 1);

-- Tipos documentales
MERGE INTO TIPO_DOCUMENTO t
USING (
    SELECT 'PUBLIC_INNOVATION_INITIATIVE_SHEET' codigo,
           'Ficha de Iniciativa de Innovación Pública' nombre, 10 orden
    FROM dual
    UNION ALL
    SELECT 'INITIATIVE_TECHNICAL_OPINION',
           'Informe de opinión técnica de evaluación de iniciativa', 20
    FROM dual
    UNION ALL
    SELECT 'FORMAL_APPROVAL_DECISION',
           'Documento formal de decisión de aprobación', 30
    FROM dual
    UNION ALL
    SELECT 'FINAL_PRODUCT_APPROVAL',
           'Documento formal de aprobación de producto final', 40
    FROM dual
    UNION ALL
    SELECT 'PROJECT_MANAGEMENT_DOCUMENTATION',
           'Documentación de la gestión del proyecto', 50
    FROM dual
    UNION ALL
    SELECT 'FINAL_CLOSURE_REPORT', 'Informe final de cierre', 60
    FROM dual
) s
ON (t.CODIGO = s.codigo)
WHEN MATCHED THEN
    UPDATE SET t.NOMBRE = s.nombre, t.ORDEN_PRESENTACION = s.orden, t.ACTIVO = 1
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, ORDEN_PRESENTACION, ACTIVO)
    VALUES (s.codigo, s.nombre, s.orden, 1);
