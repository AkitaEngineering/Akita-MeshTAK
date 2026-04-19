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
    public void customProvisioningSecretOverridesFallback() {
        AkitaProvisioningManager.setCustomProvisioningSecret(context, "CustomSecret123456");

        assertEquals("CustomSecret123456", AkitaProvisioningManager.getActiveProvisioningSecret(context));
        assertFalse(preferences.contains(AkitaProvisioningManager.PREF_PROVISIONING_SECRET));
        assertTrue(stateFile.exists());
        assertStoredFileIsEncrypted("CustomSecret123456");
    }

    @Test
    public void stagedBundleLivesInSecureStore() {
        String bundle = String.format(
                Locale.US,
                "AKITA-PROV-1|NodeAlpha|%s|%s|1|BundleSecret123456",
            com.akitaengineering.meshtak.Config.ENCRYPTED_PAYLOAD_VERSION,
            com.akitaengineering.meshtak.Config.ENCRYPTED_KEY_ID);

        AkitaProvisioningManager.setStagedProvisioningBundle(context, bundle);

        assertEquals(com.akitaengineering.meshtak.Config.CMD_PROVISION_STAGE_PREFIX + "BundleSecret123456",
                AkitaProvisioningManager.buildProvisioningStageCommand(context));
        assertArrayEquals(
            (com.akitaengineering.meshtak.Config.CMD_PROVISION_STAGE_PREFIX + "BundleSecret123456").getBytes(StandardCharsets.UTF_8),
            AkitaProvisioningManager.buildProvisioningStageCommandBytes(context));
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
        assertEquals(com.akitaengineering.meshtak.Config.CMD_PROVISION_STAGE_PREFIX + "LegacySecret123456",
                AkitaProvisioningManager.buildProvisioningStageCommand(context));
        assertArrayEquals(
            (com.akitaengineering.meshtak.Config.CMD_PROVISION_STAGE_PREFIX + "LegacySecret123456").getBytes(StandardCharsets.UTF_8),
            AkitaProvisioningManager.buildProvisioningStageCommandBytes(context));
        assertTrue(stateFile.exists());
        assertStoredFileIsEncrypted("LegacySecret123456");
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
