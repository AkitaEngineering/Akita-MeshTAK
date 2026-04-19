# Akita MeshTAK Release Process

## Purpose

This document defines the release and versioning process for coordinated firmware and ATAK plugin shipments.

## Version Source of Truth

- Update `version.properties` for every coordinated release.
- `VERSION_NAME` is the operator-facing semantic version.
- `VERSION_CODE` is the Android monotonically increasing release integer.
- `MIN_FIRMWARE_VERSION` and `MAX_FIRMWARE_VERSION` define plugin compatibility gates.
- `ATAK_SDK_VERSION` records the expected official ATAK SDK contract for release builds.

## Release Preconditions

1. `CHANGELOG.md` is updated for the target version.
2. CI passes:
   - `.github/workflows/ci.yml`
   - firmware `heltec_v3_ci`
   - Android unit tests with ATAK stubs
3. Release inputs are available outside source control.
4. Operator and technical documentation are updated if behavior changed.

## Firmware Release Inputs

Provide deployment values as environment variables before running `platformio`:

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

Release firmware command:

```bash
cd firmware
platformio run -e heltec_v3
```

CI firmware command:

```bash
cd firmware
platformio run -e heltec_v3_ci
```

## ATAK Plugin Release Inputs

Release builds require Java 17 or 21, Android SDK platform 35/build-tools 35.0.1, the official ATAK SDK jar, and signing credentials.

Set either Gradle properties or matching environment variables for:

- `akitaProvisioningSecret` / `AKITA_PROVISIONING_SECRET`
- `akitaBleServiceUuid` / `AKITA_BLE_SERVICE_UUID`
- `akitaCotCharacteristicUuid` / `AKITA_BLE_COT_CHARACTERISTIC_UUID`
- `akitaWriteCharacteristicUuid` / `AKITA_BLE_WRITE_CHARACTERISTIC_UUID`
- `akitaHeltecVendorId` / `AKITA_HELTEC_VENDOR_ID`
- `akitaHeltecProductId` / `AKITA_HELTEC_PRODUCT_ID`
- `akitaAtakSdkJar` / `AKITA_ATAK_SDK_JAR`
- `akitaReleaseKeystoreFile` / `AKITA_RELEASE_KEYSTORE_FILE`
- `akitaReleaseStorePassword` / `AKITA_RELEASE_STORE_PASSWORD`
- `akitaReleaseKeyAlias` / `AKITA_RELEASE_KEY_ALIAS`
- `akitaReleaseKeyPassword` / `AKITA_RELEASE_KEY_PASSWORD`

Release plugin command:

```bash
cd atak_plugin
./gradlew --no-daemon assembleRelease
```

Debug/unit-test command with ATAK stubs:

```bash
cd atak_plugin
./gradlew --no-daemon test -PakitaUseAtakStub=true
```

## Release Artifacts

Produce and retain:

- Signed ATAK plugin release APK/AAB
- Firmware binary from `firmware/.pio/build/heltec_v3/`
- Audit of release inputs used
- Updated `CHANGELOG.md`

## Tagging

Use annotated Git tags matching the version number:

```bash
git tag -a v0.2.0 -m "Akita MeshTAK 0.2.0"
```

## Rollback

- Keep the last signed plugin artifact and the previous firmware binary.
- Do not rotate compatibility bounds in `version.properties` until rollback artifacts are validated.
- If provisioning material was rotated for the release, document the rollback secret handling separately before field deployment.
