// File: firmware/src/meshtastic_setup.cpp
// Description: Implements Meshtastic setup, receive callback, and loop.

#include "meshtastic_setup.h"
#include "config.h"
#include "cot_generation.h"
#include "serial_bridge.h" // For sending CoT out
#include "ble_setup.h"     // For sending CoT out
#include "input_validation.h"
#include "audit_log.h"
#include "mailbox_escape.h" // Shared escape/unescape

static const char* MESH_MAILBOX_FRAME_PREFIX = "AKITA:MBX:MSG:";
static const char* MESH_MAILBOX_ACK_PREFIX = "AKITA:MBX:ACK:";

static String encodeMailboxFormat(const String& format) {
  if (format.equalsIgnoreCase("JSON")) {
    return "JSON";
  }
  if (format.equalsIgnoreCase("Custom")) {
    return "CUSTOM";
  }
  return "TEXT";
}

static String decodeMailboxFormat(const String& formatToken) {
  if (formatToken.equalsIgnoreCase("JSON")) {
    return "JSON";
  }
  if (formatToken.equalsIgnoreCase("CUSTOM")) {
    return "Custom";
  }
  return "Plain Text";
}

static bool parseMeshMailboxFrame(const String& payload,
                                  String& originNodeId,
                                  String& messageId,
                                  String& format,
                                  String& body) {
  if (!payload.startsWith(MESH_MAILBOX_FRAME_PREFIX)) {
    return false;
  }

  String frame = payload.substring(strlen(MESH_MAILBOX_FRAME_PREFIX));
  int firstSeparator = frame.indexOf(':');
  int secondSeparator = frame.indexOf(':', firstSeparator + 1);
  int thirdSeparator = frame.indexOf(':', secondSeparator + 1);
  if (firstSeparator <= 0 || secondSeparator <= firstSeparator + 1 || thirdSeparator <= secondSeparator + 1) {
    return false;
  }

  originNodeId = frame.substring(0, firstSeparator);
  messageId = frame.substring(firstSeparator + 1, secondSeparator);
  format = decodeMailboxFormat(frame.substring(secondSeparator + 1, thirdSeparator));
  body = unescapeMailboxPayload(frame.substring(thirdSeparator + 1));
  return body.length() > 0;
}

static bool parseMeshMailboxAck(const String& payload, String& messageId, String& acknowledgingNodeId) {
  if (!payload.startsWith(MESH_MAILBOX_ACK_PREFIX)) {
    return false;
  }

  String frame = payload.substring(strlen(MESH_MAILBOX_ACK_PREFIX));
  int separator = frame.indexOf(':');
  if (separator <= 0 || separator + 1 >= frame.length()) {
    return false;
  }

  messageId = frame.substring(0, separator);
  acknowledgingNodeId = frame.substring(separator + 1);
  return acknowledgingNodeId.length() > 0;
}

static String buildMeshMailboxFrame(const String& messageId, const String& format, const String& payload) {
  return String(MESH_MAILBOX_FRAME_PREFIX)
      + getLocalNodeId()
      + ":"
      + messageId
      + ":"
      + encodeMailboxFormat(format)
      + ":"
      + escapeMailboxPayload(payload);
}

static String buildMeshMailboxAck(const String& messageId) {
  return String(MESH_MAILBOX_ACK_PREFIX) + messageId + ":" + getLocalNodeId();
}

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

  String messageId = "";
  String acknowledgingNodeId = "";
  if (parseMeshMailboxAck(payloadStr, messageId, acknowledgingNodeId)) {
    String ackStatus = String(STATUS_MAILBOX_ACK_PREFIX) + messageId + ":DELIVERED:" + acknowledgingNodeId;
    logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "MESH", "Peer mailbox acknowledgement received", true);
#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
    sendDataSerial((const uint8_t*)ackStatus.c_str(), ackStatus.length());
#endif
#if defined(ENABLE_BLE) && ENABLE_BLE
    sendDataBLE((const uint8_t*)ackStatus.c_str(), ackStatus.length());
#endif
    return;
  }

  String originNodeId = "";
  String format = "";
  String mailboxPayload = "";
  if (parseMeshMailboxFrame(payloadStr, originNodeId, messageId, format, mailboxPayload)) {
    if (originNodeId == getLocalNodeId()) {
      logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "MESH", "Ignoring self-echo mailbox frame", true);
      return;
    }

    String inboundStatus = String(STATUS_MAILBOX_RX_PREFIX)
        + originNodeId
        + ":"
        + messageId
        + ":"
        + encodeMailboxFormat(format)
        + ":"
        + escapeMailboxPayload(mailboxPayload);
    logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "MESH", "Mailbox payload forwarded", true);
#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
    sendDataSerial((const uint8_t*)inboundStatus.c_str(), inboundStatus.length());
#endif
#if defined(ENABLE_BLE) && ENABLE_BLE
    sendDataBLE((const uint8_t*)inboundStatus.c_str(), inboundStatus.length());
#endif

    String ackFrame = buildMeshMailboxAck(messageId);
    bool ackSent = mt_send_text(ackFrame.c_str(), from, channel);
    logAuditEvent(AUDIT_EVENT_DATA_SENT, ackSent ? 0 : 2, "MESH",
                 ackSent ? "Mailbox peer acknowledgement sent" : "Mailbox peer acknowledgement failed", ackSent);
    return;
  }

  String inboundStatus = String(STATUS_MAILBOX_RX_PREFIX) + getNodeId(from) + ":" + escapeMailboxPayload(payloadStr);
  logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "MESH", "Generic mission payload forwarded", true);
#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
  sendDataSerial((const uint8_t*)inboundStatus.c_str(), inboundStatus.length());
#endif
#if defined(ENABLE_BLE) && ENABLE_BLE
  sendDataBLE((const uint8_t*)inboundStatus.c_str(), inboundStatus.length());
#endif
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

bool sendMailboxPayloadOverMesh(const String& messageId, const String& format, const String& payload) {
  String frame = buildMeshMailboxFrame(messageId, format, payload);
  return mt_send_text(frame.c_str(), BROADCAST_ADDR, 0);
}
