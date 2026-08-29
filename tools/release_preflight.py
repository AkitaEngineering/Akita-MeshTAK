#!/usr/bin/env python3
"""Local field-shipment preflight for Akita MeshTAK.

Reports missing external inputs. Never prints secret values. Not used by CI.
"""

from __future__ import annotations

import os
import pathlib
import stat
import subprocess
import sys
import uuid


ROOT = pathlib.Path(__file__).resolve().parents[1]
REQUIRED_ENV = (
    "AKITA_DEVICE_ID",
    "AKITA_MESH_SERIAL_RX_PIN",
    "AKITA_MESH_SERIAL_TX_PIN",
    "AKITA_BLE_SERVICE_UUID",
    "AKITA_BLE_COT_CHARACTERISTIC_UUID",
    "AKITA_BLE_WRITE_CHARACTERISTIC_UUID",
    "AKITA_ATAK_SDK_JAR",
    "AKITA_RELEASE_KEYSTORE_FILE",
    "AKITA_RELEASE_STORE_PASSWORD",
    "AKITA_RELEASE_KEY_ALIAS",
    "AKITA_RELEASE_KEY_PASSWORD",
)
FORBIDDEN_TRUE_ENV = (
    "AKITA_ALLOW_PLACEHOLDER_SECRET",
    "AKITA_ALLOW_INSECURE_MQTT",
    "AKITA_USE_ATAK_STUB",
)
PLACEHOLDER_UUIDS = {
    "0000181A-0000-1000-8000-00805F9B34FB",
    "00002A6E-0000-1000-8000-00805F9B34FB",
    "00002A6C-0000-1000-8000-00805F9B34FB",
}
UUID_ENV_NAMES = (
    "AKITA_BLE_SERVICE_UUID",
    "AKITA_BLE_COT_CHARACTERISTIC_UUID",
    "AKITA_BLE_WRITE_CHARACTERISTIC_UUID",
)
PIN_ENV_NAMES = (
    "AKITA_MESH_SERIAL_RX_PIN",
    "AKITA_MESH_SERIAL_TX_PIN",
)


def env_value(name: str) -> str:
    return os.environ.get(name, "").strip()


def is_truthy(name: str) -> bool:
    return env_value(name).lower() in {"1", "true", "yes", "on"}


def report(ok: bool, message: str, failures: list[str]) -> None:
    status = "OK" if ok else "FAIL"
    print(f"{status}: {message}")
    if not ok:
        failures.append(message)


def resolve_sdk_jar(configured: str) -> pathlib.Path | None:
    path = pathlib.Path(configured).expanduser()
    if not path.is_absolute():
        path = (pathlib.Path.cwd() / path).resolve()
    else:
        path = path.resolve()
    if path.is_dir():
        path = path / "main.jar"
    if path.is_file():
        return path
    return None


def main() -> int:
    failures: list[str] = []
    print("Akita MeshTAK field-shipment preflight", flush=True)
    print(f"Repository: {ROOT}", flush=True)
    print(flush=True)

    static = subprocess.run(
        [sys.executable, str(ROOT / "tools" / "deployment_readiness_check.py")],
        cwd=ROOT,
    )
    report(static.returncode == 0, "static deployment readiness check passed", failures)
    print()

    for name in REQUIRED_ENV:
        present = bool(env_value(name))
        report(present, f"{name} is set" if present else f"{name} is missing", failures)

    for name in FORBIDDEN_TRUE_ENV:
        report(not is_truthy(name), f"{name} is not enabled for field shipment", failures)

    for name in UUID_ENV_NAMES:
        value = env_value(name)
        if not value:
            continue
        try:
            parsed = str(uuid.UUID(value))
        except ValueError:
            report(False, f"{name} is a valid UUID", failures)
            continue
        report(
            parsed.upper() not in PLACEHOLDER_UUIDS and not value.upper().startswith("YOUR_"),
            f"{name} is not a placeholder/default UUID",
            failures,
        )

    for name in PIN_ENV_NAMES:
        value = env_value(name)
        if not value:
            continue
        try:
            pin = int(value, 10)
        except ValueError:
            report(False, f"{name} is an integer pin number", failures)
            continue
        report(pin >= 0, f"{name} is a non-negative UART pin", failures)

    sdk_path = env_value("AKITA_ATAK_SDK_JAR")
    if sdk_path:
        sdk_jar = resolve_sdk_jar(sdk_path)
        report(sdk_jar is not None, "official ATAK SDK jar exists at AKITA_ATAK_SDK_JAR", failures)

    keystore_path = env_value("AKITA_RELEASE_KEYSTORE_FILE")
    if is_truthy("AKITA_OTS_SSL"):
        cert_path = env_value("AKITA_OTS_CLIENT_P12")
        cert = pathlib.Path(cert_path).expanduser() if cert_path else None
        report(cert is not None and cert.is_file(),
               "AKITA_OTS_CLIENT_P12 exists because AKITA_OTS_SSL is enabled",
               failures)

    if keystore_path:
        keystore = pathlib.Path(keystore_path).expanduser().resolve()
        report(keystore.is_file(), "release keystore file exists", failures)
        try:
            report(
                ROOT.resolve() not in keystore.parents and keystore != ROOT.resolve(),
                "release keystore is outside the source checkout",
                failures,
            )
        except OSError:
            report(False, "release keystore path is readable", failures)
        if keystore.is_file():
            mode = stat.S_IMODE(keystore.stat().st_mode)
            report(
                (mode & (stat.S_IRWXG | stat.S_IRWXO)) == 0,
                "release keystore permissions are owner-only",
                failures,
            )

    print()
    if failures:
        print("Field shipment is not ready:")
        for failure in failures:
            print(f"- {failure}")
        print()
        print("Fill `.env.example` in a file outside this checkout, then rebuild with:")
        print("  platformio run -d firmware -e heltec_v3")
        print("  cd atak_plugin && ./gradlew --no-daemon assembleRelease")
        return 1

    print("Field-shipment inputs look complete. Build heltec_v3 firmware and assembleRelease next.")
    print("Hardware acceptance and OpenTAKServer live checks are still required before tagging.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
