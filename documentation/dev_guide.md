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
│ │ ├── config.h # CRITICAL: UUIDs / provisioning / guards / commands
│ │ ├── ble_setup.h/.cpp # BLE peripheral setup
│ │ ├── meshtastic_setup.h/.cpp # Meshtastic mesh integration
│ │ ├── serial_bridge.h/.cpp # Serial/USB communication
│ │ ├── mqtt_client.h/.cpp # Optional MQTT client
│ │ ├── payload_codec.h/.cpp # Shared BLE/Serial encrypted envelope helpers
│ │ ├── mailbox_escape.h/.cpp # Shared mailbox payload escape helpers
│ │ ├── cot_generation.h/.cpp # CoT XML generation
│ │ ├── display_handler.h/.cpp # OLED display management
│ │ ├── power_management.h/.cpp # Battery & power
│ │ ├── security.h/.cpp # AES-256-GCM transport + PBKDF2-HMAC-SHA256 key derivation
│ │ ├── audit_log.h/.cpp # Security audit logging
│ │ └── input_validation.h/.cpp # Input sanitization
│ └── platformio.ini # PlatformIO build config (Meshtastic-arduino v0.0.7)
├── atak_plugin/ # ATAK plugin source code
│ ├── src/
│ │ ├── AkitaMeshTAKPlugin.java # Plugin lifecycle
│ │ ├── com/akitaengineering/meshtak/
│ │ │ ├── Config.java # CRITICAL: UUIDs / VIDs / provisioning fallback
│ │ │ ├── AkitaMissionControl.java # Mailbox queue, replay, and failover state
│ │ │ ├── AuditLogger.java # Security audit logging
│ │ │ ├── SecurityManager.java # AES-256-GCM transport + PBKDF2-HMAC-SHA256 key derivation
│ │ │ └── VersionManager.java # Version management
│ │ ├── services/
│ │ │ ├── BLEService.java
│ │ │ └── SerialService.java
│ │ └── ui/
│ │ ├── AkitaToolbar.java
│ │ ├── AkitaOperationalReadiness.java
│ │ ├── AkitaProvisioningManager.java
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
3. Configure `firmware/src/config.h`: UUIDs, `PROVISIONING_SECRET`, and MQTT credentials if `ENABLE_MQTT` is enabled  
4. Placeholder BLE UUIDs, provisioning material, and MQTT credentials will fail the build unless `ALLOW_PLACEHOLDER_SECRET` is explicitly defined for bench-only rehearsal  
5. Build: ```pio run```
6. Upload: ```pio run -t upload```

---

## ATAK Plugin

The ATAK plugin is built using **Android Studio** or the Gradle wrapper from the command line.

1. Install Android Studio (or the Android command-line tools) and Android SDK (platform 35, build-tools 35.0.1)  
2. Place `atak-sdk.jar` in `atak_plugin/libs/`  
3. Open `atak_plugin/` in Android Studio — it will create `local.properties` automatically.  
   *Or* create `atak_plugin/local.properties` manually with your SDK path:  
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ```  
   *Or* set the `ANDROID_HOME` environment variable instead. `local.properties` is gitignored and machine-specific.  
4. Configure UUIDs & USB IDs in `atak_plugin/src/com/akitaengineering/meshtak/Config.java`; runtime provisioning from the settings UI is preferred over relying on the build-time fallback secret  
5. Build:  
   - **Android Studio:** Build → Build Bundle(s) / APK(s) → Build APK(s)  
   - **Command line:** `cd atak_plugin && ./gradlew assembleDebug` (or `.\gradlew.bat assembleDebug` on Windows)

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
- `CMD:MAILBOX:PUT:<messageId>:<format>:<payload>`  
Queues guaranteed-delivery mission traffic for relay by the firmware  
- `CMD:PROV:STAGE:<secret>`  
Stages runtime provisioning material to the connected device over a trusted local bearer  

## Firmware → ATAK
- `STATUS:BATT:XX%`  
Response to battery query (e.g., `STATUS:BATT:85%`)  
- `STATUS:VERSION:X.Y.Z`  
Response to version query (e.g., `STATUS:VERSION:0.2.0`)  
- `STATUS:MAILBOX:ACK:<messageId>:IN_FLIGHT|FAILED`  
Local acknowledgement that a mailbox frame was accepted for relay or failed locally  
- `STATUS:MAILBOX:ACK:<messageId>:DELIVERED:<peerNode>`  
Peer mailbox receipt confirming end-to-end delivery across the mesh  
- `STATUS:MAILBOX:RX:<originNode>:<messageId>:<format>:<payload>`  
Inbound mission traffic received from the mesh  
- `STATUS:PROV:STAGED:<version>:<key-id>` / `STATUS:PROV:FAILED:<version>:<key-id>`  
Runtime provisioning stage result returned by firmware  

All other received data is treated as **CoT XML**.

Protocol definitions exist in:

- `firmware/src/config.h`  
- `atak_plugin/src/com/akitaengineering/meshtak/Config.java`

Shared transport helpers live in `firmware/src/payload_codec.h/.cpp` and `firmware/src/mailbox_escape.h/.cpp` so BLE and Serial paths stay consistent.

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

**Copyright (C) 2026 Akita Engineering**


