# OPERATOR'S MANUAL
## AKITA MESHTAK SYSTEM
## OPERATOR'S MANUAL

**Document Number:** OM-AKITA-MESHTAK-001  
**Revision:** 1.4  
**Date:** 2026-04-14  
**Classification:** UNCLASSIFIED  
**Prepared By:** Akita Engineering  
**Approved By:** [Approval Authority]

---

## DOCUMENT CONTROL

| Item | Information |
|------|-------------|
| Document Title | Akita MeshTAK System Operator's Manual |
| Document Number | OM-AKITA-MESHTAK-001 |
| Revision | 1.4 |
| Date | 2026-04-14 |
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
- **Mission Profiles**: Adjust workflow for SAR, law enforcement, coast guard, military, or private security
- **Mission Assurance**: Surface encryption, audit, interoperability, and provisioning posture before transmission
- **Guaranteed Delivery Mailbox**: Preserve queued frames, show `IN_FLIGHT` vs peer-delivered state, and support queue retry
- **Bearer Failover**: Preserve queued traffic while rerouting between BLE and Serial when required
- **Air-Gapped Provisioning Ceremony**: Generate, apply, and stage offline provisioning bundles during trusted local access
- **Mission Replay**: Rehearse recent mailbox and provisioning checkpoints in Mock Transport Mode
- **Incident Board**: Keep operational tempo, role pack, and next-action context visible
- **Tactical Map Layer**: Show route health, geofence, sectors, and stale-marker warnings on the ATAK map
- **Rehearsal and Theme Modes**: Support Mock Transport Mode plus Dark Ops, Light Ops, Night Red, and Night Green displays

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
  - **Mission Profile**: Select the operational workflow for your team
  - **Dashboard Theme**: Select Dark Ops, Light Ops, Night Red, or Night Green
  - **Auto Bearer Failover**: Preserve queued traffic across BLE and Serial when the preferred bearer is unavailable
  - **Security and Provisioning**: Treat this as a go/no-go gate for live traffic. Confirm encrypted transport is enabled, generate/apply bundle material as needed, stage the connected device only on a trusted local route, and replace any placeholder provisioning secret before live operations
  - **Mock Transport Mode**: Enable only for rehearsal without hardware or for replay drills

### 3.2 Daily Startup

#### For BLE Connection:
1. Power on Meshtastic device
2. Wait for device to initialize (LED indicators)
3. Power on Android device
4. Open ATAK
5. Wait for connection (status shows "Connected" in green)
6. Review Mission Assurance and confirm encrypted transport is enabled and there is no placeholder-secret warning for deployment use
7. Do not transmit live traffic until Step 6 is satisfied

#### For Serial Connection:
1. Power on Meshtastic device
2. Connect USB cable between devices
3. Grant USB permission if prompted
4. Power on Android device
5. Open ATAK
6. Wait for connection (status shows "Connected" in green)
7. Review Mission Assurance and confirm the route and security posture are operational with encrypted transport enabled
8. Do not transmit live traffic until Step 7 is satisfied

---

## 4. OPERATIONAL PROCEDURES

### 4.1 Monitoring System Status

#### 4.1.1 Toolbar Display
The Akita MeshTAK toolbar shows:
- **Secure Route**: BLE or Serial plus the active endpoint
- **Profile**: Current mission profile
- **Security**: Current provisioning and encrypted transport posture
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
2. Review the **Operational Summary**, **Mission Assurance**, **Guaranteed Delivery Mailbox**, and **Incident Board** cards
3. Treat **Mission Assurance** as release authority for live traffic; if security is degraded, simulated, or placeholder-backed, stop and remediate before proceeding
4. Optionally load a **Mission Playbook** or **Queue Action** from the active role pack
5. Enter or review your message in the text field
6. Select data format:
   - **Plain Text**: Standard text message
   - **JSON**: Structured data
   - **Custom**: Custom format
7. Confirm payload budget, failover posture, mailbox state, and operational encryption/provisioning posture
8. Tap **Transmit** button to queue and dispatch the frame
9. Verify the frame advances to **In Flight**; **Delivered** indicates a peer mailbox receipt

#### 4.2.2 Command History
- Previously sent commands appear in dropdown
- Select from history to resend
- History persists during session

### 4.3 Receiving Data

#### 4.3.1 Location Data (CoT)
- Team member locations appear automatically on ATAK map
- Markers update in real-time
- Tap marker for details
- Tactical overlay may additionally show route health, mission geofence, sector arcs, and stale-marker warnings

#### 4.3.2 Status Updates
- Battery status updates every 30-60 seconds
- Connection status updates in real-time
- Mailbox acknowledgements and provisioning status updates surface in the dashboard workflow
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
- Displays: "Secure route: BLE" or "Secure route: Serial"
- Shows current connection type and active route
- Updates when connection method changes

#### 5.1.2 Connection Status Indicator
- **BLE: Connected** (green): BLE connection active
- **BLE: Connecting** (yellow): Establishing BLE connection
- **BLE: Disconnected** (red): BLE connection failed
- **Serial: Connected** (green): Serial connection active
- **Serial: Connecting** (yellow): Establishing Serial connection
- **Serial: Disconnected** (red): Serial connection failed

#### 5.1.3 Mission and Security Indicators
- **Profile**: Current mission profile badge and workflow context
- **Security**: Provisioning and encrypted transport posture; degraded or placeholder-backed state is a no-transmit warning for live operations
- **Secure Link**: Readiness summary for the active route

#### 5.1.4 Battery Status Indicator
- **BATT: XX%**: Battery percentage
- Color coding:
  - Green: > 50% (good)
  - Yellow: 20-50% (low)
  - Red: < 20% (critical)

#### 5.1.5 SOS Button
- Red button labeled "SOS"
- Sends emergency alert when tapped
- **Use only in emergency situations**

### 5.2 Settings Interface

#### 5.2.1 Accessing Settings
1. Open ATAK
2. Navigate to: **Settings → Tool Preferences → Akita MeshTAK**

#### 5.2.2 Settings Options
- **Connection Method**: BLE or Serial
- **Mission Profile**: Search & Rescue, Law Enforcement, Coast Guard, Military, or Private Security
- **Dashboard Theme**: Dark Ops, Light Ops, Night Red, Night Green
- **Auto Bearer Failover**: Preserve queue and reroute between BLE/Serial when possible
- **BLE Device Name**: Device identifier for BLE
- **Serial Baud Rate**: Communication speed (default: 115200)
- **Enable Encrypted Transport**: Enables or disables protected payload transport; keep enabled for all operational traffic
- **Provisioning Secret**: Runtime deployment secret for the plugin; placeholder values are rehearsal-only and not authorized for live traffic
- **Air-Gapped Provisioning Bundle**: Staged bundle text field
- **Rotate Provisioning Secret**: Generates a new plugin-side secret
- **Generate Provisioning Bundle**: Creates an offline bundle from the active secret
- **Apply Provisioning Bundle**: Loads staged bundle material into the plugin security profile
- **Stage Secret To Connected Device**: Sends the trusted local runtime provisioning command over plaintext on a trusted local bearer; use only during a controlled provisioning ceremony
- **Export Audit Log**: Saves the Android audit trail to file
- **Reload Security State**: Re-applies current security settings to live services before traffic release
- **Mock Transport Mode**: Rehearsal mode without hardware
- **Send Test Message**: Test connection

### 5.3 Send Data Interface

#### 5.3.1 Accessing Send Data View
- From ATAK overflow menu
- Or from toolbar (if configured)

#### 5.3.2 Interface Elements
- **Data Format Spinner**: Select format (Plain Text, JSON, Custom)
- **Mission Playbook Spinner**: Load profile-specific preset traffic
- **Queue Action Spinner**: Load role-pack actions from the incident board
- **Data Input Field**: Enter message text
- **Operational Summary**: Route, payload budget, last send, and peer receipt ratio
- **Mission Assurance**: Encryption, audit, interoperability, and provisioning posture
- **Guaranteed Delivery Mailbox**: Pending / In Flight / Delivered counts, failover posture, and replay checkpoints
- **Incident Board**: Incident title, role pack, tempo, and next action
- **Payload/Format Charts**: Recent payload trend and data-format distribution
- **Command History Spinner**: Select previous command
- **Replay Last Mission**: Rehearse stored mailbox checkpoints in Mock Transport Mode
- **Transmit Button**: Queue and dispatch message

### 5.4 Map Overlay

#### 5.4.1 Connection Status Overlay
- Displays connection status on map
- Shows BLE and Serial status
- Updates in real-time

#### 5.4.2 Tactical Mission Overlay
- Displays a mission geofence around the active operating area
- Displays sector arcs for search, containment, or control operations
- Displays route-health context for the active transport
- Displays stale-marker alerts when tracked nodes stop updating

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
2. Review the **Guaranteed Delivery Mailbox** for pending, failed, or in-flight frames
3. Check message format and payload budget
4. Verify device has power
5. Try sending test message or **Retry Queue**
6. Restart connection or enable **Auto Bearer Failover**
7. Review Mission Assurance for provisioning or encryption warnings

#### Problem: Frames Remain In Flight
**Symptoms**: Message leaves the device but never reaches peer-delivered state

**Solutions**:
1. Confirm a peer node is available on the mesh
2. Allow time for the peer mailbox acknowledgement to return
3. Review failover posture and restore the preferred bearer if needed
4. Use **Replay Last Mission** in Mock Transport Mode to rehearse the last sequence

#### Problem: Security Shows "Rotate Deployment Secret"
**Symptoms**: Mission Assurance or toolbar shows degraded provisioning posture

**Solutions**:
1. Open **Settings → Tool Preferences → Akita MeshTAK**
2. Enter a deployment-specific provisioning secret or use **Rotate Provisioning Secret**
3. Confirm **Enable Encrypted Transport** is enabled
4. Tap **Reload Security State**
5. Re-check Mission Assurance before field use
6. Do not resume live traffic until the warning clears

#### Problem: Tactical Overlay Not Visible
**Symptoms**: Team markers appear but no mission layer context is visible

**Solutions**:
1. Confirm ATAK map view is active
2. Verify CoT traffic is being received
3. Check that at least one team marker updated recently
4. Review connection and security posture for transport issues

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
- **Operational Priority**: Security and encrypted transport take precedence over convenience. If Mission Assurance is degraded, simulated, or placeholder-backed, do not transmit live mission data.
- **Encryption Policy**: The Android plugin uses the active **Enable Encrypted Transport** setting and runtime provisioning secret from settings, with a build-time fallback only if no runtime secret is present. Placeholder secrets are acceptable for rehearsal only.
- **Metadata Match Required**: Firmware and plugin must use matching provisioning secret and encrypted envelope metadata (`version`, `key-id`).
- **Runtime Staging**: **Stage Secret To Connected Device** intentionally uses plaintext over a trusted local bearer. Use it only during controlled provisioning ceremonies.

#### CAUTIONS
- **Network Security**: Be aware of network security implications
- **Data Transmission**: Sensitive data is transmitted via AES-256-GCM envelopes (`ENC:v1:k1:<hex>`). If metadata mismatches, payloads are rejected.

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
- [ ] Mission profile verified
- [ ] Mission Assurance reviewed
- [ ] Encrypted transport enabled for live traffic
- [ ] No placeholder or degraded security warning present
- [ ] Battery status displaying

#### Daily Operations Checklist
- [ ] Verify connection status
- [ ] Verify mission profile and role pack
- [ ] Review Mission Assurance
- [ ] Confirm security posture remains operational before traffic release
- [ ] Check battery level
- [ ] Test message sending
- [ ] Verify location data receiving
- [ ] Review tactical overlay for warnings
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
| 1.4 | 2026-04-14 | Added guaranteed-delivery mailbox, bearer failover, air-gapped provisioning ceremony, mission replay, and Night Green operator guidance | Akita Engineering |
| 1.3 | 2026-04-14 | Added mission assurance, incident board, tactical overlay, runtime provisioning, and updated operator workflow | Akita Engineering |
| 1.2 | 2026-03-13 | Corrected encryption default state for Android plugin; added key provisioning references | Akita Engineering |
| 1.1 | 2026-03-12 | Updated operator security guidance for AES-256-GCM, versioned encrypted envelopes, and key-id alignment checks | Akita Engineering |
| 1.0 | 2025-12-31 | Initial release | Akita Engineering |

---

**END OF DOCUMENT**

**Copyright (C) 2026 Akita Engineering. All Rights Reserved.**

