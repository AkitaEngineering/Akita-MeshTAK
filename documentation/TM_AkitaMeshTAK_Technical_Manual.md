# TECHNICAL MANUAL
## AKITA MESHTAK SYSTEM
## TECHNICAL MANUAL

**Document Number:** TM-AKITA-MESHTAK-001  
**Revision:** 1.0  
**Date:** 2025-12-31  
**Classification:** UNCLASSIFIED  
**Prepared By:** Akita Engineering  
**Approved By:** [Approval Authority]

---

## DOCUMENT CONTROL

| Item | Information |
|------|-------------|
| Document Title | Akita MeshTAK System Technical Manual |
| Document Number | TM-AKITA-MESHTAK-001 |
| Revision | 1.0 |
| Date | 2025-01-XX |
| Classification | UNCLASSIFIED |
| Distribution | As Required |
| Supersedes | None |
| Status | APPROVED FOR USE |

---

## TABLE OF CONTENTS

1. [SCOPE](#1-scope)
2. [REFERENCED DOCUMENTS](#2-referenced-documents)
3. [SYSTEM DESCRIPTION](#3-system-description)
4. [TECHNICAL SPECIFICATIONS](#4-technical-specifications)
5. [INSTALLATION PROCEDURES](#5-installation-procedures)
6. [CONFIGURATION PROCEDURES](#6-configuration-procedures)
7. [OPERATION PROCEDURES](#7-operation-procedures)
8. [MAINTENANCE PROCEDURES](#8-maintenance-procedures)
9. [TROUBLESHOOTING](#9-troubleshooting)
10. [INTERFACE SPECIFICATIONS](#10-interface-specifications)
11. [TEST PROCEDURES](#11-test-procedures)
12. [SAFETY WARNINGS](#12-safety-warnings)
13. [APPENDICES](#13-appendices)

---

## 1. SCOPE

### 1.1 Purpose
This Technical Manual (TM) provides comprehensive technical information for the installation, configuration, operation, and maintenance of the Akita MeshTAK System. This system provides secure, off-grid communication and situational awareness capabilities for military, law enforcement, search and rescue, and security operations.

### 1.2 Scope
This manual covers:
- System architecture and components
- Technical specifications
- Installation procedures
- Configuration procedures
- Operational procedures
- Maintenance procedures
- Troubleshooting procedures
- Interface specifications
- Test procedures
- Safety requirements

### 1.3 Applicability
This manual applies to:
- Akita MeshTAK Firmware Version 0.2.0 and later
- Akita MeshTAK Android Plugin Version 0.2.0 and later
- Heltec V3 and compatible Meshtastic devices
- Android devices running ATAK (Android Tactical Assault Kit)

### 1.4 User Responsibilities
Users of this manual are responsible for:
- Following all safety warnings and procedures
- Maintaining system security and encryption keys
- Performing regular maintenance
- Reporting system malfunctions
- Maintaining audit logs for compliance

---

## 2. REFERENCED DOCUMENTS

### 2.1 Government Documents
- MIL-STD-461G: Requirements for the Control of Electromagnetic Interference
- MIL-STD-810H: Environmental Engineering Considerations
- NIST SP 800-53: Security and Privacy Controls
- NIST SP 800-175B: Guideline for Using Cryptographic Standards

### 2.2 Industry Standards
- IEEE 802.15.1: Bluetooth Standard
- IEEE 802.11: Wireless LAN Standards
- USB 2.0 Specification
- LoRaWAN Specification v1.0

### 2.3 Related Documents
- TM-AKITA-MESHTAK-002: Operator's Manual
- TM-AKITA-MESHTAK-003: System Specification
- TM-AKITA-MESHTAK-004: Security Guide
- TM-AKITA-MESHTAK-005: Maintenance Log

### 2.4 Software Documentation
- ATAK User Manual
- Meshtastic Documentation
- Android Developer Documentation
- PlatformIO Documentation

---

## 3. SYSTEM DESCRIPTION

### 3.1 System Overview
The Akita MeshTAK System is a secure communication and situational awareness platform that integrates Meshtastic mesh networking with the Android Tactical Assault Kit (ATAK). The system enables off-grid communication, location tracking, emergency alerts, and device health monitoring in environments where traditional communication infrastructure is unavailable or compromised.

### 3.2 System Components

#### 3.2.1 Hardware Components
1. **Meshtastic Device (Heltec V3 or compatible)**
   - ESP32 microcontroller
   - LoRa radio transceiver
   - Bluetooth Low Energy (BLE) capability
   - USB Serial interface
   - Battery power management
   - Display (optional)

2. **Android Device**
   - Android 7.0 (API 24) or later
   - Bluetooth 4.0 or later
   - USB On-The-Go (OTG) support
   - ATAK installed

#### 3.2.2 Software Components
1. **Akita MeshTAK Firmware**
   - Version: 0.2.0
   - Platform: ESP32 (Arduino framework)
   - Build System: PlatformIO

2. **Akita MeshTAK Android Plugin**
   - Version: 0.2.0
   - Platform: Android
   - Build System: Gradle/Android Studio
   - Minimum SDK: API 24 (Android 7.0)

### 3.3 System Architecture

#### 3.3.1 Communication Architecture
```
[ATAK Application]
       |
       v
[Akita MeshTAK Plugin]
       |
       +---> [BLE Service] <---> [BLE Interface] <---> [Firmware]
       |
       +---> [Serial Service] <---> [USB Serial] <---> [Firmware]
       |
       +---> [MQTT Service] <---> [WiFi/MQTT] <---> [Firmware] (Optional)
```

#### 3.3.2 Data Flow
1. **Location Data (CoT)**
   - Meshtastic Network → Firmware → Plugin → ATAK Map

2. **Commands**
   - ATAK → Plugin → Firmware → Meshtastic Network

3. **Status Information**
   - Firmware → Plugin → ATAK Toolbar

### 3.4 Security Architecture

#### 3.4.1 Encryption
- **Algorithm**: AES-256-CBC
- **Key Size**: 256 bits
- **Initialization Vector**: 128 bits (random per message)

#### 3.4.2 Integrity
- **Algorithm**: HMAC-SHA256
- **Key Size**: 256 bits
- **Output Size**: 256 bits

#### 3.4.3 Authentication
- Device ID validation
- Authentication token verification
- Connection validation

#### 3.4.4 Audit Logging
- All security-relevant events logged
- Event types: Connection, Command, Data Transfer, Security Violation
- Severity levels: Info, Warning, Error, Critical
- Retention: 1000 entries (firmware), 10,000 entries (Android)

---

## 4. TECHNICAL SPECIFICATIONS

### 4.1 Firmware Specifications

#### 4.1.1 Hardware Requirements
- **Microcontroller**: ESP32 (240 MHz dual-core)
- **Memory**: 520 KB SRAM, 4 MB Flash (minimum)
- **Radio**: LoRa transceiver (SX1262 or compatible)
- **Bluetooth**: BLE 4.2 or later
- **USB**: USB 2.0 Serial interface
- **Power**: 3.3V, 200-500 mA (operational)

#### 4.1.2 Software Requirements
- **Framework**: Arduino (ESP32)
- **Build System**: PlatformIO
- **Compiler**: GCC for ESP32
- **Libraries**:
  - Meshtastic Library
  - BLE Library (ESP32)
  - mbedTLS (for encryption)

#### 4.1.3 Performance Specifications
- **BLE Connection Time**: < 10 seconds
- **Serial Connection Time**: < 5 seconds
- **Command Processing**: < 100 ms
- **CoT Generation**: < 50 ms
- **Battery Check Interval**: 60 seconds

#### 4.1.4 Communication Specifications
- **BLE**:
  - Service UUID: Configurable (see Section 6.2)
  - Characteristic UUIDs: Configurable
  - MTU: 20 bytes (default), 512 bytes (negotiated)
  - Range: 10-50 meters (line of sight)

- **Serial**:
  - Baud Rate: 115200 (configurable: 9600-921600)
  - Data Bits: 8
  - Stop Bits: 1
  - Parity: None
  - Flow Control: None

- **LoRa/Meshtastic**:
  - Frequency: Region-dependent (EU868, US915, etc.)
  - Spreading Factor: 7-12 (adaptive)
  - Bandwidth: 125 kHz, 250 kHz, 500 kHz
  - Range: 1-10 km (terrain dependent)

### 4.2 Android Plugin Specifications

#### 4.2.1 Hardware Requirements
- **Device**: Android smartphone or tablet
- **Android Version**: 7.0 (API 24) or later
- **Bluetooth**: 4.0 or later (BLE support required)
- **USB**: USB On-The-Go (OTG) support (for Serial)
- **Memory**: 2 GB RAM (minimum), 4 GB recommended
- **Storage**: 50 MB available space

#### 4.2.2 Software Requirements
- **ATAK**: Installed and configured
- **Android SDK**: API 24 or later
- **Permissions**:
  - BLUETOOTH
  - BLUETOOTH_ADMIN
  - BLUETOOTH_SCAN (Android 12+)
  - BLUETOOTH_CONNECT (Android 12+)
  - USB permissions

#### 4.2.3 Performance Specifications
- **Plugin Startup**: < 2 seconds
- **Service Binding**: < 1 second
- **BLE Scan Time**: 10 seconds (configurable)
- **Connection Retry**: Exponential backoff (5s, 10s, 20s, 40s, 80s)
- **Max Retry Attempts**: 5

### 4.3 Security Specifications

#### 4.3.1 Encryption Specifications
- **Algorithm**: AES-256-CBC
- **Key Derivation**: Secure random generation
- **Key Storage**: Secure storage required (Android Keystore, ESP32 NVS)
- **Key Rotation**: Recommended every 90 days
- **IV Generation**: Cryptographically secure random

#### 4.3.2 Integrity Specifications
- **Algorithm**: HMAC-SHA256
- **Key Size**: 256 bits
- **Verification**: Required for all messages
- **Failure Handling**: Message rejected, event logged

#### 4.3.3 Audit Log Specifications
- **Firmware**: 1000 entries (circular buffer)
- **Android**: 10,000 entries (circular buffer) + file export
- **Entry Size**: ~200 bytes
- **Export Format**: Text file (CSV-like format)
- **Retention**: Configurable (recommended: 90 days minimum)

### 4.4 Environmental Specifications

#### 4.4.1 Operating Environment
- **Temperature**: -20°C to +60°C
- **Humidity**: 0-95% (non-condensing)
- **Altitude**: 0-5000 meters
- **Shock**: 15G, 11 ms half-sine
- **Vibration**: 5-500 Hz, 0.5G

#### 4.4.2 Storage Environment
- **Temperature**: -40°C to +70°C
- **Humidity**: 0-95% (non-condensing)

---

## 5. INSTALLATION PROCEDURES

### 5.1 Pre-Installation Requirements

#### 5.1.1 Required Materials
- Meshtastic-compatible device (Heltec V3 recommended)
- Android device with ATAK installed
- USB cable (for Serial connection)
- Computer with PlatformIO (for firmware installation)
- Computer with Android Studio (for plugin development)

#### 5.1.2 Required Software
- PlatformIO IDE or PlatformIO Core
- Android Studio (latest stable version)
- ATAK application
- USB drivers (for device)

#### 5.1.3 Safety Precautions
- **WARNING**: Ensure device is powered off before connecting USB cable
- **WARNING**: Use only approved USB cables
- **CAUTION**: Do not expose device to moisture during installation
- **CAUTION**: Handle device with anti-static precautions

### 5.2 Firmware Installation

#### 5.2.1 Preparation
1. Install PlatformIO IDE or PlatformIO Core
2. Clone or download Akita MeshTAK repository
3. Navigate to `firmware/` directory

#### 5.2.2 Configuration
1. Open `firmware/src/config.h`
2. Configure the following parameters:
   - `DEVICE_ID`: Unique device identifier
   - `BLE_SERVICE_UUID`: BLE service UUID
   - `BLE_COT_CHARACTERISTIC_UUID`: CoT characteristic UUID
   - `BLE_WRITE_CHARACTERISTIC_UUID`: Write characteristic UUID
   - `LORA_REGION`: LoRa region (EU868, US915, etc.)

**CRITICAL**: UUIDs must match between firmware and Android plugin.

#### 5.2.3 Build Procedure
1. Connect device to computer via USB
2. Open terminal in `firmware/` directory
3. Execute: `pio run`
4. Verify build completes without errors
5. Execute: `pio run -t upload`
6. Verify upload completes successfully

#### 5.2.4 Verification
1. Open serial monitor: `pio device monitor`
2. Verify device boots and displays:
   ```
   Akita MeshTAK Firmware v0.2.0 Starting...
   Device ID: [your device ID]
   Initializing Meshtastic...
   Meshtastic Started. My Node ID: [node ID]
   Initializing BLE...
   BLE Advertising started.
   Setup complete.
   ```

### 5.3 Android Plugin Installation

#### 5.3.1 Preparation
1. Install Android Studio
2. Install Android SDK (API 24 or later)
3. Configure ATAK SDK as library module
4. Clone or download Akita MeshTAK repository

#### 5.3.2 Configuration
1. Open `atak_plugin/` in Android Studio
2. Edit `atak_plugin/src/com/akitaengineering/meshtak/Config.java`
3. Configure the following parameters:
   - `BLE_SERVICE_UUID`: Must match firmware
   - `COT_CHARACTERISTIC_UUID`: Must match firmware
   - `WRITE_CHARACTERISTIC_UUID`: Must match firmware
   - `HELTEC_VENDOR_ID`: USB vendor ID (decimal)
   - `HELTEC_PRODUCT_ID`: USB product ID (decimal)

**CRITICAL**: All UUIDs and IDs must match firmware configuration.

#### 5.3.3 Build Procedure
1. Sync Gradle files
2. Build APK: `Build → Build Bundle(s) / APK(s) → Build APK(s)`
3. Locate APK: `atak_plugin/app/build/outputs/apk/debug/` or `release/`
4. Transfer APK to Android device

#### 5.3.4 Installation on Device
1. Enable "Install from Unknown Sources" in Android settings
2. Locate APK file on device
3. Tap APK file to install
4. Grant all required permissions when prompted
5. Open ATAK application
6. Navigate to: **Settings → Plugin Manager**
7. Enable "Akita MeshTAK" plugin
8. Restart ATAK if required

#### 5.3.5 Verification
1. Verify plugin appears in ATAK toolbar
2. Verify connection status displays
3. Verify battery status displays (after connection)

---

## 6. CONFIGURATION PROCEDURES

### 6.1 Firmware Configuration

#### 6.1.1 Device Identification
Edit `firmware/src/config.h`:
```cpp
#define DEVICE_ID "AkitaNode01"  // Change to unique identifier
```

#### 6.1.2 BLE Configuration
Edit `firmware/src/config.h`:
```cpp
#define BLE_SERVICE_UUID        "0000181A-0000-1000-8000-00805F9B34FB"
#define BLE_COT_CHARACTERISTIC_UUID "00002A6E-0000-1000-8000-00805F9B34FB"
#define BLE_WRITE_CHARACTERISTIC_UUID "00002A6C-0000-1000-8000-00805F9B34FB"
```

**NOTE**: Generate unique UUIDs using UUID generator. Do not use default values in production.

#### 6.1.3 LoRa Configuration
Edit `firmware/src/config.h`:
```cpp
#define LORA_REGION EU868  // Or US915, AS923, etc.
```

#### 6.1.4 Security Configuration
**CRITICAL**: Security keys must be provisioned securely. See Section 6.3.

### 6.2 Android Plugin Configuration

#### 6.2.1 BLE Configuration
Edit `atak_plugin/src/com/akitaengineering/meshtak/Config.java`:
```java
public static final UUID BLE_SERVICE_UUID = 
    UUID.fromString("0000181A-0000-1000-8000-00805F9B34FB");
public static final UUID COT_CHARACTERISTIC_UUID = 
    UUID.fromString("00002A6E-0000-1000-8000-00805F9B34FB");
public static final UUID WRITE_CHARACTERISTIC_UUID = 
    UUID.fromString("00002A6C-0000-1000-8000-00805F9B34FB");
```

#### 6.2.2 USB Configuration
Edit `atak_plugin/src/com/akitaengineering/meshtak/Config.java`:
```java
public static final int HELTEC_VENDOR_ID = 1027;  // Decimal
public static final int HELTEC_PRODUCT_ID = 24577;  // Decimal
```

**NOTE**: Use `lsusb` (Linux) or Device Manager (Windows) to find vendor/product IDs.

#### 6.2.3 Runtime Configuration
Configure via ATAK settings:
1. Open ATAK
2. Navigate to: **Settings → Tool Preferences → Akita MeshTAK**
3. Configure:
   - Connection Method: BLE or Serial
   - BLE Device Name: Device identifier
   - Serial Baud Rate: 115200 (default)

### 6.3 Security Configuration

#### 6.3.1 Key Provisioning (CRITICAL)
**WARNING**: Keys must be provisioned securely. Never hardcode keys in source code.

**Firmware**:
1. Generate secure keys using cryptographically secure random number generator
2. Store keys in ESP32 NVS (Non-Volatile Storage) with encryption
3. Implement key rotation policy (recommended: 90 days)

**Android Plugin**:
1. Generate secure keys using Android KeyGenerator
2. Store keys in Android Keystore
3. Implement key rotation policy

#### 6.3.2 Encryption Configuration
- **Algorithm**: AES-256-CBC (default)
- **Mode**: SECURITY_MODE_AES256_HMAC (recommended)
- **Key Size**: 256 bits
- **HMAC Key Size**: 256 bits

#### 6.3.3 Audit Log Configuration
- **Firmware**: 1000 entries (fixed)
- **Android**: 10,000 entries (configurable)
- **Export**: Enable file export for compliance
- **Retention**: Configure retention policy

---

## 7. OPERATION PROCEDURES

### 7.1 Initial Startup

#### 7.1.1 Power-On Sequence
1. Power on Meshtastic device
2. Wait for device initialization (LED indicators)
3. Power on Android device
4. Launch ATAK application
5. Verify plugin loads (check toolbar)

#### 7.1.2 Connection Establishment

**BLE Connection**:
1. Ensure BLE is enabled on Android device
2. Plugin automatically scans for device
3. Connection status displays in toolbar
4. Wait for "Connected" status (green)

**Serial Connection**:
1. Connect USB cable between devices
2. Grant USB permission if prompted
3. Plugin automatically detects device
4. Connection status displays in toolbar
5. Wait for "Connected" status (green)

### 7.2 Normal Operations

#### 7.2.1 Monitoring Connection Status
- **Toolbar Display**:
  - Method: BLE or Serial
  - Status: Connected (green), Connecting (yellow), Disconnected (red)
  - Battery: XX% (green/yellow/red based on level)

#### 7.2.2 Sending Data
1. Open "Send Data" view from ATAK menu
2. Enter message text
3. Select data format (Plain Text, JSON, Custom)
4. Tap "Send" button
5. Verify message sent (toast notification)

#### 7.2.3 Receiving Data
- CoT location data automatically appears on ATAK map
- Battery status updates automatically
- Status messages displayed in toolbar

#### 7.2.4 Emergency Alert (SOS)
1. Locate SOS button in toolbar
2. Tap SOS button
3. Confirm alert sent (toast notification)
4. Alert broadcast across Meshtastic network
5. Event logged in audit log (CRITICAL severity)

**WARNING**: SOS alerts are logged and cannot be undone. Use only in emergency situations.

### 7.3 Shutdown Procedures

#### 7.3.1 Normal Shutdown
1. Close ATAK application
2. Plugin services stop automatically
3. Disconnect USB cable (if Serial connection)
4. Power off Meshtastic device

#### 7.3.2 Emergency Shutdown
1. Power off Android device (if necessary)
2. Power off Meshtastic device
3. Document reason for emergency shutdown in maintenance log

---

## 8. MAINTENANCE PROCEDURES

### 8.1 Preventive Maintenance

#### 8.1.1 Daily Checks
- Verify connection status
- Check battery level
- Verify data transmission/reception
- Review audit logs for errors

#### 8.1.2 Weekly Checks
- Export audit logs
- Verify encryption keys are valid
- Check for firmware/plugin updates
- Clean device surfaces

#### 8.1.3 Monthly Checks
- Review security configuration
- Rotate encryption keys (if policy requires)
- Verify backup procedures
- Update documentation

### 8.2 Corrective Maintenance

#### 8.2.1 Connection Issues
See Section 9.1 (Troubleshooting)

#### 8.2.2 Security Issues
1. Review audit logs
2. Identify security violations
3. Rotate encryption keys if compromised
4. Report security incidents per policy

#### 8.2.3 Firmware Updates
1. Backup current configuration
2. Download new firmware version
3. Follow installation procedures (Section 5.2)
4. Verify compatibility with plugin version
5. Test all functions
6. Update documentation

#### 8.2.4 Plugin Updates
1. Backup current configuration
2. Download new plugin version
3. Follow installation procedures (Section 5.3)
4. Verify compatibility with firmware version
5. Test all functions
6. Update documentation

### 8.3 Maintenance Log
Maintain maintenance log with:
- Date and time
- Maintenance type (preventive/corrective)
- Actions taken
- Results
- Technician name/signature

---

## 9. TROUBLESHOOTING

### 9.1 Connection Issues

#### 9.1.1 BLE Connection Fails
**Symptoms**: Status shows "Disconnected" or "Error"

**Possible Causes**:
1. BLE not enabled on Android device
2. Device out of range
3. UUID mismatch between firmware and plugin
4. Device name mismatch

**Corrective Actions**:
1. Enable BLE in Android settings
2. Move devices closer together (within 10 meters)
3. Verify UUIDs match in Config files
4. Verify device name in settings matches firmware DEVICE_ID
5. Restart both devices
6. Check audit logs for error details

#### 9.1.2 Serial Connection Fails
**Symptoms**: Status shows "Disconnected" or "Error"

**Possible Causes**:
1. USB cable not connected
2. USB permission not granted
3. USB vendor/product ID mismatch
4. Baud rate mismatch
5. USB drivers not installed

**Corrective Actions**:
1. Verify USB cable connection
2. Grant USB permission when prompted
3. Verify vendor/product IDs in Config.java
4. Verify baud rate matches firmware (default: 115200)
5. Install USB drivers if required
6. Try different USB cable
7. Check audit logs for error details

### 9.2 Data Transmission Issues

#### 9.2.1 Data Not Received
**Symptoms**: CoT markers not appearing on map

**Possible Causes**:
1. Meshtastic network not connected
2. CoT data format invalid
3. Encryption/decryption failure

**Corrective Actions**:
1. Verify Meshtastic network connectivity
2. Check CoT XML format
3. Verify encryption keys match
4. Check audit logs for integrity failures
5. Test with plain text data (if encryption disabled)

#### 9.2.2 Commands Not Executed
**Symptoms**: Commands sent but not executed

**Possible Causes**:
1. Command format invalid
2. Input validation failure
3. Connection lost during transmission

**Corrective Actions**:
1. Verify command format (see Section 10.2)
2. Check audit logs for validation errors
3. Verify connection status
4. Retry command

### 9.3 Security Issues

#### 9.3.1 Encryption Failures
**Symptoms**: Audit log shows authentication failures

**Possible Causes**:
1. Encryption keys mismatch
2. Key corruption
3. IV generation failure

**Corrective Actions**:
1. Verify keys match between devices
2. Regenerate and provision new keys
3. Check key storage integrity
4. Review security configuration

#### 9.3.2 Integrity Failures
**Symptoms**: Audit log shows integrity failures

**Possible Causes**:
1. HMAC key mismatch
2. Message tampering
3. Transmission errors

**Corrective Actions**:
1. Verify HMAC keys match
2. Investigate potential tampering
3. Check transmission quality
4. Review audit logs for patterns

### 9.4 Performance Issues

#### 9.4.1 Slow Connection
**Symptoms**: Connection takes > 30 seconds

**Possible Causes**:
1. Device interference
2. Low battery
3. Network congestion

**Corrective Actions**:
1. Move to area with less interference
2. Charge/replace battery
3. Reduce network traffic
4. Check device temperature

---

## 10. INTERFACE SPECIFICATIONS

### 10.1 Command Protocol

#### 10.1.1 Command Format
All commands are ASCII strings terminated with newline (`\n`).

**Format**: `CMD:<COMMAND>[:PARAMETERS]`

#### 10.1.2 Supported Commands

**Battery Status Request**:
```
CMD:GET_BATT
```
**Response**: `STATUS:BATT:XX%` (where XX is 0-100)

**Version Request**:
```
CMD:GET_VERSION
```
**Response**: `STATUS:VERSION:X.Y.Z`

**Emergency Alert**:
```
CMD:ALERT:SOS
```
**Response**: Alert broadcast on Meshtastic network

#### 10.1.3 Status Responses
**Format**: `STATUS:<TYPE>:<VALUE>`

**Types**:
- `BATT`: Battery percentage (0-100)
- `VERSION`: Firmware version (X.Y.Z)

### 10.2 CoT (Cursor on Target) Protocol

#### 10.2.1 CoT XML Format
CoT messages follow the TAK (Tactical Assault Kit) XML format:

```xml
<event version="2.0" type="a-f-G-U-U">
  <uid generator="DEVICE_ID" uniqueid="UNIQUE_ID"/>
  <point lat="LATITUDE" lon="LONGITUDE" hae="ALTITUDE" ce="10" le="10"/>
  <detail>
    <contact callsign="CALLSIGN"/>
    <precisionlocation geopointsrc="GPS"/>
  </detail>
</event>
```

#### 10.2.2 CoT Type Codes
- `a-f-G-U-U`: Ground unit, unknown, team
- `a-h-G-U-T`: Human, ground, unknown, team
- See ATAK documentation for complete type code list

### 10.3 BLE Interface

#### 10.3.1 Service UUID
- **Service**: Custom UUID (configurable)
- **Characteristics**:
  - CoT Characteristic: Notify only
  - Write Characteristic: Write, Write No Response

#### 10.3.2 Data Format
- **CoT Data**: XML string (UTF-8)
- **Commands**: ASCII string (UTF-8)
- **Status**: ASCII string (UTF-8)

### 10.4 Serial Interface

#### 10.4.1 Physical Interface
- **Connector**: USB Type-A or Micro-USB
- **Protocol**: USB Serial (CDC)
- **Baud Rate**: 115200 (configurable: 9600-921600)

#### 10.4.2 Data Format
- **Line Terminated**: Newline (`\n`)
- **Encoding**: UTF-8
- **Flow Control**: None

---

## 11. TEST PROCEDURES

### 11.1 Pre-Operational Tests

#### 11.1.1 Connection Test
1. Power on both devices
2. Establish connection (BLE or Serial)
3. Verify "Connected" status in toolbar
4. Verify connection logged in audit log
5. **PASS**: Status shows "Connected" within 30 seconds

#### 11.1.2 Battery Status Test
1. Establish connection
2. Wait 30-60 seconds
3. Verify battery percentage displays in toolbar
4. Verify battery status logged in audit log
5. **PASS**: Battery percentage displays correctly

#### 11.1.3 Data Transmission Test
1. Establish connection
2. Send test message via "Send Data" view
3. Verify message sent (toast notification)
4. Verify message logged in audit log
5. **PASS**: Message sent successfully

#### 11.1.4 CoT Reception Test
1. Establish connection
2. Receive CoT data from Meshtastic network
3. Verify markers appear on ATAK map
4. Verify CoT data logged in audit log
5. **PASS**: CoT markers appear correctly

### 11.2 Security Tests

#### 11.2.1 Encryption Test
1. Enable encryption in configuration
2. Send encrypted message
3. Verify message decrypts correctly
4. Verify encryption logged in audit log
5. **PASS**: Encryption/decryption successful

#### 11.2.2 Integrity Test
1. Enable HMAC in configuration
2. Send message with HMAC
3. Verify HMAC validates correctly
4. Attempt to modify message
5. Verify HMAC validation fails
6. **PASS**: HMAC validation works correctly

#### 11.2.3 Input Validation Test
1. Send invalid command format
2. Verify command rejected
3. Verify security violation logged
4. Send injection pattern
5. Verify injection detected
6. **PASS**: Input validation works correctly

### 11.3 Performance Tests

#### 11.3.1 Connection Time Test
1. Power on devices
2. Measure time to "Connected" status
3. **PASS**: Connection time < 30 seconds

#### 11.3.2 Command Response Time Test
1. Send command
2. Measure time to response
3. **PASS**: Response time < 1 second

#### 11.3.3 Throughput Test
1. Send multiple messages rapidly
2. Verify all messages processed
3. **PASS**: No message loss

---

## 12. SAFETY WARNINGS

### 12.1 Electrical Safety
- **WARNING**: Use only approved USB cables and chargers
- **WARNING**: Do not expose device to moisture
- **CAUTION**: Disconnect power before maintenance

### 12.2 Radio Frequency Safety
- **WARNING**: Maintain safe distance from transmitting antennas
- **CAUTION**: Radio emissions may interfere with medical devices
- **NOTE**: Comply with local RF regulations

### 12.3 Data Security
- **WARNING**: Encryption keys must be kept secure
- **WARNING**: Never share encryption keys
- **CAUTION**: Audit logs may contain sensitive information
- **CAUTION**: Secure audit log storage and transmission

### 12.4 Operational Safety
- **WARNING**: SOS alerts are logged and cannot be undone
- **WARNING**: Verify connection before critical operations
- **CAUTION**: Test system before deployment
- **CAUTION**: Maintain backup communication methods

---

## 13. APPENDICES

### Appendix A: Acronyms and Abbreviations
- **ATAK**: Android Tactical Assault Kit
- **BLE**: Bluetooth Low Energy
- **CoT**: Cursor on Target
- **ESP32**: Espressif Systems 32-bit microcontroller
- **HMAC**: Hash-based Message Authentication Code
- **LoRa**: Long Range radio technology
- **MQTT**: Message Queuing Telemetry Transport
- **NVS**: Non-Volatile Storage
- **OTG**: On-The-Go
- **SOS**: Save Our Souls (emergency alert)
- **UUID**: Universally Unique Identifier
- **USB**: Universal Serial Bus

### Appendix B: Error Codes
- **VALIDATION_OK**: Input validation passed
- **VALIDATION_ERROR_NULL**: Null input
- **VALIDATION_ERROR_TOO_LONG**: Input exceeds maximum length
- **VALIDATION_ERROR_INVALID_CHARS**: Invalid characters detected
- **VALIDATION_ERROR_MALFORMED**: Malformed input
- **VALIDATION_ERROR_INJECTION_ATTEMPT**: Injection pattern detected

### Appendix C: Configuration Examples
See Section 6 for configuration examples.

### Appendix D: Contact Information
- **Manufacturer**: Akita Engineering
- **Website**: www.akitaengineering.com
- **Email**: info@akitaengineering.com
- **Support**: support@akitaengineering.com
- **Security**: security@akitaengineering.com

---

## DOCUMENT REVISION HISTORY

| Revision | Date | Description | Author |
|----------|------|-------------|--------|
| 1.0 | 2025-12-31 | Initial release | Akita Engineering |

---

**END OF DOCUMENT**

**Copyright (C) 2025 Akita Engineering. All Rights Reserved.**

