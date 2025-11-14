// File: firmware/src/serial_bridge.cpp
// Description: Implements Serial data handling to and from ATAK.

#ifdef ENABLE_SERIAL
#include "serial_bridge.h"
#include "config.h"
#include "cot_generation.h"
#include "power_management.h" // For processIncomingCommand

void setupSerialBridge() {
  Serial.println("Initializing Serial Bridge...");
  Serial.println("Serial bridge ready.");
}

void loopSerialBridge() {
  if (Serial.available() > 0) {
    String receivedData = Serial.readStringUntil('\n');
    receivedData.trim(); // Clean up whitespace
    if (receivedData.length() > 0) {
        Serial.print("Received command via Serial: ");
        Serial.println(receivedData);
        processIncomingCommand(receivedData); // Process the command
    }
  }
}

// Function to send CoT or Status data to ATAK
void sendDataSerial(const uint8_t* data, size_t len) {
  Serial.write(data, len);
  Serial.println(); // Send newline to ensure ATAK reads the line
  Serial.flush();
  Serial.print("Sent data via Serial: ");
  Serial.write(data, len);
  Serial.println();
}
#endif
