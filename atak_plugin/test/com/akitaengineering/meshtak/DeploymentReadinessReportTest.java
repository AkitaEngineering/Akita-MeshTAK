package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.akitaengineering.meshtak.ui.AkitaMockSettings;
import com.akitaengineering.meshtak.ui.AkitaProvisioningManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DeploymentReadinessReportTest {

    private Context context;
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
        File stateFile = new File(context.getNoBackupFilesDir(), "akita-provisioning-state.json");
        if (stateFile.exists()) {
            stateFile.delete();
        }
        SecurityManager.getInstance().reset();
        AuditLogger.getInstance().initialize(context);
        OpenTakStreamingClient.getInstance().resetForTests();
        OpenTakStreamingClient.getInstance().attach(context);
    }

    @Test
    public void placeholderSecretAndMockModeFailFieldReadiness() {
        preferences.edit().putBoolean(AkitaMockSettings.PREF_MOCK_MODE, true).commit();
        DeploymentReadinessReport.Report report = DeploymentReadinessReport.evaluate(context);
        assertFalse(report.isFieldReady());
        assertTrue(report.asText().contains("Provisioning secret"));
        assertTrue(report.asText().contains("Mock transport"));
    }

    @Test
    public void sslWithoutCertificateFailsClosed() {
        AkitaProvisioningManager.setCustomProvisioningSecret(context, "UniqueDeviceSecret1234567890");
        SecurityManager.getInstance().initializeFromProvisioning("AkitaNode01", "UniqueDeviceSecret1234567890");
        SecurityManager.getInstance().setEncryptionEnabled(true);
        preferences.edit()
                .putBoolean(AkitaMockSettings.PREF_MOCK_MODE, false)
                .putBoolean(OpenTakStreamingClient.PREF_SSL, true)
                .putBoolean(OpenTakStreamingClient.PREF_ENABLED, true)
                .putString(OpenTakStreamingClient.PREF_HOST, "tak.example.local")
                .commit();
        DeploymentReadinessReport.Report report = DeploymentReadinessReport.evaluate(context);
        assertFalse(report.isFieldReady());
        assertTrue(report.asText().contains("OpenTAKServer SSL"));
        assertTrue(report.asText().contains("PKCS#12"));
    }
}
