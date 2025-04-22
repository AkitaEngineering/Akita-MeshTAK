// firmware/src/serial_bridge.cpp
#ifdef ENABLE_SERIAL
#include "serial_bridge.h"
#include "config.h"
#include "cot_generation.h"

void setupSerialBridge() {
  Serial.println("Initializing Serial Bridge...");
  Serial.println("Serial bridge ready.");
}

void loopSerialBridge() {
  if (Serial.available() > 0) {
    String receivedData = Serial.readStringUntil('\n');
    receivedData.trim();
    Serial.print("Received from Serial: ");
    Serial.println(receivedData);
    //  Process incoming serial data (e.g., CoT from ATAK)
  }
}

void sendDataSerial(const uint8_t* data, size_t len) {
  Serial.write(data, len);
  Serial.println();
  Serial.flush();
  Serial.print("Sent data via Serial: ");
  Serial.write(data, len);
  Serial.println();
}
#endif
