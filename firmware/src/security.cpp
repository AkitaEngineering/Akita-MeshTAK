// File: firmware/src/security.cpp
// Description: Security implementation for encryption, authentication, and integrity.
// CRITICAL: For military/law enforcement use

#include "security.h"
#include "config.h"
#include "provisioning_store.h"
#include <esp_random.h>
#include <mbedtls/md.h>
#include <mbedtls/pkcs5.h>
#include <string.h>

#define KEY_ID_STORAGE_LEN 8

struct KeySlot {
    char key_id[KEY_ID_STORAGE_LEN];
    uint8_t aes_key[AES_KEY_SIZE];
    uint8_t hmac_key[HMAC_KEY_SIZE];
    bool loaded;
};

static KeySlot g_current_slot = {0};
static KeySlot g_previous_slot = {0};
static uint8_t g_auth_token[AUTH_TOKEN_SIZE];
static SecurityStatus g_security_status = {0};
static bool g_security_initialized = false;

// Temporary IV storage
static uint8_t g_iv[IV_SIZE];

static bool isAllZero(const uint8_t* buffer, size_t len) {
    if (buffer == nullptr || len == 0) {
        return true;
    }

    for (size_t i = 0; i < len; i++) {
        if (buffer[i] != 0) {
            return false;
        }
    }
    return true;
}

static bool deriveProvisionedKey(const String& deviceId,
                                 const String& sharedSecret,
                                 const char* purpose,
                                 uint8_t* outKey,
                                 size_t outLen) {
    if (outKey == nullptr || outLen == 0) {
        return false;
    }

    memset(outKey, 0, outLen);

    // Build salt from deviceId + purpose so each derived key is unique.
    String salt = deviceId + ":" + String(purpose);
    bool derived = false;

    // PBKDF2-HMAC-SHA256 with 100 000 iterations.
    mbedtls_md_context_t ctx;
    const mbedtls_md_info_t *md_info = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    mbedtls_md_init(&ctx);
    if (md_info == nullptr || mbedtls_md_setup(&ctx, md_info, 1) != 0) {
        mbedtls_md_free(&ctx);
        for (unsigned int i = 0; i < salt.length(); i++) {
            salt.setCharAt(i, '\0');
        }
        return false;
    }

    if (mbedtls_pkcs5_pbkdf2_hmac(
            &ctx,
            reinterpret_cast<const unsigned char*>(sharedSecret.c_str()),
            sharedSecret.length(),
            reinterpret_cast<const unsigned char*>(salt.c_str()),
            salt.length(),
            100000,
            outLen,
            outKey) != 0) {
        memset(outKey, 0, outLen);
    } else {
        derived = !isAllZero(outKey, outLen);
        if (!derived) {
            memset(outKey, 0, outLen);
        }
    }
    mbedtls_md_free(&ctx);

    // Best-effort wipe of the Arduino String holding secret material
    for (unsigned int i = 0; i < salt.length(); i++) {
        salt.setCharAt(i, '\0');
    }

    return derived;
}

static void clearKeySlot(KeySlot* slot) {
    if (slot == nullptr) {
        return;
    }
    memset(slot, 0, sizeof(*slot));
}

static bool copyKeyId(KeySlot* slot, const String& keyId) {
    if (slot == nullptr || !isKnownKeyId(keyId) || keyId.length() >= KEY_ID_STORAGE_LEN) {
        return false;
    }
    memset(slot->key_id, 0, sizeof(slot->key_id));
    keyId.toCharArray(slot->key_id, sizeof(slot->key_id));
    return true;
}

static bool loadKeySlot(KeySlot* slot,
                        const String& deviceId,
                        const String& sharedSecret,
                        const String& keyId) {
    if (slot == nullptr) {
        return false;
    }
    clearKeySlot(slot);
    if (deviceId.length() == 0 || sharedSecret.length() < 16 || !copyKeyId(slot, keyId)) {
        return false;
    }
    if (!deriveProvisionedKey(deviceId, sharedSecret, "aes", slot->aes_key, sizeof(slot->aes_key))
            || !deriveProvisionedKey(deviceId, sharedSecret, "hmac", slot->hmac_key, sizeof(slot->hmac_key))) {
        clearKeySlot(slot);
        return false;
    }
    slot->loaded = true;
    return true;
}

static KeySlot* slotForKeyId(const String& keyId) {
    if (g_current_slot.loaded && keyId == String(g_current_slot.key_id)) {
        return &g_current_slot;
    }
    if (g_previous_slot.loaded && keyId == String(g_previous_slot.key_id)) {
        return &g_previous_slot;
    }
    return nullptr;
}

bool initSecurity(const uint8_t* aes_key, const uint8_t* hmac_key, uint8_t security_mode) {
    if (aes_key == nullptr || hmac_key == nullptr
            || isAllZero(aes_key, AES_KEY_SIZE)
            || isAllZero(hmac_key, HMAC_KEY_SIZE)) {
        return false;
    }

    cleanupSecurity();
    memcpy(g_current_slot.aes_key, aes_key, AES_KEY_SIZE);
    memcpy(g_current_slot.hmac_key, hmac_key, HMAC_KEY_SIZE);
    strncpy(g_current_slot.key_id, ENCRYPTED_KEY_ID, sizeof(g_current_slot.key_id) - 1);
    g_current_slot.loaded = true;
    g_security_status.security_mode = security_mode;
    g_security_status.encryption_enabled = (security_mode != SECURITY_MODE_NONE);
    g_security_status.initialized = true;
    g_security_initialized = true;

    generateAuthToken(g_auth_token);
    return true;
}

bool initSecurityFromProvisioning(const String& deviceId, const String& sharedSecret) {
    return initSecurityFromKeySlots(deviceId, sharedSecret, String(ENCRYPTED_KEY_ID), "", "");
}

bool initSecurityFromKeySlots(const String& deviceId,
                              const String& currentSecret,
                              const String& currentKeyId,
                              const String& previousSecret,
                              const String& previousKeyId) {
    KeySlot current = {0};
    KeySlot previous = {0};
    if (!loadKeySlot(&current, deviceId, currentSecret, currentKeyId)) {
        return false;
    }
    if (previousSecret.length() >= 16 && previousKeyId.length() > 0) {
        if (!loadKeySlot(&previous, deviceId, previousSecret, previousKeyId)) {
            clearKeySlot(&current);
            return false;
        }
    }

    cleanupSecurity();
    g_current_slot = current;
    g_previous_slot = previous;
    g_security_status.security_mode = SECURITY_MODE_AES256_HMAC;
    g_security_status.encryption_enabled = true;
    g_security_status.initialized = true;
    g_security_initialized = true;
    generateAuthToken(g_auth_token);
    clearKeySlot(&current);
    clearKeySlot(&previous);
    return true;
}

bool initSecurityFromStore() {
    ProvisioningMaterial material;
    if (!loadProvisioningMaterial(material)) {
        return false;
    }
    String previousKeyId = material.previousSecret.length() >= 16
        ? nextKeyId(material.currentKeyId)
        : String("");
    bool initialized = initSecurityFromKeySlots(
        String(DEVICE_ID),
        material.currentSecret,
        material.currentKeyId,
        material.previousSecret,
        previousKeyId);
    for (size_t i = 0; i < material.currentSecret.length(); i++) {
        material.currentSecret.setCharAt(i, '\0');
    }
    for (size_t i = 0; i < material.previousSecret.length(); i++) {
        material.previousSecret.setCharAt(i, '\0');
    }
    return initialized;
}

const char* getActiveKeyId() {
    if (g_current_slot.loaded && g_current_slot.key_id[0] != '\0') {
        return g_current_slot.key_id;
    }
    return ENCRYPTED_KEY_ID;
}

const char* getPreviousKeyId() {
    if (g_previous_slot.loaded && g_previous_slot.key_id[0] != '\0') {
        return g_previous_slot.key_id;
    }
    return "";
}

bool acceptsKeyId(const String& keyId) {
    return slotForKeyId(keyId) != nullptr;
}

size_t encryptData(const uint8_t* plaintext, size_t plaintext_len,
                   uint8_t* ciphertext, size_t ciphertext_max_len,
                   uint8_t* iv_out) {
    if (!g_security_initialized || plaintext == nullptr || ciphertext == nullptr) {
        return 0;
    }

    if (plaintext_len == 0 || iv_out == nullptr || ciphertext_max_len < plaintext_len + GCM_TAG_SIZE) {
        return 0;
    }

    // Generate random IV
    secureRandom(iv_out, IV_SIZE);
    memcpy(g_iv, iv_out, IV_SIZE);

    mbedtls_gcm_context gcm;
    mbedtls_gcm_init(&gcm);

    if (!g_current_slot.loaded
            || mbedtls_gcm_setkey(&gcm, MBEDTLS_CIPHER_ID_AES, g_current_slot.aes_key, 256) != 0) {
        mbedtls_gcm_free(&gcm);
        return 0;
    }

    uint8_t tag[GCM_TAG_SIZE] = {0};
    if (mbedtls_gcm_crypt_and_tag(&gcm, MBEDTLS_GCM_ENCRYPT, plaintext_len,
                                  g_iv, IV_SIZE,
                                  nullptr, 0,
                                  plaintext, ciphertext,
                                  GCM_TAG_SIZE, tag) != 0) {
        mbedtls_gcm_free(&gcm);
        return 0;
    }

    memcpy(ciphertext + plaintext_len, tag, GCM_TAG_SIZE);
    mbedtls_gcm_free(&gcm);
    g_security_status.messages_encrypted++;

    return plaintext_len + GCM_TAG_SIZE;
}

size_t decryptData(const uint8_t* ciphertext, size_t ciphertext_len,
                   const uint8_t* iv, uint8_t* plaintext, size_t plaintext_max_len) {
    if (!g_security_initialized || ciphertext == nullptr || plaintext == nullptr || iv == nullptr) {
        return 0;
    }

    if (ciphertext_len <= GCM_TAG_SIZE || plaintext_max_len < (ciphertext_len - GCM_TAG_SIZE)) {
        return 0;
    }

    mbedtls_gcm_context gcm;
    mbedtls_gcm_init(&gcm);

    if (!g_current_slot.loaded
            || mbedtls_gcm_setkey(&gcm, MBEDTLS_CIPHER_ID_AES, g_current_slot.aes_key, 256) != 0) {
        mbedtls_gcm_free(&gcm);
        return 0;
    }

    size_t dataLen = ciphertext_len - GCM_TAG_SIZE;
    const uint8_t* tag = ciphertext + dataLen;

    if (mbedtls_gcm_auth_decrypt(&gcm, dataLen,
                                 iv, IV_SIZE,
                                 nullptr, 0,
                                 tag, GCM_TAG_SIZE,
                                 ciphertext, plaintext) != 0) {
        mbedtls_gcm_free(&gcm);
        g_security_status.auth_failures++;
        return 0;
    }

    mbedtls_gcm_free(&gcm);
    g_security_status.messages_decrypted++;

    return dataLen;
}

size_t decryptDataForKeyId(const String& keyId,
                           const uint8_t* ciphertext, size_t ciphertext_len,
                           const uint8_t* iv, uint8_t* plaintext, size_t plaintext_max_len) {
    KeySlot* slot = slotForKeyId(keyId);
    if (slot == nullptr || !slot->loaded || ciphertext == nullptr || plaintext == nullptr || iv == nullptr) {
        return 0;
    }
    if (ciphertext_len <= GCM_TAG_SIZE || plaintext_max_len < (ciphertext_len - GCM_TAG_SIZE)) {
        return 0;
    }

    mbedtls_gcm_context gcm;
    mbedtls_gcm_init(&gcm);
    if (mbedtls_gcm_setkey(&gcm, MBEDTLS_CIPHER_ID_AES, slot->aes_key, 256) != 0) {
        mbedtls_gcm_free(&gcm);
        return 0;
    }

    size_t dataLen = ciphertext_len - GCM_TAG_SIZE;
    const uint8_t* tag = ciphertext + dataLen;
    if (mbedtls_gcm_auth_decrypt(&gcm, dataLen,
                                 iv, IV_SIZE,
                                 nullptr, 0,
                                 tag, GCM_TAG_SIZE,
                                 ciphertext, plaintext) != 0) {
        mbedtls_gcm_free(&gcm);
        g_security_status.auth_failures++;
        return 0;
    }

    mbedtls_gcm_free(&gcm);
    g_security_status.messages_decrypted++;
    return dataLen;
}

bool generateHMAC(const uint8_t* data, size_t data_len, uint8_t* hmac_out) {
    return generateHMACForKeyId(String(getActiveKeyId()), data, data_len, hmac_out);
}

bool generateHMACForKeyId(const String& keyId, const uint8_t* data, size_t data_len, uint8_t* hmac_out) {
    if (hmac_out != nullptr) {
        memset(hmac_out, 0, HMAC_KEY_SIZE);
    }

    KeySlot* slot = slotForKeyId(keyId);
    if (data == nullptr || hmac_out == nullptr || !g_security_initialized || slot == nullptr || !slot->loaded) {
        return false;
    }

    mbedtls_md_context_t ctx;
    const mbedtls_md_info_t *md_info = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);

    mbedtls_md_init(&ctx);
    if (md_info == nullptr || mbedtls_md_setup(&ctx, md_info, 1) != 0) {
        mbedtls_md_free(&ctx);
        return false;
    }

    bool success = true;
    if (mbedtls_md_hmac_starts(&ctx, slot->hmac_key, HMAC_KEY_SIZE) != 0
            || mbedtls_md_hmac_update(&ctx, data, data_len) != 0
            || mbedtls_md_hmac_finish(&ctx, hmac_out) != 0) {
        memset(hmac_out, 0, HMAC_KEY_SIZE);
        success = false;
    }
    mbedtls_md_free(&ctx);
    return success;
}

bool verifyHMAC(const uint8_t* data, size_t data_len, const uint8_t* hmac) {
    return verifyHMACForKeyId(String(getActiveKeyId()), data, data_len, hmac);
}

bool verifyHMACForKeyId(const String& keyId, const uint8_t* data, size_t data_len, const uint8_t* hmac) {
    if (data == nullptr || hmac == nullptr || !g_security_initialized) {
        g_security_status.integrity_failures++;
        return false;
    }

    uint8_t calculated_hmac[HMAC_KEY_SIZE] = {0};
    if (!generateHMACForKeyId(keyId, data, data_len, calculated_hmac)) {
        g_security_status.integrity_failures++;
        return false;
    }

    // Constant-time comparison to prevent timing attacks
    volatile uint8_t diff = 0;
    for (size_t i = 0; i < HMAC_KEY_SIZE; i++) {
        diff |= calculated_hmac[i] ^ hmac[i];
    }
    bool valid = (diff == 0);
    memset(calculated_hmac, 0, sizeof(calculated_hmac));
    if (!valid) {
        g_security_status.integrity_failures++;
    }

    return valid;
}

void generateAuthToken(uint8_t* token_out) {
    if (token_out == nullptr) return;
    secureRandom(token_out, AUTH_TOKEN_SIZE);
}

bool verifyAuthToken(const uint8_t* token) {
    if (token == nullptr || !g_security_initialized) {
        return false;
    }
    // Constant-time comparison to prevent timing attacks
    volatile uint8_t diff = 0;
    for (size_t i = 0; i < AUTH_TOKEN_SIZE; i++) {
        diff |= token[i] ^ g_auth_token[i];
    }
    return (diff == 0);
}

void secureRandom(uint8_t* buffer, size_t len) {
    if (buffer == nullptr || len == 0) return;

    // Use ESP32 hardware random number generator
    for (size_t i = 0; i < len; i++) {
        buffer[i] = (uint8_t)esp_random();
    }
}

SecurityStatus getSecurityStatus() {
    return g_security_status;
}

void cleanupSecurity() {
    clearKeySlot(&g_current_slot);
    clearKeySlot(&g_previous_slot);
    memset(g_auth_token, 0, AUTH_TOKEN_SIZE);
    memset(g_iv, 0, IV_SIZE);
    g_security_initialized = false;
    memset(&g_security_status, 0, sizeof(SecurityStatus));
}

