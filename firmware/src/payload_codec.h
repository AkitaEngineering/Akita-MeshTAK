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

// Decode an incoming (potentially encrypted) payload string.
// If the input starts with the encrypted prefix, decrypt it; otherwise pass through.
// Returns true on success, writing the plaintext to `output`.
bool decodeIncomingPayload(const String& input, String& output);

// Encode an outgoing payload, encrypting it when encryption is active.
// Returns true on success, writing the envelope to `output`.
bool encodeOutgoingPayload(const uint8_t* data, size_t len, String& output);

#endif // PAYLOAD_CODEC_H
