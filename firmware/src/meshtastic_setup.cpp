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

static const char* MESH_MAILBOX_FRAGMENT_PREFIX = "AKITA:MBX:FRG:";
static const char* MESH_MAILBOX_ACK_PREFIX = "AKITA:MBX:ACK:";
static const size_t MESH_FRAGMENT_BODY_BYTES = 150;
static const size_t MESH_MAX_FRAGMENTS = 16;
static const unsigned long MESH_REASSEMBLY_TIMEOUT_MS = 60000;

enum MeshFragmentResult { MESH_NOT_FRAGMENT, MESH_FRAGMENT_INCOMPLETE, MESH_FRAGMENT_COMPLETE, MESH_FRAGMENT_INVALID };
static uint32_t g_fragmentFrom = 0;
static String g_fragmentMessageId = "";
static String g_fragmentFormat = "";
static String g_fragmentBody = "";
static size_t g_fragmentNextIndex = 0;
static size_t g_fragmentCount = 0;
static unsigned long g_fragmentStartedAt = 0;
static bool g_nodeReportInFlight = false;
static unsigned long g_lastNodeReportRequest = 0;
static const unsigned long NODE_REPORT_INTERVAL_MS = 60000;

static void resetMeshFragments() {
  g_fragmentFrom = 0;
  g_fragmentMessageId = "";
  g_fragmentFormat = "";
  g_fragmentBody = "";
  g_fragmentNextIndex = 0;
  g_fragmentCount = 0;
  g_fragmentStartedAt = 0;
}

static bool parseFragmentNumber(const String& value, size_t& output) {
  if (value.length() == 0) return false;
  size_t parsed = 0;
  for (size_t i = 0; i < value.length(); i++) {
    if (!isdigit(value.charAt(i))) return false;
    parsed = parsed * 10U + (size_t)(value.charAt(i) - '0');
  }
  output = parsed;
  return true;
}

static bool isValidMeshMessageId(const String& value) {
  if (value.length() == 0 || value.length() > 32) return false;
  for (size_t i = 0; i < value.length(); i++) {
    char c = value.charAt(i);
    if (!isalnum(c) && c != '-' && c != '_') return false;
  }
  return true;
}

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

static MeshFragmentResult consumeMeshFragment(uint32_t from, const String& payload,
                                              String& messageId, String& format, String& body) {
  if (!payload.startsWith(MESH_MAILBOX_FRAGMENT_PREFIX)) return MESH_NOT_FRAGMENT;
  String frame = payload.substring(strlen(MESH_MAILBOX_FRAGMENT_PREFIX));
  int separators[5];
  int searchFrom = 0;
  for (int i = 0; i < 5; i++) {
    separators[i] = frame.indexOf(':', searchFrom);
    if (separators[i] <= searchFrom) {
      resetMeshFragments();
      return MESH_FRAGMENT_INVALID;
    }
    searchFrom = separators[i] + 1;
  }
  String claimedOrigin = frame.substring(0, separators[0]);
  String incomingMessageId = frame.substring(separators[0] + 1, separators[1]);
  String incomingFormat = frame.substring(separators[1] + 1, separators[2]);
  size_t index = 0;
  size_t count = 0;
  if (claimedOrigin != getNodeId(from)
      || !isValidMeshMessageId(incomingMessageId)
      || (incomingFormat != "TEXT" && incomingFormat != "JSON" && incomingFormat != "CUSTOM")
      || !parseFragmentNumber(frame.substring(separators[2] + 1, separators[3]), index)
      || !parseFragmentNumber(frame.substring(separators[3] + 1, separators[4]), count)
      || count == 0 || count > MESH_MAX_FRAGMENTS || index >= count) {
    resetMeshFragments();
    return MESH_FRAGMENT_INVALID;
  }
  String chunk = frame.substring(separators[4] + 1);
  if (index == 0) {
    resetMeshFragments();
    g_fragmentFrom = from;
    g_fragmentMessageId = incomingMessageId;
    g_fragmentFormat = incomingFormat;
    g_fragmentCount = count;
    g_fragmentStartedAt = millis();
    g_fragmentBody.reserve(count * MESH_FRAGMENT_BODY_BYTES);
  }
  if (millis() - g_fragmentStartedAt > MESH_REASSEMBLY_TIMEOUT_MS
      || from != g_fragmentFrom || incomingMessageId != g_fragmentMessageId
      || incomingFormat != g_fragmentFormat || count != g_fragmentCount
      || index != g_fragmentNextIndex || g_fragmentBody.length() + chunk.length() > MAX_MESSAGE_LENGTH * 3U) {
    resetMeshFragments();
    return MESH_FRAGMENT_INVALID;
  }
  g_fragmentBody += chunk;
  g_fragmentNextIndex++;
  if (g_fragmentNextIndex != g_fragmentCount) return MESH_FRAGMENT_INCOMPLETE;
  messageId = g_fragmentMessageId;
  format = decodeMailboxFormat(g_fragmentFormat);
  body = unescapeMailboxPayload(g_fragmentBody);
  resetMeshFragments();
  return body.length() > 0 ? MESH_FRAGMENT_COMPLETE : MESH_FRAGMENT_INVALID;
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
  if (!isValidMeshMessageId(messageId) || acknowledgingNodeId.length() != 8) return false;
  for (size_t i = 0; i < acknowledgingNodeId.length(); i++) {
    if (!isxdigit(acknowledgingNodeId.charAt(i))) return false;
  }
  return true;
}

static String buildMeshMailboxAck(const String& messageId) {
  return String(MESH_MAILBOX_ACK_PREFIX) + messageId + ":" + getLocalNodeId();
}

static void onNodeReport(mt_node_t* node, mt_nr_progress_t progress) {
  if (node == nullptr) {
    g_nodeReportInFlight = false;
    if (progress == MT_NR_INVALID) {
      logAuditEvent(AUDIT_EVENT_ERROR, 1, "MESH", "Meshtastic node report invalid", false);
    }
    return;
  }
  if (node->last_heard_position == 0
      || !validateCoordinate((float)node->latitude, true)
      || !validateCoordinate((float)node->longitude, false)) {
    return;
  }
  String nodeId = getNodeId(node->node_num);
  String cot = generateLocationCoT(nodeId, (float)node->latitude, (float)node->longitude, (float)node->altitude);
  if (cot.length() == 0) return;
#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
  sendDataSerial((const uint8_t*)cot.c_str(), cot.length());
#endif
#if defined(ENABLE_BLE) && ENABLE_BLE
  sendDataBLE((const uint8_t*)cot.c_str(), cot.length());
#endif
}

// Callback for when a Meshtastic text message is received
static void onTextMessage(uint32_t from, uint32_t to, uint8_t channel, const char *text) {
  (void)to;
  Serial.printf("Received Meshtastic text from %s on channel %u\n", getNodeId(from).c_str(), channel);

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
    if (acknowledgingNodeId != getNodeId(from)) {
      logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "MESH", "Mailbox acknowledgement identity mismatch", false);
      return;
    }
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

  String format = "";
  String mailboxPayload = "";
  MeshFragmentResult fragmentResult = consumeMeshFragment(from, payloadStr, messageId, format, mailboxPayload);
  if (fragmentResult == MESH_FRAGMENT_INCOMPLETE) return;
  if (fragmentResult == MESH_FRAGMENT_INVALID) {
    logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 1, "MESH", "Invalid mailbox fragment rejected", false);
    return;
  }
  if (fragmentResult == MESH_FRAGMENT_COMPLETE) {
    String originNodeId = getNodeId(from);
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
  unsigned long now = millis();
  bool ready = mt_loop(now);
  if (ready && !g_nodeReportInFlight && now - g_lastNodeReportRequest >= NODE_REPORT_INTERVAL_MS) {
    g_nodeReportInFlight = mt_request_node_report(onNodeReport);
    g_lastNodeReportRequest = now;
  }
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
  String escaped = escapeMailboxPayload(payload);
  size_t count = (escaped.length() + MESH_FRAGMENT_BODY_BYTES - 1) / MESH_FRAGMENT_BODY_BYTES;
  if (count == 0 || count > MESH_MAX_FRAGMENTS) return false;
  for (size_t index = 0; index < count; index++) {
    size_t start = index * MESH_FRAGMENT_BODY_BYTES;
    size_t end = start + MESH_FRAGMENT_BODY_BYTES;
    if (end > escaped.length()) end = escaped.length();
    String frame = String(MESH_MAILBOX_FRAGMENT_PREFIX) + getLocalNodeId() + ":" + messageId + ":"
        + encodeMailboxFormat(format) + ":" + String(index) + ":" + String(count) + ":"
        + escaped.substring(start, end);
    if (frame.length() > 230 || !mt_send_text(frame.c_str(), BROADCAST_ADDR, 0)) return false;
  }
  return true;
}
