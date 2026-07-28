# Modelo de datos final

```mermaid
erDiagram
  INSTITUCION ||--o{ UNIDAD_EJECUTORA : contiene
  UNIDAD_EJECUTORA ||--o{ UNIDAD_ORGANICA : contiene
  UNIDAD_EJECUTORA ||--o{ REGISTRO_PORTAFOLIO : administra
  REGISTRO_PORTAFOLIO ||--o{ REGISTRO_UNIDAD_RESPONSABLE : asigna
  UNIDAD_ORGANICA ||--o{ REGISTRO_UNIDAD_RESPONSABLE : participa
  USUARIO ||--o{ USUARIO_ROL_AMBITO : posee
  ROL ||--o{ USUARIO_ROL_AMBITO : concede
  REGISTRO_PORTAFOLIO ||--o{ DOCUMENTO : contiene
  DOCUMENTO ||--o{ DOCUMENTO_VERSION : versiona
  DOCUMENTO_VERSION ||--|| DOCUMENTO_CONTENIDO : almacena
  REGISTRO_PORTAFOLIO ||--o{ TAREA_TRABAJO : genera
  USUARIO ||--o{ NOTIFICACION : recibe
  USUARIO ||--o{ AUDITORIA_ACCESO : accede
  USUARIO ||--o{ EVENTO_AUDITORIA : ejecuta
```

Las restricciones entre ámbitos, la unicidad del proyecto derivado y las transiciones confirmadas se validan en servicios transaccionales y constraints generados por JPA.
