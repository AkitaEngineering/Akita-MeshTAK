// File: firmware/src/serial_bridge.cpp
// Description: Implements Serial data handling to and from ATAK.

#ifdef ENABLE_SERIAL
#include "serial_bridge.h"
#include "config.h"
#include "cot_generation.h"
#include "power_management.h" // For processIncomingCommand
#include "audit_log.h"        // For audit logging
#include "input_validation.h" // For input validation

void setupSerialBridge() {
  Serial.println("Initializing Serial Bridge...");
  Serial.println("Serial bridge ready.");
}

void loopSerialBridge() {
  if (Serial.available() > 0) {
    String receivedData = Serial.readStringUntil('\n');
    receivedData.trim(); // Clean up whitespace
    if (receivedData.length() > 0) {
        // Security: Log serial data reception
        logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "SERIAL",
                     ("Data received: " + receivedData.substring(0, 32)).c_str(), true);
        
        Serial.print("Received command via Serial: ");
        Serial.println(receivedData);
        
        // Input validation before processing
        ValidationResult validation = validateCommand(receivedData);
        if (validation == VALIDATION_OK) {
            processIncomingCommand(receivedData); // Process the command
        } else {
            logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "SERIAL",
                         "Invalid command - validation failed", false);
            Serial.printf("SECURITY: Serial command validation failed: %d\n", validation);
        }
    }
  }
}

// Function to send CoT or Status data to ATAK
void sendDataSerial(const uint8_t* data, size_t len) {
  // Input validation for outgoing data
  if (len == 0 || len > MAX_MESSAGE_LENGTH) {
    logAuditEvent(AUDIT_EVENT_ERROR, 1, "SERIAL", "Send failed - invalid data length", false);
    return;
  }
  
  Serial.write(data, len);
  Serial.println(); // Send newline to ensure ATAK reads the line
  Serial.flush();
  
  Serial.print("Sent data via Serial: ");
  Serial.write(data, len < 64 ? len : 64); // Limit serial output
  Serial.println();
  
  logAuditEvent(AUDIT_EVENT_DATA_SENT, 0, "SERIAL",
               String("Data sent, len: " + String(len)).c_str(), true);
}
#endif
