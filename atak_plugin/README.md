# Akita MeshTAK ATAK Plugin

## Overview

The **Akita MeshTAK ATAK Plugin** is an Android application that integrates Meshtastic communication with the Android Tactical Assault Kit (ATAK).  
It enables ATAK users to:

- Send and receive data, including CoT (Cursor on Target) messages  
- Send critical alerts  
- Monitor device health  

**CRITICAL:** The plugin **must** be configured before compilation.

---

# Features

## Connectivity
- Bluetooth Low Energy (BLE)  
- Serial (USB)

## Data Exchange
- Sends and receives CoT messages  
- Supports sending arbitrary data

## Tactical UI
- SOS/Critical Alert button on the toolbar  
- Device battery status displayed on the toolbar  
- Displays BLE/Serial connection status in the toolbar and on the map  
- Includes a dedicated view for sending data

## Configuration
- Central `Config.java` for UUIDs and USB IDs  
- In-app settings for runtime preferences (connection method, device name)

---

# Building the Plugin

To build the Akita MeshTAK ATAK Plugin, you need **Android Studio** and the **ATAK SDK**.

## Prerequisites
- Android Studio (latest version)  
- Android SDK  
- ATAK SDK  

## Build Instructions

### 1. Clone the Repository
```
git clone https://github.com/akitaengineering/AkitaMeshTAK.git
cd AkitaMeshTAK/atak_plugin
```

### 2. Open in Android Studio
Open the `atak_plugin` directory as an Android Studio project.

### 3. Configure the ATAK SDK
Follow the official ATAK SDK documentation to configure the SDK as a library module.

### 4. CRITICAL CONFIGURATION STEP
Open:

```
atak_plugin/src/com/akitaengineering/meshtak/Config.java
```

Replace **all placeholder UUIDs and USB IDs** with the exact values from:

- Your firmware: `firmware/src/config.h`  
- Your hardware specifications  

Incorrect values **will prevent the plugin from connecting**.

### 5. Build the APK
In Android Studio:
```
Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

The output APK (e.g., `app-debug.apk`) will be located in:

```
app/build/outputs/apk/debug/
```

---

# Installation

## 1. Locate the APK
Find the compiled APK file from the build output directory.

## 2. Install on Android Device
- Enable **Unknown sources** on your device  
- Copy the APK to your device  
- Install it normally

## 3. Enable in ATAK
Open ATAK → Plugin Manager → Enable **Akita MeshTAK**

---

# Contributing

See `documentation/dev_guide.md` for contribution guidelines.

---

# License

This project is licensed under the **GNU General Public License v3.0**.  
See the `LICENSE` and `COPYING` files in the root directory.

**Copyright (C) 2025 Akita Engineering**

