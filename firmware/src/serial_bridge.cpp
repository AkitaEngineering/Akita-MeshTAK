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
#include <string.h>

static bool parseHexPayload(const String& hex, uint8_t* out, size_t outMax, size_t* outLen) {
  if ((hex.length() % 2) != 0) {
    return false;
  }

  size_t decodedLen = hex.length() / 2;
  if (decodedLen > outMax) {
    return false;
  }

  for (size_t i = 0; i < decodedLen; i++) {
    char hi = hex.charAt(i * 2);
    char lo = hex.charAt(i * 2 + 1);

    auto hexValue = [](char c) -> int {
      if (c >= '0' && c <= '9') return c - '0';
      if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
      if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
      return -1;
    };

    int v1 = hexValue(hi);
    int v2 = hexValue(lo);
    if (v1 < 0 || v2 < 0) {
      return false;
    }
    out[i] = (uint8_t)((v1 << 4) | v2);
  }

  *outLen = decodedLen;
  return true;
}

static String encodeHexPayload(const uint8_t* data, size_t len) {
  static const char* HEX_CHARS = "0123456789abcdef";
  String out = "";
  out.reserve(len * 2);
  for (size_t i = 0; i < len; i++) {
    out += HEX_CHARS[(data[i] >> 4) & 0x0F];
    out += HEX_CHARS[data[i] & 0x0F];
  }
  return out;
}

static bool decodeIncomingPayload(const String& input, String& output) {
  if (!input.startsWith(ENCRYPTED_PAYLOAD_PREFIX)) {
    output = input;
    return true;
  }

  SecurityStatus status = getSecurityStatus();
  if (!status.initialized || !status.encryption_enabled) {
    return false;
  }

  String headerAndHex = input.substring(strlen(ENCRYPTED_PAYLOAD_PREFIX));
  int firstSep = headerAndHex.indexOf(':');
  int secondSep = headerAndHex.indexOf(':', firstSep + 1);
  if (firstSep <= 0 || secondSep <= firstSep + 1) {
    return false;
  }

  String version = headerAndHex.substring(0, firstSep);
  String keyId = headerAndHex.substring(firstSep + 1, secondSep);
  if (version != ENCRYPTED_PAYLOAD_VERSION || keyId != ENCRYPTED_KEY_ID) {
    return false;
  }

  String hex = headerAndHex.substring(secondSep + 1);
  uint8_t encryptedBuffer[MAX_MESSAGE_LENGTH * 2];
  size_t encryptedLen = 0;
  if (!parseHexPayload(hex, encryptedBuffer, sizeof(encryptedBuffer), &encryptedLen)) {
    return false;
  }

  if (encryptedLen <= IV_SIZE + GCM_TAG_SIZE) {
    return false;
  }

  const uint8_t* iv = encryptedBuffer;
  const uint8_t* ciphertext = encryptedBuffer + IV_SIZE;
  size_t ciphertextLen = encryptedLen - IV_SIZE;

  uint8_t plaintext[MAX_MESSAGE_LENGTH + 1] = {0};
  size_t plaintextLen = decryptData(ciphertext, ciphertextLen, iv, plaintext, MAX_MESSAGE_LENGTH);
  if (plaintextLen == 0) {
    return false;
  }

  plaintext[plaintextLen] = '\0';
  output = String((const char*)plaintext);
  output.trim();
  return true;
}

static bool encodeOutgoingPayload(const uint8_t* data, size_t len, String& output) {
  SecurityStatus status = getSecurityStatus();
  if (!status.initialized || !status.encryption_enabled) {
    output = String((const char*)data).substring(0, len);
    return true;
  }

  uint8_t ciphertext[MAX_MESSAGE_LENGTH + GCM_TAG_SIZE] = {0};
  uint8_t iv[IV_SIZE] = {0};
  size_t encryptedLen = encryptData(data, len, ciphertext, sizeof(ciphertext), iv);
  if (encryptedLen == 0) {
    return false;
  }

  uint8_t envelope[IV_SIZE + MAX_MESSAGE_LENGTH + GCM_TAG_SIZE] = {0};
  memcpy(envelope, iv, IV_SIZE);
  memcpy(envelope + IV_SIZE, ciphertext, encryptedLen);

  output = String(ENCRYPTED_PAYLOAD_PREFIX) + String(ENCRYPTED_PAYLOAD_VERSION) + ":" +
           String(ENCRYPTED_KEY_ID) + ":" + encodeHexPayload(envelope, IV_SIZE + encryptedLen);
  return true;
}

bool setupSerialBridge() {
  Serial.println("Initializing Serial Bridge...");
  Serial.println("Serial bridge ready.");
  return true;
}

void loopSerialBridge() {
  if (Serial.available() > 0) {
    String receivedData = Serial.readStringUntil('\n');
    receivedData.trim(); // Clean up whitespace
    if (receivedData.length() > 0) {
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
  
  Serial.print("Sent data via Serial: ");
  Serial.print(outgoingPayload.substring(0, 64)); // Limit serial output
  Serial.println();
  
  logAuditEvent(AUDIT_EVENT_DATA_SENT, 0, "SERIAL",
               String("Data sent, len: " + String(outgoingPayload.length())).c_str(), true);
}
#endif
