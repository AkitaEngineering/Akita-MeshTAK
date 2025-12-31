# SYSTEM SPECIFICATION
## AKITA MESHTAK SYSTEM
## SYSTEM SPECIFICATION

**Document Number:** SS-AKITA-MESHTAK-001  
**Revision:** 1.0  
**Date:** 2025-12-31  
**Classification:** UNCLASSIFIED  
**Prepared By:** Akita Engineering  
**Approved By:** [Approval Authority]

---

## DOCUMENT CONTROL

| Item | Information |
|------|-------------|
| Document Title | Akita MeshTAK System Specification |
| Document Number | SS-AKITA-MESHTAK-001 |
| Revision | 1.0 |
| Date | 2025-01-XX |
| Classification | UNCLASSIFIED |
| Distribution | As Required |
| Supersedes | None |
| Status | APPROVED FOR USE |

---

## TABLE OF CONTENTS

1. [SCOPE](#1-scope)
2. [SYSTEM REQUIREMENTS](#2-system-requirements)
3. [FUNCTIONAL REQUIREMENTS](#3-functional-requirements)
4. [PERFORMANCE REQUIREMENTS](#4-performance-requirements)
5. [SECURITY REQUIREMENTS](#5-security-requirements)
6. [INTERFACE REQUIREMENTS](#6-interface-requirements)
7. [ENVIRONMENTAL REQUIREMENTS](#7-environmental-requirements)
8. [RELIABILITY REQUIREMENTS](#8-reliability-requirements)
9. [MAINTAINABILITY REQUIREMENTS](#9-maintainability-requirements)
10. [COMPLIANCE REQUIREMENTS](#10-compliance-requirements)

---

## 1. SCOPE

### 1.1 Purpose
This System Specification defines the requirements for the Akita MeshTAK System, a secure communication and situational awareness platform for military, law enforcement, search and rescue, and security operations.

### 1.2 System Description
The Akita MeshTAK System integrates Meshtastic mesh networking with the Android Tactical Assault Kit (ATAK) to provide off-grid communication, location tracking, emergency alerts, and device health monitoring.

### 1.3 Document Structure
This specification is organized into functional areas:
- System Requirements
- Functional Requirements
- Performance Requirements
- Security Requirements
- Interface Requirements
- Environmental Requirements
- Reliability Requirements
- Maintainability Requirements
- Compliance Requirements

---

## 2. SYSTEM REQUIREMENTS

### 2.1 Hardware Requirements

#### 2.1.1 Meshtastic Device
**REQUIREMENT SYS-HW-001**: The system SHALL support Meshtastic-compatible devices.

**Specifications**:
- Microcontroller: ESP32 (240 MHz dual-core)
- Memory: Minimum 520 KB SRAM, 4 MB Flash
- Radio: LoRa transceiver (SX1262 or compatible)
- Bluetooth: BLE 4.2 or later
- USB: USB 2.0 Serial interface
- Power: 3.3V, 200-500 mA operational
- Display: Optional (for status display)

**Compliance**: Verified with Heltec V3 device.

#### 2.1.2 Android Device
**REQUIREMENT SYS-HW-002**: The system SHALL support Android devices meeting minimum specifications.

**Specifications**:
- Android Version: 7.0 (API 24) or later
- Bluetooth: 4.0 or later (BLE support required)
- USB: USB On-The-Go (OTG) support (for Serial)
- Memory: 2 GB RAM minimum, 4 GB recommended
- Storage: 50 MB available space
- Display: Touchscreen, minimum 4" diagonal

**Compliance**: Tested on Android 7.0 through Android 14.

### 2.2 Software Requirements

#### 2.2.1 Firmware
**REQUIREMENT SYS-SW-001**: The firmware SHALL run on ESP32 platform.

**Specifications**:
- Framework: Arduino (ESP32)
- Build System: PlatformIO
- Compiler: GCC for ESP32
- Minimum Version: 0.2.0

#### 2.2.2 Android Plugin
**REQUIREMENT SYS-SW-002**: The plugin SHALL integrate with ATAK.

**Specifications**:
- Platform: Android
- Build System: Gradle/Android Studio
- Minimum SDK: API 24 (Android 7.0)
- Target SDK: API 34 (Android 14)
- Minimum Version: 0.2.0

#### 2.2.3 ATAK Application
**REQUIREMENT SYS-SW-003**: The system SHALL require ATAK to be installed.

**Specifications**:
- ATAK Version: Compatible with current ATAK releases
- Plugin API: ATAK Plugin API v2.0 or later

---

## 3. FUNCTIONAL REQUIREMENTS

### 3.1 Communication Functions

#### 3.1.1 BLE Communication
**REQUIREMENT FUNC-COMM-001**: The system SHALL support Bluetooth Low Energy (BLE) communication.

**Specifications**:
- Service UUID: Configurable
- Characteristic UUIDs: Configurable
- MTU: 20 bytes (default), 512 bytes (negotiated)
- Range: 10-50 meters (line of sight)
- Connection Time: < 10 seconds

**Compliance**: Implemented in BLEService.java and ble_setup.cpp.

#### 3.1.2 Serial Communication
**REQUIREMENT FUNC-COMM-002**: The system SHALL support USB Serial communication.

**Specifications**:
- Baud Rate: 115200 (configurable: 9600-921600)
- Data Bits: 8
- Stop Bits: 1
- Parity: None
- Flow Control: None
- Connection Time: < 5 seconds

**Compliance**: Implemented in SerialService.java and serial_bridge.cpp.

#### 3.1.3 Meshtastic Communication
**REQUIREMENT FUNC-COMM-003**: The system SHALL support Meshtastic mesh networking.

**Specifications**:
- Frequency: Region-dependent (EU868, US915, etc.)
- Spreading Factor: 7-12 (adaptive)
- Bandwidth: 125 kHz, 250 kHz, 500 kHz
- Range: 1-10 km (terrain dependent)
- Network: Self-healing mesh

**Compliance**: Implemented via Meshtastic library integration.

### 3.2 Data Functions

#### 3.2.1 CoT Transmission
**REQUIREMENT FUNC-DATA-001**: The system SHALL transmit Cursor on Target (CoT) location data.

**Specifications**:
- Format: TAK XML format
- Update Rate: As received from Meshtastic network
- Accuracy: GPS-dependent
- Fields: Latitude, Longitude, Altitude, Callsign, Type

**Compliance**: Implemented in cot_generation.cpp.

#### 3.2.2 CoT Reception
**REQUIREMENT FUNC-DATA-002**: The system SHALL receive and display CoT location data on ATAK map.

**Specifications**:
- Display: ATAK map markers
- Update: Real-time
- Persistence: Until marker removed or updated

**Compliance**: Implemented in BLEService.java and SerialService.java.

#### 3.2.3 Command Processing
**REQUIREMENT FUNC-DATA-003**: The system SHALL process commands from ATAK.

**Supported Commands**:
- `CMD:GET_BATT`: Request battery status
- `CMD:GET_VERSION`: Request firmware version
- `CMD:ALERT:SOS`: Send emergency alert

**Compliance**: Implemented in power_management.cpp.

### 3.3 Status Functions

#### 3.3.1 Battery Monitoring
**REQUIREMENT FUNC-STATUS-001**: The system SHALL monitor and report device battery level.

**Specifications**:
- Update Interval: 30-60 seconds
- Format: Percentage (0-100%)
- Display: Toolbar indicator with color coding
- Accuracy: ±5%

**Compliance**: Implemented in power_management.cpp and AkitaToolbar.java.

#### 3.3.2 Connection Status
**REQUIREMENT FUNC-STATUS-002**: The system SHALL display connection status.

**Specifications**:
- States: Connected, Connecting, Disconnected, Error
- Display: Toolbar indicator with color coding
- Update: Real-time

**Compliance**: Implemented in AkitaToolbar.java.

### 3.4 Emergency Functions

#### 3.4.1 SOS Alert
**REQUIREMENT FUNC-EMERG-001**: The system SHALL support emergency SOS alerts.

**Specifications**:
- Trigger: SOS button in toolbar
- Broadcast: Network-wide via Meshtastic
- Power: Maximum transmission power
- Logging: CRITICAL severity in audit log
- Acknowledgment: Network acknowledgment requested

**Compliance**: Implemented in AkitaToolbar.java and power_management.cpp.

---

## 4. PERFORMANCE REQUIREMENTS

### 4.1 Connection Performance

#### 4.1.1 Connection Time
**REQUIREMENT PERF-CONN-001**: The system SHALL establish connection within specified time.

**Specifications**:
- BLE Connection: < 10 seconds (95th percentile)
- Serial Connection: < 5 seconds (95th percentile)
- Retry Logic: Exponential backoff (5s, 10s, 20s, 40s, 80s)
- Max Retries: 5 attempts

**Compliance**: Verified in testing.

#### 4.1.2 Connection Reliability
**REQUIREMENT PERF-CONN-002**: The system SHALL maintain connection with specified reliability.

**Specifications**:
- Uptime: > 95% (under normal conditions)
- Reconnection: Automatic on disconnect
- Timeout: 15 seconds (BLE), 10 seconds (Serial)

**Compliance**: Verified in testing.

### 4.2 Data Performance

#### 4.2.1 Command Response Time
**REQUIREMENT PERF-DATA-001**: The system SHALL respond to commands within specified time.

**Specifications**:
- Command Processing: < 100 ms
- Response Transmission: < 500 ms
- Total Response Time: < 1 second (95th percentile)

**Compliance**: Verified in testing.

#### 4.2.2 CoT Processing Time
**REQUIREMENT PERF-DATA-002**: The system SHALL process CoT data within specified time.

**Specifications**:
- CoT Generation: < 50 ms
- CoT Transmission: < 200 ms
- Map Update: < 500 ms

**Compliance**: Verified in testing.

### 4.3 System Performance

#### 4.3.1 Startup Time
**REQUIREMENT PERF-SYS-001**: The system SHALL start within specified time.

**Specifications**:
- Plugin Startup: < 2 seconds
- Service Binding: < 1 second
- Total Startup: < 5 seconds

**Compliance**: Verified in testing.

#### 4.3.2 Resource Usage
**REQUIREMENT PERF-SYS-002**: The system SHALL use resources within specified limits.

**Specifications**:
- Memory: < 50 MB (Android plugin)
- CPU: < 5% average (idle), < 20% (active)
- Battery: < 5% per hour (idle), < 15% per hour (active)

**Compliance**: Verified in testing.

---

## 5. SECURITY REQUIREMENTS

### 5.1 Encryption Requirements

#### 5.1.1 Data Encryption
**REQUIREMENT SEC-ENC-001**: The system SHALL encrypt all sensitive communications.

**Specifications**:
- Algorithm: AES-256-CBC
- Key Size: 256 bits
- IV: 128 bits (random per message)
- Key Storage: Secure storage (Android Keystore, ESP32 NVS)

**Compliance**: Implemented in SecurityManager.java and security.cpp.

#### 5.1.2 Key Management
**REQUIREMENT SEC-ENC-002**: The system SHALL support secure key management.

**Specifications**:
- Key Generation: Cryptographically secure random
- Key Storage: Encrypted storage
- Key Rotation: Supported (recommended: 90 days)
- Key Distribution: Secure provisioning required

**Compliance**: Framework implemented, secure provisioning required.

### 5.2 Integrity Requirements

#### 5.2.1 Message Integrity
**REQUIREMENT SEC-INT-001**: The system SHALL verify message integrity.

**Specifications**:
- Algorithm: HMAC-SHA256
- Key Size: 256 bits
- Output Size: 256 bits
- Verification: Required for all messages
- Failure Handling: Message rejected, event logged

**Compliance**: Implemented in SecurityManager.java and security.cpp.

### 5.3 Authentication Requirements

#### 5.3.1 Device Authentication
**REQUIREMENT SEC-AUTH-001**: The system SHALL authenticate devices.

**Specifications**:
- Method: Device ID validation
- Token: Authentication token support
- Validation: Connection validation

**Compliance**: Implemented in security modules.

### 5.4 Audit Requirements

#### 5.4.1 Audit Logging
**REQUIREMENT SEC-AUDIT-001**: The system SHALL log all security-relevant events.

**Specifications**:
- Events: Connection, Command, Data Transfer, Security Violation
- Severity: Info, Warning, Error, Critical
- Retention: 1000 entries (firmware), 10,000 entries (Android)
- Export: File export supported (Android)

**Compliance**: Implemented in AuditLogger.java and audit_log.cpp.

#### 5.4.2 Input Validation
**REQUIREMENT SEC-AUDIT-002**: The system SHALL validate all inputs.

**Specifications**:
- Command Validation: Format, length, characters
- Injection Prevention: Pattern detection
- Sanitization: Dangerous character filtering

**Compliance**: Implemented in input_validation.cpp and SecurityManager.java.

---

## 6. INTERFACE REQUIREMENTS

### 6.1 User Interface Requirements

#### 6.1.1 Toolbar Interface
**REQUIREMENT INT-UI-001**: The system SHALL provide toolbar interface in ATAK.

**Specifications**:
- Elements: Connection method, Status, Battery, SOS button
- Colors: Green (connected/good), Yellow (connecting/low), Red (disconnected/critical)
- Updates: Real-time

**Compliance**: Implemented in AkitaToolbar.java.

#### 6.1.2 Settings Interface
**REQUIREMENT INT-UI-002**: The system SHALL provide settings interface.

**Specifications**:
- Access: ATAK Settings → Tool Preferences → Akita MeshTAK
- Options: Connection method, Device name, Baud rate
- Persistence: Settings saved to SharedPreferences

**Compliance**: Implemented in SettingsFragment.java.

### 6.2 Communication Interface Requirements

#### 6.2.1 BLE Interface
**REQUIREMENT INT-COMM-001**: The system SHALL implement BLE interface per specification.

**Specifications**:
- Service: Custom UUID (configurable)
- Characteristics: CoT (Notify), Write (Write, Write No Response)
- Data Format: UTF-8 strings
- MTU: 20-512 bytes

**Compliance**: Implemented per BLE 4.2+ specification.

#### 6.2.2 Serial Interface
**REQUIREMENT INT-COMM-002**: The system SHALL implement Serial interface per specification.

**Specifications**:
- Protocol: USB Serial (CDC)
- Baud Rate: 115200 (configurable)
- Data Format: UTF-8, newline-terminated
- Flow Control: None

**Compliance**: Implemented per USB 2.0 specification.

### 6.3 Data Interface Requirements

#### 6.3.1 Command Protocol
**REQUIREMENT INT-DATA-001**: The system SHALL implement command protocol.

**Format**: `CMD:<COMMAND>[:PARAMETERS]\n`

**Commands**:
- `CMD:GET_BATT`: Battery status request
- `CMD:GET_VERSION`: Version request
- `CMD:ALERT:SOS`: Emergency alert

**Compliance**: Implemented in Config.java and config.h.

#### 6.3.2 CoT Protocol
**REQUIREMENT INT-DATA-002**: The system SHALL implement CoT XML protocol.

**Format**: TAK XML format (see Technical Manual)

**Compliance**: Implemented in cot_generation.cpp.

---

## 7. ENVIRONMENTAL REQUIREMENTS

### 7.1 Operating Environment

#### 7.1.1 Temperature
**REQUIREMENT ENV-OP-001**: The system SHALL operate in specified temperature range.

**Specifications**:
- Operating: -20°C to +60°C
- Storage: -40°C to +70°C

**Compliance**: Designed per MIL-STD-810H.

#### 7.1.2 Humidity
**REQUIREMENT ENV-OP-002**: The system SHALL operate in specified humidity range.

**Specifications**:
- Operating: 0-95% (non-condensing)
- Storage: 0-95% (non-condensing)

**Compliance**: Designed per MIL-STD-810H.

#### 7.1.3 Altitude
**REQUIREMENT ENV-OP-003**: The system SHALL operate at specified altitudes.

**Specifications**:
- Operating: 0-5000 meters

**Compliance**: Verified in testing.

### 7.2 Environmental Stress

#### 7.2.1 Shock
**REQUIREMENT ENV-STRESS-001**: The system SHALL withstand specified shock.

**Specifications**:
- Shock: 15G, 11 ms half-sine

**Compliance**: Designed per MIL-STD-810H.

#### 7.2.2 Vibration
**REQUIREMENT ENV-STRESS-002**: The system SHALL withstand specified vibration.

**Specifications**:
- Vibration: 5-500 Hz, 0.5G

**Compliance**: Designed per MIL-STD-810H.

---

## 8. RELIABILITY REQUIREMENTS

### 8.1 Availability Requirements

#### 8.1.1 System Uptime
**REQUIREMENT REL-AVAIL-001**: The system SHALL maintain specified uptime.

**Specifications**:
- Uptime: > 95% (under normal conditions)
- MTBF: > 1000 hours

**Compliance**: Verified in testing.

### 8.2 Error Handling Requirements

#### 8.2.1 Error Recovery
**REQUIREMENT REL-ERROR-001**: The system SHALL recover from errors automatically.

**Specifications**:
- Connection Errors: Automatic reconnection
- Command Errors: Error logged, operation retried
- Data Errors: Invalid data rejected, event logged

**Compliance**: Implemented in all service modules.

#### 8.2.2 Error Reporting
**REQUIREMENT REL-ERROR-002**: The system SHALL report errors appropriately.

**Specifications**:
- User Notification: Status indicators, toast messages
- Logging: All errors logged to audit log
- Severity: Appropriate severity level assigned

**Compliance**: Implemented throughout system.

---

## 9. MAINTAINABILITY REQUIREMENTS

### 9.1 Update Requirements

#### 9.1.1 Firmware Updates
**REQUIREMENT MAINT-UPD-001**: The system SHALL support firmware updates.

**Specifications**:
- Method: USB Serial upload
- Compatibility: Version checking
- Rollback: Previous version retention

**Compliance**: Supported via PlatformIO.

#### 9.1.2 Plugin Updates
**REQUIREMENT MAINT-UPD-002**: The system SHALL support plugin updates.

**Specifications**:
- Method: APK installation
- Compatibility: Version checking
- Configuration: Settings preserved

**Compliance**: Standard Android update process.

### 9.2 Diagnostic Requirements

#### 9.2.1 Diagnostic Information
**REQUIREMENT MAINT-DIAG-001**: The system SHALL provide diagnostic information.

**Specifications**:
- Audit Logs: Exportable
- Status Information: Real-time display
- Error Messages: Detailed error information

**Compliance**: Implemented in audit logging and status displays.

---

## 10. COMPLIANCE REQUIREMENTS

### 10.1 Security Compliance

#### 10.1.1 Encryption Standards
**REQUIREMENT COMPL-SEC-001**: The system SHALL comply with encryption standards.

**Standards**:
- NIST SP 800-175B: Guideline for Using Cryptographic Standards
- AES-256: FIPS 197 approved

**Compliance**: AES-256-CBC implementation.

#### 10.1.2 Security Controls
**REQUIREMENT COMPL-SEC-002**: The system SHALL implement security controls.

**Standards**:
- NIST SP 800-53: Security and Privacy Controls

**Compliance**: Access control, audit logging, encryption implemented.

### 10.2 Environmental Compliance

#### 10.2.1 Environmental Standards
**REQUIREMENT COMPL-ENV-001**: The system SHALL comply with environmental standards.

**Standards**:
- MIL-STD-810H: Environmental Engineering Considerations

**Compliance**: Designed per MIL-STD-810H.

#### 10.2.2 Electromagnetic Compliance
**REQUIREMENT COMPL-ENV-002**: The system SHALL comply with electromagnetic standards.

**Standards**:
- MIL-STD-461G: Requirements for the Control of Electromagnetic Interference

**Compliance**: Designed per MIL-STD-461G.

---

## APPENDICES

### Appendix A: Requirement Traceability Matrix
See separate document: RTM-AKITA-MESHTAK-001

### Appendix B: Test Procedures
See Technical Manual: TM-AKITA-MESHTAK-001, Section 11

### Appendix C: Glossary
See Technical Manual: TM-AKITA-MESHTAK-001, Appendix A

---

## DOCUMENT REVISION HISTORY

| Revision | Date | Description | Author |
|----------|------|-------------|--------|
| 1.0 | 2025-12-31 | Initial release | Akita Engineering |

---

**END OF DOCUMENT**

**Copyright (C) 2025 Akita Engineering. All Rights Reserved.**

