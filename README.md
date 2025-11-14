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
This project is ready for functional testing, but it requires configuration before it will work.

- **Firmware**: You must set the correct UUIDs in `firmware/src/config.h`.  
- **ATAK Plugin**: You must edit `atak_plugin/src/com/akitaengineering/meshtak/Config.java` and fill in the placeholder UUIDs and USB IDs to match your firmware and hardware.  

---

### Key Features and Benefits
- **Seamless ATAK Integration**: Integrates directly into the ATAK UI.  
- **Long-Range, Low-Power Communication**: Leverages Meshtastic's self-healing mesh network.  
- **Robust Off-Grid Operation**: Thrives in environments where traditional networks fail.  
- **Precision Location Tracking**: Transmits and receives accurate Cursor on Target (CoT) messages.  
- **Critical Alert (SOS) Button**: A dedicated button on the ATAK toolbar to send a high-priority alert across the mesh.  
- **Device Health Monitoring**: Displays the battery percentage of the connected Meshtastic device directly in the ATAK toolbar.  

**Versatile Connectivity Options**:
- Bluetooth Low Energy (BLE)  
- Serial (USB)  
- (Optional) MQTT  

**Intuitive Data Management**:
- Data Format Selection (Plain Text, JSON, Custom)  
- Command History for frequently sent messages  

**Enhanced Situational Awareness**:
- Clear connection status in the toolbar and on the map  
- Map overlay for critical information  

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

### Project Structure
```
AkitaMeshTAK/
├── firmware/ # Firmware for Meshtastic devices (Heltec V3)
│   ├── src/
│   │   ├── config.h # CRITICAL: Set UUIDs and commands here
│   │   ├── main.cpp
│   │   ├── meshtastic_setup.h/.cpp
│   │   ├── ble_setup.h/.cpp
│   │   ├── serial_bridge.h/.cpp
│   │   ├── power_management.h/.cpp # Handles battery & command logic
│   │   └── ...
│   ├── platformio.ini
│   └── README.md
├── atak_plugin/ # Code and resources for the ATAK plugin
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── AndroidManifest.xml
│   │   │   │   ├── java/
│   │   │   │   │   └── com/akitaengineering/meshtak/
│   │   │   │   │       ├── Config.java # CRITICAL: Set UUIDs/VIDs here
│   │   │   │   │       ├── AkitaMeshTAKPlugin.java
│   │   │   │   │       ├── services/
│   │   │   │   │       │   ├── BLEService.java
│   │   │   │   │       │   └── SerialService.java
│   │   │   │   │       └── ui/
│   │   │   │   │           ├── AkitaToolbar.java # Contains SOS/Battery logic
│   │   │   │   │           ├── ConnectionStatusOverlay.java
│   │   │   │   │           ├── SendDataView.java
│   │   │   │   │           └── SettingsFragment.java
│   │   │   │   ├── res/
│   │   │   │   │   ├── layout/
│   │   │   │   │   │   ├── akita_toolbar.xml # Contains SOS/Battery UI
│   │   │   │   │   │   └── send_data_view.xml
│   │   │   │   │   ├── xml/
│   │   │   │   │   │   └── preferences.xml
│   │   │   │   │   └── ...
│   │   ├── build.gradle
│   │   └── ...
│   └── README.md
├── documentation/
│   ├── user_guide.md
│   ├── dev_guide.md
│   └── ...
├── LICENSE     # GPLv3 License
├── COPYING     # Full GPLv3 License Text
└── README.md   # This file
```

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
```
