# Akita MeshTAK ATAK Plugin

## Overview

The **Akita MeshTAK ATAK Plugin** is an Android application that integrates Meshtastic communication with the Android Tactical Assault Kit (ATAK).
It enables ATAK users to:

- Send and receive data, including CoT (Cursor on Target) messages
- Queue mission traffic into an encrypted, acknowledgement-tracked mailbox with replayable state
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
- Build-time `BuildConfig` values for non-secret UUID and USB identifiers
- Android-Keystore-protected per-device provisioning state; operational encryption is mandatory

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
export AKITA_ATAK_SDK_JAR=/absolute/path/to/main.jar
```

or

```bash
./gradlew assembleRelease -PakitaAtakSdkJar=/absolute/path/to/main.jar
```

If you have the extracted ATAK CIV SDK directory instead of a standalone jar, you can point the same setting at that directory and the build will resolve `main.jar` automatically:

```bash
export AKITA_ATAK_SDK_JAR=/absolute/path/to/atak-civ/
```

The current target public SDK baseline for this repo is `ATAK-CIV-5.6.0.12-SDK`.

If the jar is absent, debug/unit-test builds automatically use compile-time ATAK stubs instead.

### 4. Supply Deployment Build Inputs
Release builds must receive deployment values without editing source files. Supported inputs are Gradle properties or environment variables:

- BLE service UUID: `akitaBleServiceUuid` / `AKITA_BLE_SERVICE_UUID`
- BLE CoT characteristic UUID: `akitaCotCharacteristicUuid` / `AKITA_BLE_COT_CHARACTERISTIC_UUID`
- BLE write characteristic UUID: `akitaWriteCharacteristicUuid` / `AKITA_BLE_WRITE_CHARACTERISTIC_UUID`
- USB vendor/product IDs: `akitaHeltecVendorId`, `akitaHeltecProductId` / `AKITA_HELTEC_VENDOR_ID`, `AKITA_HELTEC_PRODUCT_ID`
- Release signing material: `akitaReleaseKeystoreFile`, `akitaReleaseStorePassword`, `akitaReleaseKeyAlias`, `akitaReleaseKeyPassword`

No provisioning secret is compiled into the APK. Generate or import a per-device secret in settings, boot the controller while holding its provisioning button, and stage the secret within the two-minute physical-presence window.

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
export AKITA_BLE_SERVICE_UUID=...deployment uuid...
export AKITA_BLE_COT_CHARACTERISTIC_UUID=...deployment uuid...
export AKITA_BLE_WRITE_CHARACTERISTIC_UUID=...deployment uuid...
export AKITA_ATAK_SDK_JAR=/absolute/path/to/atak-sdk.jar
export AKITA_RELEASE_KEYSTORE_FILE=/secure/path/outside/checkout/release.keystore
export AKITA_RELEASE_STORE_PASSWORD=...store password...
export AKITA_RELEASE_KEY_ALIAS=...alias...
export AKITA_RELEASE_KEY_PASSWORD=...key password...
./gradlew assembleRelease
```

Before building, restrict the keystore to its owner on POSIX hosts: `chmod 600 "$AKITA_RELEASE_KEYSTORE_FILE"`. Never copy a signing key into this repository, even temporarily.

The release build enforces a real ATAK SDK input, valid non-placeholder BLE UUIDs, complete signing credentials, and a keystore located outside the source checkout. Per-device secrets are created after installation and never embedded in the release artifact.

The signed release APK will be located in:

```
build/outputs/apk/release/
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

