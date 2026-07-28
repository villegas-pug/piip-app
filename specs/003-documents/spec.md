# Documentos

## Alcance

Normalizar las posiciones documentales, versionar metadatos y almacenar el contenido en un BLOB separado con publicación explícita.

## Aceptación

- Se validan extensión, MIME, tamaño y checksum.
- Consulta externa descarga únicamente versiones publicadas dentro de su ámbito.
- Los documentos no se copian al proyecto derivado.
- Antimalware queda registrado como requisito previo a producción.
