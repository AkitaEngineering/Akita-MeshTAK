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
    
    private SecretKey aesKey;
    private SecretKey hmacKey;
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
    public boolean initialize(byte[] aesKeyBytes, byte[] hmacKeyBytes) {
        try {
            if (aesKeyBytes == null || aesKeyBytes.length != AES_KEY_SIZE / 8) {
                Log.e(TAG, "Invalid AES key length");
                return false;
            }
            
            if (hmacKeyBytes == null || hmacKeyBytes.length != HMAC_SIZE) {
                Log.e(TAG, "Invalid HMAC key length");
                return false;
            }
            
            aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            hmacKey = new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM);
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
    public boolean generateKeys() {
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
    public boolean initializeFromProvisioning(String deviceId, String sharedSecret) {
        if (deviceId == null || deviceId.isEmpty() || sharedSecret == null || sharedSecret.isEmpty()) {
            Log.e(TAG, "Provisioning material is missing");
            return false;
        }

        if (Config.isPlaceholderSecret() && Config.PROVISIONING_SECRET.equals(sharedSecret)) {
            Log.w(TAG, "SECURITY WARNING: Provisioning secret is still set to the compile-time placeholder. "
                    + "Replace Config.PROVISIONING_SECRET before deploying to production.");
        }

        char[] sharedSecretChars = sharedSecret.toCharArray();
        byte[] aesKeyBytes = null;
        byte[] hmacKeyBytes = null;
        try {
            aesKeyBytes = deriveKeyMaterial(deviceId, sharedSecretChars, "aes");
            hmacKeyBytes = deriveKeyMaterial(deviceId, sharedSecretChars, "hmac");
            return initialize(aesKeyBytes, hmacKeyBytes);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize keys from provisioning", e);
            return false;
        } finally {
            wipe(sharedSecretChars);
            wipe(aesKeyBytes);
            wipe(hmacKeyBytes);
        }
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
    public byte[] encrypt(byte[] plaintext) {
        if (!initialized || plaintext == null) {
            Log.e(TAG, "Security not initialized or null plaintext");
            return null;
        }

        // Compatibility: if encryption is not enabled, return plaintext copy
        if (!encryptionEnabled) {
            Log.w(TAG, "Encryption disabled – returning plaintext copy");
            byte[] copy = Arrays.copyOf(plaintext, plaintext.length);
            return copy;
        }
        
        byte[] iv = null;
        byte[] ciphertext = null;
        try {
            iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

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
    public byte[] decrypt(byte[] ciphertext) {
        if (!initialized || ciphertext == null || ciphertext.length < IV_SIZE) {
            Log.e(TAG, "Security not initialized or invalid ciphertext");
            return null;
        }

        // Compatibility: if encryption is not enabled, return ciphertext copy
        if (!encryptionEnabled) {
            Log.w(TAG, "Encryption disabled – returning data without decryption");
            byte[] copy = Arrays.copyOf(ciphertext, ciphertext.length);
            return copy;
        }
        
        byte[] iv = null;
        byte[] encryptedData = null;
        try {
            // Extract IV
            iv = Arrays.copyOfRange(ciphertext, 0, IV_SIZE);
            encryptedData = Arrays.copyOfRange(ciphertext, IV_SIZE, ciphertext.length);
            
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
            
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
    public byte[] generateHMAC(byte[] data) {
        if (!initialized || data == null) {
            return null;
        }
        
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            Log.e(TAG, "HMAC generation failed", e);
            return null;
        }
    }
    
    /**
     * Verify HMAC for message integrity.
     */
    public boolean verifyHMAC(byte[] data, byte[] hmac) {
        if (!initialized || data == null || hmac == null) {
            integrityFailures++;
            return false;
        }
        
        byte[] calculatedHMAC = generateHMAC(data);
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
        String lowerInput = input.toLowerCase();
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
        aesKey = null;
        hmacKey = null;
        initialized = false;
        encryptionEnabled = true;
    }

    public void setEncryptionEnabled(boolean enabled) {
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

    private static void wipe(char[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, '\0');
        }
    }
}

