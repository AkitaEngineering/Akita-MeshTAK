// File: firmware/src/security.cpp
// Description: Security implementation for encryption, authentication, and integrity.
// CRITICAL: For military/law enforcement use

#include "security.h"
#include "config.h"
#include <esp_random.h>
#include <string.h>

// Security keys (should be provisioned securely - NOT hardcoded in production!)
static uint8_t g_aes_key[AES_KEY_SIZE];
static uint8_t g_hmac_key[HMAC_KEY_SIZE];
static uint8_t g_auth_token[AUTH_TOKEN_SIZE];
static SecurityStatus g_security_status = {0};
static bool g_security_initialized = false;

// Temporary IV storage
static uint8_t g_iv[IV_SIZE];

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

size_t encryptData(const uint8_t* plaintext, size_t plaintext_len, 
                   uint8_t* ciphertext, size_t ciphertext_max_len,
                   uint8_t* iv_out) {
    if (!g_security_initialized || plaintext == nullptr || ciphertext == nullptr) {
        return 0;
    }
    
    if (plaintext_len == 0 || ciphertext_max_len < plaintext_len + IV_SIZE + 16) {
        return 0; // Need space for IV and padding
    }
    
    // Generate random IV
    secureRandom(iv_out, IV_SIZE);
    memcpy(g_iv, iv_out, IV_SIZE);
    
    // Initialize AES context
    mbedtls_aes_context aes;
    mbedtls_aes_init(&aes);
    
    // Set encryption key
    if (mbedtls_aes_setkey_enc(&aes, g_aes_key, 256) != 0) {
        mbedtls_aes_free(&aes);
        return 0;
    }
    
    // Calculate padding
    size_t padding = 16 - (plaintext_len % 16);
    size_t padded_len = plaintext_len + padding;
    
    // Prepare padded plaintext
    uint8_t padded_plaintext[padded_len];
    memcpy(padded_plaintext, plaintext, plaintext_len);
    memset(padded_plaintext + plaintext_len, padding, padding);
    
    // Encrypt
    if (mbedtls_aes_crypt_cbc(&aes, MBEDTLS_AES_ENCRYPT, padded_len, 
                               g_iv, padded_plaintext, ciphertext) != 0) {
        mbedtls_aes_free(&aes);
        return 0;
    }
    
    mbedtls_aes_free(&aes);
    g_security_status.messages_encrypted++;
    
    return padded_len;
}

size_t decryptData(const uint8_t* ciphertext, size_t ciphertext_len,
                   const uint8_t* iv, uint8_t* plaintext, size_t plaintext_max_len) {
    if (!g_security_initialized || ciphertext == nullptr || plaintext == nullptr || iv == nullptr) {
        return 0;
    }
    
    if (ciphertext_len == 0 || ciphertext_len % 16 != 0 || plaintext_max_len < ciphertext_len) {
        return 0;
    }
    
    // Initialize AES context
    mbedtls_aes_context aes;
    mbedtls_aes_init(&aes);
    
    // Set decryption key
    if (mbedtls_aes_setkey_dec(&aes, g_aes_key, 256) != 0) {
        mbedtls_aes_free(&aes);
        return 0;
    }
    
    // Copy IV
    uint8_t iv_copy[IV_SIZE];
    memcpy(iv_copy, iv, IV_SIZE);
    
    // Decrypt
    if (mbedtls_aes_crypt_cbc(&aes, MBEDTLS_AES_DECRYPT, ciphertext_len,
                               iv_copy, ciphertext, plaintext) != 0) {
        mbedtls_aes_free(&aes);
        g_security_status.auth_failures++;
        return 0;
    }
    
    // Remove padding
    uint8_t padding = plaintext[ciphertext_len - 1];
    if (padding > 16 || padding == 0) {
        mbedtls_aes_free(&aes);
        g_security_status.integrity_failures++;
        return 0;
    }
    
    size_t plaintext_len = ciphertext_len - padding;
    mbedtls_aes_free(&aes);
    g_security_status.messages_decrypted++;
    
    return plaintext_len;
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
    
    bool valid = (memcmp(calculated_hmac, hmac, 32) == 0);
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
    // In production, implement proper token validation logic
    // For now, basic check
    return (memcmp(token, g_auth_token, AUTH_TOKEN_SIZE) == 0);
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

