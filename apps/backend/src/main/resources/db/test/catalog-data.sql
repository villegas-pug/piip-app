-- DML idempotente exclusivo de test-reset. PEI, POI y UO son datos sintéticos de prueba.
MERGE INTO CATALOGO c USING (SELECT 'SOLUTION_TYPE' codigo, 'Tipo de solución' nombre, 10 orden FROM dual) s ON (c.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET c.NOMBRE=s.nombre,c.ORDEN_PRESENTACION=s.orden,c.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.codigo,s.nombre,s.orden,1);
MERGE INTO CATALOGO c USING (SELECT 'SOURCE_ORIGIN' codigo, 'Fuente u origen' nombre, 20 orden FROM dual) s ON (c.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET c.NOMBRE=s.nombre,c.ORDEN_PRESENTACION=s.orden,c.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.codigo,s.nombre,s.orden,1);
MERGE INTO CATALOGO c USING (SELECT 'PEI_OBJECTIVE' codigo, 'Objetivo PEI' nombre, 30 orden FROM dual) s ON (c.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET c.NOMBRE=s.nombre,c.ORDEN_PRESENTACION=s.orden,c.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.codigo,s.nombre,s.orden,1);
MERGE INTO CATALOGO c USING (SELECT 'POI_ACTIVITY' codigo, 'Actividad POI' nombre, 40 orden FROM dual) s ON (c.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET c.NOMBRE=s.nombre,c.ORDEN_PRESENTACION=s.orden,c.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.codigo,s.nombre,s.orden,1);

MERGE INTO CATALOGO_ITEM i USING (
 SELECT c.ID_CATALOGO catalogo_id, v.codigo, v.nombre, v.orden FROM CATALOGO c CROSS JOIN (
  SELECT 'POTENTIAL_OR_ADAPTABLE' codigo,'Solución potencial o adaptable' nombre,10 orden FROM dual UNION ALL
  SELECT 'TO_BE_DEFINED','Solución por definir',20 FROM dual UNION ALL
  SELECT 'NOT_APPLICABLE','No aplica',30 FROM dual) v WHERE c.CODIGO='SOLUTION_TYPE') s
ON (i.ID_CATALOGO=s.catalogo_id AND i.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET i.NOMBRE=s.nombre,i.ORDEN_PRESENTACION=s.orden,i.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (ID_CATALOGO,CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.catalogo_id,s.codigo,s.nombre,s.orden,1);

MERGE INTO CATALOGO_ITEM i USING (
 SELECT c.ID_CATALOGO catalogo_id, v.codigo, v.nombre, v.orden FROM CATALOGO c CROSS JOIN (
  SELECT 'INITIATIVE_SHEET' codigo,'Ficha de iniciativa de innovación pública' nombre,10 orden FROM dual UNION ALL
  SELECT 'INTERNAL_CONTEST','Concurso interno',20 FROM dual UNION ALL
  SELECT 'OPEN_INNOVATION','Innovación abierta',30 FROM dual UNION ALL
  SELECT 'MANAGEMENT_PROPOSAL','Propuesta de jefatura o directivos',40 FROM dual UNION ALL
  SELECT 'OTHER','Otros',50 FROM dual UNION ALL
  SELECT 'CALL','Convocatoria',60 FROM dual) v WHERE c.CODIGO='SOURCE_ORIGIN') s
ON (i.ID_CATALOGO=s.catalogo_id AND i.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET i.NOMBRE=s.nombre,i.ORDEN_PRESENTACION=s.orden,i.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (ID_CATALOGO,CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.catalogo_id,s.codigo,s.nombre,s.orden,1);

MERGE INTO CATALOGO_ITEM i USING (
 SELECT c.ID_CATALOGO catalogo_id, v.codigo, v.nombre, v.orden FROM CATALOGO c CROSS JOIN (
  SELECT 'PEI-001' codigo,'Fortalecer la gestión institucional orientada a resultados.' nombre,10 orden FROM dual UNION ALL
  SELECT 'PEI-002','Mejorar la calidad de los servicios brindados a la ciudadanía.',20 FROM dual UNION ALL
  SELECT 'PEI-003','Impulsar la transformación digital institucional.',30 FROM dual UNION ALL
  SELECT 'PEI-004','Fortalecer las capacidades institucionales para la innovación.',40 FROM dual) v WHERE c.CODIGO='PEI_OBJECTIVE') s
ON (i.ID_CATALOGO=s.catalogo_id AND i.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET i.NOMBRE=s.nombre,i.ORDEN_PRESENTACION=s.orden,i.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (ID_CATALOGO,CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.catalogo_id,s.codigo,s.nombre,s.orden,1);

MERGE INTO CATALOGO_ITEM i USING (
 SELECT c.ID_CATALOGO catalogo_id, v.codigo, v.nombre, v.orden FROM CATALOGO c CROSS JOIN (
  SELECT 'POI-001' codigo,'Ejecutar acciones de mejora de procesos institucionales.' nombre,10 orden FROM dual UNION ALL
  SELECT 'POI-002','Implementar servicios digitales para la atención de usuarios.',20 FROM dual UNION ALL
  SELECT 'POI-003','Realizar el seguimiento de indicadores de desempeño institucional.',30 FROM dual UNION ALL
  SELECT 'POI-004','Fortalecer las capacidades del personal en gestión e innovación.',40 FROM dual) v WHERE c.CODIGO='POI_ACTIVITY') s
ON (i.ID_CATALOGO=s.catalogo_id AND i.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET i.NOMBRE=s.nombre,i.ORDEN_PRESENTACION=s.orden,i.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (ID_CATALOGO,CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.catalogo_id,s.codigo,s.nombre,s.orden,1);

MERGE INTO TIPO_DOCUMENTO t USING (
 SELECT 'PUBLIC_INNOVATION_INITIATIVE_SHEET' codigo,'Ficha de Iniciativa de Innovación Pública' nombre,10 orden FROM dual UNION ALL
 SELECT 'INITIATIVE_TECHNICAL_OPINION','Informe de opinión técnica de evaluación de iniciativa',20 FROM dual UNION ALL
 SELECT 'FORMAL_APPROVAL_DECISION','Documento formal de decisión de aprobación',30 FROM dual UNION ALL
 SELECT 'FINAL_PRODUCT_APPROVAL','Documento formal de aprobación de producto final',40 FROM dual UNION ALL
 SELECT 'PROJECT_MANAGEMENT_DOCUMENTATION','Documentación de la gestión del proyecto',50 FROM dual UNION ALL
 SELECT 'FINAL_CLOSURE_REPORT','Informe final de cierre',60 FROM dual) s ON (t.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET t.NOMBRE=s.nombre,t.ORDEN_PRESENTACION=s.orden,t.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (CODIGO,NOMBRE,ORDEN_PRESENTACION,ACTIVO) VALUES (s.codigo,s.nombre,s.orden,1);

-- Solo crea UO sintéticas cuando una UE de prueba identificada por código aún no tiene UO activa.
MERGE INTO UNIDAD_ORGANICA u USING (
 SELECT ue.ID_UNIDAD_EJECUTORA ejecutora_id, ue.CODIGO||'-UO-01' codigo, 'Oficina de Planeamiento y Modernización' nombre, 'OPM' sigla
 FROM UNIDAD_EJECUTORA ue WHERE ue.CODIGO IN ('UE-001','UE-002')
 AND NOT EXISTS (SELECT 1 FROM UNIDAD_ORGANICA x WHERE x.ID_UNIDAD_EJECUTORA=ue.ID_UNIDAD_EJECUTORA AND x.ACTIVO=1)
 UNION ALL
 SELECT ue.ID_UNIDAD_EJECUTORA, ue.CODIGO||'-UO-02', 'Oficina de Tecnologías de la Información', 'OTI'
 FROM UNIDAD_EJECUTORA ue WHERE ue.CODIGO IN ('UE-001','UE-002')
 AND NOT EXISTS (SELECT 1 FROM UNIDAD_ORGANICA x WHERE x.ID_UNIDAD_EJECUTORA=ue.ID_UNIDAD_EJECUTORA AND x.ACTIVO=1)) s
ON (u.ID_UNIDAD_EJECUTORA=s.ejecutora_id AND u.CODIGO=s.codigo)
WHEN MATCHED THEN UPDATE SET u.NOMBRE=s.nombre,u.SIGLA=s.sigla,u.ACTIVO=1
WHEN NOT MATCHED THEN INSERT (ID_UNIDAD_EJECUTORA,CODIGO,NOMBRE,SIGLA,ACTIVO,VERSION) VALUES (s.ejecutora_id,s.codigo,s.nombre,s.sigla,1,0);
