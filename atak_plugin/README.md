# Akita MeshTAK ATAK Plugin

## Overview

The Akita MeshTAK ATAK Plugin is an Android application that integrates Meshtastic communication with the Android Tactical Assault Kit (ATAK).  It allows ATAK users to send and receive data with Meshtastic devices.

**Important:** This plugin needs to be compiled into an APK file before it can be installed on an Android device.  Follow the instructions below.

## Features

* **Connectivity:**
    * Bluetooth Low Energy (BLE) for direct connection to Meshtastic devices.
    * Serial (USB) for tethered connections.
* **Data Exchange:**
    * Sends and receives Cursor on Target (CoT) messages.
    * Supports sending arbitrary data.
* **User Interface:**
    * Displays connection status (BLE and Serial) in the ATAK toolbar and on the map.
    * Provides a dedicated view for sending data.
* **Configuration:**
    * Allows users to configure connection settings within ATAK.

## Building the Plugin

To build the Akita MeshTAK ATAK Plugin, you will need the Android SDK and a build tool like Gradle.  The recommended approach is to use Android Studio.

### Prerequisites

* Android Studio (latest version)
* Android SDK (API level 26 or higher)
* ATAK SDK (version compatible with your ATAK installation)

### Build Instructions

1.  **Clone the Repository:** Clone the Akita MeshTAK repository:
    ```bash
    git clone [https://github.com/akitaengineering/AkitaMeshTAK.git](https://github.com/akitaengineering/AkitaMeshTAK.git)
    cd AkitaMeshTAK/atak_plugin
    ```
2.  **Open in Android Studio:** Open Android Studio and select "Open an existing Android Studio project".  Navigate to the `atak_plugin` directory and select it.
3.  **Configure the ATAK SDK:**
    * Follow the instructions in the ATAK SDK documentation to configure the SDK in Android Studio.  This usually involves adding the SDK as a library or module.
4.  **Build the APK:**
    * In Android Studio, select "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)".
    * Android Studio will build the APK file.  The location of the APK will be shown in a message at the bottom of the IDE (usually `app/build/outputs/apk/debug/app-debug.apk`).

## Installation

1.  **Locate the APK:** Find the generated APK file (e.g., `app-debug.apk`).
2.  **Install on Android Device:**
    * Enable "Unknown sources" in your Android device settings if necessary.
    * Copy the APK file to your Android device.
    * Use a file manager to locate and install the APK.
3.  **Enable in ATAK:** Open ATAK and enable the Akita MeshTAK Plugin through the plugin manager.

## Configuration

The plugin can be configured within ATAK's plugin settings:

* **Connection Method:** Select between "BLE" and "Serial".
* **BLE Device Name:** The name of the Meshtastic device to connect to.
* **Serial Port Path:** The path to the serial port (if using Serial connection).
* **Serial Baud Rate:** The baud rate for the serial connection.

## Contributing

See the [Developer Guide](https://techdevguide.withgoogle.com/) for information on how to contribute to the ATAK plugin.

## License

The ATAK plugin is released under the [MIT License](https://www.dmv.ca.gov/portal/driver-licenses-identification-cards/driver-licenses-dl/).
