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
- **Versioned Envelope**: Encrypted payloads use `ENC:v1:k1:<hex>` format for protocol versioning and key-id rotation

#### Encryption Activation (Current Behavior)
- **Firmware Default**: Encryption is enabled by default (`SECURITY_MODE_AES256_HMAC`). The firmware encrypts/decrypts all BLE and serial payloads when a valid provisioning secret is configured.
- **Android Plugin Default**: The plugin reads `security_encryption_enabled` from settings and treats encrypted transport as enabled unless an operator explicitly disables it.
- **Provisioning Source**: The active provisioning secret is read from plugin settings when present; `Config.PROVISIONING_SECRET` is used only as a fallback.
- **Provisioning Ceremony**: The plugin can generate/apply air-gapped bundles and send a plaintext stage-to-device command over a trusted local bearer so firmware can adopt new provisioning material without a rebuild.
- **Readiness Warning**: Placeholder secrets can support rehearsal and UI preview, but Mission Assurance will flag that posture as not deployment-ready.
- **Enablement Requirement**: Firmware and plugin must use matching provisioning secret, version, and key-id metadata.
- **Behavior**: Encrypted traffic uses AES-GCM with per-message nonce and authentication tag; malformed or mismatched encrypted envelopes are rejected.

#### Implementation
- Firmware: `firmware/src/security.h` and `security.cpp`
- Android Plugin: `atak_plugin/src/com/akitaengineering/meshtak/SecurityManager.java`

#### Key Provisioning
**CRITICAL**: In production, encryption keys MUST be provisioned securely:
- Use secure key storage (Android Keystore, ESP32 NVS with encryption)
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
   - In `firmware/src/config.h`, replace the default `PROVISIONING_SECRET` with your generated secret.
   - Replace the placeholder BLE UUIDs with deployment values that match the ATAK plugin.
   - If MQTT is enabled, replace the placeholder Wi-Fi and MQTT credentials.
   - The firmware initializes transport security with `SECURITY_MODE_AES256_HMAC` after valid provisioning material is loaded.
   - The firmware build now fails if placeholder provisioning, BLE UUID, or enabled MQTT credential values remain in place unless `ALLOW_PLACEHOLDER_SECRET` is explicitly defined for bench rehearsal.
   - Build and flash the firmware.
   - For trusted local runtime rotation, you can also stage the secret later with **Stage Secret To Connected Device** instead of rebuilding immediately.

3. **Configure Android Plugin**
   - Preferred method: open **Settings → Tool Preferences → Akita MeshTAK → Security and Provisioning**.
   - Enter the deployment secret in **Provisioning Secret**.
   - Confirm **Enable Encrypted Transport** is enabled.
   - Use **Generate Provisioning Bundle** to create an offline bundle when another operator or device needs the same material.
   - Use **Apply Provisioning Bundle** to load staged bundle material into the plugin.
   - Use **Stage Secret To Connected Device** only on a trusted local bearer when runtime-provisioning firmware.
   - Tap **Reload Security State** after security changes.
   - If a fixed build-time fallback is required, set `atak_plugin/src/com/akitaengineering/meshtak/Config.java` `PROVISIONING_SECRET` to the same value.

4. **Verify Encryption**
   - Review the **Mission Assurance** card for encryption, audit, interoperability, and provisioning status.
   - Check audit logs for security initialization and data send/receive events.
   - Verify encrypted payloads use the `ENC:v1:k1:<hex>` format.
   - Confirm both sides can decrypt each other's messages.

5. **Key Rotation**
   - To rotate keys, change the provisioning secret on both firmware and plugin simultaneously.
   - The plugin can generate a new runtime secret using **Rotate Provisioning Secret**, package it with **Generate Provisioning Bundle**, and apply it offline with **Apply Provisioning Bundle**.
   - Use **Stage Secret To Connected Device** during a trusted local ceremony so firmware adopts the same secret before deployment.
   - Update the key-id (e.g., `k1` → `k2`) to distinguish new keys from old ones.
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
- **Accountability**: Full audit trail for compliance and forensics

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

Firmware audit entries are only mirrored to the serial console when `DEBUG_AUDIT` is defined. The circular in-memory audit log remains active regardless.

#### Operator Actions
- **Export Audit Log** is available from **Settings → Tool Preferences → Akita MeshTAK → Security and Provisioning**.
- Exported logs should be handled according to mission retention and evidence procedures.
- Audit export should be included in post-mission or post-exercise actions when required by SOP.

### 4. Message Integrity
- **AEAD Verification**: AES-GCM tag verification provides built-in integrity protection
- **Tamper Detection**: Invalid tags or malformed encrypted envelopes are rejected and logged
- **Replay Protection**: Timestamps and nonces prevent replay attacks (to be enhanced)

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
   - ESP32 NVS with encryption for firmware

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
   - Use TLS for MQTT (if enabled)
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

In the plugin settings UI:
- Configure **Enable Encrypted Transport**
- Configure or rotate **Provisioning Secret**
- Generate or apply **Air-Gapped Provisioning Bundle** material as required
- Use **Stage Secret To Connected Device** only during controlled provisioning ceremonies
- Export audit logs as required
- Reload security state after changes

In `atak_plugin/src/com/akitaengineering/meshtak/Config.java`:
- Set matching UUIDs
- Maintain a valid build-time fallback provisioning secret only when required; runtime provisioning from settings is preferred
- Set validation parameters

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
- **Accountability**: Full audit trail
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

- **v0.3.0**: Runtime provisioning and mission-assurance update
   - Preference-backed provisioning secret with build-time fallback
   - Encrypted transport policy surfaced in settings
   - Air-gapped provisioning bundle generation/apply and trusted local stage-to-device workflow
   - Audit export and security reload actions added to settings
   - Mission Assurance flags placeholder provisioning and degraded posture
- **v0.2.0**: Initial security implementation
   - AES-256-GCM encrypted transport with authenticated integrity
   - Versioned/key-id encrypted envelope format (`ENC:v1:k1:<hex>`)
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

