// File: firmware/src/security.h
// Description: Security module for encryption, authentication, and message integrity.
// CRITICAL: For military/law enforcement use - implements encryption and authentication

#ifndef SECURITY_H
#define SECURITY_H

#include <Arduino.h>
#include <mbedtls/gcm.h>
#include <mbedtls/sha256.h>
#include <mbedtls/md.h>

// Security configuration
#define AES_KEY_SIZE 32          // 256-bit AES key
#define HMAC_KEY_SIZE 32         // 256-bit HMAC key
#define IV_SIZE 12               // AES-GCM nonce size
#define GCM_TAG_SIZE 16          // AES-GCM authentication tag size
#define MAX_MESSAGE_SIZE 512     // Maximum encrypted message size
#define AUTH_TOKEN_SIZE 16       // Authentication token size

// Security modes
#define SECURITY_MODE_NONE 0
#define SECURITY_MODE_AES256 1
#define SECURITY_MODE_AES256_HMAC 2

// Security status
typedef struct {
    bool initialized;
    bool encryption_enabled;
    uint8_t security_mode;
    uint32_t messages_encrypted;
    uint32_t messages_decrypted;
    uint32_t auth_failures;
    uint32_t integrity_failures;
} SecurityStatus;

// Initialize security module with keys
// Keys should be provisioned securely during device setup
bool initSecurity(const uint8_t* aes_key, const uint8_t* hmac_key, uint8_t security_mode);

// Initialize security module from runtime provisioning material.
bool initSecurityFromProvisioning(const String& deviceId, const String& sharedSecret);

// Initialize current and optional previous key slots for overlapping rotation.
bool initSecurityFromKeySlots(const String& deviceId,
                              const String& currentSecret,
                              const String& currentKeyId,
                              const String& previousSecret,
                              const String& previousKeyId);

// Load NVS provisioning material and initialize current plus previous slots.
bool initSecurityFromStore();

// Active outgoing key-id, or ENCRYPTED_KEY_ID when uninitialized.
const char* getActiveKeyId();

// Previous overlapping key-id, or empty string when none is loaded.
const char* getPreviousKeyId();

// True when the key-id matches the current or previous loaded slot.
bool acceptsKeyId(const String& keyId);

// Encrypt data with AES-256-GCM
// Returns encrypted data length, or 0 on error
size_t encryptData(const uint8_t* plaintext, size_t plaintext_len,
                   uint8_t* ciphertext, size_t ciphertext_max_len,
                   uint8_t* iv_out);

// Decrypt data with AES-256-GCM
// Returns decrypted data length, or 0 on error
size_t decryptData(const uint8_t* ciphertext, size_t ciphertext_len,
                   const uint8_t* iv, uint8_t* plaintext, size_t plaintext_max_len);

// Decrypt using the slot matching key_id.
size_t decryptDataForKeyId(const String& keyId,
                           const uint8_t* ciphertext, size_t ciphertext_len,
                           const uint8_t* iv, uint8_t* plaintext, size_t plaintext_max_len);

// Generate HMAC-SHA256 for message integrity
// Returns false on any cryptographic failure. Callers must not use hmac_out
// unless this function succeeds.
bool generateHMAC(const uint8_t* data, size_t data_len, uint8_t* hmac_out);

// Generate HMAC-SHA256 using the slot matching key_id.
bool generateHMACForKeyId(const String& keyId, const uint8_t* data, size_t data_len, uint8_t* hmac_out);

// Verify HMAC-SHA256 for message integrity
// Returns true if HMAC is valid
bool verifyHMAC(const uint8_t* data, size_t data_len, const uint8_t* hmac);

// Verify HMAC-SHA256 using the slot matching key_id.
bool verifyHMACForKeyId(const String& keyId, const uint8_t* data, size_t data_len, const uint8_t* hmac);

// Generate authentication token
void generateAuthToken(uint8_t* token_out);

// Verify authentication token
bool verifyAuthToken(const uint8_t* token);

// Secure random number generation
void secureRandom(uint8_t* buffer, size_t len);

// Get security status
SecurityStatus getSecurityStatus();

// Cleanup security resources
void cleanupSecurity();

#endif // SECURITY_H

