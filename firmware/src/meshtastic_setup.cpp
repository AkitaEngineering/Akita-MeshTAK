// firmware/src/meshtastic_setup.cpp
#include "meshtastic_setup.h"
#include "config.h"
#include "cot_generation.h"

MeshtasticClass Meshtastic;

bool setupMeshtastic() {
  Serial.println("Initializing Meshtastic...");
  Meshtastic.begin();

  if (Meshtastic.isStarted()) {
    Serial.print("Meshtastic Started. My Node ID: ");
    Serial.println(Meshtastic.getNodeId().toString());
    Meshtastic.setOwner(DEVICE_ID);

    Meshtastic.addReceiveCallback([](const uint8_t *from, const rx_t &rxInfo, const uint8_t *payload, size_t len) {
      Serial.print("Received message from: ");
      Serial.print(getNodeId(from).toString());
      Serial.print(", channel: ");
      Serial.print(rxInfo.channel);
      Serial.print(", RSSI: ");
      Serial.print(rxInfo.rssi);
      Serial.print(", payload: ");
      for (int i = 0; i < len; i++) {
        Serial.printf("%02X", payload[i]);
      }
      Serial.println();
      //  Process incoming Meshtastic messages here
      //  * Check for location updates from other nodes.
      //  * Convert to CoT and send via BLE/Serial/MQTT.
    });

    return true;
  } else {
    Serial.println("Failed to start Meshtastic.");
    return false;
  }
}

void loopMeshtastic() {
  //  Handle Meshtastic events
}

String getNodeId(const uint8_t *from) {
    //  Helper function to convert a Meshtastic node ID to a string
    return Meshtastic.getNodeId(from).toString();
}
