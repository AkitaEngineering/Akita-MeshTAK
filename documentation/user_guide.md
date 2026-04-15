# Introduction

The **Akita MeshTAK Plugin** allows your ATAK device to connect to Meshtastic networks, enabling off-grid communication, location tracking, emergency alerts, and device health monitoring.

The current operator workflow also includes mission profiles, a mission-assurance dashboard, a guaranteed-delivery mailbox, air-gapped provisioning ceremony controls, tactical ATAK overlay concepts, incident-board role packs, mock transport rehearsal mode, digital-twin replay, and Dark Ops / Light Ops / Night Red / Night Green presentation themes.

---

# System Requirements

- Android device with ATAK installed  
- Meshtastic-compatible device (e.g., Heltec V3) running the Akita MeshTAK firmware (v0.2.0)  
- Meshtastic Arduino library v0.0.7 on the firmware device  
- Akita MeshTAK Plugin (compiled `.apk` file, v0.2.0)

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

## Configure Mission and Security Settings
In the same settings screen, review these operator-facing controls before field use. Security is a go/no-go gate: do not release live traffic until Mission Assurance shows operational encryption and no placeholder-secret warning.

- **Mission Profile**: Select Search & Rescue, Law Enforcement, Coast Guard, Military, or Private Security
- **Dashboard Theme**: Select Dark Ops, Light Ops, Night Red, or Night Green
- **Auto Bearer Failover**: Preserve queued traffic and reroute between BLE and Serial when the preferred bearer is unavailable
- **Security and Provisioning**:
  - Treat **Mission Assurance** as release authority for live traffic; if security is degraded, simulated, or placeholder-backed, stop and remediate before transmitting
  - Confirm **Enable Encrypted Transport** is on for operational use
  - Enter a deployment-specific **Provisioning Secret**; use the build-time fallback only for lab/testing and never for field deployment
  - Use **Rotate Provisioning Secret** to generate a new secret in the plugin
  - Use **Generate Provisioning Bundle** to prepare an offline bundle for another device or operator
  - Use **Apply Provisioning Bundle** to load a staged bundle into the plugin
  - Use **Stage Secret To Connected Device** on a trusted local bearer to runtime-reprovision firmware without a rebuild
  - Use **Export Audit Log** to save the current audit trail
  - Use **Reload Security State** after changing security settings
- **Mock Transport Mode**: Enable when no radio is available and you need to rehearse UI, replay, and workflow behavior

---

## Connection Status (Toolbar)

The plugin attempts connection automatically. Status appears in the ATAK toolbar:

- **Secure route: BLE** or **Secure route: Serial** — displays selected mode and endpoint  
- **Profile** — shows the active mission profile  
- **Security** — shows whether encryption/audit posture is operational, simulated, degraded, or still using a placeholder secret  
- **Connected** (green) — successful connection  
- **Connecting** (yellow) — attempting to connect  
- **Disconnected / Error** (red) — connection failed  

---

# Using the Plugin

## Toolbar Interface

The Akita MeshTAK toolbar provides:

- **Secure Route:** Current connection type and endpoint  
- **Profile:** Current mission profile  
- **Security:** Current provisioning and encryption posture  
- **Status:** Real-time connection state  
- **BATT: XX%:** Battery level of the connected Heltec  
  - Green = good  
  - Yellow = low  
  - Red = critical  
- **SOS Button:** Sends a high-priority network-wide emergency alert

## Mission Dashboard

The **Send Data** view is now a mission dashboard rather than a simple send form.

It includes:

- **Operational Summary**: Route, payload budget, last send, and peer receipt ratio
- **Mission Assurance**: Encryption, audit, interoperability, and provisioning state; treat degraded or placeholder-backed state as a no-transmit condition for live traffic
- **Guaranteed Delivery Mailbox**: Pending / In Flight / Delivered counts, failover posture, and replay checkpoint status
- **Incident Board**: Role-pack aware incident title, tempo, and next action
- **Mission Playbooks**: Profile-specific reusable payload templates
- **Role-Pack Actions**: Quick actions that queue directly into the secure composer
- **Payload Trend / Format Distribution**: Charts for recent traffic and message type mix
- **Rapid Reuse**: History of recent mailbox frames and replayable checkpoints

---

# Sending Data

1. Open the **Send Data** view (from ATAK overflow menu or toolbar)
2. Optionally load a **Mission Playbook** or **Role-Pack Action**
3. Enter or review the message payload
4. Choose a format:
   - Plain Text  
   - JSON  
   - Custom  
5. Review the payload budget, mission-assurance indicators, and mailbox state
6. Confirm Mission Assurance shows operational encryption/provisioning posture with no placeholder-secret warning
7. If security is degraded, simulated, or placeholder-backed, stop and remediate before transmitting live traffic
8. Tap **Transmit** to queue the frame for the active bearer
9. Watch the mailbox move through **Pending**, **In Flight**, and **Delivered**; **Delivered** indicates a peer mailbox receipt returned over the mesh
10. Command history entries can be reused from the dropdown

---

# Receiving Data

The plugin automatically receives:

- **CoT Location Data:** Other Meshtastic users appear as ATAK map markers  
- **Battery Reports:** Toolbar battery indicator updates live  
- **Mailbox Receipts:** Guaranteed-delivery frames advance from `IN_FLIGHT` to `DELIVERED` when a peer mailbox acknowledgement returns  
- **Tactical Overlay Context:** The ATAK map can show a mission geofence, sector arcs, route health, and stale-marker callouts around the active team picture

---

# Security

The plugin supports AES-256-GCM encrypted communication between the firmware and plugin. The plugin now uses a runtime provisioning secret from settings when available, with `Config.PROVISIONING_SECRET` only as a fallback. When **Enable Encrypted Transport** is on and both sides use matching metadata, BLE and serial traffic is protected with authenticated encryption. The provisioning workflow also supports air-gapped bundle generation/apply and a trusted local stage-to-device action for runtime reprovisioning.

Operational priority: encryption and provisioning health take precedence over convenience. If Mission Assurance reports degraded, simulated, or placeholder-backed posture, do not send live mission traffic until the warning is cleared.

Before field use:

- Confirm Mission Assurance reports operational encryption and provisioning posture before traffic release
- Replace any placeholder provisioning secret
- If you rotate secrets in the field, use **Generate Provisioning Bundle**, **Apply Provisioning Bundle**, and **Stage Secret To Connected Device** only on a trusted local route
- Keep encrypted transport enabled unless you are running a controlled compatibility test off-mission
- Export audit logs as required by your operating procedure
- Use mock mode only for rehearsal, not production traffic

---

# Troubleshooting

## No Connection
- Ensure the Meshtastic device is running the correct Akita MeshTAK firmware  
- Confirm all UUIDs/USB IDs were correctly set in `Config.java` **before compilation**  
- Re-check BLE device name or serial baud rate in plugin settings  
- Ensure ATAK permissions (Bluetooth, USB) are granted  

## Security Shows "Rotate Deployment Secret"
- Open **Settings → Tool Preferences → Akita MeshTAK → Security and Provisioning**
- Enter a deployment-specific provisioning secret or use **Rotate Provisioning Secret**
- Use **Reload Security State** after changing the secret
- Do not proceed with live traffic until Mission Assurance clears the warning and encrypted transport remains enabled

## Messages Stay "In Flight"
- Wait for the peer mailbox receipt to return over the mesh
- Confirm at least one peer node is reachable on the mesh
- Review the **Guaranteed Delivery Mailbox** card for pending or failed frames
- Use **Retry Queue** after bearer recovery or enable **Auto Bearer Failover**

## Dashboard Rehearsal Without Hardware
- Enable **Mock Transport Mode** in plugin settings
- Use **Replay Last Mission** to rehearse recent mailbox and provisioning checkpoints
- Review the static preview in `documentation/ui_preview.html` if you need to validate layout and theme choices on a workstation

## No Battery Status
- Wait 30–60 seconds after connecting  
- Verify firmware sends the `STATUS:BATT:` response correctly  

---

# Support

For assistance, contact **Akita Engineering**:

- Website: **www.akitaengineering.com**  
- Email: **support@akitaengineering.com**
