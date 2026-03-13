# Akita MeshTAK Plugin Developer Guide

## Introduction
This guide provides information for developers who want to contribute to the **Akita MeshTAK Plugin**.

---

# Project Overview

The Akita MeshTAK system consists of two main components:

- **Firmware:** Code running on Meshtastic devices (e.g., Heltec V3)  
- **ATAK Plugin:** Android application that integrates Meshtastic with ATAK

---

# Code Structure

```
AkitaMeshTAK/
├── firmware/ # Firmware for Meshtastic devices
│ ├── src/
│ │ ├── main.cpp # Application entry point
│ │ ├── config.h # CRITICAL: UUIDs / Commands
│ │ ├── ble_setup.h/.cpp # BLE peripheral setup
│ │ ├── meshtastic_setup.h/.cpp # Meshtastic mesh integration
│ │ ├── serial_bridge.h/.cpp # Serial/USB communication
│ │ ├── mqtt_client.h/.cpp # Optional MQTT client
│ │ ├── cot_generation.h/.cpp # CoT XML generation
│ │ ├── display_handler.h/.cpp # OLED display management
│ │ ├── power_management.h/.cpp # Battery & power
│ │ ├── security.h/.cpp # AES-256-GCM encryption / HMAC
│ │ ├── audit_log.h/.cpp # Security audit logging
│ │ └── input_validation.h/.cpp # Input sanitization
│ └── platformio.ini # PlatformIO build config (Meshtastic-arduino v0.0.7)
├── atak_plugin/ # ATAK plugin source code
│ ├── src/
│ │ ├── AkitaMeshTAKPlugin.java # Plugin lifecycle
│ │ ├── com/akitaengineering/meshtak/
│ │ │ ├── Config.java # CRITICAL: UUIDs / VIDs
│ │ │ ├── AuditLogger.java # Security audit logging
│ │ │ ├── SecurityManager.java # AES-256-GCM encryption / HMAC
│ │ │ └── VersionManager.java # Version management
│ │ ├── services/
│ │ │ ├── BLEService.java
│ │ │ └── SerialService.java
│ │ └── ui/
│ │ ├── AkitaToolbar.java
│ │ ├── ConnectionStatusOverlay.java
│ │ ├── SendDataView.java
│ │ └── SettingsFragment.java
│ ├── res/ # Android XML resources
│ ├── build.gradle # Gradle build config (targetSdk 35)
│ └── libs/atak-sdk.jar # ATAK SDK (compileOnly)
├── documentation/ # Documentation
└── LICENSE / COPYING # GPLv3 license files
```

---

# Building the Project

## Firmware

The firmware is built using **PlatformIO**.

1. Install PlatformIO  
2. Navigate to the `firmware/` directory  
3. Configure UUIDs: edit `firmware/src/config.h`  
4. Build: ```pio run```
5. Upload: ```pio run -t upload```

---

## ATAK Plugin

The ATAK plugin is built using **Android Studio**.

1. Install Android Studio and Android SDK  
2. Configure ATAK SDK as a library module  
3. Open `atak_plugin/` in Android Studio  
4. Configure UUIDs & USB IDs: edit ```atak_plugin/src/com/akitaengineering/meshtak/Config.java```
5. Build the APK

---

# Command & Status Protocol

Communication uses a simple string-based command protocol.

## ATAK → Firmware
- `CMD:GET_BATT`  
Requests battery status  
- `CMD:GET_VERSION`  
Requests firmware version  
- `CMD:ALERT:SOS`  
Triggers SOS alert broadcast  

## Firmware → ATAK
- `STATUS:BATT:XX%`  
Response to battery query (e.g., `STATUS:BATT:85%`)  
- `VERSION:X.Y.Z`  
Response to version query (e.g., `VERSION:0.2.0`)

All other received data is treated as **CoT XML**.

Protocol definitions exist in:

- `firmware/src/config.h`  
- `atak_plugin/src/com/akitaengineering/meshtak/Config.java`

---

# Contributing Guidelines

1. Fork the repository  
2. Create a new branch for your feature or fix  
3. Follow code style conventions (Google Java/C++ style)  
4. Write clear commit messages  
5. Update documentation for your changes  
6. Test thoroughly  
7. Submit a pull request  

---

# License

This project is licensed under the **GNU General Public License v3.0**.  
See the `LICENSE` and `COPYING` files in the root directory.

**Copyright (C) 2025-2026 Akita Engineering**


