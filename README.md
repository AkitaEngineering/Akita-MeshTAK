# Akita MeshTAK

## Empowering Off-Grid Communication and Situational Awareness for Critical Operations

### Overview
The **Akita MeshTAK Plugin** is a game-changing Android Tactical Assault Kit (ATAK) plugin meticulously crafted to bridge the gap between ATAK and Meshtastic, providing unparalleled communication capabilities in the most challenging and disconnected environments.  

This plugin is an indispensable asset for law enforcement, military, security personnel, and first responders who demand reliable, long-range communication when traditional networks are unavailable, unreliable, or deliberately disrupted.

By harnessing the power of Meshtastic's decentralized, low-power radio networks, Akita MeshTAK empowers ATAK users to maintain critical connectivity and situational awareness, enabling them to:

- **Operate Beyond the Grid**: Extend the reach of ATAK far beyond the limitations of cellular, Wi-Fi, and satellite networks.  
- **Enhance Team Coordination**: Facilitate the real-time sharing of location data (CoT) and text messages.  
- **Visualize the Battlefield**: Overlay Meshtastic-derived data directly onto the ATAK map.  
- **Mitigate Communication Vulnerabilities**: Reduce reliance on easily compromised infrastructure.  

---

### IMPORTANT: READ BEFORE COMPILING

- **Firmware**: You must set the correct UUIDs in `firmware/src/config.h`.  
- **ATAK Plugin**: You must edit `atak_plugin/src/com/akitaengineering/meshtak/Config.java` and fill in the placeholder UUIDs and USB IDs to match your firmware and hardware.  
- **Provisioning Secret**: The plugin can now use a runtime provisioning secret from **Settings → Tool Preferences → Akita MeshTAK → Security and Provisioning**. If you do not provide one there, the plugin falls back to `Config.PROVISIONING_SECRET`. Operators can also generate an air-gapped provisioning bundle, apply it locally, and stage the active secret to a connected device over a trusted local bearer. A placeholder secret is acceptable for rehearsal only and is not deployment-ready.  

---

### Key Features and Benefits
- **Seamless ATAK Integration**: Integrates directly into the ATAK UI.  
- **Long-Range, Low-Power Communication**: Leverages Meshtastic's self-healing mesh network.  
- **Robust Off-Grid Operation**: Thrives in environments where traditional networks fail.  
- **Precision Location Tracking**: Transmits and receives accurate Cursor on Target (CoT) messages.  
- **Critical Alert (SOS) Button**: A dedicated button on the ATAK toolbar to send a high-priority alert across the mesh.  
- **Device Health Monitoring**: Displays the battery percentage of the connected Meshtastic device directly in the ATAK toolbar.  
- **Mission Profiles**: Tailor the workflow for Search and Rescue, Law Enforcement, Coast Guard, Military, or Private Security operations.  
- **Mission Assurance Dashboard**: Surface encryption, audit, interoperability, and provisioning posture before release of field traffic.  
- **Guaranteed Delivery Mailbox**: Queue mission traffic until a bearer is available, transition frames to `IN_FLIGHT` when the radio accepts them, and close delivery only when a peer mailbox receipt returns across the mesh.  
- **Bearer Failover With Queue Preservation**: Preserve queued traffic and reroute between BLE and Serial when the preferred bearer is unavailable.  
- **Air-Gapped Provisioning Ceremony**: Generate and apply offline provisioning bundles, then runtime-stage the active secret to a connected device over a trusted local route.  
- **Mission Replay and Digital Twin**: Rehearse the last mailbox timeline in Mock Transport Mode, including queued frames, peer acknowledgements, and provisioning events.  
- **Incident Board and Role Packs**: Queue profile-specific quick actions and mission playbooks directly into the secure composer.  
- **Tactical ATAK Map Layer**: Adds route-health context, mission geofences, search sectors, and stale-marker callouts directly to the ATAK map.  
- **Operational Themes**: Dark Ops, Light Ops, strict monochrome Night Red, and strict monochrome Night Green modes are available for different field environments.  
- **No-Hardware Rehearsal Mode**: Mock transport mode, mission replay, and the static UI preview allow workflow validation without a radio on hand.  

**Versatile Connectivity Options**:
- Bluetooth Low Energy (BLE)  
- Serial (USB)  
- (Optional) MQTT  

**Intuitive Data Management**:
- Data Format Selection (Plain Text, JSON, Custom)  
- Guaranteed-delivery mailbox queue with Pending / In Flight / Delivered states  
- Command History for frequently sent messages  
- Mission playbook templates and role-pack quick actions  
- Rapid reuse plus replay checkpoints for mock-mode digital twin rehearsal  

**Enhanced Situational Awareness**:
- Clear connection status in the toolbar and on the map  
- Mission assurance telemetry and tactical map overlays for critical information  

**Security and Accountability**:
- Preference-backed provisioning secret management with build-time fallback  
- Air-gapped provisioning bundle generation/apply plus trusted local stage-to-device workflow  
- Encrypted transport enable/disable control from settings  
- Audit log export and security state reload actions  

**Reliable Performance**:
- Robust error handling, reconnection logic, and connection timeouts  

---

### Target Audience
The Akita MeshTAK Plugin is an indispensable tool for:

- **Law Enforcement**: Tactical teams, search and rescue units, and special operations.  
- **Military**: Dismounted infantry, reconnaissance units, and special operations forces.  
- **Security Personnel**: Perimeter control, critical infrastructure protection.  
- **First Responders**: Firefighters, paramedics, and emergency response teams.  
- **Search and Rescue Teams**: Enables effective tracking and coordination.  

---

### Documentation
Comprehensive MILSPEC documentation is available in the `documentation/` directory:

- **Technical Manual (TM)**: Complete technical documentation for installation, configuration, operation, and maintenance
- **Operator's Manual (OM)**: User-friendly guide for field operators
- **System Specification (SS)**: Detailed system requirements and specifications
- **Security Guide**: Comprehensive security documentation
- **Developer Guide**: Information for developers and contributors
- **UI Preview**: `documentation/ui_preview.html` provides a no-hardware visualization of the current toolbar and dashboard experience

---

### Contributing
We welcome contributions! This project is designed to be contributor-friendly. Please see the `documentation/dev_guide.md` for details on how to get started, the command protocol, and our coding standards.  

---

### License
This project is licensed under the **GNU General Public License v3.0**.  
See the LICENSE and COPYING files for the full license text.  

---

### Copyright
© 2025 Akita Engineering  

---

### Contact
- **Website**: [www.akitaengineering.com](http://www.akitaengineering.com)
- **Support**: support@akitaengineering.com
- **Security**: security@akitaengineering.com
