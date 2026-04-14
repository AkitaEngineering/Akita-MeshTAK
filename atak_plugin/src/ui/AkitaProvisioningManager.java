package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.akitaengineering.meshtak.Config;

import java.security.SecureRandom;
import java.util.Locale;

public final class AkitaProvisioningManager {

    public static final String PREF_PROVISIONING_SECRET = "security_provisioning_secret";
    public static final String PREF_ENCRYPTION_ENABLED = "security_encryption_enabled";
    public static final String PREF_LAST_ROTATION_AT = "security_last_rotation_at";

    private static final String SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GENERATED_SECRET_LENGTH = 40;

    private AkitaProvisioningManager() {
    }

    public static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getActiveProvisioningSecret(Context context) {
        return getActiveProvisioningSecret(getPreferences(context));
    }

    public static String getActiveProvisioningSecret(SharedPreferences preferences) {
        String configuredSecret = preferences.getString(PREF_PROVISIONING_SECRET, "");
        if (configuredSecret != null && !configuredSecret.trim().isEmpty()) {
            return configuredSecret.trim();
        }
        return Config.PROVISIONING_SECRET;
    }

    public static boolean hasCustomSecret(Context context) {
        return hasCustomSecret(getPreferences(context));
    }

    public static boolean hasCustomSecret(SharedPreferences preferences) {
        String configuredSecret = preferences.getString(PREF_PROVISIONING_SECRET, "");
        return configuredSecret != null && !configuredSecret.trim().isEmpty();
    }

    public static boolean isActiveSecretPlaceholder(Context context) {
        return isActiveSecretPlaceholder(getPreferences(context));
    }

    public static boolean isActiveSecretPlaceholder(SharedPreferences preferences) {
        String secret = getActiveProvisioningSecret(preferences);
        return secret == null || secret.isEmpty() || secret.contains("REPLACE_WITH");
    }

    public static boolean isEncryptionEnabled(Context context) {
        return isEncryptionEnabled(getPreferences(context));
    }

    public static boolean isEncryptionEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(PREF_ENCRYPTION_ENABLED, true);
    }

    public static String rotateProvisioningSecret(Context context) {
        SharedPreferences preferences = getPreferences(context);
        String rotatedSecret = generateSecret();
        preferences.edit()
                .putString(PREF_PROVISIONING_SECRET, rotatedSecret)
                .putLong(PREF_LAST_ROTATION_AT, System.currentTimeMillis())
                .apply();
        return rotatedSecret;
    }

    public static String getProvisioningSummary(Context context) {
        return getProvisioningSummary(getPreferences(context));
    }

    public static String getProvisioningSummary(SharedPreferences preferences) {
        if (hasCustomSecret(preferences)) {
            return "Custom secret configured: " + maskSecret(preferences.getString(PREF_PROVISIONING_SECRET, ""));
        }
        return isActiveSecretPlaceholder(preferences)
                ? "Using build-time placeholder secret"
                : "Using build-time deployment secret";
    }

    public static String getRotationSummary(Context context) {
        return getRotationSummary(getPreferences(context));
    }

    public static String getRotationSummary(SharedPreferences preferences) {
        long rotatedAt = preferences.getLong(PREF_LAST_ROTATION_AT, 0L);
        if (rotatedAt <= 0L) {
            return hasCustomSecret(preferences) ? "Custom secret loaded" : "No recorded rotation";
        }
        long ageMinutes = Math.max(0L, (System.currentTimeMillis() - rotatedAt) / 60000L);
        if (ageMinutes < 1L) {
            return "Rotated just now";
        }
        if (ageMinutes < 60L) {
            return String.format(Locale.US, "Rotated %dm ago", ageMinutes);
        }
        long ageHours = ageMinutes / 60L;
        if (ageHours < 24L) {
            return String.format(Locale.US, "Rotated %dh ago", ageHours);
        }
        return String.format(Locale.US, "Rotated %dd ago", ageHours / 24L);
    }

    public static String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "Not configured";
        }
        if (secret.length() <= 8) {
            return "********";
        }
        return secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
    }

    private static String generateSecret() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder builder = new StringBuilder(GENERATED_SECRET_LENGTH);
        for (int index = 0; index < GENERATED_SECRET_LENGTH; index++) {
            builder.append(SECRET_ALPHABET.charAt(secureRandom.nextInt(SECRET_ALPHABET.length())));
        }
        return builder.toString();
    }
}