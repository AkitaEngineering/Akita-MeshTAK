#!/usr/bin/env python3
"""Static deployment readiness checks for Akita MeshTAK."""

from __future__ import annotations

import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
EMOJI_RE = re.compile(r"[\U0001F300-\U0001FAFF\u2600-\u27BF]")


def read_text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8", errors="replace")


def check(condition: bool, message: str, failures: list[str]) -> None:
    status = "OK" if condition else "FAIL"
    print(f"{status}: {message}")
    if not condition:
        failures.append(message)


def main() -> int:
    failures: list[str] = []

    gitignore = read_text(".gitignore")
    firmware_config = read_text("firmware/src/config.h")
    plugin_config = read_text("atak_plugin/src/com/akitaengineering/meshtak/Config.java")
    cot_generation = read_text("firmware/src/cot_generation.cpp")
    power_management = read_text("firmware/src/power_management.cpp")
    ots_doc = read_text("documentation/opentakserver_compatibility.md")

    check("documentation/private/" in gitignore, "private planning docs are ignored", failures)
    check("*_PRIVATE.md" in gitignore, "private markdown pattern is ignored", failures)
    check("ALLOW_PLACEHOLDER_SECRET" in firmware_config, "firmware placeholder guard exists", failures)
    check("isPlaceholderSecret" in plugin_config, "plugin placeholder detection exists", failures)
    check("CMD_TIME_SYNC_PREFIX" in firmware_config and "CMD_TIME_SYNC_PREFIX" in plugin_config,
          "time sync command is defined on firmware and plugin", failures)
    check("CMD_COT_MISSION_PREFIX" in firmware_config and "CMD_COT_MISSION_PREFIX" in plugin_config,
          "mission sync command is defined on firmware and plugin", failures)
    check("<dest mission='" in cot_generation, "firmware can emit OpenTAKServer mission dest tags", failures)
    check("settimeofday" in power_management, "firmware accepts trusted time sync", failures)
    check("STATUS_TIME_SYNC_PREFIX" in power_management, "firmware reports time sync status", failures)
    check("STATUS_COT_MISSION_PREFIX" in power_management, "firmware reports mission tag status", failures)
    check("Mission CoT tagging" in ots_doc, "OpenTAKServer compatibility doc covers mission tagging", failures)

    docs = [
        "README.md",
        "SECURITY_IMPROVEMENTS.md",
        "documentation/DOCUMENTATION_INDEX.md",
        "documentation/opentakserver_compatibility.md",
    ]
    emoji_hits = []
    for doc in docs:
        text = read_text(doc)
        if EMOJI_RE.search(text):
            emoji_hits.append(doc)
    check(not emoji_hits, "public documentation has no emoji characters", failures)

    if failures:
        print("\nDeployment readiness check failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("\nDeployment readiness check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
