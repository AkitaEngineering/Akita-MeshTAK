// File: firmware/src/meshtastic_setup.h
// Description: Declares Meshtastic setup functions and the global Meshtastic object.
#ifndef MESHTASTIC_SETUP_H
#define MESHTASTIC_SETUP_H

#include <Meshtastic.h>

extern MeshtasticClass Meshtastic; // <-- ADDED THIS LINE

bool setupMeshtastic();
void loopMeshtastic();
String getNodeId(const uint8_t *from);

#endif
// -----------------------------------------------------------------
// File: firmware/src/meshtastic_setup.cpp
// Description: Implements Meshtastic setup, receive callback, and loop.

#include "meshtastic_setup.h"
#include "config.h"
#include "cot_generation.h"
#include "serial_bridge.h" // For sending CoT out
#include "ble_setup.h"     // For sending CoT out

MeshtasticClass Meshtastic; // Definition of the global object

// Callback for when a Meshtastic packet is received
void onReceive(const uint8_t *from, const rx_t &rxInfo, const uint8_t *payload, size_t len) {
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

    // TODO: Process incoming Meshtastic packets
    // 1. Check if it's a position packet
    // 2. If yes, generate a CoT message
    // String cotMessage = generateLocationCoT(...);
    
    // 3. Send CoT over Serial and BLE
    // #ifdef ENABLE_SERIAL
    // sendDataSerial((const uint8_t*)cotMessage.c_str(), cotMessage.length());
    // #endif
    // #ifdef ENABLE_BLE
    // sendDataBLE((const uint8_t*)cotMessage.c_str(), cotMessage.length());
    // #endif
}


bool setupMeshtastic() {
  Serial.println("Initializing Meshtastic...");
  Meshtastic.begin();

  if (Meshtastic.isStarted()) {
    Serial.print("Meshtastic Started. My Node ID: ");
    Serial.println(Meshtastic.getNodeId().toString());
    Meshtastic.setOwner(DEVICE_ID);

    Meshtastic.addReceiveCallback(onReceive);

    return true;
  } else {
    Serial.println("Failed to start Meshtastic.");
    return false;
  }
}

void loopMeshtastic() {
  Meshtastic.loop();
}

String getNodeId(const uint8_t *from) {
    return Meshtastic.getNodeId(from).toString();
}
