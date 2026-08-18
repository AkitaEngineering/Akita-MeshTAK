package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AkitaProvisioningManagerTest {

    private Context context;
    private SharedPreferences preferences;
    private File stateFile;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
        stateFile = new File(context.getNoBackupFilesDir(), "akita-provisioning-state.json");
        if (stateFile.exists()) {
            stateFile.delete();
        }
    }

    @Test
    public void encryptedTransportDefaultsToEnabled() {
        assertTrue(AkitaProvisioningManager.isEncryptionEnabled(preferences));
    }

    @Test
    public void incompatibleOrUnsafeBundlesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                AkitaProvisioningManager.previewProvisioningBundle(
                        "AKITA-PROV-1|NodeAlpha|v1|k1|1|BundleSecret123456"));
        assertThrows(IllegalArgumentException.class, () ->
                AkitaProvisioningManager.previewProvisioningBundle(
                        "AKITA-PROV-1|Node Alpha|v2|k1|1|BundleSecret123456"));
    }

    @Test
    public void customProvisioningSecretOverridesFallback() {
        AkitaProvisioningManager.setCustomProvisioningSecret(context, "CustomSecret123456");

        assertEquals("CustomSecret123456", AkitaProvisioningManager.getActiveProvisioningSecret(context));
        assertEquals(com.akitaengineering.meshtak.Config.ENCRYPTED_KEY_ID, AkitaProvisioningManager.getActiveKeyId(context));
        assertFalse(preferences.contains(AkitaProvisioningManager.PREF_PROVISIONING_SECRET));
        assertTrue(stateFile.exists());
        assertStoredFileIsEncrypted("CustomSecret123456");
    }

    @Test
    public void rotatingSecretFlipsKeyIdAndKeepsPreviousMaterial() {
        AkitaProvisioningManager.setCustomProvisioningSecret(context, "CustomSecret123456");
        String rotated = AkitaProvisioningManager.rotateProvisioningSecret(context);

        assertEquals(rotated, AkitaProvisioningManager.getActiveProvisioningSecret(context));
        assertEquals("CustomSecret123456", AkitaProvisioningManager.getPreviousProvisioningSecret(context));
        assertEquals(com.akitaengineering.meshtak.Config.ENCRYPTED_KEY_ID_K2, AkitaProvisioningManager.getActiveKeyId(context));
        assertTrue(AkitaProvisioningManager.getRotationSummary(context).contains("k2"));
    }

    @Test
    public void stagedBundleLivesInSecureStore() {
        String bundle = String.format(
                Locale.US,
                "AKITA-PROV-1|NodeAlpha|%s|%s|1|BundleSecret123456",
            com.akitaengineering.meshtak.Config.ENCRYPTED_PAYLOAD_VERSION,
            com.akitaengineering.meshtak.Config.ENCRYPTED_KEY_ID);

        AkitaProvisioningManager.setStagedProvisioningBundle(context, bundle);

        assertThrows(IllegalArgumentException.class,
                () -> AkitaProvisioningManager.buildProvisioningStageCommandBytes(context));
        AkitaProvisioningManager.applyProvisioningBundle(context);
        assertProvisioningCommand(AkitaProvisioningManager.buildProvisioningStageCommand(context), "BundleSecret123456");
        assertProvisioningCommand(new String(
                AkitaProvisioningManager.buildProvisioningStageCommandBytes(context), StandardCharsets.UTF_8),
                "BundleSecret123456");
        assertEquals("NodeAlpha", preferences.getString("ble_device_name", ""));
        assertFalse(preferences.contains(AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE));
        assertTrue(preferences.contains(AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE_SIGNAL));
        assertTrue(stateFile.exists());
        assertStoredFileIsEncrypted("BundleSecret123456");
    }

    @Test
    public void legacyPreferencePayloadsMigrateIntoSecureStore() {
        String bundle = String.format(
                Locale.US,
                "AKITA-PROV-1|NodeBravo|%s|%s|1|LegacySecret123456",
            com.akitaengineering.meshtak.Config.ENCRYPTED_PAYLOAD_VERSION,
            com.akitaengineering.meshtak.Config.ENCRYPTED_KEY_ID);
        preferences.edit()
                .putString(AkitaProvisioningManager.PREF_PROVISIONING_SECRET, "LegacySecret123456")
                .putString(AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE, bundle)
                .putLong(AkitaProvisioningManager.PREF_LAST_ROTATION_AT, 111L)
                .putLong(AkitaProvisioningManager.PREF_LAST_BUNDLE_GENERATED_AT, 222L)
                .commit();

        assertEquals("LegacySecret123456", AkitaProvisioningManager.getActiveProvisioningSecret(context));
        assertFalse(preferences.contains(AkitaProvisioningManager.PREF_PROVISIONING_SECRET));
        assertFalse(preferences.contains(AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE));
        assertFalse(preferences.contains(AkitaProvisioningManager.PREF_LAST_ROTATION_AT));
        assertFalse(preferences.contains(AkitaProvisioningManager.PREF_LAST_BUNDLE_GENERATED_AT));
        assertProvisioningCommand(AkitaProvisioningManager.buildProvisioningStageCommand(context), "LegacySecret123456");
        assertProvisioningCommand(new String(
                AkitaProvisioningManager.buildProvisioningStageCommandBytes(context), StandardCharsets.UTF_8),
                "LegacySecret123456");
        assertTrue(stateFile.exists());
        assertStoredFileIsEncrypted("LegacySecret123456");
    }

    @Test
    public void corruptSecureStoreBlocksLegacySecretFallback() {
        preferences.edit()
                .putString(AkitaProvisioningManager.PREF_PROVISIONING_SECRET, "LegacySecret123456")
                .commit();

        assertTrue(stateFile.mkdir());

        assertEquals("", AkitaProvisioningManager.getActiveProvisioningSecret(context));
        assertTrue(preferences.contains(AkitaProvisioningManager.PREF_PROVISIONING_SECRET));
    }

    private static void assertProvisioningCommand(String command, String secret) {
        String prefix = com.akitaengineering.meshtak.Config.CMD_PROVISION_STAGE_PREFIX + secret + ":";
        assertTrue(command.startsWith(prefix));
        long epoch = Long.parseLong(command.substring(prefix.length()));
        assertTrue(epoch >= 1609459200L && epoch <= 4102444800L);
    }

    @Test
    public void secureStoreWriteFailureIsSurfacedToCallers() {
        assertTrue(stateFile.mkdir());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> AkitaProvisioningManager.setCustomProvisioningSecret(context, "CustomSecret123456"));

        assertEquals(
                "Unable to persist encrypted provisioning state. Check device storage and Android Keystore availability.",
                exception.getMessage());
        assertFalse(preferences.contains(AkitaProvisioningManager.PREF_PROVISIONING_SECRET_SIGNAL));
    }

    private void assertStoredFileIsEncrypted(String rawSecret) {
        try {
            String stored = new String(Files.readAllBytes(stateFile.toPath()), StandardCharsets.UTF_8);
            assertFalse(stored.contains(rawSecret));
            assertTrue(stored.contains("\"ciphertext\""));
            assertTrue(stored.contains("\"iv\""));
        } catch (Exception exception) {
            throw new AssertionError("Failed to inspect provisioning state file", exception);
        }
    }
}
