// File: firmware/src/security.cpp
// Description: Security implementation for encryption, authentication, and integrity.
// CRITICAL: For military/law enforcement use

#include "security.h"
#include "config.h"
#include <esp_random.h>
#include <mbedtls/pkcs5.h>
#include <string.h>

// Security keys (should be provisioned securely - NOT hardcoded in production!)
static uint8_t g_aes_key[AES_KEY_SIZE];
static uint8_t g_hmac_key[HMAC_KEY_SIZE];
static uint8_t g_auth_token[AUTH_TOKEN_SIZE];
static SecurityStatus g_security_status = {0};
static bool g_security_initialized = false;

// Temporary IV storage
static uint8_t g_iv[IV_SIZE];

static void deriveProvisionedKey(const String& deviceId,
                                 const String& sharedSecret,
                                 const char* purpose,
                                 uint8_t* outKey,
                                 size_t outLen) {
    if (outKey == nullptr || outLen == 0) {
        return;
    }

    // Build salt from deviceId + purpose so each derived key is unique.
    String salt = deviceId + ":" + String(purpose);

    // PBKDF2-HMAC-SHA256 with 100 000 iterations.
    mbedtls_md_context_t ctx;
    const mbedtls_md_info_t *md_info = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    mbedtls_md_init(&ctx);
    mbedtls_md_setup(&ctx, md_info, 1); // HMAC mode
    mbedtls_pkcs5_hmac_ext(
        MBEDTLS_MD_SHA256,
        reinterpret_cast<const unsigned char*>(sharedSecret.c_str()),
        sharedSecret.length(),
        reinterpret_cast<const unsigned char*>(salt.c_str()),
        salt.length(),
        100000,
        outLen,
        outKey);
    mbedtls_md_free(&ctx);

    // Best-effort wipe of the Arduino String holding secret material
    for (unsigned int i = 0; i < salt.length(); i++) {
        salt.setCharAt(i, '\0');
    }
}

bool initSecurity(const uint8_t* aes_key, const uint8_t* hmac_key, uint8_t security_mode) {
    if (aes_key == nullptr || hmac_key == nullptr) {
        return false;
    }
    
    memcpy(g_aes_key, aes_key, AES_KEY_SIZE);
    memcpy(g_hmac_key, hmac_key, HMAC_KEY_SIZE);
    g_security_status.security_mode = security_mode;
    g_security_status.encryption_enabled = (security_mode != SECURITY_MODE_NONE);
    g_security_status.initialized = true;
    g_security_initialized = true;
    
    // Generate initial auth token
    generateAuthToken(g_auth_token);
    
    return true;
}

bool initSecurityFromProvisioning(const String& deviceId, const String& sharedSecret) {
    if (deviceId.length() == 0 || sharedSecret.length() < 12) {
        return false;
    }

    uint8_t aesKey[AES_KEY_SIZE] = {0};
    uint8_t hmacKey[HMAC_KEY_SIZE] = {0};
    deriveProvisionedKey(deviceId, sharedSecret, "aes", aesKey, sizeof(aesKey));
    deriveProvisionedKey(deviceId, sharedSecret, "hmac", hmacKey, sizeof(hmacKey));

    cleanupSecurity();
    bool initialized = initSecurity(aesKey, hmacKey, SECURITY_MODE_AES256_HMAC);
    memset(aesKey, 0, sizeof(aesKey));
    memset(hmacKey, 0, sizeof(hmacKey));
    return initialized;
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

    if (mbedtls_gcm_setkey(&gcm, MBEDTLS_CIPHER_ID_AES, g_aes_key, 256) != 0) {
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

    if (mbedtls_gcm_setkey(&gcm, MBEDTLS_CIPHER_ID_AES, g_aes_key, 256) != 0) {
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

void generateHMAC(const uint8_t* data, size_t data_len, uint8_t* hmac_out) {
    if (data == nullptr || hmac_out == nullptr || !g_security_initialized) {
        return;
    }
    
    mbedtls_md_context_t ctx;
    const mbedtls_md_info_t *md_info = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    
    mbedtls_md_init(&ctx);
    mbedtls_md_setup(&ctx, md_info, 1); // HMAC mode
    mbedtls_md_hmac_starts(&ctx, g_hmac_key, HMAC_KEY_SIZE);
    mbedtls_md_hmac_update(&ctx, data, data_len);
    mbedtls_md_hmac_finish(&ctx, hmac_out);
    mbedtls_md_free(&ctx);
}

bool verifyHMAC(const uint8_t* data, size_t data_len, const uint8_t* hmac) {
    if (data == nullptr || hmac == nullptr || !g_security_initialized) {
        g_security_status.integrity_failures++;
        return false;
    }
    
    uint8_t calculated_hmac[32];
    generateHMAC(data, data_len, calculated_hmac);
    
    // Constant-time comparison to prevent timing attacks
    volatile uint8_t diff = 0;
    for (size_t i = 0; i < 32; i++) {
        diff |= calculated_hmac[i] ^ hmac[i];
    }
    bool valid = (diff == 0);
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
    // Clear sensitive data from memory
    memset(g_aes_key, 0, AES_KEY_SIZE);
    memset(g_hmac_key, 0, HMAC_KEY_SIZE);
    memset(g_auth_token, 0, AUTH_TOKEN_SIZE);
    memset(g_iv, 0, IV_SIZE);
    g_security_initialized = false;
    memset(&g_security_status, 0, sizeof(SecurityStatus));
}

