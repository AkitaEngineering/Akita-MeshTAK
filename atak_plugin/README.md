# Akita MeshTAK ATAK Plugin

## Overview

The **Akita MeshTAK ATAK Plugin** is an Android application that integrates Meshtastic communication with the Android Tactical Assault Kit (ATAK).  
It enables ATAK users to:

- Send and receive data, including CoT (Cursor on Target) messages  
- Queue mission traffic into a guaranteed-delivery mailbox with replayable acknowledgement state  
- Send critical alerts  
- Monitor device health and provisioning posture  

**CRITICAL:** Release builds must be configured before compilation. Debug/unit-test builds can use the ATAK stub path automatically when the official ATAK SDK jar is absent.

---

# Features

## Connectivity
- Bluetooth Low Energy (BLE)  
- Serial (USB)

## Data Exchange
- Sends and receives CoT messages  
- Supports arbitrary mission payloads with mailbox delivery tracking and peer receipts

## Tactical UI
- SOS/Critical Alert button on the toolbar  
- Device battery status displayed on the toolbar  
- Displays BLE/Serial connection status in the toolbar and on the map  
- Includes a dedicated view for sending data, mission replay, and provisioning actions
- Mission Assurance readiness indicators for encryption, audit, interoperability, and placeholder-secret posture

## Configuration
- Build-time `BuildConfig` values for provisioning secret, UUIDs, and USB IDs  
- In-app settings for runtime preferences, provisioning secret management, and encrypted transport policy

---

# Building the Plugin

To build the Akita MeshTAK ATAK Plugin, you need **Android Studio** or the Android command-line tools. Release packaging also needs the **official ATAK SDK**.

## Prerequisites
- **Android Studio** (latest version) *or* the Android command-line tools  
- **Android SDK** (platform 35, build-tools 35.0.1)  
- **Java 17 or Java 21** for Gradle/Android builds  
- **ATAK SDK** for release builds only. See `ATAK_SDK_REQUIREMENTS.md`.  

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

### 3. Configure the ATAK SDK Contract
Release builds require the official ATAK SDK jar. Supply its path with one of the following:

```bash
export AKITA_ATAK_SDK_JAR=/absolute/path/to/atak-sdk.jar
```

or

```bash
./gradlew assembleRelease -PakitaAtakSdkJar=/absolute/path/to/atak-sdk.jar
```

If the jar is absent, debug/unit-test builds automatically use compile-time ATAK stubs instead.

### 4. Supply Deployment Build Inputs
Release builds must receive deployment values without editing source files. Supported inputs are Gradle properties or environment variables:

- Provisioning secret: `akitaProvisioningSecret` / `AKITA_PROVISIONING_SECRET`
- BLE service UUID: `akitaBleServiceUuid` / `AKITA_BLE_SERVICE_UUID`
- BLE CoT characteristic UUID: `akitaCotCharacteristicUuid` / `AKITA_BLE_COT_CHARACTERISTIC_UUID`
- BLE write characteristic UUID: `akitaWriteCharacteristicUuid` / `AKITA_BLE_WRITE_CHARACTERISTIC_UUID`
- USB vendor/product IDs: `akitaHeltecVendorId`, `akitaHeltecProductId` / `AKITA_HELTEC_VENDOR_ID`, `AKITA_HELTEC_PRODUCT_ID`
- Release signing material: `akitaReleaseKeystoreFile`, `akitaReleaseStorePassword`, `akitaReleaseKeyAlias`, `akitaReleaseKeyPassword`

Runtime provisioning from the settings UI remains preferred over relying on the build-time fallback secret.

### 5. Build the APK

**Android Studio:**
```
Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

**Command line (Gradle):**
```bash
cd atak_plugin
./gradlew test -PakitaUseAtakStub=true
./gradlew assembleDebug          # Linux / macOS
.\gradlew.bat assembleDebug      # Windows
```
The output APK will be in `build/outputs/apk/debug/`.

### 6. Build a Signed Release APK

```bash
cd atak_plugin
export AKITA_PROVISIONING_SECRET=...deployment secret...
export AKITA_BLE_SERVICE_UUID=...deployment uuid...
export AKITA_BLE_COT_CHARACTERISTIC_UUID=...deployment uuid...
export AKITA_BLE_WRITE_CHARACTERISTIC_UUID=...deployment uuid...
export AKITA_ATAK_SDK_JAR=/absolute/path/to/atak-sdk.jar
export AKITA_RELEASE_KEYSTORE_FILE=/absolute/path/to/release.keystore
export AKITA_RELEASE_STORE_PASSWORD=...store password...
export AKITA_RELEASE_KEY_ALIAS=...alias...
export AKITA_RELEASE_KEY_PASSWORD=...key password...
./gradlew assembleRelease
```

The release build enforces real ATAK SDK input, non-placeholder provisioning/UUID values, and signing material.

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

**Copyright (C) 2026 Akita Engineering**

