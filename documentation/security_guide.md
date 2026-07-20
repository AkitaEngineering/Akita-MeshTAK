# Security Guide for Akita MeshTAK

## Overview
This document outlines the security features implemented in Akita MeshTAK for military, law enforcement, search and rescue, and security operations.

The current Android plugin security model is surfaced directly to operators through the settings UI, mission-assurance dashboard, and runtime provisioning ceremony controls so teams can verify provisioning, encrypted transport posture, audit readiness, and interoperability before live traffic is transmitted.

---

## Security Features

### 1. Encryption
- **AES-256-GCM Encryption**: Sensitive BLE/Serial communications are encrypted using authenticated encryption
- **Authenticated Integrity**: AES-GCM authentication tag verification is required during decryption
- **PBKDF2 Key Derivation**: AES/HMAC transport keys are derived with PBKDF2-HMAC-SHA256 (100,000 iterations) from provisioning material using device/purpose salt
- **Secure Key Management**: Keys should be provisioned securely (NOT hardcoded)
- **Versioned Envelope**: Encrypted payloads use `ENC:v2:k1:<epoch>:<nonce>:<ciphertext-hex>:<hmac-hex>` format for protocol versioning and key-id rotation

#### Encryption Activation (Current Behavior)
- **Firmware Default**: Encryption is enabled by default (`SECURITY_MODE_AES256_HMAC`). The firmware encrypts/decrypts all BLE and serial payloads when a valid provisioning secret is configured.
- **Android Plugin Policy**: Operational encryption is mandatory. Missing keys or cryptographic errors block transmission instead of falling back to plaintext.
- **Provisioning Source**: The active per-device secret is read only from Android-Keystore-protected runtime state. No fallback secret is embedded in the APK.
- **Local Secret Storage**: Custom provisioning secrets and staged offline bundles are stored in a no-backup file encrypted with Android Keystore AES-GCM on device. JVM/Robolectric tests use a process-local fallback key only for test execution.
- **Provisioning Ceremony**: Plaintext staging is accepted only during the two-minute window opened by holding the controller's physical provisioning button during boot. A successful secret is persisted in ESP32 NVS and the response is encrypted with the new key.
- **Readiness Warning**: An unprovisioned plugin or controller is a no-transmit state.
- **Enablement Requirement**: Firmware and plugin must use matching provisioning secret, version, and key-id metadata.
- **Behavior**: Encrypted traffic uses AES-GCM with per-message nonce and authentication tag; malformed or mismatched encrypted envelopes are rejected.

#### Implementation
- Firmware: `firmware/src/security.h` and `security.cpp`
- Android Plugin: `atak_plugin/src/com/akitaengineering/meshtak/SecurityManager.java`

#### Key Provisioning
**CRITICAL**: In production, encryption keys MUST be provisioned securely:
- Use Android Keystore and ESP32 NVS; enable ESP32 flash encryption, secure boot, and anti-rollback on field hardware
- Implement key rotation policies
- Never hardcode keys in source code
- Use secure key exchange protocols
- Rotate key-id values in controlled deployments (for example, `k1` -> `k2`) and update both firmware/plugin configuration together.

#### Key Provisioning Workflow

Follow these steps to enable end-to-end encryption:

1. **Generate a Provisioning Secret**
   - Create a strong random secret (32+ characters): `openssl rand -hex 32`
   - This secret will be shared between firmware and plugin.
   - If the secret will move offline, generate an air-gapped provisioning bundle from the plugin after the secret is loaded.

2. **Configure Firmware**
   - Do not compile the generated secret into firmware.
   - Replace the placeholder BLE UUIDs with deployment values that match the ATAK plugin.
   - Keep MQTT disabled for production. The current plaintext implementation is bench-only and requires the explicit `ALLOW_INSECURE_MQTT` override.
   - The firmware initializes transport security with `SECURITY_MODE_AES256_HMAC` after valid provisioning material is loaded.
   - Production builds fail when BLE UUIDs or companion UART pins are not configured.
   - Build and flash the firmware.
   - Hold the physical provisioning button during boot and stage the secret within two minutes.

3. **Configure Android Plugin**
   - Preferred method: open **Settings → Tool Preferences → Akita MeshTAK → Security and Provisioning**.
   - Enter the deployment secret in **Provisioning Secret**.
   - Use **Generate Provisioning Bundle** to create an offline bundle when another operator or device needs the same material.
   - Use **Apply Provisioning Bundle** to load staged bundle material into the plugin.
   - Use **Stage Secret To Connected Device** only while physically controlling the device and its boot-time provisioning window is open.
   - Tap **Reload Security State** after security changes.

4. **Verify Encryption**
   - Review the **Mission Assurance** card for encryption, audit, interoperability, and provisioning status.
   - Check audit logs for security initialization and data send/receive events.
   - Verify encrypted payloads use the `ENC:v2:k1:<epoch>:<nonce>:<ciphertext-hex>:<hmac-hex>` format.
   - Confirm both sides can decrypt each other's messages.

5. **Key Rotation**
   - To rotate keys, change the provisioning secret on both firmware and plugin simultaneously.
   - The plugin can generate a new runtime secret using **Rotate Provisioning Secret**, package it with **Generate Provisioning Bundle**, and apply it offline with **Apply Provisioning Bundle**.
   - Use **Stage Secret To Connected Device** during a trusted local ceremony so firmware adopts the same secret before deployment.
   - The current protocol uses key-id `k1`; a future protocol revision is required before overlapping-key rotation is supported.
   - Tap **Reload Security State** or restart the plugin after the change.

### 2. Input Validation
- **Command Validation**: All incoming commands are validated before processing
- **Injection Prevention**: Protection against code injection attacks
- **Length Limits**: Maximum message/command lengths enforced
- **Character Sanitization**: Dangerous characters are filtered
- **Transport Throttling**: BLE and Serial command handlers enforce a minimum acceptance interval to reduce command-flood abuse

#### Implementation
- Firmware: `firmware/src/input_validation.h` and `input_validation.cpp`
- Android Plugin: `SecurityManager.validateInput()`

### 3. Audit Logging
- **Comprehensive Logging**: All security-relevant events are logged
- **Event Types**: Connections, disconnections, commands, data transfers, security violations
- **Severity Levels**: Info, Warning, Error, Critical
- **Diagnostic Scope**: Redacted, bounded runtime events for troubleshooting; this is not a durable evidentiary audit trail

#### Implementation
- Firmware: `firmware/src/audit_log.h` and `audit_log.cpp`
- Android Plugin: `atak_plugin/src/com/akitaengineering/meshtak/AuditLogger.java`

#### Logged Events
- Connection/disconnection events
- Command execution
- Data transmission/reception
- Security violations
- Authentication failures
- Integrity failures
- SOS triggers (CRITICAL)
- Configuration changes
- Errors

Firmware audit entries are redacted and kept in a bounded 128-entry in-memory ring. They reset on reboot and are mirrored to the serial console only when `DEBUG_AUDIT` is defined. Use an authenticated external logging system if durable evidentiary retention is required.

#### Operator Actions
- **Export Audit Log** is available from **Settings → Tool Preferences → Akita MeshTAK → Security and Provisioning**.
- Exported logs should be handled according to mission retention and evidence procedures.
- Audit export should be included in post-mission or post-exercise actions when required by SOP.

### 4. Message Integrity
- **AEAD Verification**: AES-GCM tag verification provides built-in integrity protection
- **Tamper Detection**: Invalid tags or malformed encrypted envelopes are rejected and logged
- **Replay Protection**: Authenticated v2 timestamps and nonces reject duplicates within the active process/device session. The cache resets on restart, so deployments requiring restart-resistant replay defense must add persistent monotonic peer counters.

### 5. Authentication
- **Device Authentication**: Device IDs validated
- **Token-Based Auth**: Authentication tokens for secure operations
- **Connection Validation**: All connections are validated and logged

### 6. Error Handling
- **Robust Error Recovery**: Graceful handling of errors
- **Connection Retry Logic**: Automatic reconnection with exponential backoff
- **Timeout Protection**: Connection timeouts prevent hanging
- **Resource Cleanup**: Proper cleanup on errors

---

## Security Best Practices

### For Developers

1. **Never Hardcode Credentials**
   - WiFi passwords
   - MQTT credentials
   - Encryption keys
   - API keys
   - If a rehearsal build must keep placeholders, gate it explicitly with `ALLOW_PLACEHOLDER_SECRET` and never field that image

2. **Use Secure Storage**
   - Android Keystore for Android
   - ESP32 NVS plus platform flash encryption for firmware

3. **Validate All Inputs**
   - Always validate user input
   - Check command formats
   - Verify data lengths
   - Sanitize strings

4. **Enable Audit Logging**
   - Never disable audit logging in production
   - Export logs regularly
   - Monitor for security violations

5. **Keep Dependencies Updated**
   - Regularly update security libraries
   - Patch known vulnerabilities
   - Monitor security advisories

### For Operators

1. **Secure Key Management**
   - Use strong, deployment-specific provisioning secrets
   - Replace placeholder secrets before live use
   - Rotate secrets periodically and synchronize firmware/plugin updates
   - Use air-gapped bundles for offline transfer and stage secrets only over trusted local bearers

2. **Monitor Audit Logs**
   - Review logs regularly
   - Investigate security violations
   - Export logs for compliance

3. **Use Mission Assurance**
   - Confirm provisioning status is ready for deployment
   - Confirm encrypted transport remains enabled for production operations
   - Confirm interoperability and audit signals are healthy before mission release

4. **Network Security**
   - Use encrypted WiFi (WPA3 if available)
   - Do not field the current MQTT implementation. Production MQTT requires certificate-validated TLS; `ALLOW_INSECURE_MQTT` is limited to isolated bench testing.
   - Isolate networks when possible

5. **Physical Security**
   - Secure devices physically
   - Protect against tampering
   - Use tamper-evident seals

6. **Access Control**
   - Limit who can configure devices
   - Use strong passwords
   - Implement role-based access control

---

## Security Configuration

### Firmware Configuration

In `firmware/src/config.h`:
- Set secure UUIDs (not default values)
- Replace placeholder provisioning material and, when MQTT is enabled, replace placeholder Wi-Fi/MQTT credentials
- Configure security mode and encrypted envelope metadata
- Set maximum message lengths
- Set `CMD_RATE_LIMIT_MS` if transport throttling requires controlled tuning

In firmware build flags:
- Define `DEBUG_AUDIT` only for laboratory or troubleshooting builds that need serial audit mirroring
- Define `ALLOW_PLACEHOLDER_SECRET` only for bench rehearsal when placeholder values are intentionally retained

### Android Plugin Configuration

- Custom provisioning secrets and staged bundles are no longer persisted as plain SharedPreferences values.
- The plugin stores that material in an encrypted no-backup state file protected by an Android Keystore AES key.
- Preference keys are retained only as non-sensitive UI/service refresh signals.

In the plugin settings UI:
- Configure or rotate **Provisioning Secret**
- Generate or apply **Air-Gapped Provisioning Bundle** material as required
- Use **Stage Secret To Connected Device** only during controlled provisioning ceremonies
- Export audit logs as required
- Reload security state after changes

For the Gradle release build:
- Supply matching UUIDs with `AKITA_BLE_SERVICE_UUID`, `AKITA_BLE_COT_CHARACTERISTIC_UUID`, and `AKITA_BLE_WRITE_CHARACTERISTIC_UUID`
- Never place provisioning secrets in Gradle inputs or release artifacts
- Keep all deployment inputs and signing credentials outside source control

---

## Security Violations

The system logs the following as security violations:
- Invalid command formats
- Injection pattern detection
- AES-GCM authentication failures
- Decryption failures
- Unauthorized access attempts
- Data length violations

All security violations are logged with severity level WARNING or ERROR.

---

## Compliance

### Military/Law Enforcement Requirements

The system is designed to meet requirements for:
- **Accountability**: Bounded diagnostic audit events; use an authenticated external logging system when evidentiary retention is required
- **Integrity**: Message integrity verification
- **Confidentiality**: Encryption of sensitive data
- **Availability**: Robust error handling and recovery

### Audit Log Retention

- In-memory: Up to 1000 entries (firmware)
- In-memory: Up to 10,000 entries (Android)
- File export: Available for long-term storage
- Logs should be exported and archived regularly

---

## Reporting Security Issues

If you discover a security vulnerability:
1. **DO NOT** create a public issue
2. Contact: security@akitaengineering.com
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

---

## Version History

- **Unreleased**: Runtime provisioning and mission-assurance hardening
   - Android Keystore-backed runtime provisioning state with no build-time secret fallback
   - Mandatory fail-closed encrypted operational transport
   - Air-gapped provisioning bundle generation/apply and trusted local stage-to-device workflow
   - Audit export and security reload actions added to settings
   - Mission Assurance flags placeholder provisioning and degraded posture
- **v0.2.0**: Initial security implementation
   - AES-256-GCM encrypted transport with authenticated integrity
   - Versioned/key-id encrypted envelope format (`ENC:v2:k1:<epoch>:<nonce>:<ciphertext-hex>:<hmac-hex>`)
   - Firmware encryption enabled by default; original plugin workflow required explicit opt-in after provisioning
   - Input validation
   - Audit logging
   - Constant-time HMAC comparison (timing attack prevention)
   - Error handling improvements

---

## References

- NIST Cybersecurity Framework
- OWASP Mobile Security
- Android Security Best Practices
- ESP32 Security Guidelines

---

**Copyright (C) 2026 Akita Engineering**

