# Adopción de Graphify en PIIP

## Propósito y límites

Graphify produce un índice estructural local para orientar preguntas de arquitectura, dependencias, impacto y recorridos entre capas. No sustituye al código, las especificaciones, las pruebas ni la configuración del repositorio como fuente de verdad.

El índice se guarda en `graphify-out/`, está ignorado por Git y no se comparte como artefacto versionado. Las skills globales de Codex y OpenCode son la integración del agente; este repositorio no duplica skills ni instala recordatorios específicos de una plataforma.

## Preparación local

La versión validada es `graphify 0.9.37`. Cada checkout principal necesita sus propios hooks:

```powershell
graphify hook install
git config --local --unset-all merge.graphify.name
git config --local --unset-all merge.graphify.driver
```

El instalador también agrega `graphify-out/graph.json merge=graphify` a `.gitattributes`. Como el índice es local, eliminar esa línea; si queda vacío, eliminar también el archivo. El resultado esperado de `graphify hook status` es `post-commit: installed`, `post-checkout: installed` y `merge driver: not registered`.

Los hooks se ejecutan en segundo plano después de commits y cambios de rama del checkout principal. Actualizan el AST de código sin usar proveedores externos. Los worktrees enlazados se omiten deliberadamente: si no tienen `graphify-out/graph.json`, se consulta directamente el repositorio.

## Uso diario

1. Al iniciar trabajo con el índice, ejecutar `graphify reflect --if-stale`.
2. Para exploraciones amplias, usar primero `graphify query "<pregunta>"`, `graphify path "A" "B"`, `graphify explain "concepto"` o `graphify affected "símbolo"`.
3. Confirmar cada conclusión en las fuentes canónicas. Si ya se conoce el archivo o símbolo, abrirlo directamente.
4. Tras cambios materiales de código no confirmados, ejecutar `graphify update .`.

Las consultas útiles, corregidas o sin resultado pueden guardarse con `graphify save-result` y su resultado correspondiente; `reflect` consolida esa memoria local para consultas futuras.

## Documentación y recuperación

`graphify update .` refresca el AST. Si cambian `specs/`, `docs/` o material no código, solicitar explícitamente una actualización semántica mediante la skill Graphify del agente activo. No se configuran claves ni proveedores externos para este repositorio.

Para diagnosticar el índice, ejecutar:

```powershell
graphify check-update .
graphify diagnose multigraph --graph graphify-out/graph.json --directed --json
```

Si el grafo está ausente, obsoleto o cambió el corpus excluido por `.graphifyignore`, regenerar una línea base dirigida mediante la skill Graphify. Una reducción intencional del grafo por exclusiones requiere permitir `force` y volver a ejecutar el diagnóstico.

Las excepciones puente de `shared/api` pueden aparecer como self-loops porque comparten nombre simple con su tipo de aplicación y extienden una clase con FQCN. No representan ciclos reales: confirmar la herencia en el código Java antes de interpretar ese resultado.
