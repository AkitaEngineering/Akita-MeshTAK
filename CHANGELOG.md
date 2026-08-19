# Changelog

All notable changes to this project are documented in this file.

The format follows Keep a Changelog and the project uses semantic versioning for coordinated firmware/plugin releases.

## [Unreleased]

### Changed
- ATAK plugin builds now use Android Gradle Plugin 9.3.1, Gradle 9.7.0, and build-tools 36.0.0.

## [0.2.1] - 2026-08-17

### Added
- Overlapping `k1`/`k2` key slots so a rotated secret remains readable until the next rotation.
- Persistent replay defense for authenticated envelopes on both the plugin and controller.
- Controller security-state command reporting active key-id, previous key-id, flash encryption, and secure boot.
- Mission Assurance now surfaces flash-encryption posture and the active key-id.

### Fixed
- Gradle wrapper URL/SHA mismatch that pointed at Gradle 8.9 while still hashing Gradle 8.7.
- Android secure provisioning storage now rejects non-file state targets, preserves legacy secrets when migration cannot persist, and surfaces write failures to callers.
- Firmware builds now exact-pin PlatformIO, the ESP32 platform, and library versions instead of resolving mutable upstream dependencies.
- Signing-key formats are ignored, release keystores are rejected from inside the checkout, and local signing material was moved out of the repository.
- CI now runs deployment/CoT checks and assembles a debug plugin artifact in addition to unit tests and the firmware build.
- Dependabot now monitors GitHub Actions and Android Gradle dependencies weekly.
- Android BLE calls now fail safely when runtime permissions are absent, USB receivers declare export posture on Android 13+, and lint is enforced in CI.
- Plaintext MQTT is fail-closed for production and requires an explicit isolated-bench override.
- The firmware provisioning placeholder assertion now compares the resolved secret value directly instead of stringifying an already quoted macro.
- Firmware provisioning now refuses to initialize security when PBKDF2 key derivation fails or produces zeroed key material.
- Firmware HMAC generation now fails closed instead of leaving callers with undefined output on mbedTLS setup errors.
- Firmware release guards now reject placeholder BLE characteristic UUIDs and MQTT deployment credentials, not just the primary service UUID/Wi-Fi SSID.
- Android provisioning-state writes now fail visibly instead of silently dropping secure-state updates when encrypted storage is unavailable.
- Legacy plugin provisioning secrets/bundles now remain intact if secure-state migration cannot complete.

## [0.2.0] - 2026-04-18

### Added
- Mission mailbox replay, bearer failover, and provisioning rehearsal workflows.
- Firmware CI environment `heltec_v3_ci` for reproducible placeholder-safe builds.
- ATAK stub compile path for Android debug/unit-test builds when the official ATAK SDK jar is unavailable.
- Centralized release metadata in `version.properties`.

### Changed
- Android build inputs now come from Gradle properties or environment variables instead of source edits.
- Firmware build inputs can now be injected from environment variables via PlatformIO pre-build configuration.
- Durable mailbox and replay state moved from SharedPreferences payload blobs to an atomic file-backed store.
- Release APKs now require signing material and enable shrinking/obfuscation.

### Fixed
- Firmware PBKDF2 derivation now uses the mbedTLS API available in the current PlatformIO toolchain.
- Removed committed machine-local Android SDK configuration from source control.
