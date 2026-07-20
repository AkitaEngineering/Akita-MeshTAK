package com.akitaengineering.meshtak;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PayloadEnvelope {
    private static final long MAX_SKEW_SECONDS = 300L;
    private static final int REPLAY_CACHE_LIMIT = 64;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<String> SEEN_NONCES = new LinkedHashSet<>();
    private static final byte[] HEX_DIGITS = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private PayloadEnvelope() {
    }

    public static byte[] encode(SecurityManager securityManager, byte[] plaintext) {
        if (securityManager == null || !securityManager.isInitialized()
                || !securityManager.isEncryptionEnabled() || plaintext == null || plaintext.length == 0) {
            return null;
        }

        byte[] encrypted = securityManager.encrypt(plaintext);
        if (encrypted == null || encrypted.length == 0) {
            return null;
        }

        byte[] nonce = new byte[8];
        SECURE_RANDOM.nextBytes(nonce);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonceHex = encodeHex(nonce);
        String encryptedHex = encodeHex(encrypted);
        String signedData = Config.ENCRYPTED_PAYLOAD_VERSION + ":" + Config.ENCRYPTED_KEY_ID + ":"
                + timestamp + ":" + nonceHex + ":" + encryptedHex;
        byte[] hmac = securityManager.generateHMAC(signedData.getBytes(StandardCharsets.UTF_8));
        if (hmac == null || hmac.length == 0) {
            wipe(encrypted);
            wipe(nonce);
            return null;
        }

        String envelope = Config.ENCRYPTED_PAYLOAD_PREFIX + signedData + ":" + encodeHex(hmac);
        wipe(encrypted);
        wipe(nonce);
        wipe(hmac);
        return envelope.getBytes(StandardCharsets.UTF_8);
    }

    public static String decode(SecurityManager securityManager, String payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        // Operational traffic is always authenticated. Plaintext is reserved for
        // the explicit physical-presence provisioning path and never reaches this
        // decoder.
        if (!payload.startsWith(Config.ENCRYPTED_PAYLOAD_PREFIX)) {
            return null;
        }
        if (securityManager == null || !securityManager.isInitialized() || !securityManager.isEncryptionEnabled()) {
            return null;
        }

        String body = payload.substring(Config.ENCRYPTED_PAYLOAD_PREFIX.length());
        String[] parts = body.split(":", -1);
        if (parts.length != 6) {
            return null;
        }
        if (!Config.ENCRYPTED_PAYLOAD_VERSION.equals(parts[0]) || !Config.ENCRYPTED_KEY_ID.equals(parts[1])) {
            return null;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(parts[2]);
        } catch (NumberFormatException exception) {
            return null;
        }
        long now = System.currentTimeMillis() / 1000L;
        if (timestamp < 0 || timestamp < now - MAX_SKEW_SECONDS || timestamp > now + MAX_SKEW_SECONDS) {
            return null;
        }

        String nonceKey = parts[1] + ":" + parts[3];
        byte[] parsedNonce = parseHex(parts[3]);
        if (parsedNonce == null || parsedNonce.length != 8 || hasSeenNonce(nonceKey)) {
            wipe(parsedNonce);
            return null;
        }
        wipe(parsedNonce);

        String signedData = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":" + parts[4];
        byte[] expectedHmac = parts[5].length() == 64 ? parseHex(parts[5]) : null;
        if (expectedHmac == null || expectedHmac.length != 32) {
            wipe(expectedHmac);
            return null;
        }
        boolean valid = securityManager.verifyHMAC(signedData.getBytes(StandardCharsets.UTF_8), expectedHmac);
        wipe(expectedHmac);
        if (!valid) {
            return null;
        }

        String plaintext = decryptHex(securityManager, parts[4]);
        if (plaintext == null) {
            return null;
        }
        rememberNonce(nonceKey);
        return plaintext;
    }

    private static String decryptHex(SecurityManager securityManager, String hex) {
        byte[] encrypted = parseHex(hex);
        if (encrypted == null) {
            return null;
        }
        byte[] decrypted = securityManager.decrypt(encrypted);
        if (decrypted == null) {
            wipe(encrypted);
            return null;
        }
        try {
            return new String(decrypted, StandardCharsets.UTF_8).trim();
        } finally {
            wipe(encrypted);
            wipe(decrypted);
        }
    }

    private static synchronized boolean hasSeenNonce(String nonceKey) {
        return SEEN_NONCES.contains(nonceKey);
    }

    private static synchronized void rememberNonce(String nonceKey) {
        SEEN_NONCES.add(nonceKey);
        while (SEEN_NONCES.size() > REPLAY_CACHE_LIMIT) {
            String oldest = SEEN_NONCES.iterator().next();
            SEEN_NONCES.remove(oldest);
        }
    }

    private static String encodeHex(byte[] data) {
        byte[] encoded = new byte[data.length * 2];
        for (int index = 0; index < data.length; index++) {
            int value = data[index] & 0xFF;
            encoded[index * 2] = HEX_DIGITS[value >>> 4];
            encoded[index * 2 + 1] = HEX_DIGITS[value & 0x0F];
        }
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    private static byte[] parseHex(String hex) {
        if (hex == null || (hex.length() % 2) != 0) {
            return null;
        }
        byte[] out = new byte[hex.length() / 2];
        for (int index = 0; index < out.length; index++) {
            int sourceIndex = index * 2;
            try {
                out[index] = (byte) Integer.parseInt(hex.substring(sourceIndex, sourceIndex + 2), 16);
            } catch (NumberFormatException exception) {
                wipe(out);
                return null;
            }
        }
        return out;
    }

    private static void wipe(byte[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, (byte) 0);
        }
    }
}
