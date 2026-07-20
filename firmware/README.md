# Overview

This directory contains the Akita **companion-controller firmware** for an ESP32/Heltec V3. It does not drive the Heltec's onboard LoRa radio and is not a replacement for official Meshtastic firmware. The controller connects over UART to a separate Meshtastic node and exposes ATAK-facing BLE or USB serial.

It is designed to receive commands (like **SOS** or **Battery Status requests**) from the ATAK plugin and send back responses or broadcast alerts.

---

## Features
- **Meshtastic integration**
- **Bluetooth Low Energy (BLE)** for direct connection to ATAK
- **Serial (USB)** communication for tethered connection to ATAK
- **Experimental MQTT support** for isolated bench testing; production use is blocked until the transport uses TLS
- **CoT (Cursor on Target) message generation** (from Meshtastic position packets)
- **Fragmented, acknowledgement-tracked mailbox relay** for mission traffic queued by the ATAK plugin
- **Physical-presence runtime provisioning** with persistent ESP32 NVS storage

**Command Handling:**
- Receives `CMD:ALERT:SOS` from ATAK to trigger a high-power Meshtastic broadcast
- Receives `CMD:GET_BATT` from ATAK and responds with `STATUS:BATT:XX%`
- Receives `CMD:GET_VERSION` and responds with `STATUS:VERSION:X.Y.Z`
- Receives `CMD:MAILBOX:PUT:<messageId>:<format>:<payload>` and returns mailbox acknowledgement/status frames
- Receives `CMD:PROV:STAGE:<secret>:<epoch-seconds>` during the physical-presence window, synchronizes its clock, and returns authenticated staging status
- Real-time battery voltage reading
- Display handling (for devices with displays)

---

## Hardware
- **Akita controller:** ESP32/Heltec V3
- **Mesh radio:** a separate device running official Meshtastic firmware
- UART wiring between the controller and Meshtastic node

---

## Software
- **PlatformIO IDE**

---

# Installation

## Install PlatformIO
Install the CI-pinned PlatformIO version:

```bash
python3 -m pip install platformio==6.1.19
```

`platformio.ini` exact-pins the ESP32 platform and every direct/transitive library used by the release build; the Git-based Meshtastic dependency is pinned to an immutable commit. Dependency pin changes require a reviewed CI build and hardware acceptance run.

If the checkout is on a removable or FUSE-backed filesystem and the ESP32 compiler stalls while writing object files, keep the source checkout in place and redirect only generated build output to a native filesystem: `PLATFORMIO_BUILD_DIR=/tmp/akita-meshtak-build pio run -e heltec_v3_ci`.

## Clone the Repository
Clone the Akita MeshTAK repository.

## Navigate to the Firmware Directory
`cd AkitaMeshTAK/firmware`

## Configure
Prefer environment-driven build inputs over source edits. `tools/load_build_config.py` maps deployment values into preprocessor defines before compilation.

Supported environment variables:

- `AKITA_DEVICE_ID`
- `AKITA_MESH_SERIAL_RX_PIN`
- `AKITA_MESH_SERIAL_TX_PIN`
- `AKITA_BLE_SERVICE_UUID`
- `AKITA_BLE_COT_CHARACTERISTIC_UUID`
- `AKITA_BLE_WRITE_CHARACTERISTIC_UUID`
- `AKITA_MQTT_SERVER`
- `AKITA_MQTT_PORT`
- `AKITA_MQTT_TOPIC_PREFIX`
- `AKITA_MQTT_WIFI_SSID`
- `AKITA_MQTT_WIFI_PASSWORD`
- `AKITA_MQTT_USERNAME`
- `AKITA_MQTT_PASSWORD`
- `AKITA_ALLOW_INSECURE_MQTT` (isolated bench testing only; never set for a field image)

Placeholder BLE UUIDs and unwired mesh UART pins are guarded with compile-time assertions. The `heltec_v3_ci` environment bypasses those wiring checks only to compile-test the code.

## Build and Upload

1. Connect your Heltec V3 to your computer via USB.
2. Build production firmware: `pio run -e heltec_v3`
3. Upload production firmware: `pio run -e heltec_v3 -t upload`

For CI builds that intentionally keep placeholder UUIDs and unwired UART pins, use `pio run -e heltec_v3_ci`. Never deploy that environment's image.

MQTT is disabled by default. The current implementation uses plaintext TCP and is not authorized for production. Enabling it requires the explicit `ALLOW_INSECURE_MQTT` compile definition (or `AKITA_ALLOW_INSECURE_MQTT=true`) and is limited to an isolated bench network. A production MQTT release requires certificate-validated TLS support and removal of that override.

---

# Configuration

Firmware configuration is done in `firmware/src/config.h`.
Key settings include:

- **DEVICE_ID** – A unique identifier for the device
- **MESH_SERIAL_RX_PIN**, **MESH_SERIAL_TX_PIN** – UART connection to the separate Meshtastic node
- **BLE_SERVICE_UUID**, **BLE_COT_CHARACTERISTIC_UUID**, **BLE_WRITE_CHARACTERISTIC_UUID** – Must match the ATAK plugin
- **PROVISION_BUTTON_PIN**, **PROVISIONING_WINDOW_MS** – physical-presence provisioning controls
- **ENCRYPTED_PAYLOAD_VERSION**, **ENCRYPTED_KEY_ID** – Envelope metadata that must match the ATAK plugin
- **CMD_GET_BATT**, **CMD_ALERT_SOS**, **STATUS_BATT_PREFIX** – Command strings that must match the plugin
- **CMD_MAILBOX_PUT_PREFIX**, **CMD_PROVISION_STAGE_PREFIX**, **STATUS_MAILBOX_ACK_PREFIX**, **STATUS_MAILBOX_RX_PREFIX**, **STATUS_PROVISION_STAGED_PREFIX**, **STATUS_PROVISION_FAILED_PREFIX** – Mailbox and provisioning command/status strings
- **ENABLE_BLE**, **ENABLE_SERIAL**, **ENABLE_MQTT**, **ENABLE_DISPLAY** – Feature toggles
- **CMD_RATE_LIMIT_MS** – Minimum accepted interval between BLE/Serial commands
- **MQTT_SERVER**, **MQTT_WIFI_SSID**, **MQTT_WIFI_PASSWORD**, **MQTT_USERNAME**, **MQTT_PASSWORD** – Required when MQTT is enabled

For debug work, firmware audit traffic only mirrors to the serial console when `DEBUG_AUDIT` is defined.

---

# Code Structure

- `src/` – Main application source files
- `src/payload_codec.h/.cpp` – Shared encrypted payload encode/decode utilities for BLE and Serial
- `src/mailbox_escape.h/.cpp` – Shared mailbox payload escaping utilities
- `lib/` – External libraries
- `platformio.ini` – PlatformIO configuration file

---

# Contributing

See `documentation/dev_guide.md` for contribution guidelines.

---

# License

Licensed under the **GNU General Public License v3.0**.
See the `LICENSE` and `COPYING` files in the project root.

**Copyright (C) 2026 Akita Engineering**
