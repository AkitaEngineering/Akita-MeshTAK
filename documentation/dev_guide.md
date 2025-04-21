# Akita MeshTAK Plugin Developer Guide

## Introduction

This guide provides information for developers who want to contribute to the Akita MeshTAK Plugin.

## Project Overview

The Akita MeshTAK Plugin consists of two main components:

* **Firmware:** Code that runs on the Meshtastic devices (e.g., Heltec V3).
* **ATAK Plugin:** Android application that runs within ATAK.

## Code Structure

AkitaMeshTAK/├── firmware/                     # Firmware for Meshtastic devices├── atak_plugin/                  # ATAK plugin source code├── server_scripts/               # Optional server-side scripts├── documentation/                # Documentation├── LICENSE                     # License information└── README.md                     # Top-level README
### Firmware

The firmware is written for ESP32 microcontrollers using the Arduino framework and the Meshtastic library.

* `src/`: Contains the main application source files.
* `lib/`: Contains external libraries.
* `platformio.ini`:  PlatformIO configuration file.

### ATAK Plugin

The ATAK plugin is an Android application written in Java.

* `app/src/main/java/com/akitaengineering/meshtak/`:  Contains the Java source code for the plugin.
    * `AkitaMeshTAKPlugin.java`:  The main plugin class.
    * `services/`:  Contains background services for communication (BLE, Serial).
    * `ui/`:  Contains UI components (toolbar, views, settings).
* `app/src/main/res/`: Contains resources such as layouts, strings, and preferences.
* `app/build.gradle`:  Gradle build file.
* `AndroidManifest.xml`:  Android manifest file.

## Building the Project

### Firmware

The firmware is built using PlatformIO.

1.  Install PlatformIO.
2.  Navigate to the `firmware/` directory.
3.  Build the firmware: `pio run`
4.  Upload the firmware to your device: `pio run -t upload`

### ATAK Plugin

The ATAK plugin is built using Android Studio.

1.  Install Android Studio and the Android SDK.
2.   Configure the ATAK SDK.
3.  Open the `atak_plugin/` directory as an Android Studio project.
4.  Build the APK.

## Contributing Guidelines

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Follow the code style conventions used in the project.
4.  Write clear and concise commit messages.
5.  Update the relevant documentation.
6.  Test your changes thoroughly.
7.  Submit a pull request.

## Code Style

* **Java:** Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
* **C++:** Follow the [Google C++ Style Guide](https://google.github.io/styleguide/cppguide.html).
* **XML:** Use consistent formatting and indentation.
* **Markdown:** Follow the [GitHub Markdown Style Guide](https://docs.github.com/en/github/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax)

## Testing

* Thoroughly test your changes on both emulators and physical devices.
* Write unit tests or integration tests where appropriate.

## Documentation

* Update the relevant documentation (user guide, developer guide) to reflect your changes.
* Write clear and concise documentation.

## Commit Messages

Follow these guidelines for your commit messages:

* Use the present tense ("Add feature" not "Added feature").
* Use the imperative mood ("Fix bug" not "Fixes bug").
* Keep the first line short (50 characters or less).
* Include a more detailed description in the body of the commit message.

## Pull Requests

When submitting a pull request:

* Ensure that your code is properly formatted and passes all tests.
* Provide a clear description of your changes.
* Reference any related issues.

## License

The Akita MeshTAK project is released under the GPLv3
