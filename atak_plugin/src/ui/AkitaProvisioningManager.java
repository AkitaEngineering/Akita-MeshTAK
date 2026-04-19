package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.akitaengineering.meshtak.AkitaMissionControl;
import com.akitaengineering.meshtak.Config;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class AkitaProvisioningManager {

    private static final String TAG = "AkitaProvisioningMgr";

    public static final String PREF_PROVISIONING_SECRET = "security_provisioning_secret";
    public static final String PREF_ENCRYPTION_ENABLED = "security_encryption_enabled";
    public static final String PREF_LAST_ROTATION_AT = "security_last_rotation_at";
    public static final String PREF_PROVISIONING_BUNDLE = "security_provisioning_bundle";
    public static final String PREF_LAST_BUNDLE_GENERATED_AT = "security_last_bundle_generated_at";
    public static final String PREF_PROVISIONING_SECRET_SIGNAL = "security_provisioning_secret_signal";
    public static final String PREF_PROVISIONING_BUNDLE_SIGNAL = "security_provisioning_bundle_signal";

    private static final String SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GENERATED_SECRET_LENGTH = 40;
    private static final String BUNDLE_PREFIX = "AKITA-PROV-1";
    private static final String STATE_FILE_NAME = "akita-provisioning-state.json";
    private static final String PROVISIONING_STATE_KEY_ALIAS = "akita_provisioning_state_key";
    private static final Object STATE_LOCK = new Object();
    private static final Map<SharedPreferences, ProvisioningStateStore> STORES_BY_PREFERENCES = Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile ProvisioningStateStore stateStore;

    private AkitaProvisioningManager() {
    }

    public static SharedPreferences getPreferences(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        STORES_BY_PREFERENCES.put(preferences, getStateStore(appContext));
        return preferences;
    }

    public static String getActiveProvisioningSecret(Context context) {
        ProvisioningState state = getStateStore(context).readState();
        if (hasText(state.customSecret)) {
            return state.customSecret;
        }
        return Config.PROVISIONING_SECRET;
    }

    public static String getActiveProvisioningSecret(SharedPreferences preferences) {
        ProvisioningState state = readState(preferences);
        if (hasText(state.customSecret)) {
            return state.customSecret;
        }
        return Config.PROVISIONING_SECRET;
    }

    public static boolean hasCustomSecret(Context context) {
        return hasText(getStateStore(context).readState().customSecret);
    }

    public static boolean hasCustomSecret(SharedPreferences preferences) {
        return hasText(readState(preferences).customSecret);
    }

    public static boolean isActiveSecretPlaceholder(Context context) {
        String secret = getActiveProvisioningSecret(context);
        return !hasText(secret) || secret.contains("REPLACE_WITH");
    }

    public static boolean isActiveSecretPlaceholder(SharedPreferences preferences) {
        String secret = getActiveProvisioningSecret(preferences);
        return !hasText(secret) || secret.contains("REPLACE_WITH");
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
        ProvisioningState state = getStateStore(context).readState();
        state.customSecret = rotatedSecret;
        state.lastRotationAt = System.currentTimeMillis();
        getStateStore(context).writeState(state);
        clearSensitivePreferenceMirrors(preferences);
        signalSensitiveStateChanged(preferences, true, false);
        return rotatedSecret;
    }

    public static void setCustomProvisioningSecret(Context context, String secret) {
        SharedPreferences preferences = getPreferences(context);
        ProvisioningState state = getStateStore(context).readState();
        state.customSecret = normalizeValue(secret);
        getStateStore(context).writeState(state);
        clearSensitivePreferenceMirrors(preferences);
        signalSensitiveStateChanged(preferences, true, false);
    }

    public static String getProvisioningSummary(Context context) {
        ProvisioningState state = getStateStore(context).readState();
        if (hasText(state.customSecret)) {
            return "Custom secret configured: " + maskSecret(state.customSecret);
        }
        return isActiveSecretPlaceholder(context)
                ? "Using build-time placeholder secret"
                : "Using build-time deployment secret";
    }

    public static String getProvisioningSummary(SharedPreferences preferences) {
        ProvisioningState state = readState(preferences);
        if (hasText(state.customSecret)) {
            return "Custom secret configured: " + maskSecret(state.customSecret);
        }
        return isActiveSecretPlaceholder(preferences)
                ? "Using build-time placeholder secret"
                : "Using build-time deployment secret";
    }

    public static String createProvisioningBundle(Context context, String deviceAlias) {
        SharedPreferences preferences = getPreferences(context);
        ProvisioningState state = getStateStore(context).readState();
        String alias = sanitizeBundleField(deviceAlias);
        String bundle = String.format(
                Locale.US,
                "%s|%s|%s|%s|%d|%s",
                BUNDLE_PREFIX,
                alias,
                Config.ENCRYPTED_PAYLOAD_VERSION,
                Config.ENCRYPTED_KEY_ID,
                isEncryptionEnabled(preferences) ? 1 : 0,
                getActiveProvisioningSecret(context));
        state.stagedBundle = bundle;
        state.lastBundleGeneratedAt = System.currentTimeMillis();
        getStateStore(context).writeState(state);
        clearSensitivePreferenceMirrors(preferences);
        signalSensitiveStateChanged(preferences, false, true);
        AkitaMissionControl.getInstance(context).recordProvisioningEvent(
                "PROVISIONING_BUNDLE_GENERATED",
                "Air-gapped bundle prepared for " + alias,
                AkitaMissionControl.ROUTE_MOCK);
        return bundle;
    }

    public static void setStagedProvisioningBundle(Context context, String bundle) {
        SharedPreferences preferences = getPreferences(context);
        String normalizedBundle = normalizeValue(bundle);
        ProvisioningState state = getStateStore(context).readState();
        if (hasText(normalizedBundle)) {
            parseProvisioningBundle(normalizedBundle);
            state.stagedBundle = normalizedBundle;
            state.lastBundleGeneratedAt = 0L;
        } else {
            state.stagedBundle = "";
            state.lastBundleGeneratedAt = 0L;
        }
        getStateStore(context).writeState(state);
        clearSensitivePreferenceMirrors(preferences);
        signalSensitiveStateChanged(preferences, false, true);
    }

    public static ProvisioningBundle previewProvisioningBundle(String bundle) {
        return parseProvisioningBundle(bundle);
    }

    public static ProvisioningBundle applyProvisioningBundle(Context context) {
        SharedPreferences preferences = getPreferences(context);
        ProvisioningState state = getStateStore(context).readState();
        ProvisioningBundle bundle = parseProvisioningBundle(state.stagedBundle);
        state.customSecret = bundle.secret;
        state.lastRotationAt = System.currentTimeMillis();
        getStateStore(context).writeState(state);
        preferences.edit()
                .putBoolean(PREF_ENCRYPTION_ENABLED, bundle.encryptionEnabled)
                .apply();
        clearSensitivePreferenceMirrors(preferences);
        signalSensitiveStateChanged(preferences, true, false);
        AkitaMissionControl.getInstance(context).recordProvisioningEvent(
                "PROVISIONING_BUNDLE_APPLIED",
                "Bundle applied locally for " + bundle.deviceAlias,
                AkitaMissionControl.ROUTE_MOCK);
        return bundle;
    }

    public static String buildProvisioningStageCommand(Context context) {
        ProvisioningState state = getStateStore(context).readState();
        if (hasText(state.stagedBundle)) {
            return Config.CMD_PROVISION_STAGE_PREFIX + parseProvisioningBundle(state.stagedBundle).secret;
        }
        return Config.CMD_PROVISION_STAGE_PREFIX + getActiveProvisioningSecret(context);
    }

    public static byte[] buildProvisioningStageCommandBytes(Context context) {
        ProvisioningState state = getStateStore(context).readState();
        String secret = hasText(state.stagedBundle)
                ? parseProvisioningBundle(state.stagedBundle).secret
                : getActiveProvisioningSecret(context);

        byte[] prefixBytes = Config.CMD_PROVISION_STAGE_PREFIX.getBytes(StandardCharsets.UTF_8);
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        byte[] commandBytes = new byte[prefixBytes.length + secretBytes.length];
        System.arraycopy(prefixBytes, 0, commandBytes, 0, prefixBytes.length);
        System.arraycopy(secretBytes, 0, commandBytes, prefixBytes.length, secretBytes.length);
        wipe(secretBytes);
        return commandBytes;
    }

    public static String getProvisioningBundleSummary(Context context) {
        ProvisioningState state = getStateStore(context).readState();
        if (!hasText(state.stagedBundle)) {
            return "No air-gapped provisioning bundle staged";
        }

        try {
            ProvisioningBundle bundle = parseProvisioningBundle(state.stagedBundle);
            String ageSummary = state.lastBundleGeneratedAt <= 0L
                    ? "bundle loaded"
                    : "bundle refreshed " + getRelativeAge(state.lastBundleGeneratedAt);
            return String.format(Locale.US,
                    "%s • %s/%s • %s",
                    bundle.deviceAlias,
                    bundle.version,
                    bundle.keyId,
                    ageSummary);
        } catch (IllegalArgumentException exception) {
            return "Staged air-gapped provisioning bundle is malformed";
        }
    }

    public static String getProvisioningBundleSummary(SharedPreferences preferences) {
        ProvisioningState state = readState(preferences);
        if (!hasText(state.stagedBundle)) {
            return "No air-gapped provisioning bundle staged";
        }

        try {
            ProvisioningBundle bundle = parseProvisioningBundle(state.stagedBundle);
            String ageSummary = state.lastBundleGeneratedAt <= 0L
                    ? "bundle loaded"
                    : "bundle refreshed " + getRelativeAge(state.lastBundleGeneratedAt);
            return String.format(Locale.US,
                    "%s • %s/%s • %s",
                    bundle.deviceAlias,
                    bundle.version,
                    bundle.keyId,
                    ageSummary);
        } catch (IllegalArgumentException exception) {
            return "Staged air-gapped provisioning bundle is malformed";
        }
    }

    public static String getRotationSummary(Context context) {
        ProvisioningState state = getStateStore(context).readState();
        if (state.lastRotationAt <= 0L) {
            return hasText(state.customSecret) ? "Custom secret loaded" : "No recorded rotation";
        }
        return describeRotationAge(state.lastRotationAt);
    }

    public static String getRotationSummary(SharedPreferences preferences) {
        ProvisioningState state = readState(preferences);
        if (state.lastRotationAt <= 0L) {
            return hasText(state.customSecret) ? "Custom secret loaded" : "No recorded rotation";
        }
        return describeRotationAge(state.lastRotationAt);
    }

    private static String describeRotationAge(long rotatedAt) {
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

    private static final class ProvisioningState {
        private String customSecret;
        private String stagedBundle;
        private long lastRotationAt;
        private long lastBundleGeneratedAt;

        private ProvisioningState(String customSecret,
                                  String stagedBundle,
                                  long lastRotationAt,
                                  long lastBundleGeneratedAt) {
            this.customSecret = customSecret;
            this.stagedBundle = stagedBundle;
            this.lastRotationAt = lastRotationAt;
            this.lastBundleGeneratedAt = lastBundleGeneratedAt;
        }

        private JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("schemaVersion", 1);
            json.put("customSecret", customSecret);
            json.put("stagedBundle", stagedBundle);
            json.put("lastRotationAt", lastRotationAt);
            json.put("lastBundleGeneratedAt", lastBundleGeneratedAt);
            return json;
        }

        private static ProvisioningState fromJson(JSONObject json) {
            return new ProvisioningState(
                    normalizeValue(json.optString("customSecret", "")),
                    normalizeValue(json.optString("stagedBundle", "")),
                    json.optLong("lastRotationAt", 0L),
                    json.optLong("lastBundleGeneratedAt", 0L));
        }

        private static ProvisioningState fromLegacyPreferences(SharedPreferences preferences) {
            return new ProvisioningState(
                    normalizeValue(preferences.getString(PREF_PROVISIONING_SECRET, "")),
                    normalizeValue(preferences.getString(PREF_PROVISIONING_BUNDLE, "")),
                    preferences.getLong(PREF_LAST_ROTATION_AT, 0L),
                    preferences.getLong(PREF_LAST_BUNDLE_GENERATED_AT, 0L));
        }
    }

    private static final class ProvisioningStateStore {
        private final String stateFilePath;
        private final AtomicFile stateFile;
        private final SharedPreferences preferences;
        private final ProvisioningStateCipher stateCipher;

        private ProvisioningStateStore(Context context) {
            Context appContext = context.getApplicationContext();
            preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
            File stateDirectory = appContext.getNoBackupFilesDir();
            if (!stateDirectory.exists()) {
                stateDirectory.mkdirs();
            }
            File baseFile = new File(stateDirectory, STATE_FILE_NAME);
            stateFilePath = baseFile.getAbsolutePath();
            stateFile = new AtomicFile(baseFile);
            stateCipher = new ProvisioningStateCipher();
        }

        private boolean matchesContext(Context context) {
            File expectedFile = new File(context.getApplicationContext().getNoBackupFilesDir(), STATE_FILE_NAME);
            return stateFilePath.equals(expectedFile.getAbsolutePath());
        }

        private synchronized ProvisioningState readState() {
            if (!stateFile.getBaseFile().exists()) {
                return migrateLegacyState();
            }

            byte[] payloadBytes = null;
            try (FileInputStream inputStream = stateFile.openRead()) {
                payloadBytes = readAllBytes(inputStream);
                if (payloadBytes.length == 0) {
                    return new ProvisioningState("", "", 0L, 0L);
                }
                ProvisioningState state = parseStoredState(payloadBytes);
                clearSensitivePreferenceMirrors(preferences);
                return state;
            } catch (IOException | JSONException | GeneralSecurityException exception) {
                Log.w(TAG, "Failed to read encrypted provisioning state; falling back to legacy preferences", exception);
                return migrateLegacyState();
            } finally {
                wipe(payloadBytes);
            }
        }

        private synchronized void writeState(ProvisioningState state) {
            FileOutputStream outputStream = null;
            byte[] serializedState = null;
            byte[] encryptedPayload = null;
            try {
                outputStream = stateFile.startWrite();
                serializedState = state.toJson().toString().getBytes(StandardCharsets.UTF_8);
                encryptedPayload = stateCipher.encrypt(serializedState);
                outputStream.write(encryptedPayload);
                outputStream.flush();
                stateFile.finishWrite(outputStream);
            } catch (IOException | JSONException | GeneralSecurityException exception) {
                Log.w(TAG, "Failed to persist encrypted provisioning state", exception);
                if (outputStream != null) {
                    stateFile.failWrite(outputStream);
                }
            } finally {
                wipe(serializedState);
                wipe(encryptedPayload);
            }
        }

        private ProvisioningState migrateLegacyState() {
            ProvisioningState state = ProvisioningState.fromLegacyPreferences(preferences);
            if (hasText(state.customSecret) || hasText(state.stagedBundle)
                    || state.lastRotationAt > 0L || state.lastBundleGeneratedAt > 0L) {
                writeState(state);
            }
            clearSensitivePreferenceMirrors(preferences);
            return state;
        }

        private ProvisioningState parseStoredState(byte[] payloadBytes) throws JSONException, GeneralSecurityException {
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(payload);
            if (root.has("ciphertext") && root.has("iv")) {
                byte[] decryptedPayload = stateCipher.decrypt(root);
                try {
                    return ProvisioningState.fromJson(new JSONObject(new String(decryptedPayload, StandardCharsets.UTF_8)));
                } finally {
                    wipe(decryptedPayload);
                }
            }

            ProvisioningState legacyStateFile = ProvisioningState.fromJson(root);
            writeState(legacyStateFile);
            return legacyStateFile;
        }

        private byte[] readAllBytes(FileInputStream inputStream) throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    private static final class ProvisioningStateCipher {
        private static final String TRANSFORMATION = "AES/GCM/NoPadding";
        private static final int GCM_TAG_LENGTH_BITS = 128;

        private static volatile SecretKey testFallbackKey;

        private byte[] encrypt(byte[] plaintextBytes) throws GeneralSecurityException, JSONException {
            byte[] ciphertext = null;
            byte[] iv = null;
            try {
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
                ciphertext = cipher.doFinal(plaintextBytes);
                iv = cipher.getIV();

                JSONObject envelope = new JSONObject();
                envelope.put("schemaVersion", 1);
                envelope.put("storageFormat", "AES_GCM");
                envelope.put("keySource", isRobolectricRuntime() ? "test-fallback" : "android-keystore");
                envelope.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
                envelope.put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP));
                return envelope.toString().getBytes(StandardCharsets.UTF_8);
            } finally {
                wipe(ciphertext);
                wipe(iv);
            }
        }

        private byte[] decrypt(JSONObject envelope) throws GeneralSecurityException, JSONException {
            byte[] iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP);
            byte[] ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP);

            try {
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
                return cipher.doFinal(ciphertext);
            } finally {
                wipe(iv);
                wipe(ciphertext);
            }
        }

        private SecretKey getOrCreateSecretKey() throws GeneralSecurityException {
            if (isRobolectricRuntime()) {
                return getOrCreateTestFallbackKey();
            }

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            try {
                keyStore.load(null);
            } catch (IOException exception) {
                throw new GeneralSecurityException("Unable to load Android KeyStore", exception);
            }

            KeyStore.Entry existingEntry = keyStore.getEntry(PROVISIONING_STATE_KEY_ALIAS, null);
            if (existingEntry instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) existingEntry).getSecretKey();
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    PROVISIONING_STATE_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(false)
                    .build());
            return keyGenerator.generateKey();
        }

        private SecretKey getOrCreateTestFallbackKey() throws GeneralSecurityException {
            SecretKey existingKey = testFallbackKey;
            if (existingKey != null) {
                return existingKey;
            }

            synchronized (ProvisioningStateCipher.class) {
                if (testFallbackKey == null) {
                    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                    keyGenerator.init(256);
                    testFallbackKey = keyGenerator.generateKey();
                }
                return testFallbackKey;
            }
        }

        private boolean isRobolectricRuntime() {
            return Build.FINGERPRINT != null && Build.FINGERPRINT.toLowerCase(Locale.US).contains("robolectric");
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

    private static ProvisioningStateStore getStateStore(Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (STATE_LOCK) {
            if (stateStore == null || !stateStore.matchesContext(appContext)) {
                stateStore = new ProvisioningStateStore(appContext);
            }
            STORES_BY_PREFERENCES.put(PreferenceManager.getDefaultSharedPreferences(appContext), stateStore);
            return stateStore;
        }
    }

    private static ProvisioningState readState(SharedPreferences preferences) {
        ProvisioningStateStore store = STORES_BY_PREFERENCES.get(preferences);
        if (store != null) {
            return store.readState();
        }
        ProvisioningStateStore currentStore = stateStore;
        if (currentStore != null) {
            return currentStore.readState();
        }
        return ProvisioningState.fromLegacyPreferences(preferences);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static void wipe(byte[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, (byte) 0);
        }
    }

    private static void clearSensitivePreferenceMirrors(SharedPreferences preferences) {
        preferences.edit()
                .remove(PREF_PROVISIONING_SECRET)
                .remove(PREF_LAST_ROTATION_AT)
                .remove(PREF_PROVISIONING_BUNDLE)
                .remove(PREF_LAST_BUNDLE_GENERATED_AT)
                .apply();
    }

    private static void signalSensitiveStateChanged(SharedPreferences preferences, boolean secretChanged, boolean bundleChanged) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor editor = preferences.edit();
        if (secretChanged) {
            editor.putLong(PREF_PROVISIONING_SECRET_SIGNAL, now);
        }
        if (bundleChanged) {
            editor.putLong(PREF_PROVISIONING_BUNDLE_SIGNAL, now);
        }
        editor.apply();
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