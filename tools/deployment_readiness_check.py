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

    wrapper = read_text("atak_plugin/gradle/wrapper/gradle-wrapper.properties")
    wrapper_url = re.search(r"distributionUrl=.*gradle-([0-9.]+)-bin\.zip", wrapper)
    wrapper_sha = re.search(r"distributionSha256Sum=([0-9a-f]+)", wrapper)
    known_wrapper_hashes = {
        "8.7": "544c35d6bd849ae8a5ed0bcea39ba677dc40f49df7d1835561582da2009b961d",
    }
    check(wrapper_url is not None and wrapper_sha is not None,
          "Gradle wrapper declares a versioned distribution and SHA-256", failures)
    if wrapper_url is not None and wrapper_sha is not None:
        gradle_version = wrapper_url.group(1)
        expected_sha = known_wrapper_hashes.get(gradle_version)
        check(expected_sha is not None and wrapper_sha.group(1) == expected_sha,
              "Gradle wrapper SHA-256 matches the declared distribution", failures)

    plugin_envelope = read_text("atak_plugin/src/com/akitaengineering/meshtak/PayloadEnvelope.java")
    plugin_replay = read_text("atak_plugin/src/com/akitaengineering/meshtak/ReplayGuard.java")
    firmware_payload = read_text("firmware/src/payload_codec.cpp")
    firmware_replay = read_text("firmware/src/replay_guard.cpp")
    firmware_security = read_text("firmware/src/security.cpp")
    plugin_security = read_text("atak_plugin/src/com/akitaengineering/meshtak/SecurityManager.java")
    plugin_config = read_text("atak_plugin/src/com/akitaengineering/meshtak/Config.java")
    check("ReplayGuard" in plugin_envelope and "attach(" in plugin_replay,
          "plugin replay defense persists across process restart", failures)
    check("replayGuardRemember" in firmware_payload and "akita-rpl" in firmware_replay,
          "firmware replay defense persists across reboot", failures)
    check("initSecurityFromKeySlots" in firmware_security and "previousSlot" in plugin_security,
          "firmware and plugin keep an overlapping previous key slot", failures)
    check("ENCRYPTED_KEY_ID_K2" in plugin_config and "KEY_ID_K2" in firmware_config,
          "k1/k2 overlapping key identifiers are defined on both sides", failures)
    check("CMD_GET_SEC_STATE" in firmware_config and "CMD_GET_SEC_STATE" in plugin_config,
          "controller security-state command is defined on firmware and plugin", failures)
    check("esp_flash_encryption_enabled" in read_text("firmware/src/hardware_security.cpp"),
          "firmware reports ESP32 flash-encryption posture", failures)

    version_properties = read_text("version.properties")
    check("VERSION_NAME=0.2.1" in version_properties, "release metadata is 0.2.1", failures)
    check("MIN_FIRMWARE_VERSION=0.2.1" in version_properties,
          "plugin requires firmware 0.2.1 for overlapping keys and durable replay", failures)

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
