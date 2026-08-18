package com.akitaengineering.meshtak;

import java.util.Locale;

/**
 * Last-seen controller security posture reported by firmware.
 */
public final class DeviceSecurityState {
    private static final Object LOCK = new Object();

    private static String activeKeyId = Config.ENCRYPTED_KEY_ID;
    private static String previousKeyId = "";
    private static boolean flashEncryptionEnabled;
    private static boolean secureBootEnabled;
    private static boolean reported;

    private DeviceSecurityState() {
    }

    public static void resetForTests() {
        synchronized (LOCK) {
            activeKeyId = Config.ENCRYPTED_KEY_ID;
            previousKeyId = "";
            flashEncryptionEnabled = false;
            secureBootEnabled = false;
            reported = false;
        }
    }

    public static boolean updateFromStatusLine(String line) {
        if (line == null || !line.startsWith(Config.STATUS_SEC_STATE_PREFIX)) {
            return false;
        }
        String body = line.substring(Config.STATUS_SEC_STATE_PREFIX.length()).trim();
        String parsedKey = "";
        String parsedPrevious = "";
        boolean parsedFlash = false;
        boolean parsedBoot = false;
        String[] parts = body.split(":");
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator <= 0 || separator == part.length() - 1) {
                continue;
            }
            String name = part.substring(0, separator);
            String value = part.substring(separator + 1);
            if ("key".equals(name) && Config.isKnownKeyId(value)) {
                parsedKey = value;
            } else if ("prev".equals(name)) {
                parsedPrevious = Config.isKnownKeyId(value) ? value : "";
            } else if ("flash".equals(name)) {
                parsedFlash = "1".equals(value);
            } else if ("boot".equals(name)) {
                parsedBoot = "1".equals(value);
            }
        }
        if (parsedKey.isEmpty()) {
            return false;
        }
        synchronized (LOCK) {
            activeKeyId = parsedKey;
            previousKeyId = parsedPrevious;
            flashEncryptionEnabled = parsedFlash;
            secureBootEnabled = parsedBoot;
            reported = true;
        }
        return true;
    }

    public static String getActiveKeyId() {
        synchronized (LOCK) {
            return activeKeyId;
        }
    }

    public static String getPreviousKeyId() {
        synchronized (LOCK) {
            return previousKeyId;
        }
    }

    public static boolean hasFlashEncryption() {
        synchronized (LOCK) {
            return flashEncryptionEnabled;
        }
    }

    public static boolean hasSecureBoot() {
        synchronized (LOCK) {
            return secureBootEnabled;
        }
    }

    public static boolean hasReport() {
        synchronized (LOCK) {
            return reported;
        }
    }

    public static String getHardwareSummary() {
        synchronized (LOCK) {
            if (!reported) {
                return "Controller hardware security unknown";
            }
            if (flashEncryptionEnabled && secureBootEnabled) {
                return "Controller flash encryption + secure boot";
            }
            if (flashEncryptionEnabled) {
                return "Controller flash encryption on • secure boot off";
            }
            if (secureBootEnabled) {
                return "Controller secure boot on • flash encryption off";
            }
            return "Controller flash encryption and secure boot off";
        }
    }

    public static String getKeySummary() {
        synchronized (LOCK) {
            if (!previousKeyId.isEmpty()) {
                return String.format(Locale.US, "%s active • %s overlap", activeKeyId, previousKeyId);
            }
            return activeKeyId + " active";
        }
    }
}
