# Introduction

The **Akita MeshTAK Plugin** allows your ATAK device to connect to Meshtastic networks, enabling off-grid communication, location tracking, emergency alerts, and device health monitoring.

---

# System Requirements

- Android device with ATAK installed  
- Meshtastic-compatible device (e.g., Heltec V3) running the Akita MeshTAK firmware  
- Akita MeshTAK Plugin (compiled `.apk` file)

---

# Installation

## 1. Install ATAK
Ensure ATAK is installed on your Android device.

## 2. Install the Plugin APK
- Obtain the compiled `AkitaMeshTAK.apk` file  
- If required, enable **Install from unknown sources** in Android settings  
- Locate the APK on your device and install it  

## 3. Enable the Plugin in ATAK
- Open ATAK  
- Go to: **Toolbar → Settings → Plugin Manager**  
- Locate **Akita MeshTAK** and enable it  

---

# Connecting to Meshtastic

## Configure Connection Settings
Open ATAK Plugin Settings:  
**Toolbar → Settings → Tool Preferences → Akita MeshTAK**

Choose your connection method:

### BLE
- Ensure your Meshtastic device is powered on  
- Enter the BLE Device Name (e.g., `AkitaNode01`)

### Serial
- Connect your device using USB  
- Set Serial Baud Rate (default: **115200**)

---

## Connection Status (Toolbar)

The plugin attempts connection automatically. Status appears in the ATAK toolbar:

- **Method: BLE** or **Method: Serial** — displays selected mode  
- **Connected** (green) — successful connection  
- **Connecting** (yellow) — attempting to connect  
- **Disconnected / Error** (red) — connection failed  

---

# Using the Plugin

## Toolbar Interface

The Akita MeshTAK toolbar provides:

- **Method:** Current connection type (BLE/Serial)  
- **Status:** Real-time connection state  
- **BATT: XX%:** Battery level of the connected Heltec  
  - Green = good  
  - Yellow = low  
  - Red = critical  
- **SOS Button:** Sends a high-priority network-wide emergency alert

---

# Sending Data

1. Open the **Send Data** view (from ATAK overflow menu or toolbar)
2. Enter the message
3. Choose a format:
   - Plain Text  
   - JSON  
   - Custom  
4. Tap **Send**
5. Command history entries can be reused from the dropdown

---

# Receiving Data

The plugin automatically receives:

- **CoT Location Data:** Other Meshtastic users appear as ATAK map markers  
- **Battery Reports:** Toolbar battery indicator updates live  

---

# Troubleshooting

## No Connection
- Ensure the Meshtastic device is running the correct Akita MeshTAK firmware  
- Confirm all UUIDs/USB IDs were correctly set in `Config.java` **before compilation**  
- Re-check BLE device name or serial baud rate in plugin settings  
- Ensure ATAK permissions (Bluetooth, USB) are granted  

## No Battery Status
- Wait 30–60 seconds after connecting  
- Verify firmware sends the `STATUS:BATT:` response correctly  

---

# Support

For assistance, contact **Akita Engineering**:

- Website: **www.akitaengineering.com**  
- Email: **info@akitaengineering.com**
