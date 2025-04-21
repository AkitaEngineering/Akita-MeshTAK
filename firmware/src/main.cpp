#include <Arduino.h>
#include "config.h"
#include "meshtastic_setup.h"
#include "ble_setup.h"
#include "serial_bridge.h"
#include "mqtt_client.h"
#include "power_management.h"
#include "display_handler.h"

void setup() {
  Serial.begin(115200);
  Serial.println("Akita MeshTAK Firmware v" FIRMWARE_VERSION " Starting...");
  Serial.printf("Device ID: %s\n", DEVICE_ID);

  setupConfig();

  if (!setupMeshtastic()) {
    Serial.println("Critical Error: Meshtastic initialization failed. Halting.");
    while (true) {
      delay(1000);
    }
  }

#ifdef ENABLE_BLE
  if (!setupBLE()) {
    Serial.println("Warning: BLE initialization failed.");
  }
#endif

#ifdef ENABLE_SERIAL
  setupSerialBridge();
#endif

#ifdef ENABLE_MQTT
  if (!setupMQTT()) {
    Serial.println("Warning: MQTT initialization failed.");
  }
#endif

#ifdef ENABLE_DISPLAY
  setupDisplay();
  displayMessage("Akita MeshTAK v" FIRMWARE_VERSION);
#endif

  setupPowerManagement();

  Serial.println("Setup complete.");
}

void loop() {
  loopMeshtastic();

#ifdef ENABLE_BLE
  loopBLE();
#endif

#ifdef ENABLE_SERIAL
  loopSerialBridge();
#endif

#ifdef ENABLE_MQTT
  loopMQTT();
#endif

#ifdef ENABLE_DISPLAY
  loopDisplay();
#endif

  loopPowerManagement();

  delay(10);
}
