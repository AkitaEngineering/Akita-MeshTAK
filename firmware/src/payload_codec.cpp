// File: firmware/src/payload_codec.cpp
// Description: Shared encrypted payload encode/decode and hex utilities.

#include "payload_codec.h"
#include "config.h"
#include "input_validation.h"
#include "security.h"
#include <string.h>
#include <time.h>

static const unsigned long ENVELOPE_MAX_SKEW_SECONDS = 300;
static const int REPLAY_CACHE_SIZE = 12;
static String g_seenNonces[REPLAY_CACHE_SIZE];
static int g_seenNonceIndex = 0;

bool parseHexPayload(const String& hex, uint8_t* out, size_t outMax, size_t* outLen) {
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

String encodeHexPayload(const uint8_t* data, size_t len) {
  static const char* HEX_CHARS = "0123456789abcdef";
  String out = "";
  out.reserve(len * 2);
  for (size_t i = 0; i < len; i++) {
    out += HEX_CHARS[(data[i] >> 4) & 0x0F];
    out += HEX_CHARS[data[i] & 0x0F];
  }
  return out;
}

static bool hasTrustedClock() {
  return time(nullptr) >= 1609459200;
}

static bool parseUnsignedLongLong(const String& value, unsigned long long& out) {
  if (value.length() == 0) {
    return false;
  }
  unsigned long long parsed = 0;
  for (size_t i = 0; i < value.length(); i++) {
    char c = value.charAt(i);
    if (c < '0' || c > '9') {
      return false;
    }
    parsed = (parsed * 10ULL) + (unsigned long long)(c - '0');
  }
  out = parsed;
  return true;
}

static bool isReplayNonce(const String& keyId, const String& nonceHex) {
  String cacheKey = keyId + ":" + nonceHex;
  for (int i = 0; i < REPLAY_CACHE_SIZE; i++) {
    if (g_seenNonces[i] == cacheKey) {
      return true;
    }
  }
  g_seenNonces[g_seenNonceIndex] = cacheKey;
  g_seenNonceIndex = (g_seenNonceIndex + 1) % REPLAY_CACHE_SIZE;
  return false;
}

static bool verifyEnvelopeHmac(const String& signedData, const String& hmacHex) {
  uint8_t expected[HMAC_KEY_SIZE] = {0};
  size_t expectedLen = 0;
  if (!parseHexPayload(hmacHex, expected, sizeof(expected), &expectedLen) || expectedLen != HMAC_KEY_SIZE) {
    return false;
  }
  return verifyHMAC(reinterpret_cast<const uint8_t*>(signedData.c_str()), signedData.length(), expected);
}

bool decodeIncomingPayload(const String& input, String& output) {
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
  if (keyId != ENCRYPTED_KEY_ID) {
    return false;
  }

  String hex = "";
  if (version == "v1") {
    hex = headerAndHex.substring(secondSep + 1);
  } else if (version == ENCRYPTED_PAYLOAD_VERSION) {
    int thirdSep = headerAndHex.indexOf(':', secondSep + 1);
    int fourthSep = headerAndHex.indexOf(':', thirdSep + 1);
    int fifthSep = headerAndHex.indexOf(':', fourthSep + 1);
    if (thirdSep <= secondSep + 1 || fourthSep <= thirdSep + 1 || fifthSep <= fourthSep + 1) {
      return false;
    }

    String timestampText = headerAndHex.substring(secondSep + 1, thirdSep);
    String nonceHex = headerAndHex.substring(thirdSep + 1, fourthSep);
    hex = headerAndHex.substring(fourthSep + 1, fifthSep);
    String hmacHex = headerAndHex.substring(fifthSep + 1);

    unsigned long long envelopeTime = 0;
    if (!parseUnsignedLongLong(timestampText, envelopeTime)) {
      return false;
    }
    if (hasTrustedClock()) {
      unsigned long long now = (unsigned long long)time(nullptr);
      unsigned long long delta = now > envelopeTime ? now - envelopeTime : envelopeTime - now;
      if (delta > ENVELOPE_MAX_SKEW_SECONDS) {
        return false;
      }
    }
    if (nonceHex.length() < 16 || isReplayNonce(keyId, nonceHex)) {
      return false;
    }

    String signedData = version + ":" + keyId + ":" + timestampText + ":" + nonceHex + ":" + hex;
    if (!verifyEnvelopeHmac(signedData, hmacHex)) {
      return false;
    }
  } else {
    return false;
  }

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

bool encodeOutgoingPayload(const uint8_t* data, size_t len, String& output) {
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

  uint8_t nonce[8] = {0};
  secureRandom(nonce, sizeof(nonce));
  String timestampText = String((unsigned long long)time(nullptr));
  String nonceHex = encodeHexPayload(nonce, sizeof(nonce));
  String envelopeHex = encodeHexPayload(envelope, IV_SIZE + encryptedLen);
  String signedData = String(ENCRYPTED_PAYLOAD_VERSION) + ":" + String(ENCRYPTED_KEY_ID) + ":" +
                      timestampText + ":" + nonceHex + ":" + envelopeHex;
  uint8_t hmac[HMAC_KEY_SIZE] = {0};
  generateHMAC(reinterpret_cast<const uint8_t*>(signedData.c_str()), signedData.length(), hmac);
  String hmacHex = encodeHexPayload(hmac, sizeof(hmac));
  memset(hmac, 0, sizeof(hmac));
  memset(nonce, 0, sizeof(nonce));

  output = String(ENCRYPTED_PAYLOAD_PREFIX) + signedData + ":" + hmacHex;
  return true;
}
