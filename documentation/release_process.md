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

There are two checkpoints:

- **Software snapshot**: `main` is clean, CI is green, changelog/docs match `version.properties`, and `python3 tools/deployment_readiness_check.py` passes. This does **not** produce shippable firmware or a signed plugin.
- **Field shipment**: the software snapshot plus the external inputs below. Run `python3 tools/release_preflight.py` on the release host; it must pass before `heltec_v3` or `assembleRelease`.

Copy `.env.example` to a file **outside this checkout**, fill in real values, and `source` it for the release shell. Do not copy completed env files or keystores into the repository.

Release is a no-go unless every item below is satisfied:

1. The worktree is clean and `CHANGELOG.md` is updated for the target version.
2. CI passes on the exact commit being released:
   - `.github/workflows/ci.yml`
   - deployment and OpenTAKServer CoT static checks
   - firmware `heltec_v3_ci`
   - Android unit tests with ATAK stubs
   - Android lint with ATAK stubs
   - Android debug artifact assembly with ATAK stubs
3. Release inputs are available outside source control. Signing keystores are outside the checkout and readable only by the release operator.
4. The plugin builds against the official ATAK SDK, installs into the target ATAK version, and its signature is verified.
5. Production firmware builds without `ALLOW_PLACEHOLDER_SECRET`, `ALLOW_UNWIRED_MESH_BRIDGE`, or `ALLOW_INSECURE_MQTT` and includes explicit mesh UART pins.
6. BLE, USB serial, and the controller-to-Meshtastic UART are exercised on representative hardware, including mandatory encrypted traffic, physical-button provisioning rotation, fragmented mailbox acknowledgement, position-to-CoT, SOS, and reconnect behavior.
7. OpenTAKServer CoT interoperability is verified using `documentation/opentakserver_compatibility.md` when that integration is in deployment scope.
8. Operator and technical documentation are updated if behavior changed.
9. Rollback artifacts and provisioning-secret rollback handling are validated before field deployment.

## Firmware Release Inputs

Provide deployment values as environment variables before running `platformio`:

- `AKITA_DEVICE_ID`
- `AKITA_MESH_SERIAL_RX_PIN`
- `AKITA_MESH_SERIAL_TX_PIN`
- `AKITA_BLE_SERVICE_UUID`
- `AKITA_BLE_COT_CHARACTERISTIC_UUID`
- `AKITA_BLE_WRITE_CHARACTERISTIC_UUID`

MQTT must remain disabled in a production image until certificate-validated TLS is implemented. `AKITA_ALLOW_INSECURE_MQTT` is a bench-only override and is prohibited for release builds.

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

Release builds require Java 17 or 21, Android SDK platform 35/build-tools 36.0.0, the official ATAK SDK jar, and signing credentials.

Set either Gradle properties or matching environment variables for:

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
- `akitaOpenTakSsl` / `AKITA_OTS_SSL` (optional; if true, `akitaOpenTakClientP12` / `AKITA_OTS_CLIENT_P12` must point at a client PKCS#12)

Release plugin command:

```bash
cd atak_plugin
./gradlew --no-daemon assembleRelease
```

On POSIX release hosts, protect signing material before building:

```bash
chmod 600 "$AKITA_RELEASE_KEYSTORE_FILE"
```

The release build rejects keystores stored anywhere inside the source checkout.

Debug/unit-test command with ATAK stubs:

```bash
cd atak_plugin
./gradlew --no-daemon test -PakitaUseAtakStub=true
```

## Release Artifacts

### GitHub Actions APK downloads

The **CI** workflow runs on pull requests, pushes to `main`, and manual dispatch.
After tests and lint pass, download `AkitaMeshTAK-debug-stub-<run number>` from
the run's **Artifacts** section. It contains a debug-signed APK built with ATAK
stubs for development only; it cannot establish compatibility with the ATAK host.
Test and lint reports are uploaded separately, including when checks fail.

For an APK compiled against the official SDK, configure the `release` GitHub
environment and run **Build signed APK** manually on the intended branch or tag.
Configure these environment secrets:

- `AKITA_ATAK_SDK_URL`: HTTPS URL (optionally time-limited) downloading the official
  SDK's `main.jar` directly, not an SDK ZIP or HTML page.
- `AKITA_RELEASE_KEYSTORE_BASE64`: base64 encoding of your signing keystore.
- `AKITA_RELEASE_STORE_PASSWORD`, `AKITA_RELEASE_KEY_ALIAS`, and
  `AKITA_RELEASE_KEY_PASSWORD`: matching signing credentials.

Configure these environment variables:

- `AKITA_ATAK_SDK_SHA256`: independently verified SHA-256 of that `main.jar`.
- `AKITA_BLE_SERVICE_UUID`, `AKITA_BLE_COT_CHARACTERISTIC_UUID`, and
  `AKITA_BLE_WRITE_CHARACTERISTIC_UUID`: deployment UUIDs matching the controller.
- Optionally `AKITA_HELTEC_VENDOR_ID` and `AKITA_HELTEC_PRODUCT_ID` as decimal IDs.

The workflow checks the SDK checksum, tests using stubs, then runs release lint
and builds against the official SDK with release signing. It verifies the APK
signature and uploads `AkitaMeshTAK-release-<run number>` with `SHA256SUMS`.
SDK and signing files stay in the runner's temporary directory and are removed
afterward. Configure environment branch restrictions and reviewers to match your
release policy. This workflow builds the Android plugin; coordinated firmware
shipment and hardware acceptance still follow the preconditions above.

### Shipment contents

Produce and retain:

- Signed ATAK plugin release APK/AAB
- Firmware binary from `firmware/.pio/build/heltec_v3/`
- Audit of release inputs used
- Updated `CHANGELOG.md`
- SHA-256 checksums for every shipped artifact
- APK signer verification output and signer certificate fingerprint
- Hardware/interoperability acceptance record tied to the release commit

Example artifact verification commands:

```bash
sha256sum atak_plugin/build/outputs/apk/release/*.apk firmware/.pio/build/heltec_v3/firmware.bin
apksigner verify --verbose --print-certs atak_plugin/build/outputs/apk/release/*.apk
```

## Tagging

Use annotated Git tags matching the version number:

```bash
git tag -a v0.2.1 -m "Akita MeshTAK 0.2.1"
```

Tag only the commit that passed `tools/release_preflight.py` and hardware acceptance. GitHub source archives from the tag are not a substitute for the signed APK and `heltec_v3` firmware binary.

## Rollback

- Keep the last signed plugin artifact and the previous firmware binary.
- Do not rotate compatibility bounds in `version.properties` until rollback artifacts are validated.
- If provisioning material was rotated for the release, document the rollback secret handling separately before field deployment.
