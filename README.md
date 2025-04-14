# Akita MeshTAK Plugin

## Overview

The Akita MeshTAK Plugin is an Android Tactical Assault Kit (ATAK) plugin designed to facilitate communication with Akita Mesh devices. This plugin enables ATAK users to send and receive data, including location information and potentially other sensor data, over both Bluetooth Low Energy (BLE) and serial (USB) connections.

## Features

* **Data Transmission:** Send data to Akita Mesh devices from ATAK.
* **Connection Methods:** Supports both BLE and serial (USB) connections.
* **CoT Integration:** Processes and displays Contigency of Operations (CoT) data received from Akita Mesh devices on the ATAK map.
* **User Interface:** Provides a dedicated view for sending data, including:
    * Text input for data.
    * Data format selection (Plain Text, JSON, Custom).
    * Command history.
* **Connection Status:** Displays the current connection status (BLE and Serial) in the ATAK toolbar and map overlay.
* **Dynamic UI:** Updates the UI to reflect the active connection method.
* **Error Handling:** Robust error handling with retries and timeouts for connection attempts.
* **Configuration:** Allows users to configure the plugin settings, including:
    * BLE device name.
    * Serial baud rate.

## Installation

1.  **Download:** Download the latest version of the Akita MeshTAK Plugin (.apk file).
2.  **Install:** Install the plugin on your Android device running ATAK.  You may need to enable installation from unknown sources in your device settings.
3.  **Enable:** In ATAK, enable the Akita MeshTAK Plugin through the plugin manager.

## Usage

1.  **Connect to Device:**
    * Open the plugin settings in ATAK.
    * Select the connection method (BLE or Serial).
    * Configure the device-specific settings (BLE device name or serial baud rate).
    * The plugin will attempt to connect to the Akita Mesh device.  The connection status is displayed in the ATAK toolbar and as an overlay on the map.
2.  **Send Data:**
    * Open the "Send Data" view from the plugin menu.
    * Enter the data you want to send in the text input field.
    * Select the format of the data (Plain Text, JSON, or Custom).
    * Click the "Send" button.
    * Sent commands are added to a history for easy re-sending.
3.  **Receive Data:**
    * The plugin automatically receives data from the connected Akita Mesh device.
    * CoT data is displayed on the ATAK map.
    * Other data may be displayed in a future version of the plugin.

## Configuration

The plugin can be configured via the ATAK plugin settings:

* **Connection Method:** Select between "BLE" and "Serial" communication.
* **BLE Device Name:** Specify the name of the Akita Mesh device to connect to via BLE.
* **Serial Baud Rate:** Set the baud rate for serial communication.

## Dependencies

* Android Tactical Assault Kit (ATAK)

## Contributing

Contributions are welcome!  Please follow these guidelines:

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Commit your changes.
4.  Push to the branch.
5.  Submit a pull request.


## Author

Akita Engineering

