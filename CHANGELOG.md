# Changelog

All notable changes to this project are documented in this file.

The format follows Keep a Changelog and the project uses semantic versioning for coordinated firmware/plugin releases.

## [Unreleased]

### Fixed
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
