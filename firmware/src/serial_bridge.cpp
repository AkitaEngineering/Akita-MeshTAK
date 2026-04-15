// File: firmware/src/serial_bridge.cpp
// Description: Implements Serial data handling to and from ATAK.

#include "config.h"
#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
#include "serial_bridge.h"
#include "cot_generation.h"
#include "power_management.h" // For processIncomingCommand
#include "audit_log.h"        // For audit logging
#include "input_validation.h" // For input validation
#include "security.h"         // For payload encryption/decryption
#include "payload_codec.h"    // Shared encode/decode utilities
#include <string.h>

bool setupSerialBridge() {
  Serial.println("Initializing Serial Bridge...");
  Serial.println("Serial bridge ready.");
  return true;
}

static unsigned long lastSerialCommandMs = 0;

void loopSerialBridge() {
  if (Serial.available() > 0) {
    String receivedData = Serial.readStringUntil('\n');
    receivedData.trim(); // Clean up whitespace
    if (receivedData.length() > MAX_SERIAL_LINE_LENGTH) {
      logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "SERIAL",
                   "Dropped oversized serial line", false);
      return;
    }
    if (receivedData.length() > 0) {
      // Rate limiting
      unsigned long now = millis();
      if ((now - lastSerialCommandMs) < CMD_RATE_LIMIT_MS) {
        logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 1, "SERIAL",
                     "Rate limit exceeded – command dropped", false);
        return;
      }
      lastSerialCommandMs = now;
      String decodedData = "";
      if (!decodeIncomingPayload(receivedData, decodedData)) {
        logAuditEvent(AUDIT_EVENT_AUTHENTICATION_FAILURE, 2, "SERIAL",
               "Encrypted payload decode failed", false);
        return;
      }

      // Security: Log serial data reception
      logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "SERIAL",
           ("Data received: " + decodedData.substring(0, 32)).c_str(), true);

      Serial.print("Received command via Serial: ");
      Serial.println(decodedData);

      // Input validation before processing
      ValidationResult validation = validateCommand(decodedData);
      if (validation == VALIDATION_OK) {
        processIncomingCommand(decodedData); // Process the command
      } else {
        logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "SERIAL",
                     "Invalid command - validation failed", false);
        Serial.printf("SECURITY: Serial command validation failed: %d\n", validation);
      }
    }
  }
}

// Function to send CoT or Status data to ATAK
void sendDataSerial(const uint8_t* data, size_t len, bool forcePlaintext) {
  // Input validation for outgoing data
  if (len == 0 || len > MAX_MESSAGE_LENGTH) {
    logAuditEvent(AUDIT_EVENT_ERROR, 1, "SERIAL", "Send failed - invalid data length", false);
    return;
  }

  String outgoingPayload = "";
  if (forcePlaintext) {
    outgoingPayload = String((const char*)data).substring(0, len);
  } else if (!encodeOutgoingPayload(data, len, outgoingPayload)) {
    logAuditEvent(AUDIT_EVENT_ERROR, 2, "SERIAL", "Send failed - encryption error", false);
    return;
  }
  
  Serial.print(outgoingPayload);
  Serial.println(); // Send newline to ensure ATAK reads the line
  Serial.flush();
  
  Serial.print("Sent data via Serial: ");
  Serial.print(outgoingPayload.substring(0, 64)); // Limit serial output
  Serial.println();
  
  logAuditEvent(AUDIT_EVENT_DATA_SENT, 0, "SERIAL",
               String("Data sent, len: " + String(outgoingPayload.length())).c_str(), true);
}
#endif
