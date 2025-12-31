// File: atak_plugin/src/com/akitaengineering/meshtak/SecurityManager.java
// Description: Security manager for encryption, authentication, and input validation.
// CRITICAL: For military/law enforcement use

package com.akitaengineering.meshtak;

import android.util.Log;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Security Manager for encryption, authentication, and message integrity.
 * Provides AES-256 encryption and HMAC-SHA256 for integrity checking.
 */
public class SecurityManager {
    private static final String TAG = "SecurityManager";
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int AES_KEY_SIZE = 256;
    private static final int IV_SIZE = 16;
    private static final int HMAC_SIZE = 32;
    
    private SecretKey aesKey;
    private SecretKey hmacKey;
    private boolean initialized = false;
    
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
        try {
            // Generate AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            SecretKey newAesKey = keyGen.generateKey();
            
            // Generate HMAC key
            KeyGenerator hmacKeyGen = KeyGenerator.getInstance("HmacSHA256");
            hmacKeyGen.init(256);
            SecretKey newHmacKey = hmacKeyGen.generateKey();
            
            return initialize(newAesKey.getEncoded(), newHmacKey.getEncoded());
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate keys", e);
            return false;
        }
    }
    
    /**
     * Encrypt data with AES-256-CBC.
     */
    public byte[] encrypt(byte[] plaintext) {
        if (!initialized || plaintext == null) {
            Log.e(TAG, "Security not initialized or null plaintext");
            return null;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // Prepend IV to ciphertext
            byte[] result = new byte[IV_SIZE + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_SIZE);
            System.arraycopy(ciphertext, 0, result, IV_SIZE, ciphertext.length);
            
            messagesEncrypted++;
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }
    
    /**
     * Decrypt data with AES-256-CBC.
     */
    public byte[] decrypt(byte[] ciphertext) {
        if (!initialized || ciphertext == null || ciphertext.length < IV_SIZE) {
            Log.e(TAG, "Security not initialized or invalid ciphertext");
            return null;
        }
        
        try {
            // Extract IV
            byte[] iv = Arrays.copyOfRange(ciphertext, 0, IV_SIZE);
            byte[] encryptedData = Arrays.copyOfRange(ciphertext, IV_SIZE, ciphertext.length);
            
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
            
            byte[] plaintext = cipher.doFinal(encryptedData);
            messagesDecrypted++;
            return plaintext;
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            authFailures++;
            return null;
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
        if (calculatedHMAC == null) {
            integrityFailures++;
            return false;
        }
        
        boolean valid = MessageDigest.isEqual(calculatedHMAC, hmac);
        if (!valid) {
            integrityFailures++;
        }
        return valid;
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
}

