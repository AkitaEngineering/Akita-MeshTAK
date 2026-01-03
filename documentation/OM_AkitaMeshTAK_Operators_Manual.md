# OPERATOR'S MANUAL
## AKITA MESHTAK SYSTEM
## OPERATOR'S MANUAL

**Document Number:** OM-AKITA-MESHTAK-001  
**Revision:** 1.0  
**Date:** 2025-12-31  
**Classification:** UNCLASSIFIED  
**Prepared By:** Akita Engineering  
**Approved By:** [Approval Authority]

---

## DOCUMENT CONTROL

| Item | Information |
|------|-------------|
| Document Title | Akita MeshTAK System Operator's Manual |
| Document Number | OM-AKITA-MESHTAK-001 |
| Revision | 1.0 |
| Date | 2025-01-XX |
| Classification | UNCLASSIFIED |
| Distribution | As Required |
| Supersedes | None |
| Status | APPROVED FOR USE |

---

## TABLE OF CONTENTS

1. [INTRODUCTION](#1-introduction)
2. [SYSTEM OVERVIEW](#2-system-overview)
3. [QUICK START GUIDE](#3-quick-start-guide)
4. [OPERATIONAL PROCEDURES](#4-operational-procedures)
5. [USER INTERFACE](#5-user-interface)
6. [TROUBLESHOOTING](#6-troubleshooting)
7. [SAFETY INFORMATION](#7-safety-information)
8. [APPENDICES](#8-appendices)

---

## 1. INTRODUCTION

### 1.1 Purpose
This Operator's Manual provides instructions for operating the Akita MeshTAK System. This manual is intended for operators who will use the system in field operations.

### 1.2 Scope
This manual covers:
- System overview
- Quick start procedures
- Operational procedures
- User interface description
- Troubleshooting
- Safety information

### 1.3 Related Documents
- TM-AKITA-MESHTAK-001: Technical Manual
- TM-AKITA-MESHTAK-004: Security Guide

---

## 2. SYSTEM OVERVIEW

### 2.1 What is Akita MeshTAK?
Akita MeshTAK is a secure communication system that connects your ATAK (Android Tactical Assault Kit) device to Meshtastic mesh networks. This enables off-grid communication and situational awareness when traditional networks are unavailable.

### 2.2 System Components
- **Meshtastic Device**: Radio device that connects to mesh network
- **Android Device**: Your smartphone or tablet running ATAK
- **Akita MeshTAK Plugin**: Software that connects ATAK to Meshtastic

### 2.3 Key Features
- **Off-Grid Communication**: Works without cellular or WiFi
- **Location Tracking**: See team members on ATAK map
- **Emergency Alerts**: Send SOS alerts across network
- **Battery Monitoring**: Monitor device battery level
- **Secure Communication**: Encrypted and authenticated

---

## 3. QUICK START GUIDE

### 3.1 Initial Setup (One-Time)

#### Step 1: Install Plugin
1. Obtain AkitaMeshTAK.apk file
2. Transfer to Android device
3. Install APK (enable "Install from Unknown Sources" if needed)
4. Open ATAK
5. Go to: **Settings → Plugin Manager**
6. Enable "Akita MeshTAK"

#### Step 2: Configure Connection
1. Open ATAK
2. Go to: **Settings → Tool Preferences → Akita MeshTAK**
3. Select connection method:
   - **BLE**: For wireless connection
   - **Serial**: For USB cable connection
4. Configure settings:
   - **BLE Device Name**: Enter device name (e.g., "AkitaNode01")
   - **Serial Baud Rate**: Leave at 115200 (default)

### 3.2 Daily Startup

#### For BLE Connection:
1. Power on Meshtastic device
2. Wait for device to initialize (LED indicators)
3. Power on Android device
4. Open ATAK
5. Wait for connection (status shows "Connected" in green)

#### For Serial Connection:
1. Power on Meshtastic device
2. Connect USB cable between devices
3. Grant USB permission if prompted
4. Power on Android device
5. Open ATAK
6. Wait for connection (status shows "Connected" in green)

---

## 4. OPERATIONAL PROCEDURES

### 4.1 Monitoring System Status

#### 4.1.1 Toolbar Display
The Akita MeshTAK toolbar shows:
- **Method**: BLE or Serial (connection type)
- **Status**: 
  - 🟢 **Connected** (green): System ready
  - 🟡 **Connecting** (yellow): Establishing connection
  - 🔴 **Disconnected/Error** (red): Connection failed
- **BATT**: Battery percentage
  - 🟢 Green: > 50% (good)
  - 🟡 Yellow: 20-50% (low)
  - 🔴 Red: < 20% (critical)

#### 4.1.2 Connection Status
- Check toolbar for connection status
- Status updates automatically
- If disconnected, system will attempt to reconnect

### 4.2 Sending Messages

#### 4.2.1 Send Data View
1. Open "Send Data" view from ATAK menu
2. Enter your message in the text field
3. Select data format:
   - **Plain Text**: Standard text message
   - **JSON**: Structured data
   - **Custom**: Custom format
4. Tap **Send** button
5. Verify message sent (toast notification appears)

#### 4.2.2 Command History
- Previously sent commands appear in dropdown
- Select from history to resend
- History persists during session

### 4.3 Receiving Data

#### 4.3.1 Location Data (CoT)
- Team member locations appear automatically on ATAK map
- Markers update in real-time
- Tap marker for details

#### 4.3.2 Status Updates
- Battery status updates every 30-60 seconds
- Connection status updates in real-time
- Error messages appear in toolbar

### 4.4 Emergency Procedures

#### 4.4.1 Sending SOS Alert
**WARNING**: SOS alerts are logged and broadcast network-wide. Use only in emergency situations.

1. Locate **SOS** button in toolbar
2. Tap **SOS** button
3. Confirm alert sent (notification appears)
4. Alert broadcasts across entire Meshtastic network
5. Event is logged in audit system

#### 4.4.2 Receiving SOS Alert
- SOS alerts appear as high-priority messages
- Audio/visual alerts may be configured in ATAK
- Respond according to operational procedures

### 4.5 Shutdown Procedures

#### 4.5.1 Normal Shutdown
1. Close ATAK application
2. System automatically disconnects
3. Disconnect USB cable (if Serial connection)
4. Power off Meshtastic device

#### 4.5.2 Emergency Shutdown
1. Power off Android device immediately (if necessary)
2. Power off Meshtastic device
3. Report incident per operational procedures

---

## 5. USER INTERFACE

### 5.1 Toolbar Elements

#### 5.1.1 Connection Method Indicator
- Displays: "Method: BLE" or "Method: Serial"
- Shows current connection type
- Updates when connection method changes

#### 5.1.2 Connection Status Indicator
- **BLE: Connected** (green): BLE connection active
- **BLE: Connecting** (yellow): Establishing BLE connection
- **BLE: Disconnected** (red): BLE connection failed
- **Serial: Connected** (green): Serial connection active
- **Serial: Connecting** (yellow): Establishing Serial connection
- **Serial: Disconnected** (red): Serial connection failed

#### 5.1.3 Battery Status Indicator
- **BATT: XX%**: Battery percentage
- Color coding:
  - Green: > 50% (good)
  - Yellow: 20-50% (low)
  - Red: < 20% (critical)

#### 5.1.4 SOS Button
- Red button labeled "SOS"
- Sends emergency alert when tapped
- **Use only in emergency situations**

### 5.2 Settings Interface

#### 5.2.1 Accessing Settings
1. Open ATAK
2. Navigate to: **Settings → Tool Preferences → Akita MeshTAK**

#### 5.2.2 Settings Options
- **Connection Method**: BLE or Serial
- **BLE Device Name**: Device identifier for BLE
- **Serial Baud Rate**: Communication speed (default: 115200)
- **Send Test Message**: Test connection

### 5.3 Send Data Interface

#### 5.3.1 Accessing Send Data View
- From ATAK overflow menu
- Or from toolbar (if configured)

#### 5.3.2 Interface Elements
- **Data Format Spinner**: Select format (Plain Text, JSON, Custom)
- **Data Input Field**: Enter message text
- **Command History Spinner**: Select previous command
- **Send Button**: Send message

### 5.4 Map Overlay

#### 5.4.1 Connection Status Overlay
- Displays connection status on map
- Shows BLE and Serial status
- Updates in real-time

---

## 6. TROUBLESHOOTING

### 6.1 Connection Problems

#### Problem: Cannot Connect (BLE)
**Symptoms**: Status shows "Disconnected" or "Error"

**Solutions**:
1. Verify BLE is enabled on Android device
2. Check device is within range (10-50 meters)
3. Verify device name matches in settings
4. Restart both devices
5. Check battery level on Meshtastic device

#### Problem: Cannot Connect (Serial)
**Symptoms**: Status shows "Disconnected" or "Error"

**Solutions**:
1. Verify USB cable is connected
2. Grant USB permission when prompted
3. Try different USB cable
4. Check USB port on Android device
5. Restart both devices

### 6.2 Data Problems

#### Problem: No Location Data on Map
**Symptoms**: Team members not appearing on map

**Solutions**:
1. Verify Meshtastic network connectivity
2. Check connection status in toolbar
3. Verify other devices are on same network
4. Check ATAK map settings
5. Restart ATAK application

#### Problem: Messages Not Sending
**Symptoms**: Send button does nothing or error message

**Solutions**:
1. Verify connection status (must be "Connected")
2. Check message format
3. Verify device has power
4. Try sending test message
5. Restart connection

### 6.3 Battery Problems

#### Problem: Battery Status Not Updating
**Symptoms**: Battery shows "--%" or old value

**Solutions**:
1. Wait 30-60 seconds after connection
2. Verify connection is active
3. Send battery query manually (if possible)
4. Restart connection

### 6.4 Performance Problems

#### Problem: Slow Connection
**Symptoms**: Connection takes > 30 seconds

**Solutions**:
1. Move to area with less interference
2. Check battery level
3. Reduce distance between devices (BLE)
4. Check USB cable quality (Serial)
5. Restart both devices

---

## 7. SAFETY INFORMATION

### 7.1 Operational Safety

#### WARNINGS
- **SOS Alerts**: SOS alerts are logged and broadcast network-wide. Use only in genuine emergency situations.
- **Connection Verification**: Always verify connection status before critical operations.
- **Backup Communication**: Maintain backup communication methods. Do not rely solely on this system.

#### CAUTIONS
- **Testing**: Test system before deployment in critical operations
- **Battery Level**: Monitor battery level. Low battery may cause connection loss.
- **Environmental Conditions**: System performance may degrade in extreme conditions.

### 7.2 Security Safety

#### WARNINGS
- **Encryption Keys**: Never share encryption keys with unauthorized personnel
- **Audit Logs**: Audit logs may contain sensitive information. Secure storage required.
- **Device Security**: Secure devices physically. Report lost or stolen devices immediately.
- **Encryption Default**: Encryption is off by default for compatibility. Enable only after keys are provisioned and a secure exchange method is in place; otherwise traffic is plaintext.

#### CAUTIONS
- **Network Security**: Be aware of network security implications
- **Data Transmission**: Sensitive data may be transmitted. Verify encryption is enabled.

### 7.3 Physical Safety

#### WARNINGS
- **USB Cables**: Use only approved USB cables. Damaged cables may cause injury.
- **Moisture**: Do not expose devices to moisture. Water damage may cause malfunction.

#### CAUTIONS
- **Temperature**: Extreme temperatures may affect performance
- **Handling**: Handle devices with care. Dropping may cause damage.

---

## 8. APPENDICES

### Appendix A: Quick Reference Card

#### Connection Checklist
- [ ] Meshtastic device powered on
- [ ] Android device powered on
- [ ] ATAK application running
- [ ] Plugin enabled in ATAK
- [ ] Connection method selected (BLE/Serial)
- [ ] Connection status shows "Connected" (green)
- [ ] Battery status displaying

#### Daily Operations Checklist
- [ ] Verify connection status
- [ ] Check battery level
- [ ] Test message sending
- [ ] Verify location data receiving
- [ ] Review for errors

### Appendix B: Status Indicator Reference

| Indicator | Color | Meaning | Action |
|-----------|-------|---------|--------|
| Connected | Green | System ready | Normal operations |
| Connecting | Yellow | Establishing connection | Wait for connection |
| Disconnected | Red | Connection failed | Troubleshoot connection |
| Battery > 50% | Green | Good battery | Normal operations |
| Battery 20-50% | Yellow | Low battery | Charge soon |
| Battery < 20% | Red | Critical battery | Charge immediately |

### Appendix C: Common Commands

| Command | Purpose | Response |
|---------|---------|----------|
| CMD:GET_BATT | Request battery status | STATUS:BATT:XX% |
| CMD:GET_VERSION | Request firmware version | STATUS:VERSION:X.Y.Z |
| CMD:ALERT:SOS | Send emergency alert | Alert broadcast |

### Appendix D: Contact Information

**Support**:
- Email: support@akitaengineering.com
- Website: www.akitaengineering.com

**Emergency**:
- Follow operational procedures for emergency contacts

---

## DOCUMENT REVISION HISTORY

| Revision | Date | Description | Author |
|----------|------|-------------|--------|
| 1.0 | 2025-12-31 | Initial release | Akita Engineering |

---

**END OF DOCUMENT**

**Copyright (C) 2025 Akita Engineering. All Rights Reserved.**

