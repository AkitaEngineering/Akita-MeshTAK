// File: firmware/src/meshtastic_setup.cpp
// Description: Implements Meshtastic setup, receive callback, and loop.

#include "meshtastic_setup.h"
#include "config.h"
#include "cot_generation.h"
#include "serial_bridge.h" // For sending CoT out
#include "ble_setup.h"     // For sending CoT out
#include "input_validation.h"
#include "audit_log.h"

// Callback for when a Meshtastic text message is received
static void onTextMessage(uint32_t from, uint32_t to, uint8_t channel, const char *text) {
  (void)to;
  Serial.print("Received message from: ");
  Serial.print(getNodeId(from));
  Serial.print(", channel: ");
  Serial.print(channel);
  Serial.print(", payload: ");
  Serial.println(text ? text : "");

  if (text == nullptr || text[0] == '\0') {
    return;
  }

  String payloadStr = String(text);
  payloadStr.trim();
  if (payloadStr.length() == 0) {
    return;
  }

  // If the payload is already a CoT XML, forward it to ATAK interfaces
  ValidationResult validation = validateCoTXml(payloadStr);
  if (validation == VALIDATION_OK) {
    logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "MESH", "CoT payload forwarded", true);
#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
    sendDataSerial((const uint8_t*)payloadStr.c_str(), payloadStr.length());
#endif
#if defined(ENABLE_BLE) && ENABLE_BLE
    sendDataBLE((const uint8_t*)payloadStr.c_str(), payloadStr.length());
#endif
    return;
  }

  logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 1, "MESH", "Invalid CoT payload", false);
}


bool setupMeshtastic() {
  Serial.println("Initializing Meshtastic...");

  // Initialize Meshtastic serial bridge (host mode)
  mt_set_debug(false);
  mt_serial_init(MESH_SERIAL_RX_PIN, MESH_SERIAL_TX_PIN, MESH_SERIAL_BAUD);
  set_text_message_callback(onTextMessage);

  return true;
}

void loopMeshtastic() {
  mt_loop(millis());
}

String getNodeId(uint32_t from) {
    char buf[12];
    snprintf(buf, sizeof(buf), "%08lX", (unsigned long)from);
    return String(buf);
}

String getLocalNodeId() {
    return getNodeId(my_node_num);
}
