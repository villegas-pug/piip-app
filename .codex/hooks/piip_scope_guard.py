#!/usr/bin/env python3
"""Bloquea escrituras fuera del dominio de un especialista PIIP."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SCOPES = {
    "frontend": REPOSITORY_ROOT / "apps" / "frontend",
    "backend": REPOSITORY_ROOT / "apps" / "backend",
}

DESTRUCTIVE_SHELL_PATTERNS = (
    r"\bgit\s+(?:commit|push|reset|clean|checkout|restore|switch|merge|rebase|cherry-pick)\b",
    r"\bRemove-Item\b",
    r"\b(?:rm|rmdir|del|erase)\s+",
    r"\b(?:Set-Content|Add-Content|Out-File|Copy-Item|Move-Item|Rename-Item|New-Item)\b",
    r"\b(?:touch|mkdir|cp|mv)\s+",
    r"\bsed\s+-i\b",
    r"\bperl\s+-i\b",
)


def deny(reason: str) -> None:
    print(
        json.dumps(
            {
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "deny",
                    "permissionDecisionReason": reason,
                }
            },
            ensure_ascii=False,
        )
    )
    raise SystemExit(0)


def normalized_path(raw_path: str) -> Path:
    candidate = Path(raw_path.strip().strip('"\''))
    if not candidate.is_absolute():
        candidate = REPOSITORY_ROOT / candidate
    return candidate.resolve(strict=False)


def is_within(candidate: Path, allowed_root: Path) -> bool:
    try:
        candidate.relative_to(allowed_root.resolve(strict=False))
        return True
    except ValueError:
        return False


def patch_targets(command: str) -> tuple[list[str], bool]:
    targets: list[str] = []
    deletes = False
    for line in command.splitlines():
        match = re.match(r"\*\*\* (Add|Update|Delete) File: (.+)$", line)
        if match:
            operation, path = match.groups()
            targets.append(path)
            deletes = deletes or operation == "Delete"
            continue
        move_match = re.match(r"\*\*\* Move to: (.+)$", line)
        if move_match:
            targets.append(move_match.group(1))
    return targets, deletes


def collect_file_targets(value: Any, key: str = "") -> list[str]:
    targets: list[str] = []
    if isinstance(value, dict):
        for child_key, child_value in value.items():
            targets.extend(collect_file_targets(child_value, child_key.lower()))
    elif isinstance(value, list):
        for child in value:
            targets.extend(collect_file_targets(child, key))
    elif isinstance(value, str) and any(token in key for token in ("path", "file", "target")):
        targets.append(value)
    return targets


def validate_edit(tool_name: str, tool_input: dict[str, Any], allowed_root: Path) -> None:
    if tool_name == "apply_patch":
        command = str(tool_input.get("command", ""))
        targets, deletes = patch_targets(command)
        if deletes:
            deny("Los especialistas PIIP no pueden borrar archivos.")
        if not targets:
            deny("No se pudo determinar el destino del parche del especialista PIIP.")
    else:
        targets = collect_file_targets(tool_input)
        if not targets:
            deny("No se pudo determinar el archivo de destino de la edición PIIP.")

    for raw_target in targets:
        target = normalized_path(raw_target)
        if not is_within(target, allowed_root):
            deny(f"Edición fuera del scope permitido: {raw_target}")


def validate_shell(command: str, scope: str) -> None:
    for pattern in DESTRUCTIVE_SHELL_PATTERNS:
        if re.search(pattern, command, flags=re.IGNORECASE):
            deny("Comando destructivo o escritura por shell bloqueada para especialistas PIIP.")

    if scope == "frontend" and re.search(r"\bmvn(?:\.cmd)?\b", command, flags=re.IGNORECASE):
        deny("El especialista frontend no puede ejecutar comandos Maven.")
    if scope == "backend" and re.search(r"\b(?:npm|npx|ng)(?:\.cmd)?\b", command, flags=re.IGNORECASE):
        deny("El especialista backend no puede ejecutar comandos Angular/npm.")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--scope", choices=sorted(SCOPES), required=True)
    args = parser.parse_args()

    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, TypeError) as error:
        deny(f"Entrada inválida para el guard PIIP: {error}")

    tool_name = str(payload.get("tool_name", ""))
    tool_input = payload.get("tool_input") or {}
    if not isinstance(tool_input, dict):
        deny("Entrada de herramienta no reconocida por el guard PIIP.")

    if tool_name in {"apply_patch", "Edit", "Write"}:
        validate_edit(tool_name, tool_input, SCOPES[args.scope])
    elif tool_name == "Bash":
        validate_shell(str(tool_input.get("command", "")), args.scope)


if __name__ == "__main__":
    main()
