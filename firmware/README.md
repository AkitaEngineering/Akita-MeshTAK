# Overview

This directory contains the firmware for **Meshtastic devices** (specifically Heltec V3) that are used with the **Akita MeshTAK plugin**.  
This firmware configures the devices to communicate over Meshtastic and interface with ATAK via **BLE** or **Serial**.

It is designed to receive commands (like **SOS** or **Battery Status requests**) from the ATAK plugin and send back responses or broadcast alerts.

---

## Features
- **Meshtastic integration**  
- **Bluetooth Low Energy (BLE)** for direct connection to ATAK  
- **Serial (USB)** communication for tethered connection to ATAK  
- **Optional MQTT support**  
- **CoT (Cursor on Target) message generation** (from Meshtastic position packets)  

**Command Handling:**
- Receives `CMD:ALERT:SOS` from ATAK to trigger a high-power Meshtastic broadcast  
- Receives `CMD:GET_BATT` from ATAK and responds with `STATUS:BATT:XX%`  
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
Open `firmware/src/config.h` and set your BLE UUIDs.  
These **MUST** match the values you set in the ATAK plugin's `Config.java` file.

## Build and Upload

1. Connect your Heltec V3 to your computer via USB.
2. Build the firmware: `pio run`
3. Upload the firmware: `pio run -t upload`

---

# Configuration

Firmware configuration is done in `firmware/src/config.h`.  
Key settings include:

- **DEVICE_ID** – A unique identifier for the device  
- **LORA_REGION** – LoRa region (e.g., `US915`, `EU868`)  
- **BLE_SERVICE_UUID**, **BLE_COT_CHARACTERISTIC_UUID**, **BLE_WRITE_CHARACTERISTIC_UUID** – Must match the ATAK plugin  
- **CMD_GET_BATT**, **CMD_ALERT_SOS**, **STATUS_BATT_PREFIX** – Command strings that must match the plugin  
- **ENABLE_BLE**, **ENABLE_SERIAL**, **ENABLE_MQTT**, **ENABLE_DISPLAY** – Feature toggles

---

# Code Structure

- `src/` – Main application source files  
- `lib/` – External libraries  
- `platformio.ini` – PlatformIO configuration file

---

# Contributing

See `documentation/dev_guide.md` for contribution guidelines.

---

# License

Licensed under the **GNU General Public License v3.0**.  
See the `LICENSE` and `COPYING` files in the project root.

**Copyright (C) 2025 Akita Engineering**
