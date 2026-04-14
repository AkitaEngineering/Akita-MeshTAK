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
- **Android Studio** (latest version) *or* the Android command-line tools  
- **Android SDK** (platform 35, build-tools 35.0.1)  
- **ATAK SDK** (`atak-sdk.jar` placed in `libs/`)  

## Build Instructions

### 1. Clone the Repository
```
git clone https://github.com/akitaengineering/AkitaMeshTAK.git
cd AkitaMeshTAK/atak_plugin
```

### 2. Set Up the Android SDK

**Option A — Android Studio (recommended)**  
Open the `atak_plugin` directory as an Android Studio project. Studio will create `local.properties` automatically with the correct `sdk.dir` path.

**Option B — Command line**  
Create `atak_plugin/local.properties` with the path to your Android SDK:
```properties
sdk.dir=/path/to/your/Android/Sdk
```
Or set the `ANDROID_HOME` environment variable instead:
```bash
export ANDROID_HOME=/path/to/your/Android/Sdk   # macOS / Linux
set ANDROID_HOME=C:\Users\you\AppData\Local\Android\Sdk   # Windows
```
`local.properties` is gitignored and never committed — each developer provides their own.

### 3. Configure the ATAK SDK
Place `atak-sdk.jar` in the `libs/` directory. Follow the official ATAK SDK documentation to obtain the JAR.

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

**Android Studio:**
```
Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

**Command line (Gradle):**
```bash
cd atak_plugin
./gradlew assembleDebug          # Linux / macOS
.\gradlew.bat assembleDebug      # Windows
```
The output APK will be in `build/outputs/apk/debug/`.

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

