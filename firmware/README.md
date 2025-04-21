# Akita MeshTAK Firmware

## Overview

This directory contains the firmware for Meshtastic devices (specifically Heltec V3) that are used with the Akita MeshTAK plugin.  This firmware configures the devices to communicate over Meshtastic and interface with ATAK.

## Features

* Meshtastic integration.
* Bluetooth Low Energy (BLE) for direct connection to ATAK.
* Serial (USB) communication for tethered connection to ATAK.
* Optional MQTT support for integration with Meshtastic networks.
* CoT (Cursor on Target) message generation.
* Display handling (for devices with displays).
* Power management.

## Hardware

* Heltec V3 (or other ESP32-based Meshtastic-compatible devices)

## Software

* PlatformIO IDE

## Installation

1.  **Install PlatformIO:** Follow the instructions on the [PlatformIO website](https://platformio.org/platformio-ide).
2.  **Clone the Repository:** Clone the Akita MeshTAK repository to your computer.
3.  **Navigate to the Firmware Directory:** `cd AkitaMeshTAK/firmware`
4.  **Build and Upload:**
    * Connect your Heltec V3 to your computer via USB.
    * In PlatformIO, build the firmware:  `pio run`
    * Upload the firmware to your device: `pio run -t upload`

## Configuration

The firmware can be configured by modifying the `config.h` file.  Key settings include:

* `DEVICE_ID`:  A unique identifier for the device.
* `LORA_REGION`:  The LoRa region for your location (e.g., `US915`, `EU868`).
* `ENABLE_BLE`:  Enable/disable Bluetooth.
* `ENABLE_SERIAL`: Enable/disable Serial.
* `ENABLE_MQTT`: Enable/disable MQTT.
* `MQTT_SERVER`, `MQTT_PORT`, `MQTT_USERNAME`, `MQTT_PASSWORD`, `MQTT_TOPIC_PREFIX`:  Settings for MQTT (if enabled).
* `ENABLE_DISPLAY`: Enable/disable display.
* `BATTERY_CHECK_INTERVAL`:  Interval for checking the battery voltage.

## Code Structure

* `src/`: Contains the main application source files.
* `lib/`:  Contains external libraries.
* `platformio.ini`:  PlatformIO configuration file.

## Contributing

See the [Developer Guide](https://techdevguide.withgoogle.com/) for information on how to contribute to the firmware.

## License

The firmware is released under the [MIT License](https://www.dmv.ca.gov/portal/driver-licenses-identification-cards/driver-licenses-dl/).
