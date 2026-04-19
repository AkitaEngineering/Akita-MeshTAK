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

- **Firmware**: Prefer build-time environment variables over source edits. `firmware/tools/load_build_config.py` injects deployment values such as BLE UUIDs, device ID, provisioning secret, and MQTT credentials. Placeholder UUIDs, provisioning material, and MQTT credentials still fail the production firmware build unless you explicitly define `ALLOW_PLACEHOLDER_SECRET` or use the `heltec_v3_ci` rehearsal target.  
- **ATAK Plugin**: Do not edit `atak_plugin/src/com/akitaengineering/meshtak/Config.java` for deployment values. Supply provisioning secret, BLE UUIDs, USB IDs, and the ATAK SDK jar path through Gradle properties or environment variables as documented in `atak_plugin/README.md` and `atak_plugin/ATAK_SDK_REQUIREMENTS.md`.  
- **Java Runtime**: Android builds require Java 17 or 21. Gradle/AGP validation is not reliable on Java 26 at the time of writing.  
- **Provisioning Secret**: Operators can generate an air-gapped provisioning bundle, apply it locally, and stage the active secret to a connected device over a trusted local bearer. Placeholder secrets are acceptable for rehearsal only and are surfaced as degraded posture in Mission Assurance.  

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
- PBKDF2-HMAC-SHA256 transport key derivation from provisioning material with device/purpose salt  
- Preference-backed provisioning secret management with runtime-first behavior and guarded build-time fallback  
- Air-gapped provisioning bundle generation/apply plus trusted local stage-to-device workflow  
- Encrypted transport enable/disable control from settings  
- Firmware placeholder guards for provisioning, BLE UUIDs, and MQTT credentials  
- Audit log export, debug-gated firmware serial mirroring, and security state reload actions  
- BLE/Serial command rate limiting to blunt command-flood attempts  

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
- **Release Process**: Coordinated firmware/plugin release steps, inputs, and artifact expectations
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
© 2026 Akita Engineering  

---

### Contact
- **Website**: [www.akitaengineering.com](http://www.akitaengineering.com)
- **Support**: support@akitaengineering.com
- **Security**: security@akitaengineering.com
