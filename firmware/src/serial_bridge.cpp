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
static String g_serialInput = "";
static bool g_discardOversizedSerialInput = false;

static void processSerialRecord(String receivedData) {
  receivedData.trim();
  if (receivedData.length() == 0) return;

  unsigned long now = millis();
  if ((now - lastSerialCommandMs) < CMD_RATE_LIMIT_MS) {
    logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 1, "SERIAL",
                 "Rate limit exceeded - command dropped", false);
    for (size_t i = 0; i < receivedData.length(); i++) receivedData.setCharAt(i, '\0');
    return;
  }
  lastSerialCommandMs = now;
  String decodedData = "";
  if (!decodeIncomingPayload(receivedData, decodedData, true)) {
    logAuditEvent(AUDIT_EVENT_AUTHENTICATION_FAILURE, 2, "SERIAL",
                 "Encrypted payload decode failed", false);
    for (size_t i = 0; i < receivedData.length(); i++) receivedData.setCharAt(i, '\0');
    return;
  }
  logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "SERIAL",
                "Authenticated command received", true);
  ValidationResult validation = validateCommand(decodedData);
  if (validation == VALIDATION_OK) {
    processIncomingCommand(decodedData);
  } else {
    logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "SERIAL",
                  "Invalid command - validation failed", false);
  }
  for (size_t i = 0; i < decodedData.length(); i++) decodedData.setCharAt(i, '\0');
  for (size_t i = 0; i < receivedData.length(); i++) receivedData.setCharAt(i, '\0');
}

void loopSerialBridge() {
  while (Serial.available() > 0) {
    char c = (char)Serial.read();
    if (c == '\n') {
      if (!g_discardOversizedSerialInput) processSerialRecord(g_serialInput);
      for (size_t i = 0; i < g_serialInput.length(); i++) g_serialInput.setCharAt(i, '\0');
      g_serialInput = "";
      g_discardOversizedSerialInput = false;
    } else if (c != '\r' && !g_discardOversizedSerialInput) {
      if (g_serialInput.length() < MAX_SERIAL_LINE_LENGTH) {
        g_serialInput += c;
      } else {
        g_serialInput = "";
        g_discardOversizedSerialInput = true;
        logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "SERIAL",
                      "Dropped oversized serial line", false);
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

  String outgoingPayload = "";
  if (!encodeOutgoingPayload(data, len, outgoingPayload)) {
    logAuditEvent(AUDIT_EVENT_ERROR, 2, "SERIAL", "Send failed - encryption error", false);
    return;
  }

  Serial.print(outgoingPayload);
  Serial.println(); // Send newline to ensure ATAK reads the line
  Serial.flush();

  logAuditEvent(AUDIT_EVENT_DATA_SENT, 0, "SERIAL",
               String("Data sent, len: " + String(outgoingPayload.length())).c_str(), true);
}
#endif
