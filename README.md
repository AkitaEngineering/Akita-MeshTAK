# Akita MeshTAK

**Empowering Off-Grid Communication and Situational Awareness for Critical Operations**

## Overview

The Akita MeshTAK Plugin is a game-changing Android Tactical Assault Kit (ATAK) plugin created to bridge the gap between ATAK and Meshtastic, providing unparalleled communication capabilities in the most challenging and disconnected environments. This plugin is an indispensable asset for law enforcement, military, security personnel, and first responders who demand reliable, long-range communication when traditional networks are unavailable, unreliable, or deliberately disrupted.

By harnessing the power of Meshtastic's decentralized, low-power radio networks, Akita MeshTAK empowers ATAK users to maintain critical connectivity and situational awareness, enabling them to:

* **Operate Beyond the Grid:** Extend the reach of ATAK far beyond the limitations of cellular, Wi-Fi, and satellite networks, ensuring seamless communication in remote, austere, or denied areas.
* **Enhance Team Coordination and Collaboration:** Facilitate the real-time sharing of location data, text messages, and other essential information, enabling cohesive team operations and improved decision-making.
* **Visualize the Battlefield:** Overlay Meshtastic-derived data directly onto the ATAK map, providing a comprehensive and intuitive understanding of the operational environment.
* **Mitigate Communication Vulnerabilities:** Reduce reliance on easily compromised or overloaded communication infrastructure, enhancing operational security and resilience.
* **Increase Operational Tempo and Effectiveness:** Streamline communication workflows, accelerate response times, and improve the overall effectiveness of tactical operations.

## Key Features and Benefits

* **Seamless ATAK Integration**: The plugin integrates directly into the ATAK interface, providing a unified and intuitive user experience. Users can access and utilize Meshtastic functionality without the need to switch between separate applications, minimizing distractions and maximizing focus.
* **Long-Range, Low-Power Communication**: Meshtastic devices create self-healing, mesh networks capable of spanning several kilometers, and extending even further with multiple nodes. This enables communication over vast distances with minimal power consumption, crucial for extended operations in the field.
* **Robust Off-Grid Operation**: Akita MeshTAK thrives in environments where traditional networks fail. Whether due to natural disasters, infrastructure damage, or deliberate disruption, this plugin ensures that communication remains possible.
* **Precision Location Tracking**: Transmit and receive accurate location data using Cursor on Target (CoT) messages. This allows for precise tracking of team members, assets, and points of interest, enhancing situational awareness and coordination.
* **Flexible Message Exchange**: Send and receive text messages for secure and discreet communication, enabling the exchange of critical information, updates, and commands.
* **Versatile Connectivity Options**:
    * Bluetooth Low Energy (BLE): Enables direct, peer-to-peer connections between Android devices running ATAK and nearby Meshtastic devices, ideal for small team operations.
    * Serial (USB): Provides a reliable, tethered connection for situations where devices are in close proximity, such as command posts or vehicles.
    * MQTT (Optional): Supports integration with Meshtastic networks via an MQTT bridge, allowing for connection to larger Meshtastic deployments or integration with other communication systems.
* **Intuitive Data Management**:
    * Data Format Selection: Choose the format of your data (Plain Text, JSON, Custom) for flexible communication and interoperability with other systems.
    * Command History: Quickly access and resend frequently used commands, streamlining communication and reducing the need for repetitive input.
* **Enhanced Situational Awareness**:
    * Clear Communication Status: Instantly view the connection status of your devices (BLE, Serial) in the ATAK toolbar and on the map, providing immediate feedback on network connectivity.
    * Map Overlay: Displays critical information directly on the ATAK map.
* **Robust Error Handling**: The plugin incorporates robust error handling, reconnection logic, and connection timeouts to ensure reliable connectivity in challenging and dynamic environments.

## Target Audience

The Akita MeshTAK Plugin is an indispensable tool for:

* **Law Enforcement**: Tactical teams, search and rescue units, and special operations groups can use the plugin for enhanced coordination, real-time tracking, and secure communication during critical missions and disaster response efforts.
* **Military**: Dismounted infantry, reconnaissance units, and special operations forces can leverage the plugin for secure, long-range communication, improved situational awareness, and enhanced command and control in the field.
* **Security Personnel**: Security teams, perimeter control units, and surveillance teams can utilize the plugin for reliable communication and coordination in sensitive areas, protecting critical infrastructure and personnel.
* **First Responders**: Firefighters, paramedics, and emergency response teams can rely on the plugin for seamless communication and coordination during emergencies, natural disasters, and other critical events, ensuring efficient and effective response efforts.
* **Search and Rescue Teams**: Enables effective tracking of personnel and coordination of search efforts in remote and challenging terrains.
* **Government Agencies**: Facilitates secure and reliable communication for critical infrastructure protection, border security, and emergency management operations.
* **Tactical Teams**: Enhances communication and situational awareness for teams involved in planned operations, allowing for better coordination, faster decision-making, and improved mission outcomes.

## Project Structure

```
AkitaMeshTAK/
├── firmware/ # Firmware for Meshtastic devices (Heltec V3)
│   ├── src/
│   │   ├── main.cpp # Main application entry point
│   │   ├── config.h # Device-specific configurations
│   │   ├── meshtastic_setup.cpp # Meshtastic library initialization
│   │   ├── meshtastic_setup.h
│   │   ├── ble_setup.cpp # Bluetooth setup and handling
│   │   ├── ble_setup.h
│   │   ├── serial_bridge.cpp # Serial communication handling
│   │   ├── serial_bridge.h
│   │   ├── mqtt_client.cpp # MQTT communication (optional)
│   │   ├── mqtt_client.h
│   │   ├── cot_generation.cpp # CoT message generation
│   │   ├── cot_generation.h
│   │   ├── display_handler.cpp # Display updates (optional)
│   │   ├── display_handler.h
│   │   ├── power_management.cpp # Power saving and battery monitoring
│   │   ├── power_management.h
│   │   └── ... # Other modules as needed
│   ├── lib/ # Libraries used in the firmware
│   │   ├── Meshtastic-esp32/ # Meshtastic library (submodule)
│   │   ├── ESPAsyncWebServer/ # (Optional) For web configuration
│   │   ├── PubSubClient/ # For MQTT
│   │   └── ... (other libraries)
│   ├── platformio.ini # PlatformIO build configuration
│   ├── README.md # Firmware build instructions
│   └── .gitignore
├── atak_plugin/ # Code and resources for the ATAK plugin
│   ├── app/ # Android app/plugin files
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── AndroidManifest.xml # Plugin manifest
│   │   │   │   ├── java/
│   │   │   │   │   └── com/akitaengineering/meshtak/
│   │   │   │   │       ├── AkitaMeshTAKPlugin.java # Main plugin class
│   │   │   │   │       ├── services/ # Background services
│   │   │   │   │       │   ├── BLEService.java # Bluetooth communication
│   │   │   │   │       │   ├── SerialService.java # Serial communication
│   │   │   │   │       └── ui/ # User interface components
│   │   │   │   │           ├── AkitaToolbar.java # Custom toolbar
│   │   │   │   │           ├── ConnectionStatusOverlay.java # Map overlay
│   │   │   │   │           ├── SendDataView.java # View for sending data
│   │   │   │   │           ├── SettingsFragment.java # Plugin settings
│   │   │   │   ├── res/ # Resources
│   │   │   │   │   ├── layout/ # Layout files
│   │   │   │   │   │   ├── akita_toolbar.xml
│   │   │   │   │   │   └── send_data_view.xml
│   │   │   │   │   ├── xml/ # Preferences XML
│   │   │   │   │   │   └── preferences.xml
│   │   │   │   │   ├── values/ # Resource arrays and strings
│   │   │   │   │   │   ├── arrays.xml
│   │   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── ...
│   │   ├── build.gradle # Gradle build file
│   │   └── proguard-rules.pro # (Optional) ProGuard rules
│   ├── README.md # Plugin build and installation
│   └── .gitignore
├── server_scripts/ # Optional scripts for TAK Server or MQTT
│   └── ...
├── documentation/ # Project documentation
│   ├── user_guide.md # User guide
│   ├── dev_guide.md # Developer guide
│   └── ...
├── LICENSE # License file
└── README.md # Top-level project README
```
