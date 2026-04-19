# Overview

This directory contains the firmware for **Meshtastic devices** (specifically Heltec V3) that are used with the **Akita MeshTAK plugin**.  
This firmware configures the devices to communicate over Meshtastic and interface with ATAK via **BLE** or **Serial**.

It is designed to receive commands (like **SOS** or **Battery Status requests**) from the ATAK plugin and send back responses or broadcast alerts.

---

## Features
- **Meshtastic integration**  
- **Bluetooth Low Energy (BLE)** for direct connection to ATAK  
- **Serial (USB)** communication for tethered connection to ATAK  
- **Optional MQTT support** with explicit Wi-Fi/MQTT credential configuration in `src/config.h`  
- **CoT (Cursor on Target) message generation** (from Meshtastic position packets)  
- **Guaranteed-delivery mailbox relay** for mission traffic queued by the ATAK plugin  
- **Runtime provisioning stage command** so trusted local bearers can rotate device secrets without a rebuild  

**Command Handling:**
- Receives `CMD:ALERT:SOS` from ATAK to trigger a high-power Meshtastic broadcast  
- Receives `CMD:GET_BATT` from ATAK and responds with `STATUS:BATT:XX%`  
- Receives `CMD:GET_VERSION` and responds with `STATUS:VERSION:X.Y.Z`  
- Receives `CMD:MAILBOX:PUT:<messageId>:<format>:<payload>` and returns mailbox acknowledgement/status frames  
- Receives `CMD:PROV:STAGE:<secret>` and returns runtime staging status  
- Real-time battery voltage reading  
- Display handling (for devices with displays)  

---

## Hardware
- **Heltec V3** (or other ESP32-based Meshtastic-compatible devices)  

---

## Software
- **PlatformIO IDE**  

---

# Installation

## Install PlatformIO
Follow the instructions on the PlatformIO website.

## Clone the Repository
Clone the Akita MeshTAK repository.

## Navigate to the Firmware Directory
`cd AkitaMeshTAK/firmware`

## Configure
Prefer environment-driven build inputs over source edits. `tools/load_build_config.py` maps deployment values into preprocessor defines before compilation.

Supported environment variables:

- `AKITA_DEVICE_ID`
- `AKITA_PROVISIONING_SECRET`
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

Placeholders remain guarded with compile-time assertions. If deployment values are absent, the production target fails unless `ALLOW_PLACEHOLDER_SECRET` is explicitly defined for local bench rehearsal.

## Build and Upload

1. Connect your Heltec V3 to your computer via USB.
2. Build the firmware: `pio run`
3. Upload the firmware: `pio run -t upload`

For CI or rehearsal-only builds that intentionally keep placeholder provisioning or UUID material, use `pio run -e heltec_v3_ci` or define `ALLOW_PLACEHOLDER_SECRET` in your PlatformIO build flags. Do not use that override for fielded or production images.

---

# Configuration

Firmware configuration is done in `firmware/src/config.h`.  
Key settings include:

- **DEVICE_ID** – A unique identifier for the device  
- **LORA_REGION** – LoRa region (e.g., `US915`, `EU868`)  
- **BLE_SERVICE_UUID**, **BLE_COT_CHARACTERISTIC_UUID**, **BLE_WRITE_CHARACTERISTIC_UUID** – Must match the ATAK plugin  
- **PROVISIONING_SECRET** – Deployment provisioning secret used to derive transport keys  
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
