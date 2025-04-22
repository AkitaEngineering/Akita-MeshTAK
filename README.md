# Akita MeshTAK

**An ATAK Plugin for Meshtastic Integration**

## Overview

The Akita MeshTAK Plugin is an Android Tactical Assault Kit (ATAK) plugin that enables communication between ATAK and Meshtastic networks.  This allows ATAK users to:

* Send and receive user positions and messages over Meshtastic.
* Operate in areas without traditional network infrastructure.
* Utilize Meshtastic's long-range, low-power capabilities.

**Important:** This project requires you to compile the Android plugin (`.apk`) yourself.  See the [ATAK Plugin README](atak_plugin/README.md) for detailed instructions.

## Features

* **Connectivity:**
    * Bluetooth Low Energy (BLE) for direct connection to Meshtastic devices.
    * Serial (USB) for tethered connections.
    * (Optional) MQTT for integration with Meshtastic networks via a bridge.
* **Data Exchange:**
    * Send and receive Cursor on Target (CoT) messages for user location and presence.
    * Support for sending arbitrary data.
* **User Interface:**
    * Connection status display in the ATAK toolbar and on the map.
    * Dedicated view for sending data with format selection and command history.
* **Configuration:**
    * Plugin settings for connection method, device selection, and serial parameters.

## Target Audience

This plugin is designed for:

* ATAK users who need to communicate in off-grid environments.
* Teams that utilize Meshtastic for tactical communication.
* Developers who want to extend ATAK's capabilities with Meshtastic integration.

## Project Structure

AkitaMeshTAK/├── firmware/                     # Firmware for Meshtastic devices (Heltec V3)│   ├── src/│   │   ├── main.cpp│   │   ├── config.h│   │   ├── meshtastic_setup.cpp│   │   ├── meshtastic_setup.h│   │   ├── ble_setup.cpp│   │   ├── ble_setup.h│   │   ├── serial_bridge.cpp│   │   ├── serial_bridge.h│   │   ├── mqtt_client.cpp│   │   ├── mqtt_client.h│   │   ├── cot_generation.cpp│   │   ├── cot_generation.h│   │   ├── display_handler.cpp│   │   ├── display_handler.h│   │   ├── power_management.cpp│   │   ├── power_management.h│   │   └── ...│   ├── lib/                       # Libraries used in the firmware│   │   ├── Meshtastic-esp32/      # Submodule, don't modify directly│   │   ├── ESPAsyncWebServer/     # For potential web configuration interface (optional)│   │   ├── PubSubClient/          # For MQTT communication│   │   ├── ... (other libraries)│   ├── platformio.ini            # PlatformIO configuration file for building the firmware│   └── README.md                 # Description of the firmware and build process│   └── .gitignore├── atak_plugin/                  # Code and resources for the ATAK plugin│   ├── app/                    # Android app/plugin files│   │   ├── src/│   │   │   ├── main/│   │   │   │   ├── AndroidManifest.xml│   │   │   │   ├── java/│   │   │   │   │   └── com/akitaengineering/meshtak/│   │   │   │   │       ├── AkitaMeshTAKPlugin.java│   │   │   │   │       ├── services/│   │   │   │   │       │   ├── BLEService.java│   │   │   │   │       │   ├── SerialService.java│   │   │   │   │       └── ui/│   │   │   │   │           ├── AkitaToolbar.java│   │   │   │   │           ├── ConnectionStatusOverlay.java│   │   │   │   │           ├── SendDataView.java│   │   │   │   │           ├── SettingsFragment.java│   │   │   │   ├── res/│   │   │   │   │   ├── layout/│   │   │   │   │   │   ├── akita_toolbar.xml│   │   │   │   │   │   └── send_data_view.xml│   │   │   │   │   ├── xml/│   │   │   │   │   │   └── preferences.xml│   │   │   │   │   ├── values/│   │   │   │   │   │   ├── arrays.xml│   │   │   │   │   │   ├── strings.xml│   │   │   │   │   └── ...│   │   ├── build.gradle              # Gradle build file for the ATAK plugin│   │   └── proguard-rules.pro     # If used│   ├── README.md                 # Instructions specific to the ATAK plugin│   └── .gitignore├── server_scripts/               # Optional scripts for the TAK Server or MQTT bridge│   └── ...├── documentation/                # Project documentation│   ├── user_guide.md           # User guide for the plugin│   ├── dev_guide.md            # Developer guide for contributing│   └── ...├── LICENSE                     # License file└── README.md                     # Top-level project
