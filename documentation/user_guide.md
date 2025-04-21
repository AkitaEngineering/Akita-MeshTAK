# Akita MeshTAK Plugin User Guide

## Introduction

The Akita MeshTAK Plugin allows you to connect your ATAK device to Meshtastic networks, enabling off-grid communication.

## System Requirements

* Android device with ATAK installed
* Meshtastic-compatible devices (e.g., Heltec V3)
* Akita MeshTAK Plugin APK file

## Installation

1.  **Install ATAK:** Ensure that ATAK is installed on your Android device.
2.  **Install Plugin APK:**
    * Download the Akita MeshTAK Plugin APK file.
    * If necessary, enable "Install from unknown sources" in your Android device's settings.
    * Locate the APK file on your device and install it.
3.  **Enable Plugin in ATAK:**
    * Open ATAK.
    * Go to the plugin manager.
    * Find the "Akita MeshTAK" plugin and enable it.

## Connecting to Meshtastic

1.  **Configure Connection Settings:**
    * Open the ATAK plugin settings.
    * Select your preferred **Connection Method**:
        * **BLE:** Ensure your Meshtastic device is powered on and advertising.
        * **Serial:** Connect your Meshtastic device to your Android device via USB.
    * Configure the connection parameters:
        * If using BLE, enter the **BLE Device Name** of your Meshtastic device.
        * If using Serial, enter the **Serial Port Path** and **Serial Baud Rate**.  (The defaults are usually correct)
2.  **Connection Status:**
    * The plugin will attempt to connect automatically.
    * The connection status is displayed in the ATAK toolbar:
        * "BLE: Connected" or "Serial: Connected" indicates a successful connection.
        * "BLE: Disconnected" or "Serial: Disconnected" indicates that the plugin is not connected.
        * "BLE: Connecting" or "Serial: Connecting" indicates that the plugin is attempting to connect.
        * "BLE: Error" or "Serial: Error" indicates an error occurred.
    * The connection status is also shown as an overlay on the map.

## Sending Data

1.  **Open the Send Data View:** Access the "Send Data" view from the plugin menu in ATAK.
2.  **Enter Data:** Type the data you want to send in the text input field.
3.  **Select Data Format:** Choose the format of your data:
    * **Plain Text:** Sends the data as plain text.
    * **JSON:** Sends the data as a JSON string.
    * **Custom:** Sends the data with a custom prefix.
4.  **Send:** Tap the "Send" button.
5.  **Command History:** The sent command will be added to the command history dropdown, allowing you to easily re-send it.

## Receiving Data

The plugin automatically receives data from the connected Meshtastic device.

* **CoT Data:** CoT messages (for example, user location) are displayed as markers on the ATAK map.
* **Other Data:** Future versions of the plugin may display other types of received data.

## Troubleshooting

* **Connection Issues:**
    * Ensure your Meshtastic device is powered on and within range.
    * Double-check the BLE device name or serial port settings.
    * Try restarting the ATAK app and the Meshtastic device.
    * Ensure that the Android device has the necessary permissions (Bluetooth, USB).
* **Data Not Displaying:**
    * Verify that the Meshtastic device is sending data in the correct format.
    * Check the connection status to ensure that the plugin is connected.
    * Consult the ATAK logs for any error messages.

## Support

For support, please contact Akita Engineering or visit our website:

* Akita Engineering: [www.akitaengineering.com](www.akitaengineering.com)
* Email: [info@akitaengineering.com](mailto:info@akitaengineering.com)
