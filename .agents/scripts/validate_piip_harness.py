#!/usr/bin/env python3
"""Valida estructura, routing y aislamiento del harness PIIP."""

from __future__ import annotations

import json
import re
import sys
import tomllib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FRONTEND_SKILLS = (
    "fe-implement-standalone-ui",
    "fe-sync-openapi-client",
    "fe-maintain-auth-session",
    "fe-fix-reproduced-ui-bug",
)
BACKEND_SKILLS = (
    "be-implement-transactional-use-case",
    "be-evolve-jpa-oracle-model",
    "be-enforce-authorization-audit",
    "be-publish-openapi-contract",
    "be-diagnose-oracle-runtime",
    "be-fix-reproduced-backend-bug",
)
LEGACY_AGENTS = (
    "backend-architect.toml",
    "frontend-integrator.toml",
    "data-model-reviewer.toml",
    "security-reviewer.toml",
    "test-reviewer.toml",
)
LEGACY_SKILLS = ("piip-frontend", "piip-backend", "piip-data-model")
GRADLE_SKILLS = (
    "be-diagnose-oracle-runtime",
    "be-fix-reproduced-backend-bug",
    "be-implement-transactional-use-case",
    "be-publish-openapi-contract",
)
FORBIDDEN_BACKEND_BUILD_PATTERNS = (
    r"\bmaven\b",
    r"\bmvn\b",
    r"\bpom\.xml\b",
)
BACKEND_SEMANTIC_RULES = {
    "be-implement-transactional-use-case": {
        "adapters, commands y read models": (r"\badapter", r"\bcommands?\b", r"read models?"),
        "ownership de application": (r"application.{0,120}(casos de uso|commands)", r"límites? `?@transactional"),
        "dependencia application hacia api prohibida": (r"(?:no|ni) (?:introducir|crear).{0,80}application\s*[-=]>\s*api",),
        "controllers sin persistencia": (r"no (inyectar|exponer).{0,80}(repositorios|entidades jpa).{0,80}control",),
        "autorización y auditoría transaccionales": (r"autorización.{0,100}auditoría", r"misma orquestación transaccional"),
    },
    "be-enforce-authorization-audit": {
        "Keycloak autentica y Oracle autoriza": (r"keycloak.{0,80}autentic", r"oracle.{0,80}autoriza"),
        "ámbito Oracle completo": (r"asignación oracle activa y vigente", r"institución.{0,80}unidad ejecutora"),
        "auditoría de acceso independiente": (r"auditoría de acceso.{0,160}requires_new",),
        "evento funcional atómico": (r"evento funcional.{0,120}(misma|dentro de la) transacción", r"confirmen o reviertan juntos"),
    },
    "be-publish-openapi-contract": {
        "ProblemDetail correcto": (r"application/problem\+json", r"schema `?problemdetail"),
        "assertions estructurales": (r"assertions estructurales", r"paths?.{0,160}schemas?.{0,160}media types?"),
        "freshness verificable": (r"freshness", r"checkout y revisión actuales"),
        "fuente, artefacto y handoff separados": (r"contrato fuente", r"artefacto generado", r"handoff al agente principal"),
    },
    "be-evolve-jpa-oracle-model": {
        "JPA y JPQL canónicos": (r"jpa.{0,120}fuente canónica", r"\bjpql\b"),
        "matriz ddl-auto": (r"ddl-auto=validate", r"create-drop", r"ddl-auto=none"),
        "test-reset fail-closed": (r"test-reset.{0,160}fail-closed", r"allowlisted"),
        "DDL derivado con ownership y freshness": (r"ddl.{0,100}derivad", r"freshness", r"agente principal o dba"),
    },
    "be-diagnose-oracle-runtime": {
        "matriz vigente de perfiles": (r"ddl-auto=validate", r"create-drop", r"ddl-auto=none", r"test,test-reset"),
        "test-reset fail-closed": (r"test-reset.{0,240}fail-closed", r"fingerprint jdbc", r"schema allowlisted"),
        "build no prueba conectividad": (r"no declarar resuelta la conectividad.{0,120}compilación",),
    },
    "be-fix-reproduced-backend-bug": {
        "guardas modulares portables": (r"api.{0,100}application.{0,100}domain.{0,100}persistence",),
        "dependencia application hacia api prohibida": (r"no introducir dependencias `?application\s*[-=]>\s*api",),
        "baseline no autoriza patrón": (r"baseline.{0,80}no como (permiso|patrón)",),
        "ProblemDetail y OpenAPI estructural": (r"application/problem\+json", r"problemdetail", r"assertions estructurales openapi"),
    },
}

BACKEND_PROFILE_RULES = {
    "ownership de capas": (r"api.{0,120}application.{0,120}domain.{0,120}persistence",),
    "dependencia application hacia api prohibida": (r"no (introduzcas|crear).{0,40}application\s*[-=]>\s*api",),
    "persistencia fuera de controllers": (r"no expongas.{0,80}(repositorios|entidades jpa)",),
    "baseline no es precedente": (r"baseline.{0,80}no (como|precedentes?)",),
    "handoff cross-domain": (r"(?:handoff.{0,160}agente principal|agente principal.{0,40}handoff)", r"orden de integración"),
}


def split_frontmatter(path: Path) -> tuple[dict[str, str], str, str]:
    text = path.read_text(encoding="utf-8")
    match = re.match(r"^---\s*\n(.*?)\n---\s*\n(.*)$", text, flags=re.DOTALL)
    if not match:
        raise ValueError(f"frontmatter inválido: {path.relative_to(ROOT)}")
    raw_frontmatter, body = match.groups()
    values: dict[str, str] = {}
    for line in raw_frontmatter.splitlines():
        if not line.strip() or line.startswith(" "):
            continue
        key, separator, value = line.partition(":")
        if separator:
            values[key.strip()] = value.strip()
    return values, raw_frontmatter, body.strip()


def validate_skills(errors: list[str]) -> None:
    for skill_name in FRONTEND_SKILLS + BACKEND_SKILLS:
        path = ROOT / ".agents" / "skills" / skill_name / "SKILL.md"
        if not path.is_file():
            errors.append(f"Falta {path.relative_to(ROOT)}")
            continue
        try:
            values, raw_frontmatter, body = split_frontmatter(path)
        except ValueError as error:
            errors.append(str(error))
            continue
        top_level_keys = {
            line.partition(":")[0].strip()
            for line in raw_frontmatter.splitlines()
            if line and not line.startswith(" ") and ":" in line
        }
        if top_level_keys != {"name", "description"}:
            errors.append(f"{skill_name}: frontmatter debe contener solo name y description")
        if values.get("name") != skill_name:
            errors.append(f"{skill_name}: name no coincide con el directorio")
        if not values.get("description"):
            errors.append(f"{skill_name}: description vacía")
        if "fix-reproduced" in skill_name:
            reproduce_at = body.lower().find("reproduc")
            edit_at = body.lower().find("editar")
            if reproduce_at < 0 or edit_at < 0 or reproduce_at > edit_at:
                errors.append(f"{skill_name}: debe exigir reproducción antes de editar")


def validate_skill_semantics(errors: list[str]) -> None:
    for skill_name in GRADLE_SKILLS:
        path = ROOT / ".agents" / "skills" / skill_name / "SKILL.md"
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        for pattern in FORBIDDEN_BACKEND_BUILD_PATTERNS:
            if re.search(pattern, text, flags=re.IGNORECASE):
                errors.append(f"{skill_name}: referencia obsoleta al build anterior")

    for skill_name, rules in BACKEND_SEMANTIC_RULES.items():
        path = ROOT / ".agents" / "skills" / skill_name / "SKILL.md"
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8").casefold()
        for concept, patterns in rules.items():
            if any(re.search(pattern, text, flags=re.DOTALL) is None for pattern in patterns):
                errors.append(f"{skill_name}: semántica incompleta para {concept}")

    auth_path = ROOT / ".agents" / "skills" / "fe-maintain-auth-session" / "SKILL.md"
    if auth_path.is_file() and "apps/frontend/core" in auth_path.read_text(encoding="utf-8"):
        errors.append("fe-maintain-auth-session: conserva la ruta frontend anterior")


def validate_codex(errors: list[str]) -> None:
    config = tomllib.loads((ROOT / ".codex" / "config.toml").read_text(encoding="utf-8"))
    if config.get("features", {}).get("multi_agent") is not True:
        errors.append("Codex: features.multi_agent debe ser true")
    agents_config = config.get("agents", {})
    if agents_config.get("max_threads") != 2:
        errors.append("Codex: concurrencia debe ser 2")
    if agents_config.get("max_depth") != 1:
        errors.append("Codex: profundidad debe ser 1")

    for agent_name, forbidden_skills, expected_scope in (
        ("frontend-specialist", BACKEND_SKILLS, "frontend"),
        ("backend-specialist", FRONTEND_SKILLS, "backend"),
    ):
        path = ROOT / ".codex" / "agents" / f"{agent_name}.toml"
        try:
            agent = tomllib.loads(path.read_text(encoding="utf-8"))
        except (FileNotFoundError, tomllib.TOMLDecodeError) as error:
            errors.append(f"Codex {agent_name}: {error}")
            continue
        if "sandbox_mode" in agent or "approval_policy" in agent:
            errors.append(f"Codex {agent_name}: debe heredar sandbox y approval")
        disabled_paths = {
            item.get("path")
            for item in agent.get("skills", {}).get("config", [])
            if item.get("enabled") is False
        }
        expected_paths = {f".agents/skills/{name}/SKILL.md" for name in forbidden_skills}
        if disabled_paths != expected_paths:
            errors.append(f"Codex {agent_name}: catálogo contrario incompleto")
        serialized = path.read_text(encoding="utf-8")
        if f"--scope {expected_scope}" not in serialized:
            errors.append(f"Codex {agent_name}: hook de scope ausente")

    backend_text = (ROOT / ".codex" / "agents" / "backend-specialist.toml").read_text(encoding="utf-8").casefold()
    for concept, patterns in BACKEND_PROFILE_RULES.items():
        if any(re.search(pattern, backend_text, flags=re.DOTALL) is None for pattern in patterns):
            errors.append(f"Codex backend-specialist: semántica incompleta para {concept}")

    frontend_config = tomllib.loads((ROOT / ".codex" / "agents" / "frontend-specialist.toml").read_text(encoding="utf-8"))
    frontend_disabled = {
        item.get("path")
        for item in frontend_config.get("skills", {}).get("config", [])
        if item.get("enabled") is False
    }
    missing_disabled = {f".agents/skills/{name}/SKILL.md" for name in BACKEND_SKILLS} - frontend_disabled
    if missing_disabled:
        errors.append("Codex frontend-specialist: solo backend-specialist puede habilitar skills be-*")


def validate_opencode(errors: list[str]) -> None:
    try:
        config = json.loads((ROOT / "opencode.json").read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError) as error:
        errors.append(f"OpenCode config: {error}")
        return

    build_tasks = config.get("agent", {}).get("build", {}).get("permission", {}).get("task", {})
    plan_tasks = config.get("agent", {}).get("plan", {}).get("permission", {}).get("task", {})
    for name in ("frontend-specialist", "backend-specialist"):
        if build_tasks.get(name) != "allow" or plan_tasks.get(name) != "deny":
            errors.append(f"OpenCode routing incorrecto para {name}")
    for name in ("frontend-specialist-plan", "backend-specialist-plan"):
        if plan_tasks.get(name) != "allow" or build_tasks.get(name) != "deny":
            errors.append(f"OpenCode routing incorrecto para {name}")

    for domain, prefix, scope in (
        ("frontend", "fe-", "apps/frontend/**"),
        ("backend", "be-", "apps/backend/**"),
    ):
        visible = ROOT / ".opencode" / "agents" / f"{domain}-specialist.md"
        hidden = ROOT / ".opencode" / "agents" / f"{domain}-specialist-plan.md"
        try:
            _, visible_frontmatter, visible_body = split_frontmatter(visible)
            _, hidden_frontmatter, hidden_body = split_frontmatter(hidden)
        except (FileNotFoundError, ValueError) as error:
            errors.append(f"OpenCode {domain}: {error}")
            continue
        if visible_body != hidden_body:
            errors.append(f"OpenCode {domain}: prompts visible/plan divergentes")
        for label, frontmatter in (("visible", visible_frontmatter), ("plan", hidden_frontmatter)):
            if "task: deny" not in frontmatter or f'"{prefix}*": allow' not in frontmatter:
                errors.append(f"OpenCode {domain} {label}: aislamiento de task/skill incompleto")
        if scope not in visible_frontmatter:
            errors.append(f"OpenCode {domain}: scope de escritura ausente")
        if "hidden: true" not in hidden_frontmatter or re.search(r"^\s*edit: deny$", hidden_frontmatter, re.MULTILINE) is None:
            errors.append(f"OpenCode {domain}-plan: debe ser oculto y read-only")

    for path in (ROOT / ".opencode" / "agents").glob("*.md"):
        text = path.read_text(encoding="utf-8")
        if '"be-*": allow' in text and path.name not in {"backend-specialist.md", "backend-specialist-plan.md"}:
            errors.append(f"OpenCode {path.name}: solo backend-specialist puede habilitar skills be-*")

    backend_visible = ROOT / ".opencode" / "agents" / "backend-specialist.md"
    backend_command = ROOT / ".opencode" / "commands" / "backend-specialist.md"
    combined = "\n".join(path.read_text(encoding="utf-8").casefold() for path in (backend_visible, backend_command))
    for concept, patterns in BACKEND_PROFILE_RULES.items():
        if any(re.search(pattern, combined, flags=re.DOTALL) is None for pattern in patterns):
            errors.append(f"OpenCode backend-specialist/command: semántica incompleta para {concept}")


def validate_retirement_and_routing(errors: list[str]) -> None:
    for filename in LEGACY_AGENTS:
        if (ROOT / ".codex" / "agents" / filename).exists():
            errors.append(f"Agente legado aún presente: {filename}")
    for directory in LEGACY_SKILLS:
        if (ROOT / ".agents" / "skills" / directory / "SKILL.md").exists():
            errors.append(f"SKILL legada aún presente: {directory}")
    agents_text = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
    for required in ("frontend-specialist", "backend-specialist", "invoca ambos en paralelo", "propietario canónico"):
        if required not in agents_text:
            errors.append(f"AGENTS.md no contiene routing requerido: {required}")


def main() -> int:
    errors: list[str] = []
    validate_skills(errors)
    validate_skill_semantics(errors)
    validate_codex(errors)
    validate_opencode(errors)
    validate_retirement_and_routing(errors)
    if errors:
        print("Harness PIIP inválido:")
        for error in errors:
            print(f"- {error}")
        return 1
    print("Harness PIIP válido: 2 roles Codex, 4 perfiles OpenCode y 10 SKILLs aisladas.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
