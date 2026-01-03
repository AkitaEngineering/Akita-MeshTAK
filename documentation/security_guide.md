# Security Guide for Akita MeshTAK

## Overview
This document outlines the security features implemented in Akita MeshTAK for military, law enforcement, search and rescue, and security operations.

---

## Security Features

### 1. Encryption
- **AES-256-CBC Encryption**: All sensitive communications are encrypted using AES-256
- **HMAC-SHA256**: Message integrity verification using HMAC-SHA256
- **Secure Key Management**: Keys should be provisioned securely (NOT hardcoded)

#### Encryption Activation (Current Behavior)
- **Default State**: Encryption is **disabled by default** to maintain compatibility until a secure key exchange/handshake is in place.
- **Enablement Requirement**: Turn on encryption only after provisioning matching keys on firmware and plugin and establishing a secure exchange procedure.
- **Behavior**: When disabled, traffic is plaintext. When enabled, BLE/Serial paths encrypt/decrypt with configured keys.

#### Implementation
- Firmware: `firmware/src/security.h` and `security.cpp`
- Android Plugin: `atak_plugin/src/com/akitaengineering/meshtak/SecurityManager.java`

#### Key Provisioning
**CRITICAL**: In production, encryption keys MUST be provisioned securely:
- Use secure key storage (Android Keystore, ESP32 NVS with encryption)
- Implement key rotation policies
- Never hardcode keys in source code
- Use secure key exchange protocols
- After provisioning, explicitly enable encryption in the plugin; otherwise, plaintext is used.

### 2. Input Validation
- **Command Validation**: All incoming commands are validated before processing
- **Injection Prevention**: Protection against code injection attacks
- **Length Limits**: Maximum message/command lengths enforced
- **Character Sanitization**: Dangerous characters are filtered

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

### 4. Message Integrity
- **HMAC Verification**: All messages include HMAC for integrity checking
- **Tamper Detection**: Invalid HMAC indicates tampering
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
   - Use strong, unique keys for each device
   - Rotate keys periodically
   - Store keys securely (hardware security modules if available)

2. **Monitor Audit Logs**
   - Review logs regularly
   - Investigate security violations
   - Export logs for compliance

3. **Network Security**
   - Use encrypted WiFi (WPA3 if available)
   - Use TLS for MQTT (if enabled)
   - Isolate networks when possible

4. **Physical Security**
   - Secure devices physically
   - Protect against tampering
   - Use tamper-evident seals

5. **Access Control**
   - Limit who can configure devices
   - Use strong passwords
   - Implement role-based access control

---

## Security Configuration

### Firmware Configuration

In `firmware/src/config.h`:
- Set secure UUIDs (not default values)
- Configure security mode
- Set maximum message lengths

### Android Plugin Configuration

In `atak_plugin/src/com/akitaengineering/meshtak/Config.java`:
- Set matching UUIDs
- Configure security settings
- Set validation parameters

---

## Security Violations

The system logs the following as security violations:
- Invalid command formats
- Injection pattern detection
- HMAC verification failures
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

- **v0.2.0**: Initial security implementation
  - AES-256 encryption
  - HMAC integrity checking
  - Input validation
  - Audit logging
  - Error handling improvements

---

## References

- NIST Cybersecurity Framework
- OWASP Mobile Security
- Android Security Best Practices
- ESP32 Security Guidelines

---

**Copyright (C) 2025 Akita Engineering**

