#include "transport_framing.h"

static String g_activeMessageId = "";
static size_t g_expectedIndex = 0;
static size_t g_expectedCount = 0;
static String g_payload = "";

static void resetFrameState() {
  g_activeMessageId = "";
  g_expectedIndex = 0;
  g_expectedCount = 0;
  for (size_t i = 0; i < g_payload.length(); i++) g_payload.setCharAt(i, '\0');
  g_payload = "";
}

static bool parseSize(const String& value, size_t& output) {
  if (value.length() == 0) return false;
  size_t parsed = 0;
  for (size_t i = 0; i < value.length(); i++) {
    char c = value.charAt(i);
    if (!isdigit(c)) return false;
    parsed = parsed * 10U + (size_t)(c - '0');
  }
  output = parsed;
  return true;
}

bool consumeBleFrame(const String& frame, String& completePayload) {
  completePayload = "";
  if (!frame.startsWith("F1|")) {
    resetFrameState();
    return false;
  }
  int idEnd = frame.indexOf('|', 3);
  int indexEnd = frame.indexOf('|', idEnd + 1);
  int countEnd = frame.indexOf('|', indexEnd + 1);
  if (idEnd != 11 || indexEnd <= idEnd + 1 || countEnd <= indexEnd + 1) {
    resetFrameState();
    return false;
  }
  String messageId = frame.substring(3, idEnd);
  size_t index = 0;
  size_t count = 0;
  if (!parseSize(frame.substring(idEnd + 1, indexEnd), index)
      || !parseSize(frame.substring(indexEnd + 1, countEnd), count)
      || count == 0 || count > BLE_MAX_FRAME_PARTS || index >= count) {
    resetFrameState();
    return false;
  }
  if (index == 0) {
    resetFrameState();
    g_activeMessageId = messageId;
    g_expectedCount = count;
    g_payload.reserve(count * BLE_FRAME_PAYLOAD_BYTES);
  }
  if (messageId != g_activeMessageId || count != g_expectedCount || index != g_expectedIndex) {
    resetFrameState();
    return false;
  }
  g_payload += frame.substring(countEnd + 1);
  if (g_payload.length() > BLE_MAX_REASSEMBLED_BYTES) {
    resetFrameState();
    return false;
  }
  g_expectedIndex++;
  if (g_expectedIndex != g_expectedCount) {
    return false;
  }
  completePayload = g_payload;
  resetFrameState();
  return true;
}

String buildBleFrame(const String& messageId, size_t index, size_t count, const String& chunk) {
  return String("F1|") + messageId + "|" + String(index) + "|" + String(count) + "|" + chunk;
}
