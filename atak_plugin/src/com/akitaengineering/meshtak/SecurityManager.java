// File: atak_plugin/src/com/akitaengineering/meshtak/SecurityManager.java
// Description: Security manager for encryption, authentication, and input validation.
// CRITICAL: For military/law enforcement use

package com.akitaengineering.meshtak;

import android.util.Log;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Security Manager for encryption, authentication, and message integrity.
 * Provides AES-256 encryption and HMAC-SHA256 for integrity checking.
 */
public class SecurityManager {
    private static final String TAG = "SecurityManager";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int AES_KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int HMAC_SIZE = 32;
    private static final int MIN_PROVISIONING_SECRET_LENGTH = 16;

    private static final class KeySlot {
        private final String keyId;
        private final SecretKey aesKey;
        private final SecretKey hmacKey;

        private KeySlot(String keyId, SecretKey aesKey, SecretKey hmacKey) {
            this.keyId = keyId;
            this.aesKey = aesKey;
            this.hmacKey = hmacKey;
        }
    }

    private KeySlot currentSlot;
    private KeySlot previousSlot;
    private volatile boolean initialized = false;
    private volatile boolean encryptionEnabled = true;

    // Security statistics
    private long messagesEncrypted = 0;
    private long messagesDecrypted = 0;
    private long integrityFailures = 0;
    private long authFailures = 0;

    private static SecurityManager instance;

    private SecurityManager() {
        // Private constructor for singleton
    }

    public static synchronized SecurityManager getInstance() {
        if (instance == null) {
            instance = new SecurityManager();
        }
        return instance;
    }

    /**
     * Initialize security with keys.
     * In production, keys should be provisioned securely, not hardcoded.
     */
    public synchronized boolean initialize(byte[] aesKeyBytes, byte[] hmacKeyBytes) {
        try {
            if (aesKeyBytes == null || aesKeyBytes.length != AES_KEY_SIZE / 8) {
                Log.e(TAG, "Invalid AES key length");
                return false;
            }
            if (isAllZero(aesKeyBytes)) {
                Log.e(TAG, "Refusing to initialize with an all-zero AES key");
                return false;
            }

            if (hmacKeyBytes == null || hmacKeyBytes.length != HMAC_SIZE) {
                Log.e(TAG, "Invalid HMAC key length");
                return false;
            }
            if (isAllZero(hmacKeyBytes)) {
                Log.e(TAG, "Refusing to initialize with an all-zero HMAC key");
                return false;
            }

            currentSlot = new KeySlot(
                    Config.ENCRYPTED_KEY_ID,
                    new SecretKeySpec(aesKeyBytes, "AES"),
                    new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM));
            previousSlot = null;
            initialized = true;

            Log.i(TAG, "Security manager initialized successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize security manager", e);
            return false;
        }
    }

    /**
     * Generate new keys (for initial setup).
     */
    public synchronized boolean generateKeys() {
        byte[] generatedAesKey = null;
        byte[] generatedHmacKey = null;
        try {
            // Generate AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            SecretKey newAesKey = keyGen.generateKey();

            // Generate HMAC key
            KeyGenerator hmacKeyGen = KeyGenerator.getInstance("HmacSHA256");
            hmacKeyGen.init(256);
            SecretKey newHmacKey = hmacKeyGen.generateKey();

            generatedAesKey = newAesKey.getEncoded();
            generatedHmacKey = newHmacKey.getEncoded();
            return initialize(generatedAesKey, generatedHmacKey);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate keys", e);
            return false;
        } finally {
            wipe(generatedAesKey);
            wipe(generatedHmacKey);
        }
    }

    /**
     * Derive deterministic AES/HMAC keys from provisioning material.
     */
    public synchronized boolean initializeFromProvisioning(String deviceId, String sharedSecret) {
        return initializeFromProvisioning(deviceId, sharedSecret, Config.ENCRYPTED_KEY_ID, null, null);
    }

    public synchronized boolean initializeFromProvisioning(String deviceId,
                                                           String currentSecret,
                                                           String currentKeyId,
                                                           String previousSecret,
                                                           String previousKeyId) {
        if (deviceId == null || deviceId.isEmpty() || currentSecret == null || currentSecret.isEmpty()) {
            Log.e(TAG, "Provisioning material is missing");
            return false;
        }
        if (currentSecret.length() < MIN_PROVISIONING_SECRET_LENGTH) {
            Log.e(TAG, "Provisioning secret is too short");
            return false;
        }
        String normalizedCurrentKeyId = Config.normalizeKeyId(currentKeyId);
        KeySlot loadedCurrent;
        KeySlot loadedPrevious = null;
        try {
            loadedCurrent = deriveSlot(deviceId, currentSecret, normalizedCurrentKeyId);
            if (previousSecret != null && previousSecret.length() >= MIN_PROVISIONING_SECRET_LENGTH
                    && Config.isKnownKeyId(previousKeyId)
                    && !normalizedCurrentKeyId.equals(previousKeyId)) {
                loadedPrevious = deriveSlot(deviceId, previousSecret, previousKeyId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize keys from provisioning", e);
            return false;
        }

        currentSlot = loadedCurrent;
        previousSlot = loadedPrevious;
        initialized = true;
        Log.i(TAG, "Security manager initialized with key " + normalizedCurrentKeyId
                + (loadedPrevious != null ? " and overlap " + loadedPrevious.keyId : ""));
        return true;
    }

    private KeySlot deriveSlot(String deviceId, String sharedSecret, String keyId) throws Exception {
        char[] sharedSecretChars = sharedSecret.toCharArray();
        byte[] aesKeyBytes = null;
        byte[] hmacKeyBytes = null;
        try {
            aesKeyBytes = deriveKeyMaterial(deviceId, sharedSecretChars, "aes");
            hmacKeyBytes = deriveKeyMaterial(deviceId, sharedSecretChars, "hmac");
            if (isAllZero(aesKeyBytes) || isAllZero(hmacKeyBytes)) {
                throw new IllegalStateException("Derived key material was zeroed");
            }
            return new KeySlot(
                    keyId,
                    new SecretKeySpec(aesKeyBytes, "AES"),
                    new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM));
        } finally {
            wipe(sharedSecretChars);
            wipe(aesKeyBytes);
            wipe(hmacKeyBytes);
        }
    }

    public synchronized String getActiveKeyId() {
        return currentSlot != null ? currentSlot.keyId : Config.ENCRYPTED_KEY_ID;
    }

    public synchronized boolean acceptsKeyId(String keyId) {
        return slotForKeyId(keyId) != null;
    }

    private KeySlot slotForKeyId(String keyId) {
        if (currentSlot != null && currentSlot.keyId.equals(keyId)) {
            return currentSlot;
        }
        if (previousSlot != null && previousSlot.keyId.equals(keyId)) {
            return previousSlot;
        }
        return null;
    }

    private byte[] deriveKeyMaterial(String deviceId, char[] sharedSecretChars, String purpose) throws Exception {
        byte[] saltBytes = null;
        PBEKeySpec spec = null;
        try {
            // PBKDF2-HMAC-SHA256 with 100 000 iterations — matches firmware security.cpp.
            saltBytes = (deviceId + ":" + purpose).getBytes(StandardCharsets.UTF_8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            spec = new PBEKeySpec(sharedSecretChars, saltBytes, 100000, 256);
            SecretKey derived = factory.generateSecret(spec);
            return derived.getEncoded();
        } finally {
            if (spec != null) {
                spec.clearPassword();
            }
            wipe(saltBytes);
        }
    }

    /**
     * Encrypt data with AES-256-GCM.
     */
    public synchronized byte[] encrypt(byte[] plaintext) {
        if (!initialized || plaintext == null) {
            Log.e(TAG, "Security not initialized or null plaintext");
            return null;
        }

        if (!encryptionEnabled) {
            Log.e(TAG, "Encryption is disabled; refusing to encrypt operational traffic");
            return null;
        }

        byte[] iv = null;
        byte[] ciphertext = null;
        try {
            iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            if (currentSlot == null) {
                Log.e(TAG, "No current key slot available for encryption");
                return null;
            }
            cipher.init(Cipher.ENCRYPT_MODE, currentSlot.aesKey, gcmSpec);

            ciphertext = cipher.doFinal(plaintext);

            // Prepend IV to ciphertext
            byte[] result = new byte[IV_SIZE + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_SIZE);
            System.arraycopy(ciphertext, 0, result, IV_SIZE, ciphertext.length);

            messagesEncrypted++;
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        } finally {
            wipe(iv);
            wipe(ciphertext);
        }
    }

    /**
     * Decrypt data with AES-256-GCM.
     */
    public synchronized byte[] decrypt(byte[] ciphertext) {
        return decrypt(ciphertext, getActiveKeyId());
    }

    public synchronized byte[] decrypt(byte[] ciphertext, String keyId) {
        if (!initialized || ciphertext == null || ciphertext.length < IV_SIZE) {
            Log.e(TAG, "Security not initialized or invalid ciphertext");
            return null;
        }

        if (!encryptionEnabled) {
            Log.e(TAG, "Encryption is disabled; refusing to decrypt operational traffic");
            return null;
        }

        KeySlot slot = slotForKeyId(keyId);
        if (slot == null) {
            Log.e(TAG, "No key slot available for decryption");
            authFailures++;
            return null;
        }

        byte[] iv = null;
        byte[] encryptedData = null;
        try {
            // Extract IV
            iv = Arrays.copyOfRange(ciphertext, 0, IV_SIZE);
            encryptedData = Arrays.copyOfRange(ciphertext, IV_SIZE, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, slot.aesKey, gcmSpec);

            byte[] plaintext = cipher.doFinal(encryptedData);
            messagesDecrypted++;
            return plaintext;
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            authFailures++;
            return null;
        } finally {
            wipe(iv);
            wipe(encryptedData);
        }
    }

    /**
     * Generate HMAC for message integrity.
     */
    public synchronized byte[] generateHMAC(byte[] data) {
        return generateHMAC(data, getActiveKeyId());
    }

    public synchronized byte[] generateHMAC(byte[] data, String keyId) {
        if (!initialized || data == null) {
            return null;
        }
        KeySlot slot = slotForKeyId(keyId);
        if (slot == null) {
            return null;
        }

        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(HMAC_ALGORITHM);
            mac.init(slot.hmacKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            Log.e(TAG, "HMAC generation failed", e);
            return null;
        }
    }

    /**
     * Verify HMAC for message integrity.
     */
    public synchronized boolean verifyHMAC(byte[] data, byte[] hmac) {
        return verifyHMAC(data, hmac, getActiveKeyId());
    }

    public synchronized boolean verifyHMAC(byte[] data, byte[] hmac, String keyId) {
        if (!initialized || data == null || hmac == null) {
            integrityFailures++;
            return false;
        }

        byte[] calculatedHMAC = generateHMAC(data, keyId);
        try {
            if (calculatedHMAC == null) {
                integrityFailures++;
                return false;
            }

            boolean valid = MessageDigest.isEqual(calculatedHMAC, hmac);
            if (!valid) {
                integrityFailures++;
            }
            return valid;
        } finally {
            wipe(calculatedHMAC);
        }
    }

    /**
     * Validate input string for security.
     */
    public boolean validateInput(String input, int maxLength) {
        if (input == null) {
            return false;
        }

        if (input.length() > maxLength) {
            return false;
        }

        // Check for injection patterns
        String lowerInput = input.toLowerCase(Locale.ROOT);
        String[] dangerousPatterns = {
            "<script", "javascript:", "onerror=", "onload=",
            "eval(", "exec(", "system(", "<?php", "${", "$(", "`"
        };

        for (String pattern : dangerousPatterns) {
            if (lowerInput.contains(pattern)) {
                Log.w(TAG, "Injection pattern detected: " + pattern);
                return false;
            }
        }

        return true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public synchronized void reset() {
        currentSlot = null;
        previousSlot = null;
        initialized = false;
        encryptionEnabled = true;
    }

    public synchronized void setEncryptionEnabled(boolean enabled) {
        this.encryptionEnabled = enabled;
        Log.i(TAG, "Encryption enabled set to: " + enabled);
    }

    public long getMessagesEncrypted() {
        return messagesEncrypted;
    }

    public long getMessagesDecrypted() {
        return messagesDecrypted;
    }

    public long getIntegrityFailures() {
        return integrityFailures;
    }

    public long getAuthFailures() {
        return authFailures;
    }

    private static void wipe(byte[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, (byte) 0);
        }
    }

    private static boolean isAllZero(byte[] buffer) {
        if (buffer == null || buffer.length == 0) {
            return true;
        }
        for (byte value : buffer) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static void wipe(char[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, '\0');
        }
    }
}

