#ifndef TRANSPORT_FRAMING_H
#define TRANSPORT_FRAMING_H

#include <Arduino.h>

// Fits the full framing header inside the Android client's minimum accepted
// ATT MTU of 64 bytes (61-byte attribute payload).
#define BLE_FRAME_PAYLOAD_BYTES 40
#define BLE_MAX_FRAME_PARTS 128
#define BLE_MAX_REASSEMBLED_BYTES 2048

bool consumeBleFrame(const String& frame, String& completePayload);
String buildBleFrame(const String& messageId, size_t index, size_t count, const String& chunk);

#endif
