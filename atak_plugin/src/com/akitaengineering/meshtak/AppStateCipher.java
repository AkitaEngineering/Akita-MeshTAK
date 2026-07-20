package com.akitaengineering.meshtak;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Android-Keystore-backed authenticated encryption for sensitive state files. */
final class AppStateCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_BITS = 128;
    private static final Map<String, SecretKey> TEST_KEYS = new HashMap<>();

    private final String keyAlias;

    AppStateCipher(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    byte[] encrypt(byte[] plaintext) throws GeneralSecurityException, JSONException {
        byte[] ciphertext = null;
        byte[] iv = null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            ciphertext = cipher.doFinal(plaintext);
            iv = cipher.getIV();
            JSONObject envelope = new JSONObject();
            envelope.put("schemaVersion", 1);
            envelope.put("storageFormat", "AES_GCM");
            envelope.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
            envelope.put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP));
            return envelope.toString().getBytes(StandardCharsets.UTF_8);
        } finally {
            wipe(ciphertext);
            wipe(iv);
        }
    }

    byte[] decrypt(byte[] encryptedEnvelope) throws GeneralSecurityException, JSONException {
        JSONObject envelope = new JSONObject(new String(encryptedEnvelope, StandardCharsets.UTF_8));
        if (!"AES_GCM".equals(envelope.optString("storageFormat"))) {
            throw new GeneralSecurityException("State file is not an authenticated envelope.");
        }
        byte[] iv;
        byte[] ciphertext;
        try {
            iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP);
            ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP);
        } catch (IllegalArgumentException exception) {
            throw new GeneralSecurityException("State envelope contains invalid base64.", exception);
        }
        if (iv.length != 12 || ciphertext.length < 16) {
            wipe(iv);
            wipe(ciphertext);
            throw new GeneralSecurityException("State envelope contains invalid AES-GCM fields.");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } finally {
            wipe(iv);
            wipe(ciphertext);
        }
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException {
        if (Build.FINGERPRINT != null && Build.FINGERPRINT.toLowerCase(Locale.US).contains("robolectric")) {
            synchronized (TEST_KEYS) {
                SecretKey key = TEST_KEYS.get(keyAlias);
                if (key == null) {
                    KeyGenerator generator = KeyGenerator.getInstance("AES");
                    generator.init(256);
                    key = generator.generateKey();
                    TEST_KEYS.put(keyAlias, key);
                }
                return key;
            }
        }
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        try {
            keyStore.load(null);
        } catch (IOException exception) {
            throw new GeneralSecurityException("Unable to load Android Keystore", exception);
        }
        KeyStore.Entry entry = keyStore.getEntry(keyAlias, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build());
        return generator.generateKey();
    }

    private static void wipe(byte[] value) {
        if (value != null) Arrays.fill(value, (byte) 0);
    }
}
