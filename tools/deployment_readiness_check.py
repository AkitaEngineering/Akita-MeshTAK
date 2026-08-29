#!/usr/bin/env python3
"""Static deployment readiness checks for Akita MeshTAK."""

from __future__ import annotations

import pathlib
import re
import subprocess
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
EMOJI_RE = re.compile(r"[\U0001F300-\U0001FAFF\u2600-\u27BF]")
SKIP_SECRET_SCAN_DIRS = {".git", ".gradle", "build", ".pio", "__pycache__"}
SIGNING_SUFFIXES = (
    ".keystore",
    ".jks",
    ".bks",
    ".p12",
    ".pfx",
    ".pkcs12",
    ".pk8",
    ".pkcs8",
    ".p8",
)
TRACKED_SECRET_SUFFIXES = SIGNING_SUFFIXES + (
    ".pem",
    ".key",
    ".crt",
    ".cer",
    ".der",
    ".p7b",
    ".p7c",
    ".ppk",
    ".ovpn",
    ".kdbx",
    ".pcap",
    ".pcapng",
)
TRACKED_SECRET_NAMES = {
    "local.properties",
    "key.properties",
    "keystore.properties",
    "secrets.properties",
    "secrets.gradle",
    "google-services.json",
    "credentials.json",
    "auth.json",
    "akita-provisioning-state.json",
    "secrets.h",
    "arduino_secrets.h",
    "atak-sdk.jar",
    "secring.gpg",
    "id_rsa",
    "id_dsa",
    "id_ecdsa",
    "id_ed25519",
}
REQUIRED_IGNORED_PATHS = (
    "release.keystore",
    "atak_plugin/upload.jks",
    "certs/client.p12",
    "certs/tls.pem",
    "secrets/deploy.key",
    "key.properties",
    "atak_plugin/key.properties",
    "secrets.properties",
    ".env",
    "atak_plugin/google-services.json",
    "akita-provisioning-state.json",
    "atak_plugin/libs/atak-sdk.jar",
    "documentation/private/notes.md",
    "id_rsa",
)


def read_text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8", errors="replace")


def check(condition: bool, message: str, failures: list[str]) -> None:
    status = "OK" if condition else "FAIL"
    print(f"{status}: {message}")
    if not condition:
        failures.append(message)


def path_is_skipped(path: pathlib.Path) -> bool:
    return any(part in SKIP_SECRET_SCAN_DIRS for part in path.parts)


def git_output(args: list[str]) -> subprocess.CompletedProcess[bytes]:
    return subprocess.run(
        ["git", *args],
        cwd=ROOT,
        check=False,
        capture_output=True,
    )


def git_tracked_files() -> list[str] | None:
    result = git_output(["ls-files", "-z"])
    if result.returncode != 0:
        return None
    return [entry for entry in result.stdout.decode("utf-8", "replace").split("\0") if entry]


def git_ignores(relative_path: str) -> bool | None:
    result = git_output(["check-ignore", "-q", "--no-index", "--", relative_path])
    if result.returncode == 0:
        return True
    if result.returncode == 1:
        return False
    return None


PROPERTY_SECRET_KEY_MARKERS = ("password", "secret", "apikey", "api_key", "token", "keystore")


def committed_properties_have_no_secrets(text: str) -> bool:
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        key = stripped.split("=", 1)[0].strip().lower()
        if any(marker in key for marker in PROPERTY_SECRET_KEY_MARKERS):
            return False
    return True


def is_secret_tracked_path(relative_path: str) -> bool:
    posix_path = relative_path.replace("\\", "/")
    name = pathlib.PurePosixPath(posix_path).name
    suffix = pathlib.PurePosixPath(posix_path).suffix.lower()
    if posix_path.startswith("documentation/private/"):
        return True
    if name.endswith("_PRIVATE.md") or name.endswith("_TODO_PRIVATE.md"):
        return True
    if name.startswith(".env") and name not in {".env.example", ".env.sample"}:
        return True
    if name.startswith("service-account") and name.endswith(".json"):
        return True
    if name.endswith("-credentials.json") or name.endswith(".secrets"):
        return True
    if name.startswith("audit_log_") and name.endswith(".txt"):
        return True
    if suffix in TRACKED_SECRET_SUFFIXES:
        return True
    return name in TRACKED_SECRET_NAMES


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
    check(".env" in gitignore and "key.properties" in gitignore and "google-services.json" in gitignore,
          "env files, key properties, and cloud credential JSON are ignored", failures)
    check("akita-provisioning-state.json" in gitignore, "provisioning-state exports are ignored", failures)
    env_example = read_text(".env.example")
    check("AKITA_RELEASE_KEYSTORE_FILE=" in env_example and "AKITA_ATAK_SDK_JAR=" in env_example,
          "env example documents field-shipment inputs", failures)
    check("*.pem" in gitignore and "*.key" in gitignore, "PEM and private-key files are ignored", failures)
    check("!atak_plugin/gradle/wrapper/gradle-wrapper.jar" in gitignore,
          "Gradle wrapper jar remains committable", failures)
    signing_material = [
        path for path in ROOT.rglob("*")
        if path.is_file()
        and not path_is_skipped(path)
        and path.suffix.lower() in SIGNING_SUFFIXES
    ]
    check(not signing_material, "signing material is stored outside the repository root", failures)
    ignore_misses = []
    ignore_probe_available = True
    for relative_path in REQUIRED_IGNORED_PATHS:
        ignored = git_ignores(relative_path)
        if ignored is None:
            ignore_probe_available = False
            break
        if not ignored:
            ignore_misses.append(relative_path)
    if ignore_probe_available:
        check(not ignore_misses, "gitignore covers signing keys, env files, and credential exports", failures)
        if ignore_misses:
            print("    missed: " + ", ".join(ignore_misses))
    tracked_files = git_tracked_files()
    if tracked_files is not None:
        tracked_secrets = [path for path in tracked_files if is_secret_tracked_path(path)]
        check(not tracked_secrets, "git is not tracking secret or credential files", failures)
        if tracked_secrets:
            print("    tracked: " + ", ".join(tracked_secrets[:12]))
    check(committed_properties_have_no_secrets(read_text("atak_plugin/gradle.properties")),
          "committed Gradle properties contain no secret keys", failures)
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
    check("CMD_COT_IDENTITY_PREFIX" in firmware_config and "CMD_COT_IDENTITY_PREFIX" in plugin_config,
          "CoT identity command is defined on firmware and plugin", failures)
    check("CMD_COT_STALE_PREFIX" in firmware_config and "CMD_COT_STALE_PREFIX" in plugin_config,
          "CoT stale command is defined on firmware and plugin", failures)
    check("CMD_MESH_ATAK_PREFIX" in firmware_config and "CMD_MESH_ATAK_PREFIX" in plugin_config,
          "ATAK_PLUGIN protobuf command is defined on firmware and plugin", failures)
    check("meshtastic_PortNum_ATAK_PLUGIN" in read_text("firmware/src/atak_plugin_codec.cpp"),
          "firmware can send Meshtastic ATAK_PLUGIN packets", failures)
    check("PORTNUM = 72" in read_text("atak_plugin/src/com/akitaengineering/meshtak/AtakPluginPacket.java"),
          "plugin ATAK_PLUGIN codec uses port 72", failures)
    check("msh/2/json" in read_text("atak_plugin/src/com/akitaengineering/meshtak/MeshtasticMqttCodec.java"),
          "plugin maps OpenTAKServer Meshtastic MQTT topics", failures)
    check("b-f-t-file" in read_text("atak_plugin/src/com/akitaengineering/meshtak/DataPackageHandoff.java"),
          "plugin data-package handoff emits fileshare CoT", failures)
    check("AKITA_ATAK_PLUGIN_HOOK" in read_text("firmware/tools/patch_meshtastic_atak.py"),
          "firmware patches Meshtastic-arduino for ATAK_PLUGIN receive", failures)
    check("<status battery=" in cot_generation,
          "firmware CoT can emit NodeInfo battery status", failures)
    check("<dest mission='" in cot_generation, "firmware can emit OpenTAKServer mission dest tags", failures)
    check("__group name='%s' role='%s'" in cot_generation, "firmware CoT group identity is configurable", failures)
    plugin_cot = read_text("atak_plugin/src/com/akitaengineering/meshtak/CotEventFactory.java")
    plugin_stream = read_text("atak_plugin/src/com/akitaengineering/meshtak/OpenTakStreamingClient.java")
    plugin_readiness = read_text("atak_plugin/src/com/akitaengineering/meshtak/DeploymentReadinessReport.java")
    check("GEOCHAT_TYPE" in plugin_cot and "b-t-f" in plugin_cot,
          "plugin can generate GeoChat CoT", failures)
    check("OpenTAKServer SSL requires an imported client PKCS#12" in plugin_stream,
          "native OpenTAKServer SSL is fail-closed without a client certificate", failures)
    check("Placeholder or missing secret" in plugin_readiness,
          "plugin deployment readiness report covers placeholder secrets", failures)
    check("android.permission.INTERNET" in read_text("atak_plugin/AndroidManifest.xml"),
          "plugin declares INTERNET for native OpenTAKServer streaming", failures)
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
        "9.7.0": "84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae",
        "9.7.1": "acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a",
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
