// File: firmware/src/payload_codec.h
// Description: Shared encrypted payload encode/decode and hex utilities.
// Eliminates duplication between ble_setup.cpp and serial_bridge.cpp.

#ifndef PAYLOAD_CODEC_H
#define PAYLOAD_CODEC_H

#include <Arduino.h>

// Decode a hex-encoded string into a raw byte buffer.
// Returns true on success; sets *outLen to number of bytes decoded.
bool parseHexPayload(const String& hex, uint8_t* out, size_t outMax, size_t* outLen);

// Encode a raw byte buffer into a lowercase hex string.
String encodeHexPayload(const uint8_t* data, size_t len);

// Decode authenticated operational traffic. Plaintext is always rejected and is
// handled only by the physical-presence provisioning path.
// Returns true on success, writing the plaintext to `output`.
bool decodeIncomingPayload(const String& input, String& output, bool allowPhysicalProvisioning = false);

// Encode an outgoing payload. Security must be initialized and enabled.
// Returns true on success, writing the envelope to `output`.
bool encodeOutgoingPayload(const uint8_t* data, size_t len, String& output);

#endif // PAYLOAD_CODEC_H
