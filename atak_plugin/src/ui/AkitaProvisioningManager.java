package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.akitaengineering.meshtak.AkitaMissionControl;
import com.akitaengineering.meshtak.Config;

import java.security.SecureRandom;
import java.util.Locale;

public final class AkitaProvisioningManager {

    public static final String PREF_PROVISIONING_SECRET = "security_provisioning_secret";
    public static final String PREF_ENCRYPTION_ENABLED = "security_encryption_enabled";
    public static final String PREF_LAST_ROTATION_AT = "security_last_rotation_at";
    public static final String PREF_PROVISIONING_BUNDLE = "security_provisioning_bundle";
    public static final String PREF_LAST_BUNDLE_GENERATED_AT = "security_last_bundle_generated_at";

    private static final String SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GENERATED_SECRET_LENGTH = 40;
    private static final String BUNDLE_PREFIX = "AKITA-PROV-1";

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

    public static String createProvisioningBundle(Context context, String deviceAlias) {
        SharedPreferences preferences = getPreferences(context);
        String alias = sanitizeBundleField(deviceAlias);
        String bundle = String.format(
                Locale.US,
                "%s|%s|%s|%s|%d|%s",
                BUNDLE_PREFIX,
                alias,
                Config.ENCRYPTED_PAYLOAD_VERSION,
                Config.ENCRYPTED_KEY_ID,
                isEncryptionEnabled(preferences) ? 1 : 0,
                getActiveProvisioningSecret(preferences));
        preferences.edit()
                .putString(PREF_PROVISIONING_BUNDLE, bundle)
                .putLong(PREF_LAST_BUNDLE_GENERATED_AT, System.currentTimeMillis())
                .apply();
        AkitaMissionControl.getInstance(context).recordProvisioningEvent(
                "PROVISIONING_BUNDLE_GENERATED",
                "Air-gapped bundle prepared for " + alias,
                AkitaMissionControl.ROUTE_MOCK);
        return bundle;
    }

    public static ProvisioningBundle previewProvisioningBundle(String bundle) {
        return parseProvisioningBundle(bundle);
    }

    public static ProvisioningBundle applyProvisioningBundle(Context context) {
        SharedPreferences preferences = getPreferences(context);
        ProvisioningBundle bundle = parseProvisioningBundle(preferences.getString(PREF_PROVISIONING_BUNDLE, ""));
        preferences.edit()
                .putString(PREF_PROVISIONING_SECRET, bundle.secret)
                .putBoolean(PREF_ENCRYPTION_ENABLED, bundle.encryptionEnabled)
                .putLong(PREF_LAST_ROTATION_AT, System.currentTimeMillis())
                .apply();
        AkitaMissionControl.getInstance(context).recordProvisioningEvent(
                "PROVISIONING_BUNDLE_APPLIED",
                "Bundle applied locally for " + bundle.deviceAlias,
                AkitaMissionControl.ROUTE_MOCK);
        return bundle;
    }

    public static String buildProvisioningStageCommand(Context context) {
        SharedPreferences preferences = getPreferences(context);
        String stagedBundle = preferences.getString(PREF_PROVISIONING_BUNDLE, "");
        if (stagedBundle != null && !stagedBundle.trim().isEmpty()) {
            return Config.CMD_PROVISION_STAGE_PREFIX + parseProvisioningBundle(stagedBundle).secret;
        }
        return Config.CMD_PROVISION_STAGE_PREFIX + getActiveProvisioningSecret(preferences);
    }

    public static String getProvisioningBundleSummary(Context context) {
        return getProvisioningBundleSummary(getPreferences(context));
    }

    public static String getProvisioningBundleSummary(SharedPreferences preferences) {
        String bundleValue = preferences.getString(PREF_PROVISIONING_BUNDLE, "");
        if (bundleValue == null || bundleValue.trim().isEmpty()) {
            return "No air-gapped provisioning bundle staged";
        }

        ProvisioningBundle bundle = parseProvisioningBundle(bundleValue);
        long generatedAt = preferences.getLong(PREF_LAST_BUNDLE_GENERATED_AT, 0L);
        String ageSummary = generatedAt <= 0L
                ? "bundle loaded"
                : "bundle refreshed " + getRelativeAge(generatedAt);
        return String.format(Locale.US,
                "%s • %s/%s • %s",
                bundle.deviceAlias,
                bundle.version,
                bundle.keyId,
                ageSummary);
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

    public static final class ProvisioningBundle {
        public final String deviceAlias;
        public final String version;
        public final String keyId;
        public final boolean encryptionEnabled;
        public final String secret;

        private ProvisioningBundle(String deviceAlias,
                                   String version,
                                   String keyId,
                                   boolean encryptionEnabled,
                                   String secret) {
            this.deviceAlias = deviceAlias;
            this.version = version;
            this.keyId = keyId;
            this.encryptionEnabled = encryptionEnabled;
            this.secret = secret;
        }
    }

    private static ProvisioningBundle parseProvisioningBundle(String bundle) {
        if (bundle == null || bundle.trim().isEmpty()) {
            throw new IllegalArgumentException("Provisioning bundle is empty.");
        }

        String[] parts = bundle.trim().split("\\|", 6);
        if (parts.length != 6 || !BUNDLE_PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Provisioning bundle is malformed.");
        }

        String alias = sanitizeBundleField(parts[1]);
        String version = parts[2].trim();
        String keyId = parts[3].trim();
        boolean encryptionEnabled = "1".equals(parts[4].trim());
        String secret = parts[5].trim();
        if (secret.length() < 12) {
            throw new IllegalArgumentException("Provisioning bundle secret is too short.");
        }
        return new ProvisioningBundle(alias, version, keyId, encryptionEnabled, secret);
    }

    private static String sanitizeBundleField(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "AkitaNode01";
        }
        return value.trim().replace('|', '_');
    }

    private static String getRelativeAge(long timestamp) {
        long ageMinutes = Math.max(0L, (System.currentTimeMillis() - timestamp) / 60000L);
        if (ageMinutes < 1L) {
            return "just now";
        }
        if (ageMinutes < 60L) {
            return ageMinutes + "m ago";
        }
        long ageHours = ageMinutes / 60L;
        if (ageHours < 24L) {
            return ageHours + "h ago";
        }
        return (ageHours / 24L) + "d ago";
    }
}