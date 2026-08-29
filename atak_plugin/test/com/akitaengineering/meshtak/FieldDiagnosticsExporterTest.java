package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class FieldDiagnosticsExporterTest {

    private Context context;
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
        AuditLogger.getInstance().initialize(context);
        OpenTakStreamingClient.getInstance().resetForTests();
        OpenTakStreamingClient.getInstance().attach(context);
    }

    @Test
    public void bundleRedactsSecretLikeTextAndOmitsSecretKeys() throws Exception {
        preferences.edit()
                .putString("opentakserver_host", "tak.example.local")
                .putString("security_provisioning_secret", "should-not-appear")
                .commit();
        JSONObject bundle = FieldDiagnosticsExporter.buildBundle(context, preferences);
        String encoded = bundle.toString();
        assertFalse(encoded.contains("should-not-appear"));
        assertTrue(encoded.contains("readiness"));
        assertTrue(encoded.contains("openTakServer"));
        assertEquals("secret=REDACTED", FieldDiagnosticsExporter.redact("secret=super-secret-value"));
    }
}
