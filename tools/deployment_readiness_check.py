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
    platformio_config = read_text("firmware/platformio.ini")
    firmware_config = read_text("firmware/src/config.h")
    plugin_config = read_text("atak_plugin/src/com/akitaengineering/meshtak/Config.java")
    ci_config = read_text(".github/workflows/ci.yml")
    cot_generation = read_text("firmware/src/cot_generation.cpp")
    power_management = read_text("firmware/src/power_management.cpp")
    ots_doc = read_text("documentation/opentakserver_compatibility.md")

    check("documentation/private/" in gitignore, "private planning docs are ignored", failures)
    check("*_PRIVATE.md" in gitignore, "private markdown pattern is ignored", failures)
    check("*.keystore" in gitignore and "*.jks" in gitignore and "*.p12" in gitignore,
          "signing material patterns are ignored", failures)
    signing_material = []
    for suffix in ("*.keystore", "*.jks", "*.p12", "*.pfx"):
        signing_material.extend(ROOT.glob(suffix))
    check(not signing_material, "signing material is stored outside the repository root", failures)
    check("platformio/espressif32@6.12.0" in platformio_config,
          "firmware platform is exact-pinned", failures)
    pinned_libraries = [
        "Meshtastic-arduino.git#77cdc035dbc3813c5f64efa24d20dcb698cdfc59",
        "Heltec ESP32 Dev-Boards@2.1.5",
        "TinyGPSPlus@1.1.0",
        "PubSubClient@2.8",
        "Adafruit BusIO@1.17.4",
        "Adafruit GFX Library@1.12.6",
    ]
    check(all(dependency in platformio_config for dependency in pinned_libraries),
          "firmware libraries are exact-pinned", failures)
    firmware_main = read_text("firmware/src/main.cpp")
    firmware_loader = read_text("firmware/tools/load_build_config.py")
    provisioning_store = read_text("firmware/src/provisioning_store.cpp")
    check("PROVISIONING_SECRET" not in firmware_config + firmware_main + firmware_loader,
          "firmware contains no compile-time provisioning-secret path", failures)
    check("Preferences" in provisioning_store and "isProvisioningWindowOpen" in provisioning_store,
          "firmware provisioning requires the physical-presence NVS workflow", failures)
    check("ALLOW_INSECURE_MQTT" in firmware_config,
          "plaintext MQTT requires an explicit bench-only override", failures)
    plugin_security = read_text("atak_plugin/src/ui/AkitaProvisioningManager.java")
    check("PROVISIONING_SECRET" not in plugin_config,
          "plugin configuration contains no embedded provisioning secret", failures)
    check("ProvisioningStateStore" in plugin_security and '"AES_GCM"' in plugin_security,
          "plugin provisioning state uses encrypted storage", failures)
    check("CMD_TIME_SYNC_PREFIX" in firmware_config and "CMD_TIME_SYNC_PREFIX" in plugin_config,
          "time sync command is defined on firmware and plugin", failures)
    check("CMD_COT_MISSION_PREFIX" in firmware_config and "CMD_COT_MISSION_PREFIX" in plugin_config,
          "mission sync command is defined on firmware and plugin", failures)
    check("<dest mission='" in cot_generation, "firmware can emit OpenTAKServer mission dest tags", failures)
    check("settimeofday" in power_management, "firmware accepts trusted time sync", failures)
    check("STATUS_TIME_SYNC_PREFIX" in power_management, "firmware reports time sync status", failures)
    check("STATUS_COT_MISSION_PREFIX" in power_management, "firmware reports mission tag status", failures)
    check("Mission CoT tagging" in ots_doc, "OpenTAKServer compatibility doc covers mission tagging", failures)
    check("platformio==6.1.19" in ci_config, "CI pins PlatformIO", failures)
    check("deployment_readiness_check.py" in ci_config and "validate_opentakserver_cot.py" in ci_config,
          "CI runs deployment and CoT static checks", failures)

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
